/**
 * JNETApplication.java
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
 * Creation date: Aug 01, 2006
 *
 * The command-line application
 **/

package de.julielab.coordination;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import cc.mallet.fst.CRF;
import cc.mallet.types.Alphabet;
import de.julielab.coordination.tagger.CoordinationException;
import de.julielab.coordination.tagger.CoordinationTagger;
import de.julielab.coordination.tagger.Sentence;
import de.julielab.coordination.tagger.Tags;
import de.julielab.coordination.tagger.Unit;
import de.julielab.coordination.utils.FormatConverter;
import de.julielab.coordination.utils.IOBEvaluation;
import de.julielab.coordination.utils.IOEvaluation;
import de.julielab.coordination.utils.Utils;

/**
 * Command line application
 */

public class CoordinationApplication {

    /**
     * @param args
     */
    public static void main(String[] args) {

        // System.out.println(" * running JulieTagger...");

        if (args.length < 1) {
            System.err.println("usage: JNETApplication <mode> <mode-specific-parameters>");
            showModes();
            System.exit(-1);
        }

        String mode = args[0];

        if (mode.equals("f")) {
            /*
             * FormatConverter
             */

            if (args.length < 4) {
                System.out.println(
                        "usage: JNETApplication f <iobFile> <1st meta data file> [further meta data files] <outFile> <taglist (or 0 if not used)>");
                System.exit(0);
            }
            String[] converterArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                converterArgs[i - 1] = args[i];
            }
            FormatConverter.main(converterArgs);
        } else if (mode.equals("s")) {
            /*
             * 90-10 split
             */

            if (args.length < 4) {
                System.err.println("usage: JNETApplication s <data.ppd> <tags.def> <pred-out> [featureConfigFile]");
                System.err.println("pred-out format: token pred gold");
                System.exit(-1);
            }

            File trainFile = new File(args[1]);
            File tagsFile = new File(args[2]);
            File predFile = new File(args[3]);
            File featureConfigFile = null;
            if (args.length == 5) {
                featureConfigFile = new File(args[4]);
            }
            eval9010(trainFile, tagsFile, predFile, featureConfigFile);

        } else if (mode.equals("x")) {
            /*
             * x-validation
             */
            if (args.length < 6) {
                System.err.println(
                        "usage: JNETApplication x <trainData.ppd> <tags.def> <pred-out> <x-rounds> <performance-out-file> [featureConfigFile]");
                System.err.println("pred-out format: token pred gold");
                System.exit(-1);
            }

            File trainFile = new File(args[1]);
            File tagsFile = new File(args[2]);
            File predFile = new File(args[3]);
            int rounds = new Integer(args[4]).intValue();
            File performanceOutFile = new File(args[5]);
            File featureConfigFile = null;
            if (args.length == 7) {
                featureConfigFile = new File(args[6]);
            }
            evalXVal(trainFile, tagsFile, rounds, predFile, performanceOutFile, featureConfigFile);

        } else if (mode.equals("t")) {
            /*
             * train
             */
            if (args.length < 4) {
                System.err.println(
                        "usage: JNETApplication t <trainData.ppd> <tags.def> <model-out-file> [featureConfigFile]");
                System.exit(-1);
            }

            File trainFile = new File(args[1]);
            File tagsFile = new File(args[2]);
            File modelFile = new File(args[3]);
            File featureConfigFile = null;
            if (args.length == 5) {
                featureConfigFile = new File(args[4]);
            }
            train(trainFile, tagsFile, modelFile, featureConfigFile);

        } else if (mode.equals("p")) {
            /*
             * predict
             */

            if (args.length != 6) {
                System.err.println(
                        "usage: JNETApplication p <unlabeled data.ppd> <tag.def> <modelFile> <outFile> <estimate segment conf>");
                System.exit(-1);
            }

            File trainFile = new File(args[1]);
            File tagsFile = new File(args[2]);
            File modelFile = new File(args[3]);
            File outFile = new File(args[4]);
            boolean conf = new Boolean(args[5]);
            predict(trainFile, tagsFile, modelFile, outFile, conf);

        } else if (mode.equals("c")) {
            /*
             * compare gold and prediction
             */

            if (args.length != 4) {

                System.err.println("\ncompares the gold standard agains the prediction: "
                        + "give both IOB files, they must have the same length!");

                System.err.println("\nusage: JNETApplication c <predData.iob> <goldData.iob> <tag.def>");
                System.exit(-1);
            }

            File predFile = new File(args[1]);
            File goldFile = new File(args[2]);
            File tagsFile = new File(args[3]);
            double[] eval = compare(predFile, goldFile, tagsFile);
            System.out.println(eval[0] + "\t" + eval[1] + "\t" + eval[2]); // (R/P/F)

        } else if (mode.equals("oc")) {
            /*
             * output properties
             */

            if (args.length != 2) {
                System.err.println("\nusage: JNETApplication oc <model>");
                System.exit(-1);
            }

            File modelFile = new File(args[1]);
            printFeatureConfig(modelFile);

        } else if (mode.equals("oa")) {
            /*
             * output output alphabet
             */

            if (args.length != 2) {
                System.err.println("\nusage: JNETApplication oa <model>");
                System.exit(-1);
            }

            File modelFile = new File(args[1]);
            printOutputAlphabet(modelFile);

        } else {
            System.err.println("ERR: unknown mode");
            showModes();
            System.exit(-1);
        }

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
    static void train(File trainFile, File tagsFile, File outFile, File featureConfigFile) {
        ArrayList<String> ppdSentences = Utils.readFile(trainFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        Tags tags = new Tags(tagsFile.toString());

        CoordinationTagger tagger;
        if (featureConfigFile != null) {
            tagger = new CoordinationTagger(featureConfigFile);
        } else {
            tagger = new CoordinationTagger();
        }
        for (String ppdSentence : ppdSentences) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (CoordinationException e) {
                e.printStackTrace();
            }
        }
        tagger.train(sentences, tags);
        tagger.writeModel(outFile.toString());
    }

