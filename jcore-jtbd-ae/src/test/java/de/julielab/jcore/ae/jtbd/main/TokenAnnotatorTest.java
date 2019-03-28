/**
 * TokenAnnotatorTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.0 Since version: 1.0
 *
 * Creation date: Nov 29, 2006
 *
 * This is a JUnit test for the TokenAnnotator.
 **/

package de.julielab.jcore.ae.jtbd.main;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import junit.framework.TestCase;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class TokenAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TokenAnnotatorTest.class);

	private static final String DESCRIPTOR = "src/test/resources/de/julielab/jcore/ae/jtbd/desc/TokenAnnotatorTest.xml";

	private static final String TEST_TERM = "alpha protein(s)";
	//	private static final String TEST_TERM = "Broadly speaking, TUFs can be classified into three categories: 1.";
	//	private static final String TEST_SENTENCES = "X-inactivation, T-cells and CD44 are XYZ! CD44-related " +
	//			"stuff is\t(not).";
	private static final String TEST_SENTENCES = "X-inactivation, T-cells and CD44 are XYZ! CD44-related "
			+ "stuff is\t(not).";

	private static final String TEST_SENTENCES_OFFSETS = "0-1;1-2;2-14;14-15;16-23;24-27;28-32;33-36;37-40;40-41;"
			+ "42-46;46-47;47-54;55-60;61-63;64-65;65-68;68-69;69-70";
	private static final String TEST_TERM_OFFSETS = "0-5;6-13;13-14;14-15;15-16";

	private static final String TEST_SENTENCES_TOKEN_NUMBERS = "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19";
	private static final String TEST_TERM_TOKEN_NUMBERS = "1;2;3;4;5";

	private String getPredictedOffsets(final Iterator<?> tokIter) {
		String predictedOffsets = "";
		while (tokIter.hasNext()) {
			final Token t = (Token) tokIter.next();
			System.out.println("getPredictedOffsets() - token: " + t.getCoveredText()
					+ " " + t.getBegin() + " - " + t.getEnd());
			LOGGER.debug("getPredictedOffsets() - token: " + t.getCoveredText()
					+ " " + t.getBegin() + " - " + t.getEnd());
			predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
			predictedOffsets += t.getBegin() + "-" + t.getEnd();
		}
		return predictedOffsets;
	}

	private String getTokenNumbers(final Iterator<?> tokIter) {
		String tokenNumbers = "";
		while (tokIter.hasNext()) {
			final Token t = (Token) tokIter.next();
			LOGGER.debug("getTokenNumbers() - token: " + t.getCoveredText()
					+ " " + t.getId());
			System.out.println("getTokenNumbers() - token: " + t.getCoveredText()
					+ " " + t.getId());
			tokenNumbers += (tokenNumbers.length() > 0) ? ";" : "";
			tokenNumbers += t.getId();
		}
		return tokenNumbers;
	}

	/**
	 * initialize a CAS which is then used for the test. 2 sentences are added
	 */
	public void initSentenceCas(final JCas jcas) {
		jcas.reset();
		jcas.setDocumentText(TEST_SENTENCES);

		final Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(41);
		s1.addToIndexes();

		final Sentence s2 = new Sentence(jcas);
		s2.setBegin(42);
		s2.setEnd(70);
		s2.addToIndexes();
	}

	/**
	 * initialize a CAS which is then used for the test, the CAS holds no token
	 * annotations
	 */
	public void initTermCas(final JCas jcas) {
		jcas.reset();
		jcas.setDocumentText(TEST_TERM);
	}

	/**
	 * Test CAS with sentence annotations.
	 */
	@Test
	public void testProcess() {

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		try {
			tokenXML = new XMLInputSource(DESCRIPTOR);
			tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					tokenXML);
			tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);
			tokenAnnotator.setConfigParameterValue("UseDocText", false);
			tokenAnnotator.reconfigure();
		} catch (final Exception e) {
			LOGGER.error("testProcess()", e);
		}

		JCas jcas = null;
		try {
			jcas = tokenAnnotator.newJCas();
		} catch (final ResourceInitializationException e) {
			LOGGER.error("testProcess()", e);
		}
		initSentenceCas(jcas);
		try {
			tokenAnnotator.process(jcas);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// get the offsets of the sentences
		final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		final String predictedOffsets = getPredictedOffsets(tokIter);
		// compare offsets
		LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
		LOGGER.debug("testProcess() -    wanted: " + TEST_SENTENCES_OFFSETS);
		System.out.println("predicted: " + predictedOffsets);
		System.out.println("wanted: " + TEST_SENTENCES_OFFSETS);
		assertEquals(TEST_SENTENCES_OFFSETS, predictedOffsets);

		// get the token numbers of the sentences
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		final String tokenNumbers = getTokenNumbers(tokIter);
		// compare token numbers
		LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
		LOGGER.debug("testProcess() -    wanted: "
				+ TEST_SENTENCES_TOKEN_NUMBERS);
		System.out.println("predicted: "+tokenNumbers);
		System.out.println("wanted: " +TEST_SENTENCES_TOKEN_NUMBERS);
		assertEquals(TEST_SENTENCES_TOKEN_NUMBERS, tokenNumbers);
	}

	/**
	 * Test CAS without sentence annotations.
	 */
	@Test
	public void testProcessUseWholeDocumentText() {

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		try {
			tokenXML = new XMLInputSource(DESCRIPTOR);
			tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					tokenXML);
			tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);
			tokenAnnotator.setConfigParameterValue("UseDocText", true);
			tokenAnnotator.reconfigure();
		} catch (final Exception e) {
			LOGGER.error("testProcess()", e);
		}

		JCas jcas = null;
		try {
			jcas = tokenAnnotator.newJCas();
		} catch (final ResourceInitializationException e) {
			LOGGER.error("testProcess()", e);
		}

		// ------------- testing TEST_SENTENCES as input ----------------
		initSentenceCas(jcas);
		try {
			tokenAnnotator.process(jcas);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String predictedOffsets = getPredictedOffsets(tokIter);
		// compare offsets
		LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
		LOGGER.debug("testProcess() -    wanted: " + TEST_SENTENCES_OFFSETS);
		assertEquals(TEST_SENTENCES_OFFSETS, predictedOffsets);

		// get the token numbers of the sentences
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String tokenNumbers = getTokenNumbers(tokIter);
		// compare token numbers
		LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
		LOGGER.debug("testProcess() -    wanted: "
				+ TEST_SENTENCES_TOKEN_NUMBERS);
		assertEquals(TEST_SENTENCES_TOKEN_NUMBERS, tokenNumbers);

		// ------------- testing TEST_TERM as input ----------------
		initTermCas(jcas);
		try {
			tokenAnnotator.process(jcas, null);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// get the offsets of the term
		indexes = jcas.getJFSIndexRepository();
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		predictedOffsets = getPredictedOffsets(tokIter);
		// compare offsets
		LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
		LOGGER.debug("testProcess() -    wanted: " + TEST_TERM_OFFSETS);
		assertEquals(TEST_TERM_OFFSETS, predictedOffsets);

		// get the token numbers of the sentences
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		tokenNumbers = getTokenNumbers(tokIter);
		// compare token numbers
		LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
		LOGGER.debug("testProcess() -    wanted: " + TEST_TERM_TOKEN_NUMBERS);
		assertEquals(TEST_TERM_TOKEN_NUMBERS, tokenNumbers);

	}

}
