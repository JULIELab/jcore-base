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
