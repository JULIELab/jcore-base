/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.ae.coordbaseline.main;

import de.julielab.jcore.ae.coordbaseline.tools.DataTypeHandling;
import de.julielab.jcore.ae.coordbaseline.tools.DataTypes;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;

import java.util.ArrayList;

/**
 * @author lichtenwald
 *
 * This class encapsulates the data that belong to the input sentence and the methods, which modify this data.
 */
public class SentenceAnalyser 
{	
	public SentenceAnalyser(){}
		
	public SentenceAnalyser (ArrayList<CoordinationToken> coordinationTokenList)
	{
		setSentenceAttributes(coordinationTokenList);
	} // of Sentence Constructor
	
	
	private static final String OUTSIDE_LABEL = "O";
	
	private static final String EEE_LABEL = "EEE";
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList represents the sentence which was given in the piped format.	*/
	/* Each element of this list is a coordinationToken. This means that every 		*/
	/* element of the input becomes an CoordinationToken object.					*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();
	
	/*------------------------------------------------------------------------------*/
	/* The tokenList represents the sentence which was given in the piped format.	*/
	/* Each element of this list is a token. Each token consists of a word followed	*/
	/* by labels and is separated from other tokens within the sentence by a space 	*/
	/* character.																	*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> tokenList = new ArrayList<String>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList contains only the words which will be extracted from the		*/
	/* tokenList.																	*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> wordList = new ArrayList<String>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList contains the pos tags which will be extracted from the			*/
	/* tokenList.																	*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> posTagList = new ArrayList<String>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList contains the entity labels of the sentence which will be 		*/
	/* extracted frin the tokenList.												*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> entityLabelList = new ArrayList<String>();
	
	/*------------------------------------------------------------------------------*/
	/* This boolean variable indicates whether the sentence contains a named entity.*/
	/*------------------------------------------------------------------------------*/
	private boolean containsEntity = false;
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList will contain EEEs which are provided by the sentence.			*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<EEEAnalyser> EEEList = new ArrayList<EEEAnalyser>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList will contain the labels which mark the EEE within the sentence*/
	/* Each element of this list is a String, either it's "O" and marks the outside	*/
	/* of the EEE, or it is "EEE" and marks therefore the inside of the EEE.			*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> EEELabelList = new ArrayList<String>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList will contain the labels which mark the ellipsis components	*/
	/* within the EEE (and  thus, of course, within the sentence). Each element of 	*/
	/* this list is a String, either it is "CONJ" indicating a conjunct, or it is	*/
	/* "A" indicating an antecedent or it is "O" indicating that the current token	*/
	/* is neither a conjunct nor an antecedent. In this case it is most likely a	*/
	/* coordination or it is simply a token outside of the EEE.						*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> coordinationLabelList = new ArrayList<String>();
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList will contain the complete token list which will contain the 	*/
	/* tokens of the input list (or rather the input array), the EEE labels and the	*/
	/* and the ellipsis labels which will be added to the tokens. The EEE labels 	*/
	/* mark the EEE within the sentence and the ellipsis labels mark the ellipsis	*/
	/* components (conjuncts and antecedents) within the EEE.						*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<String> completeList = new ArrayList<String>();
	
	
	/*------------------------------------------------------------------------------*/
	/* This String will contain the same information as the completeList does. Every	*/
	/* element of the completeList will be a token within the completeString.		*/
	/*------------------------------------------------------------------------------*/
	private String completeString = "";
	
	
	/*--------------*/
	/* get-Methods	*/
	/*--------------*/
	public ArrayList<String> getTokenList() {return this.tokenList;}
	public ArrayList<String> getWordList() {return this.wordList;}
	public ArrayList<String> getPosTagList() {return this.posTagList;}
	public ArrayList<String> getEntityLabelList() {return this.entityLabelList;}
	public ArrayList<EEEAnalyser> getEEEList() 	{return this.EEEList;}
	public ArrayList<String> getEEELabelList() 	{return this.EEELabelList;}
	public ArrayList<String> getCompleteList() {return this.completeList;}
	public String getCompleteString() 	{
											setCompleteString(DataTypeHandling.arrayListToString(completeList, " "));
											return this.completeString;
										}
	public ArrayList<String> getcoordinationLabelList() {return this.coordinationLabelList;}
	public ArrayList<CoordinationToken> getCoordinationTokenList() {return this.coordinationTokenList;}
	
