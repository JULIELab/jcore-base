package de.julielab.jcore.reader.pmc;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Optional;

public abstract class PMCReaderBase extends JCasCollectionReader_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(PMCReaderBase.class);
    public static final String PARAM_INPUT = "Input";
    public static final String PARAM_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_SEARCH_ZIP = "SearchInZipFiles";
    @ConfigurationParameter(name = PARAM_INPUT, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
    protected File input;

    @ConfigurationParameter(name = PARAM_RECURSIVELY, defaultValue = "false", mandatory = false, description = "If set to true, subdirectories of the given input directory " + PARAM_INPUT + " are also searched for NXML files. Defaults to false.")
    protected boolean searchRecursively;

    @ConfigurationParameter(name = PARAM_SEARCH_ZIP, defaultValue = "false", mandatory = false, description = "If set to true, ZIP files found among the input are opened and also searched for NXML files. Defaults to false.")
    protected boolean searchZip;

    protected Iterator<URI> pmcFiles;

    protected int completed;

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
    }


    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return pmcFiles.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] {new ProgressImpl(completed, -1, "documents")};
    }
}
