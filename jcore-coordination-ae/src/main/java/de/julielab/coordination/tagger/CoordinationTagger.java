/**
 * NETagger.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 *
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Nov 1, 2006
 *
 * The main object for named entity tagging.
 *
 * TODO: confidence estimation also for IOB tags (not only IO)
 *
 **/

package de.julielab.coordination.tagger;

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
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cc.mallet.fst.CRF;
import cc.mallet.fst.MultiSegmentationEvaluator;
import cc.mallet.fst.Segment;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.fst.confidence.ConstrainedForwardBackwardConfidenceEstimator;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import de.julielab.coordination.utils.IOEvaluation;

public class CoordinationTagger {

    private CRF model;

    private Properties featureConfig;

    private boolean trained = false;

    public CoordinationTagger() {
        Properties defaults = new Properties();
        InputStream defaultFeatureConfigStream = getClass().getResourceAsStream("/defaultFeatureConf.conf");

        try {
            defaults.load(defaultFeatureConfigStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        featureConfig = new Properties(defaults);
        System.out.println(featureConfig.size());
    }

    public CoordinationTagger(File featureConfigFile) {
        featureConfig = new Properties();
        if (!featureConfigFile.isFile()) {
            // TODO: catch exception
            System.err.println("ERR: specified file for feature configuration not found!");
            System.exit(-1);
        }
        try {
            featureConfig.load(new FileInputStream(featureConfigFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * returns true when model has been successfully trained.
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
     * @param tags
     *            specify the tag set used in the training data here!
     */
    public void train(ArrayList<Sentence> sentences, Tags tags) {

        System.out.println("   * training model... on " + sentences.size() + " sentences");

        InstanceList data = FeatureGenerator.createFeatureData(sentences, tags.getAlphabet(), featureConfig);
        System.out.println("  * no of features for training: " + data.getDataAlphabet().size());

        // train model
        long start = System.currentTimeMillis();
        CRF crf = new CRF(data.getPipe(), null);
        crf.addStatesForLabelsConnectedAsIn(data);
        crf.train(data, (InstanceList) null, (InstanceList) null, (MultiSegmentationEvaluator) null, 99999, 10,
                new double[] { .2, .5, .8 });

        long stop = System.currentTimeMillis();
        System.out.println("  * learning took (sec): " + (stop - start) / 1000);

        this.model = crf;
        this.trained = true;
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
    public void predict(Sentence sentence, boolean showSegmentConfidence) throws CoordinationException {
        if (trained == false || model == null) {
            throw new CoordinationException("No model available. Train or load trained model first.");
        }

        Pipe myPipe = model.getInputPipe();
        Instance inst = new Instance(sentence, "", "", "", myPipe);
        Sequence input = (Sequence) inst.getData();
        Sequence output = model.viterbiPath(input).output();

        if (output.size() != sentence.getUnits().size()) {
            throw new CoordinationException("Wrong number of labels predicted.");
        }

        // calculate performance on segments
        double[] conf = null;
        if (showSegmentConfidence) {
            conf = getSegmentConfidence(input, output);
        }

        // now add the label to the unit object
        for (int i = 0; i < sentence.getUnits().size(); i++) {
            Unit unit = sentence.get(i);
            unit.setLabel((String) output.get(i));
            if (showSegmentConfidence) {
                unit.setConfidence(conf[i]);
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

    public ArrayList<String> predictIOB(ArrayList<Sentence> sentences, boolean showSegmentConfidence)
            throws CoordinationException {

        if (trained == false || model == null) {
            throw new CoordinationException("no model available. Train or load trained model first.");
        }

        System.out.println("  * predicting with crf model...");

        Pipe myPipe = model.getInputPipe();

        // iterate through sentences and predict `em as IOB Output
        ArrayList<String> iobList = new ArrayList<String>();
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            Instance inst = new Instance(sentence, "", "", "", myPipe);
            Sequence input = (Sequence) inst.getData();
            Sequence output = model.viterbiPath(input).output();

            ArrayList<Unit> units = sentence.getUnits();

            if (output.size() != sentence.getUnits().size()) {
                throw new CoordinationException("Wrong number of labels predicted.");
            }

            // calculate performance on segments
            double[] conf = null;
            if (showSegmentConfidence) {
                conf = getSegmentConfidence(input, output);
            }

            // now add the label to the unit object
            // and write in IOB ArrayList
            for (int j = 0; j < sentence.getUnits().size(); j++) {
                Unit unit = sentence.get(j);

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
        return iobList;
    }

    /**
     * Estimates the confidence the tagger had on those tokens which were finally annotated as not OUTSIDE.
     *
     * Contstraint forward backward confidence estimation is applied here.
     *
     * This currently only works in the IO approach (IOB tags not properly considerd here!). TODO: handle IOB tags also
     *
     * @param input
     *            the input sequence (features)
     * @param output
     *            the label sequence predicted
     * @return array with confidence values
     */
    private double[] getSegmentConfidence(Sequence input, Sequence output) {

        // initialize confidence estimator and get lattice
        ConstrainedForwardBackwardConfidenceEstimator constrFBEstimator = new ConstrainedForwardBackwardConfidenceEstimator(
                model);
        // Transducer.Lattice lattice = ((Transducer) model).forwardBackward(input);
        SumLatticeDefault fLattice = new SumLatticeDefault(model, input);

        // make empty confidence list
        double[] confidenceList = new double[output.size()];
        for (int i = 0; i < confidenceList.length; i++) {
            confidenceList[i] = -1;
        }

        // get segments (only for IO case currently)
        ArrayList<String> labels = new ArrayList<String>();
        for (int i = 0; i < output.size(); i++) {
            labels.add((String) output.get(i));
        }
        HashMap entities = IOEvaluation.getChunksIO(labels);

        // loop over segments and estimate confidence
        for (Iterator iter = entities.keySet().iterator(); iter.hasNext();) {
            String[] offset = ((String) iter.next()).split(",");
            int start = new Integer(offset[0]).intValue();
            int stop = new Integer(offset[1]).intValue();
            Segment seg = new Segment(input, output, output, start, stop, "entity", "entity");
            double constrFBConf = constrFBEstimator.estimateConfidenceFor(seg, fLattice);

            // System.out.println(start + " - " + stop + " constr fb confidence: " + constrFBConf);

            // add confidence to list
            for (int i = start; i <= stop; i++) {
                confidenceList[i] = constrFBConf;
            }

        }
        return confidenceList;
    }

    /**
     * Save the model learned to disk. THis is done via Java's object serialization.
     *
     * @param filename
     *            where to write it (full path!)
     */
    public void writeModel(String filename) {
        if (trained == false || model == null || featureConfig == null) {
            System.err.println("train or load trained model first.");
            System.exit(0);
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(filename + ".gz"));
            GZIPOutputStream gout = new GZIPOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(gout);
            oos.writeObject(new FeatureSubsetModel(model, featureConfig));
            oos.close();
        } catch (Exception e) {
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
    public void readModel(String filename) throws IOException, FileNotFoundException, ClassNotFoundException {

        FileInputStream fis = new FileInputStream(new File(filename));
        GZIPInputStream gin = new GZIPInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(gin);
        FeatureSubsetModel fsm = (FeatureSubsetModel) ois.readObject();
        this.model = fsm.getModel();
        this.featureConfig = fsm.getFeatureConfig();
        this.trained = true;
    }

    /**
     * return the model
     */
    public CRF getModel() {
        return this.model;
    }

    public void setFeatureConfig(Properties featureConfig) {
        this.featureConfig = featureConfig;
    }

    public Properties getFeatureConfig() {
        return this.featureConfig;
    }

    /**
     * takes a sentence in piped format and returns the corresponding unit sentence as a Sentence object
     *
     * @param string
     *            in piped format to be converted
     */
    public Sentence PPDtoUnits(String sentence) throws CoordinationException {
        String[] tokens = sentence.trim().split("[\t ]+");
        String features[];
        String label, word;
        String featureName; // name of feature for units given by featureConfig
        Unit unit;
        HashMap<String, String> metas; // will contain all known meta datas of
        // a token
        ArrayList<Unit> units = new ArrayList<Unit>();
        String[] trueMetas = FeatureConfiguration.getTrueMetas(featureConfig);

        for (int i = 0; i < tokens.length; i++) {
            metas = new HashMap<String, String>();

            features = tokens[i].split("\\|+");

            word = features[0];
            label = features[features.length - 1];

            if (trueMetas.length + 2 != features.length) {
                System.err.println("Error in input format (PipedFormat)! Mal-formatted sentence: " + sentence
                        + "\n token: " + tokens[i]);
                System.err.println(
                        "Perhaps your configuration file uses more or less meta datas as are available in your input files? "
                                + "If you don't use a config file, you should check whether your input files fit to the default configuration.");
                System.exit(-1);
            }

            for (int j = 0; j < trueMetas.length; j++) {
                int position = Integer.parseInt(featureConfig.getProperty(trueMetas[j] + "_feat_position"));
                featureName = featureConfig.getProperty(trueMetas[j] + "_feat_unit");
                if (!features[position].equals(featureConfig.getProperty("gap_character"))) {
                    metas.put(featureName, features[position]);
                }
            }

            unit = new Unit(0, 0, word, label, metas);
            units.add(unit);
        }
        return new Sentence(units);
    }
}
