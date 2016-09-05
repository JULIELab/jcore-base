/** 
 * SentenceSplitter.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.0	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The main class for the JULIE Sentence Boundary Detector. This class has all 
 * the function for training and prediction etc.
 * The following labels are used: "IS" -> "inside sentence", "EOS" -> "end-of-sentence" (a unit at 
 * the end of a sentence)
 **/

package de.julielab.jsbd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.TreeSet;
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

public class SentenceSplitter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceSplitter.class);

	CRF model = null;

	boolean trained = false;

	public SentenceSplitter() {
		model = null;
		trained = false;
	}

	/**
	 * creates a single instance from the arraylist with lines provided and the given pipe
	 */
	public Instance makePredictionData(ArrayList<String> lines, Pipe myPipe) {
		Instance inst = model.getInputPipe().instanceFrom(new Instance(lines, "", "", ""));
		return inst;
	}

	/**
	 * creates a single instance from the file provided and the given pipe
	 */
	public Instance makePredictionData(File predictFile, Pipe myPipe) {
		ArrayList<String> lines = readFile(predictFile);
		Instance inst = model.getInputPipe().instanceFrom(new Instance(lines, "", "", predictFile.getName()));
		return inst;
	}

	/**
	 * creates a list of instances with the pipe provided from the given array of files
	 */
	public InstanceList makePredictionData(File[] predictFiles, Pipe myPipe) {
		InstanceList predictData = new InstanceList(myPipe);
		for (int i = 0; i < predictFiles.length; i++) {
			ArrayList<String> fileLines = readFile(predictFiles[i]);
			Instance inst =
					model.getInputPipe().instanceFrom(new Instance(fileLines, "", "", predictFiles[i].getName()));
			predictData.add(inst);
		}
		return predictData;
	}

	/**
	 * 
	 * @param trainFiles
	 * @param useTokenOffset
	 *            if true the tokens offset and not is string representation is stored in the instance source
	 * @return InstanceList with training data
	 */
	public InstanceList makeTrainingData(File[] trainFiles, boolean useTokenOffset) {

		LabelAlphabet dict = new LabelAlphabet();
		dict.lookupLabel("EOS", true); // end of sentence label
		dict.lookupLabel("IS", true); // inside sentence label

		Pipe myPipe =
				new SerialPipes(new Pipe[] { new Abstract2UnitPipe(),
						new OffsetConjunctions(new int[][] { { -1 }, { 0 }, { 1 } }),
						new TokenSequence2FeatureVectorSequence(true, true) });
		InstanceList instList = new InstanceList(myPipe);

		System.out.print("preparing training data...");
		int step = trainFiles.length / 20;
		int percentage = 0;
		for (int i = 0; i < trainFiles.length; i++) {
			ArrayList<String> fileLines = readFile(trainFiles[i]);
			if (step > 0 && i % step == 0 && i > 0) {
				percentage += 5;
				System.out.print(percentage + "%...");
			}
			instList.addThruPipe(new Instance(fileLines, "", "", trainFiles[i].getName()));
		}
		return instList;
	}

	public void train(InstanceList instList, Pipe dataPipe) {
		long s1 = System.currentTimeMillis();
		// set up model
		model = new CRF(instList.getPipe(), (Pipe) null);
		model.addStatesForLabelsConnectedAsIn(instList);

		// get trainer
		CRFTrainerByLabelLikelihood crfTrainer = new CRFTrainerByLabelLikelihood(model);

		// do the training with unlimited amount of iterations
		// --> refrained from using modified version of mallet;
		// it's now the original source
		boolean b = crfTrainer.train(instList);
		LOGGER.info("SentencesSplitter training: model converged: " + b);

		long s2 = System.currentTimeMillis();

		// stop growth and set trained
		model.getInputPipe().getDataAlphabet().stopGrowth();
		trained = true;

		LOGGER.info("training time: " + (s2 - s1) / 1000 + " sec");
	}

	/**
	 * predict a couple of lines
	 * 
	 * @param lines
	 * @param doPostprocessing
	 * @return ArrayList of Unit objects
	 */
	public ArrayList<Unit> predict(ArrayList<String> lines, boolean doPostprocessing) {
		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}
		Instance inst = model.getInputPipe().instanceFrom(new Instance(lines, "", "", ""));
		return predict(inst, doPostprocessing);
	}

	/**
	 * predict a single Instance
	 * 
	 * @param inst
	 * @param doPostProcessing
	 * @return ArrayList of Unit objects
	 */
	public ArrayList<Unit> predict(Instance inst, boolean doPostProcessing) {

		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}

		// get sequence
		Sequence input = (Sequence) inst.getData();
		ArrayList<Unit> units = (ArrayList<Unit>) inst.getName();
		ArrayList<String> labelList = new ArrayList<String>();

		// transduce and generate output
		Sequence<String> crfOutput = model.transduce(input);
		for (int j = 0; j < crfOutput.size(); j++) {
			labelList.add(crfOutput.get(j));
		}

		// postprocessing
		if (doPostProcessing) {
			labelList = postprocessingFilter(labelList, units);
		}

		// now write output to units
		for (int j = 0; j < labelList.size(); j++) {
			(units.get(j)).label = labelList.get(j);
		}
		return units;
	}

	/**
	 * a postprocessing filter (to be used after prediction) which can correct known errors
	 * 
	 * @param predLabels
	 * @param units
	 * @param abbrList
	 * @return
	 */
	public ArrayList<String> postprocessingFilter(ArrayList<String> predLabels, ArrayList<Unit> units) {

		Abbreviations abr = new Abbreviations();
		TreeSet<String> abrSet = abr.getSet();

		String[] labels = (String[]) predLabels.toArray(new String[predLabels.size()]);
		ArrayList<String> newPred = new ArrayList<String>();

		// do not set an EOS after opening bracket until bracket is closed again
		int openNormalBrackets = 0;
		int openSquareBrackets = 0;
		int count = 0;

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			char[] c = unitRep.toCharArray();
			for (int j = 0; j < c.length; j++) {
				switch (c[j]) {
				case '(':
					openNormalBrackets++;
					break;
				case '[':
					openSquareBrackets++;
					break;
				case ')':
					openNormalBrackets--;
					break;
				case ']':
					openSquareBrackets--;
					break;
				}
			}

			if (openSquareBrackets > 0 || openNormalBrackets > 0) {
				labels[i] = "IS";
				count++;
			}

			// close all brackets after 50 tokens inside brackets
			if (count >= 50) {
				openSquareBrackets = 0;
				openNormalBrackets = 0;
			}

			if (openSquareBrackets < 0)
				openSquareBrackets = 0;

			if (openNormalBrackets < 0)
				openNormalBrackets = 0;

		}

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			// remove EOS from known abbreviations
			if (abrSet.contains(unitRep))
				labels[i] = "IS";

			// set EOS if ends with ? or ! ."
			if (unitRep.endsWith(".\"") || unitRep.endsWith("?") || unitRep.endsWith("!"))
				labels[i] = "EOS";

			// add to final arrayList
			newPred.add(labels[i]);
		}

		return newPred;
	}

	public ArrayList<String> getLabelsFromLabelSequence(LabelSequence ls) {
		ArrayList<String> labels = new ArrayList<String>();
		for (int j = 0; j < ls.size(); j++)
			labels.add((String) ls.get(j));
		return labels;
	}

	/**
	 * Save the model learned to disk. THis is done via Java's object serialization.
	 * 
	 * @param filename
	 *            where to write it (full path!)
	 */
	public void writeModel(String filename) {
		if (trained == false || model == null) {
			String info = "train or load trained model first.";
			IllegalStateException e = new IllegalStateException(info);
			LOGGER.error(info, e);
		}

		try {
			FileOutputStream fos = new FileOutputStream(new File(filename + ".gz"));
			GZIPOutputStream gout = new GZIPOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(this.model);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was stored as serialized object to disk.
	 * 
	 * @param filename
	 *            where to find the serialized featureSubsetModel (full path!)
	 */
	public void readModel(File file) throws IOException, FileNotFoundException, ClassNotFoundException {
		readModel(new FileInputStream(file));
	}
	
	public void readModel(InputStream is) throws IOException, ClassNotFoundException {
		GZIPInputStream gin = new GZIPInputStream(is);
		try (ObjectInputStream ois = new ObjectInputStream(gin)) {
			model = (CRF) ois.readObject();
			trained = true;
			model.getInputPipe().getDataAlphabet().stopGrowth();
		}
	}

	public ArrayList<String> readFile(File myFile) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			BufferedReader b = new BufferedReader(new FileReader(myFile));
			String line = "";
			while ((line = b.readLine()) != null) {
				lines.add(line);
			}
			b.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return lines;
	}

	public CRF getModel() {
		return model;
	}

	void setModel(CRF crf) {
		trained = true;
		this.model = crf;
	}

}
