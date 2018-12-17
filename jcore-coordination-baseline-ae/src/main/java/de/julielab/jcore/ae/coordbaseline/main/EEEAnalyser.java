/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.ae.coordbaseline.main;

import java.util.ArrayList;

import de.julielab.jcore.ae.coordbaseline.tools.*;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import de.julielab.jcore.ae.coordbaseline.types.Element;

/**
 * 
 * @author lichtenwald
 *
 * This class represents the elliptical entity expression (EEE)
 */
class EEEAnalyser 
{
	/*--------------*/
	/* Constructor	*/
	/*--------------*/
	EEEAnalyser(ArrayList<String> wordList, ArrayList<String> posTagList, int start, String entityLabel)
	{
		setEEEAttributes(wordList, posTagList, start, entityLabel);
	} // of Constructor EEE
	
	
	EEEAnalyser(ArrayList<CoordinationToken> coordinationTokenList)
	{
		setEEEAttributes(coordinationTokenList);
	} // of Constructor
	
	
	private static final String COMMA = ",";
	
	private static final String PIPE = "|";
	
	private static final String ANTECEDENT_LABEL = "A";
	
	private static final String CONJUNCT_LABEL = "CONJ";
	
	private static final String EEE_LABEL = "EEE";
	
	private static final String CONJUNCTION_LABEL = "CC";
	
	
	/*------------------------------------------------------------------------------*/
	/* This ArrayList represents the sentence which was given in the piped format.	*/
	/* Each element of this list is a coordinationToken. This means that every 		*/
	/* element of the input becomes an CoordinationToken object.					*/
	/*------------------------------------------------------------------------------*/
	private ArrayList<CoordinationToken> coordinationTokenList = new ArrayList<CoordinationToken>();
	
	/*--------------------------------------------------------------*/
	/* This ArrayList contains the tokens which represent the EEE.	*/
	/*--------------------------------------------------------------*/
	private ArrayList<String> tokenList = new ArrayList<String>();
	
	/*--------------------------------------------------------------*/
	/* This ArrayList contains the words which represent the EEE.	*/
	/*--------------------------------------------------------------*/
	private ArrayList<String> wordList = new ArrayList<String>();
	
	/*--------------------------------------------------------------*/
	/* This ArrayList contains the pos tags which represent the EEE	*/
	/*--------------------------------------------------------------*/
	private ArrayList<String> posTagList = new ArrayList<String>();
	
	/*--------------------------------------------------------------------------*/
	/* This integer variable marks the beginning of the EEE within the sentence.	*/
	/*--------------------------------------------------------------------------*/
	private int start = 0;
	
	
	/*--------------------------------------------------------------------------*/
	/* This String variable contains the entity label of the EEE.				*/
	/*--------------------------------------------------------------------------*/
	private String entityLabel = "";
	
	
	/*--------------------------------------------------------------------------*/
	/* This ArrayList will contain the elements of the EEE which are separated	*/
	/* by the coordination.														*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<Element> separatedElementList = new ArrayList<Element>();
	
	
	/*--------------------------------------------------------------------------*/
	/* This ArrayList will contain the Tokens which represent the coordinations	*/
	/* within the EEE.															*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<CoordinationToken> coordinationList = new ArrayList<CoordinationToken>();
	
	
	/*--------------------------------------------------------------------------*/
	/* This ArrayList contains the same elements as the separatedElementList	*/
	/* does except for the first and the last elements.							*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<Element> intermediateElementList = new ArrayList<Element>();
	
	
	/*--------------------------------------------------------------------------*/
	/* This ArrayList contains the labels which mark the miscellaneous compo-	*/
	/* nents within the EEE. Each element of this list is a String. It is either*/
	/* "CONJ" and marks thus the conjuncts, or it is "A" to mark the antecedent,*/
	/* or it is "O" to mark a component of the EEE which is neither a conjunct	*/
	/* nor an antecedent - most likely it's a coordination.						*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<String> ellipsisLabelList = new ArrayList <String>();
	
	
	/*--------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<String> resolvedEllipsisList = new ArrayList<String>();
	
	
	/*--------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<String> leftAntecedentList = new ArrayList<String>();
	
	
	/*--------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<String> rightAntecedentList = new ArrayList<String>();
	
	
	/*--------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------*/
	private ArrayList<Element> conjunctList = new ArrayList<Element>();
	
	
	/*--------------------------------------------------------------------------*/
	/*--------------------------------------------------------------------------*/
	private String resolvedEllipsisString = "";
	
	
	/*--------------------------------------------------------------------------*/
	/* The auxiliary arrays are used to help building the ellipsisLabelList. For*/ 
	/* further explanations see the comments in the code where these array are 	*/
	/* acutally being used.														*/
	/*--------------------------------------------------------------------------*/
	private boolean [] leftAuxiliaryArray;
	private boolean [] rightAuxiliaryArray; 
	
