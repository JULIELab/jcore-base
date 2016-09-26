/** 
 * ConllConsumer.java
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

package de.julielab.jcore.consumer.cas2conll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.DependencyRelation;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class ConllConsumer extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConllConsumer.class);
	
	public static final String PARAM_OUTPUT_DIR = "outputDir";
	public static final String PARAM_DEPENDENCY_PARSE = "dependencyParse";
	private static final Boolean DEFAULT_DEPENDENCY_PARSE = false;
	
	@ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = true)
	private String outputDir;
	@ConfigurationParameter(name = PARAM_DEPENDENCY_PARSE, mandatory = false)
	private Boolean dependencyParse; 
	
	int docs = 0;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("INITIALIZING CONLL Consumer ...");
		outputDir = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR);
		if (!outputDir.endsWith("/")) {
			outputDir += "/";
		}
		
		dependencyParse = (aContext.getConfigParameterValue(PARAM_DEPENDENCY_PARSE) != null) ?
			(Boolean) aContext.getConfigParameterValue(PARAM_DEPENDENCY_PARSE) : DEFAULT_DEPENDENCY_PARSE;
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		LOGGER.info("Processing next document ... ");
		try {
			FSIterator sentenceIterator = aJCas
					.getAnnotationIndex(Sentence.type).iterator();

			AnnotationIndex tokenIndex = aJCas
					.getAnnotationIndex(Token.type);

			ArrayList<String> sentences = new ArrayList<String>();
			while (sentenceIterator.hasNext()) {
				Sentence sentence = (Sentence) sentenceIterator.next();
				FSIterator tokIterator = tokenIndex.subiterator(sentence);
				ArrayList<Token> toks2convert = new ArrayList<Token>();

				while (tokIterator.hasNext()) {
					Token rel = (Token) tokIterator.next();
					toks2convert.add(rel);
				}

				String sentenceCONLL = "";
				for (int i = 0; i < toks2convert.size(); i++) {

					Token token = toks2convert.get(i);
					Token head = null;
					String depLabel = "ROOT";
					String pos = null;
					int headNumber = 0;

					if (token.getDepRel() == null) {
						if (dependencyParse) {
							LOGGER.error("Token without dependency relation occured "
									+ token.getCoveredText());
						}
						headNumber = 0;
						depLabel = "_";

					} else {
						DependencyRelation depRel = token.getDepRel(0);
						if (depRel != null) {
							head = depRel.getHead();
							headNumber = toks2convert.indexOf(head) + 1;
						}
						if (head != null)
							depLabel = depRel.getLabel();
					}
					String line = //see http://barcelona.research.yahoo.net/dokuwiki/doku.php?id=conll2008:format
						(i + 1) + //ID
						"\t" + token.getCoveredText() + //FORM
						"\t" + token.getCoveredText() + //LEMMA
						"\t" + token.getPosTag(0).getValue() + //GPOS 
						"\t" + token.getPosTag(0).getValue() + //PPOS
						"\t" + "_" + //SPLIT_FORM
						"\t" + "_" + //SPLIT_LEMMA
						"\t" + "_" + //PPOSS
						"\t" + headNumber + //HEAD
						"\t" + depLabel + //DEPREL
						"\t" + "_" + //PRED
						"\t" + "_" + "\n"; //ARG(s)

					sentenceCONLL = sentenceCONLL + line;

				}
				//sentenceCONLL = sentenceCONLL + "\n";
				sentences.add(sentenceCONLL);
			}
			String fileId = getDocID(aJCas);
			if (fileId == null || fileId.isEmpty()){
				fileId = new Integer(docs++).toString();
			}
			writeSentences2File(fileId, sentences);

		} catch (CASRuntimeException e) {
			e.printStackTrace();
		} catch (CASException e) {
			e.printStackTrace();
		}

	}

	public String getDocID(JCas aJCas) throws CASException {
		String docID = "";
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
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
			(new File(outputDir)).mkdirs(); 
			IOUtils.arraylist_to_file(sentences, new File(outputDir + fileId + ".CONLL"));
			LOGGER.info("wrote file " + outputDir + fileId + ".CONLL");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
