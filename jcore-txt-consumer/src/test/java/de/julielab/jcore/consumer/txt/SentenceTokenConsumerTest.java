package de.julielab.jcore.consumer.txt;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
				SentenceTokenConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

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
				SentenceTokenConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

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
				SentenceTokenConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data",
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

}
