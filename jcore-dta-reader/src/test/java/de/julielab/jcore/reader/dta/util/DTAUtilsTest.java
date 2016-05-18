package de.julielab.jcore.reader.dta.util;

import static org.junit.Assert.*;

import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.reader.dta.DTAFileReaderTest;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;

public class DTAUtilsTest {

	@Test
	public void test() throws Exception {
		JCas jcas = DTAFileReaderTest.process(true);
		assertTrue(DTAUtils.hasAnyClassification(jcas, DTABelletristik.class));
	}

}
