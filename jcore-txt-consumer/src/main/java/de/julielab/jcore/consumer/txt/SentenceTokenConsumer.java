/** 
 * TXTConsumer.java
 * 
 * Copyright (c) 2009, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: buyko
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: 13.05.2009 
 * 
 * //TODO insert short description
 **/

package de.julielab.jcore.consumer.txt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class SentenceTokenConsumer extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = Logger.getLogger(SentenceTokenConsumer.class);
	public static final String PARAM_OUTPUT_DIR = "outDirectory";
	@ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = true)
	private File directory;
	int docs = 0;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("INITIALIZING TXT Consumer ...");
		String dirName = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR);
		directory = new File(dirName);
		if (!directory.exists()) {
            directory.mkdir();
        }
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		LOGGER.info("Processing next document ... ");
		try {
			FSIterator sentenceIterator = jcas.getAnnotationIndex(
					Sentence.type).iterator();

			AnnotationIndex tokenIndex = jcas.getAnnotationIndex(
					Token.type);

			ArrayList<String> sentences = new ArrayList<String>();
			while (sentenceIterator.hasNext()) {
				Sentence sentence = (Sentence) sentenceIterator.next();
				FSIterator tokIterator = tokenIndex.subiterator(sentence);

				String sentenceText = "";
				while (tokIterator.hasNext()) {
					Token token = (Token) tokIterator.next();
					String tokenText = token.getCoveredText();
					if (sentenceText.equals(""))
						sentenceText = tokenText;
					else
						sentenceText = sentenceText + " " + tokenText;
				}

				sentences.add(sentenceText);

			}

			String fileId = getDocID(jcas);
			if (fileId == null)
				fileId = new Integer(docs++).toString();
			writeSentences2File(fileId, sentences);

		} catch (CASRuntimeException e) {
			e.printStackTrace();
		} catch (CASException e) {
			e.printStackTrace();
		}

	}

	public String getDocID(JCas jcas) throws CASException {
		String docID = "";
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> headerIter = indexes.getAnnotationIndex(Header.type)
				.iterator();
		while (headerIter.hasNext()) {
			Header h = (Header) headerIter.next();
			docID = h.getDocId();
		}
		return docID;
	}

	private void writeSentences2File(String fileId, ArrayList<String> sentences) {
		try {
			IOUtils.arraylist_to_file(sentences, new File(directory.getCanonicalPath() + File.separator + fileId
					+ ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