    /**
     * performs a 'n'-fold-cross-validation on 'dataFile'
     */
    static void evalXVal(File dataFile, File tagsFile, int n, File predictionOutFile, File performanceOutFile,
            File featureConfigFile) {

        ArrayList<String> output = new ArrayList<String>(); // for output of
        // gold standard and
        // prediction in
        // outFile, created
        // by eval method

        Tags tags = new Tags(tagsFile.toString());
        ArrayList<String> ppdData = Utils.readFile(dataFile);
        Collections.shuffle(ppdData);

        int pos = 0;
        int sizeRound = ppdData.size() / n;
        int sizeAll = ppdData.size();
        int sizeLastRound = sizeRound + sizeAll % n;
        System.out.println(" * number of sentences: " + sizeAll);
        System.out.println(" * size of each/last round: " + sizeRound + "/" + sizeLastRound);
        System.out.println();

        double[] fscores = new double[n];
        double[] recalls = new double[n];
        double[] precisions = new double[n];

        for (int i = 0; i < n; i++) { // in each round

            ArrayList<String> ppdTrainData = new ArrayList<String>();
            ArrayList<String> ppdTestData = new ArrayList<String>();

            int p = 0;
            int t = 0;

            if (i == n - 1) {
                // last round

                for (int j = 0; j < ppdData.size(); j++) {
                    if (j < pos) {
                        ppdTrainData.add(ppdData.get(j));
                        t++;
                    } else {
                        ppdTestData.add(ppdData.get(j));
                        p++;
                    }
                }

            } else {
                // other rounds
                for (int j = 0; j < ppdData.size(); j++) {
                    if (j < pos || j >= pos + sizeRound) {
                        ppdTrainData.add(ppdData.get(j));
                        t++;
                    } else {
                        ppdTestData.add(ppdData.get(j));
                        p++;
                    }
                }
                pos += sizeRound;
            }

            System.out.println(" * training on: " + ppdTrainData.size() + " -- testing on: " + ppdTestData.size());

            // eval-array: R: eval[0], P: eval[1], F: eval[2]
            double[] eval = eval(ppdTrainData, ppdTestData, tags, output, featureConfigFile);

            recalls[i] = eval[0];
            precisions[i] = eval[1];
            fscores[i] = eval[2];

            System.out.println("\n** round " + (i + 1) + ": R/P/F: " + eval[0] + "/" + eval[1] + "/" + eval[2]);

        }

        // now get average performance
        double avgRecall = getAverage(recalls);
        double avgPrecision = getAverage(precisions);
        double avgFscore = getAverage(fscores);

        double stdRecall = getStandardDeviation(recalls, avgRecall);
        double stdPrecision = getStandardDeviation(precisions, avgPrecision);
        double stdFscore = getStandardDeviation(fscores, avgFscore);

        DecimalFormat df = new DecimalFormat("0.000");
        StringBuffer summary = new StringBuffer();

        summary.append("Cross-validation results:\n");
        summary.append("Number of sentences in evaluation data set: " + sizeAll + "\n");
        summary.append(
                "Number of sentences for training in each/last round: " + sizeRound + "/" + sizeLastRound + "\n\n");
        summary.append("Overall performance: avg (standard deviation)\n");
        summary.append("Recall: " + df.format(avgRecall) + "(" + df.format(stdRecall) + ")\n");
        summary.append("Precision: " + df.format(avgPrecision) + "(" + df.format(stdPrecision) + ")\n");
        summary.append("F1-Score: " + df.format(avgFscore) + "(" + df.format(stdFscore) + ")\n");

        // write performance
        Utils.writeFile(performanceOutFile, summary.toString());

        // write prediction
        Utils.writeFile(predictionOutFile, output);

        System.out.println("\n\nCross-validation finished. Results written to: " + performanceOutFile);
        System.out.println(summary.toString());
    }

