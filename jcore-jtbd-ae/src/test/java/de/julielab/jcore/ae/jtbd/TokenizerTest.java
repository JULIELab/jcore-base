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
package de.julielab.jcore.ae.jtbd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import de.julielab.jcore.ae.jtbd.Tokenizer;
import de.julielab.jcore.ae.jtbd.Unit;
import de.julielab.jcore.ae.jtbd.main.TokenAnnotator;
import de.julielab.jcore.types.Token;

/**
 * Test for the class {@link Tokenizer}
 * 
 * @author tomanek
 */
public class TokenizerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(TokenizerTest.class);

	private static final String FILENAME_MODEL = "src/test/resources/de/julielab/jcore/ae/jtbd/model/jtbd-2.0-biomed.gz";
	private static final String FILENAME_TRAIN_DATA_ORG = "src/test/resources/testdata/train/train.sent";
	private static final String FILENAME_TRAIN_DATA_TOK = "src/test/resources/testdata/train/train.tok";
	private static final String FILENAME_TRAIN_MODEL_OUTPUT = "/tmp/TestModelOuput.mod";
	private static final String FILENAME_ABSTRACT = "src/test/resources/testdata/test/abstract.txt";

	private List<String> readLinesFromFile(final String filename) throws IOException {
		return FileUtils.readLines(new File(filename), "utf-8");
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
						"de/julielab/jcore/ae/jtbd/model/jtbd-2.0-biomed.gz", TokenAnnotator.USE_DOC_TEXT_PARAM, true);
		engine.process(jCas.getCas());

		Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
		assertEquals(5, tokens.size());
	}

}
