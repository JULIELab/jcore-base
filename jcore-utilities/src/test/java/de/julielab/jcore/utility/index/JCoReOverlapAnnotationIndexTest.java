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

import de.julielab.jcore.types.Token;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		List<Token> result = index.search(t2);
		assertTrue(result.contains(t1));
		assertTrue(result.contains(t2));
		assertTrue(result.contains(t3));
		assertEquals(3, result.size());
		
		result = index.search(t1);
		assertTrue(result.contains(t1));
		assertTrue(result.contains(t2));
		assertEquals(2, result.size());
		
		result = index.search(t4);
		assertTrue(result.contains(t4));
		assertTrue(result.contains(t5));
		assertEquals(2, result.size());
		
		result = index.search(t6);
		assertTrue(result.contains(t6));
		assertEquals(1, result.size());
	}
}
