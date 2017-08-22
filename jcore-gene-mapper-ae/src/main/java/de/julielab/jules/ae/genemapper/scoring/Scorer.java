/** 
 * Scorer.java
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
 * An abstract Scorer class
 **/

package de.julielab.jules.ae.genemapper.scoring;

public abstract class Scorer {

	public static final double PERFECT_SCORE = 9999;
	
	/**
	 * checks wether we have a perfect match
	 */
	public boolean isPerfectMatch(String term1, String term2) {
		if (null == term1 || null == term2)
			return false;
		term1 = term1.trim().replaceAll("[\\s ]+", " ");
		term2 = term2.trim().replaceAll("[\\s ]+", " ");
		if (term1.equals(term2)) {
			return true;
		} else {
			return false;
		}
	}
	
	public abstract double getScore(String term1, String term2) throws RuntimeException;

	public abstract String info();
	
	public abstract int getScorerType();
}
