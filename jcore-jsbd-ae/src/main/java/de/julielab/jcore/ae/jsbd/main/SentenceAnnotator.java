/** 
 * SentenceAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.3
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a wrapper to the JULIE Sentence Boundary Detector (JSBD). 
 * It splits a text into single sentences and adds annotations of 
 * the type Sentence to the respective UIMA (J)Cas.
 **/

package de.julielab.jcore.ae.jsbd.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jsbd.SentenceSplitter;
import de.julielab.jsbd.Unit;
import de.julielab.jcore.types.Sentence;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_MODEL_FILE = "ModelFilename";
	public static final String PARAM_POSTPROCESSING = "Postprocessing";
	public static final String PARAM_PROCESSING_SCOPE = "ProcessingScope";
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceAnnotator.class);

	// activate post processing
	@ConfigurationParameter(name = PARAM_POSTPROCESSING, mandatory = false)
	private String postprocessingFilter = null;

	@ConfigurationParameter(name = PARAM_PROCESSING_SCOPE, mandatory = false)
	private String processingScope;

	@ConfigurationParameter(name = PARAM_MODEL_FILE, mandatory = true)
	private String modelFilename;

	private SentenceSplitter sentenceSplitter;
	private Type scopeType;

	/**
	 * initiaziation of JSBD: load the model, set post processing
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("[JSBD] initializing...");

		// invoke default initialization
		super.initialize(aContext);

		// get parameters
		
		// initialize sentenceSplitter
		sentenceSplitter = new SentenceSplitter();
		try {
			LOGGER.info("[JSBD] initializing JSBD Annotator ...");
			// Get configuration parameter values
			modelFilename = (String) aContext.getConfigParameterValue(PARAM_MODEL_FILE);

			InputStream modelIs;
			File modelFile = new File(modelFilename);
			if (modelFile.exists()) {
				modelIs = new FileInputStream(modelFile);
			} else {
				LOGGER.debug("File \"{}\" does not exist. Searching for the model as a classpath resource.",
						modelFilename);
				modelIs =
						this.getClass().getResourceAsStream(
								modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename);
				if (null == modelIs)
					throw new IllegalArgumentException("The model file \"" + modelFilename
							+ "\" could be found neither in the file system nor in the classpath.");
			}
			sentenceSplitter.readModel(modelIs);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
		
		// this parameter is not mandatory, so first check whether it is there
		Object pp = aContext.getConfigParameterValue("Postprocessing");
		if (pp != null) {
			postprocessingFilter = (String) aContext.getConfigParameterValue("Postprocessing");
		}

		// this parameter is not mandatory, so first check whether it is there
		Object obj = aContext.getConfigParameterValue("ProcessingScope");
		if (obj != null) {
			processingScope = (String) aContext.getConfigParameterValue("ProcessingScope");
			processingScope = processingScope.trim();
			if (processingScope.length() == 0)
				processingScope = null;
		} else {
			processingScope = null;
		}
		LOGGER.info("initialize() - processing scope set to: " + ((processingScope == null) ? "document text"
				: processingScope));

		// initialize sentenceSplitter
//		sentenceSplitter = new SentenceSplitter();
//		try {
//			modelFilename = (String) aContext.getConfigParameterValue(PARAM_MODEL_FILE);
//			InputStream is;
//			File modelFile = new File(modelFilename);
//			if (!modelFile.exists()) {
//				// perhaps the parameter value does not point to a file but to a classpath resource
//				String resourceLocation = modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename;
//				is = getClass().getResourceAsStream(resourceLocation);
//			} else {
//				is = new FileInputStream(modelFile);
//			}
//
//			sentenceSplitter.readModel(is);
//		} catch (Exception e) {
//			LOGGER.error("[JSBD] Could not load sentence splitter model: " + e.getMessage());
//			throw new ResourceInitializationException();
//		}
	}

	/**
	 * process method is in charge of doing the sentence splitting. If processingScope is set, we iterate over
	 * Annotation objects of this type and do the sentence splitting within this scope. Otherwise, the whole document
	 * text is considered.
	 * 
	 * @throws AnalysisEngineProcessException
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		// get the text from the cas
		if (processingScope != null) {
			// loop of annotation object of the scope
			try {
				if (null == scopeType) {
					scopeType = aJCas.getTypeSystem().getType(processingScope);
					if (null == scopeType)
						throw new AnalysisEngineProcessException(new IllegalArgumentException(
								"The processing scope type " + processingScope
										+ " for parameter "
										+ PARAM_PROCESSING_SCOPE
										+ " was not found in the type system."));
				}
				FSIterator<org.apache.uima.jcas.tcas.Annotation> iter = aJCas.getAnnotationIndex(scopeType).iterator();
				while (iter.hasNext()) {
					org.apache.uima.jcas.tcas.Annotation annotation = iter.next();
					String annoText = annotation.getCoveredText();
					if (annoText != null && annoText.length() > 0) {
						doSegmentation(aJCas, annoText);
					}
				}
			} catch (Exception e) {
				LOGGER.error("process() - could not create Annotation object for the ProcessingScope.");
				throw new AnalysisEngineProcessException();
			}
		} else {
			// if no processingScope set -> use documentText
			if (aJCas.getDocumentText() != null && aJCas.getDocumentText().length() > 0) {
				doSegmentation(aJCas, aJCas.getDocumentText());
			} else {
				LOGGER.warn("process() - document text empty. Skipping this document.");
			}
		}
	}

	private void doSegmentation(JCas aJCas, String text) throws AnalysisEngineProcessException {
		ArrayList<String> lines = new ArrayList<String>();
		lines.add(text);

		// make prediction
		ArrayList<Unit> units;
		units = sentenceSplitter.predict(lines, postprocessingFilter);

		// add to UIMA annotations
		addAnnotations(aJCas, units);
	}

	/**
	 * Add all the sentences to CAS. Sentence is split into single units, for each such unit we decide whether this unit
	 * is at the end of a sentence. If so, this unit gets the label "EOS" (end-of-sentence).
	 * 
	 * @param aJCas
	 *            the associated JCas
	 * @param units
	 *            all sentence units as returned by JSBD
	 */
	private void addAnnotations(JCas aJCas, ArrayList<Unit> units) {
		int start = 0;
		for (int i = 0; i < units.size(); i++) {
			Unit myUnit = units.get(i);
			String decision = units.get(i).label;

			if (start == -1) { // now a new sentence is starting
				start = myUnit.begin;
			}

			if (decision.equals("EOS") || (i == units.size() - 1)) {
				// end-of-sentence predicted (EOS)
				// or last unit reached (finish a last sentence here!)
				Sentence annotation = new Sentence(aJCas);
				annotation.setBegin(start);
				annotation.setEnd(myUnit.end);
				annotation.setComponentId(this.getClass().getName());
				annotation.addToIndexes();
				start = -1;
			}

		}
	}
}
