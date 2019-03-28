/** 
 * EntityAnnotator.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.4
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is an UIMA wrapper for the JULIE NETagger. It produces named entity annotations, 
 * given sentence and token annotations.
 **/

package de.julielab.jcore.ae.jnet.uima;

import cc.mallet.fst.CRF;
import cc.mallet.types.Alphabet;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.index.JCoReCoverIndex;
import de.julielab.jnet.tagger.NETagger;
import de.julielab.jnet.tagger.Unit;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityAnnotator extends JCasAnnotator_ImplBase {

	private static final String COMPONENT_ID = "de.julielab.jules.ae.netagger.EntityAnnotator";

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityAnnotator.class);

	private final static String OUTSIDE_LABEL = "O"; // default outside label

	/*
	 * pattern for (non-local, non-introduced) abbreviation only those with 2 or
	 * three upper-case latters are considered here this is a first version,
	 * refinement will be needed ! KT, 04.06.2008
	 */
	protected final static String ABBREV_PATTERN = "[A-Z]{2,3}s?";

	public Pattern abbrevPattern = null;

	// which NE label (key) is to be mapped to which UIMA type (value)
	private HashMap<String, String> entityMap;

	private NETagger tagger;

	protected boolean expandAbbr = false;

	protected ConsistencyPreservation consistencyPreservation = null;
	protected float confidenceThresholdForConsistencyPreservation = -1;

	protected boolean showSegmentConf = false;

	protected TreeSet<String> entityMentionTypes = null; // the EntityMention
															// subtypes to be
	// filled

	protected NegativeList negativeList;

	Properties featureConfig = null;
	ArrayList<String> activatedMetas = null;
	ArrayList<FSIterator<org.apache.uima.jcas.tcas.Annotation>> annotationIterators = null;
	ArrayList<String> valueMethods = null;

	private String maxEnt_parameter = "maxEnt";
	private String iteration_parameter = "iterations";

	private boolean maxEnt = false;
	private int iterations_number = 0;

	/**
	 * Initialisiation of UIMA-JNET. Reads in and checks descriptor's
	 * parameters.
	 * 
	 * @throws ResourceInitializationException
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("initialize() - initializing JNET...");

		// invoke default initialization
		super.initialize(aContext);

		try {

			// compulsory param: ModelFilename
			setModel(aContext);

			// compulsory param: EntityTypes
			setEntityTypes(aContext);

			// non-compulsory param: show segment confidence
			setShowSegmentConfidence(aContext);

			// non-compulsory param: NegativeList
			setNegativeList(aContext);

			// compulsory param: ExpandAbbreviations
			Object tmp = aContext.getConfigParameterValue("ExpandAbbreviations");
			if (tmp != null) {
				expandAbbr = (Boolean) tmp;
			}

			// consistency preservation
			tmp = aContext.getConfigParameterValue("ConsistencyPreservation");
			if (tmp != null) {
				consistencyPreservation = new ConsistencyPreservation((String) tmp);
			}

			tmp = aContext.getConfigParameterValue("ConfidenceThresholdForConsistencyPreservation");
			if (tmp != null) {
				confidenceThresholdForConsistencyPreservation = (float) tmp;
			}

			// ignore non-local/not introduced abbreviations
			tmp = aContext.getConfigParameterValue("IgnoreNotIntroducedAbbreviations");
			if (tmp != null) {
				if ((Boolean) tmp == true) {
					abbrevPattern = Pattern.compile(ABBREV_PATTERN);
				}
			}

			tmp = aContext.getConfigParameterValue(maxEnt_parameter);
			if (tmp != null) {
				if ((Boolean) tmp == true) {
					maxEnt = true;
					tagger.set_Max_Ent(maxEnt);
				}
			}

			tmp = aContext.getConfigParameterValue(iteration_parameter);
			if (tmp != null) {
				if ((Integer) tmp != null) {
					iterations_number = ((Integer) tmp).intValue();
					tagger.set_Number_Iterations(iterations_number);
				}
			}

			// show configuration
			LOGGER.info("initialize() - abbreviation expansion: " + expandAbbr);
			LOGGER.info("initialize() - negative list: " + ((negativeList != null) ? true : false));
			LOGGER.info("initialize() - show confidence: " + showSegmentConf);
			LOGGER.info("initialize() - consistency preservation: "
					+ ((consistencyPreservation != null) ? consistencyPreservation.toString() : "none"));
			LOGGER.info("initialize() - ignore not introduces abbreviations: " + (abbrevPattern != null));

		} catch (AnnotatorContextException e) {
			// e.printStackTrace();
			throw new ResourceInitializationException(e);
		} catch (AnnotatorConfigurationException e) {
			// e.printStackTrace();
			throw new ResourceInitializationException(e);
		} catch (AnnotatorInitializationException e) {
			// e.printStackTrace();
			throw new ResourceInitializationException(e);
		}
	}

	/**
	 * get initialization of meta information which is used later to get token
	 * level meta-info
	 */
	private void retrieveMetaInformation(JCas aJCas) throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		featureConfig = tagger.getFeatureConfig();
		activatedMetas = new ArrayList<String>();
		annotationIterators = new ArrayList<FSIterator<org.apache.uima.jcas.tcas.Annotation>>();
		valueMethods = new ArrayList<String>();
		Enumeration<?> keys = featureConfig.keys();
		// reading which meta datas are enabled
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String meta = "";
			if (key.matches("[A-Za-z]+_feat_enabled") && featureConfig.getProperty(key).matches("true")) {
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
			Annotation ann = null;
			try {
				String typeClassName = featureConfig.getProperty(activatedMetas.get(i) + "_feat_data");
				// JCoRe compatibility hack
				typeClassName = typeClassName.replaceAll("jules", "jcore");
				ann = JCoReAnnotationTools.getAnnotationByClassName(aJCas, typeClassName);
				// System.out.println("[JNET] using: " +
				// ann.getClass().getName());
				annotationIterators.add(indexes.getAnnotationIndex(ann.getTypeIndexID()).iterator());
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}
	}

	/**
	 * sets the entity types to be used for different predicted labels
	 */
	private void setEntityTypes(UimaContext aContext)
			throws ResourceInitializationException, AnnotatorContextException, AnnotatorConfigurationException {

		entityMentionTypes = new TreeSet<String>();

		// get entity types from descriptorentityMap = new HashMap<String,
		// String>();
		String[] entityTypes;
		Object o = aContext.getConfigParameterValue("EntityTypes");
		if (o != null) {
			entityTypes = (String[]) o;
		} else {
			LOGGER.error("setEntityTypes() - descriptor incomplete, entity types not specified!");
			throw new AnnotatorConfigurationException();
		}

		// build hashmap from it
		entityMap = new HashMap<String, String>();
		for (int i = 0; i < entityTypes.length; i++) {
			String entityParts[] = entityTypes[i].split("=");
			// format <NE label, UIMA type>
			entityMap.put(entityParts[0], entityParts[1]);
			entityMentionTypes.add(entityParts[1]);
		}

		// test if all labels given through the descriptor exist in the tagger's
		// OutputAlphabet
		CRF model = (CRF) tagger.getModel();
		int j = 0;
		if (model != null) {
			Alphabet alpha = model.getOutputAlphabet();
			Object modelLabels[] = alpha.toArray();

			for (int i = 0; i < entityTypes.length; i++) {
				String[] entityParts = entityTypes[i].split("=");
				boolean entityFound = false;

				for (j = 0; j < modelLabels.length; j++) {
					if (entityParts[0].equals(modelLabels[j])) {
						entityFound = true;
					}
				}
				if (!entityFound) {// this does not happen if we find our label
					LOGGER.error(
							"setEntityTypes() - Could not find entity label \"{}\" from descriptor in the tagger's OutputAlphabet.",
							entityParts[0]);
					throw new AnnotatorConfigurationException();
				}
			}
		}
		LOGGER.debug("Entity mention types: " + entityMentionTypes.toString());
	}

	/**
	 * set and load the JNET model
	 */
	private void setModel(UimaContext aContext)
			throws AnnotatorConfigurationException, AnnotatorContextException, AnnotatorInitializationException {

		// get model filename
		String modelFilename = "";
		Object o = aContext.getConfigParameterValue("ModelFilename");
		if (o != null) {
			modelFilename = (String) o;
		} else {
			LOGGER.error("setModel() - descriptor incomplete, no model file specified!");
			throw new AnnotatorConfigurationException();
		}

		// produce an instance of JNET with this model
		tagger = new NETagger();
		try {
			File modelPath = new File(modelFilename);
			InputStream is;
			if (modelPath.exists()) {
				LOGGER.info("Loading model from file {}", modelPath);
				is = new FileInputStream(modelPath);
			} else {
				String cpResource = modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename;
				is = getClass().getResourceAsStream(cpResource);
				if (is != null)
					LOGGER.info("Loading model from classpath location {}", cpResource);
			}
			tagger.readModel(is);
		} catch (Exception e) {
			LOGGER.error("setModel() - Could not load JNET model: " + e.getMessage(), e);
			throw new AnnotatorInitializationException();
		}
	}

	/**
	 * set and initialize negative, if it should be used
	 */
	private void setNegativeList(UimaContext aContext)
			throws AnnotatorConfigurationException, AnnotatorContextException {
		Object o = aContext.getConfigParameterValue("NegativeList");
		if (o != null) {
			// if NegativeList is set, make the respective object
			File listFile = new File((String) o);
			try {
				InputStream is;
				if (listFile.exists()) {
					LOGGER.debug("setNegativeList() - using negative list from file: {}", listFile);
					is = new FileInputStream(listFile);
				} else {
					String cpResource = (String) o;
					if (!cpResource.startsWith("/"))
						cpResource = "/" + cpResource;
					is = getClass().getResourceAsStream(cpResource);
					if (null != is)
						LOGGER.info("Read negative list from classpath location {}", cpResource);
				}
				negativeList = new NegativeList(is);
			} catch (IOException e) {
				LOGGER.error("setNegativeList() - specified negative list file cannot be read: " + e.getMessage());
				throw new AnnotatorConfigurationException(e);
			}
		} else {
			LOGGER.info("No negative list file given.");
		}
	}

	/**
	 * set whether confidence should be estimated for entities
	 */
	private void setShowSegmentConfidence(UimaContext aContext) throws AnnotatorContextException {
		Object o = aContext.getConfigParameterValue("ShowSegmentConfidence");
		if (o != null) {
			showSegmentConf = (Boolean) o;
		}
		LOGGER.debug("setShowSegmentConfidence() - show segment confidence: " + showSegmentConf);
	}

	/**
	 * process current CAS. In case, abbreviation expansion is turned on, the
	 * abbreviation is replaced by its full form which is used during
	 * prediction. The labels of this full form are then applied to the
	 * original, short form.
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		LOGGER.debug("process() - processing next document");

		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		retrieveMetaInformation(aJCas);

		// get all sentences and tokens
		Iterator<org.apache.uima.jcas.tcas.Annotation> sentenceIter = indexes.getAnnotationIndex(Sentence.type)
				.iterator();

		JCoReCoverIndex<Token> tokenIndex = new JCoReCoverIndex<>(aJCas, Token.type);
		JCoReCoverIndex<Abbreviation> abbreviationIndex = new JCoReCoverIndex<>(aJCas, Abbreviation.type);

		// do entity recognition over single sentences
		while (sentenceIter.hasNext()) {
			Sentence sentence = (Sentence) sentenceIter.next();

			// get tokens, abbreviations, and metas for this sentence
			// @SuppressWarnings("unchecked")
			// ArrayList<Token> tokenList = (ArrayList<Token>)
			// UIMAUtils.getAnnotations(aJCas, sentence,
			// (new Token(aJCas, 0, 0)).getClass());
			List<Token> tokenList = tokenIndex.search(sentence).collect(Collectors.toList());
			ArrayList<HashMap<String, String>> metaList = getMetaList(tokenList);

			if (tokenList.size() != metaList.size()) {
				LOGGER.error("process() - token list, and meta list for this sentence not of same size!");
				throw new AnalysisEngineProcessException();
			}
			// make the Sentence object
			de.julielab.jnet.tagger.Sentence unitSentence = createUnitSentence(tokenList, aJCas, metaList,
					abbreviationIndex, tokenIndex);

			LOGGER.debug("process() - original sentence: " + sentence.getCoveredText());
			StringBuffer unitS = new StringBuffer();
			for (Unit unit : unitSentence.getUnits()) {
				unitS.append(unit.getRep() + " ");
			}
			LOGGER.debug("process() - sentence for prediction: " + unitSentence.toString());

			// predict with JNET
			try {
				tagger.predict(unitSentence, showSegmentConf);
			} catch (IllegalStateException e) {
				LOGGER.error("process() - predicting with JNET failed: " + e.getMessage());
				throw new AnalysisEngineProcessException();
			}

			// remove duplicated tokens which might occure when abbrev expansion
			// enabled
			if (expandAbbr) {
				unitSentence = removeDuplicatedTokens(unitSentence);
			}
			LOGGER.debug("process() - sentence with labels: " + unitSentence.toString());

			// write predicted labels to CAS
			writeToCAS(unitSentence, aJCas, abbreviationIndex);

		}

		// now do consistency preservation over whole document
		if (consistencyPreservation != null) {
			LOGGER.debug("process() - running consistency preservation");
			consistencyPreservation.stringMatch(aJCas, entityMentionTypes,
					confidenceThresholdForConsistencyPreservation);
			consistencyPreservation.acroMatch(aJCas, entityMentionTypes);
		}
	}

	/**
	 * removes duplicate tokens in a unit sentence (i.e., tokens having the same
	 * offset position). This is necessary if abbreviations in sentence were
	 * expanded for prediction. Then, afterwards, this method needs to be called
	 * before writing the prediction into the CAS. When tokens within
	 * abbreviation long form differ in their prediction, the outside label is
	 * assumed for the abbreviation!
	 */
	protected de.julielab.jnet.tagger.Sentence removeDuplicatedTokens(de.julielab.jnet.tagger.Sentence unitSentence) {
		de.julielab.jnet.tagger.Sentence newUnitSentence = new de.julielab.jnet.tagger.Sentence();
		String lastPos = null;
		Unit lastUnit = null;
		TreeSet<String> lastLabels = new TreeSet<String>();
		for (int k = 0; k < unitSentence.getUnits().size(); k++) {
			Unit unit = unitSentence.get(k);
			lastLabels.add(unit.getLabel());
			String currPos = unit.begin + "@" + unit.end;
			if (lastPos != null && (lastPos.equals(currPos))) {
				lastLabels.add(unit.getLabel());
				if (lastLabels.size() > 1) {
					lastUnit.setLabel(OUTSIDE_LABEL);
					// JNET accordingly!
				}
			} else {
				// write unit and probably change label of previous unit
				newUnitSentence.add(unit);
				lastLabels = new TreeSet<String>();
				lastLabels.add(unit.getLabel());
			}
			lastPos = currPos;
			lastUnit = unit;
		}
		return newUnitSentence;
	}

	/**
	 * Takes all info about meta data and generates the corresponding unit
	 * sequence represented by a Sentence object. Abbreviation is expanded when
	 * specified in descriptor. Only abbreviations which span over single tokens
	 * can be interpreted here. Other case (which is very rare and thus probably
	 * not relevant) is ignored!
	 * 
	 * @param tokenList
	 *            a list of Token objects of the current sentence
	 * @param JCas
	 *            the CAS we are working on
	 * @param metaList
	 *            a Arraylist of meta-info HashMaps which specify the meta
	 *            information of the respective token
	 * @param abbreviationIndex
	 * @param tokenIndex 
	 * @return an array of two sequences of units containing all available meta
	 *         data for the corresponding tokens. In the first sequence,
	 *         abbreviations are expanded to their fullform. In the second
	 *         sequence, the tokens are of their original form.
	 */
	protected de.julielab.jnet.tagger.Sentence createUnitSentence(List<Token> tokenList, JCas JCas,
			ArrayList<HashMap<String, String>> metaList, JCoReCoverIndex<Abbreviation> abbreviationIndex, JCoReCoverIndex<Token> tokenIndex) {

		de.julielab.jnet.tagger.Sentence unitSentence = new de.julielab.jnet.tagger.Sentence();
		ArrayList<Abbreviation> abbreviationList = getAbbreviationList(tokenList, JCas, abbreviationIndex);

		for (int i = 0; i < tokenList.size(); i++) {
			Token token = tokenList.get(i);
			HashMap<String, String> metas = metaList.get(i);
			Abbreviation abbreviation = abbreviationList.get(i);
			String tokenRepresentation = token.getCoveredText();

			// for abbreviation expansion, if there is an abbreviation on the
			// current token
			if (expandAbbr == true && abbreviation != null) {
				if (abbreviation.getDefinedHere()) {
					// when abbreviation is defined here: ignore this token
					tokenRepresentation = null;
				} else {
					// abbreviation only used here: replace token representation
					// by full form
					tokenRepresentation = abbreviation.getTextReference().getCoveredText();
				}
			}

			// now make JNET Unit object for this token and add to Sentence
			if (tokenRepresentation != null) {
				if (tokenRepresentation.equals(token.getCoveredText())) {
					// no abbrevs were expanded here
					Unit unit = new de.julielab.jnet.tagger.Unit(token.getBegin(), token.getEnd(), tokenRepresentation,
							"", metas);
					unitSentence.add(unit);
				} else {
					// abbrev was expanded, so we probably need to make more
					// than one units
//					@SuppressWarnings("unchecked")
//					ArrayList<Token> abbrevTokens = (ArrayList<Token>) UIMAUtils.getAnnotations(JCas,
//							abbreviation.getTextReference(), (new Token(JCas, 0, 0)).getClass());
					List<Token> abbrevTokens = tokenIndex.search(abbreviation.getTextReference()).collect(Collectors.toList());
					if (abbreviation.getTextReference().getCoveredText().length() > 0 && abbrevTokens.size() == 0) {
						// white space tokenization when no tokens found on
						// abbreviation full form,
						// which
						// typically is because full form didn't start/end on
						// token boundaries
						StringTokenizer st = new StringTokenizer(tokenRepresentation);
						while (st.hasMoreTokens()) {
							String fullformToken = st.nextToken();
							Unit unit = new de.julielab.jnet.tagger.Unit(token.getBegin(), token.getEnd(),
									fullformToken, "", metas);
							unitSentence.add(unit);
						}
					} else {
						// tokens within full form
						for (Token abbrevToken : abbrevTokens) {
							Unit unit = new de.julielab.jnet.tagger.Unit(token.getBegin(), token.getEnd(),
									abbrevToken.getCoveredText(), "", metas);
							unitSentence.add(unit);
						}
					}
				}
			}
		}

		// clean-up: remove tokens with consecutive brackets
		if (expandAbbr) {
			unitSentence = removeConsecutiveBrackets(unitSentence);
		}
		return unitSentence;
	}

	/**
	 * remove consecutive (opening and closing) brackets from unit sentence.
	 * This is necessary after an abbreviation that is introduced is removed.
	 * 
	 * @param unitSentence
	 *            the original unit sentence to be modified
	 * @return
	 */
	private de.julielab.jnet.tagger.Sentence removeConsecutiveBrackets(de.julielab.jnet.tagger.Sentence unitSentence) {
		de.julielab.jnet.tagger.Sentence finalUnitSentence = new de.julielab.jnet.tagger.Sentence();
		for (int i = 0; i < unitSentence.getUnits().size(); i++) {
			Unit currentUnit = unitSentence.getUnits().get(i);
			if ((i + 1) < unitSentence.getUnits().size()) {
				Unit nextUnit = unitSentence.getUnits().get(i + 1);
				if ((currentUnit.getRep().equals("(") && nextUnit.getRep().equals(")"))
						|| (currentUnit.getRep().equals("[")) && nextUnit.getRep().equals("]")) {
					// if this unit is a bracket and next unit too -> ignore
					// this and next unit
					i = i + 1;
					continue;
				}
			}
			finalUnitSentence.add(currentUnit);
		}
		return finalUnitSentence;
	}

	/**
	 * build an arraylist of abbreviation objects, one for each token. If there
	 * is no abbreviation on current token, null is added to the list.
	 * 
	 * @param tokenList
	 * @param JCas
	 * @param abbreviationIndex
	 * @return
	 */
	private ArrayList<Abbreviation> getAbbreviationList(List<Token> tokenList, JCas JCas,
			JCoReCoverIndex<Abbreviation> abbreviationIndex) {
		ArrayList<Abbreviation> abbreviationList = new ArrayList<Abbreviation>();

		for (Token token : tokenList) {
			// @SuppressWarnings("unchecked")
			// ArrayList<Abbreviation> abbreviations = (ArrayList<Abbreviation>)
			// UIMAUtils.getAnnotations(JCas, token,
			// (new Abbreviation(JCas, 0, 0)).getClass());
			List<Abbreviation> abbreviations = abbreviationIndex.search(token).collect(Collectors.toList());
			if (abbreviations != null && abbreviations.size() > 0) {
				abbreviationList.add(abbreviations.get(0));
			} else {
				abbreviationList.add(null);
			}
		}
		return abbreviationList;
	}

	/**
	 * create an ArrayList of meta-info HashMaps, e.g., one such HashMap for
	 * each token which was given as input.
	 * 
	 * @param tokenList
	 *            the tokens for which we want meta infos
	 * @return
	 */
	private ArrayList<HashMap<String, String>> getMetaList(List<Token> tokenList) {
		ArrayList<HashMap<String, String>> metaList = new ArrayList<HashMap<String, String>>();
		Interval[] metaAnnotationValues = new Interval[activatedMetas.size()];
		for (int i = 0; i < metaAnnotationValues.length; i++) {
			metaAnnotationValues[i] = null;
		}
		// add meta info for each token to list
		for (Token token : tokenList) {
			metaList.add(getMetas(token, metaAnnotationValues));
		}
		return metaList;
	}

	/**
	 * Extracts the meta data for a token out of the annotationIterators.
	 * 
	 * @param tokenIter
	 *            - iterator over token annotations
	 * @param sentEnd
	 *            - end of the currently examined sentence
	 * @param annotationIterators
	 *            - an ArrayList of iterators over all meta datas
	 * @param activatedMetas
	 *            - an ArrayList containing the names of all activated meta
	 *            datas
	 * @param valueMethods
	 *            - An ArrayList of method names. The corresponding methods
	 *            serve to obtain the annotation value of an annotation object.
	 * @param featureConfig
	 *            - a Properties object that represents the feature
	 *            configuration of the used model
	 * @return metaInfos - a HashMap of pairs (<name of meta info> , <meta
	 *         info>), e.g. (pos, NN) whereas the meta info name is found within
	 *         the feature configuration
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
			// get next meta data values (e.g. next available pos-tag)
			for (i = 0; i < annotationIterators.size(); i++) {
				if (annotationIterators.get(i).hasNext() && metaAnnotationValues[i] == null) {
					Annotation ann = (Annotation) annotationIterators.get(i).next();
					String valueMethodName = valueMethods.get(i);
					Method valueMethod = ann.getClass().getMethod(valueMethodName);
					metaAnnotationValues[i] = new Interval(ann.getBegin(), ann.getEnd(),
							"" + valueMethod.invoke(ann, (Object[]) null));
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
						metaAnnotationValues[i] = null; // this annotation has
														// been
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

	/**
	 * creates the respective uima annotations from JNET's predictions.
	 * Therefore, we loop over JNET's Sentence objects which contain
	 * predictions/labels for each Unit (i.e., for each token).
	 * 
	 * @param unitSentence
	 *            the current Sentence object
	 * @param aJCas
	 *            the cas to write the annotation to
	 * @param abbreviationIndex 
	 */
	public void writeToCAS(de.julielab.jnet.tagger.Sentence unitSentence, JCas aJCas, JCoReCoverIndex<Abbreviation> abbreviationIndex) {
		String lastLabel = OUTSIDE_LABEL;
		int lastStart = 0;
		int lastEnd = 0;
		double conf = -1;
		double lastConf = -1;

		de.julielab.jnet.tagger.Unit unit = null;
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
				addAnnotation(aJCas, lastStart, lastEnd, lastLabel, lastConf, abbreviationIndex);
				lastStart = unit.begin;
			}

			lastLabel = label;
			lastEnd = unit.end;
			lastConf = conf;

			if (i == unitSentence.size() - 1) {
				// last unit handled separately, add annotation to CAS
				if (!label.equals(OUTSIDE_LABEL)) {
					lastEnd = unit.end;
					addAnnotation(aJCas, lastStart, lastEnd, lastLabel, lastConf, abbreviationIndex);

				}
			}
		}
	}

	/**
	 * Create annotation CAS. The label predicted by JNET is written to
	 * specificType.
	 * 
	 * @param aJCas
	 *            the cas to write the annotation to
	 * @param start
	 *            begin offset of annotation
	 * @param end
	 *            end offset of annotation
	 * @param label
	 *            label for this entity mention
	 * @param confidence
	 *            certainty of JNET on this label
	 * @param abbreviationIndex 
	 */
	private void addAnnotation(JCas aJCas, int start, int end, String label, double confidence, JCoReCoverIndex<Abbreviation> abbreviationIndex) {

		String coveredText = aJCas.getDocumentText().substring(start, end);

		// check whether label was predicted on a not introduced abbreviation
		// and if so, do not add
		// this annotation
		if (ignoreLabel(aJCas, start, end, abbreviationIndex)) {
			return;
		}

		// check against negative list whether this annotation should be really
		// added
		if (negativeList != null && negativeList.contains(coveredText, label)) {
			LOGGER.debug("addAnnotation() - ignoring current entity mention as contained in negativeList");
			return; // ignore this entity mention
		}

		// create EntityMention object
		EntityMention entity = null;
		String entityType;

		if ((entityType = entityMap.get(label)) != null) {
			// if the predicted label is in the entityTypes map -> make a new
			// EntityMention
			try {
				entity = (EntityMention) JCoReAnnotationTools.getAnnotationByClassName(aJCas, entityType);
				// add feature values
				entity.setBegin(start);
				entity.setEnd(end);
				entity.setTextualRepresentation(aJCas.getDocumentText().substring(start, end));
				entity.setSpecificType(label);
				entity.setComponentId(COMPONENT_ID);
				if (showSegmentConf)
					entity.setConfidence(confidence + "");
				entity.addToIndexes();
			} catch (Exception e) {
				LOGGER.error("addAnnotation() - could not generate new EntityMention", e);
			}
		} else {
			LOGGER.debug("addAnnotation() - ommitted entity mention for label: " + label);
		}

	}

	/**
	 * tests whether annotation should be ignored as this label is on a not
	 * introduced abbreviation
	 * 
	 * @param aJCas
	 * @param start
	 * @param end
	 * @param abbreviationIndex 
	 * @param coveredText
	 * @return
	 */
	protected boolean ignoreLabel(JCas aJCas, int start, int end, JCoReCoverIndex<Abbreviation> abbreviationIndex) {
		String coveredText = aJCas.getDocumentText().substring(start, end);
		if (abbrevPattern != null && abbrevPattern.matcher(coveredText).matches()) {
			// check whether this abbreviation was introduced (if so, it has a
			// TextReference)
//			Annotation windowAnno = new Annotation(aJCas, start, end);
//			windowAnno.addToIndexes();
//			@SuppressWarnings("unchecked")
//			ArrayList<Abbreviation> abbreviations = (ArrayList<Abbreviation>) UIMAUtils.getAnnotations(aJCas,
//					windowAnno, (new Abbreviation(aJCas, 0, 0)).getClass());
			List<Abbreviation> abbreviations = abbreviationIndex.search(start, end).collect(Collectors.toList());
//			windowAnno.removeFromIndexes();
//			windowAnno = null;
			if (abbreviations != null && abbreviations.size() > 0) {
				LOGGER.debug("ignoreLabel() - found JACRO-recognized abbreviations under this string: " + coveredText);
				for (Abbreviation abbreviation : abbreviations) {
					if (abbreviation.getTextReference() != null
							&& abbreviation.getCoveredText().matches(ABBREV_PATTERN)) {
						LOGGER.debug("ignoreLabel() - abbreviation: " + abbreviation.getCoveredText()
								+ " introduced for: " + abbreviation.getTextReference().getCoveredText());
						return false; // should not be ignored
					}
				}
			}
			LOGGER.debug("ignoreLabel() - ignoring annotations " + "on " + coveredText
					+ " because it is a not introduced abbreviation!");
			return true; // should be ignored
		}
		return false; // should not be ignored
	}
}