	/*--------------*/
	/* set-Methods	*/
	/*--------------*/
	public void setTokenList(ArrayList <String> tokenList) {this.tokenList = tokenList;}
	public void setWordList(ArrayList <String> wordList) {this.wordList = wordList;}
	public void setPosTagList(ArrayList<String> posTagList) {this.posTagList = posTagList;}
	public void setEntityLabelList(ArrayList<String> entityLabelList) {this.entityLabelList = entityLabelList;}
	public void setEEEList(ArrayList<EEEAnalyser> EEEList) {this.EEEList = EEEList;}
	public void setEEELabelList(ArrayList<String> EEELabelList) {this.EEELabelList = EEELabelList;}
	public void setCompleteList(ArrayList<String> completeList) {this.completeList = completeList;}
	public void setCompleteString(String completeString) {this.completeString = completeString;}
	public void setcoordinationLabelList(ArrayList<String> coordinationLabelList) {this.coordinationLabelList = coordinationLabelList;}
	public void setCoordinationTokenList(ArrayList<CoordinationToken> coordinationTokenList) {this.coordinationTokenList = coordinationTokenList;}
	
	/*------------------------------------------------------------------------------------------*/
	/* 									Other methods 											*/
	/*------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to compute the proposal of the EEE within a given sentence. Please note 
	 * that before calling this method, the object must be instantiated using proper data (this data 
	 * must contain words (tokens) and their entity labels. The approach is first to find the EEE
	 * within the given sentence and then to put this information in the coordinationTokenList using 
	 * the method createEEELabelList.
	 * 
	 * @return coordiantionTolenList ArrayList of CoordinationToken which represents the input 
	 * sentence and contains EEE labels for every CoordinationToken. 
	 */
	public ArrayList<CoordinationToken> predictEEE()
	{
		/*--------------------------------------------------------------*/
		/* Now the findEEE method is used to find the occurrences of 	*/
		/* the EEE using the labels in the entityLabelList. Every found	*/
		/* EEE will be put in the EEEList for further computation.		*/
		/*--------------------------------------------------------------*/
		findEEE();
		
		/*--------------------------------------------------------------*/
		/* Now, all NEs present in the sentence should be identified by */
		/* the EEELabelList as being an EEE or not. The EEELabelList 	*/
		/* will be update in order to accommodate this new information.	*/
		/*--------------------------------------------------------------*/
		createEEELabelList();			
		
		/*------------------------------------------------------------------*/
		/* After creating the EEELabelList, the information contained there */
		/* will be converted and put into the coordinationTokenList.		*/
		/*------------------------------------------------------------------*/
		setEEELabelToCoordinationTokens(EEELabelList);

		return coordinationTokenList;
	} // of predictEEE


/*------------------------------------------------------------------------------------------*/
	/**
	 * This methos is used to predict the conjuncts of a given EEE
	 * 
	 * @param coordinationTokenList ArrayList<CoordinationToken> which represents the EEE
	 * 
	 * @return coordinationTokenList ArrayList<CoordinationToken> which is basically the input list but with additional information about conjuncts
	 */
	public ArrayList<CoordinationToken> predictConjuncts(ArrayList<CoordinationToken> coordinationTokenList)
	{		
		EEEAnalyser eee = new EEEAnalyser(coordinationTokenList);
		eee.computeConjuncts();
		EEEList.add(eee);
		
		/*--------------------------------------------------------------*/
		/* All EEEs should now be marked within the sentence with the	*/
		/* EEELabelList. Now the coordinationLabelList will be established	*/
		/*--------------------------------------------------------------*/
		createcoordinationLabelList(coordinationTokenList);
		
		return coordinationTokenList;
	} // of predictConjuncts
	
/*------------------------------------------------------------------------------------------*/
	/**
	 *  This methos is used to predict the resolved ellipsis of a given EEE
	 *  
	 *  @param coordinationTokenList ArrayList<CoordinationToken> which represents the EEE
	 *  
	 *  @return resolvedEllipsis String which represents the resolved ellipsis in text form
	 */
	public String predictEllipsis(ArrayList<CoordinationToken> coordinationTokenList)
	{
		String resolvedEllipsis = "";
		EEEAnalyser eee = new EEEAnalyser(coordinationTokenList);
		resolvedEllipsis = eee.computeResolvedEllipsisString(); 
		return resolvedEllipsis;
	} // of predictEllipsis
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set up several attributes using the coordinationTokenList
	 * 
	 * @param coordinationTokenList ArrayList which contains coordinationTokens and represents the sentence
	 */
	private void setSentenceAttributes(ArrayList<CoordinationToken> coordinationTokenList)
	{
		setCoordinationTokenList(coordinationTokenList);
		setWordList(extractWords(coordinationTokenList));
		setPosTagList(extractPosTags(coordinationTokenList));
		setEntityLabelList(extractEntityLabels(coordinationTokenList));	
	} // of setSentenceAttributes
	
/*------------------------------------------------------------------------------------------*/
	/**
	 *  This method is used to "extract" the tokens/words from the coordinationTokenList and return them in a wordList
	 *  
	 *  @param coordinationTokenList ArrayList<CoordinationToken> which represents a sentence
	 *  
	 *  @return wordList ArrayList<String> which contains the words/tokens from the coordinationTokenList
	 */
	private ArrayList<String> extractWords(ArrayList<CoordinationToken> coordinationTokenList)
	{
		ArrayList <String> wordList = new ArrayList<String>();
		String word = "";
		CoordinationToken coordinationToken = new CoordinationToken();
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			coordinationToken = coordinationTokenList.get(i);
			word = coordinationToken.getWord();
			wordList.add(word);
		} // of for
		return wordList;
	} // of extractWords
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to "extract" the POS tags from the coordinationTokenList and return them in a posTagList
	 *  
	 *  @param coordinationTokenList ArrayList<CoordinationToken> which represents a sentence
	 *  
	 *  @return posTagList ArrayList<String> which contains the posTags from the coordinationTokenList
	 */
	private ArrayList <String> extractPosTags(ArrayList<CoordinationToken> coordinationTokenList)
	{
		ArrayList<String> posTagList = new ArrayList<String>();
		String posTag = "";
		CoordinationToken coordinationToken = new CoordinationToken();
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			coordinationToken = coordinationTokenList.get(i);
			posTag = coordinationToken.getPosTag();
			posTagList.add(posTag);
		} // of for	
		return posTagList;
	} // of extractPosTags
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to "extract" the entity labels from the coordinationTokenList and return them in a entityLabelList
	 *  
	 *  @param coordinationTokenList ArrayList<CoordinationToken> which represents a sentence
	 *  
	 *  @return entityLabelList ArrayList<String> which contains the entity labels from the coordinationTokenList
	 */
	private ArrayList<String> extractEntityLabels(ArrayList<CoordinationToken> coordinationTokenList)
	{
		ArrayList<String> entityLabelList = new ArrayList<String>();
		String entityLabel = "";
		CoordinationToken coordinationToken = new CoordinationToken();
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			coordinationToken = coordinationTokenList.get(i);
			entityLabel = coordinationToken.getEntityLabel();
			entityLabelList.add(entityLabel);
		} // of for	
		return entityLabelList;
	} // of extractEntityLabels
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to determine if the sentence contains a named entity
	 * 
	 * @return containsEntity boolean variable which indicates the presence of a named entity 
	 */
	private boolean checkEntity()
	{
		boolean containsEntity = false;
		String token = "";
		
		for (int i = 0; i < this.entityLabelList.size(); i++)
		{
			token = entityLabelList.get(i);
			if (!token.equals(OUTSIDE_LABEL)) 
				{
					containsEntity = true;
					i = entityLabelList.size();
				} // of if
		} // of for
		
		return containsEntity;
	} // of checkEntity
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to create the EEELabelList by using the computed EEEs (every EEE 
	 * contains a start variable which indicates the start position of the EEE within the 
	 * sentence) and the auxiliary array EEELabelArray. This auxiliary array is used because 
	 * it allows easier access to every element when it comes to mark the EEE within this array 
	 * (which represents the sentence). 
	 */
	private void createEEELabelList()
	{
		int size = coordinationTokenList.size();		
		ArrayList<String> EEELabelList = new ArrayList<String>();
		String[] EEELabelArray = new String[size];
		
		/*------------------------------------------------------------------*/
		/* First, the auxiliary array EEELabelArray will be filled with "O" */
		/*------------------------------------------------------------------*/
		DataTypeHandling.initializeArray(EEELabelArray, OUTSIDE_LABEL);
		
		/*------------------------------------------------------------------*/
		/* Now, every EEE have to be "inserted" into the auxiliary array.	*/
		/*------------------------------------------------------------------*/
		for (int i = 0; i < EEEList.size(); i++)
		{
			int start = EEEList.get(i).getStart();
			
			/*--------------------------------------------------------------------------*/
			/* Here, j marks the start of the current EEE within the sentence and thus	*/
			/* within the auxiliary array. k is used to iterate though the wordList of	*/
			/* the EEE in order to put the right amount of EEE tokens to the aux array.	*/
			/*--------------------------------------------------------------------------*/
			for (int j = start, k = 0; k < EEEList.get(i).getWordList().size(); j++, k++)
			{
				EEELabelArray[j] = EEE_LABEL;
			} // of for	
		} // of for
				
		/*------------------------------------------------------------------------------*/
		/* Now, the auxiliary array will be converted into a ArrayList using the proper	*/
		/* method from the Conversion class. Afterwards, the obtained EEELabelList will */
		/* be stored in the proper class variable.										*/
		/*------------------------------------------------------------------------------*/
		EEELabelList = DataTypeHandling.arrayToArrayList(EEELabelArray);
		setEEELabelList(EEELabelList);	
	} // of createEEELabelList
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to convert the information which is contained in the EEELabelList
	 * to the coordinationTokenList. Please note that the already available coordinationTokenList
	 * will be used and the CoordinationTokens in this list will be updated by the additional
	 * EEE label information. The approach is to iterate through the established EEELabelList, 
	 * get the current EEElabel and put it into the proper CooordinationToken.
	 */
	private void setEEELabelToCoordinationTokens(ArrayList<String> EEELabelList) 
	{
		String EEELabel = "";
		CoordinationToken coordinationToken = new CoordinationToken();

		if (!(EEELabelList.size() == coordinationTokenList.size()))
		{
			System.err.println("ERROR! EEE LABEL LIST SIZE AND COORDINATION TOKEN LIST SIZE DON'T MATCH!");
			return;
		} // of if		
		
		for (int i=0; i<EEELabelList.size(); i++)
		{
			EEELabel = EEELabelList.get(i);
			coordinationToken = coordinationTokenList.get(i);
			coordinationToken.setEEELabel(EEELabel);
		} // of for
	} // setEEELabelToCoordinationTokens	

