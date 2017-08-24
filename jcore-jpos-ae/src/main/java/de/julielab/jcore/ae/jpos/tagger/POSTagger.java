/**
 * POSAnnotator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: hellrich
 *
 * Current version: 0.0.1
 *
 * Creation date: Sep 11, 2014
 *
 * Based on Katrin Tomanek's JNET
 *
 **/

package de.julielab.jcore.ae.jpos.tagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import de.julielab.jcore.ae.jpos.pipes.FeatureGenerator;

/**
*
* general class which does all the ML stuff
*
* @author hellrich, based on tomanek
*/
public class POSTagger implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private Object model = null;

	private Properties featureConfig = null;

	private boolean trained = false;

	static Logger LOGGER = LoggerFactory.getLogger(POSTagger.class);

	private int number_iterations = 0;

	private Pipe generalPipe = null;

	private String defaultLabel;

	/**
	 * default constructor
	 */
	public POSTagger() {
		final Properties defaults = new Properties();
		final InputStream defaultFeatureConfigStream = getClass()
				.getResourceAsStream("/defaultFeatureConf.conf");

		try {
			LOGGER.debug("loading default configuration");
			defaults.load(defaultFeatureConfigStream);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			LOGGER.error("", e);
		} catch (final IOException e) {
			e.printStackTrace();
			LOGGER.error("", e);
		}
		featureConfig = new Properties(defaults);
	}

	/**
	 * constructor for feature config file
	 *
	 * @param featureConfigFile
	 */
	public POSTagger(final File featureConfigFile) {
		featureConfig = new Properties();
		if (!featureConfigFile.isFile()) {
			final RuntimeException e = new IllegalStateException(
					"specified file for feature configuration not found!");
			LOGGER.error("", e);
			throw (e);
		}
		try {
			featureConfig.load(new FileInputStream(featureConfigFile));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			LOGGER.error("", e);
		} catch (final IOException e) {
			e.printStackTrace();
			LOGGER.error("", e);
		}
	}

	/**
	 * returns true when model has been successfully trained.
	 *
	 * @return true if trained
	 */
	public boolean isTrained() {
		return trained;
	}

	/**
	 * this is to train a NE model (based on CRF); when trained, the model is
	 * stored internally. The model can be saved to disk using the writeModel
	 * command.
	 *
	 * @param sentences
	 *            training data, an ArrayList of Sentence objects, File which
	 *            contains the feature subset to be used in a text format
	 */
	public void train(final ArrayList<Sentence> sentences) {
		System.out.println("   * training model... on " + sentences.size()
				+ " sentences");

		defaultLabel = sentences.get(0).get(0).getLabel();
		final FeatureGenerator featureGenerator = new FeatureGenerator();
		// create features ones for CRF or ME
		final InstanceList data = featureGenerator.createFeatureData(sentences,
				featureConfig);
		// pipe which effects on the feature creation
		generalPipe = data.getPipe();

		LOGGER.info("  * number of features for training: "
				+ data.getDataAlphabet().size());

		final long start = System.currentTimeMillis();
		final InstanceList tokenData = FeatureGenerator
				.convertFeatsforClassifier(generalPipe, data); // changed

		LOGGER.info("train() - now training on " + data.size() + " instances");

		// now train an ME model
		final MaxEntTrainer maxEntTrainer = new MaxEntTrainer();
		LOGGER.info("JNET ME training ...");
		Classifier me = null;
		if (number_iterations == 0)
			me = maxEntTrainer.train(tokenData);
		else
			me = maxEntTrainer.train(tokenData, number_iterations);
		model = me;

		final long stop = System.currentTimeMillis();
		LOGGER.info("  * learning took (sec): " + ((stop - start) / 1000));

		trained = true;
	}

	/**
	 *
	 * predicts the entity labels by means of a model. this method is needed by
	 * UIMA-JNET!
	 *
	 * @param sentence
	 *            a Sentence object containing all units (= tokens) of that
	 *            sentence
	 */
	public void predictForUIMA(final Sentence sentence) {
		if ((trained == false) || (model == null)) {
			final RuntimeException e = new IllegalStateException(
					"No model available. Train or load trained model first.");
			LOGGER.error("", e);
			throw (e);
		}
		final Classifier classifier = (Classifier) model;
		predictSentence(sentence, classifier);
	}

	void predictSentence(final Sentence sentence, final Classifier classifier) {

		// dummyPipe = new SerialPipes(new Pipe[] { new METrainerDummyPipe(
		// data.getDataAlphabet(), data.getTargetAlphabet()), });
		// final FeatureGenerator featureGenerator = new FeatureGenerator();
		// // create features ones for CRF or ME
		// final InstanceList data =
		// featureGenerator.createFeatureData(sentences, featureConfig);
		// // pipe which effects on the feature creation
		// generalPipe = data.getPipe();

		// default label prevents errors
		for (int i = 0; i < sentence.size(); ++i)
			sentence.get(i).setLabel(defaultLabel);

		final Instance inst = generalPipe.instanceFrom(new Instance(sentence,
				"", "", ""));
		// transform to token based features for ME
		final InstanceList tokenList = FeatureGenerator
				.convertFeatsforClassifier(classifier.getInstancePipe(), inst);
		LOGGER.info("current sentence has this number of token features: "
				+ tokenList.size());

		final ArrayList<Unit> tokens = sentence.getUnits();

		if (tokens.size() != tokenList.size()) {
			LOGGER.error("predict() - something went wrong with sequence feature conversion");
			System.exit(-1);
		}

		// correct tags are set here
		for (int j = 0; j < tokenList.size(); j++) {
			final Classification C = classifier.classify(tokenList.get(j));
			final String label = C.getLabeling().getBestLabel().toString();
			final Unit token = tokens.get(j);
			token.setLabel(label);
		}
	}

	/**
	 * predict the entity labels by means of a previously learned model.
	 *
	 * this method is used by JNET stand alone version (for UIMA-JNET see other
	 * predict method)
	 *
	 * Output is an arraylist of IOB
	 *
	 * @param sentences
	 *            an ArrayList of Sentence objects
	 * @return IOB output for the sentences to be predicted. Each element of the
	 *         ArrayList is a string which refers to one word and its label
	 *         ("token\tlabel")
	 */

	public ArrayList<String> predictForCLI(final ArrayList<Sentence> sentences) {
		if ((trained == false) || (model == null)) {
			final RuntimeException e = new IllegalStateException(
					"no model available. Train or load trained model first.");
			LOGGER.error("", e);
			throw (e);
		}
		final Classifier classifier = (Classifier) model;

		final ArrayList<String> tagged = new ArrayList<>();
		final StringBuilder taggedSentence = new StringBuilder();
		for (final Sentence sentence : sentences) {
			predictSentence(sentence, classifier);
			final int limit = sentence.getUnits().size();
			for (int i = 0; i < limit; ++i) {
				final Unit token = sentence.get(i);
				taggedSentence.append(String.format("%s|%s ", token.getRep(),
						token.getLabel()));
			}
			taggedSentence.replace(taggedSentence.length() - 1,
					taggedSentence.length(), "\n");
			tagged.add(taggedSentence.toString());
			taggedSentence.delete(0, taggedSentence.length());
		}
		return tagged;
	}

	/**
	 * Save the model learned to disk. THis is done via Java's object
	 * serialization.
	 *
	 * @param filename
	 *            where to write it (full path!)
	 */
	public void writeModel(final String filename) {
		if ((trained == false) || (model == null) || (featureConfig == null)) {
			System.err.println("train or load trained model first.");
			System.exit(0);
		}
		try {
			final FileOutputStream fos = new FileOutputStream(
					new File(filename));
			final GZIPOutputStream gout = new GZIPOutputStream(fos);
			final ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(this);
			oos.close();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was
	 * stored as serialized object to disk.
	 *
	 * @param is
	 *            InputStream for a serialized featureSubsetModel
	 */
	public static POSTagger readModel(final InputStream is) throws IOException,
			FileNotFoundException, ClassNotFoundException {
		final GZIPInputStream gin = new GZIPInputStream(is);
		final ObjectInputStream ois = new ObjectInputStream(gin);
		final POSTagger pos = (POSTagger) ois.readObject();
		ois.close();
		return pos;
	}

	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was
	 * stored as serialized object to disk.
	 *
	 * @param filename
	 *            where to find the serialized featureSubsetModel (full path!)
	 */
	public static POSTagger readModel(final File modelFile) throws IOException,
			FileNotFoundException, ClassNotFoundException {
		final FileInputStream fis = new FileInputStream(modelFile);
		return readModel(fis);
	}

	/**
	 * return the model
	 */
	public Object getModel() {
		return model;
	}

	public void setFeatureConfig(final Properties featureConfig) {
		this.featureConfig = featureConfig;
	}

	public Properties getFeatureConfig() {
		return featureConfig;
	}

	public Sentence textToUnits(final String sentence) {
		final String[] tokens = sentence.trim().split(" +");
		final ArrayList<Unit> units = new ArrayList<Unit>();

		for (final String token : tokens)
			units.add(new Unit(0, 0, token, ""));

		return new Sentence(units);
	}

	/**
	 * takes a sentence in piped format and returns the corresponding unit
	 * sentence as a Sentence object
	 *
	 * @param sentence
	 *            in piped format to be converted
	 */
	public Sentence PPDtoUnits(final String sentence) {
		final String[] tokens = sentence.trim().split("[\t ]+");
		String features[];
		String label, word;
		Unit unit;
		// a token
		final ArrayList<Unit> units = new ArrayList<Unit>();

		for (final String token : tokens) {
			new HashMap<String, String>();

			features = token.split("\\|+");

			word = features[0];
			label = features[features.length - 1];

			unit = new Unit(0, 0, word, label);
			units.add(unit);
		}
		return new Sentence(units);
	}

	public int getNumber_Iterations() {
		return number_iterations;
	}

	public void set_Number_Iterations(final int number_iter) {
		number_iterations = number_iter;
	}
}

