package de.julielab.jcore.reader;

import com.pengyifan.bioc.*;
import com.pengyifan.bioc.io.BioCCollectionReader;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.types.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a BioC collection from file and adds the species and gene annotations from its documents to a JCases.
 */
public class BioCCasPopulator {

    private final static Logger log = LoggerFactory.getLogger(BioCCasPopulator.class);
    private final BioCCollection bioCCollection;
    private Map<String, Integer> maxXmiIdMap;
    private Map<String, String> sofaMaps;
    private int pos;

    public BioCCasPopulator(Path biocCollectionPath, Path costosysConfiguration, String documentsTable) throws XMLStreamException, IOException, SQLException {
        try (BioCCollectionReader bioCCollectionReader = new BioCCollectionReader(biocCollectionPath)) {
            bioCCollection = bioCCollectionReader.readCollection();
        }
        if (costosysConfiguration != null) {
            maxXmiIdMap = new HashMap<>();
            sofaMaps = new HashMap<>();
            DataBaseConnector dbc = new DataBaseConnector(costosysConfiguration.toString());
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                retrieveXmiMetaData(documentsTable, dbc, conn);
            }
        }
        pos = 0;
    }

    private void retrieveXmiMetaData(String documentsTable, DataBaseConnector dbc, CoStoSysConnection conn) throws SQLException {
        log.debug("Retrieving the max XMI IDs for the current BioC collection of size {} from the database.", bioCCollection.getDocmentCount());
        Statement stmt = conn.createStatement();
        StringBuilder maxIdQueryBuilder = new StringBuilder();
        if (dbc.getActiveTableFieldConfiguration().getPrimaryKey().length > 1)
            throw new IllegalArgumentException("The primary key of the active field schema '" + dbc.getActiveTableFieldConfiguration().getName() + "' is a compound key. Compound primary keys are currently not supported in this component.");
        String pkString = dbc.getActiveTableFieldConfiguration().getPrimaryKeyString();
        maxIdQueryBuilder.append("SELECT ").append(pkString).append(",max_xmi_id,sofa_mapping FROM ").append(documentsTable).append(" WHERE ").append(pkString).append(" in ").append("(");
        for (BioCDocument document : bioCCollection.getDocuments()) {
            String docId = document.getID();
            maxIdQueryBuilder.append("'").append(docId).append("'").append(",");
        }
        // remove trailing comma
        maxIdQueryBuilder.deleteCharAt(maxIdQueryBuilder.length() - 1);
        maxIdQueryBuilder.append(")");
        String maxIdQuery = maxIdQueryBuilder.toString();
        ResultSet rs = stmt.executeQuery(maxIdQuery);
        while (rs.next()) {
            maxXmiIdMap.put(rs.getString(1), rs.getInt(2));
            sofaMaps.put(rs.getString(1), rs.getString(3));
        }
        if (log.isTraceEnabled()) {
            log.trace("XMI ID sample: {}", maxXmiIdMap.entrySet().stream().limit(10).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            log.trace("Sofa map sample: {}", sofaMaps.entrySet().stream().limit(10).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        log.debug("Obtained {} max XMI IDs.", maxXmiIdMap.size());
    }

    public void populateWithNextDocument(JCas jCas) {
        BioCDocument document = bioCCollection.getDocument(pos++);
        setDocumentId(jCas, document);
        setDocumentText(jCas, document);
        setMaxXmiId(jCas, document);
        Iterator<BioCAnnotation> allAnnotations = Stream.concat(document.getAnnotations().stream(), document.getPassages().stream().map(BioCPassage::getAnnotations).flatMap(Collection::stream)).iterator();
        for (BioCAnnotation annotation : (Iterable<BioCAnnotation>)() ->allAnnotations) {
            Optional<String> type = annotation.getInfon("type");
            if (!type.isPresent())
                throw new IllegalArgumentException("BioCDocument " + document.getID() + " has an annotation that does not specify its type: " + annotation);
            try {
                switch (type.get()) {
                    case "Gene":
                        addGeneAnnotation(annotation, jCas);
                        break;
                    case "FamilyName":
                        addFamilyAnnotation(annotation, jCas);
                        break;
                    case "Species":
                        addSpeciesAnnotation(annotation, jCas);
                        break;
                }
            } catch (MissingInfonException | IllegalArgumentException e) {
                throw new IllegalArgumentException("BioCDocument " + document.getID() + " has an annotation issue; see cause exception.", e);
            }
        }
    }


    private void setMaxXmiId(JCas jCas, BioCDocument document) {
        if (maxXmiIdMap != null) {
            Integer maxXmiId = maxXmiIdMap.get(document.getID());
            String mappingString = sofaMaps.get(document.getID());
            if (maxXmiId == null)
                throw new IllegalStateException("No max XMI ID was obtained for the document with ID " + document.getID() + ". This means that this document is not already part of the database documents table. When adding annotations to existing database documents, make sure that all documents exist in the database already.");
            XmiMetaData xmiMetaData = new XmiMetaData(jCas);
            xmiMetaData.setMaxXmiId(maxXmiId);
            String[] mappings = mappingString != null ? mappingString.split("\\|") : null;
            StringArray mappingsArray = null;
            if (mappings != null) {
                mappingsArray = new StringArray(jCas, mappings.length);
                for (int i = 0; i < mappings.length; i++) {
                    String mapping = mappings[i];
                    mappingsArray.set(i, mapping);
                }
            }
            if (mappingsArray != null)
                xmiMetaData.setSofaIdMappings(mappingsArray);
            xmiMetaData.addToIndexes();
        }
    }

    private void setDocumentId(JCas jCas, BioCDocument document) {
        Header h = new Header(jCas);
        h.setDocId(document.getID());
        h.addToIndexes();
    }

    private void setDocumentText(JCas jCas, BioCDocument document) {
        StringBuilder sb = new StringBuilder();
        // iterate over the passages and create the complete document text from their individual text elements
        for (BioCPassage passage : document.getPassages()) {
            int offset = passage.getOffset();
            // The offset of the passage must match its starting position in the StringBuilder or the annotation
            // offsets won't match. We might need to fill up the StringBuilder to reach the given offset.
            while (sb.length() < offset)
                sb.append(" ");
            if (passage.getText().isPresent()) {
                sb.append(passage.getText().get());
                Optional<String> type = passage.getInfon("type");
                if (type.isPresent()) {
                    int passageEnd = offset + passage.getText().get().length();
                    Zone passageAnnotation;
                    // The values in this switch are basically determined by the values created in the BioCDocumentPopulator in the jcore-gnp-bioc-writer project.
                    switch (type.get()) {
                        case "title":
                            passageAnnotation = new Title(jCas, offset, passageEnd);
                            ((Title) passageAnnotation).setTitleType("document");
                            break;
                        case "section_title":
                            passageAnnotation = new Title(jCas, offset, passageEnd);
                            ((Title) passageAnnotation).setTitleType("section");
                            break;
                        case "figure_title":
                            passageAnnotation = new Title(jCas, offset, passageEnd);
                            ((Title) passageAnnotation).setTitleType("figure");
                            break;
                        case "table_title":
                            passageAnnotation = new Title(jCas, offset, passageEnd);
                            ((Title) passageAnnotation).setTitleType("table");
                            break;
                        case "other_title":
                            passageAnnotation = new Title(jCas, offset, passageEnd);
                            ((Title) passageAnnotation).setTitleType("other");
                            break;
                        case "abstract":
                            passageAnnotation = new AbstractText(jCas, offset, passageEnd);
                            break;
                        case "paragraph":
                            passageAnnotation = new Paragraph(jCas, offset, passageEnd);
                            break;
                        case "figure":
                        case "table":
                            // for figures and tables we have actually no means to distinguish between captions and the actual object; mainly because the actual objects have so far not been part of the CAS documents; thus, this can only be a caption until the objects themselves are added
                            passageAnnotation = new Caption(jCas, offset, passageEnd);
                            ((Caption) passageAnnotation).setCaptionType(type.get());
                        default:
                            log.debug("Unhandled passage type {}", type.get());
                            passageAnnotation = new Zone(jCas, offset, passageEnd);
                            break;
                    }
                    passageAnnotation.setComponentId(GNormPlusFormatMultiplier.class.getCanonicalName());
                    passageAnnotation.addToIndexes();
                }
            }
        }
        jCas.setDocumentText(sb.toString());
    }

    private void addSpeciesAnnotation(BioCAnnotation annotation, JCas jCas) throws MissingInfonException {
        Optional<String> taxId = annotation.getInfon("NCBI Taxonomy");
        if (!taxId.isPresent())
            throw new MissingInfonException("Species annotation does not specify its taxonomy ID: " + annotation);
        // the "total location" is the span from the minimum location value to the maximum location value;
        // for GNormPlus, there are no discontinuing annotations anyway
        BioCLocation location = annotation.getTotalLocation();
        Organism organism = new Organism(jCas, location.getOffset(), location.getOffset() + location.getLength());
        ResourceEntry resourceEntry = new ResourceEntry(jCas, organism.getBegin(), organism.getEnd());
        resourceEntry.setSource("NCBI Taxonomy");
        resourceEntry.setComponentId(GNormPlusFormatMultiplierReader.class.getCanonicalName());
        resourceEntry.setEntryId(taxId.get());
        FSArray resourceEntryList = new FSArray(jCas, 1);
        resourceEntryList.set(0, resourceEntry);
        organism.setResourceEntryList(resourceEntryList);
        organism.addToIndexes();
    }

    private void addGeneAnnotation(BioCAnnotation annotation, JCas jCas) throws MissingInfonException {
        Optional<String> geneId = annotation.getInfon("NCBI Gene");
        if (!geneId.isPresent())
            throw new MissingInfonException("Gene annotation does not specify its gene ID: " + annotation);
        // the "total location" is the span from the minimum location value to the maximum location value;
        // for GNormPlus, there are no discontinuing annotations anyway
        BioCLocation location = annotation.getTotalLocation();
        Gene gene = new Gene(jCas, location.getOffset(), location.getOffset() + location.getLength());
        gene.setComponentId(GNormPlusFormatMultiplierReader.class.getCanonicalName());
        gene.setSpecificType("Gene");
        // one gene mention might have multiple IDs when there are ranges or enumerations, e.g. "IL2-5", "B7-1 and B7-2" or "B7-1/2"
        String[] geneIds = geneId.get().split(";");
        FSArray resourceEntryList = new FSArray(jCas, geneIds.length);
        for (int i = 0; i < geneIds.length; i++) {
            ResourceEntry resourceEntry = new ResourceEntry(jCas, gene.getBegin(), gene.getEnd());
            // 9999 ist the GeNo score for exact matches; GNP only recognized exact dictionary matches and transfers
            // their IDs to other forms under certain circumstances (abbreviations, for example)
            resourceEntry.setConfidence("9999");
            resourceEntry.setSource("NCBI Gene");
            resourceEntry.setComponentId(GNormPlusFormatMultiplierReader.class.getCanonicalName());
            resourceEntry.setEntryId(geneIds[i]);
            resourceEntryList.set(i, resourceEntry);
        }
        gene.setResourceEntryList(resourceEntryList);
        gene.addToIndexes();
    }

    private void addFamilyAnnotation(BioCAnnotation annotation, JCas jCas) {
        // the "total location" is the span from the minimum location value to the maximum location value;
        // for GNormPlus, there are no discontinuing annotations anyway
        BioCLocation location;
        try {
            location = annotation.getTotalLocation();
        } catch (Exception e) {
            // This handles a legacy issue: We modified GNormPlus to output FamilyName annotations. For some reason,
            // FamilyNames can have zero length. This has been fixed but there is still old output that would
            // cause an error at this point. Thus, when the offsets are invalid, skip the annotation.
            return;
        }
        Gene gene = new Gene(jCas, location.getOffset(), location.getOffset() + location.getLength());
        gene.setSpecificType("FamilyName");
        // e.g.  <infon key="FocusSpecies">NCBITaxonomyID:9606</infon>
        Optional<String> focusSpecies = annotation.getInfon("FocusSpecies");
        if (focusSpecies.isPresent()) {
            String taxId = focusSpecies.get().substring(15);
            StringArray speciesArray = new StringArray(jCas, 1);
            speciesArray.set(0, taxId);
            gene.setSpecies(speciesArray);
        }
        gene.addToIndexes();
    }

    public int documentsLeftInCollection() {
        return bioCCollection.getDocmentCount() - pos;
    }

    public long getCollectionTextLength() {
        return bioCCollection.getDocuments().stream().map(BioCDocument::getPassages).flatMap(Collection::stream).mapToInt(passage -> passage.getText().orElse("").length()).sum();
    }

    public int getNumDocumentsInCollection() {
        return bioCCollection.getDocmentCount();
    }
}
