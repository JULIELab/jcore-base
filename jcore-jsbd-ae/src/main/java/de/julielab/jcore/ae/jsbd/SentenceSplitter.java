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
package de.julielab.jcore.ae.jsbd;

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
import java.lang.reflect.Constructor;
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
import de.julielab.jcore.ae.jsbd.PostprocessingFilters.PostprocessingFilter;

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
	public ArrayList<Unit> predict(ArrayList<String> lines, String postprocessingFilter) {
		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}
		Instance inst = model.getInputPipe().instanceFrom(new Instance(lines, "", "", ""));
		return predict(inst, postprocessingFilter);
	}

	/**
	 * predict a single Instance
	 * 
	 * @param inst
	 * @param doPostProcessing
	 * @return ArrayList of Unit objects
	 */
	public ArrayList<Unit> predict(Instance inst, String filterName) {
		if (trained == false || model == null) {
			throw new IllegalStateException("No model available. Train or load trained model first.");
		}

		// get sequence
		Sequence input = (Sequence) inst.getData();
		@SuppressWarnings("unchecked")
		ArrayList<Unit> units = (ArrayList<Unit>) inst.getName();
		ArrayList<String> labelList = new ArrayList<String>();

		// transduce and generate output
		Sequence<String> crfOutput = model.transduce(input);
		for (int j = 0; j < crfOutput.size(); j++) {
			labelList.add(crfOutput.get(j));
		}

		// postprocessing
		if (filterName != null) {
			LOGGER.debug("Postprocessing with " + filterName);
			if ( (filterName.toLowerCase()).equals(PostprocessingFilter.BIOMED_POSTPROC) ) {
				labelList = PostprocessingFilter.Mode.BIOMED.process(labelList, units);
			}
			else if ( (filterName.toLowerCase()).equals(PostprocessingFilter.MEDICAL_POSTPROC) ) {
				labelList = PostprocessingFilter.Mode.MEDICAL.process(labelList, units);
			}
		}

		// now write output to units
		for (int j = 0; j < labelList.size(); j++) {
			(units.get(j)).label = labelList.get(j);
		}
		return units;
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
