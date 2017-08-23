/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.jtbd.main;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

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

	private static final String TEST_SENTENCES_OFFSETS = "0-14;14-15;16-23;24-27;28-32;33-36;37-40;40-41;"
			+ "42-46;46-47;47-54;55-60;61-63;64-65;65-68;68-69;69-70";
	private static final String TEST_TERM_OFFSETS = "0-5;6-13;13-14;14-15;15-16";

	private static final String TEST_SENTENCES_TOKEN_NUMBERS = "1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17";
	private static final String TEST_TERM_TOKEN_NUMBERS = "1;2;3;4;5";

	private String getPredictedOffsets(final Iterator<?> tokIter) {
		String predictedOffsets = "";
		while (tokIter.hasNext()) {
			final Token t = (Token) tokIter.next();
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
		assertEquals(TEST_SENTENCES_OFFSETS, predictedOffsets);

		// get the token numbers of the sentences
		tokIter = indexes.getAnnotationIndex(Token.type).iterator();
		final String tokenNumbers = getTokenNumbers(tokIter);
		// compare token numbers
		LOGGER.debug("testProcess() - predicted: " + tokenNumbers);
		LOGGER.debug("testProcess() -    wanted: "
				+ TEST_SENTENCES_TOKEN_NUMBERS);
		assertEquals(TEST_SENTENCES_TOKEN_NUMBERS, tokenNumbers);
	}

	/**
	 * Test CAS without sentence annotations.
	 */
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
