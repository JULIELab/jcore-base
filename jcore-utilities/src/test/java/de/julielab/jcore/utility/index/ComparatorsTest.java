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
