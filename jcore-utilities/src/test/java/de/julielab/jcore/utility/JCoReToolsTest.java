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
package de.julielab.jcore.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import com.google.common.collect.Lists;

import de.julielab.jcore.types.Token;

public class JCoReToolsTest {
	@Test
	public void testAddCollectionToFSArray1() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 2);
		Annotation anno = new Annotation(jCas);
		fsArray.set(0, anno);
		assertEquals(anno, fsArray.get(0));
		assertNull(fsArray.get(1));
		Annotation newElement = new Annotation(jCas);
		Collection<Annotation> newElements = Lists.newArrayList(newElement);
		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElements);
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(newElement, joinedArray.get(1));
	}

	@Test
	public void testAddCollectionToFSArray2() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 2);
		Annotation anno = new Annotation(jCas, 0, 0);
		fsArray.set(0, anno);
		assertEquals(anno, fsArray.get(0));
		assertNull(fsArray.get(1));
		Annotation newElement1 = new Annotation(jCas, 1, 1);
		Annotation newElement2 = new Annotation(jCas, 2, 2);
		Annotation newElement3 = new Annotation(jCas, 3, 3);
		Annotation newElement4 = new Annotation(jCas, 4, 4);
		Collection<Annotation> newElements = Lists.newArrayList(newElement1, newElement2, newElement3, newElement4);
		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElements);
		assertNotSame("The old FSArray was returned although a new one should have been created", fsArray, joinedArray);
		assertEquals(newElement1, joinedArray.get(1));
		assertEquals(newElement2, joinedArray.get(2));
		assertEquals(newElement3, joinedArray.get(3));
		assertEquals(newElement4, joinedArray.get(4));
	}

	@Test
	public void testAddCollectionToFSArray3() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 0);
		Annotation newElement = new Annotation(jCas);
		Collection<Annotation> newElements = Lists.newArrayList(newElement);
		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElements);
		assertNotSame("The old FSArray was returned although a new one should have been created", fsArray, joinedArray);
		assertEquals(newElement, joinedArray.get(0));
	}

	@Test
	public void testAddCollectionToFSArray4() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 1);
		Annotation newElement = new Annotation(jCas);
		Collection<Annotation> newElements = Lists.newArrayList(newElement);
		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElements);
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(newElement, joinedArray.get(0));
	}

	@Test
	public void testAddElementToFSArray1() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 2);
		Annotation anno = new Annotation(jCas);
		fsArray.set(0, anno);
		assertEquals(anno, fsArray.get(0));
		assertNull(fsArray.get(1));
		Annotation newElement = new Annotation(jCas);
		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElement);
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(newElement, joinedArray.get(1));
	}

	@Test
	public void testAddElementToFSArray2() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray fsArray = new FSArray(jCas, 2);
		Annotation anno = new Annotation(jCas, 0, 0);
		fsArray.set(0, anno);
		assertEquals(anno, fsArray.get(0));
		assertNull(fsArray.get(1));
		Annotation newElement1 = new Annotation(jCas, 1, 1);
		Annotation newElement2 = new Annotation(jCas, 2, 2);
		Annotation newElement3 = new Annotation(jCas, 3, 3);
		Annotation newElement4 = new Annotation(jCas, 4, 4);
		List<Annotation> newElements = Lists.newArrayList(newElement1, newElement2, newElement3, newElement4);

		FSArray joinedArray = JCoReTools.addToFSArray(fsArray, newElements.get(0));
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(2, joinedArray.size());
		assertEquals(newElement1, joinedArray.get(1));
		fsArray = joinedArray;

		joinedArray = JCoReTools.addToFSArray(fsArray, newElements.get(1));
		assertNotSame("The old FSArray was returned although a new one should have been created", fsArray, joinedArray);
		assertEquals(newElement2, joinedArray.get(2));
		fsArray = joinedArray;

		joinedArray = JCoReTools.addToFSArray(fsArray, newElements.get(2));
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(newElement3, joinedArray.get(3));
		fsArray = joinedArray;

		joinedArray = JCoReTools.addToFSArray(fsArray, newElements.get(3));
		assertEquals("A new FSArray was instantiated although the old one should have been kept", fsArray, joinedArray);
		assertEquals(newElement4, joinedArray.get(4));

	}

	@Test
	public void testAddElementToNullFSArray() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		FSArray array = JCoReTools.addToFSArray(null, new Annotation(jCas, 1, 1));
		assertEquals(1, array.size());
	}

	@Test
	public void testBinarySearch() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("token1 token2 token3 token4");
		Token t1 = new Token(cas, 0, 6);
		Token t2 = new Token(cas, 7, 13);
		Token t3 = new Token(cas, 14, 20);
		Token t4 = new Token(cas, 21, 27);
		List<Token> list = Arrays.asList(t1, t2, t3, t4);

		// finding existing elements
		int pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 0);
		assertEquals(0, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 7);
		assertEquals(1, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 14);
		assertEquals(2, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 21);
		assertEquals(3, pos);

		// searching for non-existent elements
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), -1);
		assertEquals(-1, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 5);
		assertEquals(-2, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 10);
		assertEquals(-3, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 15);
		assertEquals(-4, pos);
		pos = JCoReTools.binarySearch(list, t -> t.getBegin(), 25);
		assertEquals(-5, pos);

	}
}
