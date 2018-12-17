/** 
 * MUC7Coreference.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 * 
 * Author: poprat
 * 
 * Current version: 1.0 	
 * Since version:   1.0
 *
 * Creation date: Oct 11, 2007 
 * 
 * Object to handle MUC7 coreferences before transferring them to CAS objects
 **/

package de.julielab.jcore.reader.muc7;

public class MUC7Coreference {

	public int begin;
	
	public int end;
	
	public int id;
	
	public int refID;
	
	public String typeOfCoref;
	
	public String minHead;
	
	
	public MUC7Coreference() {
		// TODO Auto-generated constructor stub
	}
	
	
	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMinHead() {
		return minHead;
	}

	public void setMinHead(String minHead) {
		this.minHead = minHead;
	}

	public int getRefID() {
		return refID;
	}

	public void setRefID(int refID) {
		this.refID = refID;
	}

	public String getTypeOfCoref() {
		return typeOfCoref;
	}

	public void setTypeOfCoref(String typeOfCoref) {
		this.typeOfCoref = typeOfCoref;
	}



	
	
	
}
