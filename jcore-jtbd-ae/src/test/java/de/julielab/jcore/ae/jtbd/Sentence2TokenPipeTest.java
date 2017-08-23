/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.jtbd;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.jtbd.Sentence2TokenPipe;
import de.julielab.jcore.ae.jtbd.Unit;

public class Sentence2TokenPipeTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Sentence2TokenPipeTest.class);

	private static final String TEST_SENTENCE = "this is   a \t junit -test";

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
