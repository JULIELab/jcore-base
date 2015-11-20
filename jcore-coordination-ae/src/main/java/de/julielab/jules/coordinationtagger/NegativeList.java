/** 
 * NegativeList.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.2
 * Since version:   2.2
 *
 * Methods to handle a negative list of entity mentions. 
 **/

package de.julielab.jules.coordinationtagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeSet;


public class NegativeList {

	private final static String DELIM = "@";
	
	private TreeSet<String> negativeList;

	public NegativeList(File myFile) throws IOException {
		init(myFile);
	}
	
	/**
	 * reads the negative list from a file and stored the entries in a set
	 * @param myFile
	 */
	private void init(File myFile) throws IOException {
		negativeList = new TreeSet<String>();
		BufferedReader br;
			br = new BufferedReader(new FileReader(myFile));
		String line = "";
			while ((line = br.readLine()) != null) {
				negativeList.add(line);
			}
	}
	

	/**
	 * checks whether an entity mention is contained in the negative list. 
	 * @param mentionText the text covered by the entity annotation
	 * @param label the label assigned to this entity annotation
	 * @return true if negative list contains mentionText (with label)
	 */
	public boolean contains(String mentionText, String label) {
	
		// check with label
		String searchString = mentionText + DELIM + label;
		if (negativeList.contains(searchString)) {
			return true;
		}
	
		// check without label
		searchString = mentionText;
		if (negativeList.contains(searchString)) {
			return true;
		}
		return false;
	}
	
}

