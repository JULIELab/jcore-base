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
package de.julielab.jcore.utility;

import de.julielab.jcore.types.Token;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;


public class JCoReFSListIteratorTest {
	@Test
	public void testIterator() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		List<Token> tokenList = new ArrayList<>();
		Token t1 = new Token(jCas, 11, 17);
		Token t2 = new Token(jCas, 5, 10);
		Token t3 = new Token(jCas, 1, 20);
		Token t4 = new Token(jCas, 3, 5);
		tokenList.addAll(Arrays.asList(t1, t2, t3, t4));

		JCoReFSListIterator<Token> it = new JCoReFSListIterator<>(tokenList);
		Token token;
		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t1, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t2, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t3, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t4, token);

		assertFalse(it.hasNext());
	}

	@Test
	public void testRemove() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		List<Token> tokenList = new ArrayList<>();
		Token t1 = new Token(jCas, 11, 17);
		Token t2 = new Token(jCas, 5, 10);
		Token t3 = new Token(jCas, 1, 20);
		Token t4 = new Token(jCas, 3, 5);
		tokenList.addAll(Arrays.asList(t1, t2, t3, t4));

		Token token;
		JCoReFSListIterator<Token> it = new JCoReFSListIterator<>(tokenList);
		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t1, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t2, token);

		// Remove the second token; the third should move to its former position
		it.remove();
		assertTrue(it.isValid());
		assertEquals(t3, it.get());

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t4, token);

		// begin from the start and make sure the second token is gone indeed
		it.moveToFirst();

		it = new JCoReFSListIterator<>(tokenList);
		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t1, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t3, token);

		assertTrue(it.hasNext());
		token = (Token) it.next();
		assertEquals(t4, token);

		assertFalse(it.hasNext());
	}

	@Test
	public void testIteration() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		List<Token> tokenList = new ArrayList<>();
		Token t1 = new Token(jCas, 11, 17);
		Token t2 = new Token(jCas, 5, 10);
		Token t3 = new Token(jCas, 1, 20);
		Token t4 = new Token(jCas, 3, 5);
		tokenList.addAll(Arrays.asList(t1, t2, t3, t4));

		JCoReFSListIterator<Token> it = new JCoReFSListIterator<>(tokenList);
		int count = 0;
		while (it.hasNext()) {
			Token token = (Token) it.next();
			assertNotNull(token);
			++count;
		}
		assertEquals(4, count);
	}

	@Test
	public void testMove() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		List<Token> tokenList = new ArrayList<>();
		Token t1 = new Token(jCas, 11, 17);
		Token t2 = new Token(jCas, 5, 10);
		Token t3 = new Token(jCas, 1, 20);
		Token t4 = new Token(jCas, 3, 5);
		tokenList.addAll(Arrays.asList(t1, t2, t3, t4));

		JCoReFSListIterator<Token> it = new JCoReFSListIterator<>(tokenList);
		it.next();
		it.next();
		assertEquals(t2, it.get());

		it.moveToFirst();
		assertEquals(t1, it.get());

		it.moveToLast();
		assertEquals(t4, it.get());

		assertFalse(it.hasNext());

		it.moveToNext();
		boolean exceptionThrown = false;
		try {
			it.get();
		} catch (NoSuchElementException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);

		it.moveToPrevious();
		assertEquals(t4, it.get());

		it.moveToPrevious();
		assertEquals(t3, it.get());

		it.moveToFirst();
		assertEquals(t1, it.get());

		it.moveToPrevious();
		assertFalse(it.isValid());

		assertTrue(it.hasNext());

		// We interpret "next" as "at the next index"; if we move too far left,
		// the next element would still be invalid and thus, there is no 'next'
		// element
		it.moveToPrevious();
		assertFalse(it.hasNext());
	}
}
