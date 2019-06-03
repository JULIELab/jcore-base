/** 
 * SentenceSplitterApplication.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 * 
 * Current version: 2.0	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 **/

package de.julielab.jcore.ae.jsbd;

import cc.mallet.fst.CRF;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import de.julielab.jcore.ae.jsbd.postprocessingfilters.PostprocessingFilter;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * * The user interface (command line version) for the JULIE Sentence Boundary Detector. Includes
 * training, prediction, file format check, and evaluation.
 * 
 * When splits are done (e.g., for 90/10 or X-Val) the same randomization is enforced by seeding a
 * random number generator with 1.
 * 
 * @author tomanek
 */
public class SentenceSplitterApplication {

	private static String doPostprocessing = "biomed";

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("usage: JSBD <mode> {mode_specific_parameters}");
			System.err.println("different modes:");
			System.err.println("c: check texts");
			System.err.println("t: train a sentence splitting model");
			System.err.println("p: do the sentence splitting");
			System.err.println("s: evaluation with 90-10 split");
			System.err.println("x: evaluation with cross-validation");
			System.err.println("e: evaluation on previously trained model");
			System.exit(-1);
		}

		String mode = args[0];

		if (mode.equals("c")) { // check mode
			startCheckMode(args);

		} else if (mode.equals("t")) { // training mode
			startTrainingMode(args);

		} else if (mode.equals("p")) { // prediction mode
			startPredictionMode(args);

		} else if (mode.equals("x")) { // cross validation mode
			startXValidationMode(args);

		} else if (mode.equals("s")) { // 90-10 validation split mode
			start9010ValidationMode(args);

		} else if (mode.equals("e")) { // compare validation mode
			startCompareValidationMode(args);

		} else { // unknown mode
			System.err.println("Unknown run mode.");
			System.exit(-1);
		}

	}

	private static void startCompareValidationMode(String[] args) {
		System.out.println("performing evaluation previously trained model.");

		if (args.length < 4) {
			System.err.println("usage: JSBD e <modelFile> <predictInDir> <errorFile> [<postprocessing>]");
			System.exit(-1);
		}
		
		if (args.length > 4) {
			if (PostprocessingFilter.POSTPROC_STREAM.anyMatch(x -> args[4].equals(x))) {
				doPostprocessing = args[4];
			}
		}

		ObjectInputStream in;
		CRF crf = null;
		try {
			// load model
			in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[1])));
			crf = (CRF) in.readObject();
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		File abstractDir = new File(args[2]);
		if (!abstractDir.isDirectory()) {
			System.err.println("Error: the specified directory does not exist.");
			System.exit(-1);
		}

		File[] abstractArray = abstractDir.listFiles();
		TreeSet<String> errorList = new TreeSet<>();

		EvalResult er = doEvaluation(crf, abstractArray, errorList);
		writeFile(errorList, new File(args[3]));

		System.out.println("\n\nAccuracy on pretrained model: " + er.ACC);
		System.exit(0);
	}

	/**
	 * Entry point for 90-10 split.
	 * 
	 * @param args
	 *            the command line arguments.
	 */
	private static void start9010ValidationMode(String[] args) {
		System.out.println("performing evaluation on 90/10 split");

		if (args.length < 4) {
			System.err.println("usage: JSBD s <textDir> <errorFile> <allow split on all punctuation (false: splits only occur before whitespaces)> [<postprocessing>]");
			System.exit(-1);
		}
		
		if (args.length > 4) {
			if (PostprocessingFilter.POSTPROC_STREAM.anyMatch(x -> args[4].equals(x))) {
				doPostprocessing = args[4];
			}
		}

		File abstractDir = new File(args[1]);
		if (!abstractDir.isDirectory()) {
			System.err.println("Error: the specified directory does not exist.");
			System.exit(-1);
		}
		File[] abstractArray = abstractDir.listFiles();
		TreeSet<String> errorList = new TreeSet<>();

        boolean splitUnitsAfterPunctuation = Boolean.parseBoolean(args[3]);
        System.out.println("Allow sentence split after all punctuation: " + splitUnitsAfterPunctuation);
        EvalResult er = do9010Evaluation(abstractArray, errorList, splitUnitsAfterPunctuation);
		writeFile(errorList, new File(args[2]));

		System.out.println("\n\nAccuracy on 90/10 split: " + er.ACC);
		System.exit(0);
	}

	/**
	 * Entry point for cross validation mode
	 * 
	 * @param args
	 *            the command line mode
	 */
	private static void startXValidationMode(String[] args) {
		System.out.println("performing cross-validation");
		if (args.length < 5) {
			System.err.println("usage: JSBD x <textDir> <cross-val-rounds> <errorFile> <allow split on all punctuation (false: splits only occur before whitespaces)> [<postprocessing>]");
			System.exit(-1);
		}

		if (args.length > 5) {
			if (PostprocessingFilter.POSTPROC_STREAM.anyMatch(x -> args[5].equals(x))) {
				doPostprocessing = args[5];
			}
		}
		
		File abstractDir = new File(args[1]);
		if (!abstractDir.isDirectory()) {
			System.err.println("Error: the specified directory does not exist.");
			System.exit(-1);
		}
		File[] abstractArray = abstractDir.listFiles();
		int n = ((new Integer(args[2])).intValue());

		if (n > (abstractArray.length / 2) || n > 10 || n < 2) {
			System.err.println("Error: cannot perform " + n + " cross-validation rounds. Choose n in [2:10].");
			System.exit(-1);
		}

		TreeSet<String> errorList = new TreeSet<>();

        boolean splitUnitsAfterPunctuation = Boolean.parseBoolean(args[4]);
        System.out.println("Allowing sentence split after all punctuation: " + splitUnitsAfterPunctuation);
        double acc = doCrossEvaluation(abstractArray, n, errorList, splitUnitsAfterPunctuation);
		writeFile(errorList, new File(args[3]));

		System.out.println("\n\nAccuracy on cross validation: " + acc);
		System.exit(0);
	}

	/**
	 * Entry point for prediction mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startPredictionMode(String[] args) {
		System.out.println("doing the sentence splitting...");
		if (args.length < 4) {
			System.err.println("usage: JSBD p <inDir> <outDir> <modelFilename> [<postprocessing>]");
			System.exit(-1);
		}
		
		if (args.length > 4 && PostprocessingFilter.POSTPROC_STREAM.anyMatch(args[4]::equals)) {
				doPostprocessing = args[4];
		}

		File inDir = new File(args[1]);
		if (!inDir.isDirectory()) {
			System.err.println("Error: the specified input directory does not exist.");
			System.exit(-1);
		}
		File[] inFiles = inDir.listFiles();

		File outDir = new File(args[2]);
		if (!outDir.isDirectory()) {
			System.err.println("Error: the specified output directory does not exist.");
			System.exit(-1);
		}

		String modelFilename = args[3];
		doPrediction(inFiles, outDir, modelFilename);
	}

	/**
	 * Entry point for training mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startTrainingMode(String[] args) {
		System.out.println("training the model...");
		if (args.length != 4) {
			System.err.println("usage: JSBD t <trainDir> <allow split on all punctuation (false: splits only occur before whitespaces)> <modelFilename>");
			System.exit(-1);
		}

		File trainDir = new File(args[1]);
		if (!trainDir.isDirectory()) {
			System.err.println("Error: the specified directory does not exist.");
			System.exit(-1);
		}
		File[] trainFiles = trainDir.listFiles();

		System.out.println("number of files to train on: " + trainFiles.length);
        boolean splitUnitsAfterPunctuation = Boolean.parseBoolean((args[2]));
        System.out.println("Allow sentence split after all punctuation: " + splitUnitsAfterPunctuation);
		String modelFilename = args[3];
		doTraining(trainFiles, splitUnitsAfterPunctuation, modelFilename);

		System.out.println("Saved model to: " + modelFilename);
	}

	/**
	 * Entry point for check mode
	 * 
	 * @param args
	 *            the command line arguments
	 */
	private static void startCheckMode(String[] args) {
		System.out.println("checking abstracts...");
		if (args.length != 2) {
			System.err.println("usage: JSBD c <textDir>");
			System.exit(-1);
		}

		File abstractDir = new File(args[1]);
		if (!abstractDir.isDirectory()) {
			System.err.println("Error: the specified directory does not exist.");
			System.exit(-1);
		}
		File[] abstractArray = abstractDir.listFiles();
		// check data for validity:
		doCheckAbstracts(abstractArray, false);
		System.exit(0);
	}

	/**
	 * checks the data for validity... just for the beginning and to debug
	 * 
	 * @param abstractList
	 */
	private static void doCheckAbstracts(File[] abstractList, boolean splitUnitsAfterPunctuation) {
		SentenceSplitter tpFunctions = new SentenceSplitter();
		tpFunctions.makeTrainingData(abstractList, false, splitUnitsAfterPunctuation);

		System.out.println("done.");
	}

	/**
	 * evaluation via 90-10 split of data
	 */
	private static EvalResult do9010Evaluation(File[] abstractArray, TreeSet<String> errorList, boolean splitUnitsAfterPunctuation) {

		ArrayList<File> abstractList = new ArrayList<>();
		for (int i = 0; i < abstractArray.length; i++)
			abstractList.add(abstractArray[i]);

		Collections.shuffle(abstractList, new Random(1));

		int sizeAll = abstractList.size();
		int sizeTest = (int) (sizeAll * 0.1);
		int sizeTrain = sizeAll - sizeTest;

		if (sizeTest == 0) {
			System.err.println("Error: no test files for this split. Number of files in directory might be too small.");
			System.exit(-1);
		}
		System.out.println("all: " + sizeAll + "\ttrain: " + sizeTrain + "\t" + "test: " + sizeTest);

		File[] trainFiles = new File[sizeTrain];
		File[] predictFiles = new File[sizeTest];

		for (int i = 0; i < sizeTrain; i++)
			trainFiles[i] = abstractList.get(i);

		int j = 0;
		for (int i = sizeTrain; i < abstractList.size(); i++)
			predictFiles[j++] = abstractList.get(i);

		return doEvaluation(trainFiles, predictFiles, errorList, splitUnitsAfterPunctuation);
	}

	/**
	 * cross-evaluation, returns average accuracy
	 * 
	 * @param abstractArray
	 *            an array of File-objects
	 * @param n
	 *            the number of rounds for cross-validation
	 * @return avg accuracy over all x-validation rounds
	 */
	private static double doCrossEvaluation(File[] abstractArray, int n, TreeSet<String> errorList, boolean splitUnitsAfterPunctuation) {

		ArrayList<File> abstractList = new ArrayList<>();
		for (int i = 0; i < abstractArray.length; i++)
			abstractList.add(abstractArray[i]);
		Collections.shuffle(abstractList, new Random(1));

		int pos = 0;
		int sizeRound = abstractArray.length / n;
		int sizeAll = abstractArray.length;
		int sizeLastRound = sizeRound + sizeAll % n;
		System.out.println("number of files in directory: " + sizeAll);
		System.out.println("size of each/last round: " + sizeRound + "/" + sizeLastRound);
		System.out.println();

		EvalResult[] evalResults = new EvalResult[n]; // 
		double avgAcc = 0;
		double avgF = 0;

		for (int i = 0; i < n; i++) { // in each round

			File[] trainFiles;
			File[] predictFiles;

			int p = 0;
			int t = 0;

			if (i == n - 1) {
				// last round

				trainFiles = new File[sizeAll - sizeLastRound];
				predictFiles = new File[sizeLastRound];

				for (int j = 0; j < abstractList.size(); j++) {
					File f = abstractList.get(j);
					if (j < pos) {
						trainFiles[t] = f;
						t++;
					} else {
						predictFiles[p] = f;
						p++;
					}
				}

			} else {
				// other rounds

				trainFiles = new File[sizeAll - sizeRound];
				predictFiles = new File[sizeRound];

				for (int j = 0; j < abstractList.size(); j++) {
					File f = abstractList.get(j);
					if (j < pos || j >= (pos + sizeRound)) {
						trainFiles[t] = f;
						t++;
					} else {
						predictFiles[p] = f;
						p++;
					}
				}
				pos += sizeRound;
			}

			// now evaluate for this round
			System.out.println("training size: " + trainFiles.length);
			System.out.println("prediction size: " + predictFiles.length);
			evalResults[i] = doEvaluation(trainFiles, predictFiles, errorList, splitUnitsAfterPunctuation);
		}

		DecimalFormat df = new DecimalFormat("0.000");
		for (int i = 0; i < evalResults.length; i++) {
			avgAcc += evalResults[i].ACC;
			avgF += evalResults[i].getF();
			System.out.println(i + ": " + df.format(evalResults[i].ACC));
		}
		avgAcc = avgAcc / (double) n;
		avgF = avgF / (double) n;

		System.out.println("avg accuracy: " + df.format(avgAcc));
		System.out.println("avg f-score: " + df.format(avgF));

		return avgAcc;
	}

	/**
	 * normal evaluation, returns the accuracy errorList has format: filename<tab>orglabel<tab>pred
	 * label<tab>token
	 * 
	 * @param trainFiles
	 *            the files from which the model should be learned
	 * @param predictFiles
	 *            the files for evaluated prediction
	 * @param errorList:
	 *            write classification errors there stored...
	 * @return accuracy
	 */
	private static EvalResult doEvaluation(File[] trainFiles, File[] predictFiles, TreeSet<String> errorList, boolean splitUnitsAfterPunctuation) {

		SentenceSplitter tpFunctions = new SentenceSplitter();

		// get EOS symbols
		EOSSymbols eoss = new EOSSymbols();

		// get training data
		InstanceList trainData = tpFunctions.makeTrainingData(trainFiles, splitUnitsAfterPunctuation, false);
		Pipe myPipe = trainData.getPipe();

		// train a model
		System.out.println("training...");
		tpFunctions.train(trainData, myPipe);

		if (true)
			return doEvaluation(tpFunctions.getModel(), predictFiles, errorList);

		// get testing data
		InstanceList predictData = tpFunctions.makePredictionData(predictFiles, myPipe);

		// predict with model and evaluate
		System.out.println("predicting...");
		int corr = 0;
		int all = 0;
		int fp = 0;
		int fn = 0;
		double acc = 0;
		for (int i = 0; i < predictData.size(); i++) {
			Instance inst = (Instance) predictData.get(i);
			String abstractName = (String) inst.getSource();
			List<Unit> units = null;
			try {
				units = tpFunctions.predict(inst, doPostprocessing);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

			List<String> orgLabels = getLabelsFromLabelSequence((LabelSequence) inst.getTarget());

			for (int j = 0; j < units.size(); j++) {
				String unitRep = units.get(j).rep;
				String pred = units.get(j).label;
				String org = orgLabels.get(j);

				if (eoss.tokenEndsWithEOSSymbol(unitRep)) { // evaluate only if
					// token ends with
					// EOS symbol
					all++;
					if (pred.equals(org))
						corr++;
					else { // store errors
						String error = abstractName + "\t" + org + "\t" + pred + "\t" + unitRep + "  (" + j + ")";
						// System.out.println(error);
						errorList.add(error);
						if (pred.equals("EOS") && org.equals("IS"))
							fp++;
						else if (pred.equals("IS") && org.equals("EOS"))
							fn++;
					}
				}
			}
		}
		
		acc = corr / (double) all;
		EvalResult er = new EvalResult();
		er.corrDecisions = corr;
		er.nrDecisions = all;
		er.fn = fn;
		er.fp = fp;
		er.ACC = acc;
		System.out.println("all : " + all);
		System.out.println("corr: " + corr);
		System.out.println("fp :" + fp);
		System.out.println("fn :" + fn);
		System.out.println("R :" + er.getR());
		System.out.println("P :" + er.getP());
		System.out.println("F :" + er.getF());
		System.out.println("ACC : " + acc);


//		return acc;
		return er;
	}

	/**
	 * normal evaluation, returns the accuracy errorList has format: filename<tab>orglabel<tab>pred
	 * label<tab>token
	 * 
	 * @param crf
	 *            a previously trained model
	 * @param predictFiles
	 *            the files for evaluated prediction
	 * @param errorList:
	 *            write classification errors there stored...
	 * @return accuracy
	 */
	private static EvalResult doEvaluation(CRF crf, File[] predictFiles, TreeSet<String> errorList) {

		SentenceSplitter tpFunctions = new SentenceSplitter();
		tpFunctions.setModel(crf);

		// get EOS symbols
		EOSSymbols eoss = new EOSSymbols();

		// get testing data
		InstanceList predictData = tpFunctions.makePredictionData(predictFiles, crf.getInputPipe());

		// predict with model and evaluate
		System.out.println("predicting...");
		int corr = 0;
		int all = 0;
		int fn= 0;
		int fp=0;
		double acc = 0;
		for (int i = 0; i < predictData.size(); i++) {
			Instance inst = predictData.get(i);
			String abstractName = (String) inst.getSource();
			List<Unit> units = null;
			try {
				units = tpFunctions.predict(inst, doPostprocessing);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

			List<String> orgLabels = getLabelsFromLabelSequence((LabelSequence) inst.getTarget());

			// for postprocessing
			// if (doPostprocessing) {
			// predLabels = tpFunctions.postprocessingFilter(predLabels, units);
			// }
			// System.out.println("\n" + abstractName + "\n" + Tokens + "\n" +
			// orgLabels + "\n" + predLabels);

			for (int j = 0; j < units.size(); j++) {
				String unitRep = units.get(j).rep;
				String pred = units.get(j).label;
				String org = orgLabels.get(j);

				if (eoss.tokenEndsWithEOSSymbol(unitRep)) { // evaluate only if
					// token ends with
					// EOS symbol
					all++;
					if (pred.equals(org))
						corr++;
					else { // store errors
						String error = abstractName + "\t" + org + "\t" + pred + "\t" + unitRep + "  (" + j + ")";
						// System.out.println(error);
						errorList.add(error);
						if (pred.equals("EOS") && org.equals("IS"))
							fp++;
						else if (pred.equals("IS") && org.equals("EOS"))
							fn++;
					}
				}
			}
		}
		acc = corr / (double) all;
		EvalResult er = new EvalResult();
		er.corrDecisions = corr;
		er.nrDecisions = all;
		er.fn = fn;
		er.fp = fp;
		er.ACC = acc;
		System.out.println("all : " + all);
		System.out.println("corr: " + corr);
		System.out.println("fp :" + fp);
		System.out.println("fn :" + fn);
		System.out.println("R :" + er.getR());
		System.out.println("P :" + er.getP());
		System.out.println("F :" + er.getF());
		System.out.println("ACC : " + acc);

		
//		return acc;
		return er;
	}

	/**
	 * to train a sentence boundary detector input: abstracts with one sentence per line outout: a
	 * crf model stored in file
	 * 
	 * @param trainFiles
	 *            the directory with training abstracts
	 * @param modelFilename
	 *            the file to store the trained model
	 */
	private static void doTraining(File[] trainFiles, boolean splitUnitsAfterPunctuation, String modelFilename) {
		SentenceSplitter sentenceSplitter = new SentenceSplitter();

		// get training data
		System.out.println("making training data...");
		InstanceList trainData = sentenceSplitter.makeTrainingData(trainFiles, false, splitUnitsAfterPunctuation);
		Pipe myPipe = trainData.getPipe();

		// train a model
		System.out.println("training model...");
		sentenceSplitter.train(trainData, myPipe);
		sentenceSplitter.writeModel(modelFilename);
	}

	/**
	 * this performs sentence splitting input: files with all sentences of an abstract within one
	 * line output: files with one sentence per line
	 * 
	 * @param inFiles
	 *            the input
	 * @param outDir
	 *            the output, where to store the splitted sentences
	 * @param modelFilename
	 *            the stored model to load
	 */
	private static void doPrediction(File[] inFiles, File outDir, String modelFilename) {

		SentenceSplitter sentenceSplitter = new SentenceSplitter();

		System.out.println("reading model...");
		try {
			sentenceSplitter.readModel(new File(modelFilename));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		// make prediction data
		System.out.println("starting sentence splitting...");
		Pipe myPipe = sentenceSplitter.getModel().getInputPipe();

		int step = 100;
		int percentage = 0;

		Instance inst = null;
		Instance tmp = null;
		for (int i = 0; i < inFiles.length; i++) {

			long s1 = System.currentTimeMillis();
			if (i % step == 0 && i > 0) {
				percentage += 1;
				System.out.println(i + " files done...");
			}

			List<String> fileLines = sentenceSplitter.readFile(inFiles[i]);

			tmp = new Instance(fileLines, "", "", inFiles[i].getName());
			inst = myPipe.instanceFrom(tmp);

			List<Unit> units = null;

			try {
				units = sentenceSplitter.predict(inst, doPostprocessing);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

		//	ArrayList<String> orgLabels = getLabelsFromLabelSequence((LabelSequence) inst.getTarget());

			// for postprocessing
			// if (doPostprocessing) {
			// predLabels = tpFunctions.postprocessingFilter(predLabels, units);
			// }

			// System.out.println(inFiles[i].toString());
			// for (int j = 0; j < Tokens.size(); j++)
			// System.out.println(orgLabels.get(j).equals(predLabels.get(j))
			// + "\t" + orgLabels.get(j) + "\t" + predLabels.get(j) + "\t" +
			// Tokens.get(j));

			// now write to file
			String fName = inFiles[i].toString();
			String newfName = fName.substring(fName.lastIndexOf("/") + 1, fName.length());

			File fNew = new File(outDir.toString() + "/" + newfName);

			ArrayList<String> lines = new ArrayList<>();
			String sentence = "";
			for (Unit unit : units) {
				String label = unit.label;
				String unitRep = unit.rep;
                sentence += unitRep;
                if (unit.afterWs)
                    sentence += " ";
				if (label.equals("EOS")) {
					lines.add(sentence);
					sentence = "";
				}
			}

			long s2 = System.currentTimeMillis();
			writeFile(lines, fNew);
		}

	}

	private static ArrayList<String> getLabelsFromLabelSequence(LabelSequence ls) {
		ArrayList<String> labels = new ArrayList<>();
		for (int j = 0; j < ls.size(); j++)
			labels.add((String) ls.get(j));
		return labels;
	}

	private static void writeFile(TreeSet<String> lines, File outFile) {
		try {
			FileWriter fw = new FileWriter(outFile);
			for (Iterator iter = lines.iterator(); iter.hasNext();)
				fw.write((String) iter.next() + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void writeFile(ArrayList<String> lines, File outFile) {
		try {
			FileWriter fw = new FileWriter(outFile);

			for (int i = 0; i < lines.size(); i++)
				fw.write(lines.get(i) + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	private static class EvalResult {
		int nrDecisions;
		double ACC;
		double fp;
		double fn;
		double corrDecisions;

		double getF() {
			return 2 * getR() * getP() / (getR() + getP());
		}

		double getR() {
			return (double) corrDecisions / (corrDecisions + fn);
		}

		double getP() {
			return (double) corrDecisions / (corrDecisions + fp);
		}
	}
}
