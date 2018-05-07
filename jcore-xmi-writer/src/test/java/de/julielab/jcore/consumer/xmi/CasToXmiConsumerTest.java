/** 
 * CasToXmiConsumerTest.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: muehlhausen
 * 
 * Current version: 1.1
 * Since version:   1.1
 *
 * Creation date: 11.03.2008 
 **/
package de.julielab.jcore.consumer.xmi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

import de.julielab.jcore.consumer.xmi.CasToXmiConsumer;
import de.julielab.jcore.types.Header;

/**
 * Test for class {@link CasToXmiConsumer}
 * 
 * @author muehlhausen
 */
public class CasToXmiConsumerTest {

	private static final String OUTPUT_FOLDER_XMI = "src/test/resources/output-xmi";
	private static final int NUM_EXPECTED_FILES = 3;
	private static final String DESC_CAS_TO_XMI_CONSUMER = "src/test/resources/de/julielab/jcore/consumer/xmi/CasToXmiConsumer.xml";
	private static final String PARAM_OUTPUTDIR = "OutputDirectory";
	private static final String PARAM_CREATE_BATCH_SUBDIRS = "CreateBatchSubDirs";
	private static final String PARAM_COMPRESS = "Compress";
	private static final String PARAM_COMPRESS_SINGLE = "CompressSingle";
	private static final String PARAM_FILE_NAME_TYPE = "FileNameType";
	private static final String PARAM_FILE_NAME_FEATURE = "FileNameFeature";
	private static final String XMI_EXTENSION = ".xmi";
	private static final String GZIP_EXTENSION = ".gz";
	private static final String TEST_TEXT = "Hallo & " + Math.random();

	private final class XMIFilter implements FilenameFilter {
		public boolean accept(File file, String name) {
			if (name.endsWith("xmi")) {
				return true;
			}
			return false;
		}
	}

	private final class XMIGzipFilter implements FilenameFilter {
		public boolean accept(File file, String name) {
			if (name.endsWith("xmi.gz")) {
				return true;
			}
			return false;
		}
	}
	
	private final class XMIZipFilter implements FilenameFilter {
		public boolean accept(File file, String name) {
			if (name.endsWith(".zip")) {
				return true;
			}
			return false;
		}
	}


	/**
	 * Object under test
	 */
	private AnalysisEngine consumer;

	/**
	 * Delete all files ending with "xmi" or "xmi.gzip" in the output directory, 
	 * and do the same for all subdirectories of outputDir, recursively
	 */
	@Before	
	public void clearDirectory() {
		File outputDir = new File(OUTPUT_FOLDER_XMI);
		removeXmiGzipAndZipFiles(outputDir);
		for (File file : outputDir.listFiles()){
			if (file.isDirectory()){
				removeXmiGzipAndZipFiles(file);
				file.delete();
			}
		}
	}
	
	private void removeXmiGzipAndZipFiles(File dir) {
		File[] xmiFiles = dir.listFiles(new XMIFilter());
		for (File file : xmiFiles) {
			file.delete();
		}
		File[] xmiGzipFiles = dir.listFiles(new XMIGzipFilter());
		for (File file : xmiGzipFiles) {
			file.delete();
		}
		File[] xmiZipFiles = dir.listFiles(new XMIZipFilter());
		for (File file : xmiZipFiles) {
			file.delete();
		}
	}

