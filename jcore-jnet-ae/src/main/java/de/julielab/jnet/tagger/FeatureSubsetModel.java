/** 
 * FeatureSubsetModel.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: faessler
 * 
 * Current version: 2.4
 * Since version:   2.2
 *
 * Creation date: 29.01.2007 
 * 
 * This class contains a CRF4 model as well as a properties object "featureConfig". The featureConfig
 * object contains information about the feature subset used with this CRF4 model.
 **/

package de.julielab.jnet.tagger;

import java.io.Serializable;
import java.util.Properties;

public class FeatureSubsetModel implements Serializable {

	private Object model;

	private Properties featureConfig;

	static final long serialVersionUID = 24; // used since V 2.4

	FeatureSubsetModel() {
		model = null;
		featureConfig = null;
	}

	FeatureSubsetModel(final Object model, final Properties featureConfig) {
		this.model = model;
		this.featureConfig = featureConfig;
	}

	void setModel(final Object model) {
		this.model = model;
	}

	void setFeatureConfig(final Properties featureConfig) {
		this.featureConfig = featureConfig;
	}

	Object getModel() {
		return model;
	}

	Properties getFeatureConfig() {
		return featureConfig;
	}
}
