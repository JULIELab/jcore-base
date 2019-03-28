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
package de.julielab.jcore.ae.stanford.lemma;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class StanfordLemmatizerTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(StanfordLemmatizerTest.class);
	private static final String DESCRIPTOR = "src/test/resources/de/julielab/jcore/ae/stanford/lemma/desc/jcore-stanford-lemmatizer-ae.xml";

	private static final String TEST_SENTENCES = "Plectranthus barbatus is a medicinal plant used to treat a wide range of disorders including seizure.";
	private static final int[] tokenBegins = { 0, 13, 22, 25, 27, 37, 43, 48,
			51, 57, 59, 64, 70, 73, 83, 93, 100 };
	private static final int[] tokenEnds = { 12, 21, 24, 26, 36, 42, 47, 50,
			56, 58, 63, 69, 72, 82, 92, 100, 101 };
	private static final String[] posTags = { "NN", "NN", "VBZ", "DT", "JJ",
			"NN", "VBN", "TO", "VB", "DT", "JJ", "NN", "IN", "NNS", "VBG",
			"NN", "." };
	private static final String TEST_LEMMAS = "plectranthus;barbatus;be;a;medicinal;plant;use;to;treat;a;wide;range;of;disorder;include;seizure;.";

	public void initCas(JCas aJCas) {
		aJCas.reset();
		aJCas.setDocumentText(TEST_SENTENCES);

		Sentence sent = new Sentence(aJCas);
		sent.setBegin(0);
		sent.setEnd(101);
		sent.addToIndexes();

		for (int i = 0; i < tokenBegins.length; i++) {
			Token tok = new Token(aJCas);
			tok.setBegin(tokenBegins[i]);
			tok.setEnd(tokenEnds[i]);
			FSArray posArray = new FSArray(aJCas, 1);
			PennBioIEPOSTag posTag = new PennBioIEPOSTag(aJCas);
			posTag.setValue(posTags[i]);
			posArray.set(0, posTag);
			tok.setPosTag(posArray);
			tok.addToIndexes();
		}
	}

	@SuppressWarnings("rawtypes")
	public void testProcess() {

		XMLInputSource lemmaXML = null;
		ResourceSpecifier lemmaSpec = null;
		AnalysisEngine lemmaAnnotator = null;

		try {
			lemmaXML = new XMLInputSource(DESCRIPTOR);
			lemmaSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					lemmaXML);
			lemmaAnnotator = UIMAFramework.produceAnalysisEngine(lemmaSpec);
		} catch (Exception e) {
			LOGGER.error("testProcess()", e);
		}

		JCas aJCas = null;
		try {
			aJCas = lemmaAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			LOGGER.error("testProcess()", e);
		}
		initCas(aJCas);
		try {
			lemmaAnnotator.process(aJCas);
		} catch (Exception e) {
			e.printStackTrace();
		}

		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		Iterator tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		String predictedLemmas = getPredictedLemmas(tokIter);
		LOGGER.debug("testProcess() - predicted: " + predictedLemmas);
		LOGGER.debug("testProcess() -    wanted: " + TEST_LEMMAS);
		assertEquals(TEST_LEMMAS, predictedLemmas);
	}

	@SuppressWarnings("rawtypes")
	public String getPredictedLemmas(Iterator tokIter) {
		String predictedLemmas = "";
		while (tokIter.hasNext()) {
			Token tok = (Token) tokIter.next();
			String tokText = tok.getCoveredText();
			String lemmaText = tok.getLemma().getValue();
			LOGGER.debug("getPredictedLemmas() - Token: " + tokText
					+ " - Lemma: " + lemmaText);
			if (tokIter.hasNext()) {
				predictedLemmas = predictedLemmas + lemmaText + ";";
			} else {
				predictedLemmas = predictedLemmas + lemmaText;
			}
		}
		return predictedLemmas;
	}
}
