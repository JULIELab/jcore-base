package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.reader.pmc.parser.*;
import de.julielab.jcore.types.Header;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CasPopulator {
    private final static Logger log = LoggerFactory.getLogger(CasPopulator.class);
    private NxmlDocumentParser nxmlDocumentParser;
    private Iterator<URI> nxmlIterator;
    private int truncationSize;

    public CasPopulator(Iterator<URI> nxmlIterator, Boolean omitBibReferences, int truncationSize) throws IOException {
        this.nxmlIterator = nxmlIterator;
        this.truncationSize = truncationSize;
        nxmlDocumentParser = new NxmlDocumentParser();
        String settings = omitBibReferences ? "/de/julielab/jcore/reader/pmc/resources/elementproperties-no-bib-refs.yml" : "/de/julielab/jcore/reader/pmc/resources/elementproperties.yml";
        nxmlDocumentParser.loadElementPropertyFile(settings);
    }

    public CasPopulator(Boolean omitBibReferences, int truncationSize) throws IOException {
        this(null, omitBibReferences, truncationSize);
    }

    public CasPopulator(Boolean omitBibReferences) throws IOException {
        this(null, omitBibReferences, Integer.MAX_VALUE);
    }

    public CasPopulator(Iterator<URI> pmcFiles, boolean omitBibReferences) throws IOException {
        this(pmcFiles, omitBibReferences, Integer.MAX_VALUE);
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
        truncateTextAndAnnotations(sb.toString(), cas);
    }

    private void truncateTextAndAnnotations(String documentText, JCas cas) {
        String text = documentText.length() > truncationSize ? documentText.substring(0, truncationSize) : documentText;
        cas.setDocumentText(text);
        // if truncation happened, we need to remove annotations exceeding the valid text span
        List<Annotation> toRemove = new ArrayList<>();
        if (text.length() < documentText.length()) {
            for (Annotation a : cas.getAnnotationIndex()) {
                if (a.getEnd() > text.length()) {
                    if (a instanceof Header) {
                        // We don't want to remove the header. It is not really a text-anchored annotation anyway,
                        // just shrink its span.
                        a.removeFromIndexes();
                        if (a.getBegin() > text.length())
                            a.setBegin(0);
                        a.setEnd(text.length());
                        a.addToIndexes();
                    } else {
                        toRemove.add(a);
                    }
                }
            }
        }
        toRemove.forEach(Annotation::removeFromIndexes);
    }

    private String truncateText(String documentText) {
        // Truncate the document text to the given length
        return documentText.length() > truncationSize ? documentText.substring(0, truncationSize) : documentText;
    }

    public void populateCas(InputStream is, JCas cas) throws ElementParsingException, NoDataAvailableException {
        ElementParsingResult result;
        try {
            nxmlDocumentParser.reset(is, cas);
            result = nxmlDocumentParser.parse();
        } catch (DocumentParsingException e) {
            throw new NoDataAvailableException(e);
        }
        String documentText = populateCas(result, new StringBuilder()).toString();
        truncateTextAndAnnotations(documentText, cas);
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
                final String text = textParsingResult.getText();
                // some special handling for documents that contain formatting tabs, newlines or no-break-spaces in the text
                boolean textBeginsWithWhitespace = text.isEmpty() ? false : Character.isWhitespace(text.charAt(0));
                boolean textEndsWithWhitespace = text.isEmpty() ? false : Character.isWhitespace(text.charAt(text.length()-1));
                boolean sbEndsWithWhitespace = sb.length() == 0 ? false : Character.isWhitespace(sb.charAt(sb.length() - 1));
                if (textBeginsWithWhitespace && !sbEndsWithWhitespace)
                    sb.append(" ");
                sb.append(StringUtils.normalizeSpace(text));
                if (textEndsWithWhitespace)
                    sb.append(" ");
                break;
            case NONE:
                // do nothing
                break;
        }
        return sb;
    }
}
