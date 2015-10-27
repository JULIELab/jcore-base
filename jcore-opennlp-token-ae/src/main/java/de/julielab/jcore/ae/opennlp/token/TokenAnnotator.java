/** 
 * OpennlpTokenizer.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: buyko
 * 
 * Current version: 2.0	
 * Since version:   1.0
 *
 * Creation date: 30.01.2008 
 * 
 * Analysis Engine that uses the OpenNLP Tokenizer.  This engine assumes
 * that sentences have been annotated in the CAS. 
 * It iterates over sentences and invoke the OpenNLP Tokenizer on each sentence. 
 **/

package de.julielab.jcore.ae.opennlp.token;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class TokenAnnotator extends JCasAnnotator_ImplBase {

	private static final String PARAM_NAME = "ModelFile";

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TokenAnnotator.class);

	/**
	 * component Id
	 */
	public static final String COMPONENT_ID = TokenAnnotator.class.getName();

	/**
	 * instance of the OpenNLP Tokenizer
	 */
	private TokenizerME tokenizer;

	private String modelFilePath;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		LOGGER.info("Initializing OpenNLP Token Annotator ...");
		super.initialize(aContext);
		modelFilePath = (String) aContext.getConfigParameterValue(PARAM_NAME);

		try {
			InputStream is;
			File modelFile = new File(modelFilePath);
			if (modelFile.exists()) {
				is = new FileInputStream(modelFile);
			} else {
				String modelResource = modelFilePath.startsWith("/") ? modelFilePath
						: "/" + modelFilePath;
				is = getClass().getResourceAsStream(modelResource);
			}
			if (null == is)
				throw new ResourceInitializationException(
						ResourceInitializationException.COULD_NOT_ACCESS_DATA,
						new Object[] { modelFilePath });
			tokenizer = new TokenizerME(new TokenizerModel(is));
		} catch (IOException e) {
			LOGGER.error("Could not load tokenizer model: " + e.getMessage());
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) {
		int tokenNumber = 1;

		LOGGER.trace("Processing document ...");
		AnnotationIndex<Annotation> sentenceIndex = aJCas
				.getJFSIndexRepository().getAnnotationIndex(Sentence.type);

		FSIterator<Annotation> sentenceIterator = sentenceIndex.iterator();
		if (!sentenceIterator.hasNext())
			LOGGER.debug("Current document has no annotations of type {}, skipping.", Sentence.class);
		while (sentenceIterator.hasNext()) {
			Sentence sentence = (Sentence) sentenceIterator.next();

			String text = sentence.getCoveredText();
			Span[] tokenSpans = tokenizer.tokenizePos(text);
			for (int i = 0; i < tokenSpans.length; i++) {
				Span span = tokenSpans[i];
				Token token = new Token(aJCas);
				token.setId("" + tokenNumber);
				token.setBegin(sentence.getBegin() + span.getStart());
				token.setEnd(sentence.getBegin() + span.getEnd());
				token.setComponentId(COMPONENT_ID);
				token.addToIndexes();
				tokenNumber++;

			}
		}

	}
}
