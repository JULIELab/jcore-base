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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.Token;

public class JCoReOverlapAnnotationIndexTest {
	@Test
	public void testOverlapAnnotationIndex() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		Token t1 = new Token(jcas, 0, 3);
		Token t2 = new Token(jcas, 2, 8);
		Token t3 = new Token(jcas, 7, 10);
		Token t4 = new Token(jcas, 11, 18);
		Token t5 = new Token(jcas, 15, 21);
		Token t6 = new Token(jcas, 22, 27);
		t1.addToIndexes();
		t2.addToIndexes();
		t3.addToIndexes();
		t4.addToIndexes();
		t5.addToIndexes();
		t6.addToIndexes();

		JCoReOverlapAnnotationIndex<Token> index = new JCoReOverlapAnnotationIndex<>(jcas, Token.type);
		List<Token> result = index.search(t2).collect(Collectors.toList());
		assertTrue(result.contains(t1));
		assertTrue(result.contains(t2));
		assertTrue(result.contains(t3));
		assertEquals(3, result.size());
		
		result = index.search(t1).collect(Collectors.toList());
		assertTrue(result.contains(t1));
		assertTrue(result.contains(t2));
		assertEquals(2, result.size());
		
		result = index.search(t4).collect(Collectors.toList());
		assertTrue(result.contains(t4));
		assertTrue(result.contains(t5));
		assertEquals(2, result.size());
		
		result = index.search(t6).collect(Collectors.toList());
		assertTrue(result.contains(t6));
		assertEquals(1, result.size());
	}
}
