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
package de.julielab.jcore.reader.dta.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.reader.dta.DTAFileReaderTest;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;

public class DTAUtilsTest {

	@Test
	public void testHasAnyClassification() throws Exception {
		final JCas jcas = DTAFileReaderTest.process(true);
		assertTrue(DTAUtils.hasAnyClassification(jcas, DTABelletristik.class));
	}

	@Test
	public void testSlidingSymetricWindow() throws Exception {
		final JCas jcas = DTAFileReaderTest.process(true);
		final ArrayList<List<String>> expected = new ArrayList<>();
		expected.add(Arrays
				.asList("Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano ."
						.split(" ")));
		assertEquals(expected, DTAUtils.slidingSymetricWindow(jcas, 6));
		assertEquals(4, DTAUtils.slidingSymetricWindow(jcas, 5).size());
	}

}
