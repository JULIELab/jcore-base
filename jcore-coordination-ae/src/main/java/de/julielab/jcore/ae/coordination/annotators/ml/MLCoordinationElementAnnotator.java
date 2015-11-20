/** 
 * CoordinationElementAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: lichtenwald, buyko, tomanek
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
package de.julielab.jcore.ae.coordination.annotators.ml;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import de.julielab.coordination.tagger.CoordinationException;
import de.julielab.coordination.tagger.CoordinationTagger;
import de.julielab.coordination.tagger.Unit;
import de.julielab.jcore.ae.coordination.annotators.main.CoordinationElementAnnotator;
import de.julielab.jules.coordinationtagger.Interval;
import de.julielab.jules.types.Abbreviation;
import de.julielab.jules.types.Annotation;
import de.julielab.jules.types.EntityMention;
import de.julielab.jules.types.Token;
import de.julielab.jules.types.EEE;
import de.julielab.jules.types.CoordinationElement;
import de.julielab.jcore.utility.JCoReAnnotationTools;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.*;

public class MLCoordinationElementAnnotator extends CoordinationElementAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(MLCoordinationElementAnnotator.class);
	private CoordinationTagger tagger;
	Properties featureConfig = null;
	ArrayList<String> activatedMetas = null;
	ArrayList<Iterator> annotationIterators = null;
	ArrayList<String> valueMethods = null;
	protected boolean showSegmentConf = false;
	private final static String OUTSIDE_LABEL = "OUT"; // default outside label

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to predict coordination elements of every EEE within the JCas
	 * 
	 * @param jcas
	 *            JCas which will be used to retrieve EEE from it and to predict the coordination
	 *            elements
	 */
	public void predictCoordinationElements(JCas jcas) {
		LOGGER.info("CoordinationElementAnnotator is processing ... ");
		try {
			AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
			AnnotationIndex eeeTokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
							Token.type);
			FSIterator eeeIterator = eeeIndex.iterator();
			ArrayList<Token> tokenArrayList = null;
			while (eeeIterator.hasNext()) {
				EEE eee = (EEE) eeeIterator.next();
				FSIterator eeeTokenIterator = eeeTokenIndex.subiterator(eee);
				tokenArrayList = new ArrayList<Token>();
				while (eeeTokenIterator.hasNext()) {
					Token eeeToken = (Token) eeeTokenIterator.next();
					tokenArrayList.add(eeeToken);
				} // of while
				// premark the coordination elements as antecedents
				retrieveMetaInformation(jcas);
				ArrayList<HashMap<String, String>> metaList = getMetaList(tokenArrayList);
				if (tokenArrayList.size() != metaList.size()) {
					throw new AnalysisEngineProcessException();
				}
				// make the Sentence object for JNET tagger
				de.julielab.coordination.tagger.Sentence unitSentence = createUnitSentence(tokenArrayList, jcas, metaList);
				StringBuffer unitS = new StringBuffer();
				for (Unit unit : unitSentence.getUnits()) {
					unitS.append(unit.getRep() + " ");
				}
				LOGGER.debug("process() - EEE for prediction: " + unitSentence.toString());
				// TODO Katja
				// predict with JNET CRFs
				tagger.predict(unitSentence, showSegmentConf);
				// TODO Katja
				// write predicted labels to CAS
				writeMLConjunctsToCAS(unitSentence, jcas);
			}
		} catch (AnalysisEngineProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoordinationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @author tomanek get initialization of meta information which is used later to get token level
	 *         meta-info
	 */
	public void retrieveMetaInformation(JCas aJCas) throws AnalysisEngineProcessException {
		LOGGER.debug("Retrieve Meta Information ...");
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		featureConfig = tagger.getFeatureConfig();
		activatedMetas = new ArrayList<String>();
		annotationIterators = new ArrayList<Iterator>();
		valueMethods = new ArrayList<String>();
		Enumeration keys = featureConfig.propertyNames();
		if (keys.hasMoreElements()) {
		}
		// reading which meta datas are enabled
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String meta = "";
			if (key.matches("[A-Za-z]+_feat_enabled") && featureConfig.getProperty(key).equals("true")) {
				meta = key.substring(0, key.indexOf("_feat_enabled"));
				activatedMetas.add(meta);
			}
			if (key.matches("[A-Za-z]+_feat_valMethod")) {
				meta = key.substring(0, key.indexOf("_feat_valMethod"));
				valueMethods.add(featureConfig.getProperty(key));
			}
		}
		// obtaining iterators over meta data information
		for (int i = 0; i < activatedMetas.size(); i++) {
			org.apache.uima.jcas.tcas.Annotation ann = null;
			try {
				ann = JCoReAnnotationTools.getAnnotationByClassName(aJCas, featureConfig.getProperty(activatedMetas.get(i)
								+ "_feat_data"));
				annotationIterators.add(indexes.getAnnotationIndex(ann.getTypeIndexID()).iterator());
			} catch (Exception e) {
				throw new AnalysisEngineProcessException();
			}
		}
	}

	/**
	 * @author tomanek set and load the CRF-model
	 */
	public void setModel(UimaContext aContext) throws AnnotatorConfigurationException, AnnotatorContextException,
					AnnotatorInitializationException {
		// get model filename
		String modelFilename = "";
		Object o = aContext.getConfigParameterValue(MODEL);
		if (o != null) {
			modelFilename = (String) o;
		} else {
			LOGGER.error("setModel() - descriptor incomplete, no model file specified!");
			throw new AnnotatorConfigurationException();
		}
		// produce an instance of JNET with this model
		tagger = new CoordinationTagger();// new File("src/main/resources/defaultFeatureConf.conf"));
		try {
			LOGGER.debug("setModel() -  loading CRF model...");
			File modelPath = new File(modelFilename);
			tagger.readModel(modelPath.getAbsolutePath());
			featureConfig = tagger.getFeatureConfig();
			Enumeration keys = featureConfig.propertyNames();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
			}
		} catch (Exception e) {
			LOGGER.error("setModel() - Could not load CRF model: " + e.getMessage(), e);
			throw new AnnotatorInitializationException();
		}
	}

	// of markAntecedents
	/*--------------------------------------------------------------------------------------------*/
	/**
	 * @author tomanek Takes all info about meta data and generates the corresponding unit sequence
	 *         represented by a Sentence object. Abbreviation is expanded when specified in
	 *         descriptor. Only abbreviations which span over single tokens can be interpreted here.
	 *         Other case (which is very rare and thus probably not relevant) is ignored!
	 * @param tokenList
	 *            a list of Token objects of the current sentence
	 * @param JCas
	 *            the CAS we are working on
	 * @param metaList
	 *            a Arraylist of meta-info HashMaps which specify the meta information of the
	 *            respective token
	 * @return an array of two sequences of units containing all available meta data for the
	 *         corresponding tokens. In the first sequence, abbreviations are expanded to their
	 *         fullform. In the second sequence, the tokens are of their original form.
	 */
	protected de.julielab.coordination.tagger.Sentence createUnitSentence(ArrayList<Token> tokenList, JCas JCas,
					ArrayList<HashMap<String, String>> metaList) {
		de.julielab.coordination.tagger.Sentence unitSentence = new de.julielab.coordination.tagger.Sentence();
		for (int i = 0; i < tokenList.size(); i++) {
			Token token = tokenList.get(i);
			HashMap<String, String> metas = metaList.get(i);
			String tokenRepresentation = token.getCoveredText();
			Unit unit = new de.julielab.coordination.tagger.Unit(token.getBegin(), token.getEnd(), token.getCoveredText(), "",
							metas);
			unitSentence.add(unit);
		}
		return unitSentence;
	}

	/**
	 * TO CHANGE
	 */
	public void writeMLConjunctsToCAS(de.julielab.coordination.tagger.Sentence unitSentence, JCas aJCas) {
		String lastLabel = OUTSIDE_LABEL;
		int lastStart = 0;
		int lastEnd = 0;
		double conf = -1;
		double lastConf = -1;
		de.julielab.coordination.tagger.Unit unit = null;
		for (int i = 0; i < unitSentence.size(); i++) {
			unit = unitSentence.get(i);
			String label = unit.getLabel();
			conf = unit.getConfidence();
			if (lastLabel.equals(OUTSIDE_LABEL) && !label.equals(OUTSIDE_LABEL)) {
				// new entity starts
				lastStart = unit.begin;
			} else if ((!lastLabel.equals(OUTSIDE_LABEL) && !label.equals(OUTSIDE_LABEL) && !lastLabel.equals(label))
							|| (!lastLabel.equals(OUTSIDE_LABEL) && label.equals(OUTSIDE_LABEL))) {
				// entity is finished, add annotation to CAS
				addAnnotation(aJCas, lastStart, lastEnd, lastLabel, lastConf);
				lastStart = unit.begin;
			}
			lastLabel = label;
			lastEnd = unit.end;
			lastConf = conf;
			if (i == unitSentence.size() - 1) {
				// last unit handled separately, add annotation to CAS
				if (!label.equals(OUTSIDE_LABEL)) {
					lastEnd = unit.end;
					addAnnotation(aJCas, lastStart, lastEnd, lastLabel, lastConf);
				}
			}
		}
	}

	/**
	 * TO CHANGE
	 */
	private void addAnnotation(JCas aJCas, int start, int end, String label, double confidence) {
		// check against negative list whether this annotation should be really added
		String coveredText = aJCas.getDocumentText().substring(start, end);
		// create EntityMention object
		CoordinationElement coordElement = null;
		try {
			coordElement = new CoordinationElement(aJCas);
		} catch (Exception e) {
			LOGGER.error("addAnnotation()", e);
		}
		// add feature values
		coordElement.setBegin(start);
		coordElement.setEnd(end);
		if (label.equals("C"))
			coordElement.setCat(CONJUNCT);
		if (label.equals("O"))
			coordElement.setCat(ANTECEDENT);
		if (label.equals("CC"))
			coordElement.setCat(CONJUNCTION);
		LOGGER.debug("Adding new coordination element " + coordElement.getCoveredText() + ", category "
						+ coordElement.getCat());
		coordElement.setComponentId(COMPONENT_ID);
		if (showSegmentConf) {
			coordElement.setConfidence(confidence + "");
			LOGGER.debug("confidence set to: " + confidence);
		}
		coordElement.addToIndexes();
	}

	/**
	 * create an ArrayList of meta-info HashMaps, e.g., one such HashMap for each token which was
	 * given as input.
	 * 
	 * @param tokenList
	 *            the tokens for which we want meta infos
	 * @return
	 */
	private ArrayList<HashMap<String, String>> getMetaList(ArrayList<Token> tokenList) {
		ArrayList<HashMap<String, String>> metaList = new ArrayList<HashMap<String, String>>();
		// add meta info for each token to list
		for (Token token : tokenList) {
			Interval[] metaAnnotationValues = new Interval[activatedMetas.size()];
			for (int i = 0; i < metaAnnotationValues.length; i++) {
				metaAnnotationValues[i] = null;
			}
			HashMap metas = getMetas(token, metaAnnotationValues);
			metaList.add(metas);
		}
		return metaList;
	}

	/**
	 * Extracts the meta data for a token out of the annotationIterators.
	 * 
	 * @param tokenIter -
	 *            iterator over token annotations
	 * @param sentEnd -
	 *            end of the currently examined sentence
	 * @param annotationIterators -
	 *            an ArrayList of iterators over all meta datas
	 * @param activatedMetas -
	 *            an ArrayList containing the names of all activated meta datas
	 * @param valueMethods -
	 *            An ArrayList of method names. The corresponding methods serve to obtain the
	 *            annotation value of an annotation object.
	 * @param featureConfig -
	 *            a Properties object that represents the feature configuration of the used model
	 * @return metaInfos - a HashMap of pairs (<name of meta info> , <meta info>), e.g. (pos, NN)
	 *         whereas the meta info name is found within the feature configuration
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private HashMap<String, String> getMetas(Token token, Interval[] metaAnnotationValues) {
		int i = 0;
		HashMap<String, String> metaInfos = new HashMap<String, String>();
		// hack for easy JUnit testing without all the meta-infos
		if (featureConfig == null) {
			return metaInfos;
		}
		try {
			for (i = 0; i < annotationIterators.size(); i++) {
				boolean found = false;
				while (annotationIterators.get(i).hasNext() && found == false) {
					Annotation ann = (Annotation) annotationIterators.get(i).next();
					String valueMethodName = valueMethods.get(i);
					Method valueMethod = ann.getClass().getMethod(valueMethodName);
					metaAnnotationValues[i] = new Interval(ann.getBegin(), ann.getEnd(), ""
									+ valueMethod.invoke(ann, (Object[]) null));
					if (ann.getBegin() == token.getBegin() && ann.getEnd() == token.getEnd()) {
						found = true;
					}
				}
			}
			for (i = 0; i < activatedMetas.size(); i++) {
				Interval annotationInterval = metaAnnotationValues[i];
				String metaName = featureConfig.getProperty(activatedMetas.get(i) + "_feat_unit");
				if (annotationInterval != null && annotationInterval.isIn(token.getBegin(), token.getEnd())) {
					if (featureConfig.getProperty(activatedMetas.get(i) + "_begin_flag").equals("true")
									&& annotationInterval.getBegin() == token.getBegin()) {
						metaInfos.put(metaName, "B_" + metaAnnotationValues[i].getAnnotation());
					} else {
						metaInfos.put(metaName, metaAnnotationValues[i].getAnnotation());
					}
					if (annotationInterval.getEnd() == token.getEnd()) {
						metaAnnotationValues[i] = null; // this annotation has been
						// used, we
						// can get the next one
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("getMetas() - failed getting meta information for current token. No metas used!");
			metaInfos = new HashMap<String, String>();
		}
		return metaInfos;
	}
} // CoordinationElementAnnotator
