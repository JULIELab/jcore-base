package de.julielab.jcore.consumer.cas2conll.test;

import static org.junit.Assert.assertTrue;

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

import de.julielab.jcore.consumer.cas2conll.ConllConsumer;
import de.julielab.jcore.types.DependencyRelation;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.pubmed.Header;

public class ConllConsumerTest {

	private String expectedText = "1" + "\t" + "I" + "\t" + "I" + "\t" + "PRP" + "\t" + "PRP" + "\t" + "_" + "\t" + "_"
			+ "\t" + "_" + "\t" + "0" + "\t" + "_" + "\t" + "_" + "\t" + "_";

	@Test
	public void testProcessEmptyCAS() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ConllConsumer.class,
				ConllConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

		Header header = new Header(cas);
		header.setDocId("emptyFile");
		header.addToIndexes();

		consumer.process(cas);

		assertTrue(new File("src/test/resources/data/0.CONLL").exists());

		assertTrue(new File("src/test/resources/data/emptyFile.CONLL").exists());
	}

	@Test
	public void testProcess() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ConllConsumer.class,
				ConllConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

		cas.setDocumentText("I love food. I like sleeping.");

		Sentence s1 = new Sentence(cas, 0, 12);
		s1.addToIndexes();

		Sentence s2 = new Sentence(cas, 13, 29);
		s2.addToIndexes();

		Token t1 = new Token(cas, 0, 1);
		t1.addToIndexes();

		FSArray array1 = new FSArray(cas, 1);
		PennBioIEPOSTag pos1 = new PennBioIEPOSTag(cas);
		pos1.setValue("PRP");
		array1.set(0, pos1);
		t1.setPosTag(array1);

		FSArray array11 = new FSArray(cas, 1);
		PennBioIEPOSTag pos11 = new PennBioIEPOSTag(cas);
		pos11.setValue("PRP");
		array11.set(0, pos11);
		t1.setPosTag(array11);

		DependencyRelation d1 = new DependencyRelation(cas);
		d1.addToIndexes();

		// -----------------------------------------------------

		Token t2 = new Token(cas, 2, 6);
		t2.addToIndexes();

		FSArray array2 = new FSArray(cas, 1);
		PennBioIEPOSTag pos2 = new PennBioIEPOSTag(cas);
		pos2.setValue("VBP");
		array2.set(0, pos2);
		t2.setPosTag(array2);

		FSArray array22 = new FSArray(cas, 1);
		PennBioIEPOSTag pos22 = new PennBioIEPOSTag(cas);
		pos22.setValue("VBP");
		array22.set(0, pos22);
		t2.setPosTag(array22);

		DependencyRelation d2 = new DependencyRelation(cas);
		d2.addToIndexes();

		// -----------------------------------------------------

		Token t3 = new Token(cas, 7, 11);
		t3.addToIndexes();

		FSArray array3 = new FSArray(cas, 1);
		PennBioIEPOSTag pos3 = new PennBioIEPOSTag(cas);
		pos3.setValue("NN");
		array3.set(0, pos3);
		t3.setPosTag(array3);

		FSArray array33 = new FSArray(cas, 1);
		PennBioIEPOSTag pos33 = new PennBioIEPOSTag(cas);
		pos33.setValue("NN");
		array33.set(0, pos33);
		t3.setPosTag(array33);

		DependencyRelation d3 = new DependencyRelation(cas);
		d3.addToIndexes();

		// -----------------------------------------------------

		Token t4 = new Token(cas, 11, 12);
		t4.addToIndexes();

		FSArray array4 = new FSArray(cas, 1);
		PennBioIEPOSTag pos4 = new PennBioIEPOSTag(cas);
		pos4.setValue(".");
		array4.set(0, pos4);
		t4.setPosTag(array4);

		FSArray array44 = new FSArray(cas, 1);
		PennBioIEPOSTag pos44 = new PennBioIEPOSTag(cas);
		pos44.setValue(".");
		array44.set(0, pos44);
		t4.setPosTag(array44);

		DependencyRelation d4 = new DependencyRelation(cas);
		d4.addToIndexes();

		// -------------------------------------------------------

		Token t5 = new Token(cas, 13, 14);
		t5.addToIndexes();

		FSArray array5 = new FSArray(cas, 1);
		PennBioIEPOSTag pos5 = new PennBioIEPOSTag(cas);
		pos5.setValue("PRP");
		array5.set(0, pos5);
		t5.setPosTag(array5);

		FSArray array55 = new FSArray(cas, 1);
		PennBioIEPOSTag pos55 = new PennBioIEPOSTag(cas);
		pos55.setValue("PRP");
		array55.set(0, pos55);
		t5.setPosTag(array55);

		DependencyRelation d5 = new DependencyRelation(cas);
		d5.addToIndexes();

		// ---------------------------------------------------------

		Token t6 = new Token(cas, 15, 19);
		t6.addToIndexes();

		FSArray array6 = new FSArray(cas, 1);
		PennBioIEPOSTag pos6 = new PennBioIEPOSTag(cas);
		pos6.setValue("VBP");
		array6.set(0, pos6);
		t6.setPosTag(array6);

		FSArray array66 = new FSArray(cas, 1);
		PennBioIEPOSTag pos66 = new PennBioIEPOSTag(cas);
		pos66.setValue("VBP");
		array66.set(0, pos66);
		t6.setPosTag(array66);

		DependencyRelation d6 = new DependencyRelation(cas);
		d6.addToIndexes();

		// ----------------------------------------------------------

		Token t7 = new Token(cas, 20, 28);
		t7.addToIndexes();

		FSArray array7 = new FSArray(cas, 1);
		PennBioIEPOSTag pos7 = new PennBioIEPOSTag(cas);
		pos7.setValue("NN");
		array7.set(0, pos7);
		t7.setPosTag(array7);

		FSArray array77 = new FSArray(cas, 1);
		PennBioIEPOSTag pos77 = new PennBioIEPOSTag(cas);
		pos77.setValue("NN");
		array77.set(0, pos77);
		t7.setPosTag(array77);

		DependencyRelation d7 = new DependencyRelation(cas);
		d7.addToIndexes();

		// -----------------------------------------------------------

		Token t8 = new Token(cas, 28, 29);
		t8.addToIndexes();

		FSArray array8 = new FSArray(cas, 1);
		PennBioIEPOSTag pos8 = new PennBioIEPOSTag(cas);
		pos8.setValue(".");
		array8.set(0, pos8);
		t8.setPosTag(array8);

		FSArray array88 = new FSArray(cas, 1);
		PennBioIEPOSTag pos88 = new PennBioIEPOSTag(cas);
		pos88.setValue(".");
		array88.set(0, pos88);
		t8.setPosTag(array88);

		DependencyRelation d8 = new DependencyRelation(cas);
		d8.addToIndexes();

		Header header = new Header(cas);
		header.setDocId("conllTest");
		header.addToIndexes();

		consumer.process(cas);

		assertTrue(new File("src/test/resources/data/conllTest.CONLL").exists());

		List<String> file = readFile("src/test/resources/data/conllTest.CONLL");
		assertTrue(file.contains(expectedText));

		System.out.println(file.get(0) + "\n" + file.get(1));

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
