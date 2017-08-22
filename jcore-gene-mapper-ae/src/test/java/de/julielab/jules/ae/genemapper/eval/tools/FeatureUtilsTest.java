package de.julielab.jules.ae.genemapper.eval.tools;

import java.util.Arrays;

import org.junit.Test;

import de.julielab.jules.ae.genemapper.svm.StandardizationStats;
import de.julielab.jules.ae.genemapper.utils.FeatureUtils;

public class FeatureUtilsTest {
	@Test
	public void testStandardizeFeatures() {
		double[][] featureMatrix = new double[2][];
		featureMatrix[0] = new double[]{20, 7};
		featureMatrix[1] = new double[]{4, 6};
		System.out.println(Arrays.toString(featureMatrix[0]));
		System.out.println(Arrays.toString(featureMatrix[1]));
		System.out.println();
		StandardizationStats features = FeatureUtils.standardizeFeatures(featureMatrix);
		System.out.println(Arrays.toString(features.means));
		System.out.println(Arrays.toString(features.stdDeviations));
		System.out.println();
		System.out.println(Arrays.toString(featureMatrix[0]));
		System.out.println(Arrays.toString(featureMatrix[1]));
	}
}
