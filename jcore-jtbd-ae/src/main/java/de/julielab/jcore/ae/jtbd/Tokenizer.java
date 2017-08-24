/**
 * Tokenizer.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.0 Since version: 1.0
 *
 * Creation date: Aug 01, 2006
 *
 * The main class for the JULIE Token Boundary Detector. This class has all the
 * function for training and prediction etc. The following labels are used:
 **/

package de.julielab.jcore.ae.jtbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Sequence;

public class Tokenizer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Tokenizer.class);

	CRF model = null;

	private boolean trained = false;

	public Tokenizer() {
		LOGGER.debug("this is the JTBD constuctor");
		model = null;
		trained = false;
	}

	/**
	 * retrieve the labels from a LabelSequence
	 *
	 * @param ls
	 * @return
	 */
	ArrayList<String> getLabelsFromLabelSequence(final LabelSequence ls) {
		final ArrayList<String> labels = new ArrayList<String>();
		for (int j = 0; j < ls.size(); j++)
			labels.add((String) ls.get(j));
		return labels;
	}

	public CRF getModel() {
		return model;
	}

	/**
	 * make material for prediction from a collection of sentences
	 */
	InstanceList makePredictionData(final List<String> orgSentences,
			final List<String> tokSentences) {

		LOGGER.debug("makePredictionData() - making prediction data");

		final InstanceList predictData = new InstanceList(model.getInputPipe());
		for (int i = 0; i < orgSentences.size(); i++) {
			final StringBuffer orgSentence = new StringBuffer(
					orgSentences.get(i));
			final StringBuffer tokSentence = new StringBuffer(
					tokSentences.get(i));

			final Instance inst = makePredictionData(orgSentence, tokSentence);
			// As of now, we should have an array of whitespace information. If
			// we still have a string, there was a problem converting the
			// sentence to units.
			if (!(inst.getSource() instanceof String))
				predictData.add(inst);
		}
		return predictData;
	}

	/**
	 * make material for prediction
	 *
	 * @param orgSentence
	 *            the original sentence
	 * @param tokSentence
	 *            empty string may be provided
	 * @return
	 */
	private Instance makePredictionData(final StringBuffer orgSentence,
			final StringBuffer tokSentence) {
		// remove last character of orgSentence if this is an EOS-symbol
		Character lastChar = null;
		if (tokSentence.length() > 0) {
			lastChar = tokSentence.charAt(tokSentence.length() - 1);
			if (EOSSymbols.contains(lastChar))
				tokSentence.deleteCharAt(tokSentence.length() - 1);
		}

		if (orgSentence.length() > 0) {
			lastChar = orgSentence.charAt(orgSentence.length() - 1);
			if (EOSSymbols.contains(lastChar))
				orgSentence.deleteCharAt(orgSentence.length() - 1);
		}
		Instance inst = null;
		// Logging level 'Trace' is used that is unknown to log4j versions older
		// than 1.2.12.
		try {
			inst = model.getInputPipe().instanceFrom(
					new Instance(orgSentence.toString(), null, null,
							tokSentence.toString()));
		} catch (final NoSuchMethodError e) {
			e.printStackTrace();
			System.exit(0);
		}
		return inst;
	}

	/**
	 * make material for training from given data
	 *
	 * @param orgSentences
	 *            original sentence
	 * @param tokSentences
	 *            a tokenized sentence
	 * @return
	 */
	InstanceList makeTrainingData(final List<String> orgSentences,
			final List<String> tokSentences) {

		LOGGER.debug("makeTrainingData() - making training data...");

		final LabelAlphabet dict = new LabelAlphabet();
		dict.lookupLabel("P", true); // unit is a token boundary
		dict.lookupLabel("N", true); // unit is not a token boundary

		final Pipe myPipe = new SerialPipes(new Pipe[] {
				new Sentence2TokenPipe(),
				new OffsetConjunctions(new int[][] { { -1 }, { 1 } }),
				// new PrintTokenSequenceFeatures(),
				new TokenSequence2FeatureVectorSequence(true, true) });
		final InstanceList instList = new InstanceList(myPipe);

		System.out.print("preparing training data...");
		for (int i = 0; i < orgSentences.size(); i++) {

			// remove leading and trailing ws
			final StringBuffer orgSentence = new StringBuffer(orgSentences.get(
					i).trim());
			final StringBuffer tokSentence = new StringBuffer(tokSentences.get(
					i).trim());

			// remove last character of orgSentence if this is an EOS-symbol

			Character lastChar = tokSentence.charAt(tokSentence.length() - 1);
			if (EOSSymbols.contains(lastChar))
				tokSentence.deleteCharAt(tokSentence.length() - 1);

			lastChar = orgSentence.charAt(orgSentence.length() - 1);
			if (EOSSymbols.contains(lastChar))
				orgSentence.deleteCharAt(orgSentence.length() - 1);

			// make instance
			instList.addThruPipe(new Instance(orgSentence.toString(), "",
					new Integer(i), tokSentence.toString()));
		}

		LOGGER.debug("makeTrainingData() -  number of features on training data: "
				+ myPipe.getDataAlphabet().size());

		return instList;
	}

	/**
	 * do the prediction
	 *
	 * @param an
	 *            instance for prediction
	 * @return an ArrayList of Unit objects containing the predicted label
	 */
	@SuppressWarnings("unchecked")
	ArrayList<Unit> predict(final Instance inst) {

		if ((trained == false) || (model == null))
			throw new IllegalStateException(
					"No model available. Train or load trained model first.");

		final ArrayList<Unit> units = (ArrayList<Unit>) inst.getName();
		if (units.size() > 0) {
			// get sequence
			final Sequence<?> input = (Sequence<?>) inst.getData();

			// transduce and generate output
			final Sequence<?> crfOutput = model.transduce(input);
			for (int j = 0; j < crfOutput.size(); j++)
				units.get(j).label = (String) crfOutput.get(j);
		}
		return units;
	}

	/**
	 * do the prediction
	 *
	 * @param original
	 *            sentence
	 * @return an ArrayList of Unit objects containing the predicted label
	 */
	public ArrayList<Unit> predict(final String sentence) {
		LOGGER.debug("predict() - before pedicting labelss ...");
		if ((trained == false) || (model == null))
			throw new IllegalStateException(
					"No model available. Train or load trained model first.");
		LOGGER.debug("predict() - now making pedictions ...");
		final Instance inst = makePredictionData(new StringBuffer(sentence),
				new StringBuffer(""));
		LOGGER.debug("predict() - after pedicting labels ...");
		return predict(inst);
	}

	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was
	 * stored as serialized object to disk.
	 *
	 * @param filename
	 *            where to find the serialized featureSubsetModel (full path!)
	 */
	public void readModel(final File file) throws IOException,
	FileNotFoundException, ClassNotFoundException {
		final FileInputStream fis = new FileInputStream(file);
		readModel(fis);
	}

	public void readModel(InputStream is) throws IOException, ClassNotFoundException {
		final GZIPInputStream gin = new GZIPInputStream(is);
		final ObjectInputStream ois = new ObjectInputStream(gin);
		model = (CRF) ois.readObject();
		trained = true;
		model.getInputPipe().getDataAlphabet().stopGrowth();
		ois.close();
	}

	void setModel(final CRF crf) {
		trained = true;
		model = crf;
	}

	/**
	 * show the context of c words around a error
	 *
	 * @param i
	 * @param units
	 * @param orgLabels
	 * @return
	 */
	String showErrorContext(final int i, final ArrayList<Unit> units,
			final ArrayList<String> orgLabels) {

		final int c = 2;

		String orgContext = "";
		String newContext = "";

		for (int j = 0; j < units.size(); j++)
			if ((j >= (i - c)) && (j <= (i + c))) {
				final String orgL = (orgLabels.get(j).equals("P")) ? " " : "";
				final String newL = (units.get(j).label.equals("P")) ? " " : "";
				orgContext += units.get(j).rep + orgL;
				newContext += units.get(j).rep + newL;
			}
		return newContext + "\n" + orgContext + "\n";
	}

	/**
	 * do the training
	 *
	 * @param instList
	 * @param myPipe
	 */
	void train(final InstanceList instList, final Pipe myPipe) {
		final long s1 = System.currentTimeMillis();

		// set up model
		model = new CRF(myPipe, null);
		model.addStatesForLabelsConnectedAsIn(instList);

		// get trainer
		final CRFTrainerByLabelLikelihood crfTrainer = new CRFTrainerByLabelLikelihood(
				model);

		// do the training with unlimited amount of iterations
		// --> refrained from using modified version of mallet;
		// it's now the original source
		final boolean b = crfTrainer.train(instList);
		LOGGER.info("Tokenizer training: model converged: " + b);

		final long s2 = System.currentTimeMillis();

		// stop growth and set trained
		model.getInputPipe().getDataAlphabet().stopGrowth();
		trained = true;

		LOGGER.debug("train() - training time: " + ((s2 - s1) / 1000) + " sec");
	}

	/**
	 * Save the model learned to disk. THis is done via Java's object
	 * serialization.
	 *
	 * @param filename
	 *            where to write it (full path!)
	 */
	void writeModel(final String filename) {
		if ((trained == false) || (model == null))
			throw new IllegalStateException(
					"train or load trained model first.");
		try {
			final FileOutputStream fos = new FileOutputStream(new File(filename
					+ ".gz"));
			final GZIPOutputStream gout = new GZIPOutputStream(fos);
			final ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(model);
			oos.close();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
