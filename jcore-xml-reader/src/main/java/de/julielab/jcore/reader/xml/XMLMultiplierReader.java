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
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@ResourceMetaData(name = "JCoRe XML Multiplier Reader")
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
    @ConfigurationParameter(name = PARAM_FILE_NAME_REGEX, description = "If a directory is given, all inputUris with a name matching" +
            "one of these regular expressions will be read, others will be discarded. Defaults to {'.*\\.xml', '.*\\.xml.gz'}.", defaultValue = {".xml", ".xml.gz"})
    private String[] fileNameRegex = new String[]{".*\\.xml", ".*\\.xml.gz"};
    @ConfigurationParameter(name = PARAM_SEARCH_IN_ZIP, mandatory = false, description = "If set to true, contents of ZIP files in the " +
            "given input directory will also be searched for files matching the specified file name regular expression. Defaults to false.", defaultValue = "false")
    private boolean searchZip;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize() throws ResourceInitializationException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Component configuration:");
            for (String name : getUimaContext().getConfigParameterNames())
                LOGGER.info("{}: {}", name, getConfigParameterValue(name));
        }
        getInputFiles();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(CAS)
     */
    public void getNext(CAS cas) throws CollectionException {


        URI uri = inputUris.pop();
        while (uri.toString().endsWith(".zip")) {
            try (FileSystem fs = FileSystems.newFileSystem(Paths.get(uri), null)) {
                Iterable<Path> rootDirectories = fs.getRootDirectories();
                for (Path rootDir : rootDirectories) {
                    Stream<Path> walk = Files.walk(rootDir);
                    walk.filter(Files::isRegularFile).forEach(p -> {
                                if (matchesFileNameRegex(p.getFileName().toString())) {
                                    inputUris.push(p.toUri());
                                }
                            }
                    );
                }
            } catch (IOException e) {
                LOGGER.error("Could not read from {}", uri);
                throw new CollectionException(e);
            }
            uri = inputUris.pop();
        }

        LOGGER.debug("Reading URI " + uri.toString());

        try {
            JCoReURI fileType = new JCoReURI(cas.getJCas());
            fileType.setUri(uri.toString());
            fileType.addToIndexes();
        } catch (Exception e) {
            LOGGER.error("Exception with URI: " + uri.toString(), e);
            throw new CollectionException(e);
        } catch (Throwable e) {
            throw new CollectionException(e);
        }
    }

    /**
     * Get inputUris from directory that is specified in the configuration parameter
     * PARAM_INPUTDIR of the collection reader descriptor.
     *
     * @throws ResourceInitializationException thrown if there is a problem with a configuration parameter
     */
    private void getInputFiles() throws ResourceInitializationException {

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
        if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
            throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[]{directoryName, PARAM_INPUT_DIR});
        }
        inputUris = new ArrayDeque<>();
        Stream.of(inputDirectory.listFiles((dir, name) -> matchesFileNameRegex(name))).map(File::toURI).forEach(inputUris::push);
    }

    private boolean matchesFileNameRegex(String name) {
        for (String regex : fileNameRegex) if (name.matches(regex) || (searchZip && name.toLowerCase().endsWith("zip"))) return true;
        return false;
    }

    /**
     * Get a list of the single file defined in the descriptor
     *
     * @return
     * @throws ResourceInitializationException
     */
    private void getSingleFile() throws ResourceInitializationException {

        LOGGER.info("getSingleFile() - MedlineReader is used in SINGLE FILE mode.");
        String singleFile = (String) getConfigParameterValue(PARAM_INPUT_FILE);

        if (singleFile == null) {
            return;
        }
        File file = new File(singleFile.trim());
        if (!file.exists() || file.isDirectory()) {
            throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
                    new Object[]{"file does not exist or is a directory" + PARAM_INPUT_FILE});
        }
        inputUris = new ArrayDeque<>();
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
