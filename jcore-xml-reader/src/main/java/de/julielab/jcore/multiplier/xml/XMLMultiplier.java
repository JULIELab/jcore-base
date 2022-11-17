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

package de.julielab.jcore.multiplier.xml;

import de.julielab.java.utilities.UriUtilities;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.casmultiplier.JCoReURI;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

@ResourceMetaData(name="JCoRe XML Multiplier")
@OperationalProperties(outputsNewCases = true, modifiesCas = false)
public class XMLMultiplier extends JCasMultiplier_ImplBase {

	/**
	 * UIMA Logger for this class
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(XMLMultiplier.class);
	
	/**
	 * File directory passed from the Collection Reader as feature from the CAS
	 */
	String fileToRead;

	/**
	 * Configuration parameter defined in the descriptor
	 */
	public static final String PARAM_MAPPING_FILE = "MappingFile";

	/**
	 * Configuration parameter defined in the descriptor. Defaults to
	 * "de.julielab.jcore.types.Header". Must be assignment compatible to
	 * "de.julielab.jcore.types.Header". Only required when no header is built
	 * by the XML mapper.
	 */
	public static final String PARAM_HEADER_TYPE = "HeaderType";
	
	/**
	 * Configuration parameter defined in the descriptor. The Xpath defines 
	 * in what XML fractions the documents shall be splitted. 
	 */
	public static final String PARAM_FOR_EACH = "DocumentXpath";

	/**
	 * List of all files with abstracts XML
	 */
	private File[] files;

	/**
	 * Current currentUri number
	 */
	private int currentIndex = 1;

	/**
	 * Mapper which maps XML to a cas with the jules type system via an XML
	 * configuration currentUri.
	 */
	private XMLMapper xmlMapper;
	
	/**
	 * Detects the XML fractions 
	 */
	private Iterator<Map<String, Object>> rowIterator;

	/**
	 * File to read passed from the Collection Reader CAS
	 */
	private String currentUri;

	/**
	 * Passes parameters to initialize() 
	 */
	@org.apache.uima.fit.descriptor.ConfigurationParameter(name = PARAM_MAPPING_FILE)
	private String mappingFileStr;
	@org.apache.uima.fit.descriptor.ConfigurationParameter(name = PARAM_FOR_EACH)
	private String forEach;

	@org.apache.uima.fit.descriptor.ConfigurationParameter(name = PARAM_HEADER_TYPE, mandatory = false)
	private String headerTypeName;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.debug("initialize() - Initializing XML Multiplier...");

		super.initialize(aContext);
		
		headerTypeName = (String) aContext.getConfigParameterValue(PARAM_HEADER_TYPE);
		if (null == headerTypeName)
			headerTypeName = "de.julielab.jcore.types.Header";
		mappingFileStr = (String) aContext.getConfigParameterValue(PARAM_MAPPING_FILE);
		forEach = (String) aContext.getConfigParameterValue(PARAM_FOR_EACH);
		InputStream is = null;

		LOGGER.info("Header type set to {}. A header of this type is only created if no header is created using the XML mapping currentUri.", headerTypeName);
		LOGGER.info("Mapping file is searched as file or classpath resource at {}", mappingFileStr);

		File mappingFile = new File(mappingFileStr);
		if (mappingFile.exists()) {
			try {
				is = new FileInputStream(mappingFile);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				throw new ResourceInitializationException(e1);
			}
		} else {
			if (!mappingFileStr.startsWith("/"))
				mappingFileStr = "/" + mappingFileStr;

			is = getClass().getResourceAsStream(mappingFileStr);
			if (is == null) {
				throw new IllegalArgumentException(
						"MappingFile "
								+ mappingFileStr
								+ " could not be found as a file or on the classpath (note that the prefixing '/' is added automatically if not already present for classpath lookup)");
			}
		}

		try {
			xmlMapper = new XMLMapper(is);
		} catch (FileNotFoundException e) {
			throw new ResourceInitializationException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {

		JCoReURI xmlFile = JCasUtil.selectSingle(cas, JCoReURI.class);
		currentUri = xmlFile.getUri();
        currentIndex = 1;
		LOGGER.debug("Reading file " + currentUri);
		String[] fieldPaths = new String [1];
		fieldPaths[0] = ".";
		List<Map<String, String>> fields = new ArrayList<>();
		for (int i = 0; i < fieldPaths.length; i++) {
			String path = fieldPaths[i];
			Map<String, String> field = new HashMap<String, String>();
			field.put(JulieXMLConstants.NAME, "fieldvalue" + i);
			field.put(JulieXMLConstants.XPATH, path);
			field.put(JulieXMLConstants.RETURN_XML_FRAGMENT, "true");
			fields.add(field);
		};
        try {
            rowIterator = JulieXMLTools.constructRowIterator(
                    JulieXMLTools.readStream(UriUtilities.getInputStreamFromUri(new java.net.URI(currentUri)), 1024),
                    1024, forEach, fields, currentUri, true);
        } catch (IOException | URISyntaxException e) {
            throw new AnalysisEngineProcessException(e);
        }

    }
	
	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas cas = getEmptyJCas();
		
		Map<String, Object> row = rowIterator.next();
		String xmlFragment = (String) row.get("fieldvalue" + 0);
		String identifier = currentUri + "#" + currentIndex++;
		try {
			xmlMapper.parse(xmlFragment.getBytes(), identifier.getBytes(), cas);
		} catch (Exception e) {
			LOGGER.error("Exception in next(): currentUri: " + currentUri, e);
			throw new AnalysisEngineProcessException(e);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			checkHeader(currentUri, cas);
		} catch (CASException e) {
			LOGGER.error("Exception in next(): currentUri: " + currentUri, e);
			throw new AnalysisEngineProcessException(e);
		}
		return cas;
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		if (rowIterator.hasNext()) {
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private void checkHeader(String name, JCas cas) throws CASException {
		Type headerType = cas.getTypeSystem().getType(headerTypeName);
		if (null == headerType)
			throw new CASException(CASException.JCAS_INIT_ERROR, new Object[] { "Header type \"" + headerTypeName
					+ "\" could not be found in the type system." });
		FSIterator<Annotation> headerIter = cas.getAnnotationIndex(headerType).iterator();
		if (headerIter.hasNext()) {
			try {
				Header header = (Header) headerIter.next();
				if (header.getDocId() == null || header.getDocId() == "" || header.getDocId() == "-1") {
					header.setDocId(name);
				}
			} catch (ClassCastException e) {
				LOGGER.error("Configured header type is {}. However, the header type must be assignment compatible to de.julielab.jcore.types.Header.",
						headerTypeName);
				throw new CASException(e);
			}

		} else {
			Class<? extends Header> headerClass;
			try {
				headerClass = (Class<? extends Header>) Class.forName(headerTypeName);
			} catch (ClassNotFoundException e) {
				LOGGER.error("Header type class {} could not be found.", headerTypeName);
				throw new CASException(e);
			}
			Header header = AnnotationFactory.createAnnotation(cas, 0, 0, headerClass);
			header.setDocId(name);
			header.addToIndexes();
		}

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
	public void close() throws IOException {
	}
}
