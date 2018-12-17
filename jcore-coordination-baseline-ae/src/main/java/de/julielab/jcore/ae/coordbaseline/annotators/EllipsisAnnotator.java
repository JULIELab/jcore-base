/** 
 * EllipsisAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: Buyko, Lichtenwald
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 17.10.2007 
 * 
 * Analysis Engine that invokes the baseline prediction of resolved ellipses within a 
 * given sentence. This annotator assumes that sentences, tokens, and EEEs have been 
 * annotated in the CAS. We iterate over sentences, then iterate EEEs in the current 
 * sentece to accumulate a list of tokens (ArrayList of CoordinationTokens), then 
 * invoke the baseline ellipses prediction on this list. 
 **/

package de.julielab.jcore.ae.coordbaseline.annotators;

import java.util.ArrayList;

import de.julielab.jcore.ae.coordbaseline.main.Baseline;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.EEE;
import de.julielab.jcore.types.Coordination;

import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EllipsisAnnotator extends JCasAnnotator_ImplBase 
{
	private final static Logger LOGGER = LoggerFactory.getLogger(EllipsisAnnotator.class);
	public final static String COMPONENT_ID = "jcore-coordination-baseline-ae"; 

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException 
	{
		LOGGER.info("EllipsisAnnotator IS BEING INITIALIZED");
		super.initialize(aContext);
	} // of initialize
	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to process the information found so far (e.g. by the Tokenizer) and 
	 * add new resolved ellipsis information to the CAS. For every sentence within the CAS, a 
	 * coordinationTokenList will be created. This list will be just another manifestation of 
	 * the sentence which is suitable for baseline prediction of resolved ellipses. Once the 
	 * coordiantionTokenList is processed, the new information will be added to the CAS (e.g.
	 * by creating the accordant Objects like resolved ellipses and writing them to the CAS). 
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException 
	{
		LOGGER.info("EllipsisAnnotator IS BEING PROCESSED");
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		FSIterator sentenceIterator = sentenceIndex.iterator();
		Baseline baseline = new Baseline();	
		
		/*--------------------------*/
		/* Iterate over sentences.	*/
		/*--------------------------*/
		while (sentenceIterator.hasNext())
		{
			Sentence sentence = (Sentence) sentenceIterator.next();	
			
			/*------------------------------------------------------------------*/
			/* Iterate over EEEs within the sentence, predict coordination 		*/
			/* elements for each EEE, create proper objects and put them into 	*/
			/* the JCas.														*/
			/*------------------------------------------------------------------*/
			iterateOverEEEs(jcas, baseline, sentence);	
		} // of while
	} // of process	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to iterate over EEEs within the sentence, to predict the coordination elements (conjunctions, conjuncts, antecedents) and the resolved ellipsis for each EEE and to put them into the JCas
	 * 
	 * @param jcas JCas which the coordination elements and the resolved ellipsis will be put into
	 * @param tokenIndex AnnotationIndex which is used to build the iterator over tokens
	 * @param EEEIndex AnnotationIndex which is used to build the iterator over EEEs
	 * @param baseline Baseline which undertakes the computation (prediction) of coordination elements and resolved ellipses for each EEE
	 * @param sentence Sentence which the EEE subiterator will iterate through
	 */
	private void iterateOverEEEs(JCas jcas, Baseline baseline, Sentence sentence) 
	{
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();
		AnnotationIndex tokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		AnnotationIndex EEEIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		FSIterator EEEIterator = EEEIndex.subiterator(sentence);
		
		/*----------------------*/
		/* Iterate over EEEs.	*/
		/*----------------------*/
		while (EEEIterator.hasNext())
		{
			tokenList.clear();
			coordinationTokenList.clear();
			EEE eee = (EEE) EEEIterator.next();
			FSIterator eeeTokenIterator = tokenIndex.subiterator(eee);
			/*----------------------*/
			/* Iterate over tokens.	*/
			/*----------------------*/
			while (eeeTokenIterator.hasNext())
			{
				Token eeeToken = (Token) eeeTokenIterator.next();
				tokenList.add(eeeToken);
				CoordinationToken coordinationToken = new CoordinationToken();
				coordinationToken.setWord(eeeToken.getCoveredText());
				coordinationToken.setPosTag(eeeToken.getPosTag(0).getValue());
				coordinationTokenList.add(coordinationToken);					
			} // of while
			
			String ellipsis = baseline.predictEllipsis(coordinationTokenList);	
			writeEllipsisToCas(ellipsis, jcas, eee);
		} // of while
	} // of iterateOverEEEs
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to write a found and resolved ellipsis into the JCas
	 * 
	 * @param ellipsis String which will contain the resolved ellipsis
	 * @param jcas JCas which the ellipsis will be put into
	 * @param eee EEE which "contains" the ellipsis and determines it's begin and end values
	 */
	private void writeEllipsisToCas(String ellipsis, JCas jcas, EEE eee) 
	{	
		Coordination coordination = new Coordination(jcas);
		coordination.setResolved(ellipsis);
		coordination.setElliptical(true);
		coordination.setBegin(eee.getBegin());
		coordination.setEnd(eee.getEnd());
		coordination.addToIndexes();
	} // of writeEllipsisToCas
/*--------------------------------------------------------------------------------------------*/
} // of EllipsisAnnotator
