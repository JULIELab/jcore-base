/** 
 * MedlineReader.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 * 
 * Author: muehlhausen
 * 
 * Current version: 1.12
 * Since version:   1.0
 *
 * Creation date: Dec 11, 2006 
 * 
 * CollectionReader for MEDLINE (www.pubmed.gov) abstracts in XML that initializes some information in the CAS.
 **/

package de.julielab.jcore.reader.xml;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class XMLMultiplierReader extends CollectionReader_ImplBase {

	/**
	 * UIMA Logger for this class
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(XMLMultiplierReader.class);

	/**
	 * Configuration parameter defined in the descriptor. Name of configuration
	 * parameter that must be set to the path of a directory containing input
	 * files.
	 */
	public static final String PARAM_INPUT_DIR = "InputDirectory";

	/**
	 * Configuration parameter defined in the descriptor
	 */
	public static final String PARAM_INPUT_FILE = "InputFile";

	/**
	 * List of all files with abstracts XML
	 */
	private File[] files;

	/**
	 * Current file number
	 */
	private int currentIndex = 0;

	@ConfigurationParameter(name = PARAM_INPUT_DIR, mandatory = false)
	private String directoryName;
	@ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = false)
	private String isSingleFileProcessing;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {
		files = getFilesFromInputDirectory();
	}

	/**
	 * @see org.apache.uima.collection.CollectionReader#getNext(CAS)
	 */
	public void getNext(CAS cas) throws CollectionException {

		File file = files[currentIndex++];

		LOGGER.debug("getNext(CAS) - Reading file " + file.getName());

		try {
			JCoReURI fileType = new JCoReURI(cas.getJCas());
        	fileType.setUri(file.toURI().toString());
        	fileType.addToIndexes();
		} catch (Exception e) {
			LOGGER.error("Exception in getNext(): file: " + file, e);
			throw new CollectionException(e);
		} catch (Throwable e) {
			throw new CollectionException(e);
		}
	}

		/**
	 * Get files from directory that is specified in the configuration parameter
	 * PARAM_INPUTDIR of the collection reader descriptor.
	 * 
	 * @throws ResourceInitializationException
	 *             thrown if there is a problem with a configuration parameter
	 */
	private File[] getFilesFromInputDirectory() throws ResourceInitializationException {

		currentIndex = 0;
		if (isSingleProcessing()) {
			return getSingleFile();
		}
		directoryName = (String) getConfigParameterValue(PARAM_INPUT_DIR);
		LOGGER.debug(PARAM_INPUT_DIR + "=" + directoryName);
		if (directoryName == null) {
			throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[] { "null", PARAM_INPUT_DIR });
		}
		File inputDirectory = new File(directoryName.trim());
		if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
			throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID, new Object[] { directoryName, PARAM_INPUT_DIR });
		}
		return inputDirectory.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml") || name.endsWith(".xml.gz");
			}
		});
	}

	/**
	 * Get a list of the single file defined in the descriptor
	 * 
	 * @return
	 * @throws ResourceInitializationException
	 */
	private File[] getSingleFile() throws ResourceInitializationException {

		LOGGER.info("getSingleFile() - MedlineReader is used in SINGLE FILE mode.");
		String singleFile = (String) getConfigParameterValue(PARAM_INPUT_FILE);

		if (singleFile == null) {
			return null;
		}
		File file = new File(singleFile.trim());
		if (!file.exists() || file.isDirectory()) {
			throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
					new Object[] { "file does not exist or is a directory" + PARAM_INPUT_FILE });
		}
		File[] fileList = new File[1];
		fileList[0] = file;
		return fileList;
	}

	/**
	 * Determines form the descriptor if this CollectionReader should only
	 * process a single file. . <b>The parameter must not have a value! It is
	 * sufficient to be defined to return <code>true</code></b>
	 * 
	 * @return <code>true</code> if there is a parameter defined called like the
	 *         value of PARAM_INPUT_FILE
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
		return currentIndex < files.length;
	}

	/**
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	 */
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(currentIndex, files.length, Progress.ENTITIES) };
	}

	/**
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
	 */
	public void close() {
	}
}
