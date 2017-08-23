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
package de.julielab.jcore.ae.jnet.tagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.Segment;
import cc.mallet.fst.SumLatticeConstrained;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.fst.Transducer;
import cc.mallet.fst.confidence.ConstrainedForwardBackwardConfidenceEstimator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import de.julielab.jcore.ae.jnet.utils.IOEvaluation;

/**
 * 
 * general class which does all the ML stuff
 * 
 * TODO confidence estimation also for IOB (not only IO)
 * 
 * @author tomanek
 */
public class NETagger {

	private Object model = null;

	private Properties featureConfig = null;

	private boolean trained = false;

	static Logger LOGGER = LoggerFactory.getLogger(NETagger.class);

	private int number_iterations = 0;

	private boolean max_ent = false;

	private Pipe generalPipe = null;

	private Pipe dummyPipe = null;

	/**
	 * default constructor
	 */
	public NETagger() {
		final Properties defaults = new Properties();
		final InputStream defaultFeatureConfigStream = getClass().getResourceAsStream("/defaultFeatureConf.conf");

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
	public NETagger(final File featureConfigFile) {
		featureConfig = new Properties();
		if (!featureConfigFile.isFile()) {
			final RuntimeException e = new IllegalStateException("specified file for feature configuration not found!");
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
	 * this is to train a NE model (based on CRF); when trained, the model is stored internally. The model can be saved
	 * to disk using the writeModel command.
	 * 
	 * @param sentences
	 *            training data, an ArrayList of Sentence objects, File which contains the feature subset to be used in
	 *            a text format
	 */
	public void train(final ArrayList<Sentence> sentences) {
		System.out.println("   * training model... on " + sentences.size() + " sentences");

		final FeatureGenerator featureGenerator = new FeatureGenerator();
		// create features ones for CRF or ME
		final InstanceList data = featureGenerator.createFeatureData(sentences, featureConfig);
		// pipe which effects on the feature creation
		generalPipe = data.getPipe();

		LOGGER.info("  * number of features for training: " + data.getDataAlphabet().size());

		final long start = System.currentTimeMillis();

		// set up CRF model

		if (!max_ent) {

			model = new CRF(data.getPipe(), null);
			((CRF) model).addStatesForLabelsConnectedAsIn(data);

			// get trainer
			final CRFTrainerByLabelLikelihood crfTrainer = new CRFTrainerByLabelLikelihood((CRF) model);

			// CRFTrainerByStochasticGradient crfTrainerbyStochasticGradient =
			// new CRFTrainerByStochasticGradient((CRF) model, data);
			boolean b;

			if (number_iterations == 0) {
				// do the training with unlimited amount of iterations
				b = crfTrainer.trainOptimized(data);
				LOGGER.info("JNET training: model converged: " + b);
			} else {
				crfTrainer.train(data, number_iterations);
				LOGGER.info("JNET training: with iterations = " + number_iterations);
			}
		}
		// set up Maximum Entropy model
		else if (max_ent) {

			// my new pipe only for ME
			dummyPipe = new SerialPipes(new Pipe[] { new METrainerDummyPipe(data.getDataAlphabet(),
					data.getTargetAlphabet()), });

			// convert features to ME-conform representation
			final InstanceList tokenData = FeatureGenerator.convertFeatsforClassifier(dummyPipe, data);

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

		}

		final long stop = System.currentTimeMillis();
		LOGGER.info("  * learning took (sec): " + ((stop - start) / 1000));

		trained = true;
	}

	/**
	 * 
	 * predicts the entity labels by means of a model. this method is needed by UIMA-JNET!
	 * 
	 * @param sentence
	 *            a Sentence object containing all units (= tokens) of that sentence
	 * @param showSegmentConfidence
	 *            when this flag is set to true for all found entities a confidence is estimated. The confidence is
	 *            stored in Unit object.
	 */
	public void predict(final Sentence sentence, final boolean showSegmentConfidence) {
		if ((trained == false) || (model == null)) {
			final RuntimeException e = new IllegalStateException(
					"No model available. Train or load trained model first.");
			LOGGER.error("", e);
			throw (e);
		}

		if (!max_ent) {
			// get sequence
			final Instance inst = ((Transducer) model).getInputPipe().instanceFrom(new Instance(sentence, "", "", ""));
			final Sequence<?> input = (Sequence<?>) inst.getData();

			// transduce and generate output
			final Sequence<?> output = ((Transducer) model).transduce(input);

			if (output.size() != sentence.getUnits().size()) {
				final RuntimeException e = new IllegalStateException("Wrong number of labels predicted.");
				LOGGER.error("", e);
				throw (e);
			}

			// calculate performance on segments
			double[] conf = null;
			if (showSegmentConfidence)
				conf = getSegmentConfidence(input, output);

			// now add the label to the unit object
			for (int i = 0; i < sentence.getUnits().size(); i++) {
				final Unit unit = sentence.get(i);
				unit.setLabel((String) output.get(i));
				if (showSegmentConfidence)
					unit.setConfidence(conf[i]);
			}
		} else if (max_ent) {
			// get instance
			System.out.println("  * predicting with me model...");

			final Classifier classifier = (Classifier) model;
			final Instance inst = generalPipe.instanceFrom(new Instance(sentence, "", "", ""));
			// transform to token based features for ME
			final InstanceList tokenList = FeatureGenerator.convertFeatsforClassifier(classifier.getInstancePipe(),
					inst);
			LOGGER.info("current sentence has this number of token features: " + tokenList.size());

			final ArrayList<Unit> units = sentence.getUnits();

			if (units.size() != tokenList.size()) {
				LOGGER.error("precit() - something went wrong with sequence feature conversion");
				System.exit(-1);
			}

			for (int j = 0; j < tokenList.size(); j++) {
				final Classification C = classifier.classify(tokenList.get(j));
				final String label = C.getLabeling().getBestLabel().toString();
				C.getLabeling().getBestValue();
				final Unit unit = units.get(j);
				unit.setLabel(label);

			}

		}
	}

	/**
	 * predict the entity labels by means of a previously learned model.
	 * 
	 * this method is used by JNET stand alone version (for UIMA-JNET see other predict method)
	 * 
	 * Output is an arraylist of IOB
	 * 
	 * @param sentences
	 *            an ArrayList of Sentence objects
	 * @param showSegmentConfidence
	 *            when this flag is set to true for all found entities a confidence is estimated. Confidence is written
	 *            to IOB outputfile.
	 * @return IOB output for the sentences to be predicted. Each element of the ArrayList is a string which refers to
	 *         one word and its label ("token\tlabel")
	 */

	public ArrayList<String> predictIOB(final ArrayList<Sentence> sentences, final boolean showSegmentConfidence) {

		if ((trained == false) || (model == null)) {
			final RuntimeException e = new IllegalStateException(
					"no model available. Train or load trained model first.");
			LOGGER.error("", e);
			throw (e);
		}

		final long t1 = System.currentTimeMillis();
		// iterate through sentences and predict `em as IOB Output
		final ArrayList<String> iobList = new ArrayList<String>();

		if (!max_ent) {
			System.out.println("  * predicting with crf model...");
			for (int i = 0; i < sentences.size(); i++) {
				final Sentence sentence = sentences.get(i);
				final Instance inst = ((Transducer) model).getInputPipe().instanceFrom(
						new Instance(sentence, "", "", ""));

				final Sequence<?> input = (Sequence<?>) inst.getData();
				final Sequence<?> output = ((Transducer) model).transduce(input);

				final ArrayList<Unit> units = sentence.getUnits();

				if (output.size() != sentence.getUnits().size()) {
					final RuntimeException e = new IllegalStateException("Wrong number of labels predicted.");
					LOGGER.error("", e);
					throw (e);
				}

				// calculate performance on segments
				double[] conf = null;
				if (showSegmentConfidence)
					conf = getSegmentConfidence(input, output);

				// now add the label to the unit object
				// and write in IOB ArrayList
				for (int j = 0; j < sentence.getUnits().size(); j++) {
					final Unit unit = sentence.get(j);

					unit.setLabel((String) output.get(j));
					String iobString = units.get(j).getRep() + "\t" + (String) output.get(j);

					if (showSegmentConfidence) {
						unit.setConfidence(conf[j]);
						iobString += "\t" + conf[j];
					}

					iobList.add(iobString);
				}

				iobList.add("O" + "\t" + "O"); // O<tab>O at sentence end
			}
		} else if (max_ent) {
			System.out.println("  * predicting with me model...");

			final Classifier classifier = (Classifier) model;

			final InstanceList instanceList = new InstanceList(generalPipe);

			for (int i = 0; i < sentences.size(); i++) {
				final Sentence sentence = sentences.get(i);
				final Instance inst = generalPipe.instanceFrom(new Instance(sentence, "", "", ""));
				instanceList.add(inst);

			}

			// transform to token based features for ME
			for (int i = 0; i < instanceList.size(); i++) { // loop over
															// sentences
				final Instance inst = instanceList.get(i);

				final InstanceList tokenList = FeatureGenerator.convertFeatsforClassifier(classifier.getInstancePipe(),
						inst);
				LOGGER.info("current sentence has this number of token features: " + tokenList.size());
				final Sentence sentence = sentences.get(i);
				final ArrayList<Unit> units = sentence.getUnits();

				if (units.size() != tokenList.size()) {
					LOGGER.error("precit() - something went wrong with sequence feature conversion");
					System.exit(-1);
				}

				for (int j = 0; j < tokenList.size(); j++) {
					final Classification C = classifier.classify(tokenList.get(j));
					final String label = C.getLabeling().getBestLabel().toString();
					C.getLabeling().getBestValue();
					final Unit unit = units.get(j);
					unit.setLabel(label);
					final String iobString = units.get(j).getRep() + "\t" + label;
					iobList.add(iobString);
				}

				iobList.add("O" + "\t" + "O"); // O<tab>O at sentence end

			}

		}
		final long t2 = System.currentTimeMillis();
		System.out.println("prediction took: " + (t2 - t1));
		return iobList;
	}

	/**
	 * Estimates the confidence the tagger had on those tokens which were finally annotated as not OUTSIDE.
	 * 
	 * Constraint forward backward confidence estimation is applied here.
	 * 
	 * This currently only works in the IO approach (IOB tags not properly considered here!).
	 * 
	 * @param input
	 *            the input sequence (features)
	 * @param output
	 *            the label sequence predicted
	 * @return array with confidence values
	 */
	private double[] getSegmentConfidence(final Sequence<?> input, final Sequence<?> output) {

		// initialize confidence estimator and get lattice
		// ConstrainedForwardBackwardConfidenceEstimator constrFBEstimator = new
		// ConstrainedForwardBackwardConfidenceEstimator(
		// (Transducer) model);
		// make empty confidence list
		final double[] confidenceList = new double[output.size()];
		for (int i = 0; i < confidenceList.length; i++)
			confidenceList[i] = -1;

		// get segments (only for IO case currently)
		final ArrayList<String> labels = new ArrayList<String>();
		for (int i = 0; i < output.size(); i++)
			labels.add((String) output.get(i));
		final HashMap<?, ?> entities = IOEvaluation.getChunksIO(labels);

		// loop over segments and estimate confidence
		for (final Object name : entities.keySet()) {
			final String key = (String) name;
			final String entLabel = ((String) entities.get(key)).split("#")[0];
			final String[] offset = (key).split(",");
			final int start = (new Integer(offset[0])).intValue();
			final int stop = (new Integer(offset[1])).intValue();
			final Segment seg = new Segment(input, output, output, start, stop, entLabel, entLabel);
			// double constrFBConf =
			// constrFBEstimator.estimateConfidenceFor(seg,
			// null);
			final double constrFBConf = estimateConfidenceFor(seg, null);
			// System.out.println(start + " - " + stop +
			// " constr fb confidence: " + constrFBConf);

			// add confidence to list
			for (int i = start; i <= stop; i++)
				confidenceList[i] = constrFBConf;

		}
		// A try to get the confidence for all units to be able to apply some threshold for which outside units should
		// be re-classified to genes, for example. But for CRFs it doesnt seem to work this way, outside units have
		// higher confidence values than gene units...
		// for (int i = 0; i < confidenceList.length; i++) {
		// if (confidenceList[i] == -1) {
		// final Segment seg = new Segment(input, output, output, i, i,
		// "O", "O");
		// final double constrFBConf = estimateConfidenceFor(seg, null);
		// confidenceList[i] = constrFBConf;
		// }
		//
		// }
		return confidenceList;
	}

	/**
	 * <p>
	 * Taken from MALLET code:
	 * {@link ConstrainedForwardBackwardConfidenceEstimator#estimateConfidenceFor(Segment, SumLatticeDefault)} with a
	 * small change.
	 * </p>
	 * <p>
	 * The change concerns the computation of the final confidence value which reads in the original code:
	 * <p>
	 * <code>double confidence = Math.exp
	 * (latticeWeight - constrainedLatticeWeight)</code>&nbsp;&nbsp;(1)
	 * </p>
	 * However, it seems to me that <code>latticeWeight</code> is to be read as an "unconstrained lattice weight" in
	 * contrast to <code>contrainedLatticeWeight</code>.<br>
	 * The unconstrained lattice weight would be the sum of all paths through the transducer given the input sequence.
	 * The constrained lattice weight, on the other hand, would be the sum of all paths through the transducer given the
	 * input sequence, where these paths are <it>constrained</it> to lead through the specified output sequence labels,
	 * thus producing this sequence (and thus fewer paths).<br>
	 * It follows that <code>constrainedLatticeWeight</code> is limited by <code>latticeWeight</code>. Building the
	 * difference between the two numbers sets the number of paths generating the output sequence in relation to all
	 * possible paths.<br>
	 * If, e.g. there would be unconstrained 100 paths to generate the output sequence, we have to set this in relation
	 * to how large the whole model is. When there are only 101 paths to produce the output sequence, this output has
	 * high confidence. If, however, the whole model would have like 400 paths, the output sequence wouldn't seem to fit
	 * very well into the model, which means a low confidence value.<br>
	 * Now the original code says <code>latticeWeight - unconstrainedLatticeWeight</code>, which always is a value
	 * greater or equal than zero. Setting this number into the exponent (see formula (1)) guarantees a value greater or
	 * equal to one. However, the aim of the original algorithm was to return a value in [0,1] (see original method
	 * documentation). I suppose this contradiction is due to an error in the difference computation: It is not to be
	 * <code>latticeWeight - unconstrainedLatticeWeight</code> but reversed:
	 * <code>unconstrainedLatticeWeight - latticeWeight</code>. This way, we always get a negative number or zero. This,
	 * in turn causes the final expression <code>Math.exp(constrainedLatticeWeight - latticeWeight)</code> to equal 1 -
	 * when there is no difference - or to be below 1 and above 0 (thus, in (0,1)].<br>
	 * High values mean a high amount of paths to generate the output sequence, low values mean few ways to generate the
	 * output sequence (i.e., low confidence). </p>
	 * <p>
	 * Please note that I did not find a paper or other documentation on this very topic. While the above argumentation
	 * makes sense to me, it could be flawed. I will try to get affirmation from the MALLET mailing list - I am not yet
	 * approved to join, however (still pending).
	 * </p>
	 * <p>
	 * Erik Faessler, 02.04.2012
	 * </p>
	 * 
	 * @param segment
	 * @param cachedLattice
	 * @return
	 */
	private double estimateConfidenceFor(final Segment segment, final SumLatticeDefault cachedLattice) {
		final Sequence<?> predSequence = segment.getPredicted();
		final Sequence<?> input = segment.getInput();
		final SumLatticeDefault lattice = (cachedLattice == null) ? new SumLatticeDefault((Transducer) model, input)
				: cachedLattice;
		// constrained lattice
		final SumLatticeDefault constrainedLattice = new SumLatticeConstrained((Transducer) model, input, null,
				segment, predSequence);
		final double latticeWeight = lattice.getTotalWeight();
		final double constrainedLatticeWeight = constrainedLattice.getTotalWeight();
		// Original computation. I have reversed the difference computation.
		// double confidence = Math.exp(latticeWeight -
		// constrainedLatticeWeight);
		final double confidence = Math.exp(constrainedLatticeWeight - latticeWeight);
		return confidence;
	}

	/**
	 * Save the model learned to disk. THis is done via Java's object serialization.
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
			final FileOutputStream fos = new FileOutputStream(new File(filename + ".gz"));
			final GZIPOutputStream gout = new GZIPOutputStream(fos);
			final ObjectOutputStream oos = new ObjectOutputStream(gout);
			oos.writeObject(new FeatureSubsetModel(model, featureConfig));
			oos.close();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	
	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was stored as serialized object to disk.
	 * 
	 * @param is
	 *            input stream of the serialized featureSubsetModel
	 */
	public void readModel(File f) throws IOException, FileNotFoundException, ClassNotFoundException {
		readModel(new FileInputStream(f));
	}
	
	
	/**
	 * load a previously trained FeatureSubsetModel (CRF4+Properties) which was stored as serialized object to disk.
	 * 
	 * @param is
	 *            input stream of the serialized featureSubsetModel
	 */
	public void readModel(InputStream is) throws IOException, FileNotFoundException, ClassNotFoundException {
		final GZIPInputStream gin = new GZIPInputStream(is);

		final ObjectInputStream ois = new ObjectInputStream(gin);

		final FeatureSubsetModel fsm = (FeatureSubsetModel) ois.readObject();
		ois.close();
		
		model = fsm.getModel();
		featureConfig = fsm.getFeatureConfig();
		trained = true;

		if (model instanceof MaxEnt) {
			((MaxEnt) model).getInstancePipe().getDataAlphabet().stopGrowth();
			max_ent = true;
		}
		else {
			((Transducer) model).getInputPipe().getDataAlphabet().stopGrowth();
		}
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

	/**
	 * takes a sentence in piped format and returns the corresponding unit sentence as a Sentence object
	 * 
	 * @param sentence
	 *            in piped format to be converted
	 */
	public Sentence PPDtoUnits(final String sentence) {
		final String[] tokens = sentence.trim().split("[\t ]+");
		String features[];
		String label, word;
		String featureName; // name of feature for units given by featureConfig
		Unit unit;
		HashMap<String, String> metas; // will contain all known meta datas of
		// a token
		final ArrayList<Unit> units = new ArrayList<Unit>();
		final FeatureConfiguration fc = new FeatureConfiguration();
		final String[] trueMetas = fc.getTrueMetas(featureConfig);

		for (final String token : tokens) {
			metas = new HashMap<String, String>();

			features = token.split("\\|+");

			word = features[0];
			label = features[features.length - 1];

			if ((trueMetas.length + 2) != features.length) {
				System.err.println("Error in input format (PipedFormat)! Mal-formatted sentence: " + sentence
						+ "\n token: " + token);
				System.err
						.println("Check your configuration file. Most probably you use more or less meta-data as specified in the configuration file.\n"
								+ "If you don't use a config file, you should check whether your input files fit to the default configuration.");
				System.exit(-1);
			}

			for (final String trueMeta : trueMetas) {
				final int position = Integer.parseInt(featureConfig.getProperty(trueMeta + "_feat_position"));
				featureName = featureConfig.getProperty(trueMeta + "_feat_unit");
				if (!features[position].equals(featureConfig.getProperty("gap_character")))
					metas.put(featureName, features[position]);
			}

			unit = new Unit(0, 0, word, label, metas);
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

	public boolean is_Max_Ent() {
		return max_ent;
	}

	public void set_Max_Ent(final boolean me_train) {
		max_ent = me_train;
	}
}
