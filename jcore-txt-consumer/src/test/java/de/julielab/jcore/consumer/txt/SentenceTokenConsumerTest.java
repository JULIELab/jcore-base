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
import org.junit.Test;

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

	@Test
	public void testProcess() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(SentenceTokenConsumer.class,
				SentenceTokenConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

		cas.setDocumentText("I love food. I like sleeping.");

		Sentence s1 = new Sentence(cas, 0, 12);
		s1.addToIndexes();

		Sentence s2 = new Sentence(cas, 13, 29);
		s2.addToIndexes();

		new Token(cas, 0, 1).addToIndexes();

		new Token(cas, 2, 6).addToIndexes();

		new Token(cas, 7, 11).addToIndexes();

		new Token(cas, 11, 12).addToIndexes();

		new Token(cas, 13, 14).addToIndexes();

		new Token(cas, 15, 19).addToIndexes();

		new Token(cas, 20, 28).addToIndexes();

		new Token(cas, 28, 29).addToIndexes();

		// JCoReTools.printAnnotationIndex(cas, Sentence.type);

		Header header = new Header(cas);
		header.setDocId("pew");
		header.addToIndexes();

		consumer.process(cas);

		assertTrue(new File("src/test/resources/data/pew.txt").exists());

		assertFalse(new File("src/test/resources/data/piu.txt").exists());

		List<String> file = readFile("src/test/resources/data/pew.txt");
		assertTrue(file.contains("I_null love_null food_null ._null"));
		//assertTrue(file.contains("I_ like_ sleeping_ ._"));
		//assertTrue(file.contains("I love food .\nI like sleeping ."));
		
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
