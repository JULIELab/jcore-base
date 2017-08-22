package de.julielab.jules.ae.genemapper.svm;

import java.util.List;

import de.julielab.jules.ae.genemapper.genemodel.GeneMention;

public class SVMTrainData {
	public SVMTrainData(double[] labels, double[][] featureMatrix) {
		this.labels = labels;
		this.featureMatrix = featureMatrix;
	}

	public double[] labels;
	public double[][] featureMatrix;
	/**
	 * For each i in {0, ..., labels.size()} contains the gene mention
	 * corresponding to the respective index in labels and featureMatrix or is null.
	 */
	public List<GeneMention> geneList;
	public int numMentionsRejected;
}
