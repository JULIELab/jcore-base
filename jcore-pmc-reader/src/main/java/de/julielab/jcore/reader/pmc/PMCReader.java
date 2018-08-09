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
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
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

public class PMCReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT = "Input";
    public static final String PARAM_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_SEARCH_ZIP = "SearchInZipFiles";
    private static final Logger log = LoggerFactory.getLogger(PMCReader.class);
    @ConfigurationParameter(name = PARAM_INPUT, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
    private File input;

    @ConfigurationParameter(name = PARAM_RECURSIVELY, defaultValue = "false", mandatory = false, description = "If set to true, subdirectories of the given input directory " + PARAM_INPUT + " are also searched for NXML files. Defaults to false.")
    private boolean searchRecursively;

    @ConfigurationParameter(name = PARAM_SEARCH_ZIP, defaultValue = "false", mandatory = false, description = "If set to true, ZIP files found among the input are opened and also searched for NXML files. Defaults to false.")
    private boolean searchZip;

    private Iterator<URI> pmcFiles;
    private Set<String> alreadyReadFilenames = Collections.emptySet();

    private long completed;
    private CasPopulator casPopulator;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        if (log.isInfoEnabled()) {
            log.info("Component configuration:");
            for (String configName : context.getConfigParameterNames()) {
                log.info("    {}: {}", configName, getConfigParameterValue(configName));
            }
        }
        input = new File((String) getConfigParameterValue(PARAM_INPUT));
        searchRecursively = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_RECURSIVELY)).orElse(false);
        searchZip = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_SEARCH_ZIP)).orElse(false);
        log.info("Reading PubmedCentral NXML file(s) from {}", input);
        try {
            pmcFiles = new NXMLURIIterator(input, searchRecursively, searchZip);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        completed = 0;
        casPopulator = new CasPopulator(pmcFiles);
    }

    @Override
    public void getNext(JCas cas) throws CollectionException {
        try {
            URI next = pmcFiles.next();
            casPopulator.populateCas(next, cas);

        } catch (DocumentParsingException | ElementParsingException e) {
            throw new CollectionException(e);
        }
        completed++;
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
