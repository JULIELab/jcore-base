/** 
 * SimpleScorer.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.4.2	
 * Since version:   1.3
 *
 * Creation date: Aug 2, 2007 
 * 
 * calculates the score as an overlap of both the query and the document
 **/

package de.julielab.jules.ae.genemapper.scoring;

import java.util.TreeSet;

import de.julielab.jules.ae.genemapper.GeneMapper;


public class SimpleScorer extends Scorer {

	
	/**
	 * get the score for an unlabeled pair
	 * @param term1 the original term in text
	 * @param term2 the synonym found in db
	 * @return
	 * @throws Exception
	 */
	
	public double getScore(String term1, String term2) {
		//System.out.println("[SimpleScorer] comparing: " + term1 + " <-> " + term2);
		
		if (isPerfectMatch(term1,term2)) {
			return PERFECT_SCORE;
		}
		
		String[] term1Elements = term1.split(" ");
		TreeSet<String> term1Set = new TreeSet<String>();
		for (int i = 0; i < term1Elements.length; i++) {
			term1Set.add(term1Elements[i]);
		}

		String[] term2Elements = term2.split(" ");
		TreeSet<String> term2Set = new TreeSet<String>();
		for (int i = 0; i < term2Elements.length; i++) {
			term2Set.add(term2Elements[i]);
		}

		int term1Size = term1Set.size();
		int term2Size = term2Set.size();
		// get the intersection of tokens from term1 and tokens from term2
		term2Set.retainAll(term1Set);  
		int intersectionSize = term2Set.size();

		double score = (intersectionSize / (double) term1Size)
				* (intersectionSize / (double) term2Size);
		return score;
	}
	
	
	public double getMaxLenDiffScore(String term1, String term2) {
		//System.out.println("[SimpleScorer] comparing: " + term1 + " <-> " + term2);
		
		if (term1.equals(term2)) {
			return 1;
		}
		
		String[] term1Elements = term1.split(" ");
		TreeSet<String> term1Set = new TreeSet<String>();
		for (int i = 0; i < term1Elements.length; i++) {
			term1Set.add(term1Elements[i]);
		}

		String[] term2Elements = term2.split(" ");
		TreeSet<String> term2Set = new TreeSet<String>();
		for (int i = 0; i < term2Elements.length; i++) {
			term2Set.add(term2Elements[i]);
		}

		int term1Size = term1Set.size();
		int term2Size = term2Set.size();
		// get the intersection of tokens from term1 and tokens from term2
		term2Set.retainAll(term1Set);  
		int intersectionSize = term2Set.size();

		double lendiff1 = 0;
		double lendiff2 = 0;
		lendiff1 = term1Size-intersectionSize;
		lendiff2 = term2Size-intersectionSize;
		if (lendiff1>lendiff2) {
			return lendiff1;
		} else {
			return lendiff2;
		}
	}
	
	public String info() {
		return "SimpleScorer";
	}
	
	public static void main(String[] args) {
		String s1 = "hemoglobin gamma a chain";
		String s2="hemoglobin gamma g";
		System.out.println((new SimpleScorer()).getScore(s1,s2));
	}


	@Override
	public int getScorerType() {
		return GeneMapper.SIMPLE_SCORER;
	}

}
