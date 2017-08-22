/** 
 * TokenJaroSimilarity.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.4 	
 * Since version:   1.4
 *
 * Creation date: Feb 8, 2007 
 * 
 * calculated the jaro similarity (taken from SecondString) on a token instead of a character
 * level
 **/

package de.julielab.jules.ae.genemapper.scoring;

import java.io.Serializable;
import java.util.HashMap;

import com.wcohen.ss.AbstractStringDistance;
import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.api.*;

/**
 * Jaro distance metric. From 'An Application of the Fellegi-Sunter Model of
 * Record Linkage to the 1990 U.S. Decennial Census' by William E. Winkler and
 * Yves Thibaudeau.
 */

public class TokenJaroSimilarity extends AbstractStringDistance implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TokenJaroSimilarity() {
	}

	public String toString() {
		return "[Jaro]";
	}

	
	public double tokenScore(String first, String second) {
		int replacement = 0;
		
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		
		StringBuffer term1 = new StringBuffer();
		StringBuffer term2 = new StringBuffer();
		term1.append("0");
		term2.append("0");
		for (int i = 0; i < firstArray.length; i++) {
			String token = firstArray[i];
			if (map.containsKey(token)) {
				term1.append(map.get(token).toString());
			} else {
				replacement++;
				term1.append(replacement);
				map.put(token, replacement);
			}
		}
		
		for (int i = 0; i < secondArray.length; i++) {
			String token = secondArray[i];
			if (map.containsKey(token)) {
				term2.append(map.get(token).toString());
			} else {
				replacement++;
				term2.append(replacement);
				map.put(token, replacement);
			}
		}
	
		//System.out.println("term1:" + term1.toString().trim());
		//System.out.println("term2:" + term2.toString().trim());
		
		String s1=term1.toString().trim();
		String s2=term2.toString().trim();
		return score(prepare(s1),prepare(s2));
	}

	
	public int getTokenTranspositions(String first, String second) {
		int replacement = 1;
		
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		
		StringBuffer term1 = new StringBuffer();
		StringBuffer term2 = new StringBuffer();
		term1.append("0");
		term2.append("0");
		for (int i = 0; i < firstArray.length; i++) {
			String token = firstArray[i];
			if (map.containsKey(token)) {
				term1.append(map.get(token).toString());
			} else {
				replacement++;
				term1.append(replacement);
				map.put(token, replacement);
			}
		}
		
		for (int i = 0; i < secondArray.length; i++) {
			String token = secondArray[i];
			if (map.containsKey(token)) {
				term2.append(map.get(token).toString());
			} else {
				replacement++;
				term2.append(replacement);
				map.put(token, replacement);
			}
		}
	
		String s1 = term1.toString().trim();
		String s2 = term2.toString().trim();
		
		return getTransposition(prepare(s1),prepare(s2));
	}
	
	
	public double score(StringWrapper s, StringWrapper t) {
		String str1 = s.unwrap();
		String str2 = t.unwrap();
		int halflen = halfLengthOfShorter(str1, str2);
		String common1 = commonChars(str1, str2, halflen);
		String common2 = commonChars(str2, str1, halflen);
		if (common1.length() != common2.length())
			return 0;
		if (common1.length() == 0 || common2.length() == 0)
			return 0;
		int transpositions = transpositions(common1, common2);
		double dist = (common1.length() / ((double) str1.length())
				+ common2.length() / ((double) str2.length()) + (common1
				.length() - transpositions)
				/ ((double) common1.length())) / 3.0;
		return dist;
	}

	public int getTransposition(StringWrapper s, StringWrapper t) {
		String str1 = s.unwrap();
		String str2 = t.unwrap();
		int halflen = halfLengthOfShorter(str1, str2);
		String common1 = commonChars(str1, str2, halflen);
		String common2 = commonChars(str2, str1, halflen);

		int res = 0;
		int trans = transpositions(common1, common2);
		if (score(s, t) == 0.0 && trans == 0) {
			res = -1;
		} else {
			res = trans;
		}
		return res;

	}


	public String explainScore(StringWrapper s, StringWrapper t) {
		String str1 = s.unwrap();
		String str2 = t.unwrap();
		int halflen = halfLengthOfShorter(str1, str2);
		String common1 = commonChars(str1, str2, halflen);
		String common2 = commonChars(str2, str1, halflen);
		// count transpositions
		if (common1.length() != common2.length())
			return "common1!=common2: '" + common1 + "' != '" + common2
					+ "'\nscore: " + score(s, t) + "\n";
		if (common1.length() == 0 || common2.length() == 0)
			return "|commoni|=0: common1='" + common1 + "' common2='" + common2
					+ "'\nscore: " + score(s, t) + "\n";
		int transpositions = transpositions(common1, common2);
		String explanation = "common1: '" + common1 + "'\n" + "common2: '"
				+ common2 + "'\n" + "transpositions: " + transpositions + "\n";
		return explanation + "internal score: " + score(s, t) + "\n";
	}

	private int halfLengthOfShorter(String str1, String str2) {
		return (str1.length() > str2.length()) ? str2.length() / 2 + 1 : str1
				.length() / 2 + 1;
	}

	private String commonChars(String s, String t, int halflen) {
		StringBuilder common = new StringBuilder();
		StringBuilder copy = new StringBuilder(t);
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			boolean foundIt = false;
			for (int j = Math.max(0, i - halflen); !foundIt
					&& j < Math.min(i + halflen, t.length()); j++) {
				if (copy.charAt(j) == ch) {
					foundIt = true;
					common.append(ch);
					copy.setCharAt(j, '*');
				}
			}
		}
		return common.toString();
	}

	public int transpositions(String common1, String common2) {
		int transpositions = 0;
		int len = Math.min(common1.length(),common2.length());
		for (int i = 0; i < len
		; i++) {
			//for (int i = 0; i < common1.length(); i++) {
			if (common1.charAt(i) != common2.charAt(i))
				transpositions++;
		}
		transpositions /= 2;
		return transpositions;
	}

	public StringWrapper prepare(String s) {
		return new BasicStringWrapper(s.toLowerCase());
	}

	static public void main(String[] argv) {
		System.out.println((new TokenJaroSimilarity()).getTokenTranspositions("il 12 a b", "il receptor 12"));
	}
}