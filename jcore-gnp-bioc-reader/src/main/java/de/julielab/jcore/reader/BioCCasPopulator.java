package de.julielab.jcore.reader;

import com.pengyifan.bioc.*;
import com.pengyifan.bioc.io.BioCCollectionReader;
import de.julielab.jcore.types.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads a BioC collection from file and adds the species and gene annotations from its documents to a JCases.
 */
public class BioCCasPopulator {

    private final static Logger log = LoggerFactory.getLogger(BioCCasPopulator.class);
    private final BioCCollection bioCCollection;
    private int pos;

    public BioCCasPopulator(Path biocCollectionPath) throws XMLStreamException, IOException {
        try (BioCCollectionReader bioCCollectionReader = new BioCCollectionReader(biocCollectionPath)) {
            bioCCollection = bioCCollectionReader.readCollection();
        }
        pos = 0;
    }

    public void populateWithNextDocument(JCas jCas) throws XMLStreamException, IOException {
        BioCDocument document = bioCCollection.getDocument(pos++);
        setDocumentText(jCas, document);
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
                    case "Species":
                        addSpeciesAnnotation(annotation, jCas);
                        break;
                }
            } catch (MissingInfonException e) {
                throw new IllegalArgumentException("BioCDocument " + document.getID() + " has an annotation issue; see cause exception.", e);
            }
        }
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
        ResourceEntry resourceEntry = new ResourceEntry(jCas, gene.getBegin(), gene.getEnd());
        resourceEntry.setSource("NCBI Gene");
        resourceEntry.setComponentId(GNormPlusFormatMultiplierReader.class.getCanonicalName());
        resourceEntry.setEntryId(geneId.get());
        FSArray resourceEntryList = new FSArray(jCas, 1);
        resourceEntryList.set(0, resourceEntry);
        gene.setResourceEntryList(resourceEntryList);
        gene.addToIndexes();
    }

    public int documentsLeftInCollection() {
        return bioCCollection.getDocmentCount() - pos;
    }
}
