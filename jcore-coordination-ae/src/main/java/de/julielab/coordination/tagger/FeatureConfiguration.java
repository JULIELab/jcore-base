/** 
 * FeatureConfiguration.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
  * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Feb 27, 2008 
 * 
 * helper class for reading in feature configurations
 **/

package de.julielab.coordination.tagger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureConfiguration {

	/**
	 * check whether a boolean feature exists and is set to "true"
	 * @param propDef
	 * @param propName
	 * @return
	 */
	public static boolean featureActive(Properties propDef, String propName) {
		if (propDef.getProperty(propName) != null
				&& propDef.getProperty(propName).equals("true")) {
			return true;
		} else {
			return false;
		}
	}

	public static int[] getIntArray(Properties propDef, String propName) {
		int[] intArray = null;

		if (propDef.getProperty(propName) != null) {
			String[] stringArray = propDef.getProperty(propName).trim().split(",");

			intArray = new int[stringArray.length];
			for (int i = 0; i < stringArray.length; i++) {
				intArray[i] = (new Integer(stringArray[i])).intValue();
			}
		}
		return intArray;
	}
	
	/**
	 * extracts the offset conjunction information (feature creation horizon)
	 * from a String of the form (-1) (0) (1), (-1) (0) (1,2) or (-1) (0) (1 2)
	 */
	public static int[][] offsetConjFromConfig(String offset_conjunctions) {
		if (offset_conjunctions==null) {
			return null;
		}
		
		int[][] conjunctions;

		// to find round brackets with digits sperated with whitespaces
		Pattern inBrackets = Pattern.compile("\\([-\\d\\s,]+\\)");
		Matcher bracketMatcher = inBrackets.matcher(offset_conjunctions);

		ArrayList<String> bracketContents = new ArrayList<String>();
		int bracketStart, bracketEnd;
		int pos = 0;

		// get all brackets and write their content in the ArrayList
		while (bracketMatcher.find(pos)) {
			bracketStart = bracketMatcher.start() + 1;
			bracketEnd = bracketMatcher.end() - 1;
			bracketContents.add(offset_conjunctions.substring(bracketStart,
					bracketEnd));
			pos = bracketMatcher.end();
		}
		// create an array for each bracket and fill it with the digits in the
		// bracket
		conjunctions = new int[bracketContents.size()][];
		for (int i = 0; i < bracketContents.size(); i++) {
			String[] digits = bracketContents.get(i).split(",");
			conjunctions[i] = new int[digits.length]; // array creation
			for (int j = 0; j < digits.length; j++) {
				conjunctions[i][j] = Integer.parseInt(digits[j].trim()); // filling
			}
		}
		return conjunctions;
	}
	
	public static ArrayList<String> getLexiconKeys(Properties propDef) {
		ArrayList<String> lexiconKeys = new ArrayList<String>();

		Enumeration keys = propDef.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.matches("[a-zA-Z]+_lexicon")) {
				lexiconKeys.add(key);
			}
		}
		return lexiconKeys;
	}
	
	/**
	 * The Properties Object featureConfig contains key-value pairs. Some of
	 * these pairs correspond to information about meta datas. This method
	 * returns a list of the used meta datas by searching for pairs of the form
	 * "xxx_feat_enabled = true". It then adds the "xxx" to the list. For
	 * "pos_feat_enabled = true" it will add pos to the list. For
	 * "chunk_feat_enabled = false" it will do nothing.
	 * 
	 */
	public static String[] getTrueMetas(Properties featureConfig) {
		ArrayList<String> trueMetas = new ArrayList<String>();

		Enumeration keys = featureConfig.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.matches("[a-zA-Z]+_feat_enabled")
					&& featureConfig.getProperty(key).equals("true")) {
				trueMetas.add(key.substring(0, key.indexOf("_feat_enabled")));
			}
		}
		String[] ret = new String[trueMetas.size()];
		trueMetas.toArray(ret);
		return ret;
	}
	
	
}
