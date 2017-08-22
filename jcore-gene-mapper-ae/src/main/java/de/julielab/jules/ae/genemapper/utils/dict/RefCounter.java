package de.julielab.jules.ae.genemapper.utils.dict;

/** 
 * RefCounter.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3
 * Since version:   1.3
 *
 * Creation date: Sep 18, 2007 
 * 
 * Class to store frequencies of UniRef50 synonyms. 
 * Intended to be used for BioThesaurus cleanup.
 **/

import java.util.TreeSet;

public class RefCounter {

	private TreeSet<String> unirefs;
	private int counter;
	
	RefCounter (){
		this.unirefs = new TreeSet<String>();
		this.counter = 0;
	}

	RefCounter (TreeSet<String> unirefs){
		this.unirefs = unirefs;
		this.counter = 0;
	}
	
	RefCounter (String uniref){
		this.unirefs = new TreeSet<String>();
		this.unirefs.add(uniref);
		this.counter = 0;
	}
	
	RefCounter (TreeSet<String> unirefs, int counter){
		this.unirefs = unirefs;
		this.counter = counter;
	}
  
	public TreeSet<String> getUniRefs (){
		return unirefs;
	}
  
	public int getCounter (){
		return counter;
	}
	
	public void setCounter (int counter){
		this.counter = counter;
	}
	
	public void increment (){
		++counter;
	}
	
	public String toString() {
		return counter + "\t" + unirefs;
	}
}
