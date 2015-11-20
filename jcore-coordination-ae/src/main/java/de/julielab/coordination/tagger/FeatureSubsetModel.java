/**
 * FeatureSubsetModel.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: faessler
 *
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: 29.01.2007
 *
 * This class contains a CRF4 model as well as a properties object "featureConfig". The featureConfig
 * object contains information about the feature subset used with this CRF4 model.
 **/

package de.julielab.coordination.tagger;

import java.io.Serializable;
import java.util.Properties;

import cc.mallet.fst.CRF;

public class FeatureSubsetModel implements Serializable {

    private CRF model;

    private Properties featureConfig;

    static final long serialVersionUID = 23; // used since V 1.3

    FeatureSubsetModel() {
        this.model = null;
        this.featureConfig = null;
    }

    FeatureSubsetModel(CRF model, Properties featureConfig) {
        this.model = model;
        this.featureConfig = featureConfig;
    }

    void setModel(CRF model) {
        this.model = model;
    }

    void setFeatureConfig(Properties featureConfig) {
        this.featureConfig = featureConfig;
    }

    CRF getModel() {
        return this.model;
    }

    Properties getFeatureConfig() {
        return this.featureConfig;
    }
}
