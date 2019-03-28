/** 
 * EEEAnnotator.java
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
 * Analysis Engine that invokes the baseline prediction of EEEs (elliptical entity 
 * expressiona) within a given sentence. This annotator assumes that sentences, tokens, 
 * and entities have been annotated in the CAS. We iterate over sentences, then 
 * iterate over tokens in the current sentece to accumulate a list of tokens (ArrayList 
 * of CoordinationTokens), then invoke the baseline EEE prediction on this list. 
 **/

package de.julielab.jcore.ae.coordbaseline.annotators;

import de.julielab.jcore.ae.coordbaseline.main.Baseline;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import de.julielab.jcore.types.EEE;
import de.julielab.jcore.types.Entity;
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
import java.util.HashMap;

public class EEEAnnotator extends JCasAnnotator_ImplBase 
{
	private final static Logger LOGGER = LoggerFactory.getLogger(EEEAnnotator.class);
	public final static String COMPONENT_ID = "jcore-coordination-baseline-ae"; 
	public static final String EEE_LABEL = "EEE";
	public static final String OUTSIDE_LABEL = "O";
	public static final String CONJUNCTION_LABEL = "CC";
	public static final String CONJUNCT_LABEL = "CONJ";
	public static final String ANTECEDENT_LABEL = "A";

	
/*--------------------------------------------------------------------------------------------*/	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException 
	{
		LOGGER.info("EEEAnnotator IS BEING INITIALIZED");
		super.initialize(aContext);
	} // of initialize
	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to process the information found so far (e.g. by the Tokenizer) 
	 * and add new EEE information to the CAS. For every sentence within the CAS, a 
	 * coordinationTokenList will be created. This list will be just another manifestation 
	 * of the sentence which is suitable for baseline prediction of EEEs. Once the 
	 * coordiantionTokenList is processed, the new information will be added to the CAS 
	 * (e.g. by creating the accordant Objects like EEEs and writing them to the CAS). 
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException
	{
		LOGGER.info("EEEAnnotator IS BEING PROCESSED");
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();
		HashMap<Token,CoordinationToken> tokenMap = new HashMap<Token,CoordinationToken>(); 
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		AnnotationIndex tokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		AnnotationIndex entityIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Entity.type);
		FSIterator sentenceIterator = sentenceIndex.iterator();
		Baseline baseline = new Baseline();	
		
		/*--------------------------*/
		/* Iterate over sentences.	*/
		/*--------------------------*/
		while (sentenceIterator.hasNext())
		{
			tokenList.clear();
			Sentence sentence = (Sentence) sentenceIterator.next();	
			FSIterator tokenIterator = tokenIndex.subiterator(sentence);
			FSIterator entityIterator = entityIndex.subiterator(sentence);
			
			/*----------------------------------------------------------------------*/		
			/* Iterate over tokens within the sentence, create coordinationTokens	*/
			/* for each token and match them via the hash map.						*/
			/*----------------------------------------------------------------------*/
			iterateOverTokens(tokenList, tokenMap, tokenIterator);
			
			/*----------------------------------------------------------------------*/
			/* Iterate over entities within the sentence, create entityLabels for 	*/
			/* each token/coordinationToken.										*/
			/*----------------------------------------------------------------------*/
			iterateOverEntities(tokenMap, tokenIndex, entityIterator);
			
			/*------------------------------------------------------------------*/
			/* Iterate over tokens again and build the coordinationTokenList	*/
			/* using the previously constructed hash map.						*/
			/*------------------------------------------------------------------*/
			iterateOverTokens(coordinationTokenList, tokenMap, tokenIndex, sentence);
			
			/*------------------------------------------------------------------*/
			/* Find any EEE within the sentence, create the accordant objects	*/
			/* and add them to the JCas.										*/ 
			/*------------------------------------------------------------------*/		
			baseline.predictEEE(coordinationTokenList);
			writeEEEToCas(coordinationTokenList, tokenList, jcas);		
		} // of while
	} // of process
