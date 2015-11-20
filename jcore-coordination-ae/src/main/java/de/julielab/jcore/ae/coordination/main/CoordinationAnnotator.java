/**
 * CoordinationAnnotator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: Lichtenwald, Buyko
 *
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: 09.04.2008
 **/
package de.julielab.jcore.ae.coordination.main;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.coordination.annotators.baseline.BaselineCoordinationElementAnnotator;
import de.julielab.jcore.ae.coordination.annotators.main.CoordinationElementAnnotator;
import de.julielab.jcore.ae.coordination.annotators.main.EEEAnnotator;
import de.julielab.jcore.ae.coordination.annotators.main.EllipsisAnnotator;
import de.julielab.jcore.ae.coordination.annotators.ml.MLCoordinationElementAnnotator;

/**
 * This annotator invokes either the baseline prediction of elliptical entity expressions (EEEs),
 * coordination elements (conjuncts, conjunctions and antecedents) and the resolved ellipsis or the
 * machine learning approach to the prediction of the annotations named above. The annotator assumes
 * that tokens, POS tags and entity mentions have been annotated in the Cas.
 * 
 * @author lichtenwald, buyko
 */
public class CoordinationAnnotator extends JCasAnnotator_ImplBase {

	private final static String COMPONENT_ID_BASELINE = "jules-coordination-tagger-baseline-ae";
	private static final String COMPONENT_ID_ML = "jules-coordination-tagger-ml-ae";
	public static String COMPONENT_ID = COMPONENT_ID_BASELINE;
	private final static Logger LOGGER = LoggerFactory.getLogger(CoordinationAnnotator.class);
	public final static String EMPTY_STRING = "";
	public final static String SPACE_CHAR = " ";
	public final static String CONJUNCT = "conjunct";
	public final static String ANTECEDENT = "antecedent";
	public final static String CONJUNCTION = "conjunction";
	public final static String ML = "ml";
	public final static String BASELINE = "baseline";
	public final static String ENTITY = "entity";
	public final static String PHRASE = "phrase";
	public final static String COORDINATION = "coordination";
	public final static String MODE = "mode";
	public static final String MODEL = "modelFile";
	public final static String OBJECT_OF_ANALYSIS = "objectOfAnalysis";
	public final static String RESULT_OF_ANALYSIS = "resultOfAnalysis";
	public final static String USED_CONJUNCTIONS = "usedConjunctions";
	public final static String[] DEFAULT_CONJUNCTIONS_ARRAY = { "and", "or", "," };
	public static String value_mode = null;
	public static String value_object_of_analysis = null;
	public static String value_result_of_analysis = null;
	public static String[] used_conjunctions_array = null;
	private EEEAnnotator eeeAnnotator;
	private CoordinationElementAnnotator coordinationElementAnnotator;
	private EllipsisAnnotator ellipsisAnnotator;

	/*--------------------------------------------------------------------------------------------*/
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("CoordinationAnnotator is being initialized ... ");
		// invoke default initialization
		try {
			super.initialize(aContext);
			// get parameters from UIMA context
			value_mode = (String) aContext.getConfigParameterValue(MODE);
			value_object_of_analysis = (String) aContext.getConfigParameterValue(OBJECT_OF_ANALYSIS);
			value_result_of_analysis = (String) aContext.getConfigParameterValue(RESULT_OF_ANALYSIS);
			used_conjunctions_array = (String[]) aContext.getConfigParameterValue(USED_CONJUNCTIONS);
			
			LOGGER.info("Coordination Tagger in mode " + value_mode + ", object of analysis = " + value_object_of_analysis + ", value result of analysis = " + value_result_of_analysis);
			
			eeeAnnotator = new EEEAnnotator();
			ellipsisAnnotator = new EllipsisAnnotator();
			if (value_mode.equals(BASELINE)) {
				coordinationElementAnnotator = new BaselineCoordinationElementAnnotator();
			}
			// set parameters for the crf-based approach for coordination resolution
			if (value_mode.equals(ML)) {
				COMPONENT_ID = COMPONENT_ID_ML;
				coordinationElementAnnotator = new MLCoordinationElementAnnotator();
				coordinationElementAnnotator.setModel(aContext);
			}
			if (coordinationElementAnnotator == null){
				LOGGER.error("Check the mode, set to 'baseline' or 'ml'");
				System.exit(-1);
				
			}
		} catch (AnnotatorConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AnnotatorContextException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AnnotatorInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} // of initialize

	/*--------------------------------------------------------------------------------------------*/
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		LOGGER.info("CoordinationAnnotator is being processed ... ");
		if (checkParameters()) {
			boolean eeePredicted = eeeAnnotator.predictEEE(jcas);
			if (eeePredicted) {
				coordinationElementAnnotator.predictCoordinationElements(jcas);
				ellipsisAnnotator.predictEllipsis(jcas);
			} // of if
		} else {
			LOGGER.error("Cannot continue coordination processing. Please check the parameters!");
		} // of if
	} // of process

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Checks the validity of the parameters obtained from the descriptor. Parameters are valid if
	 * they all have values and if these values are allowed
	 * 
	 * @return valid boolean which indicates the validity of the parameters
	 */
	private boolean checkParameters() {
		boolean valid = true;
		if (value_mode == null || !(value_mode.equals(ML)) && !(value_mode.equals(BASELINE))) {
			LOGGER.error("Parameter " + MODE + " has an invalid value: " + value_mode + ". Allowed are only \"" + ML
							+ "\" and \"" + BASELINE + "\".");
			valid = false;
		} // of if
		if (value_object_of_analysis == null
						|| !(value_object_of_analysis.equals(ENTITY) || value_object_of_analysis.equals(PHRASE))) {
			LOGGER.error("Parameter " + OBJECT_OF_ANALYSIS + " has an invalid value: " + value_object_of_analysis
							+ ". Allowed are only \"" + ENTITY + "\" and \"" + PHRASE + "\".");
			valid = false;
		} // of if
		if (value_result_of_analysis == null
						|| !(value_result_of_analysis.equals(ENTITY) || value_result_of_analysis.equals(COORDINATION))) {
			LOGGER.error("Parameter " + RESULT_OF_ANALYSIS + " has an invalid value: " + value_result_of_analysis
							+ ". Allowed are only \"" + ENTITY + "\" and \"" + COORDINATION + "\".");
			valid = false;
		} // of if
		if (used_conjunctions_array.length == 0) {
			LOGGER.info("Parameter " + USED_CONJUNCTIONS + " was not set; using default conjunctions.");
			used_conjunctions_array = DEFAULT_CONJUNCTIONS_ARRAY;
		} // of if
		return valid;
	} // of checkParameters
	/*--------------------------------------------------------------------------------------------*/
} // of CoordinationAnnotator
