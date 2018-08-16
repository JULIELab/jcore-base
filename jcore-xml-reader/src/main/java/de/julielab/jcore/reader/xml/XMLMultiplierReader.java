/**
 * MedlineReader.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: muehlhausen
 * <p>
 * Current version: 1.12
 * Since version:   1.0
 * <p>
 * Creation date: Dec 11, 2006
 * <p>
 * CollectionReader for MEDLINE (www.pubmed.gov) abstracts in XML that initializes some information in the CAS.
 **/

package de.julielab.jcore.reader.xml;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.ducc.Workitem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@ResourceMetaData(name = "JCoRe XML Multiplier Reader", description = "Reads Medline/PubMed XML blobs as downloaded " +
        "from the NCBI FTP. Each blob is one large XML file containing a PubmedArticleSet. This component is UIMA DUCC " +
        "compatible and forwards the work item CAS to the CAS consumer in order to indicate the finishing " +
        "of the current XML blob. It also sets the work item feature 'lastBlock' to true if there are not more " +
        "work items and, thus, the processing comes to an end.")
public class XMLMultiplierReader extends CollectionReader_ImplBase {

    /**
     * Configuration parameter defined in the descriptor. Name of configuration
     * parameter that must be set to the path of a directory containing input
     * inputUris.
     */
    public static final String PARAM_INPUT_DIR = "InputDirectory";
    /**
     * Configuration parameter defined in the descriptor
     */
    public static final String PARAM_INPUT_FILE = "InputFile";
    public static final String PARAM_FILE_NAME_REGEX = "FileNameRegex";
    public static final String PARAM_SEARCH_IN_ZIP = "SearchInZipFiles";
    public static final String PARAM_SEND_CAS_TO_LAST = "SendCasToLast";
    /**
     * UIMA Logger for this class
     */
    private static Logger LOGGER = LoggerFactory.getLogger(XMLMultiplierReader.class);
    /**
     * List of all inputUris with abstracts XML
     */
    private Deque<URI> inputUris;

    /**
     * Current file number
     */
    private int currentIndex = 0;