/*--------------------------------------------------------------------------------------------*/	
	/**
	 * This method is used to iterate over the tokens within the sentence, create coordinationTokens
	 * and put them (tokens and coordinationTokens) into proper list and hash map for further processing
	 * 
	 * @param tokenList ArrayList<Token> whichwill contain the tokens of the sentence
	 * @param tokenMap HashMap<Token, CoordinationToken> which will make it possible to match toe tokens against the proper coordinationTokens
	 * @param tokenIterator FSIterator is a subiterator which will iterate over tokens within the sentence
	 */
	private void iterateOverTokens(ArrayList<Token> tokenList, HashMap<Token, CoordinationToken> tokenMap, FSIterator tokenIterator) 
	{
		while (tokenIterator.hasNext())
		{			
			Token token = (Token) tokenIterator.next();
			tokenList.add(token);
			
			CoordinationToken coordinationToken = new CoordinationToken();
			coordinationToken.setWord(token.getCoveredText());
			coordinationToken.setEntityLabel(OUTSIDE_LABEL); 
			tokenMap.put(token, coordinationToken);
		} // of while
	} // of iterateOverTokens
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method isused to iterate over the entities within the sentence, create the proper entityLabel and set it to the coordinationTokens
	 * 
	 * @param tokenMap HashMap<Token, CoordinationToken> is used to match the tokens within entities against the coordinationTokens in order to set their entityLabel
	 * @param tokenIndex AnnotationIndex which is used to build the subiterator over tokens within the entities
	 * @param entityIterator FSIterator which iterate over entities within the sentence 
	 */
	private void iterateOverEntities(HashMap<Token, CoordinationToken> tokenMap, AnnotationIndex tokenIndex, FSIterator entityIterator) 
	{
		while (entityIterator.hasNext())
		{
			Entity entity = (Entity) entityIterator.next();
			String entityLabel = entity.getSpecificType(); 
			FSIterator entityTokenIterator = tokenIndex.subiterator(entity);
			
			/*----------------------*/
			/* Iterate over tokens.	*/
			/*----------------------*/
			while (entityTokenIterator.hasNext())
			{
				Token entityToken = (Token) entityTokenIterator.next();
				CoordinationToken coordinationToken = tokenMap.get(entityToken);
				coordinationToken.setEntityLabel(entityLabel);
			} // of while
		} // of while
	} // of iterateOverEntities	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to iterate over tokens within the sentence and build the coordinationTokenList
	 * 
	 * @param coordinationTokenList ArrayList<CoordinationToken> will be created and made accessible for the caller of this method
	 * @param tokenMap HashMap<Token, CoordinationToken> is used to match the tokens against the proper coordinationTokens
	 * @param tokenIndex AnnotationIndex which will be used to create the subiterator over tokens
	 * @param sentence Sentence which the subiterator will iterate through
	 */
	private void iterateOverTokens(ArrayList<CoordinationToken> coordinationTokenList, HashMap<Token, CoordinationToken> tokenMap, AnnotationIndex tokenIndex, Sentence sentence) 
	{
		FSIterator tokenIterator = tokenIndex.subiterator(sentence);
		
		while (tokenIterator.hasNext())
		{
			Token token = (Token) tokenIterator.next();
			CoordinationToken coordinationToken = tokenMap.get(token);
			coordinationTokenList.add(coordinationToken);
		} // of while
	} // of iterateOverTokens	
/*--------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to write the EEE to the CAS. For this purpose the coordinationTokenList and the tokenList are 
	 * used. CoordinationTokenList contains information about EEEs (EEELabel marks every token either to be within the 
	 * EEE or outside). TokenList is used to access the begin and end information. The approach is to iterate trough the
	 * coordinationTokenList and to check every coordinationToken if it is inside an EEE (in other words, to check it's 
	 * EEELabel). Once the first coordinationToken which is inside an EEE is found on a certain position in the 
	 * coordinationTokenList, the token on the same position in the tokenList will be accessed and it's begin value will 
	 * be kept in the beginEEE variable. Thus we found the begin of the EEE. Now the end of the EEE has to be established.
	 * For this purpose the variable endEEE will be updated while the coordinationTokens are inside the EEE. Thus it 
	 * marks the end information of the currently last checked token within the EEE.    
	 * 
	 * @param coordinationTokenList ArrayList<CoordinationToken> is used to find the EEE using the EEELabel of the coordinationTokens
	 * @param tokenList ArrayList<Token> is used to get the begin and end values of tokens
	 * @param jcas JCas which the EEE will be put into 
	 */
	public void writeEEEToCas(ArrayList<CoordinationToken> coordinationTokenList, ArrayList<Token> tokenList, JCas jcas)
	{	
		boolean insideEEE = false;
		String EEELabel = "";
		Token token = null;
		int beginEEE = 0;
		int endEEE = 0;
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			CoordinationToken coordToken = coordinationTokenList.get(i);			
			EEELabel = coordToken.getEEELabel();
			token = tokenList.get(i);
		
			/*--------------------------------------------------------------------------*/
			/* If there the first token inside the EEE was found, the begin of this EEE	*/
			/* will be marked by the beginEEE variable.									*/
			/*--------------------------------------------------------------------------*/
			if (EEELabel.equals(EEE_LABEL) && !insideEEE)
			{
				insideEEE = true;
				beginEEE = token.getBegin();
			} // of if
			
			/*--------------------------------------------------------------------------*/
			/* If the current token is still inside the EEE, the variable endEEE will be*/
			/* updated. Thus it marks the end of the last checked token inside the EEE.	*/
			/* Once tokens outside the EEE were found, endEEE still will mark the end	*/
			/* of the last EEE.															*/
			/*--------------------------------------------------------------------------*/
			if (EEELabel.equals(EEE_LABEL))
			{
				endEEE = token.getEnd();
			} // of if
			
			/*--------------------------------------------------------------------------*/
			/* If the first token outside the EEE was found, a new EEE will be created	*/
			/* using the variables beginEEE and endEEE.									*/
			/*--------------------------------------------------------------------------*/
			if (EEELabel.equals(OUTSIDE_LABEL) && insideEEE)
			{
				insideEEE = false;
				EEE eee = new EEE(jcas);
				eee.setBegin(beginEEE);
				eee.setEnd(endEEE);
				eee.addToIndexes();
			} // of if
		} // of for
	} // of writeEEEToCas
	
	
} // of EEEAnnotator