	/**
	 * Create the CasConsumer under test
	 */
	@Before	
	public void createConsumer() {
//		XMLInputSource source;
		try {
//			source = new XMLInputSource(DESC_CAS_TO_XMI_CONSUMER);
//			ResourceSpecifier resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(source);
			consumer = AnalysisEngineFactory.createEngine(CasToXmiConsumer.class,
					CasToXmiConsumer.PARAM_OUTPUTDIR, "src/test/resources/output-xmi");
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return a CAS conforming to the typesystem of the CasConsumer under test
	 */
	private CAS createCas() {
		CAS cas = null;
		try {
			cas = CasCreationUtils.createCas(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
					new XMLInputSource(DESC_CAS_TO_XMI_CONSUMER)));
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		} catch (InvalidXMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cas;
	}

	/**
	 * Check number of files with xmi ending that was written by the {@link CasToXmiConsumer}
	 * @throws Exception
	 */
	@Test 
	public void testProcessCas() throws Exception {
		CAS cas = createCas();
		cas.setDocumentText(TEST_TEXT);
		JCas jcas = cas.getJCas();
		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
			consumer.process(jcas);
		}
		File outputDir = new File(OUTPUT_FOLDER_XMI);
		String[] list = outputDir.list(new XMIFilter());
		int numberOfFiles = list.length;
		assertEquals(NUM_EXPECTED_FILES, numberOfFiles);
		
		//check if subdirectories with XMIs are created if batchProcessComplete is called
//		consumer. setConfigParameterValue(PARAM_CREATE_BATCH_SUBDIRS, true);
//		consumer.reconfigure();
//		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
//			consumer.process(jcas);
//		}
//		consumer.batchProcessComplete(null);
//		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
//			consumer.process(jcas);
//		}
//		List<File> dirList = new ArrayList<File>();
//		for (File file : outputDir.listFiles()){
//			if (file.isDirectory() && !file.getName().equals(".svn")){
//				dirList.add(file);
//			}
//		}
//		assertEquals(2, dirList.size());
//		assertEquals(NUM_EXPECTED_FILES, dirList.get(0).list(new XMIFilter()).length);
//		assertEquals(NUM_EXPECTED_FILES, dirList.get(1).list(new XMIFilter()).length);
	}

	/**
	 * Sets configuration parameter Compress to true and checks if file with correct ending
	 * is written by the {@link CasToXmiConsumer}
	 * @throws Exception
	 */
	@Test 
	public void testProcessCasWithCompressTrue() throws Exception {
//		consumer.setConfigParameterValue(PARAM_COMPRESS, true);
//		consumer.reconfigure();
//		CAS cas = createCas();
//		cas.setDocumentText(TEST_TEXT);
//		consumer.processCas(cas);
//		File expectedXMI = new File(OUTPUT_FOLDER_XMI + "/1" + XMI_EXTENSION + GZIP_EXTENSION); 
//		assertTrue(expectedXMI.exists());
	}
	
	/**
	 * Check number of files with zip ending that were written by the {@link CasToXmiConsumer}
	 * @throws Exception
	 */
	@Test 
	public void testProcessCasWithCompressSingleTrue() throws Exception {
//		consumer.setConfigParameterValue(PARAM_COMPRESS_SINGLE, true);
//		consumer.setConfigParameterValue(PARAM_COMPRESS, true);	//only to check if param really not considered
//		consumer.reconfigure();
//		CAS cas = createCas();
//		cas.setDocumentText(TEST_TEXT);
//		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
//			consumer.processCas(cas);
//		}
//		File outputDir = new File(OUTPUT_FOLDER_XMI);
//		String[] list = outputDir.list(new XMIZipFilter());
//		assertEquals(1, list.length);
//		clearDirectory();
//		
//		//check if multiple files are created if batchProcessComplete is called
//		consumer.setConfigParameterValue(PARAM_CREATE_BATCH_SUBDIRS, true);
//		consumer.reconfigure();
//		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
//			consumer.processCas(cas);
//		}
//		consumer.batchProcessComplete(null);
//		for (int i = 0; i < NUM_EXPECTED_FILES; i++) {
//			consumer.processCas(cas);
//		}
//		list = outputDir.list(new XMIZipFilter());
//		assertEquals(2, list.length);
	}


	/**
	 * Adds Header.source annotation to the CAS and checks if file with value of Header.source 
	 * annotation is written by the {@link CasToXmiConsumer}. Expects the configuration parameters
	 * FileNameType and FileNameFeature not to be set. 
	 * @throws Exception
	 */
	@Test 
	public void testProcessCasWithHeaderSource() throws Exception {
		String sourceValue = "test_1";
		CAS cas = createCas();
		JCas jcas = cas.getJCas();
		jcas.setDocumentText(TEST_TEXT);
		Header header = new Header(jcas);
		header.setDocId(sourceValue);
		header.addToIndexes();
		consumer.process(cas);
		File expectedXMI = new File(OUTPUT_FOLDER_XMI + "/" + sourceValue + XMI_EXTENSION); 
		assertTrue(expectedXMI.exists());
	}

	/**
	 * Check file names of files written by the {@link CasToXmiConsumer} when FileNameType 
	 * and FileNameFeature were set and are present as annotations.
	 * @throws Exception
	 */
	@Test 
	public void testProcessCasWithCustomFileNameSource() throws Exception {
//		String idValue = "001";
//		consumer.setConfigParameterValue(PARAM_FILE_NAME_TYPE, "de.julielab.jcore.types.Header");
//		consumer.setConfigParameterValue(PARAM_FILE_NAME_FEATURE, "id");
//		consumer.reconfigure();
//		CAS cas = createCas();
//		JCas jcas = cas.getJCas();
//		jcas.setDocumentText(TEST_TEXT);
//		Header header = new Header(jcas);
//		header.setId(idValue);
//		header.addToIndexes();
//		consumer.processCas(cas);	
//		File expectedXMI = new File(OUTPUT_FOLDER_XMI + "/" + idValue + XMI_EXTENSION); 
//		assertTrue(expectedXMI.exists());
	}

	/**
	 * Check file names of files written by the {@link CasToXmiConsumer} when FileNameType 
	 * and FileNameFeature were set but are not present as annotations.
	 * @throws Exception
	 */
	@Test 
	public void testProcessCasWithCustomFileNameSourceNonAvailable() throws Exception {
//		consumer.setConfigParameterValue(PARAM_FILE_NAME_TYPE, "de.julielab.jcore.types.Header");
//		consumer.setConfigParameterValue(PARAM_FILE_NAME_FEATURE, "id");
//		consumer.reconfigure();
//		CAS cas = createCas();
//		JCas jcas = cas.getJCas();
//		jcas.setDocumentText(TEST_TEXT);
//		Header header = new Header(jcas);
//		header.setSource("test_1");
//		header.addToIndexes();
//		consumer.processCas(cas);			
//		File expectedXMI = new File(OUTPUT_FOLDER_XMI + "/1" + XMI_EXTENSION); 
//		assertTrue(expectedXMI.exists());
	}

}
