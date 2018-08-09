package de.julielab.jcore.reader.pmc;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class PMCReaderBase extends JCasCollectionReader_ImplBase {
    public static final String PARAM_INPUT = "Input";
    public static final String PARAM_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_SEARCH_ZIP = "SearchInZipFiles";
    public static final String PARAM_WHITELIST = "WhitelistFile";
    private final static Logger log = LoggerFactory.getLogger(PMCReaderBase.class);
    @ConfigurationParameter(name = PARAM_INPUT, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
    protected File input;

    @ConfigurationParameter(name = PARAM_RECURSIVELY, defaultValue = "false", mandatory = false, description = "If set to true, subdirectories of the given input directory " + PARAM_INPUT + " are also searched for NXML files. Defaults to false.")
    protected boolean searchRecursively;

    @ConfigurationParameter(name = PARAM_SEARCH_ZIP, defaultValue = "false", mandatory = false, description = "If set to true, ZIP files found among the input are opened and also searched for NXML files. Defaults to false.")
    protected boolean searchZip;

    @ConfigurationParameter(name = PARAM_WHITELIST, mandatory = false, description = "A file listing the file names that should be read. All other files will be discarded. The file name must be given without any extensions. For example, the file \"PMC2847692.nxml.gz\" would be represented as \"PMC2847692\" in the whitelist file. Each file name must appear on a line of its own. An empty file will cause nothing to be read. A file containing only the keyword \"all\" will behave as if no file was given at all.")
    protected File whitelistFile;

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
        whitelistFile = Optional.ofNullable((String) getConfigParameterValue(PARAM_WHITELIST)).map(File::new).orElse(null);
        log.info("Reading PubmedCentral NXML file(s) from {}", input);
        try {
            Set<String> whitelist = readWhitelist(whitelistFile);
            pmcFiles = new NXMLURIIterator(input, whitelist, searchRecursively, searchZip);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        completed = 0;
    }

    private Set<String> readWhitelist(File whitelistFile) throws IOException {
        Set<String> whitelist = new HashSet<>();
        if (whitelistFile == null) {
            whitelist.add("all");
        } else {
            try (BufferedReader br = Files.newBufferedReader(whitelistFile.toPath(), StandardCharsets.UTF_8)) {
                whitelist = br.lines().filter(l -> !StringUtils.isBlank(l)).collect(Collectors.toSet());
            }
            log.debug("Read whitelist with {} entries from {}", whitelist.size(), whitelistFile);
        }
        return whitelist;
    }


    @Override
    public boolean hasNext() {
        return pmcFiles.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(completed, -1, "documents")};
    }

    @Override
    public void close() {
        pmcFiles = null;
    }
}
