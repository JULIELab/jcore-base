/**
 * MSdocReaderTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 *
 * @author: Christina Lohr 
 *
 * Current version: 1.0 Since version: 1.0
 *
 * Creation date: 31.03.2017
 *
 * Tests for class <code>FileReader</code>, a UIMA <code>CollctionReader</code>.
 */

package de.julielab.jcore.reader.msdoc.main;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MSdocReaderTest {
	/**
	 * Path to the FileReader descriptor
	 */
	private static final String DESC_FILE_READER = "src/main/resources/de/julielab/jcore/reader/msdoc/desc/jcore-msdoc-reader.xml";

	/**
	 * CAS for UIMA
	 */
	private static CAS cas;

	/**
	 * Names of dummies
	 */
	private static final String DIRECTORY_INPUT = "data/files/input";
	// do not delete data/files directory, it is used by 'DESC_FILE_READER'

	private static final String DIR1 = DIRECTORY_INPUT + "/dir1";
	private static final String SUBDIR1 = DIR1 + "/subdir1";

	private static final String DIR2 = DIRECTORY_INPUT + "/dir2";
	private static final String SUBDIR2 = DIR2 + "/subdir2";

	private static final String DIR3 = DIRECTORY_INPUT + "/dir3";
	private static final String SUBDIR31 = DIR3 + "/subdir31";
	private static final String SUBDIR32 = DIR3 + "/subdir32";

	private static final String DOC_DUMMY_NAME = "dummy.doc";
	private static final String DOC_DUMMY_FILE = "src/test/resources/" + DOC_DUMMY_NAME;

	@BeforeClass
	public static void setUp() throws Exception {
		/**
		 * Create dummies of *.doc-files.
		 */

		new File(DIRECTORY_INPUT).mkdirs();

		new File(DIR1).mkdir();
		new File(SUBDIR1).mkdir();
		writeArtifact(SUBDIR1 + "/" + DOC_DUMMY_NAME);

		new File(DIR2).mkdir();
		new File(SUBDIR2).mkdir();
		writeArtifact(SUBDIR2 + "/" + DOC_DUMMY_NAME);

		new File(DIR3).mkdir();
		new File(SUBDIR31).mkdir();
		writeArtifact(SUBDIR31 + "/" + DOC_DUMMY_NAME);
		new File(SUBDIR32).mkdir();
		writeArtifact(SUBDIR32 + "/" + DOC_DUMMY_NAME);
	}

	@Test
	public void testDocumentTextPresent() throws CASException, Exception {

		File file = new File(DOC_DUMMY_FILE);

		ReadSingleMSdoc.INPUT_FILE = file.getPath();
		ReadSingleMSdoc.doc2Text();

		String artifactText = ReadSingleMSdoc.CONTENT_TAB_MARKED;

		CollectionReader fileReader = getCollectionReader(DESC_FILE_READER);
		fileReader.setConfigParameterValue("InputDirectory", DIRECTORY_INPUT);
		fileReader.setConfigParameterValue("UseFilenameAsDocId", true);
		fileReader.setConfigParameterValue("ReadSubDirs", true);
		fileReader.setConfigParameterValue(MSdocReader.ALLOWED_FILE_EXTENSIONS, new String[] { "doc" });
		fileReader.reconfigure();

		cas = CasCreationUtils.createCas((AnalysisEngineMetaData) fileReader.getMetaData());
		assertTrue(fileReader.hasNext());

		while (fileReader.hasNext()) {
			fileReader.getNext(cas);
			assertEquals(cas.getDocumentText(), artifactText);
			cas.reset();
		}
	}

	/**
	 * Produces a CollectionReader form the given descriptor file name
	 *
	 * @param descriptor
	 *            The path to the descriptor
	 * @return A collection reader
	 */
	public static CollectionReader getCollectionReader(String descriptor) {
		CollectionReader reader = null;
		XMLInputSource inputSource = null;

		ResourceSpecifier readerResourceSpecifier = null;

		try {
			inputSource = new XMLInputSource(descriptor);
			readerResourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(inputSource);
			reader = UIMAFramework.produceCollectionReader(readerResourceSpecifier);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InvalidXMLException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
		return reader;
	}

	/**
	 * Write the artifact to a file.
	 *
	 * @param artifact
	 *            Text to be written to file
	 * @throws IOException
	 */
	private static void writeArtifact(String file_name) throws IOException {

		File file = new File(file_name);
		if (!file.exists()) {
			Files.copy(Paths.get(DOC_DUMMY_FILE), Paths.get(file_name));
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		/**
		 * Delete dummies from setUp.
		 */

		FileUtils.deleteDirectory(new File(DIRECTORY_INPUT));
	}
}
