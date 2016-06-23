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
