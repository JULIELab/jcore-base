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
package de.julielab.jcore.utility.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

public class ComparatorsTest {
	@Test
	public void testLongOverlapComparator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		Annotation a = new Annotation(cas, 1234, 9876);
		
		// overlapping to the end
		long l = (5000l << 32) | 15000;
		assertEquals(0, Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));
		
		// overlapping to the beginning
		l = (50l << 32) | 5000;
		assertEquals(0, Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));
		
		// embedded
		l = (2000l << 32) | 5000;
		assertEquals(0, Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));
		
		// covering
		l = (50l << 32) | 15000;
		assertEquals(0, Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));
		
		// ends before
		l = (10l << 32) | 17;
		assertTrue(0 > Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));

		// starts after
		l = (10000l << 32) | 15000;
		assertTrue(0 < Comparators.longOverlapComparator().compare(l, TermGenerators.longOffsetTermGenerator().asKey(a)));
	}
}
