/** 
 * CoordinationElementAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: lichtenwald, buyko
 * 
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: 11.04.2008 
 * 
 * Coordination element annotator that invokes the baseline prediction of coordination 
 * elements within a given EEE (elliptical entity expression). This annotator assumes 
 * that tokens, POS tags and EEEs have been annotated in the CAS. 
 **/
package de.julielab.jcore.ae.coordination.annotators.main;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CoordinationElementAnnotator {

	public static final Logger LOGGER = LoggerFactory.getLogger(CoordinationElementAnnotator.class);

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to predict coordination elements of every EEE within the JCas
	 * 
	 * @param jcas
	 *            JCas which will be used to retrieve EEE from it and to predict the coordination
	 *            elements
	 * @return
	 */
	public abstract void predictCoordinationElements(JCas jcas);

	public abstract void setModel(UimaContext aContext) throws AnnotatorConfigurationException,
					AnnotatorContextException, AnnotatorInitializationException;
} // CoordinationElementAnnotator
