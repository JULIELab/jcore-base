/** 
 * Column.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3.1
 * Since version:   1.2
 *
 * Creation date: Aug 02, 2007 
 * 
 * Class containing rules for ThesaurusCleaner.
 * Extended class to contain totalMatch switch (true/false),
 * which restricts matches to "equals".
 **/

package de.julielab.jules.ae.genemapper.utils.dict;

public class Column {

	private int column;
	private String filter;
	private boolean totalMatch;

	Column (int column, String filter, boolean totalMatch){
		this.column = column;
		this.filter = filter;
		this.totalMatch = totalMatch;
	}
  
	public int getColumn (){
		return column;
	}
  
	public String getFilter (){
		return filter;
	}
	
	public boolean getMatchStyle (){
		return totalMatch;
	}
}