	/*--------------*/
	/* get-Methods	*/
	/*--------------*/
	public ArrayList<String> getWordList() {return this.wordList;}
	public int getStart() {return this.start;}
	public String getEntityLabel() {return this.entityLabel;}
	public ArrayList<String> getPosTagList() {return this.posTagList;}
	public ArrayList<Element> getElementList() {return this.separatedElementList;}
	public ArrayList<CoordinationToken> getCoordinationList() {return this.coordinationList;}
	public ArrayList<Element> getIntermediateElementList() {return this.intermediateElementList;}
	public boolean[] getLeftAuxiliaryArray() {return this.leftAuxiliaryArray;}
	public boolean[] getRightAuxiliaryArray() {return this.rightAuxiliaryArray;}
	public ArrayList<String> getEllipsisLabelList() {return this.ellipsisLabelList;}
	public ArrayList<String> getTokenList() {return this.tokenList;}
	public String getResolvedEllipsisString() {return this.resolvedEllipsisString;}
	
	/*--------------*/
	/* set-Methods	*/
	/*--------------*/
	public void setWordList(ArrayList<String> wordList) {this.wordList = wordList;}
	public void setStart(int start) {this.start = start;}
	public void setEntityLabel(String entityLabel) {this.entityLabel = entityLabel;}
	public void setPosTagList(ArrayList<String> posTagList) {this.posTagList = posTagList;}
	public void setElementList(ArrayList<Element> separatedElementList) {this.separatedElementList = separatedElementList;}
	public void setCoordinationList(ArrayList<CoordinationToken> coordinationList) {this.coordinationList = coordinationList;}
	public void setIntermediateElementList(ArrayList<Element> intermediateElementList) {this.intermediateElementList = intermediateElementList;}
	public void setLeftAuxiliaryArray(boolean[] leftAuxiliaryArray) {this.leftAuxiliaryArray = leftAuxiliaryArray;}
	public void setRightAuxiliaryArray(boolean[] rightAuxiliaryArray) {this.rightAuxiliaryArray = rightAuxiliaryArray;}
	public void setEllipsisLabelList(ArrayList<String> ellipsisLabelList) {this.ellipsisLabelList = ellipsisLabelList;}
	public void setTokenList(ArrayList<String> tokenList) {this.tokenList = tokenList;}
	public void setResolvedEllipsisString(String resolvedEllipsisString) {this.resolvedEllipsisString = resolvedEllipsisString;}
	
	
	/*------------------------------------------------------------------------------------------*/
	/* 									Other methods 											*/
	/*------------------------------------------------------------------------------------------*/
	/**
	 *  // TODO
	 */
	public void computeEEEAttributes()
	{
		computeElements();			
		computeCoordinationList();
		computeIntermediateElements();
		compareElements();
		computeEllipsisLabelList(); 
		computeConjunctList();
		computeResolvedEllipsisList();
		computeCompleteString();
	} // of computeEEEAttributes
	
/*----------------------------------------------------------------------------------------------*/
	/**
	 *  //TODO
	 */
	public String computeResolvedEllipsisString()
	{		
		computeEEEAttributes();
		try {resolvedEllipsisString = postWorkResolvedEllipsisString(resolvedEllipsisString);}
		catch (Exception e) {System.err.println("ERROR!" + e.getMessage());}
		return resolvedEllipsisString;
	} // of computeResolvedEllipsisString

/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to post work the resolved ellipsis string and to eliminate unwanted space characters.
	 * Example: let the resolved ellipsis String be "A cells , B cells , and C cells". 
	 * The two space characters between "cells" and "," are the result of the generic resolving of the ellipsis
	 * and have to be removed. The approach is first to check, if there is at least one comma in the inputString. 
	 * If so, the algorithm is to iterate through the inputString "backwards" and insert any found characters in 
	 * the outputString unless it is a space character which follows a comma. Once such character was found, it 
	 * will not be inserted into the outputString and "jumped over". 
	 * 
	 *@param inputString String which will be cleared of unwanted space characters ahead of commas
	 *
	 *@return outputString String which basically complies with the inputString which the space characters were removed from
	 */
	private String postWorkResolvedEllipsisString(String inputString) 
	{
		/*------------------------------------------------------------------*/
		/* If there is no comma, there is no need to postwork the string.	*/
		/*------------------------------------------------------------------*/
		if (!inputString.contains(COMMA)) return inputString;
		
		String outputString = "";
		Character currentCharacter = new Character(inputString.charAt(0));
		Character previousCharacter = new Character(inputString.charAt(0));	
		/*--------------------------------------------------------------------------*/
		/* Please note that only the last n-1 characters will be iterated through. 	*/
		/* The very first character was left over, it has to be added separately.	*/
		/*--------------------------------------------------------------------------*/
		for (int i=inputString.length()-1; i>0; i--)
		{
			currentCharacter = new Character(inputString.charAt(i));
			previousCharacter = new Character(inputString.charAt(i-1));
			outputString = currentCharacter.charValue() + outputString;
			/*----------------------------------------------------------------------*/
			/* If there is a comma which is preceded by a space character, jump over*/
			/* this character.														*/
			/*----------------------------------------------------------------------*/
			if (currentCharacter.equals(',') && previousCharacter.equals(' ')) i--;
		} // of for
		/*----------------------------------------------------------------------*/
		/* The very first character is added to the outputString separately.	*/
		/*----------------------------------------------------------------------*/
		outputString = previousCharacter.charValue() + outputString;		
		return outputString;
	} // postWorkResolvedEllipsisString

/*----------------------------------------------------------------------------------------------*/
	/**
	 *  // TODO
	 */
	public void computeConjuncts()
	{	
		computeElements();			
		computeCoordinationList();
		computeIntermediateElements();		
		compareElements();
		computeEllipsisLabelList(); 
	} // of computeConjuncts
 	
	
/*----------------------------------------------------------------------------------------------*/
	/**
	 *  // TODO
	 */
	private void compareElements() 
	{		
		/*--------------------------------------------------------------------------------------*/
		/* If there are more than zero intermediate elements, their length and their pos tags 	*/
		/* will be compared in order to use this information for further handling of the left	*/
		/* and right separated elements. For example, if the pos tags aren't equal, then there	*/
		/* will be no comparison with the pos tags ofthe remaining separated elements.			*/
		/*--------------------------------------------------------------------------------------*/
		if ((intermediateElementList.size() > 0))
		{
			if (compareIntermediateElementsLength() && compareIntermediatePosTags())
			{
				compareOuterElements(intermediateElementList.get(0).getPosTag());
			} // of if
			else
			{
				compareOuterElements("");
			} // of else
		} // of if
		/*--------------------------------------------------------------------------------------*/
		/* Else if there are no intermediate elements, then only the left and right separated	*/
		/* parts will be handled.																*/
		/*--------------------------------------------------------------------------------------*/
		else
		{
			compareOuterElements("");
		} // of else
	} // of compareElements


/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to resolve the ellipsis. The result will be put into the resolvedEllipsisList
	 */
	private void computeResolvedEllipsisList() 
	{
		computeLeftAntecedentList();
		computeRightAntecedentList();
		
		for (int i=0; i<coordinationList.size(); i++)
		{
			if (!(conjunctList.get(i) == null))
			{	
				addLeftAntecedent();
				addConjunct(conjunctList.get(i));
				addRightAntecedent();
			} // of if
				addCoordination(i);
		} // of for
		
		if (!(conjunctList.get(conjunctList.size()-1) == null))
		{	
			addLeftAntecedent();
			addConjunct(conjunctList.get(conjunctList.size()-1));
			addRightAntecedent();
		} // of if
	} // of computeResolvedEllipsis()

/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to transform the resolved ellipsis information from an ArrayList to a String.
	 */
	private void computeCompleteString() 
	{
		String word = "";
		String outputString = "";
		String element = "";
		
		for (int i=0; i<resolvedEllipsisList.size() -1; i++)
		{
			element = resolvedEllipsisList.get(i);
			word = element.substring(0, element.indexOf(PIPE));
			outputString = outputString + word + " ";
		} // of for
		
		element = resolvedEllipsisList.get(resolvedEllipsisList.size()-1);
		word = element.substring(0, element.indexOf(PIPE));
		outputString = outputString + word;		
		
		resolvedEllipsisString = outputString;
	} // of computeCompleteString
	
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to add coordinations from the coordinationList to the resolvedEllipsisList
	 */
	private void addCoordination(int i) 
	{
		CoordinationToken token = coordinationList.get(i);
		String coordination = token.getWord() + PIPE + token.getPosTag(); // + "|" + token.getEntityLabel() + "|O";
		resolvedEllipsisList.add(coordination);
	} // addCoordination
	
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to add the right antecedent from the rightAntecedentList to the resolvedEllipsisList
	 */
	private void addRightAntecedent() 
	{
		for (int i=0; i<rightAntecedentList.size(); i++)
		{
			resolvedEllipsisList.add(rightAntecedentList.get(i));
		} // of for
		
	} // of addRightAntecedent
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to add conjuncts to the resolvedEllipsisList. As every conjunct can contain more than
	 * one token, that is why conjuncts contain tokenLists
	 * 
	 * @param element Element which represents the conjunct
	 */
	private void addConjunct(Element element) 
	{
		ArrayList<CoordinationToken> tokenList = element.getTokenList();
		String conjunct = "";
		
		
		for (int i=0; i<tokenList.size(); i++)
		{
			CoordinationToken token = tokenList.get(i);
			conjunct = token.getWord() + PIPE + token.getPosTag(); // + PIPE + token.getEntityLabel() + "|CONJ";
			resolvedEllipsisList.add(conjunct);
		} // of for
		
	} // addConjunct
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to add the left antecedent from the leftAntecedentList to the resolvedEllipsisList
	 */
	private void addLeftAntecedent() 
	{
		for (int i=0; i<leftAntecedentList.size(); i++)
		{
			resolvedEllipsisList.add(leftAntecedentList.get(i));
		} // of for
	
	} // of computeLeftAntecedentList
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This is the method which builds the conjunctList
	 */
	private void computeConjunctList() 
	{
		String word = "";
		String posTag = "";
		String previousEllipsisElement = "";
		String ellipsisLabelListElement = "";
		ArrayList<CoordinationToken> tokenList = new ArrayList<CoordinationToken>();
		boolean conjunctFound = false;
		
		for (int i=0; i<ellipsisLabelList.size(); i++)
		{
			ellipsisLabelListElement = ellipsisLabelList.get(i);
			if (ellipsisLabelListElement.equals(CONJUNCT_LABEL))
			{		
				conjunctFound = true;
				word = wordList.get(i);
				posTag = posTagList.get(i);
				CoordinationToken token = new CoordinationToken(word, posTag, entityLabel, EEE_LABEL, ""); // TODO get rid of entityLabel and EEE_LABEL
				tokenList.add(token);				
			} // of if
	
			if ((conjunctFound && ellipsisLabelListElement.equals(CONJUNCTION_LABEL)) || (conjunctFound && i==ellipsisLabelList.size() -1))
			{
				Element conjunct = new Element(tokenList);
				conjunctList.add(conjunct);
				tokenList = new ArrayList<CoordinationToken>();
				conjunctFound = false;
			} // of if
			
			if (previousEllipsisElement.equals(CONJUNCTION_LABEL) && ellipsisLabelListElement.equals(CONJUNCTION_LABEL))
			{
				conjunctList.add(null);
			} // of if
			
			previousEllipsisElement = ellipsisLabelListElement;		
		} // of for
	} // of computeConjunctList
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compute the right antecedent. For this purpose the beforehand calculated
	 * ellipsisLabelList is used. 
	 */
	private void computeRightAntecedentList() 
	{
		String ellipsisLabelListElement = "";
		String rightAntecedentListElement = "";
		
		for (int i=ellipsisLabelList.size()-1; i>=0; i--)
		{
			ellipsisLabelListElement = ellipsisLabelList.get(i);
			if (ellipsisLabelListElement.equals(ANTECEDENT_LABEL))
			{
				rightAntecedentListElement = wordList.get(i) + PIPE + posTagList.get(i); // + PIPE + entityLabel + "|EEE|" + ellipsisLabelListElement;
				rightAntecedentList.add(rightAntecedentListElement);
			} // of if
			else
			{
				i = -1;
			} // of else
		} // of for
		
		rightAntecedentList = DataTypeHandling.reverseList(rightAntecedentList);
	} // of computerightAntecedentList
/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compute the left antecedent. For this purpose the beforehand calculated
	 * ellipsisLabelList is used. 
	 */
	private void computeLeftAntecedentList() 
	{
		String ellipsisLabelListElement = "";
		String leftAntecedentListElement = "";
		for (int i=0; i<ellipsisLabelList.size(); i++)
		{
			ellipsisLabelListElement = ellipsisLabelList.get(i);
			if (ellipsisLabelListElement.equals(ANTECEDENT_LABEL))
			{
				leftAntecedentListElement = wordList.get(i) + PIPE + posTagList.get(i); // + PIPE + entityLabel + "|EEE|" + ellipsisLabelListElement;
				leftAntecedentList.add(leftAntecedentListElement);
			} // of if
			else
			{
			 	i = ellipsisLabelList.size();
			} // of else
		} // of for
	} // of computeLeftAntecedentList
/*----------------------------------------------------------------------------------------------*/
	/**
	 *  //TODO
	 */
	private void setEEEAttributes(ArrayList<String> wordList, ArrayList<String> posTagList, int start, String entityLabel)
	{
		setWordList(wordList);
		setStart(start);
		setEntityLabel(entityLabel);
		setPosTagList(posTagList);
	} // of setEEEAttributes
	
/*----------------------------------------------------------------------------------------------*/	
	/**
	 *  // TODO
	 * @param coordinationTokenList
	 */
	private void setEEEAttributes(ArrayList <CoordinationToken> coordinationTokenList)
	{		
		setWordList(extractWords(coordinationTokenList));
		setPosTagList(extractPosTags(coordinationTokenList));
		setEllipsisLabelList(extractCoordinationLabels(coordinationTokenList));	
	} // of setEEEAttributes
	
/*------------------------------------------------------------------------------------------*/
	/**
	 *  // TODO
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
	 *  // TODO
	 */
	private ArrayList<String> extractCoordinationLabels(ArrayList<CoordinationToken> coordinationTokenList) 
	{
		ArrayList <String> coordinationLabelList = new ArrayList<String>();
		String coordinationLabel = "";
		CoordinationToken coordinationToken = new CoordinationToken();
		
		for (int i=0; i<coordinationTokenList.size(); i++)
		{
			coordinationToken = coordinationTokenList.get(i);
			coordinationLabel = coordinationToken.getCoordinationLabel();
			coordinationLabelList.add(coordinationLabel);
		} // of for
		return coordinationLabelList;
	} // of extractCoordinationLabels
	
/*------------------------------------------------------------------------------------------*/
	/**
	 *  // TODO
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
		
/*----------------------------------------------------------------------------------------------*/	
	/**
	 * This method is used to compute the parts of the EEE that are separated by a coordination.
	 * 
	 */
	private void computeElements()
	{
		String [] coordinationArray = DataTypes.getCoordinationArray();
		boolean isCoordination = false;
		CoordinationToken token;
		ArrayList<CoordinationToken> tokenList = new ArrayList<CoordinationToken>();
		Element separatedElement;
		
		/*--------------------------------------------------------------------------*/
		/* Every element of the wordList will be checked if it is a coordination. 	*/
		/* If it is not, then a new Token will be created using the word and its	*/
		/* pos tag, then this Token will be added to the tokenList.	If the current	*/
		/* is a coordination, then a new Element will be created using 	*/
		/* previously built tokenList. This new Element will be added to 	*/
		/* the separatedElementList.												*/
		/*--------------------------------------------------------------------------*/
		for (int i = 0; i < wordList.size(); i++)
		{
			/*----------------------------------------------------------*/
			/* Current word will be checked for being a coordination.	*/
			/*----------------------------------------------------------*/
			for (int j = 0; j < coordinationArray.length; j++)
			{
				if (coordinationArray[j].equals(wordList.get(i))) isCoordination = true;
			} // of for j
			
			if (!isCoordination)
			{
				token = new CoordinationToken(wordList.get(i), posTagList.get(i), entityLabel, "", ""); // TODO get rid of entityLabel and ""
				tokenList.add(token);
			} // of if
			
			if(isCoordination || i == wordList.size() - 1)
			{
				separatedElement = new Element(DataTypeHandling.cloneList(tokenList)); 
				separatedElementList.add(separatedElement);
				tokenList.clear();
				isCoordination = false;
			} // of if			
		} // of for
	} // of computeElements
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compute the ArrayList, which will contain the coordinations of the EEE.
	 */
	private void computeCoordinationList()
	{
		String [] coordinationArray = DataTypes.getCoordinationArray();
		CoordinationToken token;
		
		/*--------------------------------------------------------------------------------------*/
		/* Each word from the wordList will be checked if it equals one of the entries of the 	*/
		/* coordination array. If it does so, then the word is a coordination thus it will be	*/
		/* added to the coordination list.														*/
		/*--------------------------------------------------------------------------------------*/		 
		for (int i = 0; i < wordList.size(); i++)
		{
			for (int j = 0; j < coordinationArray.length; j++)
			{
				if (coordinationArray[j].equals(wordList.get(i))) 
				{
					token = new CoordinationToken(wordList.get(i), posTagList.get(i), entityLabel, "", ""); //TODO get rid of entityLabel and ""
					coordinationList.add(token);
				} // of if
			} // of for
		} // of for
	} // of computeCoordinationList
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compute the intermediate elements using the list of separated elements. 
	 * The intermediate elements will be stored in the intermediateList.
	 */
	private void computeIntermediateElements()
	{
		/*------------------------------------------------------------------*/
		/* The intermediate elements are separated elements except for the	*/ 
		/* first and the last separated element.								*/
		/*------------------------------------------------------------------*/ 
		for (int i = 1; i < this.separatedElementList.size()-1; i++ )
		{
			this.intermediateElementList.add(this.separatedElementList.get(i));
		} // of for
	} // of computeIntermediateElements
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compare the length of the elements of the intermediateElementList. This list 
	 * consists of ArrayLists itself, namely TokenLists, thus the size of these TokenLists will be compared. 
	 * If all those TokenLists are of the same size, true will be returned. Otherwise, of course, false will 
	 * be returned.
	 * 
	 *  @return equalLength boolean variable which indicates equality of the list sizes, true means equal.
	 */
	private boolean compareIntermediateElementsLength()
	{
		boolean equalLength = true;
		int currentSize = 0;
		int newSize = 0;
		
		/*------------------------------------------------------------------------------------------*/
		/* If the size of the intermediateElementList is lesser than 2, that means there are either	*/
		/* one or none elements (TokenLists). This means in turn that all of the elements have the 	*/
		/* same size and therefore true will be returned. Otherwise the element sizes will be 		*/
		/* compared.																					*/
		/*------------------------------------------------------------------------------------------*/
		if (intermediateElementList.size() > 1)
		{
			/*--------------------------------------------------*/
			/* The very first TokenList size will be determined.	*/
			/*--------------------------------------------------*/
			currentSize = intermediateElementList.get(0).getTokenList().size();
			
			for (int i = 1; i < intermediateElementList.size(); i++)
			{
					newSize = intermediateElementList.get(i).getTokenList().size();		
					if (!(currentSize == newSize) && newSize > 0) // FIXME why newSize > 0 ???
					{ 
						equalLength = false;
						currentSize = newSize;
					} // of if
			} // of for
		} // of else
		
		return equalLength;
	} // of compareIntermediateElementsLength
		
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compare the pos tags of the intermediate elements. If all of the 
	 * intermediate elements match regarding the pos tags (if the tags are equal), the method 
	 * returns true, otherwise false.
	 * 
	 * @return equalTag boolean variable that indicates the equality ot the intermediate elements 
	 * regarding the pos tags (equality of the pos tags)
	 */
	private boolean compareIntermediatePosTags()
	{
		boolean equalTag = true;
		String currentTag = "";
		String newTag = "";
		
		/*----------------------------------------------------------------------------------*/
		/* If there is an intermediateElementList with less than two elements, it means that*/
		/* the tags are equal. Otherwise the equality will be checked.						*/
		/*----------------------------------------------------------------------------------*/
		if (intermediateElementList.size() > 1 )
		{
			/*--------------------------------------*/
			/* Very first tag will be determined.	*/
			/*--------------------------------------*/
			currentTag = intermediateElementList.get(0).getPosTag();
			
			for (int i = 1; i < intermediateElementList.size(); i++)
			{
				newTag = intermediateElementList.get(i).getPosTag();
				if (!currentTag.equals(newTag) && newTag.length() > 0) // FIXME why newTag.length() > 0
				{
					equalTag = false;
					currentTag = newTag;
				} // of if
			} // of for
		} // of else
		
		return equalTag;
	} // of compareIntermediatePosTags

/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compare the pos tags of separated elements on the far left, on the far right and the 
	 * pos tag which is representative for the pos tags of the intermediate elements (by definition the intermediate
	 * pos tags must be equal). Dependent on the comparison result, the auxiliary boolean arrays help to distinguish 
	 * between conjuncts and antecedents within both of the elements.
	 * 
	 * @param posTag String which contain a representative pos tag from the intermediate list. 
	 */
	private void compareOuterElements(String posTag)
	{
		Element outerLeftElement = this.separatedElementList.get(0);
		Element outerRightElement = this.separatedElementList.get(this.separatedElementList.size() - 1);
		String leftPosTag = "";
		String rightPosTag = "";
		
		/*----------------------------------------------------------------------------------------------------------*/
		/* The counters will helpt to keep track within the outer left or outer right element. As the left element	*/
		/* will be processed "backwards" from the last token to the first, the left counter is initialized with the */
		/* last position within the proper TokenList. The right element will be processed in the right direction, so*/
		/* the appropriate counter is initialized with 0.															*/
		/*----------------------------------------------------------------------------------------------------------*/
		int leftCounter = outerLeftElement.getTokenList().size() -1;
		int rightCounter = 0;
		
		/*----------------------------------------------------------------------------------------------------------*/
		/* The auxiliary arrays are used to mark	 the already compared pos tags as their sizes correspond to the	*/
		/* sizes of the appropriate elements (TokenLists) and every field element of these auxiliary arrays			*/
		/* correspond to tokens of the left and right element. "True" means that the proper pos tags were equal.	*/
		/*----------------------------------------------------------------------------------------------------------*/
		this.leftAuxiliaryArray = new boolean[outerLeftElement.getTokenList().size()];
		this.rightAuxiliaryArray = new boolean[outerRightElement.getTokenList().size()];
		
		this.leftAuxiliaryArray = DataTypeHandling.initializeArray(this.leftAuxiliaryArray, false);
		this.rightAuxiliaryArray = DataTypeHandling.initializeArray(this.rightAuxiliaryArray, false);

		/*------------------------------------------------------------------------------------------*/
		/* The loop will be executed until both of the counters counted trough their TokenLists.	*/
		/*------------------------------------------------------------------------------------------*/
		while (leftCounter >= 0 && rightCounter < outerRightElement.getTokenList().size())
		{
			/*--------------------------------------------------------------------------------------------------*/
			/* The pos tags will be determined. To determine the current left pos tag, the algorithm accesses	*/
			/* the outerLeftElement's TokenList and gets the PosTag from the current Token. The current left 	*/
			/* Token is marked by the leftCounter. The processing is analogous for the rightPosTag. The current	*/
			/* strings containing the posTags will be concatenated with the determined strings from the previous*/
			/* loop turn. This is done too catch the cases where the elements reveal their pos tag equality 	*/
			/* across several pos tags, consider this example: Type_NN 1_CD and_CC Type_NN 2_CD receptors_NNS.	*/
			/* Notice that the concatenation instructions for the leftPosTag differs from the concatenation	for	*/
			/* the rightPosTag due to different "processing directions" within the TokenLists.					*/
			/*--------------------------------------------------------------------------------------------------*/ 
			leftPosTag = outerLeftElement.getTokenList().get(leftCounter).getPosTag() + leftPosTag;
			rightPosTag = rightPosTag + outerRightElement.getTokenList().get(rightCounter).getPosTag();
			
			/*----------------------------------------------------------------------------------------------*/
			/* If the passed pos tag is empty (which means that there was no pos tag, most likely there are */
			/* no intermediate elements) and the left and right pos tags were equal, this will be marked in */ 
			/* the auxiliary arrays using the counters to mark the right position. Notice that as the		*/
			/* processing goes on, the current pos tags will be concatenated with the previously obtained	*/
			/* pos tags. Thus the positions in the auxiliary arrays indicate the equality of the concate-	*/
			/* nated pos tags, not the current pos tags. Also consider the different "processing directions"*/
			/* of the auxiliary arrays.																		*/
			/*----------------------------------------------------------------------------------------------*/
			if (posTag.length() == 0 && leftPosTag.equals(rightPosTag))
			{
				this.rightAuxiliaryArray[rightCounter] = true;
				this.leftAuxiliaryArray[leftCounter] = true;
			} // of if
			
			/*----------------------------------------------------------------------------------------------*/
			/* If the passed posTag isn't empty (means there were intermediate elements, their pos tags were*/
			/* compared to each other and these pos tags were equal, so the posTag represents those equal 	*/
			/* intermediate pos tags) and it is equal to the leftPosTag and the rightPosTag, this will be	*/
			/* marked in the auxiliary arrays.																*/
			/*----------------------------------------------------------------------------------------------*/
			if (posTag.length() > 0 && leftPosTag.equals(rightPosTag) &&  leftPosTag.equals(posTag)) // FIXME
			{
				this.rightAuxiliaryArray[rightCounter] = true;
				this.leftAuxiliaryArray[leftCounter] = true;
			} // of if
			
			leftCounter--;
			rightCounter++;
		} // of while
		
		/*------------------------------------------------------------------------------------------------------*/
		/* Now, the auxiliary arrays mark positions within the proper outerLeftElement or outerRightElement at 	*/
		/* which the pos tags of these elements match (true) or not (false). There can be cases where the tags	*/
		/* at first don't match and then they do match (consider the example: Type_NN 1_CD and_CC Type_NN 2_CD	*/
		/* receptors_NNS). The result for the auxiliary arrays would be:  										*/
		/* leftAuxiliaryArray: [true, false]																	*/
		/* rightAuxiliaryArray: [false, true, false]															*/
		/* In order to use the auxiliary arrays to mark the conjuncts with true and the antecedents with false	*/
		/* a postprocessing has to be done. Consider the rightAuxiliaryArray. The idea is, that the position	*/
		/* with the highest index which contains true marks as discussed the congruent (concatenated) pos tags,	*/
		/* and thus the "end" of the conjunct - all of the higher indices will contain false and therefore		*/
		/* mark the antecedent. Some of the positions with indices lesser than the "end" of the conjunct might	*/
		/* contain false, because at the time of their computation the pos tags didn't match, but still they are*/
		/* within the conjunct. Exactly these "false"-positions have to be turned to "true" in a postprocessing	*/
		/* step so that the conjunct can simply be read off the auxiliary array.	 Given the example and the	*/
		/* proper auxiliary arrays, the resulting auxiliary arrays would be:									*/
		/* leftAuxiliaryArray: [true, true]																		*/
		/* rightAuxiliaryArray: [true, true, false]																*/
		/*------------------------------------------------------------------------------------------------------*/
		this.leftAuxiliaryArray = postprocessLeftAuxiliaryArray(this.leftAuxiliaryArray);
		this.rightAuxiliaryArray = postprocessRightAuxiliaryArray(this.rightAuxiliaryArray);
	} // of compareOuterElements
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to postprocess the leftAuxiliaryArray. The first elements of the inputArray 
	 * which contain "false" will be taken over to the outputArray until the first "true" element is found.
	 * From then the remaining elements of the outputArray will become "true.
	 * 
	 *  @param inputArray boolean array which serves as a sceletal structure to the outputArray, the inputArray
	 *  will be postprocessed and the results appear in the outputArray
	 *  @return outputArray booleanArray which is the result of the postprocessing the inputArray
	 */
	private boolean[] postprocessLeftAuxiliaryArray(boolean [] inputArray)
	{
		boolean[] outputArray = new boolean [inputArray.length];
		boolean trueFound = false;
		
		for (int i = 0; i < outputArray.length; i++)
		{
			if (inputArray[i] == true) trueFound = true;
			if (trueFound) outputArray[i] = true;
			else outputArray[i] = false;
		} // of for
		
		return outputArray;
	} // of postprocessLeftAuxiliaryArray
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to postprocess the rightAuxiliaryArray. Unlike the leftAuxiliaryArray, the rightAuxiliaryArray
	 * will be postprocessed in the opposite direction. The last elements of the inputArray which contain "false" will
	 * be taken over to the outputArray until the first "true" element is found. From then the remaining elements of the 
	 * outputArray will become "true.
	 * 
	 *  @param inputArray boolean array which serves as a sceletal structure to the outputArray, the inputArray
	 *  will be postprocessed and the results appear in the outputArray
	 *  @return outputArray booleanArray which is the result of the postprocessing the inputArray
	 */
	private boolean[] postprocessRightAuxiliaryArray(boolean [] inputArray)
	{
		boolean[] outputArray = new boolean [inputArray.length];
		boolean trueFound = false;
		
		for (int i = outputArray.length -1; i >= 0; i--)
		{
			if (inputArray[i] == true) trueFound = true;
			if (trueFound) outputArray[i] = true;
			else outputArray[i] = false;
		} // of for
		
		return outputArray;
	} // of postprocessLeftAuxiliaryArray

/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to compute the ellipsisLabelList which marks the EEEs components. The computation
	 * is divided into three main steps: 1) using leftAuxiliaryArray, 2) using coordinationList and the 
	 * intermediateList, 3) using the rightAuxiliaryArray
	 */
	private void computeEllipsisLabelList()
	{
		ellipsisLabelList = new ArrayList<String>();
		
		/*----------------------------------*/
		/* 1) Using the leftAuxiliaryArray	*/
		/*----------------------------------*/
		for (int i = 0; i < leftAuxiliaryArray.length; i++)
		{
			if (leftAuxiliaryArray[i]) ellipsisLabelList.add(CONJUNCT_LABEL);
			else ellipsisLabelList.add(ANTECEDENT_LABEL);
		} // of for
		
		/*--------------------------------------------------------------------------*/
		/* 2) Using the coodrinationList and the intermediateList. Notice that the	*/
		/* size of the coordinationList should be larger than the size of the inter-*/
		/* mediateElementList by exactly one element.								*/
		/*--------------------------------------------------------------------------*/
		for (int i = 0; i < coordinationList.size() - 1; i++)
		{
			ellipsisLabelList.add(CONJUNCTION_LABEL);
			setIntermediateEllipsisLabels (intermediateElementList.get(i).getTokenList());
		} // of for
		/*----------------------------------------------------------------------------------------------*/
		/* To cover the remaining element of the coordinationList, a final conjunction label is added.	*/
		/*----------------------------------------------------------------------------------------------*/
		ellipsisLabelList.add(CONJUNCTION_LABEL);
		
		
		/*----------------------------------*/
		/* 3) Using the rightAuxiliaryArray	*/
		/*----------------------------------*/
		for (int i = 0; i < rightAuxiliaryArray.length; i++)
		{
			if (rightAuxiliaryArray[i]) ellipsisLabelList.add(CONJUNCT_LABEL);
			else ellipsisLabelList.add(ANTECEDENT_LABEL);
		} // of for
		
		/*------------------------------------------------------------------------------------------*/
		/* If the size of the computed ellipsisLabelList does not match the size of the tokenList,	*/
		/* then obviously something gone wrong...													*/
		/*------------------------------------------------------------------------------------------*/	
		if (!(ellipsisLabelList.size() == wordList.size())) System.err.println("ERROR!"); 		// FIXME LOGFILE!!!		
	} // of computeEllipsisLabelList
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to add the conjunct label to the ellipsisLabelList for every token
	 * in a given tokenList.
	 * 
	 * @param tokenList ArrayList which contains tokens, for every token in this list a conjunct label
	 * will be added to the ellipsisLabelList
	 */
	private void setIntermediateEllipsisLabels (ArrayList<CoordinationToken> tokenList)
	{
		for (int i = 0; i < tokenList.size(); i++)
		{
			ellipsisLabelList.add(CONJUNCT_LABEL);
		} // of for
	} // of setIntermediateEllipsisLabel
	
} // of EEE
