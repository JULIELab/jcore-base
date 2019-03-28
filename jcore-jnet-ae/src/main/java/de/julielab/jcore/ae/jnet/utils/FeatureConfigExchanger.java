/** 
 * FeatureConfigExchanger.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 * 
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Sep 27, 2007 
 * 
 * class to exchange the feature config of an already saved model
 **/

package de.julielab.jcore.ae.jnet.utils;

import de.julielab.jnet.tagger.NETagger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class FeatureConfigExchanger {

	public static void main(final String[] args) throws FileNotFoundException,
			IOException, ClassNotFoundException {
		if (args.length != 3) {
			System.err
					.println("Usage: FeatureConfigExchanger <model file> <new model file> <new feature config file>");
			System.exit(-1);
		}
		final String orgModelName = args[0];
		final String newModelName = args[1];
		final String newFeatureConfig = args[2];
		final NETagger tagger = new NETagger();
		tagger.readModel(new FileInputStream(orgModelName));
		final Properties featureConfig = new Properties();
		featureConfig.load(new FileInputStream(newFeatureConfig));
		tagger.setFeatureConfig(featureConfig);
		tagger.writeModel(newModelName);

		System.out.println("wrote model with new feature config to: "
				+ newModelName);
	}
}
