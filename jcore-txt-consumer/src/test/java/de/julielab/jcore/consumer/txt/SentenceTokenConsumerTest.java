/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.consumer.txt;

import static de.julielab.jcore.consumer.txt.SentenceTokenConsumer.*;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import de.julielab.java.utilities.FileUtilities;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.Test;

import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.pubmed.Header;
import static org.assertj.core.api.Assertions.*;
public class SentenceTokenConsumerTest {
	/**
	 * just tests if there is an error with an empty CAS
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessEmptyCAS() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class,
				PARAM_OUTPUT_DIR, "src/test/resources/data");

		consumer.process(cas);
	}
	/**
	 * testing without POSTags, just sentences and tokens
	 * @throws Exception
	 */
	@Test
	public void testProcessWithoutPOSTags() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class,
				PARAM_OUTPUT_DIR, "src/test/resources/data");

		cas.setDocumentText("I love food. I like sleeping.");

		Sentence s1 = new Sentence(cas, 0, 12);
		s1.addToIndexes();

		Sentence s2 = new Sentence(cas, 13, 29);
		s2.addToIndexes();

		Token t1 = new Token(cas, 0, 1);
		t1.addToIndexes();

		Token t2 = new Token(cas, 2, 6);
		t2.addToIndexes();

		Token t3 = new Token(cas, 7, 11);
		t3.addToIndexes();

		Token t4 = new Token(cas, 11, 12);
		t4.addToIndexes();

		Token t5 = new Token(cas, 13, 14);
		t5.addToIndexes();

		Token t6 = new Token(cas, 15, 19);
		t6.addToIndexes();

		Token t7 = new Token(cas, 20, 28);
		t7.addToIndexes();

		Token t8 = new Token(cas, 28, 29);
		t8.addToIndexes();

		Header header = new Header(cas);
		header.setDocId("withoutPOS");
		header.addToIndexes();

		consumer.process(cas);

		assertTrue(new File("src/test/resources/data/withoutPOS.txt").exists());

		List<String> file = readFile("src/test/resources/data/withoutPOS.txt");
		assertTrue(file.contains("I love food ."));
		assertTrue(file.contains("I like sleeping ."));

		//System.out.println(file.get(0) + "\n" + file.get(1));

	}

	/**
	 * tests with POSTags
	 * @throws Exception
	 */
	@Test
	public void testProcessWithPOSTags() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class,
				PARAM_OUTPUT_DIR, "src/test/resources/data",
				SentenceTokenConsumer.PARAM_DELIMITER, "$");

		cas.setDocumentText("I love food. I like sleeping.");

		Sentence s1 = new Sentence(cas, 0, 12);
		s1.addToIndexes();

		Sentence s2 = new Sentence(cas, 13, 29);
		s2.addToIndexes();

		Token t1 = new Token(cas, 0, 1);
		t1.addToIndexes();
		// TODO
		FSArray array1 = new FSArray(cas, 1);
		PennBioIEPOSTag pos1 = new PennBioIEPOSTag(cas);
		pos1.setValue("PRP");
		array1.set(0, pos1);
		t1.setPosTag(array1);

		Token t2 = new Token(cas, 2, 6);
		t2.addToIndexes();

		FSArray array2 = new FSArray(cas, 1);
		PennBioIEPOSTag pos2 = new PennBioIEPOSTag(cas);
		pos2.setValue("VBP");
		array2.set(0, pos2);
		t2.setPosTag(array2);

		Token t3 = new Token(cas, 7, 11);
		t3.addToIndexes();

		FSArray array3 = new FSArray(cas, 1);
		PennBioIEPOSTag pos3 = new PennBioIEPOSTag(cas);
		pos3.setValue("NN");
		array3.set(0, pos3);
		t3.setPosTag(array3);

		Token t4 = new Token(cas, 11, 12);
		t4.addToIndexes();

		FSArray array4 = new FSArray(cas, 1);
		PennBioIEPOSTag pos4 = new PennBioIEPOSTag(cas);
		pos4.setValue(".");
		array4.set(0, pos4);
		t4.setPosTag(array4);

		Token t5 = new Token(cas, 13, 14);
		t5.addToIndexes();

		FSArray array5 = new FSArray(cas, 1);
		PennBioIEPOSTag pos5 = new PennBioIEPOSTag(cas);
		pos5.setValue("PRP");
		array5.set(0, pos5);
		t5.setPosTag(array5);

		Token t6 = new Token(cas, 15, 19);
		t6.addToIndexes();

		FSArray array6 = new FSArray(cas, 1);
		PennBioIEPOSTag pos6 = new PennBioIEPOSTag(cas);
		pos6.setValue("VBP");
		array6.set(0, pos6);
		t6.setPosTag(array6);

		Token t7 = new Token(cas, 20, 28);
		t7.addToIndexes();

		FSArray array7 = new FSArray(cas, 1);
		PennBioIEPOSTag pos7 = new PennBioIEPOSTag(cas);
		pos7.setValue("VBG");
		array7.set(0, pos7);
		t7.setPosTag(array7);

		Token t8 = new Token(cas, 28, 29);
		t8.addToIndexes();

		FSArray array8 = new FSArray(cas, 1);
		PennBioIEPOSTag pos8 = new PennBioIEPOSTag(cas);
		pos8.setValue(".");
		array8.set(0, pos8);
		t8.setPosTag(array8);

		// JCoReTools.printAnnotationIndex(cas, Sentence.type);

		Header header = new Header(cas);
		header.setDocId("withPOS");
		header.addToIndexes();

		consumer.process(cas);

		assertTrue(new File("src/test/resources/data/withPOS.txt").exists());

		assertFalse(new File("src/test/resources/data/nemo.txt").exists());

		List<String> file = readFile("src/test/resources/data/withPOS.txt");
		assertFalse(file.contains("I_PRP love_VBP food_NN ._."));
		assertTrue(file.contains("I$PRP like$VBP sleeping$VBG .$."));

		System.out.println(file.get(0) + "\n" + file.get(1));
		// assertTrue(file.contains("I|PRP love|VBP food|NN .|." + "\\n" +
		// "I|PRP like|VBP sleeping|VBG .|."));

	}

	public List<String> readFile(String fileName) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
		String line = null;
		while ((line = bufferedReader.readLine()) != null)
			lines.add(line);

		bufferedReader.close();
		return lines;
	}
	
	@Test
	public void testWriteDocumentText() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
		jcas.setDocumentText("This is line 1\nthis is line two.");
		// add a few annotations that should totally be ignored in document text mode
		new Token(jcas, 0, 4).addToIndexes();
		new Token(jcas, 5, 7).addToIndexes();
		new Token(jcas, 8, 12).addToIndexes();
		new Sentence(jcas, 0, 7).addToIndexes();
		Header header = new Header(jcas, 0, 0);
		header.setDocId("documentTest");
		header.addToIndexes();
		
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class, PARAM_OUTPUT_DIR, "src/test/resources/data", SentenceTokenConsumer.PARAM_MODE, "DOCUMENT");
		consumer.process(jcas);
		
		File outputFile = new File("src/test/resources/data/documentTest.txt");
		assertTrue(outputFile.exists());
		List<String> lines = readFile(outputFile.getAbsolutePath());
		assertEquals(2, lines.size());
		assertEquals("This is line 1", lines.get(0));
		assertEquals("this is line two.", lines.get(1));
	}
	
	@Test
	public void testFromDescriptor1() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
		jcas.setDocumentText("This is line 1\nthis is line two.");
		// add a few annotations that should totally be ignored in document text mode
		new Token(jcas, 0, 4).addToIndexes();
		new Token(jcas, 5, 7).addToIndexes();
		new Token(jcas, 8, 12).addToIndexes();
		new Sentence(jcas, 0, 7).addToIndexes();
		Header header = new Header(jcas, 0, 0);
		header.setDocId("documentTest");
		header.addToIndexes();
		
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.txt.desc.jcore-txt-consumer", PARAM_OUTPUT_DIR, "src/test/resources/data", SentenceTokenConsumer.PARAM_MODE, "DOCUMENT");
		consumer.process(jcas);
		
		File outputFile = new File("src/test/resources/data/documentTest.txt");
		assertTrue(outputFile.exists());
		List<String> lines = readFile(outputFile.getAbsolutePath());
		assertEquals(2, lines.size());
		assertEquals("This is line 1", lines.get(0));
		assertEquals("this is line two.", lines.get(1));
	}
	
	@Test
	public void testFromDescriptor2() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
		jcas.setDocumentText("This is line 1\nthis is line two.");
		// add a few annotations that should totally be ignored in document text mode
		new Token(jcas, 0, 4).addToIndexes();
		new Token(jcas, 5, 7).addToIndexes();
		new Token(jcas, 8, 12).addToIndexes();
		new Sentence(jcas, 0, 7).addToIndexes();
		Header header = new Header(jcas, 0, 0);
		header.setDocId("tokenTest");
		header.addToIndexes();
		
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.txt.desc.jcore-txt-consumer", PARAM_OUTPUT_DIR, "src/test/resources/data", SentenceTokenConsumer.PARAM_MODE, "TOKEN");
		consumer.process(jcas);
		
		File outputFile = new File("src/test/resources/data/tokenTest.txt");
		assertTrue(outputFile.exists());
		List<String> lines = readFile(outputFile.getAbsolutePath());
		assertEquals(1, lines.size());
		assertEquals("This is", lines.get(0));
	}

	@Test
	public void testZip() throws Exception {
		Stream.of(new File("src/test/resources/data").listFiles((dir, name) -> name.startsWith("TXTConsumerArchive"))).forEach(f -> f.delete());
		final AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class, PARAM_OUTPUT_DIR, "src/test/resources/data", PARAM_ZIP_ARCHIVE, true, PARAM_ZIP_MAX_SIZE, 2, PARAM_MODE, "DOCUMENT");
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
		jcas.setDocumentText("Document 1");
		consumer.process(jcas);

		jcas.reset();
		jcas.setDocumentText("Document 2");
		consumer.process(jcas);

		jcas.reset();
		jcas.setDocumentText("Document 3");
		consumer.process(jcas);

		consumer.collectionProcessComplete();

		final File[] archives = new File("src/test/resources/data").listFiles((dir, name) -> name.startsWith("TXTConsumerArchive"));
		assertThat(archives).hasSize(2);

		File archive1 = new File("src/test/resources/data").listFiles((dir, name) -> name.startsWith("TXTConsumerArchive1"))[0];
		try (FileSystem zipfs = FileSystems.newFileSystem(archive1.toPath(), null)) {
            String line = new BufferedReader(new InputStreamReader(zipfs.provider().newInputStream(zipfs.getPath("0.txt")), StandardCharsets.UTF_8)).readLine();
            assertThat(line).isEqualTo("Document 1");
            line = new BufferedReader(new InputStreamReader(zipfs.provider().newInputStream(zipfs.getPath("1.txt")), StandardCharsets.UTF_8)).readLine();
            assertThat(line).isEqualTo("Document 2");
        }
		File archive2= new File("src/test/resources/data").listFiles((dir, name) -> name.startsWith("TXTConsumerArchive2"))[0];
        try (FileSystem zipfs = FileSystems.newFileSystem(archive2.toPath(), null)) {
            String line = new BufferedReader(new InputStreamReader(zipfs.provider().newInputStream(zipfs.getPath("2.txt")), StandardCharsets.UTF_8)).readLine();
            assertThat(line).isEqualTo("Document 3");
        }
	}

    @Test
    public void testLowercasing() throws Exception {
        final AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class, PARAM_OUTPUT_DIR, "src/test/resources/data", PARAM_LOWERCASE, true, PARAM_MODE, "DOCUMENT");
        JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
        jcas.setDocumentText("DoCUmeNt 1");
        consumer.process(jcas);

        final String line = FileUtilities.getReaderFromFile(new File("src/test/resources/data/0.txt")).readLine();
        assertThat(line).isEqualTo("document 1");
    }

}
