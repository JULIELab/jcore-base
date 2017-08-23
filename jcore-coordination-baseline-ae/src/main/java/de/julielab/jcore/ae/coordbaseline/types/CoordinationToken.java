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

/**
 * 
 * @author lichtenwald
 *
 * This class encapsulates the data that belong to the token (like the word or the appropriate
 * pos tag) and the methods which modify these data.	
 */
public class CoordinationToken
{
	public CoordinationToken(){}
	
	CoordinationToken(String word, String posTag)
	{
		setWord(word);
		setPosTag(posTag);
	} // of Constructor
	
	CoordinationToken(String word, String posTag, String entityLabel)
	{
		setWord(word);
		setPosTag(posTag);
		setEntityLabel(entityLabel); 
	} // of Constructor
	
	public CoordinationToken(String word, String posTag, String entityLabel, String EEELabel, String coordinationLabel)
	{
		setWord(word);
		setPosTag(posTag);
		setEntityLabel(entityLabel);
		setEEELabel(EEELabel);
		setCoordinationLabel(coordinationLabel); 		// former setEllipsisLabel(ellipsisLabel); 
	} // of Constructor
	
	/*--------------------------------------------------------------*/
	/* This string contains the word that specifies the token.		*/
	/*--------------------------------------------------------------*/
	private String word = "-";
	
	/*--------------------------------------------------------------*/
	/* This string contains the pos tag that specifies the token.	*/
	/*--------------------------------------------------------------*/
	private String posTag = "-";
	
	/*--------------------------------------------------------------------------*/
	/* This String variable contains the entity label of the EEE.				*/
	/*--------------------------------------------------------------------------*/
	private String entityLabel = "-";
	
	/*------------------------------------------------------------------*/
	/* This string contains the label, which will indicate the token as */
	/* a part of an EEE (it will then get the EEELabel "EEE") or not	*/
	/* (it will then get the EEELabel "O" - outside).					*/
	/*------------------------------------------------------------------*/
	private String EEELabel = "-";
	
	/*--------------------------------------------------------------*/
	/* This string contains the label, which will mark the token as */
	/* a certain part of the ellipsis. There will be "CONJ" indi-	*/
	/* cating the token as a conjunct, "A" indicating it as an 		*/
	/* ellipsis antecedent and "O" to mark the token neither a con-	*/
	/* junct nor an antecedent. Default value is "O".				*/
	/*--------------------------------------------------------------*/
	private String coordinationLabel = "-";
	
	
	/*--------------*/
	/* get-Methods	*/
	/*--------------*/
	public String getWord() {return this.word;}
	public String getPosTag() {return this.posTag;}
	public String getCoordinationLabel() {return this.coordinationLabel;}
	public String getEntityLabel() {return this.entityLabel;}
	public String getEEELabel() {return this.EEELabel;}
	
	/*--------------*/
	/* set-Methods	*/
	/*--------------*/
	public void setWord(String word) {this.word = word;}
	public void setPosTag(String posTag) {this.posTag = posTag;}
	public void setCoordinationLabel(String coordinationLabel) {this.coordinationLabel = coordinationLabel;}
	public void setEntityLabel(String entityLabel) {this.entityLabel = entityLabel;}
	public void setEEELabel(String EEELabel) {this.EEELabel = EEELabel;}
	
	
	/*------------------------------------------------------------------------------------------*/
	/* 									Other methods 											*/
	/*------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to output the components of the Token to a String
	 * 
	 * @return output String which will contain the token components
	 */
	public String toString()
	{
		String output = "";
		output = output + word + "|" + posTag + "|" + entityLabel + "|" + EEELabel + "|" + coordinationLabel; 
		return output;
	} // toString
	
	
	
} // of Token
