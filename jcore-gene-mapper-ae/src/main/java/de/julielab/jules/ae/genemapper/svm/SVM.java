package de.julielab.jules.ae.genemapper.svm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.utils.FeatureUtils;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVM {
	private static final Logger log = LoggerFactory.getLogger(SVM.class);

	public static svm_problem getSvmProblem(double[] labels, double[][] featureMatrix) {

		svm_node[][] nodeMatrix = new svm_node[featureMatrix.length][];

		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			svm_node[] nodeVector = new svm_node[features.length];
			for (int j = 0; j < features.length; j++) {
				double featureValue = features[j];
				svm_node node = new svm_node();
				node.index = j + 1;
				node.value = featureValue;
				nodeVector[j] = node;

			}
			nodeMatrix[i] = nodeVector;
		}

		svm_problem prob = new svm_problem();

		prob.l = featureMatrix.length;
		prob.x = nodeMatrix;
		prob.y = labels;

		return prob;
	}

	public static double predict(double[] features, svm_model model) {
		svm_node[] featureVector = new svm_node[features.length];
		for (int i = 0; i < features.length; i++) {
			double featureValue = features[i];
			svm_node node = new svm_node();
			node.index = i + 1;
			node.value = featureValue;
			featureVector[i] = node;
		}
		// double[] dec_values = new double[1];
		// svm.svm_predict_values(model, featureVector, dec_values);
		// return dec_values[0];
		// return svm.svm_predict(model, featureVector);
		double[] prob_estimates = new double[2];
		double outcome = svm.svm_predict_probability(model, featureVector, prob_estimates);

		return outcome;
	}

	public static double[] predictProbability(double[] features, svm_model model) {
		svm_node[] featureVector = new svm_node[features.length];
		for (int i = 0; i < features.length; i++) {
			double featureValue = features[i];
			svm_node node = new svm_node();
			node.index = i + 1;
			node.value = featureValue;
			featureVector[i] = node;
		}
		double[] prob_estimates = new double[2];
		svm.svm_predict_probability(model, featureVector, prob_estimates);

		return prob_estimates;
	}

	public static SVMModel train(double[] labels, double[][] originalFeatureMatrix, SVMTrainOptions options) {

		if (originalFeatureMatrix.length == 0)
			return SVMModel.EMPTY;

		double[][] featureMatrix = originalFeatureMatrix;

		SVMModel model = new SVMModel(options);

		if (options.copyData) {
			featureMatrix = new double[originalFeatureMatrix.length][];
			for (int i = 0; i < originalFeatureMatrix.length; i++) {
				double[] features = originalFeatureMatrix[i];
				featureMatrix[i] = Arrays.copyOf(features, features.length);
			}
		}

		if (options.rangeScaleFeatures) {
			double[] maxFeatureValues = FeatureUtils.scaleFeatures(featureMatrix);
			model.maxFeatureValues = maxFeatureValues;
			model.featuresRangeScaled = options.rangeScaleFeatures;
		}
		if (options.centerFeatures && !options.standardizeFeatures) {
			double[] means = FeatureUtils.centerFeatures(featureMatrix);
			model.featureMeans = means;
			model.featuresCentered = options.centerFeatures;
		}
		if (options.standardizeFeatures) {
			StandardizationStats standardizationStats = FeatureUtils.standardizeFeatures(featureMatrix);
			model.featureMeans = standardizationStats.means;
			model.featureStdDeviations = standardizationStats.stdDeviations;
			model.featuresStandardized = options.standardizeFeatures;
		}

		svm_problem prob = getSvmProblem(labels, featureMatrix);
		svm_parameter param = getSvmParameter(options);

		String errorMsg = svm.svm_check_parameter(prob, param);
		if (errorMsg != null) {
			log.error("Error in the SVM parameters: " + errorMsg);
		} else {
			String kType = "";
			switch (param.kernel_type) {
			case svm_parameter.LINEAR:
				kType = "Linear";
				break;
			case svm_parameter.POLY:
				kType = "Polynomial";
				break;
			case svm_parameter.RBF:
				kType = "RBF";
				break;
			case svm_parameter.SIGMOID:
				kType = "Sigmoid";
				break;
			}
			log.info("Starting SVM training with settings:\n" + "Kernel type: " + kType + "\n" + "C: " + param.C + "\n"
					+ "Gamma: " + param.gamma + "\n" + "Degree: " + param.degree + "\n" + "r (coef0): " + param.coef0);
			// if (param.nr_weight > 0) {
			// LOGGER.info("NrWeight: " + param.nr_weight);
			// String weightStr = "";
			// for (int j = 0; j < param.weight.length; j++)
			// weightStr += "Label " + labelMapRev.get(param.weight_label[j]) +
			// ": Weight " + param.weight[j] + ";";
			// LOGGER.info(weightStr);
			// }
			svm_model svmModel = svm.svm_train(prob, param);
			log.info("SVM training done");
			model.svmModel = svmModel;
		}

		return model;
	}

	public static svm_parameter getSvmParameter(SVMTrainOptions options) {
		svm_parameter param = new svm_parameter();

		param.svm_type = options.svmType;
		param.C = options.C;
		param.kernel_type = options.kernelType;
		param.gamma = options.svmGamma;
		param.coef0 = options.coef0;
		param.degree = options.svmDegree;
		param.cache_size = options.cacheSize;
		param.eps = options.eps;
		param.shrinking = options.shrinking ? 1 : 0;
		param.probability = options.propability ? 1 : 0;
		return param;
	}

	public static double[] predict(double[][] featureMatrix, SVMModel model) {
		double[] outcomes = new double[featureMatrix.length];
		for (int i = 0; i < featureMatrix.length; i++) {
			double[] features = featureMatrix[i];
			if (model.featuresRangeScaled)
				FeatureUtils.rangeScaleFeatures(features, model.maxFeatureValues);
			if (model.featuresCentered && !model.featuresStandardized)
				FeatureUtils.centerFeatures(features, model.featureMeans);
			if (model.featuresStandardized)
				FeatureUtils.standardizeFeatures(features, model.featureMeans, model.featureStdDeviations);
			double outcome = predict(features, model.svmModel);
			outcomes[i] = outcome;
		}
		return outcomes;
	}

	public static double predict(double[] features, SVMModel model) {
		if (model.featuresRangeScaled)
			FeatureUtils.rangeScaleFeatures(features, model.maxFeatureValues);
		if (model.featuresCentered && !model.featuresStandardized)
			FeatureUtils.centerFeatures(features, model.featureMeans);
		if (model.featuresStandardized)
			FeatureUtils.standardizeFeatures(features, model.featureMeans, model.featureStdDeviations);
		return predict(features, model.svmModel);
	}

	public static void storeModel(File destination, SVMModel model) throws FileNotFoundException, IOException {
		try (ObjectOutputStream os = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(destination)))) {
			os.writeObject(model);
		}
	}

	/**
	 * Reads the model either from file as a classpath resource. To read
	 * classpath resources, the <em>classpath:</em> prefix must be present in
	 * <code>source</code>.
	 * 
	 * @param source
	 * @return The loaded model.
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static SVMModel readModel(String source) throws FileNotFoundException, ClassNotFoundException, IOException {
		if (source.startsWith("classpath:")) {
			// We must use getClassLoader() to get exactly that class loader that has been used to load this class. This is required 
			URL resource = SVM.class.getClassLoader().getResource(source.substring(10));
			if (null == resource)
				throw new IllegalArgumentException("The classpath resource " + source + " could not be found.");
			return readModel(resource);
		}
		return readModel(new File(source));

	}

	public static SVMModel readModel(File origin)
			throws FileNotFoundException, ClassNotFoundException, MalformedURLException, IOException {
		return readModel(origin.toURI().toURL());
	}

	public static SVMModel readModel(URL origin) throws FileNotFoundException, IOException, ClassNotFoundException {
		try (InputStream modelStream = origin.openStream()) {
			if (modelStream == null)
				throw new IllegalArgumentException("No model could be found at location " + origin.toString());
			try (ObjectInputStream is = new ObjectInputStream(new GZIPInputStream(modelStream))) {
				return (SVMModel) is.readObject();
			}
		}
	}
}