/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to determine whether a given ArrayList of Strings contains a
	 * coordination. For this purpose the coodrinationArray will be used.
	 * 
	 * @param NEList ArrayList which will be searched for coordinations
	 * @return containsCoordination boolean variable which indicates the presence of a coordinataion in the ArrayList
	 */
	private boolean checkCoordination(ArrayList<String> NEList)
	{
		boolean containsCoordination = false;
		String [] coordinationArray = DataTypes.getCoordinationArray();
		
		for (int i = 0; i < NEList.size(); i++)
		{
			for (int j = 0; j < coordinationArray.length; j++)
			{
				if (coordinationArray[j].equals(NEList.get(i))) containsCoordination = true;
			} // of for j
		} // of for i
		
		return containsCoordination;
	} // of checkCoordination
	
/*------------------------------------------------------------------------------------------*/
	// FIXME two consecutive entity labels
	// FIXME two consecutive entity labels
	
	/**
	 * This method is used to find any EEEs within the sentence and to store them in the EEEList. For this purpose
	 * the entityLabelList is used. This list indicates every word (token) as a specific entity of not. For further
	 * computation every found EEE will be stored in the sentences' EEEList. 
	 */
	private void findEEE()
	{
		/*----------------------------------------------------------*/
		/* These two lists are used to put the proper words (tokens)*/
		/* and pos tags in an EEE if there was found any.			*/
		/*----------------------------------------------------------*/
		ArrayList<String> NEWordList = new ArrayList<String>();
		ArrayList<String> NEPosTagList = new ArrayList<String>();
		
 		String token;
		String entityLabel = "";
		int start = 0;
		int mode = 0;
		int counter = 0;
		
		
		for (int i = 0; i < this.entityLabelList.size(); i++)
		{
			token = this.entityLabelList.get(i);
			/*--------------------------------------------------------------*/
			/* If there was a token which doesn't contain the out tag, the 	*/
			/* algorithm will switch to mode 1: NE was found. The start po-	*/
			/* sition of the NE and the entity label will be determined.	*/
			/*--------------------------------------------------------------*/
			if (!token.equals(OUTSIDE_LABEL) && mode == 0) 
			{
				mode = 1;
				start = i;
				entityLabel = token;
			} // of if
					
			/*----------------------------------------------------------*/
			/* If there was an out tag while mode 1, the algorithm will */
			/* switch to mode 0. If there was a NE, it will be checked	*/
			/* for being EEE. If so, a new EEE will be created by 		*/
			/* by passing the proper data to the constructor. Both the 	*/
			/* NEWordList and the NEPosTagList will be cleared.			*/
			/*----------------------------------------------------------*/
			if (token.equals(OUTSIDE_LABEL) && mode == 1)
			{
				mode = 0;
				/*----------------------------------------------------------------------*/
				/* If there was a coordination within the NE, a new EEE will be created	*/
				/*----------------------------------------------------------------------*/
				if (checkCoordination(NEWordList)) 
				{
					EEEAnalyser newEEE = new EEEAnalyser(DataTypeHandling.cloneList(NEWordList), DataTypeHandling.cloneList(NEPosTagList), start, entityLabel);
					EEEList.add(newEEE);
					counter++;
				} // of if 
				
				NEWordList.clear();
				NEPosTagList.clear();
			} // of if
					
			if (mode == 1)
			{
				NEWordList.add(wordList.get(i));
				NEPosTagList.add(posTagList.get(i));
			} // of if			
		} // of for		
		
	} // of findEEE
	
