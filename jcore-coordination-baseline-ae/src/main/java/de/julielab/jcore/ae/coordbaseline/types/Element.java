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
package de.julielab.jcore.ae.coordbaseline.types;
import java.util.ArrayList;


/**
 * 
 * @author lichtenwald
 *
 * This class will encapsulate a set of Tokens which are assembled in an ArrayList
 */
public class Element 
{	
	public Element(ArrayList<CoordinationToken> tokenList)
	{
		setTokenList(tokenList);
	} // of Constructor SeparatedElement


	private ArrayList<CoordinationToken> tokenList = new ArrayList<CoordinationToken>();
	
	/*--------------*/
	/* get-Methods.	*/
	/*--------------*/
	public ArrayList<CoordinationToken> getTokenList() {return this.tokenList;}
	
	
	/*--------------*/
	/* set-Methods.	*/
	/*--------------*/
	public void setTokenList(ArrayList<CoordinationToken> tokenList) {this.tokenList = tokenList;} 
		
	
	/*------------------------------------------------------------------------------------------*/
	/* 									Other methods 											*/
	/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to output the components of the element to String
	 * 
	 * @return output String which will contain the token components 
	 */
	public String toString()
	{
		String output = "";
		for (int i = 0; i < this.tokenList.size(); i++)
		{
			output = output + "\n" + tokenList.get(i).toString();
		} // of for
		
		output = output + "\n";
		return output;
	} // of toString
		
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to return the tag of the Element which simply will be a String which	
	 * contains all tags of the tokens of the current element.
	 * 
	 * @return tag String which will contain all of the tags of the tokens of the current element.
	 */
	public String getPosTag()
	{
		String tag = "";
		CoordinationToken token;
		
		for (int i = 0; i < this.tokenList.size(); i++)
		{
			token = this.tokenList.get(i);
			tag = tag + token.getPosTag();
		} // of for
		
		return tag;
	} // of getTag
	
	
/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set up an ellipsis label for an element which means that all of 
	 * the tokens will get this ellipsis label
	 * 
	 * @param ellipsisLabel String which contains the ellipsis label to be given to the element
	 */
	public void setEllipsisLabel(String ellipsisLabel)
	{
		for (int i = 0; i < this.tokenList.size(); i++)
		{
			tokenList.get(i).setCoordinationLabel(ellipsisLabel);
		} // of for
	} // of setEllipsisTag
	
} // of Element
