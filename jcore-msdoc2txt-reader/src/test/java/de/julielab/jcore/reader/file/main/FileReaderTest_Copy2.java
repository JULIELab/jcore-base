/**
 * FileReaderTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: muehlhausen
 *
 * Current version: 1.0 Since version: 1.0
 *
 * Creation date: 27.08.2007
 *
 * Tests for class <code>FileReader</code>, a UIMA <code>CollctionReader</code>.
 */

package de.julielab.jcore.reader.file.main;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.pubmed.Header;

public class FileReaderTest_Copy2
{
	/**
	 * Path to the FileReader descriptor
	 */
	private static final String DESC_FILE_READER = "src/main/resources/de/julielab/jcore/reader/file/desc/jcore-file-reader.xml";

	private static CAS cas;

	private static final String FILE_NAME = "data/files/2017-03-16-TEST2.doc";
//	private static final String FILE_NAME = "data/resources/input/Arztbrief";
	
	private static final String TEXT_ARTIFACT = ReadMSdocWithTable.readDocFileTableAndTransform2Text(FILE_NAME);
	
	private static final String DIRECTORY_INPUT = "data/files";
	
	@Test
	public void testDocumentTextPresent() throws CASException, Exception
	{
		System.out.println("FileReaderTest.testDocumentTextPresent()");
		
		// TODO folgenden Code über Ordnerstruktur laufen lassen, vlt. 2. Test ansetzen!
		
		System.out.println("XXXX - 0");
		
		CollectionReader fileReader = getCollectionReader(DESC_FILE_READER);	// führt initialize() aus
//		fileReader.setConfigParameterValue("InputDirectory",FILE_NAME.substring(0, FILE_NAME.lastIndexOf("/")));
//		System.out.println(FILE_NAME.substring(0, FILE_NAME.lastIndexOf("/")));
		fileReader.setConfigParameterValue("InputDirectory", DIRECTORY_INPUT);
		fileReader.setConfigParameterValue("UseFilenameAsDocId", true);

		fileReader.setConfigParameterValue("ReadSubDirs", true);
		
		fileReader.setConfigParameterValue(MSdoc2txtReader.ALLOWED_FILE_EXTENSIONS, new String[]{"doc"});
		fileReader.reconfigure();	// führt initialize() aus

		cas = CasCreationUtils.createCas( (AnalysisEngineMetaData) fileReader.getMetaData() );
		
//		assertTrue(fileReader.hasNext());		// JUnit-Funktion, ob Behauptung wahr ist - if (fileReader.hasNext()){...}

//		fileReader.getNext(cas);	// 1 Dokument gelesen!
//		cas.reset();

		while(fileReader.hasNext())
		{
			fileReader.getNext(cas);
			cas.reset();
		}
		
		
//		assertTrue(cas.getDocumentText().equals(TEXT_ARTIFACT));

//		Type headerType = cas.getTypeSystem().getType(Header.class.getCanonicalName());
//		FSIterator<FeatureStructure> headerIt = cas.getJCas().getFSIndexRepository().getAllIndexedFS(headerType);
//		assertTrue(headerIt.hasNext());		// war drin
		
//		System.out.println(headerIt.hasNext());
		
//		Header header = (Header) headerIt.next();
//		assertEquals("8563171", header.getDocId());
		

//		Type dateType = cas.getTypeSystem().getType(Date.class.getCanonicalName());
//		FSIterator<FeatureStructure> dateIt = cas.getJCas().getFSIndexRepository().getAllIndexedFS(dateType);
//		assertTrue(dateIt.hasNext());	// war drin
//		Date date = (Date) dateIt.next();	// war drin - Date => jCoRe-Type
		
//		assertEquals(1995, date.getYear());	// war drin
//		assertEquals(10, date.getMonth());	// war drin
//		System.out.println("pubmed-id: " + header.getDocId() + ", publication date: " + date.getYear() + "/" + date.getMonth());
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

	@AfterClass
	public static void tearDown() throws Exception
	{
//		File artifactFile = new File(FILE_ARTIFACT);
//		artifactFile.delete();
	}
}
