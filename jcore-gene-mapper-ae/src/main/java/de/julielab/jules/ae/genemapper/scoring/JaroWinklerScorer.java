/** 
 * JaroWinklerScorer.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: Jan 17, 2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jules.ae.genemapper.scoring;

import com.wcohen.ss.JaroWinkler;

import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class JaroWinklerScorer extends Scorer {

	private JaroWinkler jaroWinkler;
	
	public JaroWinklerScorer() {
		jaroWinkler = new JaroWinkler();
	}
	@Override
	public double getScore(String term1, String term2) {
		return jaroWinkler.score(term1, term2);
	}

	public String info() {
		return "JaroWinklerScorer";
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		TermNormalizer normalizer = new TermNormalizer();
		String t1 = "ribosomal-protein-alanine N-acetyltransferase rimI";
		String t2 = "Ribosomal-protein-alanine acetyltransferase";
		String n1 = normalizer.normalize(t1);
		String n2 = normalizer.normalize(t2);
		JaroWinklerScorer s = new JaroWinklerScorer();
		System.out.println ("score (original): " + s.getScore(t1, t2));
		System.out.println ("score (normalized): " + s.getScore(n1, n2));
		

	}
	@Override
	public int getScorerType() {
		return GeneMapper.JAROWINKLER_SCORER;
	}

}
