/** 
 * NormalizerTest.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.4.2
 * Since version:   1.0
 *
 * Creation date: Feb 12, 2007 
 * 
 * JUnit Test for TermNormalizer
 * 
 * TODO: repair test
 **/

package de.julielab.jules.ae.genemapper.utils.norm;

import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class TermNormalizerTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TermNormalizerTest.class);

	protected void setUp() throws Exception {
		super.setUp();
		// set log4j properties file
//		PropertyConfigurator.configure("src/test/java/log4j.properties");
	}



	/**
	 * @throws IOException
	 */
	public void testNormalize() throws IOException {
		LOGGER.info("testNormalize()");
		ArrayList<String> terms = new ArrayList<String>();
		ArrayList<String> normalizedTerms = new ArrayList<String>();

		terms.add("IL-2Rhigh");
		normalizedTerms.add("il 2 r high");

		terms.add("TNFalpha");
		normalizedTerms.add("tnf alpha");
		
		terms.add("TNFrelated");
		normalizedTerms.add("tnf related");
		
		terms.add("2TNFchaperone");
		normalizedTerms.add("2 tnf chaperone");
		
		terms.add("IL2R");
		normalizedTerms.add("il 2 r");

		terms.add("IL L2Ralpha");
		normalizedTerms.add("il l 2 r alpha");

		terms.add("p45chaperone");
		normalizedTerms.add("p 45 chaperone");

		terms.add("IP1a5t");
		normalizedTerms.add("ip 1 a 5 t");

		terms.add("1.5kda proteins");
		normalizedTerms.add("1.5 kda proteins"); // was 1.5 kda protein (before deleting non-descriptive terms)

		terms.add("receptor of IL2");
		normalizedTerms.add("receptor il 2");

		terms.add("H2B.n");
		normalizedTerms.add("h 2 b n");
		
		terms.add("abc_d");
		normalizedTerms.add("abc d");
		
		terms.add("abc*!\"§$%&/()=#'~\\d");
		normalizedTerms.add("abc d");

		terms.add("123L");
		normalizedTerms.add("123 l");
		
		terms.add("12aaL");
		normalizedTerms.add("12 aa l");
		
		terms.add("1aal");
		normalizedTerms.add("1 aal");

		terms.add("123l");
		normalizedTerms.add("123 l");
		
		terms.add("2.4 Pu.m");
		normalizedTerms.add("2.4 pu m");

		terms.add("l");
		normalizedTerms.add("l");

		terms.add("IL 1rA type 2");
		normalizedTerms.add("il 1 r a type 2");

		
		terms.add("IL1ra");
		normalizedTerms.add("il 1 ra");
		
		terms.add("IL b");
		normalizedTerms.add("il b");
		
		terms.add("IL b 5");
		normalizedTerms.add("il b 5");

		terms.add("p50 subunit of Nfkappa-B");
		normalizedTerms.add("p 50 subunit nf kappa b");

		terms.add("IL r beta");
		normalizedTerms.add("il r beta");
		
		terms.add("IL rB");
		normalizedTerms.add("il r b");
		
		terms.add("IL RG");
		normalizedTerms.add("il rg");
		
		terms.add("IL13ra1");
		normalizedTerms.add("il 13 ra 1");
		
		terms.add("IL11 RA locus");
		normalizedTerms.add("il 11 ra locus");
		
		terms.add("IL RB 1");
		normalizedTerms.add("il rb 1");
		
		terms.add("IRE-BP");
		normalizedTerms.add("ire bp"); // was: ire binding protein (before deleting non-descriptive terms)
		
		terms.add("IRE-BP 2");
		normalizedTerms.add("ire bp 2"); // was: ire binding protein 2 (before deleting non-descriptive terms)
		
		terms.add("kinase II");
		normalizedTerms.add("kinase 2");
		
		terms.add("CS-IV");
		normalizedTerms.add("cs 4");
		
		terms.add("csIV");
		normalizedTerms.add("cs 4");

		terms.add("IL2R isoform");
		normalizedTerms.add("il 2 r");
		
		terms.add("NFkappaB p65");
		normalizedTerms.add("nf kappa b p 65");
		
		terms.add("Abeta");
		normalizedTerms.add("a beta");
		
		terms.add("Tau");
		normalizedTerms.add("tau");
		
		
		TermNormalizer normalizer = new TermNormalizer();

		boolean allOK = true;
		for (int i = 0; i < terms.size(); i++) {
			String out = normalizer.normalize(terms.get(i));
			String wanted = normalizedTerms.get(i);
			LOGGER.debug(terms.get(i) + " -> " + out + " (" + wanted + ")");
			if (!out.equals(wanted)) {
				allOK = false;
				LOGGER.error(" ====> unexpected normalization ^^^");
				continue;
			}
		}
		assertTrue(allOK);
		
	}
	
	public void testMuh() throws IOException {
		TermNormalizer normalizer = new TermNormalizer();
		String s = normalizer.normalize("TrxR1");
		System.out.println(s);
		
	}

}
