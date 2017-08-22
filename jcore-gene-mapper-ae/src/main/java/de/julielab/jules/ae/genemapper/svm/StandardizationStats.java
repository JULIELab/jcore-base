package de.julielab.jules.ae.genemapper.svm;

public class StandardizationStats {
	public StandardizationStats(double[] means, double[] stdDevs) {
		this.means = means;
		stdDeviations = stdDevs;
	}
	public double[] means;
	public double[] stdDeviations;
}
