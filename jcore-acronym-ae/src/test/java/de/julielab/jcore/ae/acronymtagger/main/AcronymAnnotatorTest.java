/** 
 * AcronymAnnotatorTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tusche
 * 
 * Current version: 2.0
 *
 * Creation date: 15. Jan, 2007
 * 
 **/

package de.julielab.jcore.ae.acronymtagger.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.AbbreviationLongform;
import de.julielab.jcore.types.Sentence;
import junit.framework.TestCase;

/**
 * The AcronymAnnotatorTest class
 * 
 * @author jwermter
 */
public class AcronymAnnotatorTest extends TestCase {

	private static final String DOCUMENT_TEXT = "[TAZ]Die Firma Kohl-kopf (FK-K) hat für die Straßenverkehrsordnung (StVO) "
			+ "in der Bundesrepublik Deutschland(BRD)  einen hochintelligenten Manager für die Chefetage "
			+ "(HIMFDC) eingestellt und dabei jede Menge Quatsch (jMQ) gemacht  "
			+ "Hydrogenovibrio marinus MH-110 possesses three different sets of genes "
			+ "for ribulose 1,5-bisphosphate carboxylase/oxygenase (RubisCO)  "
			+ "two form I (cbbLS-1 and cbbLS-2) and one form II (cbbM)  "
			+ "However, StVO does not interact with BRD. And intracranial aneurisms (IAs) suck a lot. We show that IA causes "
			+ "Multiple myeloMa (MM). That MM is really nasty stuff! "
			+ "dumm-dämliches Tandemkarzinom (ddTDK) ist generell gefährlich."
			+ "The proton affinity (PA) of the methyl esters increases continually. PAs are ...";

	private static final Logger LOGGER = LoggerFactory.getLogger(AcronymAnnotatorTest.class);

	private static final String BASE_PATH = "src/test/resources/de/julielab/jcore/ae/acronymtagger/";

	private static final String AA_DESCRIPTOR = BASE_PATH + "desc/jcore-acronymtagger-test.xml";

	private static final String FILE_DESCRIPTOR_AE = BASE_PATH + "desc/JulesToolsAEDescriptor.xml";

	private static final String ALL_TYPES_NAME = "de.julielab.jcore.types.jcore-all-types";

	public void testProcess() throws ResourceInitializationException, InvalidXMLException, IOException, CASException {

		CAS cas = CasCreationUtils.createCas(
				UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(FILE_DESCRIPTOR_AE)));
		JCas testCas = null;
		testCas = cas.getJCas();

		boolean allOK = true;

