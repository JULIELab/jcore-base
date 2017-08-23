/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.coordbaseline.annotators;

import java.util.ArrayList;
import java.util.HashMap;

import de.julielab.jcore.ae.coordbaseline.main.Baseline;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.EEE;
import de.julielab.jcore.types.Coordination;
import de.julielab.jcore.types.CoordinationElement;

import org.apache.uima.jcas.JCas;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CoordinationAnnotator extends JCasAnnotator_ImplBase 
{
	private final static Logger LOGGER = LoggerFactory.getLogger(CoordinationAnnotator.class);
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
		LOGGER.info("CoordinationAnnotator IS BEING INITIALIZED");
		super.initialize(aContext);
	} // of initialize
	
/*--------------------------------------------------------------------------------------------*/
	
	/**
	 * This method is used to process the information found so far (e.g. by the Tokenizer) and 
	 * add new information to the CAS (like EEEs inside the Sentences, Conjuncts inside the EEEs
	 * or resolved ellipses. For every sentence within the CAS, a coordinationTokenList will be 
	 * created. This List will be just another manifestation of the sentence which is suitable 
	 * for baseline prediction of EEEs, conjuncts and resolved ellipses. Once the 
	 * coordiantionTokenList is processed, the new information will be added to the CAS (e.g.
	 * by creating the accordant Objects like EEEs and writing them to the CAS). 
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException 
	{
		LOGGER.info("CoordinationAnnotator IS BEING PROCESSED");
//		ArrayList<String> tokenTextList = new ArrayList<String>();
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();
		HashMap<Token,CoordinationToken> tokenMap = new HashMap<Token,CoordinationToken>(); 
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		AnnotationIndex tokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		AnnotationIndex entityIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Entity.type);
		AnnotationIndex EEEIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
//		AnnotationIndex coordElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(CoordinationElement.type);
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
			
			
			/*------------------------------------------------------------------*/
			/* Iterate over EEEs within the sentence, predict coordination 		*/
			/* elements and resolved ellipsis for each EEE, create proper		*/
			/* objects and put them into the JCas.								*/
			/*------------------------------------------------------------------*/
			iterateOverEEEs(jcas, tokenIndex, EEEIndex, baseline, sentence);
			
		} // of while
				
	} // of process

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
	 * This method is used to iterate over EEEs within the sentence, to predict the coordination elements (conjunctions, conjuncts, antecedents) and the resolved ellipsis for each EEE and to put them into the JCas
	 * 
	 * @param jcas JCas which the coordination elements and the resolved ellipsis will be put into
	 * @param tokenIndex AnnotationIndex which is used to build the iterator over tokens
	 * @param EEEIndex AnnotationIndex which is used to build the iterator over EEEs
	 * @param baseline Baseline which undertakes the computation (prediction) of coordination elements and resolved ellipses for each EEE
	 * @param sentence Sentence which the EEE subiterator will iterate through
	 */
	private void iterateOverEEEs(JCas jcas,  AnnotationIndex tokenIndex, AnnotationIndex EEEIndex, Baseline baseline, Sentence sentence) 
	{
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();		
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
			
			String ellipsis = baseline.predictEllipsis(coordinationTokenList);	
			writeEllipsisToCas(ellipsis, jcas, eee);
			
			System.out.println("ELLIPSIS: " + ellipsis);
		} // of while
	} // of iterateOverEEEs

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
	 * This method is used to write found and resolved ellipsis into the JCas
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
	} // of writeToCas
	

} // of CoordinationAnnotator
