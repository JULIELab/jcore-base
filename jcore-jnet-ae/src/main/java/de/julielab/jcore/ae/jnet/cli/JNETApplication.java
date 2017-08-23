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
package de.julielab.jcore.ae.jnet.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import cc.mallet.fst.CRF;
import cc.mallet.types.Alphabet;
import de.julielab.jcore.ae.jnet.utils.FormatConverter;
import de.julielab.jcore.ae.jnet.utils.IOBEvaluation;
import de.julielab.jcore.ae.jnet.utils.IOEvaluation;
import de.julielab.jcore.ae.jnet.utils.Utils;
import de.julielab.jnet.tagger.NETagger;
import de.julielab.jnet.tagger.Sentence;
import de.julielab.jnet.tagger.Tags;
import de.julielab.jnet.tagger.Unit;

/**
 * Command line application
 */

public class JNETApplication {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		// System.out.println(" * running JulieTagger...");

		final long startTime = System.currentTimeMillis();

		if (args.length < 1) {
			System.err
					.println("usage: JNETApplication <mode> <mode-specific-parameters>");
			showModes();
			System.exit(-1);
		}

		final String mode = args[0];

		if (mode.equals("f")) {
			/*
			 * FormatConverter
			 */

			if (args.length < 4) {
				System.out
						.println("usage: JNETApplication f <iobFile> <1st meta data file> [further meta data files] <outFile> <taglist (or 0 if not used)>");
				System.exit(0);
			}
			final String[] converterArgs = new String[args.length - 1];
			for (int i = 1; i < args.length; i++)
				converterArgs[i - 1] = args[i];
			FormatConverter.main(converterArgs);
		} else if (mode.equals("s")) {
			/*
			 * 90-10 split
			 */

			if (args.length < 4) {
				System.err
						.println("usage: JNETApplication s <data.ppd> <tags.def> <pred-out> [featureConfigFile] [number of iterations]");
				System.err.println("pred-out format: token pred gold");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File tagsFile = new File(args[2]);
			final File predFile = new File(args[3]);
			File featureConfigFile = null;
			int number_iter = 0;
			boolean max_ent = false;

			if (args.length == 5)
				featureConfigFile = new File(args[4]);

			if (args.length == 6) {
				featureConfigFile = new File(args[4]);
				number_iter = (new Integer(args[5])).intValue();
			}

			if (args.length == 7) {
				featureConfigFile = new File(args[4]);
				number_iter = (new Integer(args[5])).intValue();
				max_ent = new Boolean(args[6]).booleanValue();
			}

			eval9010(trainFile, tagsFile, predFile, featureConfigFile,
					number_iter, max_ent);

		} else if (mode.equals("x")) {
			/*
			 * x-validation
			 */
			if (args.length < 6) {
				System.err
						.println("usage: JNETApplication x <trainData.ppd> <tags.def> <pred-out> <x-rounds> <performance-out-file> [featureConfigFile] [number of iterations]");
				System.err.println("pred-out format: token pred gold");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File tagsFile = new File(args[2]);
			final File predFile = new File(args[3]);
			final int rounds = (new Integer(args[4])).intValue();
			final File performanceOutFile = new File(args[5]);
			File featureConfigFile = null;
			int number_iter = 0;
			boolean max_ent = false;

			if (args.length == 7)
				featureConfigFile = new File(args[6]);

			if (args.length == 8) {
				featureConfigFile = new File(args[6]);
				number_iter = (new Integer(args[7])).intValue();
			}

			if (args.length == 9) {
				featureConfigFile = new File(args[6]);
				number_iter = (new Integer(args[7])).intValue();
				max_ent = new Boolean(args[8]).booleanValue();
			}

			evalXVal(trainFile, tagsFile, rounds, predFile, performanceOutFile,
					featureConfigFile, number_iter, max_ent);

		} else if (mode.equals("t")) {
			/*
			 * train
			 */
			if (args.length < 3) {
				System.err
						.println("usage: JNETApplication t <trainData.ppd> <model-out-file> [featureConfigFile] [number of iterations]");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File modelFile = new File(args[2]);
			File featureConfigFile = null;
			int number_iter = 0;
			boolean max_ent = false;

			if (args.length == 4)
				featureConfigFile = new File(args[3]);
			if (args.length == 5) {
				featureConfigFile = new File(args[3]);
				number_iter = (new Integer(args[4])).intValue();
			}

			if (args.length == 6) {
				featureConfigFile = new File(args[3]);
				number_iter = (new Integer(args[4])).intValue();
				max_ent = new Boolean(args[5]).booleanValue();
			}
			train(trainFile, modelFile, featureConfigFile, number_iter, max_ent);

		} else if (mode.equals("p")) {
			/*
			 * predict
			 */

			if (args.length != 5) {
				System.err
						.println("usage: JNETApplication p <unlabeled data.ppd> <modelFile> <outFile> <estimate segment conf>");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File modelFile = new File(args[2]);
			final File outFile = new File(args[3]);
			final boolean conf = new Boolean(args[4]);
			predict(trainFile, modelFile, outFile, conf);

		} else if (mode.equals("c")) {
			/*
			 * compare gold and prediction
			 */

			if (args.length != 4) {

				System.err
						.println("\ncompares the gold standard agains the prediction: "
								+ "give both IOB files, they must have the same length!");

				System.err
						.println("\nusage: JNETApplication c <predData.iob> <goldData.iob> <tag.def>");
				System.exit(-1);
			}

			final File predFile = new File(args[1]);
			final File goldFile = new File(args[2]);
			final File tagsFile = new File(args[3]);
			final double[] eval = compare(predFile, goldFile, tagsFile);
			System.out.println(eval[0] + "\t" + eval[1] + "\t" + eval[2]); // (R/P/F)

		} else if (mode.equals("oc")) {
			/*
			 * output properties
			 */

			if (args.length != 2) {
				System.err.println("\nusage: JNETApplication oc <model>");
				System.exit(-1);
			}

			final File modelFile = new File(args[1]);
			printFeatureConfig(modelFile);

		} else if (mode.equals("oa")) {
			/*
			 * output output alphabet
			 */

			if (args.length != 2) {
				System.err.println("\nusage: JNETApplication oa <model>");
				System.exit(-1);
			}

			final File modelFile = new File(args[1]);
			printOutputAlphabet(modelFile);

		} else {
			System.err.println("ERR: unknown mode");
			showModes();
			System.exit(-1);
		}
		final long timeNeeded = ((System.currentTimeMillis() - startTime) / 1000) / 60;
		System.out.println("Finished in " + timeNeeded + " minutes");
	}

	static void showModes() {

		System.err.println("\nAvailable modes:");
		System.err.println("f: converting multiple annotations to one file");
		System.err.println("s: 90-10 split evaluation");
		System.err.println("x: cross validation ");
		System.err.println("c: compare goldstandard and prediction");
		System.err.println("t: train ");
		System.err.println("p: predict ");
		System.err.println("oc: output model configuration ");
		System.err.println("oa: output the model's output alphabet ");

		System.exit(-1);
	}

	/**
	 * trains a model and stores it to the the file 'outFile'
	 */
	static void train(final File trainFile, final File outFile,
			final File featureConfigFile, final int number_iter,
			final boolean maxEnt) {
		final ArrayList<String> ppdSentences = Utils.readFile(trainFile);
		final ArrayList<Sentence> sentences = new ArrayList<Sentence>();

		NETagger tagger;
		if (featureConfigFile != null)
			tagger = new NETagger(featureConfigFile);
		else
			tagger = new NETagger();
		tagger.set_Number_Iterations(number_iter);
		tagger.set_Max_Ent(maxEnt);

		for (final String ppdSentence : ppdSentences)
			sentences.add(tagger.PPDtoUnits(ppdSentence));
		tagger.train(sentences);
		tagger.writeModel(outFile.toString());
	}

	/**
	 * performs a 'n'-fold-cross-validation on 'dataFile'
	 */
	static void evalXVal(final File dataFile, final File tagsFile, final int n,
			final File predictionOutFile, final File performanceOutFile,
			final File featureConfigFile, final int number_iter,
			final boolean maxEnt) {
		final List<String> ppdData = Utils.readFile(dataFile);
		evalXVal(ppdData, tagsFile, n, predictionOutFile, performanceOutFile,
				featureConfigFile, number_iter, maxEnt);
	}

	/**
	 * performs a 'n'-fold-cross-validation on a list of sentence strings in
	 * piped format, like "this|o is|o a|o NE|i"
	 * 
	 * The pred files lists token\tprediction\tgold\tPOS
	 */
	public static void evalXVal(final List<String> ppdData,
			final File tagsFile, final int n, final File predictionOutFile,
			final File performanceOutFile, final File featureConfigFile,
			final int number_iter, final boolean maxEnt) {

		final ArrayList<String> output = new ArrayList<String>(); // for output
																	// of
		// gold standard and
		// prediction in
		// outFile, created
		// by eval method

		final Tags tags = new Tags(tagsFile.toString());

		final long seed = 1;
		Collections.shuffle(ppdData, new Random(seed));

		int pos = 0;
		final int sizeRound = ppdData.size() / n;
		final int sizeAll = ppdData.size();
		final int sizeLastRound = sizeRound + (sizeAll % n);
		System.out.println(" * number of sentences: " + sizeAll);
		System.out.println(" * size of each/last round: " + sizeRound + "/"
				+ sizeLastRound);
		System.out.println();

		final double[] fscores = new double[n];
		final double[] recalls = new double[n];
		final double[] precisions = new double[n];

		for (int i = 0; i < n; i++) { // in each round

			final ArrayList<String> ppdTrainData = new ArrayList<String>();
			final ArrayList<String> ppdTestData = new ArrayList<String>();

			if (i == (n - 1)) {
				// last round

				for (int j = 0; j < ppdData.size(); j++)
					if (j < pos) {
						ppdTrainData.add(ppdData.get(j));
					} else {
						ppdTestData.add(ppdData.get(j));
					}

			} else {
				// other rounds
				for (int j = 0; j < ppdData.size(); j++)
					if ((j < pos) || (j >= (pos + sizeRound))) {
						ppdTrainData.add(ppdData.get(j));
					} else {
						ppdTestData.add(ppdData.get(j));
					}
				pos += sizeRound;
			}

			System.out.println(" * training on: " + ppdTrainData.size()
					+ " -- testing on: " + ppdTestData.size());

			// eval-array: R: eval[0], P: eval[1], F: eval[2]

			final double[] eval = eval(ppdTrainData, ppdTestData, tags, output,
					featureConfigFile, number_iter, maxEnt);

			recalls[i] = eval[0];
			precisions[i] = eval[1];
			fscores[i] = eval[2];

			System.out.println("\n** round " + (i + 1) + ": R/P/F: " + eval[0]
					+ "/" + eval[1] + "/" + eval[2]);

		}

		// now get average performance
		final double avgRecall = getAverage(recalls);
		final double avgPrecision = getAverage(precisions);
		final double avgFscore = getAverage(fscores);

		final double stdRecall = getStandardDeviation(recalls, avgRecall);
		final double stdPrecision = getStandardDeviation(precisions,
				avgPrecision);
		final double stdFscore = getStandardDeviation(fscores, avgFscore);

		final DecimalFormat df = new DecimalFormat("0.000");
		final StringBuffer summary = new StringBuffer();

		summary.append("Cross-validation results:\n");
		summary.append("Number of sentences in evaluation data set: " + sizeAll
				+ "\n");
		summary.append("Number of sentences for training in each/last round: "
				+ sizeRound + "/" + sizeLastRound + "\n\n");
		summary.append("Overall performance: avg (standard deviation)\n");
		summary.append("Recall: " + df.format(avgRecall) + "("
				+ df.format(stdRecall) + ")\n");
		summary.append("Precision: " + df.format(avgPrecision) + "("
				+ df.format(stdPrecision) + ")\n");
		summary.append("F1-Score: " + df.format(avgFscore) + "("
				+ df.format(stdFscore) + ")\n");

		// write performance
		Utils.writeFile(performanceOutFile, summary.toString());

		// write prediction
		Utils.writeFile(predictionOutFile, output);

		System.out
				.println("\n\nCross-validation finished. Results written to: "
						+ performanceOutFile);
		System.out.println(summary.toString());
	}

	public static double getStandardDeviation(final double[] values,
			final double avg) {
		double sum = 0;
		for (final double value : values)
			sum += Math.pow((value - avg), 2);
		return Math.sqrt(sum / ((double) values.length - 1));
	}

	public static double getAverage(final double[] values) {
		double sum = 0;
		for (final double value : values)
			sum += value;
		return (sum / values.length);
	}

	/**
	 * 
	 * @param dataFile
	 *            in pipedformat, must have entity labels
	 * @param tagsFile
	 * @param err
	 * @param pred
	 */
	static void eval9010(final File dataFile, final File tagsFile,
			final File outFile, final File featureConfigFile,
			final int number_iter, final boolean maxEnt) {

		final ArrayList<String> output = new ArrayList<String>(); // for output
																	// of
		// gold standard and
		// prediction in
		// outFile, created
		// by eval method

		final Tags tags = new Tags(tagsFile.toString());
		final ArrayList<String> ppdData = Utils.readFile(dataFile);
		final long seed = 1;
		Collections.shuffle(ppdData, new Random(seed));

		final int sizeAll = ppdData.size();
		final int sizeTest = (int) (sizeAll * 0.1);
		final int sizeTrain = sizeAll - sizeTest;

		if (sizeTest == 0) {
			System.err.println("Error: no test files for this split.");
			System.exit(-1);
		}
		System.out.println(" * all: " + sizeAll + "\ttrain: " + sizeTrain
				+ "\t" + "test: " + sizeTest);

		final ArrayList<String> ppdTrainData = new ArrayList<String>();
		final ArrayList<String> ppdTestData = new ArrayList<String>();

		for (int i = 0; i < ppdData.size(); i++)
			if (i < sizeTrain)
				ppdTrainData.add(ppdData.get(i));
			else
				ppdTestData.add(ppdData.get(i));

		System.out.println(" * training on: " + ppdTrainData.size()
				+ " -- testing on: " + ppdTestData.size());

		final double[] eval = eval(ppdTrainData, ppdTestData, tags, output,
				featureConfigFile, number_iter, maxEnt);
		final DecimalFormat df = new DecimalFormat("0.000");
		System.out.println("\n\n** R/P/F: " + df.format(eval[0]) + "/"
				+ df.format(eval[1]) + "/" + df.format(eval[2]));

		// write prediction and gold standard to outfile
		Utils.writeFile(outFile, output);

	}

	static void predict(final File testDataFile, final File modelFile,
			final File outFile, final boolean showSegmentConfidence) {
		final ArrayList<String> ppdTestData = Utils.readFile(testDataFile);
		final ArrayList<Sentence> sentences = new ArrayList<Sentence>();

		final NETagger tagger = new NETagger();

		try {
			tagger.readModel(modelFile);

			for (final String ppdSentence : ppdTestData)
				sentences.add(tagger.PPDtoUnits(ppdSentence));

			Utils.writeFile(outFile,
					tagger.predictIOB(sentences, showSegmentConfidence));
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param ppdTrainData
	 *            arraylist with sentences in pipedformat
	 * @param ppdTestData
	 *            arraylist with sentences in pipedformat
	 * @param tags
	 *            the tags to be used
	 * @param pred
	 *            arraylist with iob of predictions
	 * @param output
	 *            an arraylist that stores both the preiductions and the gold
	 *            standard labels along with the tokens
	 */
	static double[] eval(final ArrayList<String> ppdTrainData,
			final ArrayList<String> ppdTestData, final Tags tags,
			final ArrayList<String> output, final File featureConfigFile,
			final int number_iter, final boolean maxEnt) {

		// train
		final ArrayList<Sentence> trainSentences = new ArrayList<Sentence>();
		final ArrayList<Sentence> testSentences = new ArrayList<Sentence>();
		NETagger tagger;
		if (featureConfigFile != null)
			tagger = new NETagger(featureConfigFile);
		else
			tagger = new NETagger();

		tagger.set_Number_Iterations(number_iter);
		tagger.set_Max_Ent(maxEnt);

		// Konvertierung von Strings zu Units
		for (final String ppdTrainSentence : ppdTrainData)
			trainSentences.add(tagger.PPDtoUnits(ppdTrainSentence));
		for (final String ppdTestSentence : ppdTestData)
			testSentences.add(tagger.PPDtoUnits(ppdTestSentence));
		tagger.train(trainSentences);

		// get test data in iob format
		final ArrayList<String> pos = new ArrayList<String>();
		final ArrayList<String> gold = new ArrayList<String>();
		for (int i = 0; i < testSentences.size(); i++) {
			final Sentence sentence = testSentences.get(i);
			for (final Unit unit : sentence.getUnits()) {
				gold.add(unit.getRep() + "\t" + unit.getLabel());
				pos.add(unit.getMetaInfo(tagger.getFeatureConfig().getProperty(
						"pos_feat_unit")));
			}
			gold.add("O\tO");
			pos.add("");
		}

		tagger.predictIOB(testSentences, false);

		// get test data in iob format
		final ArrayList<String> pred = new ArrayList<String>();
		for (int i = 0; i < testSentences.size(); i++) {
			final Sentence sentence = testSentences.get(i);
			for (final Unit unit : sentence.getUnits())
				pred.add(unit.getRep() + "\t" + unit.getLabel());
			pred.add("O\tO");
		}

		// System.out.println(pred);
		// System.out.println(gold);

		// calculate performance
		double[] eval = { 0, 0, 0 };

		if (tags.type.equals("IO"))
			// IO Evaluation
			eval = IOEvaluation.evaluate(gold, pred);
		else
			// IOB Evaluation
			try {
				eval = IOBEvaluation.evaluate(gold, pred);
			} catch (final Exception e) {
				e.printStackTrace();
			}

		for (int i = 0; i < pred.size(); i++)
			output.add(pred.get(i) + "\t" + gold.get(i).split("\t")[1] + "\t"
					+ pos.get(i));

		return eval;

	}

	/**
	 * compares prediction with separate gold standard and calculates P/R/F
	 * 
	 * @param goldFile
	 *            gold standard in IOB format
	 * @param predFile
	 *            prediction in IOB format
	 * @param tags
	 *            which tags to consider for evaluation
	 * @return double array of P/R/F
	 */
	static double[] compare(final File predFile, final File goldFile,
			final File tagsFile) {
		final ArrayList<String> gold = Utils.readFile(goldFile);
		final ArrayList<String> pred = Utils.readFile(predFile);
		final Tags tags = new Tags(tagsFile.toString());

		// replace emtpy lines (end of a sentence by O\tO lines)
		for (int i = 0; i < gold.size(); i++)
			if (gold.get(i).equals(""))
				gold.set(i, "O\tO");

		// replace emtpy lines (end of a sentence by O\tO lines)
		for (int i = 0; i < pred.size(); i++)
			if (pred.get(i).equals(""))
				pred.set(i, "O\tO");

		if (gold.size() != pred.size()) {
			System.err
					.println("ERR: number of tokens/lines in gold standard is different from prediction... please check!");
			System.exit(-1);
		}

		double[] eval = { 0, 0, 0 };
		if (tags.type.equals("IO"))
			// IO Evaluation
			eval = IOEvaluation.evaluate(gold, pred);
		else
			// IOB Evaluation
			try {
				eval = IOEvaluation.evaluate(gold, pred);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		return eval;

	}

	/**
	 * prints out the feature configuration used in the model 'modelFile'
	 */
	public static void printFeatureConfig(final File modelFile) {
		Properties featureConfig;
		final NETagger tagger = new NETagger();
		try {
			tagger.readModel(modelFile);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}

		featureConfig = tagger.getFeatureConfig();
		final Enumeration<?> keys = featureConfig.propertyNames();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			System.out.printf("%s = %s\n", key, featureConfig.getProperty(key));
		}
	}

	/**
	 * prints out the tagset used in the model 'modelFile'
	 */
	public static void printOutputAlphabet(final File modelFile) {
		Object model;
		final NETagger tagger = new NETagger();
		try {
			tagger.readModel(modelFile);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}

		model = tagger.getModel();
		final Alphabet alpha = ((CRF) model).getOutputAlphabet();
		final Object modelLabels[] = alpha.toArray();
		for (final Object modelLabel : modelLabels)
			System.out.println(modelLabel);
	}
}
