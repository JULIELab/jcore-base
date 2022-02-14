package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.reader.pmc.parser.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

public class CasPopulator {
    private final static Logger log = LoggerFactory.getLogger(CasPopulator.class);
    private NxmlDocumentParser nxmlDocumentParser;
    private Iterator<URI> nxmlIterator;

    public CasPopulator(Iterator<URI> nxmlIterator, Boolean omitBibReferences) throws IOException {
        this.nxmlIterator = nxmlIterator;
        nxmlDocumentParser = new NxmlDocumentParser();
        String settings = omitBibReferences ? "/de/julielab/jcore/reader/pmc/resources/elementproperties-no-bib-refs.yml" : "/de/julielab/jcore/reader/pmc/resources/elementproperties.yml";
        nxmlDocumentParser.loadElementPropertyFile(settings);
    }

    public CasPopulator(Boolean omitBibReferences) throws IOException {
        this(null, omitBibReferences);
    }

    public void populateCas(URI nxmlUri, JCas cas) throws ElementParsingException, NoDataAvailableException {
        ElementParsingResult result = null;
        URI currentUri = nxmlUri;
        while (currentUri != null && result == null) {
            try {
                nxmlDocumentParser.reset(currentUri, cas);
                result = nxmlDocumentParser.parse();
            } catch (DocumentParsingException e) {
                log.warn("Error occurred when trying to read from URI {} (ASCII string: {}): {}. Skipping document.", currentUri, currentUri.toASCIIString(), e.getMessage());
                if (nxmlIterator.hasNext()) {
                    currentUri = nxmlIterator.next();
                } else {
                    String msg = "Cannot just skip the errored document because there is no next document currently available. Returning without adding any data to the CAS.";
                    log.warn(msg);
                    throw new NoDataAvailableException(msg);
                }
            }
        }
        StringBuilder sb = populateCas(result, new StringBuilder());
        cas.setDocumentText(sb.toString());
    }

    public void populateCas(InputStream is, JCas cas) throws ElementParsingException, NoDataAvailableException {
        ElementParsingResult result;
        try {
            nxmlDocumentParser.reset(is, cas);
            result = nxmlDocumentParser.parse();
        } catch (DocumentParsingException e) {
            throw new NoDataAvailableException(e);
        }
        StringBuilder sb = populateCas(result, new StringBuilder());
        cas.setDocumentText(sb.toString());
    }

    /**
     * This is the actual method that reads the parsing results, created the CAS document text and adds
     * the annotations from the parsing results.
     *
     * @param result
     * @param sb
     * @return
     */
    private StringBuilder populateCas(ParsingResult result, StringBuilder sb) {
        switch (result.getResultType()) {
            case ELEMENT:
                ElementParsingResult elementParsingResult = (ElementParsingResult) result;
                String elementName = elementParsingResult.getElementName();
                boolean isBlockElement = elementParsingResult.isBlockElement() || (boolean) nxmlDocumentParser
                        .getTagProperties(elementName).getOrDefault(ElementProperties.BLOCK_ELEMENT, false);

                // There are elements that should have line breaks before and after
                // them like paragraphs, sections, captions etc. Other elements are
                // inline-elements, like xref, which should be embedded in the
                // surrounding text without line breaks.
                if (isBlockElement && sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append("\n");
                }

                int begin = sb.length();
                for (ParsingResult subResult : elementParsingResult.getSubResults()) {
                    populateCas(subResult, sb);
                }
                int end = sb.length();

                // There are elements that should have line breaks before and after
                // them like paragraphs, sections, captions etc. Other elements are
                // inline-elements, like xref, which should be embedded in the
                // surrounding text without line breaks.
                if (isBlockElement && sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append("\n");
                }
                Annotation annotation = elementParsingResult.getAnnotation();
                // if no annotation should be created, the parser is allowed to
                // return null
                if (annotation != null) {
                    annotation.setBegin(begin);
                    annotation.setEnd(end);
                    if (elementParsingResult.addAnnotationToIndexes())
                        annotation.addToIndexes();
                }
                break;
            case TEXT:
                TextParsingResult textParsingResult = (TextParsingResult) result;
                sb.append(textParsingResult.getText());
                break;
            case NONE:
                // do nothing
                break;
        }
        return sb;
    }
}
