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
import static org.junit.Assert.assertFalse;

import java.util.function.BinaryOperator;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

public class TermGeneratorsTest {
	
	private BinaryOperator<String> concatentor = (s1, s2) -> s1 + " " + s2;
	@Test
	public void testNGramTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("1234567");
		Annotation a = new Annotation(cas, 0, 7);
		IndexTermGenerator<String> unigramGenerator = TermGenerators.nGramTermGenerator(1);
		assertEquals("1 2 3 4 5 6 7", unigramGenerator.asStream(a).reduce(concatentor).get());

		IndexTermGenerator<String> bigramGenerator = TermGenerators.nGramTermGenerator(2);
		assertEquals("12 23 34 45 56 67",
				bigramGenerator.asStream(a).reduce(concatentor).get());

		IndexTermGenerator<String> trigramGenerator = TermGenerators.nGramTermGenerator(3);
		assertEquals("123 234 345 456 567",
				trigramGenerator.asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testEdgeNGramTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 9);
		IndexTermGenerator<String> unigramGenerator = TermGenerators.edgeNGramTermGenerator(5);
		assertEquals("1 12 123 1234 12345", unigramGenerator.asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testPrefixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertEquals("1", TermGenerators.prefixTermGenerator(1).asStream(a).reduce(concatentor).get());
		assertEquals("1", TermGenerators.prefixTermGenerator(3).asStream(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 9);
		assertEquals("12345", TermGenerators.prefixTermGenerator(5).asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testExactPrefixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertFalse(TermGenerators.exactPrefixTermGenerator(3).asStream(a).reduce(concatentor).isPresent());
		assertEquals("1", TermGenerators.exactPrefixTermGenerator(1).asStream(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 9);
		assertEquals("12345", TermGenerators.exactPrefixTermGenerator(5).asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testSuffixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertEquals("1", TermGenerators.suffixTermGenerator(1).asStream(a).reduce(concatentor).get());
		assertEquals("1", TermGenerators.suffixTermGenerator(3).asStream(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 11);
		assertEquals("34567", TermGenerators.suffixTermGenerator(5).asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testExactSuffixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertFalse(TermGenerators.exactSuffixTermGenerator(3).asStream(a).reduce(concatentor).isPresent());
		assertEquals("1", TermGenerators.exactSuffixTermGenerator(1).asStream(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 11);
		assertEquals("34567", TermGenerators.exactSuffixTermGenerator(5).asStream(a).reduce(concatentor).get());
	}
	
	@Test
	public void testLongOffsetTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		Annotation a = new Annotation(cas, 1234, 9876);
		Long offsets = TermGenerators.longOffsetTermGenerator().asKey(a);
		assertEquals(1234, (int)(offsets >> 32));
		assertEquals(9876, (int)(offsets >> 0));
	}
}
