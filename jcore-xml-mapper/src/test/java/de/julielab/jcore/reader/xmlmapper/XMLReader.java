/** 
 * XMLReader.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: muehlhausen
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 28.10.2008 
 **/
package de.julielab.jcore.reader.xmlmapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.xml.JulieXMLTools;

/**
 * Generic XML {@link CollectionReader}. Uses a mapping file to map elements of the XML document to
 * type system objects.
 * 
 * @author muehlhausen, weigel
 */
public class XMLReader extends CollectionReader_ImplBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLReader.class);
	public static final String PARAM_INPUT_DIR = "InputDirectory";
	public static final String PARAM_INPUT_FILE = "InputFile";
	public static final String RESOURCE_MAPPING_FILE = "MappingFile";
	private List<File> files = null;
	private int currentIndex = 0;

	/**
	 * The tested component
	 */
	private XMLMapper xmlMapper;
	
	@Override
	public void initialize() throws ResourceInitializationException {
		super.initialize();
		
		String inputDir = (String) getUimaContext().getConfigParameterValue(PARAM_INPUT_DIR);
		String inputFile = (String) getUimaContext().getConfigParameterValue(PARAM_INPUT_FILE);
		InputStream is = null;
		try {
			is = getUimaContext().getResourceAsStream(RESOURCE_MAPPING_FILE);
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
		String mappingFile = (String) getUimaContext().getConfigParameterValue(RESOURCE_MAPPING_FILE);
		if (is == null) {
			try {
				is = new FileInputStream(new File(mappingFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (is == null) {
			if (!mappingFile.startsWith("/"))
				mappingFile = "/" + mappingFile;
			is = getClass().getResourceAsStream(mappingFile);
		}
		if (is == null) {
			throw new ResourceInitializationException(ResourceInitializationException.RESOURCE_DEPENDENCY_NOT_SATISFIED,
							new Object[] { RESOURCE_MAPPING_FILE });
		}
		if ((inputDir != null) && (inputFile != null)) {
			LOGGER.error("You can't define both parameters: " + PARAM_INPUT_DIR + " and " + PARAM_INPUT_FILE);
			throw new ResourceInitializationException(ResourceInitializationException.INCORRECT_NUMBER_OF_PARAMETERS,
							new Object[] { 2 });
		}
		if ((inputDir == null) && (inputFile == null)) {
			LOGGER.error("You must define one of this parameters: " + PARAM_INPUT_DIR + " or " + PARAM_INPUT_FILE);
			throw new ResourceInitializationException(ResourceInitializationException.INCORRECT_NUMBER_OF_PARAMETERS,
							new Object[] { 2 });
		}
		if (inputDir != null) {
			files = getInputStreamsFromDirectory(inputDir);
		}
		if (inputFile != null) {
			files = getInputStreamsFromFile(inputFile);
		}
		
		try {
			xmlMapper = new XMLMapper(JulieXMLTools.readStream(is, 1000));
		} catch (FileNotFoundException e) {
			throw new ResourceInitializationException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void getNext(CAS cas) throws CollectionException {
		JCas jcas;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			LOGGER.error(e.getMessage(), e);
			throw new CollectionException(e);
		}
		File file = files.get(currentIndex++);
		LOGGER.debug("CollectionReader will now process file " + file.toString());
		try {
			xmlMapper.parse(file, jcas);
		} catch (Throwable e) {
			LOGGER.error("error while parsing "+ file.toString(),e);
		}
		LOGGER.debug("Document " + currentIndex + " was parsed.");
	}

	private List<File> getInputStreamsFromFile(String inputFileName) throws ResourceInitializationException {
		assert inputFileName != null;
		List<File> files = new ArrayList<File>();
		File inputFile = new File(inputFileName);
		if (!inputFile.isFile()) {
			LOGGER.error("inputFile is not a file");
			throw new ResourceInitializationException(ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
							new Object[] { inputFile.getAbsolutePath() });
		}
		files.add(inputFile);
		return files;
	}

	private List<File> getInputStreamsFromDirectory(String inputDirName) throws ResourceInitializationException {
		assert inputDirName != null;
		List<File> streams = new ArrayList<File>();
		File inputDir = new File(inputDirName);
		if (!inputDir.isDirectory()) {
			LOGGER.error("inputDirectory is not a directory");
			throw new ResourceInitializationException(ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
							new Object[] { inputDir.getAbsolutePath() });
		}
		File[] allFiles = inputDir.listFiles();
		for (int i = 0; i < allFiles.length; i++) {
			if (allFiles[i].isFile()) {
				streams.add(allFiles[i]);
			}
		}
		return streams;
	}

	public void close() throws IOException {
	}

	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(currentIndex, files.size(), Progress.ENTITIES) };
	}

	public boolean hasNext() throws IOException, CollectionException {
		return currentIndex < files.size();
	}

}