		try {
			ResourceSpecifier spec;
			// abbr annotator
			spec = UIMAFramework.getXMLParser().parseResourceSpecifier(new XMLInputSource(AA_DESCRIPTOR));

			AnalysisEngine abbreAn = UIMAFramework.produceAnalysisEngine(spec);

			testCas.setDocumentText(DOCUMENT_TEXT);
			Sentence sent = new Sentence(testCas, 0, DOCUMENT_TEXT.length());
			sent.addToIndexes();

			// addTokens(testCas, DOCUMENT_TEXT);

			LOGGER.info(testCas.getDocumentText());

			abbreAn.process(testCas);
			ArrayList<String> result = new ArrayList<String>();
			ArrayList<Integer> begins = new ArrayList<Integer>();
			ArrayList<Integer> ends = new ArrayList<Integer>();
			ArrayList<Boolean> definedHere = new ArrayList<Boolean>();

			result.add("Firma Kohl-kopf");
			begins.add(Integer.valueOf(9));
			ends.add(Integer.valueOf(24));
			definedHere.add(Boolean.valueOf(true));
			result.add("Straßenverkehrsordnung");
			begins.add(Integer.valueOf(44));
			ends.add(Integer.valueOf(66));
			definedHere.add(Boolean.valueOf(true));
			result.add("Bundesrepublik Deutschland");
			begins.add(Integer.valueOf(81));
			ends.add(Integer.valueOf(107));
			definedHere.add(Boolean.valueOf(true));
			result.add("hochintelligenten Manager für die Chefetage");
			begins.add(Integer.valueOf(120));
			ends.add(Integer.valueOf(163));
			definedHere.add(Boolean.valueOf(true));
			result.add("jede Menge Quatsch");
			begins.add(Integer.valueOf(195));
			ends.add(Integer.valueOf(213));
			definedHere.add(Boolean.valueOf(true));
			// embedded acronym:
			result.add("Menge Quatsch");
			begins.add(Integer.valueOf(200));
			ends.add(Integer.valueOf(213));
			definedHere.add(Boolean.valueOf(true));

			result.add("ribulose 1,5-bisphosphate carboxylase/oxygenase");
			begins.add(Integer.valueOf(304));
			ends.add(Integer.valueOf(351));
			definedHere.add(Boolean.valueOf(true));

			result.add("Straßenverkehrsordnung");
			begins.add(Integer.valueOf(44));
			ends.add(Integer.valueOf(66));
			definedHere.add(Boolean.valueOf(false));
			result.add("Bundesrepublik Deutschland");
			begins.add(Integer.valueOf(81));
			ends.add(Integer.valueOf(107));
			definedHere.add(Boolean.valueOf(false));

			result.add("intracranial aneurisms");
			begins.add(Integer.valueOf(466));
			ends.add(Integer.valueOf(488));
			definedHere.add(Boolean.valueOf(true));
			result.add("intracranial aneurisms");
			begins.add(Integer.valueOf(466));
			ends.add(Integer.valueOf(488));
			definedHere.add(Boolean.valueOf(false));

			result.add("multiple Myeloma");
			begins.add(Integer.valueOf(530));
			ends.add(Integer.valueOf(546));
			definedHere.add(Boolean.valueOf(true));
			result.add("Multiple myeloma");
			begins.add(Integer.valueOf(530));
			ends.add(Integer.valueOf(546));
			definedHere.add(Boolean.valueOf(false));

			result.add("dumm-dämliches Tandemkarzinom");
			begins.add(Integer.valueOf(584));
			ends.add(Integer.valueOf(613));
			definedHere.add(Boolean.valueOf(true));
			// embedded acronym:
			result.add("Tandemkarzinom");
			begins.add(Integer.valueOf(599));
			ends.add(Integer.valueOf(613));
			definedHere.add(Boolean.valueOf(true));

			result.add("proton affinity");
			begins.add(Integer.valueOf(650));
			ends.add(Integer.valueOf(665));
			definedHere.add(Boolean.valueOf(true));
			result.add("proton affinity");
			begins.add(Integer.valueOf(650));
			ends.add(Integer.valueOf(665));
			definedHere.add(Boolean.valueOf(false));

			// KT, 27.09.2007, as i have removed normalization in AE, also the
			// test was modified accordingly
			// result.add(al.normalize("Firma Kohl-kopf", true));
			// result.add(al.normalize("Straßenverkehrsordnung", true));
			// result.add(al.normalize("Bundesrepublik Deutschland", true));
			// result.add(al.normalize(
			// "hochintelligenten Manager für die Chefetage", true));
			// result.add(al.normalize("jede Menge Quatsch", true));

			JFSIndexRepository indexes = testCas.getJFSIndexRepository();

			FSIterator abbreIter = indexes.getAnnotationIndex(Abbreviation.type).iterator();

			int i = 0;
			Abbreviation a;
			while (abbreIter.hasNext()) {
				a = (Abbreviation) abbreIter.next();

				// if (a.getExpan().equals(result.get(i))) {
				if (a.getExpan().equalsIgnoreCase(result.get(i))
						&& a.getTextReference().getBegin() == begins.get(i).intValue()
						&& a.getTextReference().getEnd() == ends.get(i).intValue()
						&& a.getDefinedHere() == definedHere.get(i).booleanValue()) {
					LOGGER.info("\n\nCORRECT annotation made:");
					LOGGER.info("\nabbr = " + a.getCoveredText() + "\nexpansion = " + a.getExpan() + "\nbeginTextRef = "
							+ a.getTextReference().getBegin() + "\nendTextRef = " + a.getTextReference().getEnd()
							+ "\ngetTextRefCoveredText = " + a.getTextReference().getCoveredText() + "\ndefineHere = "
							+ a.getDefinedHere());

				} else {
					allOK = false;
					LOGGER.info("\n\nWRONG annotation made: ");
					LOGGER.info("\nabbr = " + a.getCoveredText() + "\nexpansion = " + a.getExpan() + "\nbeginTextRef = "
							+ a.getTextReference().getBegin() + "\nendTextRef = " + a.getTextReference().getEnd()
							+ "\ngetTextRefCoveredText = " + a.getTextReference().getCoveredText() + "\ndefineHere = "
							+ a.getDefinedHere());

				}
				i++;
			}

		} catch (Exception e) {
			allOK = false;
			LOGGER.error("testProcess()", e);

		}

