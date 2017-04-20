/**
 * MSdoc2txtReaderTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * @author: Christina Lohr 
 *
 * Current version: 1.0 Since version: 1.0
 *
 * Creation date: 31.03.2017
 *
 * Tests for class <code>FileReader</code>, a UIMA <code>CollctionReader</code>.
 */

package de.julielab.jcore.reader.msdoc2txt.main;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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
//import org.springframework.aop.ThrowsAdvice;

public class MSdoc2txtReaderTest
{
	/**
	 * Path to the FileReader descriptor
	 */
	private static final String DESC_FILE_READER = "src/main/resources/de/julielab/jcore/reader/msdoc2txt/desc/jcore-msdoc2txt-reader.xml";
	private static final String DIRECTORY_INPUT = "data/files";

	private static CAS cas;
	
	@BeforeClass
	public static void setUp() throws Exception
	{
		File dir = new File("data/input");
		dir.mkdir();
		writeArtifact(dir.getAbsolutePath() + "/writeDOC.doc");
		
		File dir1 = new File("data/input/dir1");
		dir1.mkdir();
		
		File subdir1 = new File("data/input/dir1/subdir1");
		subdir1.mkdir();
		
		File dir2 = new File("data/input/dir2");
		dir2.mkdir();
		
		File subdir2 = new File("data/input/dir3/subdir2");
		subdir2.mkdir();
		
		File dir3 = new File("data/input/dir3");
		dir3.mkdir();
		
		File subdir31 = new File("data/input/dir3/subdir31");
		subdir31.mkdir();
		
		File subdir32 = new File("data/input/dir3/subdir32");
		subdir32.mkdir();
	}
	
	@Test
	public void testDocumentTextPresent() throws CASException, Exception
	{
		CollectionReader fileReader = getCollectionReader(DESC_FILE_READER);
		fileReader.setConfigParameterValue("InputDirectory", DIRECTORY_INPUT);
		fileReader.setConfigParameterValue("UseFilenameAsDocId", true);
		fileReader.setConfigParameterValue("ReadSubDirs", true);
		fileReader.setConfigParameterValue(MSdoc2txtReader.ALLOWED_FILE_EXTENSIONS, new String[]{"doc"});
		fileReader.reconfigure();

		cas = CasCreationUtils.createCas( (AnalysisEngineMetaData) fileReader.getMetaData() );
		
		assertTrue(fileReader.hasNext());

		while(fileReader.hasNext())
		{
			fileReader.getNext(cas);
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
	public static CollectionReader getCollectionReader(String descriptor)
	{
		CollectionReader reader = null;
		XMLInputSource inputSource = null;
		
		ResourceSpecifier readerResourceSpecifier = null;
		
		try
		{
			inputSource = new XMLInputSource(descriptor);
			readerResourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(inputSource);
			reader = UIMAFramework.produceCollectionReader(readerResourceSpecifier);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		catch (InvalidXMLException e)
		{
			e.printStackTrace();
		}
		catch (ResourceInitializationException e)
		{
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
//		private static void writeArtifact(String artifact, String file_name) throws IOException {
//		FileOutputStream outStream = new FileOutputStream("resources/doc/writeDOC.doc");
		FileOutputStream outStream = new FileOutputStream(file_name);
		
		
		@SuppressWarnings("resource")
		XWPFDocument doc = new XWPFDocument();
		XWPFParagraph paraTit = doc.createParagraph();
		
		paraTit.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun paraTitRun = paraTit.createRun();
		
		paraTitRun.setBold(true);
		paraTitRun.setFontSize(20);
//		paraTitRun.setFontFamily(fontFamily);
		paraTitRun.setText("Jetzt bin ich aber mal gespannt!");
		
		doc.write(outStream);
		outStream.close();
	}

	
	@AfterClass
	public static void tearDown() throws Exception
	{
		// directories and files from setUp delete
	}
}
