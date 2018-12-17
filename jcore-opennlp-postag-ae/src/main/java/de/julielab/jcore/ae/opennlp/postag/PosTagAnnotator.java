/** 
 * OpennlpPosTagger.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: buyko, faessler
 * 
 * Current version: 3.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008
 * 
 * Analysis Engine that invokes the OpenNLP POS Tagger. This annotator assumes that
 * sentences and tokens have been annotated in the CAS. We iterate over sentences, 
 * then iterate over tokens in the current sentece to accumulate a list of tokens, then invoke the
 * OpenNLP POS Tagger on the list of tokens. 
 **/

package de.julielab.jcore.ae.opennlp.postag;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class PosTagAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_MODEL_FILE = "modelFile";

	public static final String PARAM_TAGSET = "tagset";

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PosTagAnnotator.class);

	/**
	 * instance of the Opennlp POS Tagger
	 */
	private POSTaggerME tagger;

	private Constructor<?> typeConstructor;
	/**
	 * the used postagset
	 */
	@ConfigurationParameter(
			name = PARAM_TAGSET,
			description = "The UIMA POSTag subtype to be used for the POS annotations.",
			mandatory = true)
	private String postagset;

	@ConfigurationParameter(
			name = PARAM_MODEL_FILE,
			description = "The OpenNLP POS model file path. It is expected that a tag dictionary has been included into the model at training time.",
			mandatory = true)
	private String modelFilePath;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		super.initialize(aContext);

		try {

			LOGGER.info("[OpenNLP POSTag Annotator]initializing OpenNLP POSTag Annotator ...");
			// Get configuration parameter values
			modelFilePath = (String) aContext.getConfigParameterValue(PARAM_MODEL_FILE);
			postagset = (String) aContext.getConfigParameterValue(PARAM_TAGSET);

			InputStream modelIs;
			File modelFile = new File(modelFilePath);
			if (modelFile.exists()) {
				modelIs = new FileInputStream(modelFile);
			} else {
				LOGGER.debug("File \"{}\" does not exist. Searching for the model as a classpath resource.",
						modelFilePath);
				modelIs =
						getClass().getResourceAsStream(
								modelFilePath.startsWith("/") ? modelFilePath : "/" + modelFilePath);
				if (null == modelIs)
					throw new IllegalArgumentException("The model file \"" + modelFilePath
							+ "\" could be found neither in the file system nor in the classpath.");
			}
			// Get OpenNLP POS Tagger, initialize with a model
			POSModel posModel = new POSModel(modelIs);
			tagger = new POSTaggerME(posModel);
		} catch (Exception e) {
			// LOGGER.error("[OpenNLP POStag Annotator] Could not load Part-of-speech model: " + e.getMessage());
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) {

		LOGGER.debug("[OpenNLP POSTag Annotator]  processing document ...");
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<String> tokenTextList = new ArrayList<String>();

		FSIterator<Annotation> sentenceIterator = aJCas.getAnnotationIndex(Sentence.type).iterator();
		AnnotationIndex<Annotation> tokenIndex = aJCas.getAnnotationIndex(Token.type);

		if (!sentenceIterator.hasNext())
			LOGGER.debug("CAS has no sentences, no POS tags will be added");
		
		// iterate over Sentences
		while (sentenceIterator.hasNext()) {
			tokenList.clear();
			tokenTextList.clear();
			Sentence sentence = (Sentence) sentenceIterator.next();

			// iterate over Tokens
			FSIterator<Annotation> tokenIterator = tokenIndex.subiterator(sentence);
			if (!tokenIterator.hasNext())
				LOGGER.debug("Sentence has no tokens, no POS tags will be added for this sentence");
			while (tokenIterator.hasNext()) {
				Token token = (Token) tokenIterator.next();
				tokenList.add(token);
				tokenTextList.add(token.getCoveredText());
			}

			String[] tokenTagList = tagger.tag(tokenTextList.toArray(new String[tokenList.size()]));

			try {
				for (int i = 0; i < tokenList.size(); i++) {
					Token token = (Token) tokenList.get(i);
					String posTag = (String) tokenTagList[i];
					POSTag pos = null;

					try {
						if (null == typeConstructor) {
							Class<?>[] parameterTypes = new Class[] { JCas.class };
							Class<?> typeClass = Class.forName(postagset);
							typeConstructor = typeClass.getConstructor(parameterTypes);
						}
						pos = (POSTag) typeConstructor.newInstance(aJCas);
						pos.setBegin(token.getBegin());
						pos.setEnd(token.getEnd());
						pos.setValue(posTag);
						pos.setComponentId(PosTagAnnotator.class.getName());
						pos.addToIndexes();

					} catch (SecurityException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (IllegalArgumentException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (ClassNotFoundException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (NoSuchMethodException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (InstantiationException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (IllegalAccessException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (InvocationTargetException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					}

					FSArray postags = token.getPosTag();
					if (postags == null) {
						postags = new FSArray(aJCas, 1);
						try {
							postags.set(0, pos);
						} catch (CASRuntimeException e) {
							LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
						}
						token.setPosTag(postags);
					} else {
						int numPosTags = postags.size();
						int lastElementIndex = numPosTags - 1;
						if (postags.get(lastElementIndex) != null) {
							FSArray extendedPosTags = new FSArray(aJCas, numPosTags + 1);
							extendedPosTags.copyFromArray(postags.toArray(), 0, 0, numPosTags);
							extendedPosTags.set(numPosTags, pos);
							postags = extendedPosTags;
						} else {
							while ((lastElementIndex > 0) && (postags.get(lastElementIndex - 1) == null)) {
								lastElementIndex--;
							}
							postags.set(lastElementIndex, pos);
						}
					}

				}
			} catch (CASRuntimeException e) {
				LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
				LOGGER.error("[OpenNLP POSTag Annotator]  list of tags shorter than list of words");
			}
		}
	}

}