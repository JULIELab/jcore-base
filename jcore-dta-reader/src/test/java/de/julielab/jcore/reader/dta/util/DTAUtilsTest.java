/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.dta.util;

import de.julielab.jcore.reader.dta.DTAFileReaderTest;
import de.julielab.jcore.reader.dta.DTAFileReaderTest.Version;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DTAUtilsTest {

	@Test
	public void testHasAnyClassification() throws Exception {
		for (Version version : Version.values()) {
			final JCas jcas = DTAFileReaderTest.process(true, version);
			assertTrue(
					DTAUtils.hasAnyClassification(jcas, DTABelletristik.class));
		}
	}

	@Test
	public void testSlidingSymetricWindow() throws Exception {
		final ArrayList<List<String>> expected = new ArrayList<>();
		expected.add(Arrays
				.asList("Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano ."
						.split(" ")));
		for (Version version : Version.values()) {
			JCas jcas = DTAFileReaderTest.process(true, version);
			assertEquals(expected, DTAUtils.slidingSymetricWindow(jcas, 6));
			assertEquals(4, DTAUtils.slidingSymetricWindow(jcas, 5).size());
		}
	}

}
