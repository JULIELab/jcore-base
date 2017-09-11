/** 
 * SentenceSplitterTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: muehlhausen
 * 
 * Current version: 1.0
 * Since version:   1.6
 *
 * Creation date: 01.10.2008 
 **/

package de.julielab.jcore.ae.jsbd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import de.julielab.jcore.ae.jsbd.SentenceSplitter;
import de.julielab.jcore.ae.jsbd.Unit;

/**
 * Test for the class {@link SentenceSplitter}
 * 
 * @author muehlhausen
 */
public class SentenceSplitterTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceSplitterTest.class);

	private static final String FILENAME_MODEL = "src/test/resources/de/julielab/jcore/ae/jsbd/model/test-model.gz";
	private static final String FILENAME_TRAIN_DATA = "src/test/resources/testdata/train/train.dat";
	private static final String FILENAME_TRAIN_MODEL_OUTPUT = "/tmp/TestModelOuput.mod";
	private static final String FILENAME_ABSTRACT = "src/test/resources/test-abstract.txt";

	/**
	 * @throws Test
	 *             reading a serialized model object
	 */
	@Test
	public void testReadModel() throws Exception {

		SentenceSplitter sentenceSplitter = new SentenceSplitter();
		sentenceSplitter.readModel(new File(FILENAME_MODEL));
		assertNotNull(sentenceSplitter.model);
	}

	/**
	 * Test training and outputting a model object using training data in a file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTrain() throws Exception {

		File[] trainFiles = getTrainFiles();
		SentenceSplitter sentenceSplitter = new SentenceSplitter();
		InstanceList trainData = sentenceSplitter.makeTrainingData(trainFiles, false);
		Pipe trainPipe = trainData.getPipe();
		sentenceSplitter.train(trainData, trainPipe);
		sentenceSplitter.writeModel(FILENAME_TRAIN_MODEL_OUTPUT);
		
		assertTrue(new File(FILENAME_TRAIN_MODEL_OUTPUT + ".gz").isFile());
	}

	/**
	 * Test predict
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPredict() throws Exception {

		File[] trainFiles = getTrainFiles();
		SentenceSplitter sentenceSplitter = new SentenceSplitter();
		sentenceSplitter.readModel(new File(FILENAME_MODEL));

		String text = readTextFromFile(FILENAME_ABSTRACT);
		// ArrayList<String> lines = readLinesFromFile(FILENAME_ABSTRACT);
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(text);

		Pipe myPipe = sentenceSplitter.model.getInputPipe();
		Instance instance = sentenceSplitter.makePredictionData(lines, myPipe);
		ArrayList<Unit> unitList = sentenceSplitter.predict(instance, "biomed");

		assertNotNull(unitList);
		for (Unit unit : unitList) {
			LOGGER.trace("unit=" + unit);
		}
	}

	private ArrayList<String> readLinesFromFile(String filename) {
		ArrayList<String> list = new ArrayList<String>();
		File file = new File(filename);
		if (file.isFile()) {
			try {
				FileReader reader = new FileReader(file);
				// reader.r
				BufferedReader br = new BufferedReader(reader);

				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					list.add(line);
				}
			} catch (FileNotFoundException e) {
				LOGGER.error(e.getMessage(), e);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}

		}
		return list;
	}

	private String readTextFromFile(String filename) {

		StringBuffer buffer = new StringBuffer();
		File file = new File(filename);
		if (file.isFile()) {
			try {
				FileReader reader = new FileReader(file);
				// reader.r
				BufferedReader br = new BufferedReader(reader);

				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					buffer.append(line);
				}
			} catch (FileNotFoundException e) {
				LOGGER.error(e.getMessage(), e);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}

		}
		return buffer.toString();
	}

	private File[] getTrainFiles() {

		File trainFile = new File(FILENAME_TRAIN_DATA);
		return new File[] { trainFile };
	}

}
