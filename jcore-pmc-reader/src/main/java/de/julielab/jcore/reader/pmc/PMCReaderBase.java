package de.julielab.jcore.reader.pmc;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PMCReaderBase extends JCasCollectionReader_ImplBase {
    public static final String PARAM_INPUT = "Input";
    public static final String PARAM_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_SEARCH_ZIP = "SearchInZipFiles";
    public static final String PARAM_WHITELIST = "WhitelistFile";
    public static final String PARAM_EXTRACT_ID_FROM_FILENAME = "ExtractIdFromFilename";
    public static final String PARAM_OMIT_BIB_REFERENCES = "OmitBibliographyReferences";
    private final static Logger log = LoggerFactory.getLogger(PMCReaderBase.class);
    @ConfigurationParameter(name = PARAM_INPUT, description = "The path to an NXML file or a directory with NXML files and possibly subdirectories holding more NXML files.")
    protected File input;

    @ConfigurationParameter(name = PARAM_RECURSIVELY, defaultValue = "false", mandatory = false, description = "If set to true, subdirectories of the given input directory " + PARAM_INPUT + " are also searched for NXML files. Defaults to false.")
    protected boolean searchRecursively;

    @ConfigurationParameter(name = PARAM_SEARCH_ZIP, defaultValue = "false", mandatory = false, description = "If set to true, ZIP files found among the input are opened and also searched for NXML files. Defaults to false.")
    protected boolean searchZip;

    @ConfigurationParameter(name = PARAM_WHITELIST, mandatory = false, description = "A file listing the file names that should be read. All other files will be discarded. The file name must be given without any extensions and subdirectories. For example, the file \"Neural_Regen_Res/PMC2847692.nxml.gz\" would be represented as \"PMC2847692\" in the whitelist file. Each file name must appear on a line of its own. An empty file will cause nothing to be read. A file containing only the keyword \"all\" will behave as if no file was given at all.")
    protected File whitelistFile;

    @ConfigurationParameter(name = PARAM_EXTRACT_ID_FROM_FILENAME, mandatory = false, description = "Used for NXML documents that carry their ID in the file name but not in the document itself. Extracts the string after the last path separator and the first dot after the separator and sets it to the docId feature of the Header annotation.")
    protected boolean extractIdFromFilename;

    @ConfigurationParameter(name = PARAM_OMIT_BIB_REFERENCES, mandatory = false, defaultValue = "false", description = "If set to true, references to the bibliography are omitted from the CAS text.")
    protected boolean omitBibReferences;

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
        omitBibReferences = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_OMIT_BIB_REFERENCES)).orElse(false);
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
        if (whitelistFile == null || !whitelistFile.exists()) {
            whitelist.add("all");
        } else {
            try (BufferedReader br = Files.newBufferedReader(whitelistFile.toPath(), StandardCharsets.UTF_8)) {
                whitelist = br.lines().filter(l -> !StringUtils.isBlank(l)).collect(Collectors.toSet());
            }
            log.debug("Read whitelist with {} entries from {}", whitelist.size(), whitelistFile);
        }
        return whitelist;
    }

    protected String getIdFromFilename(URI uri) {
        String uriString = uri.toString();
        int lastSlash = uriString.lastIndexOf('/');
        int firstDotAfterSlash = uriString.indexOf('.', lastSlash);
        if (lastSlash < 0)
            lastSlash = 0;
        return uriString.substring(lastSlash + 1, firstDotAfterSlash);
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
