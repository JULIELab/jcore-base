/** 
 * MaxEntScorerML.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 2.2
 * Since version:   1.2
 *
 * Creation date: Jul 19, 2007 
 * 
 * Class for training and prediction.
 **/

package de.julielab.jules.ae.genemapper.scoring;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Token2FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;

public class MaxEntScorerML {

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxEntScorerML.class);

	public InstanceList makeInstances(ArrayList<String[]> pairList, Pipe pipe) {
		LOGGER.debug("makeInstances() - making instances for pairs with old pipe ...");
		InstanceList iList = new InstanceList(pipe);
		for (int i = 0; i < pairList.size(); ++i) {
			iList.addThruPipe(new Instance(pairList.get(i), "", "", ""));
		}
		return iList;
	}

	public InstanceList makeInstances(ArrayList<String[]> pairList) {
		LOGGER.debug("makeInstances() - making instances for pairs with new pipe ...");
		SerialPipes pipe = new SerialPipes(new Pipe[] { new MaxEntScorerFeaturePipe(), new Token2FeatureVector() });
		InstanceList iList = new InstanceList(pipe);
		for (int i = 0; i < pairList.size(); ++i) {
			iList.addThruPipe(new Instance(pairList.get(i), "", "", ""));
		}
		return iList;
	}

	/**
	 * train the classifier
	 * 
	 * @param iList
	 * @return the classifier
	 */
	public Classifier train(InstanceList iList) {
		LOGGER.debug("train() - training the model from " + iList.size() + " training examples ...");
		MaxEntTrainer trainer = new MaxEntTrainer();
		Classifier meModel = trainer.train(iList);
		return meModel;
	}

	/**
	 * return the probability of the true class
	 * 
	 * @param inst
	 * @param model
	 * @return probability
	 */
	public double predict(Instance inst, Classifier model) {
		return getProbabilityTrueClass(model.classify(inst));
	}

	public void eval(Classifier model, InstanceList pairList) {
		ArrayList classifications = model.classify(pairList);
		for (Iterator iter = classifications.iterator(); iter.hasNext();) {
			Classification c = (Classification) iter.next();
			Labeling labeling = c.getLabeling();

			double predValue = getProbabilityTrueClass(c);
			System.out.println("           pair: " + c.getInstance().getSource());
			System.out.println("predicted score: " + predValue);
			System.out.println("  correct class: " + c.getInstance().getName());
			System.out.println("predicted class: " + labeling.getBestLabel() + "\n");

		}
	}

	/**
	 * gets the probability of the TRUE class
	 * 
	 * @param c
	 *            a classification
	 * @return probability
	 */
	private double getProbabilityTrueClass(Classification c) {
		// get value for "TRUE" label
		Labeling labeling = c.getLabeling();
		LabelAlphabet dict = labeling.getLabelAlphabet();
		Label label = dict.lookupLabel("TRUE");
		double predValue = labeling.value(label);
		return predValue;
	}
}