		assertTrue(allOK);
		if (allOK) {
			LOGGER.info("\n\t==> AcronymAnnotator works fine");
		}

	}

	@Test
	public void testFindFullFormInParentheses() throws Exception {
		CAS cas = CasCreationUtils.createCas(
				UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(FILE_DESCRIPTOR_AE)));
		JCas testCas = null;
		testCas = cas.getJCas();

		ResourceSpecifier spec;
		// abbr annotator
		spec = UIMAFramework.getXMLParser().parseResourceSpecifier(new XMLInputSource(AA_DESCRIPTOR));

		AnalysisEngine abbreAn = UIMAFramework.produceAnalysisEngine(spec);

		String text = "The Drosophila retinal-specific protein, TRP (transient receptor potential), is the founding member of a family of store-operated channels (SOCs) conserved from C. elegans to humans.";

		testCas.setDocumentText(text);
		Sentence sent = new Sentence(testCas, 0, text.length());
		sent.addToIndexes();

		abbreAn.process(cas);

		FSIterator<Annotation> abbrIt = testCas.getAnnotationIndex(Abbreviation.type).iterator();

		assertTrue(abbrIt.hasNext());
		Abbreviation abbreviation = (Abbreviation) abbrIt.next();
		assertEquals("TRP", abbreviation.getCoveredText());
		assertEquals("transient receptor potential", abbreviation.getExpan());
		assertEquals("transient receptor potential", abbreviation.getTextReference().getCoveredText());
	}

	/*
	 * private void addTokens(JCas testCas, String documentText) {
	 * 
	 * int pos = 0;
	 * 
	 * Token tok = new Token(testCas, pos, pos);
	 * 
	 * while ((pos = documentText.indexOf(" ", pos)) >= 0) { tok.setEnd(pos);
	 * tok.addToIndexes(); System.err.println("adding token: [" +
	 * tok.getCoveredText() + "]\n" + tok); tok = new Token(testCas, pos+1,
	 * pos+1); pos++; }
	 * 
	 * }
	 */

	@Test
	public void testUimaFitAndClassPathResource() throws Exception {
		// Preparation
		JCas jCas = JCasFactory.createJCas(ALL_TYPES_NAME);
		jCas.setDocumentText("We observed interleukin-2 (IL2) in the cell");
		Sentence sentence = new Sentence(jCas, 0, jCas.getDocumentText().length());
		sentence.addToIndexes();
		// Read the acronym list file from the classpath
		AnalysisEngine engine = AnalysisEngineFactory.createEngine(AcronymAnnotator.class,
				AcronymAnnotator.PARAM_ACROLIST, BASE_PATH + "testresources/acrolist.txt");

		// Let the annotator run
		engine.process(jCas.getCas());

		// We expect one abbreviation
		FSIterator<Annotation> iterator = jCas.getAnnotationIndex(Abbreviation.type).iterator();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testPostprocessing() throws Exception {
		// Preparation
		JCas jCas = JCasFactory.createJCas(ALL_TYPES_NAME);
		jCas.setDocumentText("Tumor necrosis factor (TNF) is one thing. The TNF receptor(TNF-R) is another.");
		new Sentence(jCas, 0, 41).addToIndexes();
		new Sentence(jCas, 42, jCas.getDocumentText().length()).addToIndexes();
		
		AnalysisEngine engine = AnalysisEngineFactory.createEngine(AcronymAnnotator.class,
				AcronymAnnotator.PARAM_ACROLIST, BASE_PATH + "testresources/acrolist.txt");

		// Let the annotator run
		engine.process(jCas.getCas());
		// We expect two abbreviations and two full form annotations
		Collection<Abbreviation> acronyms = JCasUtil.select(jCas, Abbreviation.class);
		assertEquals(2, acronyms.size());
		Collection<AbbreviationLongform> longForms = JCasUtil.select(jCas, AbbreviationLongform.class);
		assertEquals(2, longForms.size());
	}

	@Test
	public void testPostprocessing2() throws Exception {
		// Preparation
		JCas jCas = JCasFactory.createJCas(ALL_TYPES_NAME);
		String docText = new String(
				Files.readAllBytes(
						Paths.get("src/test/resources/de/julielab/jcore/ae/acronymtagger/testresources/1698610.txt")),
				"UTF-8");
		jCas.setDocumentText(docText);
		
		// do simple text splitting at full stops, works for this document
		int fromIndex = 0;
		int toIndex;
		while((toIndex = docText.indexOf('.', fromIndex)) != -1) {
			if (fromIndex > 0)
				++fromIndex;
			new Sentence(jCas, fromIndex, toIndex + 1).addToIndexes();
			fromIndex = toIndex+1;
		}
		// end sentence splitting
		
		AnalysisEngine engine = AnalysisEngineFactory.createEngine(AcronymAnnotator.class,
				AcronymAnnotator.PARAM_ACROLIST, BASE_PATH + "testresources/acrolist.txt");
		// Let the annotator run
		engine.process(jCas.getCas());
		
		// We expect 16 abbreviations and 4 full form annotations
		Collection<Abbreviation> acronyms = JCasUtil.select(jCas, Abbreviation.class);
//		acronyms.stream().map(a -> a.getCoveredText() + " " + a.getBegin() + " " + a.getEnd()).forEach(System.out::println);
		assertEquals(16, acronyms.size());
		Collection<AbbreviationLongform> longForms = JCasUtil.select(jCas, AbbreviationLongform.class);
		assertEquals(4, longForms.size());
	}

}
