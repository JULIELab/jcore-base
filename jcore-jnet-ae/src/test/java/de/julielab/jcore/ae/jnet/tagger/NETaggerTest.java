package de.julielab.jcore.ae.jnet.tagger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class NETaggerTest {
	@Test
	public void testFeatureGeneration() throws Exception {
		Sentence s = new Sentence();
		Unit u = new Unit(0, 10, "IL-2 alpha");
		s.add(u);

		Properties featureConfig = new Properties();
		InputStream defaultFeatureConfigStream = getClass().getResourceAsStream("/de/julielab/jcore/ae/jnet/uima/test-featureconfig");
		featureConfig.load(defaultFeatureConfigStream);
		FeatureGenerator featureGenerator = new FeatureGenerator();
		InstanceList data = featureGenerator.createFeatureData(new ArrayList<>(Arrays.asList(s)), featureConfig);
		Instance instance = data.get(0);
		System.out.println(instance.getData());
	}
}
