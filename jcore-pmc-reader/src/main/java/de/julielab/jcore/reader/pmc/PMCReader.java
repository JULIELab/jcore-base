package de.julielab.jcore.reader.pmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.pmc.parser.DocTypeNotFoundException;
import de.julielab.jcore.reader.pmc.parser.DocumentParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingResult;
import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser;
import de.julielab.jcore.reader.pmc.parser.ParsingResult;
import de.julielab.jcore.reader.pmc.parser.TextParsingResult;

public class PMCReader extends CollectionReader_ImplBase {

    public static final String PARAM_INPUT = "Input";
    public static final String PARAM_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_ALREADY_READ = "AlreadyRead";
    public static final String PARAM_SEARCH_ZIP = "SearchInZipFiles";
    private static final Logger log = LoggerFactory.getLogger(PMCReader.class);
    @ConfigurationParameter(name = PARAM_INPUT, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
    private File input;

    @ConfigurationParameter(name = PARAM_RECURSIVELY, defaultValue = "false", mandatory = false, description = "If set to true, subdirectories of the given input directory " + PARAM_INPUT + " are also searched for NXML files. Defaults to false.")
    private boolean searchRecursively;

    @ConfigurationParameter(name = PARAM_ALREADY_READ, mandatory = false, description = "A file that contains a list list of already read file names. Those will be skipped by the reader. While reading, the reader will append read files to this list. If it is not given, the file will not be maintained.")
    private File alreadyReadFile;

    @ConfigurationParameter(name = PARAM_SEARCH_ZIP, defaultValue = "false", mandatory = false, description = "If set to true, ZIP files found among the input are opened and also searched for NXML files. Defaults to false.")
    private boolean searchZip;

    private Iterator<URI> pmcFiles;
    private Set<String> alreadyReadFilenames = Collections.emptySet();

    private long completed;

    private NxmlDocumentParser nxmlDocumentParser;

    @Override
    public void initialize() throws ResourceInitializationException {
        if (log.isInfoEnabled()) {
            log.info("Component configuration:");
            for (String configName : getUimaContext().getConfigParameterNames()) {
                log.info("    {}: {}", configName, getConfigParameterValue(configName));
            }
        }
        input = new File((String) getConfigParameterValue(PARAM_INPUT));
        alreadyReadFile = Optional.ofNullable((String) getConfigParameterValue(PARAM_ALREADY_READ)).map(File::new)
                .orElse(null);
        searchRecursively = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_RECURSIVELY)).orElse(false);
        searchZip = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_SEARCH_ZIP)).orElse(false);
        log.info("Reading PubmedCentral NXML file(s) from {}", input);
        try {
            if (alreadyReadFile != null && alreadyReadFile.exists())
                alreadyReadFilenames = new HashSet<>(FileUtils.readLines(alreadyReadFile, "UTF-8"));
            pmcFiles = new NXMLURIIterator(input, searchRecursively, searchZip);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        completed = 0;
        nxmlDocumentParser = new NxmlDocumentParser();
        try {
            nxmlDocumentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(CAS cas) throws IOException, CollectionException {
        try {
            URI next = pmcFiles.next();
            while (pmcFiles.hasNext() && alreadyReadFilenames.contains(next.toString())) {
                log.trace("File {} has already been read. Skipping.", next);
                next = pmcFiles.next();
            }
            log.trace("Now reading file {}", next);
            ElementParsingResult result = null;
            while (next != null && result == null) {
                try {
                    nxmlDocumentParser.reset(next, cas.getJCas());
                    result = nxmlDocumentParser.parse();
                } catch (DocTypeNotFoundException e) {
                    log.warn("Error occurred: {}. Skipping document.", e.getMessage());
                    if (pmcFiles.hasNext())
                        next = pmcFiles.next();
                }
            }
            StringBuilder sb = populateCas(result, cas, new StringBuilder());
            cas.setDocumentText(sb.toString());
            if (alreadyReadFile != null)
                FileUtils.write(alreadyReadFile, next.toString() + "\n", "UTF-8", true);
        } catch (CASException | DocumentParsingException | ElementParsingException e) {
            throw new CollectionException(e);
        }
        completed++;
    }

    private StringBuilder populateCas(ParsingResult result, CAS cas, StringBuilder sb) {
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
                    populateCas(subResult, cas, sb);
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

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return pmcFiles.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new Progress() {

            /**
             *
             */
            private static final long serialVersionUID = 6058019619024287436L;

            @Override
            public boolean isApproximate() {
                return true;
            }

            @Override
            public String getUnit() {
                return "files";
            }

            @Override
            public long getTotal() {
                return -1;
            }

            @Override
            public long getCompleted() {
                return completed;
            }
        }};
    }

    @Override
    public void close() throws IOException {
        pmcFiles = null;
    }


}
