package de.julielab.jules.ae.genemapper.utils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.svm.SVMTrainData;
import de.julielab.jules.ae.genemapper.svm.StandardizationStats;
import libsvm.svm_node;

public class FeatureUtils {
	public static boolean isNumberCompatible(String normalizedMention, String synonym) {
		String[] mentionSplit = normalizedMention.split("\\s");
		String[] synSplit = synonym.split("\\s");
		Multiset<String> mentionNumbers = getNumbers(mentionSplit);
		Multiset<String> synNumbers = getNumbers(synSplit);

		return mentionNumbers.size() == synNumbers.size()
				&& Multisets.intersection(mentionNumbers, synNumbers).size() == mentionNumbers.size();
	}

	public static Multiset<String> getNumbers(String[] tokens) {
		Multiset<String> numberTokens = HashMultiset.create();
		for (String token : tokens) {
			if (token.matches("[0-9]+"))
				numberTokens.add(token);
		}
		return numberTokens;
	}

	public static Multiset<String> getNumberOfCommonTokens(String normalizedMention, String synonym) {
		String[] mentionSplit = normalizedMention.split("\\s");
		String[] synSplit = synonym.split("\\s");
		Multiset<String> mentionTokens = HashMultiset.create();
		Multiset<String> synTokens = HashMultiset.create();
		for (int i = 0; i < mentionSplit.length; i++) {
			String mentionToken = mentionSplit[i];
			mentionTokens.add(mentionToken);
		}
		for (int i = 0; i < synSplit.length; i++) {
			String synToken = synSplit[i];
			synTokens.add(synToken);
		}
		return Multisets.intersection(mentionTokens, synTokens);
	}

	public static double[] scaleFeatures(double[][] featureMatrix) {
		double[] maxFeatureValues = new double[featureMatrix[0].length];
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			svm_node[] nodeVector = new svm_node[features.length];
			for (int j = 0; j < features.length; j++) {
				double featureValue = features[j];
				svm_node node = new svm_node();
				node.index = j;
				node.value = featureValue;
				nodeVector[j] = node;

				if (featureValue > maxFeatureValues[j])
					maxFeatureValues[j] = featureValue;
			}
		}

		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				features[j] /= maxFeatureValues[j];
			}
		}
		return maxFeatureValues;
	}

	public static void rangeScaleFeatures(double[] features, double[] maxFeatureValues) {
		for (int i = 0; i < features.length; i++) {
			features[i] /= maxFeatureValues[i];
		}
	}

	public static double[] centerFeatures(double[][] featureMatrix) {
		double[] means = new double[featureMatrix[0].length];
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				double featureValue = features[j];
				means[j] += featureValue;
			}
		}
		for (int i = 0; i < means.length; i++) {
			means[i] /= featureMatrix.length;
		}
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				features[j] -= means[j];
			}
		}
		return means;
	}

	public static void centerFeatures(double[] features, double[] featureMeans) {
		for (int i = 0; i < features.length; i++) {
			features[i] -= featureMeans[i];
		}
	}

	public static StandardizationStats standardizeFeatures(double[][] featureMatrix) {
		double[] means = new double[featureMatrix[0].length];
		double[] stdDevs = new double[featureMatrix[0].length];
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				double featureValue = features[j];
				means[j] += featureValue;
			}
		}
		for (int i = 0; i < means.length; i++) {
			means[i] /= featureMatrix.length;
		}
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				double featureValue = features[j];
				stdDevs[j] += Math.pow(featureValue - means[j], 2);
			}
		}

		for (int i = 0; i < stdDevs.length; i++) {
			stdDevs[i] /= featureMatrix.length;
			stdDevs[i] = Math.sqrt(stdDevs[i]);
		}
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			for (int j = 0; j < features.length; j++) {
				features[j] -= means[j];
				features[j] /= stdDevs[j];
				if (Double.isNaN(features[j]))
					features[j] = 0;
			}
		}
		return new StandardizationStats(means, stdDevs);
	}

	public static void standardizeFeatures(double[] features, double[] featureMeans, double[] featureStdDeviations) {
		if (features.length != featureMeans.length)
			throw new IllegalArgumentException(
					"Feature dimension differs from the number of feature means. This means that the wrong model is being used for prediction. Feature dimension: "
							+ features.length + ", number of feature means: " + featureMeans.length);
		for (int i = 0; i < features.length; i++) {
			features[i] -= featureMeans[i];
			features[i] /= featureStdDeviations[i];
			if (Double.isNaN(features[i]))
				features[i] = 0;
		}
	}

	public static SVMTrainData removeDuplicates(SVMTrainData data) {
		double[][] featureMatrix = data.featureMatrix;
		Set<FeatureVector> uniqueFeatureVectors = new LinkedHashSet<>(featureMatrix.length);
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			uniqueFeatureVectors.add(new FeatureVector(features, i));
		}
		double[][] uniqueFeatureMatrix = new double[uniqueFeatureVectors.size()][];
		double[] uniqueFeatureVectorLabels = new double[uniqueFeatureVectors.size()];
		GeneMention[] uniqueFeatureVectorGenes = new GeneMention[uniqueFeatureVectors.size()];
		int i = 0;
		for (FeatureVector featureVector : uniqueFeatureVectors) {
			uniqueFeatureMatrix[i] = featureMatrix[featureVector.index];
			uniqueFeatureVectorLabels[i] = data.labels[featureVector.index];
			uniqueFeatureVectorGenes[i] = data.geneList.get(featureVector.index);
			i++;
		}
		SVMTrainData uniqueData = new SVMTrainData(uniqueFeatureVectorLabels, uniqueFeatureMatrix);
		uniqueData.geneList = Arrays.asList(uniqueFeatureVectorGenes);
		return uniqueData;
	}

	private static class FeatureVector {
		double[] features;
		private int index;

		public FeatureVector(double[] features, int index) {
			this.features = features;
			this.index = index;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(features);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FeatureVector other = (FeatureVector) obj;
			if (!Arrays.equals(features, other.features))
				return false;
			return true;
		}

	}
}
