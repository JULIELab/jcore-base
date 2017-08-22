package de.julielab.jules.ae.genemapper.svm;

import java.io.Serializable;

import libsvm.svm_model;

public class SVMModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3037185834007887273L;
	public SVMTrainOptions trainOptions;
	public SVMModel(SVMTrainOptions options) {
		this.trainOptions = options;
	}
	public static final SVMModel EMPTY = new SVMModel(null);
	public boolean featuresRangeScaled;
	public boolean featuresCentered;
	public boolean featuresStandardized;
	public double[] maxFeatureValues;
	public double[] featureMeans;
	public double[] featureStdDeviations;
	public svm_model svmModel;
}