/*------------------------------------------------------------------------------------------*/	
	/**
	 * This method is used to create the coordinationLabelList by using the computed EEEs (every EEE 
	 * contains a start variable which indicates the start position of the EEE within the 
	 * sentence and furthermore it contains its own coordinationLabelList which mark the components 
	 * of the EEE) and the auxiliary array ellipsisLabelArray which represents the sentence. 
	 * This auxiliary array is used because it allows easier access to every element when it 
	 * comes to mark the EEE within this array.  
	 * 
	 * @param coordinationTokenList ArrayList<CoordinationToken> which represents a sentence
	 */
	private void createcoordinationLabelList(ArrayList<CoordinationToken> coordinationTokenList)
	{
		int size = coordinationTokenList.size();
		ArrayList<String> coordinationLabelList = new ArrayList<String>();
		String[] ellipsisLabelArray = new String[size];
		
		/*------------------------------------------------------------------*/
		/* First, the auxiliary array EEELabelArray will be filled with "O" */
		/*------------------------------------------------------------------*/
		DataTypeHandling.initializeArray(ellipsisLabelArray, OUTSIDE_LABEL);
		
		/*------------------------------------------------------------------*/
		/* Now, for every EEE the coordinationLabelList have to be "inserted" 	*/
		/* into the auxiliary array.										*/
		/*------------------------------------------------------------------*/
		for (int i = 0; i < EEEList.size(); i++)
		{
			try
			{
				int start = EEEList.get(i).getStart();
				
				/*--------------------------------------------------------------------------*/
				/* Here, j marks the start of the current EEE within the sentence and thus	*/
				/* within the auxiliary array. k is used to iterate though the 				*/
				/* coordinationLabelList of	the EEE in order to put the right ellipsis label	*/
				/* to the aux array.														*/
				/*--------------------------------------------------------------------------*/
				for (int j = start, k = 0; k < EEEList.get(i).getEllipsisLabelList().size(); j++, k++)
				{						
					ellipsisLabelArray[j] = EEEList.get(i).getEllipsisLabelList().get(k);
				} // of for
				
				/*------------------------------------------------------------------------------*/
				/* Now, the auxiliary array will be converted into a ArrayList using the proper	*/
				/* method from the Conversion class. Afterwards, the obtained coordinationLabelList */
				/* will be stored in the proper class variable.									*/
				/*------------------------------------------------------------------------------*/
				coordinationLabelList = DataTypeHandling.arrayToArrayList(ellipsisLabelArray);
				this.setcoordinationLabelList(coordinationLabelList);	
			} // of try
			catch (Exception e)
			{
				coordinationLabelList.clear();
					
				for (int l=0; l < EEEList.get(i).getWordList().size(); l++)
				{
					coordinationLabelList.add(OUTSIDE_LABEL);
				} // of for
				EEEList.get(i).setEllipsisLabelList(coordinationLabelList);
			} // of catch
		} // of for
				
		setCoordinationLabelToCoordinationTokens(coordinationLabelList, coordinationTokenList);
	} // of createcoordinationLabelList
	
/*------------------------------------------------------------------------------------------*/
	/**
	 *  This method is used to set the proper coordination labels to coordinationTokens in the coordinationTokenList
	 *  
	 *  @param coordinationLabelList ArrayList<String> which contains the coordination labels ("A", "CC", "CONJ");
	 */
	private void setCoordinationLabelToCoordinationTokens(ArrayList<String> coordinationLabelList, ArrayList<CoordinationToken> coordinationTokenList) 
	{
		String coordinationLabel = "";
		CoordinationToken coordinationToken = new CoordinationToken();

		if (!(coordinationLabelList.size() == coordinationTokenList.size()))
		{
			System.err.println("ERROR! COORDINATION LABEL LIST SIZE AND COORDINATION TOKEN LIST SIZE DON'T MATCH!");
			return;
		} // of if		
		
		for (int i=0; i<coordinationLabelList.size(); i++)
		{
			coordinationLabel = coordinationLabelList.get(i);
			coordinationToken = coordinationTokenList.get(i);
			coordinationToken.setCoordinationLabel(coordinationLabel);
		} // of for
		
	} // of setCoordinationLabelToCoordinationTokens
	
/*------------------------------------------------------------------------------------------*/
	
} // of Sentence
