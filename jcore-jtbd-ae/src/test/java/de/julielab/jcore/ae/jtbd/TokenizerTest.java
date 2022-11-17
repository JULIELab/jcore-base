/**
 * Tokenizer.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: muehlhausen
 *
 * Current version: 2.0 Since version: 1.6
 *
 * Creation date: 14.10.2008
 **/

package de.julielab.jcore.ae.jtbd;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import de.julielab.jcore.ae.jtbd.main.TokenAnnotator;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the class {@link Tokenizer}
 * 
 * @author tomanek
 */
public class TokenizerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(TokenizerTest.class);

	private static final String FILENAME_MODEL = "src/test/resources/de/julielab/jcore/ae/jtbd/model/test-model.gz";
	private static final String FILENAME_TRAIN_DATA_ORG = "src/test/resources/testdata/train/train.sent";
	private static final String FILENAME_TRAIN_DATA_TOK = "src/test/resources/testdata/train/train.tok";
	private static final String FILENAME_TRAIN_MODEL_OUTPUT = "/tmp/TestModelOuput.mod";
	private static final String FILENAME_ABSTRACT = "src/test/resources/testdata/test/abstract.txt";

	private List<String> readLinesFromFile(final String filename) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(Path.of(filename), StandardCharsets.UTF_8)) {
			return br.lines().collect(Collectors.toList());
		}
	}

	/**
	 * Test predict
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPredict() throws Exception {

		final Tokenizer tokenizer = new Tokenizer();
		tokenizer.readModel(new File(FILENAME_MODEL));

		final List<String> orgSentences = readLinesFromFile(FILENAME_ABSTRACT);
		final ArrayList<String> tokSentences = new ArrayList<String>();
		for (int i = 0; i < orgSentences.size(); ++i)
			tokSentences.add("");

		final InstanceList iList = tokenizer.makePredictionData(orgSentences, tokSentences); // why not use
																								// predict(String) like
																								// belwo?
		for (final Instance instance : iList) {
			final ArrayList<Unit> unitList = tokenizer.predict(instance);
			assertNotNull(unitList);
			// TODO this is a rather weak test, was broken for several years due to changed paths without failing.
			// I fixed the latter, yet it is still only marginally useful, just checking if everything works,
			// yet not if it works correctly
			for (final Unit unit : unitList)
				LOGGER.trace("unit=" + unit);
		}
	}

	/**
	 * Test predict, probably a better version
	 * 
	 * @author hellrich
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPredictNewVersion() throws Exception {

		final Tokenizer tokenizer = new Tokenizer();
		tokenizer.readModel(new File(FILENAME_MODEL));

		final int[] expectedStarts = new int[] { 0, 9, 14, 21 };
		final int[] expectedEnds = new int[] { 8, 13, 20, 27 };

		final ArrayList<Unit> unitList = tokenizer.predict("Sentence with proper ending.");
		assertNotNull(unitList);
		assertEquals(4, unitList.size());

		for (int i = 0; i < unitList.size(); ++i) {
			final Unit unit = unitList.get(i);
			assertEquals(expectedStarts[i], unit.begin);
			assertEquals(expectedEnds[i], unit.end);
			LOGGER.trace("unit=" + unit);
		}
	}

	/**
	 * @throws Test
	 *             reading a serialized model object
	 */
	@Test
	public void testReadModel() throws Exception {

		final Tokenizer tokenizer = new Tokenizer();
		tokenizer.readModel(new File(FILENAME_MODEL));
		assertNotNull(tokenizer.model);
	}

	/**
	 * Test training and outputting a model object using training data in a file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrain() throws Exception {

		final Tokenizer tokenizer = new Tokenizer();
		final List<String> trainDataORG = readLinesFromFile(FILENAME_TRAIN_DATA_ORG);
		final List<String> trainDataTOK = readLinesFromFile(FILENAME_TRAIN_DATA_TOK);
		final InstanceList trainData = tokenizer.makeTrainingData(trainDataORG, trainDataTOK);
		final Pipe trainPipe = trainData.getPipe();
		tokenizer.train(trainData, trainPipe);
		tokenizer.writeModel(FILENAME_TRAIN_MODEL_OUTPUT);

		assertTrue(new File(FILENAME_TRAIN_MODEL_OUTPUT + ".gz").isFile());
	}

	@Test
	public void testClassPathModel() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		jCas.setDocumentText("Please tokenize this sentence.");
		AnalysisEngine engine =
				AnalysisEngineFactory.createEngine(TokenAnnotator.class, TokenAnnotator.PARAM_MODEL,
						"de/julielab/jcore/ae/jtbd/model/test-model.gz", TokenAnnotator.USE_DOC_TEXT_PARAM, true);
		engine.process(jCas.getCas());

		Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
		assertEquals(5, tokens.size());
	}

}
