package lingscope.algorithms;

//import generalutils.Statistics;

import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

import java.util.List;

/**
 * Compares two annotations or two annotation files
 * @author shashank
 */
public class AnnotationComparer {

    private double tp;
    private double fp;
    private double fn;
    private double tn;
    private double perfectMatches;
    private int totalSentences;
    private int numFolds;
    private double[] tpFolds;
    private double[] fpFolds;
    private double[] fnFolds;
    private double[] tnFolds;
    private double[] perfectMatchesFolds;
    private double[] totalSentencesFolds;

    public AnnotationComparer(int numFolds) {
        this.numFolds = numFolds;
        reset();
    }

    /**
     * Resets the values for the comparer
     */
    public final void reset() {
        tp = 0;
        fp = 0;
        fn = 0;
        tn = 0;
        perfectMatches = 0;
        totalSentences = 0;
        tpFolds = new double[numFolds];
        resetFold(tpFolds, numFolds);
        fpFolds = new double[numFolds];
        resetFold(fpFolds, numFolds);
        fnFolds = new double[numFolds];
        resetFold(fnFolds, numFolds);
        tnFolds = new double[numFolds];
        resetFold(tnFolds, numFolds);
        perfectMatchesFolds = new double[numFolds];
        resetFold(perfectMatchesFolds, numFolds);
        totalSentencesFolds = new double[numFolds];
        resetFold(totalSentencesFolds, numFolds);
    }

    private void resetFold(double[] folds, int numFolds) {
        for (int i = 0; i < numFolds; ++i) {
            folds[i] = 0;
        }
    }

    public void compareAnnotationFiles(String goldFile, String testFile) {
        List<AnnotatedSentence> goldSentences = AnnotatedSentencesIO.read(goldFile);
        List<AnnotatedSentence> testSentences = AnnotatedSentencesIO.read(testFile);
        if (goldSentences.size() != testSentences.size()) {
            throw new RuntimeException("Number of sentences in gold and test file are not same");
        }
        for (int i = 0; i < goldSentences.size(); ++i) {
            AnnotatedSentence goldSentence = goldSentences.get(i);
            AnnotatedSentence testSentence = testSentences.get(i);
            compareAnnotations(goldSentence, testSentence);
        }
    }

    public void compareAnnotations(AnnotatedSentence goldSentence, AnnotatedSentence testSentence) {
        int localTp = 0;
        int localFp = 0;
        int localFn = 0;
        int localTn = 0;
        if (goldSentence.getIsAnnotatedTags().size() != testSentence.getIsAnnotatedTags().size()) {
            System.err.println("Size mismatch GOLD: " + goldSentence.getRawText());
            System.err.println("Size mismatch TEST: " + testSentence.getRawText());
        }
        int numTags = Math.min(goldSentence.getIsAnnotatedTags().size(), testSentence.getIsAnnotatedTags().size());
        for (int i = 0; i < numTags; ++i) {
            boolean goldTag = goldSentence.getIsAnnotatedTags().get(i);
            boolean testTag = testSentence.getIsAnnotatedTags().get(i);
            if (goldTag && testTag) {
                ++localTp;
            } else if (goldTag && (!testTag)) {
                ++localFn;
            } else if ((!goldTag) && testTag) {
                ++localFp;
            } else if ((!goldTag) && (!testTag)) {
                ++localTn;
            }
        }
        int foldNum = totalSentences % numFolds;
        tp += localTp;
        fp += localFp;
        fn += localFn;
        tn += localTn;
        tpFolds[foldNum] += localTp;
        fpFolds[foldNum] += localFp;
        fnFolds[foldNum] += localFn;
        tnFolds[foldNum] += localTn;
        if (localFp != 0) {
            System.out.println("FP Gold: " + goldSentence.getRawText());
            System.out.println("FP Test: " + testSentence.getRawText());
        }
        if (localFn != 0) {
            System.out.println("FN Gold: " + goldSentence.getRawText());
            System.out.println("FN Test: " + testSentence.getRawText());
        }
        if (localFp == 0 && localFn == 0) {
            ++perfectMatches;
            ++perfectMatchesFolds[foldNum];
        }
        ++totalSentences;
        ++totalSentencesFolds[foldNum];
    }

    public void printStats() {
        System.out.println("TP: " + tp);
        System.out.println("FP: " + fp);
        System.out.println("FN: " + fn);
        System.out.println("TN: " + tn);
        double precision = getPrecision(tp, fp);
        double recall = getRecall(tp, fn);
        System.out.println("Overall precision: " + precision);
        System.out.println("Overall recall: " + recall);
        System.out.println("Overall F1-score: " + getFScore(recall, precision));
        System.out.println("Overall Accuracy: " + getAccuracy(tp, fp, fn, tn));
        System.out.println("Overall Perfect : " + getPerfectAccuracy(perfectMatches, totalSentences));
        double[] recalls = getRecalls();
        double[] precisions = getPrecisions();
        double[] fScores = getFScores(recalls, precisions);
        double[] accuracies = getAccuracies();
        double[] perfectAccuracies = getPerfectAccuracies();
        System.out.println("WARNING: Here should have been printed average scores. However, the lines were removed from the JCoRe version because a required class was not available. This should be easy to fix if required.");
//        System.out.println("Average precision: " + Statistics.mean(precisions) + " +/- " + Statistics.stdDev(precisions));
//        System.out.println("Average recall: " + Statistics.mean(recalls) + " +/- " + Statistics.stdDev(recalls));
//        System.out.println("Average F1-score: " + Statistics.mean(fScores) + " +/- " + Statistics.stdDev(fScores));
//        System.out.println("Average Accuracy: " + Statistics.mean(accuracies) + " +/- " + Statistics.stdDev(accuracies));
//        System.out.println("Average Perfect: " + Statistics.mean(perfectAccuracies) + " +/- " + Statistics.stdDev(perfectAccuracies));
    }

    public double[] getRecalls() {
        double[] recalls = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            recalls[i] = getRecall(tpFolds[i], fnFolds[i]);
        }
        return recalls;
    }

    public double[] getPrecisions() {
        double[] precisions = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            precisions[i] = getPrecision(tpFolds[i], fpFolds[i]);
        }
        return precisions;
    }

    public double[] getFScores(double[] recalls, double[] precisions) {
        double[] fScores = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            fScores[i] = getFScore(recalls[i], precisions[i]);
        }
        return fScores;
    }

    public double[] getAccuracies() {
        double[] accuracies = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            accuracies[i] = getAccuracy(tpFolds[i], fpFolds[i], fnFolds[i], tnFolds[i]);
        }
        return accuracies;
    }

    public double[] getPerfectAccuracies() {
        double[] pcss = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            pcss[i] = getPerfectAccuracy(perfectMatchesFolds[i], totalSentencesFolds[i]);
        }
        return pcss;
    }

    public double getPerfectAccuracy(double trues, double total) {
        return trues / total;
    }

    public double getAccuracy(double tp, double fp, double fn, double tn) {
        return (tp + tn) / (tp + fp + fn + tn);
    }

    public double getRecall(double tp, double fn) {
        return tp / (tp + fn);
    }

    public double getPrecision(double tp, double fp) {
        return tp / (tp + fp);
    }

    public double getFScore(double recall, double precision) {
        return 2 * precision * recall / (recall + precision);
    }
}
