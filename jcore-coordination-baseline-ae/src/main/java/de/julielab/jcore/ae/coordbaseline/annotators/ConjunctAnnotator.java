/** 
 * ConjunctAnnotator.java
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
 * Analysis Engine that invokes the baseline prediction of conjuncts within a given 
 * EEE (elliptical entity expression). This annotator assumes that sentences, tokens, 
 * POS tags and EEEs have been annotated in the CAS. We iterate over sentences, then 
 * iterate over EEEs in the current sentece to accumulate a list of tokens (ArrayList 
 * of CoordinationTokens), then invoke the baseline conjunct prediction on this list. 
 **/

package de.julielab.jcore.ae.coordbaseline.annotators;

import de.julielab.jcore.ae.coordbaseline.main.Baseline;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import de.julielab.jcore.types.CoordinationElement;
import de.julielab.jcore.types.EEE;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ConjunctAnnotator extends JCasAnnotator_ImplBase 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ConjunctAnnotator.class);
	public static final String COMPONENT_ID = "jcore-coordination-baseline-ae";
	public static final String EEE_LABEL = "EEE";
	public static final String OUTSIDE_LABEL = "O";
	public static final String CONJUNCTION_LABEL = "CC";
	public static final String CONJUNCT_LABEL = "CONJ";
	public static final String ANTECEDENT_LABEL = "A";
		
/*--------------------------------------------------------------------------------------------*/	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException 
	{
		LOGGER.info("ConjunctAnnotator IS BEING INITIALIZED");
		super.initialize(aContext);
	} // of initialize
/*--------------------------------------------------------------------------------------------*/	
	/**
	 * This method is used to process the information found so far (e.g. by the Tokenizer) 
	 * and add new conjunct information to the CAS. For every sentence within the CAS, a 
	 * coordinationTokenList will be created. This list will be just another manifestation 
	 * of the sentence which is suitable for baseline prediction of conjuncts. Once the 
	 * coordiantionTokenList is processed, the new information will be added to the CAS 
	 * (e.g. by creating the accordant Objects like conjuncts and writing them to the CAS). 
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException 
	{
		LOGGER.info("ConjunctAnnotator IS BEING PROCESSED"); 
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
	 * This method is used to iterate over EEEs within the sentence, to predict the coordination elements (conjunctions, conjuncts, antecedents) and to put them into the JCas
	 * 
	 * @param jcas JCas which the coordination elements and the resolved ellipsis will be put into
	 * @param tokenIndex AnnotationIndex which is used to build the iterator over tokens
	 * @param EEEIndex AnnotationIndex which is used to build the iterator over EEEs
	 * @param baseline Baseline which undertakes the computation (prediction) of coordination elements for each EEE
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
						
			baseline.predictConjuncts(coordinationTokenList);
			writeConjunctsToCas(coordinationTokenList, tokenList, jcas);
		} // of while
	} // of iterateOverEEEs	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to write the found coordination elements (conjuncts, antecedents, conjunctions) to the JCas
	 * 
	 * @param coordinationTokenList ArrayList<CoordinationToken> is used to determine the coordinationLabel of coordinationTokens and match them against the tokens from the tokenList
	 * @param tokenList ArrayList<Token> is used to determine the start and end values of tokens and to match them against the coordinationTokens from the CoordinationTokenList
	 * @param jcas JCas which the coordination elements will be put into 
	 */
	private void writeConjunctsToCas(ArrayList<CoordinationToken> coordinationTokenList, ArrayList<Token> tokenList, JCas jcas) 
	{
		int begin = 0;
		int end = 0;
		String coordLabel = "";
				
		/*------------------------------------------*/
		/* This is the default value for the cat.	*/
		/*------------------------------------------*/
		String cat = "antecedent";
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			CoordinationToken coordToken = coordinationTokenList.get(i);
			coordLabel = coordToken.getCoordinationLabel();
			
			if (coordLabel.equals(ANTECEDENT_LABEL)) cat = "antecedent";
			if (coordLabel.equals(CONJUNCTION_LABEL)) cat = "conjunction";
			if (coordLabel.equals(CONJUNCT_LABEL)) cat = "conjunct";
			
			begin = tokenList.get(i).getBegin();
			end = tokenList.get(i).getEnd();
			
			CoordinationElement coordElement = new CoordinationElement(jcas);
			coordElement.setBegin(begin);
			coordElement.setEnd(end);
			coordElement.setCat(cat);		
			coordElement.addToIndexes();
		} // of for
	} // of writeConjunctsToCas
} // ConjunctAnnotator
