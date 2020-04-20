package de.julielab.jcore.reader.cord19;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.julielab.java.utilities.FileUtilities;
import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.reader.cord19.jsonformat.CiteSpan;
import de.julielab.jcore.reader.cord19.jsonformat.Cord19Document;
import de.julielab.jcore.reader.cord19.jsonformat.Paragraph;
import de.julielab.jcore.reader.cord19.jsonformat.TabFigRef;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.casmultiplier.JCoReURI;
import de.julielab.jcore.types.pubmed.AbstractText;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.OtherID;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * <p>
 * This is currently not a UIMA reader but just the code for reading the CORD-19 JSON format.
 * </p>
 * <p>The format is described with this JSON schema: https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/2020-03-13/json_schema.txt</p>
 */
public class Cord19Reader {
    private final static Logger log = LoggerFactory.getLogger(Cord19Reader.class);
    private final ObjectMapper om;
    private final String linesep;
    private Map<String, MetadataRecord> metadataIdMap;

    public Cord19Reader() {
        om = new ObjectMapper();
        linesep = System.getProperty("line.separator");
    }

    public void readCord19JsonFile(JCoReURI uri, String metadataFile, JCas jCas) throws AnalysisEngineProcessException {
        if (metadataIdMap == null)
            readMetaData(metadataFile);
        try {
            StringBuilder doctext = new StringBuilder();
            Cord19Document document = om.readValue(URI.create(uri.getUri()).toURL(), Cord19Document.class);
            MetadataRecord metadataRecord = metadataIdMap.get(document.getPaperId());

            addMetadata(jCas, document, metadataRecord);
            addTitle(jCas, document, metadataRecord, doctext);
            addAbstract(jCas, doctext, document);
            addBody(jCas, doctext, document);
            Map<String, TabFigRef> refEntries = document.getRefEntries();
            addTabFigs(jCas, doctext, refEntries);

            jCas.setDocumentText(doctext.toString());
        } catch (IOException e) {
            log.error("Could not read document from URI {}", uri.getUri());
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void addTabFigs(JCas jCas, StringBuilder doctext, Map<String, TabFigRef> refEntries) {
        int textpos = doctext.length();
        for (String refId : refEntries.keySet()) {
            TabFigRef ref = refEntries.get(refId);
            Caption caption = new Caption(jCas, textpos, textpos + ref.getText().length());
            doctext.append(ref.getText());
            caption.setCaptionType(ref.getType());
            caption.addToIndexes();
        }
        doctext.append(linesep);
    }

    private void addBody(JCas jCas, StringBuilder doctext, Cord19Document document) {
        Section currentSection = null;
        String lastSectionName = null;
        // We use this to determine the end of the current paragraph BEFORE the newline.
        int currentEnd = 0;
        for (Paragraph p : document.getBody()) {
            if (lastSectionName != null && !lastSectionName.equals(p.getSection())) {
                currentSection.setEnd(currentEnd);
                currentSection.addToIndexes();
                currentSection = null;
            }
            if (currentSection == null) {
                Title sHeading = new Title(jCas, doctext.length(), doctext.length() + p.getSection().length());
                sHeading.setTitleType("section");
                doctext.append(p.getSection());
                doctext.append(linesep);
                currentSection = new Section(jCas);
                currentSection.setBegin(doctext.length());
                currentSection.setSectionHeading(sHeading);
            }

            int paragraphBegin = doctext.length();
            de.julielab.jcore.types.Paragraph paragraph = new de.julielab.jcore.types.Paragraph(jCas, paragraphBegin, doctext.length() + p.getText().length());
            doctext.append(p.getText());
            currentEnd = doctext.length();
            doctext.append(linesep);
            addReferences(p, Paragraph::getRefSpans, paragraphBegin, jCas);
            addReferences(p, Paragraph::getEqSpans, paragraphBegin, jCas);
            addReferences(p, Paragraph::getCiteSpans, paragraphBegin, jCas);
            paragraph.addToIndexes();
            lastSectionName = p.getSection();
        }
        // Add the very last section
        if (currentSection != null) {
            currentSection.setEnd(doctext.length());
            currentSection.addToIndexes();
            doctext.append(linesep);
        }
    }

    private void addAbstract(JCas jCas, StringBuilder doctext, Cord19Document document) {
        List<AbstractSection> sections = new ArrayList<>(document.getAbstr().size());
        int abstractBegin = doctext.length();
        // Stores the end of the last paragraph before the newline
        int lastEnd = 0;
        for (Paragraph p : document.getAbstr()) {
            int paragraphBegin = doctext.length();
            AbstractSection as = new AbstractSection(jCas, paragraphBegin, doctext.length() + p.getText().length());
            doctext.append(p.getText());
            lastEnd = doctext.length();
            doctext.append(linesep);
            AbstractSectionHeading asHeading = new AbstractSectionHeading(jCas);
            asHeading.setTitleType("abstract");
            asHeading.setLabel(p.getSection());
            as.setAbstractSectionHeading(asHeading);
            sections.add(as);
            addReferences(p, Paragraph::getRefSpans, paragraphBegin, jCas);
            addReferences(p, Paragraph::getEqSpans, paragraphBegin, jCas);
            addReferences(p, Paragraph::getCiteSpans, paragraphBegin, jCas);
        }
        AbstractText abstractText = new AbstractText(jCas, abstractBegin, lastEnd);
        abstractText.setAbstractType("main");
        abstractText.setStructuredAbstractParts(JCoReTools.addToFSArray(null, sections));
        abstractText.addToIndexes();
        doctext.append(linesep);
    }

    private void addReferences(Paragraph p, Function<Paragraph, Iterable<CiteSpan>> refFunc, int paragraphBegin, JCas jCas) {
        Iterable<CiteSpan> references = refFunc.apply(p);
        if (references == null)
            return;
        for (CiteSpan cs : references) {
            de.julielab.jcore.types.pubmed.InternalReference ref = new de.julielab.jcore.types.pubmed.InternalReference(jCas, paragraphBegin + cs.getStart(), paragraphBegin + cs.getEnd());
            ref.setId(cs.getRefId());
            ref.addToIndexes();
        }
    }


    private void addTitle(JCas jCas, Cord19Document document, MetadataRecord metadataRecord, StringBuilder doctext) {
        if (metadataRecord != null) {
            String title = metadataRecord.getTitle();
            if (title != null) {
                addTitle(jCas, title, doctext);
            }
        } else {
            addTitle(jCas, document.getMetadata().getTitle(), doctext);
        }
        doctext.append(linesep);
    }

    private void addTitle(JCas jCas, String title, StringBuilder doctext) {
        Title casTitle = new Title(jCas, doctext.length(), doctext.length() + title.length());
        doctext.append(title);
        casTitle.setTitleType("document");
        casTitle.addToIndexes();
        doctext.append(casTitle);
    }

    private void addMetadata(JCas jCas, Cord19Document document, MetadataRecord metadataRecord) {
        Header h = new Header(jCas);
        h.setDocId(document.getPaperId());
        if (metadataRecord != null) {
            String pmcid = metadataRecord.getPmcid();
            if (pmcid != null) {
                OtherID otherID = new OtherID(jCas);
                otherID.setSource("PMC");
                otherID.setId(pmcid);
                h.setOtherIDs(JCoReTools.addToFSArray(h.getOtherIDs(), otherID));
            }
            String pmid = metadataRecord.getPmid();
            if (pmid != null) {
                OtherID otherID = new OtherID(jCas);
                otherID.setSource("PubMed");
                otherID.setId(pmid);
                h.setOtherIDs(JCoReTools.addToFSArray(h.getOtherIDs(), otherID));
            }
            OtherID otherID = new OtherID(jCas);
            otherID.setSource("CORD19-UID");
            otherID.setId(metadataRecord.getCordUid());
            h.setOtherIDs(JCoReTools.addToFSArray(h.getOtherIDs(), otherID));
        }
        h.addToIndexes();
    }

    private void readMetaData(String metadataFile) {
        metadataIdMap = Collections.emptyMap();
        try {
            InputStream is = FileUtilities.findResource(metadataFile);

            if (is != null) {
                metadataIdMap = new HashMap<>();
                try (BufferedReader br = IOStreamUtilities.getReaderFromInputStream(is); CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(br)) {
                    for (CSVRecord record : parser) {
                        // the fields are:
                        // cord_uid,sha,source_x,title,doi,pmcid,pubmed_id,license,abstract,publish_time,authors,journal,Microsoft Academic Paper ID,WHO #Covidence,has_pdf_parse,has_pmc_xml_parse,full_text_file,url
                        String cordUid = record.get("cord_uid");
                        String sha = record.get("sha");
                        String title = record.get("title");
                        String pmcid = record.get("pmcid");
                        String pmid = record.get("pubmed_id");
                        MetadataRecord metadataRecord = new MetadataRecord(cordUid, sha, pmcid, pmid, title);
                        for (String hash : metadataRecord.hashes)
                            metadataIdMap.put(hash, metadataRecord);
                        if (pmcid != null)
                            metadataIdMap.put(pmcid, metadataRecord);
                        if (pmid != null)
                            metadataIdMap.put(pmid, metadataRecord);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not read");
        }
    }

    private static class MetadataRecord {
        private final String cordUid;
        private final String pmcid;
        private final String pmid;
        private final String[] hashes;
        private final String title;

        public MetadataRecord(String cordUid, String sha, String pmcid, String pmid, String title) {
            this.cordUid = cordUid;
            this.pmcid = pmcid;
            this.pmid = pmid;
            this.title = title;
            this.hashes = Arrays.stream(sha.split(";")).map(String::trim).toArray(String[]::new);
        }

        public String getCordUid() {
            return cordUid;
        }

        public String getPmcid() {
            return pmcid;
        }

        public String getPmid() {
            return pmid;
        }

        public String getTitle() {
            return title;
        }
    }
}
