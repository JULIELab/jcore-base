/** 
 * MaxEntScorer.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 2.2
 * Since version:   1.2
 *
 * Creation date: 16.07.2007 
 * 
 * The main class for using the new, ML based scorer.
 **/

package de.julielab.jules.ae.genemapper.scoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.InstanceList.CrossValidationIterator;
import de.julielab.jules.ae.genemapper.GeneMapper;

public class MaxEntScorer extends Scorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxEntScorer.class);

	private Classifier myModel;

	MaxEntScorer() {
		// explicitely set
		myModel = null;
	}

	public MaxEntScorer(InputStream is) {
		this.myModel = loadModel(is);
	}

	public MaxEntScorer(File modelFile) {
		this.myModel = loadModel(modelFile);
	}

	/**
	 * get the score for an unlabeled pair
	 * 
	 * @param term1
	 *            term 1 (needs to be normalized!)
	 * @param term2
	 *            term 2 (needs to be normalized!)
	 * @return
	 * @throws Exception
	 */
	public double getScore(String term1, String term2) throws RuntimeException {

		if (GeneMapper.LEGACY_INDEX_SUPPORT && (null == term1 || null == term2))
			return 0;
		
		if (isPerfectMatch(term1, term2)) {
			return PERFECT_SCORE;
		}

		// get base score
		// double baseScore = (new SimpleScorer()).getScore(term1, term2);

		MaxEntScorerML maxEntML = new MaxEntScorerML();

		if (this.myModel == null) {
			RuntimeException e = new IllegalStateException("Model not initialised!");
			LOGGER.error("",e);
			throw (e);
		}

		Instance inst = this.myModel.getInstancePipe().instanceFrom(
						new Instance(new String[] { term1, term2, "FALSE" }, "", "", ""));
		double predValue = maxEntML.predict(inst, myModel);

		// Instance inst = maxEntML.makeInstance(new String[] { term1, term2,
		// "FALSE" }, myModel.getInstancePipe());

		// System.out.println("[MaxEntScorer] comparing: " + term1 + " <-> " + term2 + ": " +
		// predValue);

		return predValue;

	}

	/*
	 * helper functions
	 */

	/**
	 * train a model
	 * 
	 * @param trueList
	 * @param amountTrue
	 * @param completeList
	 * @param ratioFalse
	 * @param modelFile
	 */
	void trainModel(File trueList, int amountTrue, File completeList, float ratioFalse, String modelFile) {
		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		MaxEntScorerML ml = new MaxEntScorerML();
		ArrayList<String[]> pairs = pairEx.getPairs(trueList, completeList, amountTrue, ratioFalse);

		InstanceList iList = ml.makeInstances(pairs);
		Classifier model = ml.train(iList);
		writeModel(model, modelFile);
	}

	/**
	 * train a model and write model to file
	 * 
	 * @param list
	 *            list with training examples (pairs and their label)
	 */
	void trainModel(File list, String modelFile) {
		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		MaxEntScorerML ml = new MaxEntScorerML();
		ArrayList<String[]> pairs = pairEx.readList(list);
		InstanceList iList = ml.makeInstances(pairs);
		Classifier model = ml.train(iList);
		writeModel(model, modelFile);
	}

	/**
	 * train model only
	 * 
	 * @param list
	 */
	void trainModel(File list) {
		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		MaxEntScorerML ml = new MaxEntScorerML();
		ArrayList<String[]> pairs = pairEx.readList(list);
		InstanceList iList = ml.makeInstances(pairs);
		myModel = ml.train(iList);
	}

	/**
	 * write all pairs into a file
	 * 
	 * @param trueList
	 * @param amountTrue
	 * @param completeList
	 * @param ratioFalse
	 * @param modelFile
	 */
	void pairsOut(File trueList, int amountTrue, File completeList, float ratioFalse, String pairFile) {
		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		ArrayList<String[]> pairs = pairEx.getPairs(trueList, completeList, amountTrue, ratioFalse);

		try {
			pairEx.storePairs(pairs, new File(pairFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * do an evaluation on test file after training a model. a confusion matrix and the accuracy is
	 * printed.
	 * 
	 * @param trainFile
	 *            data to train the model
	 * @param testFile
	 *            data to test the model
	 */
	void getConfusionMatrix(File trainFile, File testFile) {
		trainModel(trainFile);

		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		MaxEntScorerML ml = new MaxEntScorerML();

		ArrayList<String[]> list = pairEx.readList(testFile);
		InstanceList iList = ml.makeInstances(list, myModel.getInstancePipe());

		Trial trial = new Trial(myModel, iList);
		ConfusionMatrix c = new ConfusionMatrix(trial);
		System.out.println(c.toString());
	}

	void crossValidation(File list, int folds) {
		System.out.println("doing cross-validation with " + folds + " folds");
		double[] acc = new double[folds];
		double accSum = 0;

		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();
		MaxEntScorerML ml = new MaxEntScorerML();
		ArrayList<String[]> pairs = pairEx.readList(list);
		InstanceList iList = ml.makeInstances(pairs);
		CrossValidationIterator cross = iList.crossValidationIterator(folds, (new Random(System.currentTimeMillis()))
						.nextInt());

		int round = 0;
		while (cross.hasNext()) {
			System.out.println("@ round: " + round);

			InstanceList[] crossLists = cross.nextSplit();
			InstanceList train = crossLists[0];
			InstanceList test = crossLists[1];

			Classifier model = ml.train(train);
			Trial trial = new Trial(model, test);
			acc[round] = trial.getAccuracy();
			accSum += trial.getAccuracy();

			ConfusionMatrix confMatrix = new ConfusionMatrix(trial);
			System.out.println(confMatrix.toString());

			round++;
		}

		DecimalFormat df = new DecimalFormat("0.000");
		System.out.println("\n====== FINAL RESULT ======");
		double avgAcc = accSum / (double) folds;
		for (int i = 0; i < acc.length; i++) {
			System.out.println("round " + i + ": " + acc[i]);
		}
		System.out.println("\n--------------------------");
		System.out.println("overall accuracy: " + df.format(avgAcc));
		System.out.println("\n==========================\n");
	}

	/**
	 * evaluate a trained model against a list of pairs and their correct labels
	 * 
	 * @param modelFile
	 * @param testList
	 */
	void evalModel(File modelFile, File testList) {
		MaxEntScorerML ml = new MaxEntScorerML();
		MaxEntScorerPairExtractor pairEx = new MaxEntScorerPairExtractor();

		Classifier model = loadModel(modelFile);
		ArrayList<String[]> list = pairEx.readList(testList);
		InstanceList iList = ml.makeInstances(list, model.getInstancePipe());
		ml.eval(model, iList);
	}

	/**
	 * @deprecated get scores for a list of pairs
	 * @param pairList
	 * @return
	 * @throws Exception
	 */
	/*
	 * double[] getScore(ArrayList<String[]> pairList) throws Exception{
	 * 
	 * System.out.println("\n\n getting scores for pairlist\n\n"); double[] bestValues = new
	 * double[pairList.size()];
	 * 
	 * int i=0; for(String[] pair: pairList) { bestValues[i] = getScore(pair[0], pair[1]); i++; }
	 * return bestValues; }
	 */

	/**
	 * store the model in a file
	 * 
	 * @param model
	 * @param modelFile
	 */
	void writeModel(Classifier model, String modelFile) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(modelFile)));
			out.writeObject(model);
			out.flush();
			out.close();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	Classifier loadModel(InputStream s) {
		Pipe dummyPipe = new SerialPipes();
		dummyPipe.setDataAlphabet(new Alphabet());
		dummyPipe.setTargetAlphabet(new Alphabet());
		Classifier model = new MaxEnt(dummyPipe, new double[] { 1.0 });
		

		try {
			ObjectInputStream in = new ObjectInputStream(s);
			model = (Classifier) in.readObject();
			in.close();
		} catch (IOException io) {
			io.printStackTrace();
		} catch (ClassNotFoundException nf) {
			nf.printStackTrace();
		}
		model.getInstancePipe().getDataAlphabet().stopGrowth();
		return model;
	}

	/**
	 * load the model from file and store it as an object variable
	 * 
	 * TODO: this has to be set up, so we won't get a NullPointerException in MaxEnt (especially the
	 * setDataAlphabet part)
	 * 
	 * @return
	 */
	Classifier loadModel(File modelFile) {
		Pipe dummyPipe = new SerialPipes();
		dummyPipe.setDataAlphabet(new Alphabet());
		Classifier model = new MaxEnt(dummyPipe, new double[] { 1.0 });

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			model = (Classifier) in.readObject();
			in.close();
		} catch (IOException io) {
			io.printStackTrace();
		} catch (ClassNotFoundException nf) {
			nf.printStackTrace();
		}

		model.getInstancePipe().getDataAlphabet().stopGrowth();
		return model;
	}

	public static void main(String[] args) {
		MaxEntScorer scorer = new MaxEntScorer();

		if (args.length==0) {
			System.err.println("call with parameters!");
			System.err.println("-train: to train a model");
			System.err.println("-eval: evaluate a trained model");
			System.err.println("-conf: to get confusion matrix");
			System.err.println("-xval: for cross-validation");
			System.err.println("-score: score single example");
			System.err.println("-true: make truelist");
			System.err.println("-false: make only negative examples");
			System.err.println("-justpairs: ");
			System.exit(-1);
		} else if (args.length > 0) {

			// train
			if (args[0].equals("-train")) {
				if (args.length == 3) {
					File list = new File(args[1]);
					String modelFile = args[2];
					if (list.isFile()) {
						scorer.trainModel(list, modelFile);
					} else {
						System.err.println("One or more files not found!");
					}
				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usage: <trainDataFile> <modelOutFile>");

				}

				// eval
			} else if (args[0].equals("-eval")) {
				if (args.length == 3) {
					File modelFile = new File(args[1]);
					File testList = new File(args[2]);
					scorer.evalModel(modelFile, testList);
				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usagee: <model> <testlist>");
				}
				// confusion matrix
			} else if (args[0].equals("-conf")) {
				if (args.length == 3) {
					File trainList = new File(args[1]);
					File testList = new File(args[2]);
					scorer.getConfusionMatrix(trainList, testList);
				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usage: <training data> <test data>");
				}
				// cross validation
			} else if (args[0].equals("-xval")) {
				if (args.length == 3) {
					File list = new File(args[1]);
					int folds = (new Integer(args[2])).intValue();
					scorer.crossValidation(list, folds);
				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usagee: <data file> <folds>");
				}
				// score single pair
			} else if (args[0].equals("-score")) {
				if (args.length == 4) {
					MaxEntScorer myScorer = new MaxEntScorer(new File(args[1]));
					try {
						double eval = myScorer.getScore(args[2], args[3]);
						System.out.println(eval + ": " + args[2] + " <-> " + args[3]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					System.err.println("Too many/few parameters: <model> <term1> <term2>");
				}
				// make truelist
			} else if (args[0].equals("-true")) {
				if (args.length == 3) {
					File completeList = new File(args[1]);
					File storeList = new File(args[2]);
					MaxEntScorerPairExtractor ex = new MaxEntScorerPairExtractor();

					ex.makeTrueList(completeList, storeList);

				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usage: <completeList> <storeFile>");
				}
				// make only negative examples
			} else if (args[0].equals("-false")) {
				if (args.length == 3) {
					File completeList = new File(args[1]);
					File storeList = new File(args[2]);
					MaxEntScorerPairExtractor ex = new MaxEntScorerPairExtractor();

					ex.makeFalseList(completeList, storeList);

				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err.println("Usage: <completeList> <storeFile>");
				}
			} else if (args[0].equals("-justpairs")) {
				// make pairs and write them to file
				// arguments: trueList, amountTrue, completeList, ratioFalse,
				// pairsOutFile
				if (args.length == 6) {
					File trueList = new File(args[1]);
					int amountTrue = Integer.parseInt(args[2]);
					File completeList = new File(args[3]);
					float ratioFalse = Float.parseFloat(args[4]);
					String outputFile = args[5];
					if (trueList.isFile() && completeList.isFile()) {
						scorer.pairsOut(trueList, amountTrue, completeList, ratioFalse, outputFile);
					} else {
						System.err.println("files not existing");
					}
				} else {
					System.err.println("Too many/few parameters: " + args.length);
					System.err
									.println("Usage:  <trueListShuffled> <amountTrue> <completeListShuffled> <ratioFalse> <outputFile>");
				}
			} else {
				System.err.println("Unrecognized command!");
			}
		}
	}

	public String info() {
		return "MaxEntScorer";
	}

	@Override
	public int getScorerType() {
		return GeneMapper.MAXENT_SCORER;
	}
}