    public static double getStandardDeviation(double[] values, double avg) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += Math.pow(values[i] - avg, 2);
        }
        return Math.sqrt(sum / ((double) values.length - 1));
    }

    public static double getAverage(double[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum / values.length;
    }

    /**
     *
     * @param dataFile
     *            in pipedformat, must have entity labels
     * @param tagsFile
     * @param err
     * @param pred
     */
    static void eval9010(File dataFile, File tagsFile, File outFile, File featureConfigFile) {

        ArrayList<String> output = new ArrayList<String>(); // for output of
        // gold standard and
        // prediction in
        // outFile, created
        // by eval method

        Tags tags = new Tags(tagsFile.toString());
        ArrayList<String> ppdData = Utils.readFile(dataFile);
        Collections.shuffle(ppdData);

        int sizeAll = ppdData.size();
        int sizeTest = (int) (sizeAll * 0.1);
        int sizeTrain = sizeAll - sizeTest;

        if (sizeTest == 0) {
            System.err.println("Error: no test files for this split.");
            System.exit(-1);
        }
        System.out.println(" * all: " + sizeAll + "\ttrain: " + sizeTrain + "\t" + "test: " + sizeTest);

        ArrayList<String> ppdTrainData = new ArrayList<String>();
        ArrayList<String> ppdTestData = new ArrayList<String>();

        for (int i = 0; i < ppdData.size(); i++) {
            if (i < sizeTrain) {
                ppdTrainData.add(ppdData.get(i));
            } else {
                ppdTestData.add(ppdData.get(i));
            }
        }

        System.out.println(" * training on: " + ppdTrainData.size() + " -- testing on: " + ppdTestData.size());

        double[] eval = eval(ppdTrainData, ppdTestData, tags, output, featureConfigFile);
        DecimalFormat df = new DecimalFormat("0.000");
        System.out.println("\n\n** R/P/F: " + df.format(eval[0]) + "/" + df.format(eval[1]) + "/" + df.format(eval[2]));

        // write prediction and gold standard to outfile
        Utils.writeFile(outFile, output);

    }

    static void predict(File testDataFile, File tagsFile, File modelFile, File outFile, boolean showSegmentConfidence) {
        ArrayList<String> ppdTestData = Utils.readFile(testDataFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();

        CoordinationTagger tagger = new CoordinationTagger();

        try {
            tagger.readModel(modelFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String ppdSentence : ppdTestData) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (CoordinationException e) {
                e.printStackTrace();
            }
        }
        try {
            // tagger.readModel(modelFile.toString());
            Utils.writeFile(outFile, tagger.predictIOB(sentences, showSegmentConfidence));
        } catch (Exception e) {
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
     *            an arraylist that stores both the preiductions and the gold standard labels along with the tokens
     */
    static double[] eval(ArrayList<String> ppdTrainData, ArrayList<String> ppdTestData, Tags tags,
            ArrayList<String> output, File featureConfigFile) {

        // train
        ArrayList<Sentence> trainSentences = new ArrayList<Sentence>();
        ArrayList<Sentence> testSentences = new ArrayList<Sentence>();
        CoordinationTagger tagger;
        if (featureConfigFile != null) {
            tagger = new CoordinationTagger(featureConfigFile);
        } else {
            tagger = new CoordinationTagger();
        }
        // Konvertierung von Strings zu Units
        for (String ppdTrainSentence : ppdTrainData) {
            try {
                trainSentences.add(tagger.PPDtoUnits(ppdTrainSentence));
            } catch (CoordinationException e) {
                e.printStackTrace();
            }
        }
        for (String ppdTestSentence : ppdTestData) {
            try {
                testSentences.add(tagger.PPDtoUnits(ppdTestSentence));
            } catch (CoordinationException e) {
                e.printStackTrace();
            }
        }
        tagger.train(trainSentences, tags);

        // get test data in iob format
        ArrayList<String> pos = new ArrayList<String>();
        ArrayList<String> gold = new ArrayList<String>();
        for (int i = 0; i < testSentences.size(); i++) {
            Sentence sentence = testSentences.get(i);
            for (Unit unit : sentence.getUnits()) {
                gold.add(unit.getRep() + "\t" + unit.getLabel());
                pos.add(unit.getMetaInfo(tagger.getFeatureConfig().getProperty("pos_feat_unit")));
            }
            gold.add("O\tO");
            pos.add("");
        }

        try {
            tagger.predictIOB(testSentences, false);
        } catch (CoordinationException e) {
            e.printStackTrace();
        }

        // get test data in iob format
        ArrayList<String> pred = new ArrayList<String>();
        for (int i = 0; i < testSentences.size(); i++) {
            Sentence sentence = testSentences.get(i);
            for (Unit unit : sentence.getUnits()) {
                pred.add(unit.getRep() + "\t" + unit.getLabel());
            }
            pred.add("O\tO");
        }

        // System.out.println(pred);
        // System.out.println(gold);

        // calculate performance
        double[] eval = { 0, 0, 0 };

        if (tags.type.equals("IO")) {
            // IO Evaluation
            eval = IOEvaluation.evaluate(gold, pred);
        } else {
            // IOB Evaluation
            try {
                eval = IOBEvaluation.evaluate(gold, pred);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        for (int i = 0; i < pred.size(); i++) {
            output.add(pred.get(i) + "\t" + gold.get(i).split("\t")[1] + "\t" + pos.get(i));

        }

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
    static double[] compare(File predFile, File goldFile, File tagsFile) {
        ArrayList<String> gold = Utils.readFile(goldFile);
        ArrayList<String> pred = Utils.readFile(predFile);
        Tags tags = new Tags(tagsFile.toString());

        // replace emtpy lines (end of a sentence by O\tO lines)
        for (int i = 0; i < gold.size(); i++) {
            if (gold.get(i).equals("")) {
                gold.set(i, "O\tO");
            }
        }

        // replace emtpy lines (end of a sentence by O\tO lines)
        for (int i = 0; i < pred.size(); i++) {
            if (pred.get(i).equals("")) {
                pred.set(i, "O\tO");
            }
        }

        if (gold.size() != pred.size()) {
            System.err.println(
                    "ERR: number of tokens/lines in gold standard is different from prediction... please check!");
            System.exit(-1);
        }

        double[] eval = { 0, 0, 0 };
        if (tags.type.equals("IO")) {
            // IO Evaluation
            eval = IOEvaluation.evaluate(gold, pred);
        } else {
            // IOB Evaluation
            try {
                eval = IOEvaluation.evaluate(gold, pred);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return eval;

    }

    /**
     * prints out the feature configuration used in the model 'modelFile'
     */
    public static void printFeatureConfig(File modelFile) {
        Properties featureConfig;
        CoordinationTagger tagger = new CoordinationTagger();
        try {
            tagger.readModel(modelFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        featureConfig = tagger.getFeatureConfig();
        Enumeration keys = featureConfig.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            System.out.printf("%s = %s\n", key, featureConfig.getProperty(key));
        }
    }

    /**
     * prints out the tagset used in the model 'modelFile'
     */
    public static void printOutputAlphabet(File modelFile) {
        CRF model;
        CoordinationTagger tagger = new CoordinationTagger();
        try {
            tagger.readModel(modelFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        model = tagger.getModel();
        Alphabet alpha = model.getOutputAlphabet();
        Object modelLabels[] = alpha.toArray();
        for (int i = 0; i < modelLabels.length; i++) {
            System.out.println(modelLabels[i]);
        }
    }
}
