/**
 * Sentence2TokenPipeTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: //TODO insert current version number Since version: //TODO
 * insert version number of first appearance of this class
 *
 * Creation date: Sep 6, 2007
 *
 * //TODO insert short description
 **/

package de.julielab.jcore.ae.jtbd;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Sentence2TokenPipeTest  {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Sentence2TokenPipeTest.class);

	private static final String TEST_SENTENCE = "this is   a \t junit -test";

	@Test
	public void testMakeLabel() {
		final ArrayList<String> expectedLabels = new ArrayList<String>();
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("P");
		expectedLabels.add("N");
		expectedLabels.add("N");

		final Sentence2TokenPipe p = new Sentence2TokenPipe();
		final ArrayList<String> testLabels = p.makeLabels(TEST_SENTENCE);

		LOGGER.debug("testMakeLabel() - expected labels: " + expectedLabels);
		LOGGER.debug("testMakeLabel() - created labels: " + testLabels);

		// check labels
		boolean allOK = true;
		for (int i = 0; i < testLabels.size(); i++)
			if (!testLabels.get(i).equals(expectedLabels.get(i))) {
				allOK = false;
				break;
			}
		assertTrue(allOK);
	}

	@Test
	public void testMakeUnits() {
		final ArrayList<String> expectedUnits = new ArrayList<String>();
		expectedUnits.add("this");
		expectedUnits.add("is");
		expectedUnits.add("a");
		expectedUnits.add("junit");
		expectedUnits.add("-");
		expectedUnits.add("test");

		final Sentence2TokenPipe p = new Sentence2TokenPipe();

		final ArrayList<Unit> testUnits = new ArrayList<Unit>();
		final ArrayList<String> wSpaces = new ArrayList<String>();
		p.makeUnits(TEST_SENTENCE, testUnits, wSpaces);

		// now check whether expected units are created
		LOGGER.debug("testMakeUnits() - excepted units: "
				+ expectedUnits.toString());
		LOGGER.debug("testMakeUnits() - created units: " + testUnits.toString());
		boolean allOK = true;
		for (int i = 0; i < testUnits.size(); i++)
			if (!testUnits.get(i).rep.equals(expectedUnits.get(i))) {
				allOK = false;
				break;
			}

		assertTrue(allOK);

	}
}
