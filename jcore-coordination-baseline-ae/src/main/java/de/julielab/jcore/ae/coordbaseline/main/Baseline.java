/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.ae.coordbaseline.main;

import java.util.ArrayList;

import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;

/**
 * @author lichtenwald
 *
 * This class serves as an interface to the class Sentence, which takes on the computation. Here, three methods
 * ara available: predictEEE, predictConjuncts and predictEllipsis. 
 */
public class Baseline 
{
	/**
	 * Constructor
	 */
	public Baseline(){}

	
	/**
	 * This method is used to compute the proposal of the EEE within a given sentence. The input 
	 * consists of an ArrayList of CoordinationTokens. Each CoordinationToken represents a word(token) 
	 * and its entity label. For further information please consult the comments and documentation of 
	 * the CoordinationToken class. The output is again an ArrayList of CoordinationTokens which is 
	 * basically the input ArrayList with a major alteration: each CoordinationToken now contains a 
	 * defined EEE label. The approach is first to instantiate a Sentence object using the given 
	 * coordinationTokenList. Then in the second step to call the method predictEEE of this 
	 * instantiated object which returns the proposed information. 
	 * 
	 * @param coordinationTokenList ArrayList of CoordinationTokens which represents a sentence
	 * 
	 * @return coordinationTokenList ArrayList of CoordinationTokens which represents a sentence and now
	 * has a EEE label added to each of its CoordinationTokens 
	 */
	public ArrayList<CoordinationToken> predictEEE(ArrayList<CoordinationToken> coordinationTokenList)
	{		
		SentenceAnalyser sentence = new SentenceAnalyser(coordinationTokenList);
		return sentence.predictEEE();
	} // of predictEEE
	
	
	/**
	 * This method is used to compute the proposal of component parts (conjuncts, conjunctions, antecedents) 
	 * within an elliptical entity expression (EEE). The input consists of an ArrayList of CoordinationTokens. 
	 * Each CoordinationToken represents a word (token) and its part-of-speech tag (POS tag). For further 
	 * information please consult the comments and documentation of the CoordinationToken class. The output is
	 * again an ArrayList od CoordinationTokens which is basically the input ArrayList with a major alteration:
	 * each CoordinationToken now contains a defined coordination label. The approach is first to instantiate
	 * an object of the class Sentence using the given coordinationTokenList. Then in the second step, the 
	 * method predictConjuncts of the new instantiated object is called. It returns the proposed information.
	 *  
	 * @param coordinationTokenList ArrayList of CoordinationTokens which represents an EEE
	 * 
	 * @return coordinationTokenList ArrayList od CoordinationTokens which represents an EEE and now has a
	 * coordination label added to each of its CoordinationTokens
	 */
	public ArrayList<CoordinationToken> predictConjuncts(ArrayList<CoordinationToken> coordinationTokenList)
	{
		SentenceAnalyser sentence = new SentenceAnalyser(coordinationTokenList);
		return sentence.predictConjuncts(coordinationTokenList);
	} // of predictConjuncts
	
	
	/**
	 * This method is used to sompute the proposal of the resolved ellipsis within an elliptical entity expression
	 * (EEE). The input consists of an ArrayList of coordinationTokens. Each CoordinationToken represents a word 
	 * (token), its part-of-speech tag (POS tag) and its coordination label. For further information please consult 
	 * the comments and documentation of the CoordinationToken class. The output is a String which represents the
	 * resolved ellipsis. The approach is first to instantiate an object of the class Sentence using the given 
	 * coordinationTokenList. Then the method predictEllipsis is called, which returns the proposed information.
	 * 
	 * @param coordinationTokenList ArrayList of CoordinationTokens which represents an EEE 
	 * 
	 * @return string String thiich represents the resolved ellipsis
	 */
	public String predictEllipsis(ArrayList<CoordinationToken> coordinationTokenList)
	{
		SentenceAnalyser sentence = new SentenceAnalyser(coordinationTokenList);
		return sentence.predictEllipsis(coordinationTokenList);	 
	} // of predictEllipsis
	
} // of class Baseline
