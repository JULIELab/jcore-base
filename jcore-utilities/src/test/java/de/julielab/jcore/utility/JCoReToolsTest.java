package de.julielab.jcore.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import com.google.common.collect.Lists;

import de.julielab.jcore.utility.JCoReTools;

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
}
