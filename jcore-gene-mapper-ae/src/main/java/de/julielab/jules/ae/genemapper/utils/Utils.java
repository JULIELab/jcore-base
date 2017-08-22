/** 
 * Utils.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.7
 * Since version:   1.3
 *
 * Creation date: Aug 3, 2007 
 * 
 * Some utils employed by different classes.
 **/

package de.julielab.jules.ae.genemapper.utils;

import java.util.ArrayList;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static final Logger LOGGER = LoggerFactory
	.getLogger(Utils.class);
	
	/**
	 * returns a TreeSet of the common tokens of two strings (white space
	 * tokenization is done)
	 * 
	 * @return
	 */
	public static TreeSet<String> getCommonWords(String[] firstArray,
			String[] secondArray) {

		// add tokens to sets
		TreeSet<String> firstSet = new TreeSet<String>();
		for (int i = 0; i < firstArray.length; i++) {
			firstSet.add(firstArray[i]);
		}
		TreeSet<String> secondSet = new TreeSet<String>();
		for (int i = 0; i < secondArray.length; i++) {
			secondSet.add(secondArray[i]);
		}

		// get the intersection of both sets
		TreeSet<String> intersection = new TreeSet<String>(firstSet);
		intersection.retainAll(secondSet);
		return intersection;
	}

	public static TreeSet<String> getDifferentWords(String[] firstArray,
			String[] secondArray) {

		// add tokens to sets
		TreeSet<String> firstSet = new TreeSet<String>();
		for (int i = 0; i < firstArray.length; i++) {
			firstSet.add(firstArray[i]);
		}
		TreeSet<String> secondSet = new TreeSet<String>();
		for (int i = 0; i < secondArray.length; i++) {
			secondSet.add(secondArray[i]);
		}

		// get the difference between both
		TreeSet<String> different = new TreeSet<String>(firstSet);
		different.removeAll(secondSet);
		return different;
	}

	

	/**
	 * gets the token overlap ratio between two strings whitespace split to get
	 * tokens ratio returned is minimum of overlap ratios of both strings
	 */
	public static double getOverlapRatio(String first, String second) {

		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		int firstLength = firstArray.length;
		int secondLength = secondArray.length;
		int commonWords = getCommonWords(firstArray, secondArray).size();
		// LOGGER.debug("# common words: " + commonWords);
		double firstRatio = commonWords / (double) firstLength;
		double secondRatio = commonWords / (double) secondLength;
		return Math.min(firstRatio, secondRatio);

	}
	
   public static ArrayList<String> makeBigrams(String term) {
		
		String[] split = term.split(" ");
		ArrayList<String> bigrams = new ArrayList<String>();
		
		for (int i=1; i < split.length; i++) {
			String bigram = split[i-1] + " " + split[i];
			bigram = bigram.trim();
			bigrams.add(bigram);
		}
				
		return bigrams;
	}
   
   public static String makeUnderScoreBigrams(String term) {
		
		String[] split = term.split(" ");
		//ArrayList<String> bigrams = new ArrayList<String>();
		String bigrams = "";
		
		if(split.length == 1) {
			bigrams = term;
			//System.out.println("UNIGRAM: " + bigrams);
		}
		else {
			for (int i=1; i < split.length; i++) {
				String bigram = split[i-1] + "_" + split[i];	
				bigrams += bigram + " ";
			}
			//System.out.println("BIGRAMS: " + bigrams);
		}
		
		return bigrams.trim();
	}
}