    @ConfigurationParameter(name = PARAM_INPUT_DIR, mandatory = false)
    private String directoryName;
    @ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = false)
    private String isSingleFileProcessing;
    @ConfigurationParameter(name = PARAM_FILE_NAME_REGEX, description = "If a directory is given, all inputUris with a name matching " +
            "one of these regular expressions will be read, others will be discarded. Defaults to {'.*\\.xml', '.*\\.xml.gz'}.", defaultValue = {".*\\.xml", ".*\\xml\\.gz"})
    private String[] fileNameRegex = new String[]{".*\\.xml", ".*\\.xml.gz"};
    @ConfigurationParameter(name = PARAM_SEARCH_IN_ZIP, mandatory = false, description = "If set to true, contents of ZIP files in the " +
            "given input directory will also be searched for files matching the specified file name regular expression. Defaults to false.", defaultValue = "false")
    private boolean searchZip;
    @ConfigurationParameter(name = PARAM_SEND_CAS_TO_LAST, mandatory = false, defaultValue = "false", description = "UIMA DUCC relevant parameter when using a CAS multiplier. When set to true, the worker CAS from the collection reader is forwarded to the last component in the pipeline. This can be used to send information about the progress to the CAS consumer in order to have it perform batch operations. For this purpose, a feature structure of type WorkItem from the DUCC library is added to the worker CAS. This feature structure has information about the current progress.")
    private boolean sendCasToLast;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize() throws ResourceInitializationException {
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Component configuration:");
                for (String name : getUimaContext().getConfigParameterNames())
                    LOGGER.info("{}: {}", name, getConfigParameterValue(name));
            }
            sendCasToLast = (boolean) Optional.ofNullable(getConfigParameterValue(PARAM_SEND_CAS_TO_LAST)).orElse(false);
            getInputFiles();
        } catch (Throwable e) {
            LOGGER.error("Exception or error while initializing reader: ", e);
            throw e;
        }
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(CAS)
     */
    public void getNext(CAS cas) throws CollectionException {
        try {
            URI uri = inputUris.removeFirst();

            LOGGER.debug("Reading URI " + uri.toString());

            try {
                JCoReURI fileType = new JCoReURI(cas.getJCas());
                fileType.setUri(uri.toString());
                fileType.addToIndexes();
            } catch (Exception e) {
                LOGGER.error("Exception with URI: " + uri.toString(), e);
                throw new CollectionException(e);
            }

            if (sendCasToLast) {
                Workitem workitem = new Workitem(cas.getJCas());
                // Send the work item CAS also to the consumer. Normally, only the CASes emitted by the CAS multiplier
                // will be routed to the consumer. We do this to let the consumer know that the work item has been
                // finished.
                workitem.setSendToLast(true);
                workitem.setBlockindex(currentIndex);
                if (!hasNext())
                    workitem.setLastBlock(true);
                workitem.addToIndexes();
            }

            currentIndex++;
        } catch (CASException e) {
            LOGGER.error("Could not get the JCAS from the CAS: ", e);
            throw new CollectionException(e);
        } catch (Throwable e) {
            LOGGER.warn("Exception or error while filling CAS: ", e);
            throw e;
        }
    }

    /**
     * Get inputUris from directory that is specified in the configuration parameter
     * PARAM_INPUTDIR of the collection reader descriptor.
     *
     * @throws ResourceInitializationException thrown if there is a problem with a configuration parameter
     */
    private void getInputFiles() throws ResourceInitializationException {
        inputUris = new ArrayDeque<>();
        currentIndex = 0;
        if (isSingleProcessing()) {
            getSingleFile();
            return;
        }
        directoryName = (String) getConfigParameterValue(PARAM_INPUT_DIR);
        if (directoryName == null) {
            throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[]{"null", PARAM_INPUT_DIR});
        }
        if (getConfigParameterValue(PARAM_FILE_NAME_REGEX) != null)
            fileNameRegex = (String[]) getConfigParameterValue(PARAM_FILE_NAME_REGEX);
        searchZip = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_SEARCH_IN_ZIP)).orElse(false);
        File inputDirectory = new File(directoryName.trim());
        if (!inputDirectory.exists())
            throw new ResourceInitializationException(new FileNotFoundException("The directory " + inputDirectory.getAbsolutePath() + " does not exist."));
        else if (!inputDirectory.isDirectory())
            throw new ResourceInitializationException(new IllegalArgumentException("The file " + inputDirectory.getAbsolutePath() + " is not a directory."));
        for (File f : inputDirectory.listFiles((dir, name) -> matchesFileNameRegex(name))) {
            URI uri = f.toURI();
            if (uri.toString().toLowerCase().endsWith(".zip")) {
                LOGGER.debug("Searching ZIP archive {} for eligible documents", uri);
                try (FileSystem fs = FileSystems.newFileSystem(Paths.get(uri), null)) {
                    Iterable<Path> rootDirectories = fs.getRootDirectories();
                    for (Path rootDir : rootDirectories) {
                        Stream<Path> walk = Files.walk(rootDir);
                        walk.filter(Files::isRegularFile).forEach(p -> {
                                    LOGGER.trace("Current ZIP archive entry: {}", p.toString());
                                    if (matchesFileNameRegex(p.getFileName().toString())) {
                                        inputUris.push(p.toUri());
                                    }
                                }
                        );
                    }
                } catch (IOException e) {
                    LOGGER.error("Could not read from {}", uri);
                    throw new ResourceInitializationException(e);
                }
            } else {
                inputUris.push(uri);
            }
        }

        LOGGER.debug("Found {} input files.", inputUris.size());
    }

    private boolean matchesFileNameRegex(String name) {
        for (String regex : fileNameRegex)
            if (name.matches(regex) || (searchZip && name.toLowerCase().endsWith("zip"))) return true;
        return false;
    }

    /**
     * Get a list of the single file defined in the descriptor
     *
     * @return
     * @throws ResourceInitializationException
     */
    private void getSingleFile() throws ResourceInitializationException {

        LOGGER.info("XML reader is used in SINGLE FILE mode.");
        String singleFile = (String) getConfigParameterValue(PARAM_INPUT_FILE);

        if (singleFile == null) {
            return;
        }
        File file = new File(singleFile.trim());
        if (!file.exists() || file.isDirectory()) {
            throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
                    new Object[]{"file does not exist or is a directory" + PARAM_INPUT_FILE});
        }
        inputUris.push(file.toURI());
    }

    /**
     * Determines form the descriptor if this CollectionReader should only
     * process a single file. . <b>The parameter must not have a value! It is
     * sufficient to be defined to return <code>true</code></b>
     *
     * @return <code>true</code> if there is a parameter defined called like the
     * value of PARAM_INPUT_FILE
     */
    private boolean isSingleProcessing() {

        Object value = getConfigParameterValue(PARAM_INPUT_FILE);
        if (null != value)
            isSingleFileProcessing = (String) value;
        return isSingleFileProcessing != null;
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    public boolean hasNext() {
        return !inputUris.isEmpty();
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentIndex, inputUris.size(), Progress.ENTITIES)};
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() {
    }
}
