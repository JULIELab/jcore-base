/** 
 * PairExtracter.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3 	
 * Since version:   1.1
 *
 * Creation date: Jun 31, 2007 
 * 
 * JUnit Test for PairExtracter
 **/

package de.julielab.jules.ae.genemapper;

import java.util.Arrays;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.scoring.MaxEntScorerPairExtractor;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizerTest;

public class PairExtracterTest extends TestCase {
	
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TermNormalizerTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		// set log4j properties file
//		PropertyConfigurator.configure("src/test/java/log4j.properties");
	}
	
	public void testComparison(){
		//Test case
		//Result: same: "X Y", diff: "B C Z"
		String S1 = "X B Y";
		String S2 = "X C X Y Z";
		String[][] expected = {{"X", "Y"},{"B", "C", "Z"}};
		
		MaxEntScorerPairExtractor extracter = new MaxEntScorerPairExtractor();
		String[][] test = extracter.compareStrings(S1,S2);
		
		boolean passed = true;
		for (int i = 0; i < test.length; ++i){
			String testString = Arrays.toString(test[i]);
			String expString = Arrays.toString(expected[i]);
			LOGGER.debug(S1 + " + " + S2 + " -> " +  testString + " (" + expString + ")");
			if (!testString.equals(expString)){
				passed = false;
				continue;
			}
		}
		assertTrue(passed);
		
	}
}