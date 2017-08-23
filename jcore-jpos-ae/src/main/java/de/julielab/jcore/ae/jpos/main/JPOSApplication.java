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
package de.julielab.jcore.ae.jpos.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import de.julielab.jcore.ae.jpos.tagger.POSTagger;
import de.julielab.jcore.ae.jpos.tagger.Sentence;
import de.julielab.jcore.ae.jpos.tagger.Unit;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Command line application
 */

public class JPOSApplication {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {

		// System.out.println(" * running JulieTagger...");

		final long startTime = System.currentTimeMillis();

		if (args.length < 1) {
			System.err.println("usage: <mode> <mode-specific-parameters>");
			showModes();
			System.exit(-1);
		}

		final String mode = args[0];

		if (mode.equals("x")) {
			/*
			 * x-validation
			 */
			if (args.length < 4) {
				System.err
				.println("usage: x <trainData> <x-rounds> <featureConfigFile> [number of iterations]");
				System.err.println("pred-out format: token pred gold");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final int rounds = (new Integer(args[2])).intValue();
			final File featureConfigFile = new File(args[3]);
			int number_iter = 0;
			if (args.length == 5)
				number_iter = (new Integer(args[4])).intValue();
			final boolean max_ent = true;

			evalXVal(trainFile, rounds, featureConfigFile, number_iter, max_ent);

		} else if (mode.equals("t")) {
			/*
			 * train
			 */
			if (args.length < 4) {
				System.err
				.println("usage: t <trainData> <model-out-file> <featureConfigFile> [number of iterations]");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File modelFile = new File(args[2]);
			File featureConfigFile = null;
			int number_iter = 0; // = unlimited
			featureConfigFile = new File(args[3]);

			if (args.length == 5)
				number_iter = (new Integer(args[4])).intValue();
			train(trainFile, modelFile, featureConfigFile, number_iter);

		} else if (mode.equals("p")) {
			/*
			 * predict
			 */

			if (args.length != 4) {
				System.err
				.println("usage: p <unlabeled data> <modelFile> <outFile>");
				System.exit(-1);
			}

			final File trainFile = new File(args[1]);
			final File modelFile = new File(args[2]);
			final File outFile = new File(args[3]);
			predict(trainFile, modelFile, outFile);

		} else if (mode.equals("c")) {
			/*
			 * compare gold and prediction
			 */

			if (args.length != 3) {

				System.err
				.println("\ncompares the gold standard agains the prediction");
				System.err.println("\nusage: c <predData> <goldData>");
				System.exit(-1);
			}

			final File predFile = new File(args[1]);
			final File goldFile = new File(args[2]);
			compare(predFile, goldFile);

		} else if (mode.equals("oc")) {
			if (args.length != 2) {
				System.err.println("\nusage: oc <model>");
				System.exit(-1);
			}

			final File modelFile = new File(args[1]);
			printFeatureConfig(modelFile);

		} else if (mode.equals("ts")) {
			if (args.length != 2) {
				System.err.println("\nusage: ts <model>");
				System.exit(-1);
			}

			final File modelFile = new File(args[1]);
			printTagset(modelFile);
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
		System.err.println("x: cross validation ");
		System.err.println("c: compare goldstandard and prediction");
		System.err.println("t: train ");
		System.err.println("p: predict ");
		System.err.println("oc: output model configuration ");
		System.err.println("ts: output model tagset");
		System.exit(-1);
	}

	/**
	 * trains a model and stores it to the the file 'outFile'
	 *
	 * @throws IOException
	 */
	static void train(final File trainFile, final File outFile,
			final File featureConfigFile, final int number_iter)
					throws IOException {
		final List<String> ppdSentences = Files.readLines(trainFile,
				Charsets.UTF_8);
		final ArrayList<Sentence> sentences = new ArrayList<>();

		POSTagger tagger;
		if (featureConfigFile != null)
			tagger = new POSTagger(featureConfigFile);
		else
			tagger = new POSTagger();
		tagger.set_Number_Iterations(number_iter);

		for (final String ppdSentence : ppdSentences)
			sentences.add(tagger.PPDtoUnits(ppdSentence));
		tagger.train(sentences);
		tagger.writeModel(outFile.toString());
	}

	/**
	 * performs a 'n'-fold-cross-validation on a list of sentence strings in
	 * piped format, like "this|o is|o a|o NE|i"
	 *
	 * The pred files lists token\tprediction\tgold\tPOS
	 *
	 * @throws IOException
	 */
	public static void evalXVal(final File dataFile, final int n,
			final File featureConfigFile, final int number_iter,
			final boolean maxEnt) throws IOException {
		final List<String> ppdData = Files.readLines(dataFile, Charsets.UTF_8);
		final DecimalFormat df = new DecimalFormat("0.000");

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

		final double[] accuracies = new double[n];
		for (int i = 0; i < n; i++) { // in each round

			final ArrayList<String> ppdTrainData = new ArrayList<String>();
			final ArrayList<String> ppdTestData = new ArrayList<String>();

			if (i == (n - 1)) {
				// last round

				for (int j = 0; j < ppdData.size(); j++)
					if (j < pos)
						ppdTrainData.add(ppdData.get(j));
					else
						ppdTestData.add(ppdData.get(j));

			} else {
				// other rounds
				for (int j = 0; j < ppdData.size(); j++)
					if ((j < pos) || (j >= (pos + sizeRound)))
						ppdTrainData.add(ppdData.get(j));
					else
						ppdTestData.add(ppdData.get(j));
				pos += sizeRound;
			}

			System.out.println(" * training on: " + ppdTrainData.size()
					+ " -- testing on: " + ppdTestData.size());

			final double eval = eval(ppdTrainData, ppdTestData,
					featureConfigFile, number_iter, i);
			accuracies[i] = eval;
			System.out.println("\n** round " + (i + 1) + "\tAccuracy: "
					+ df.format(eval));

		}

		// now get average performance
		final double avgAcc = getAverage(accuracies);

		final double stdAcc = getStandardDeviation(accuracies, avgAcc);
		final StringBuffer summary = new StringBuffer();

		summary.append("Cross-validation results:\n");
		summary.append("Number of sentences in evaluation data set: " + sizeAll
				+ "\n");
		summary.append("Number of sentences for training in each/last round: "
				+ sizeRound + "/" + sizeLastRound + "\n\n");
		summary.append("Overall performance: avg (standard deviation)\n");
		summary.append("Accuracy: " + df.format(avgAcc) + "("
				+ df.format(stdAcc) + ")\n");

		System.out.println("\n\nCross-validation finished");
		System.out.println(summary);
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

	static void predict(final File testDataFile, final File modelFile,
			final File outFile) throws Exception {
		final List<String> testData = Files.readLines(testDataFile,
				Charsets.UTF_8);
		final ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		ArrayList<String> results;
		final POSTagger tagger = POSTagger.readModel(modelFile);
		try {
			System.out.println("  * predicting...");
			final long t1 = System.currentTimeMillis();

			final FileWriter fw = new FileWriter(outFile);

			for (final String sentence : testData)
				sentences.add(tagger.textToUnits(sentence));

			results = tagger.predictForCLI(sentences);
			for (final String result : results)
				fw.write(result);

			final long t2 = System.currentTimeMillis();
			System.out.println("prediction took: " + (t2 - t1));
			fw.close();
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
	static double eval(final ArrayList<String> ppdTrainData,
			final ArrayList<String> ppdTestData, final File featureConfigFile,
			final int number_iter, final int numXval) {

		// train
		final ArrayList<Sentence> trainSentences = new ArrayList<Sentence>();
		final ArrayList<Sentence> testSentences = new ArrayList<Sentence>();
		POSTagger tagger;
		if (featureConfigFile != null)
			tagger = new POSTagger(featureConfigFile);
		else
			tagger = new POSTagger();

		tagger.set_Number_Iterations(number_iter);

		// converting Strings -> Units
		for (final String ppdTrainSentence : ppdTrainData)
			trainSentences.add(tagger.PPDtoUnits(ppdTrainSentence));
		for (final String ppdTestSentence : ppdTestData)
			testSentences.add(tagger.PPDtoUnits(ppdTestSentence));
		tagger.train(trainSentences);

		// get test data in jpos format
		final ArrayList<String> gold = new ArrayList<String>();
		for (int i = 0; i < testSentences.size(); i++) {
			final Sentence sentence = testSentences.get(i);
			for (final Unit unit : sentence.getUnits())
				gold.add(unit.getRep() + "|" + unit.getLabel());
		}

		final ArrayList<String> pred = new ArrayList<String>();
		for (final String predictedSentence : tagger
				.predictForCLI(testSentences))
			for (final String predictedTag : predictedSentence.trim()
					.split(" "))
				pred.add(predictedTag);

		// calculate performance

		double correct = 0;
		if (pred.size() != gold.size())
			throw new RuntimeException();
		for (int i = 0; i < gold.size(); ++i)
			if (pred.get(i).replaceAll(".*\\|", "")
					.equals(gold.get(i).replaceAll(".*\\|", "")))
				++correct;
			else
				System.out.println("Predicted:\t" + pred.get(i) + "\tCorrect: "
						+ gold.get(i));
		return correct / gold.size();
	}

	/**
	 * compares prediction with separate gold standard and calculates accuracy
	 *
	 * @param goldFile
	 *            gold standard
	 * @param predFile
	 *            prediction
	 * @throws IOException
	 */
	static void compare(final File predFile, final File goldFile)
			throws IOException {
		final List<String> gold = Files.readLines(goldFile, Charsets.UTF_8);
		final List<String> pred = Files.readLines(predFile, Charsets.UTF_8);

		if (gold.size() != pred.size()) {
			System.err
			.println("ERR: number of lines in gold standard is different from prediction... please check!");
			System.exit(-1);
		}
		int correct = 0;
		int seen = 0;
		for (int i = 0; i < gold.size(); ++i) {
			final String[] goldToken = gold.get(i).split(" +");
			final String[] predToken = pred.get(i).split(" +");
			if (goldToken.length != predToken.length) {
				System.err
				.println("ERR: number of tokens in gold standard is different from prediction for\n"
						+ goldToken + "\n" + predToken);
				System.exit(-1);
			}
			for (int j = 0; j < goldToken.length; ++j) {
				seen += 1;
				if (goldToken[j].replaceAll(".*\\|", "").equals(
						predToken[j].replaceAll(".*\\|", "")))
					correct += 1;
			}
		}

		System.out.println("Correct: " + correct);
		System.out.println("Seen: " + seen);
		System.out.println("Accuracy: " + ((double) correct / (double) seen));
	}

	/**
	 * prints out the feature configuration used in the model 'modelFile'
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws FileNotFoundException
	 */
	public static void printFeatureConfig(final File modelFile)
			throws FileNotFoundException, ClassNotFoundException, IOException {
		Properties featureConfig;
		final POSTagger tagger = POSTagger.readModel(modelFile);

		featureConfig = tagger.getFeatureConfig();
		final Enumeration<?> keys = featureConfig.propertyNames();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			System.out.printf("%s = %s\n", key, featureConfig.getProperty(key));
		}
	}

	/**
	 * prints out the tagset used in the model 'modelFile'
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws FileNotFoundException
	 */
	public static void printTagset(final File modelFile)
			throws FileNotFoundException, ClassNotFoundException, IOException {
		Object model;
		final POSTagger tagger = POSTagger.readModel(modelFile);

		model = tagger.getModel();
		final Alphabet alpha = ((CRF) model).getOutputAlphabet();
		final Object modelLabels[] = alpha.toArray();
		for (final Object modelLabel : modelLabels)
			System.out.println(modelLabel);
	}
}
