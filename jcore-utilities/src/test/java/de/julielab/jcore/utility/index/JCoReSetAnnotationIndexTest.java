package de.julielab.jcore.utility.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.Token;

public class JCoReSetAnnotationIndexTest {
	@Test
	public void testStringIndex() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("token1 token2 token3 token4");
		Token t1 = new Token(cas, 0, 6);
		Token t2 = new Token(cas, 7, 13);
		Token t3 = new Token(cas, 14, 20);
		Token t4 = new Token(cas, 21, 27);
		t1.addToIndexes();
		t2.addToIndexes();
		t3.addToIndexes();
		t4.addToIndexes();


		JCoReSetAnnotationIndex<Annotation> index = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator());
		index.index(cas, Token.type);

		// this entity overlaps token1 and token2
		Entity e1 = new Entity(cas, 3, 10);
		Set<Annotation> search = index.search(e1).collect(Collectors.toSet());
		assertEquals(2, search.size());
		assertTrue(search.contains(t1));
		assertTrue(search.contains(t2));
		
		// embedded in token3
		e1 = new Entity(cas, 15, 19);
		search = index.search(e1).collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertTrue(search.contains(t3));
		
		// not overlapping any token
		e1 = new Entity(cas, 30, 35);
		search = index.search(e1).collect(Collectors.toSet());
		assertEquals(0, search.size());
		
		// not overlapping negatively (just for the sake of the test)
		e1 = new Entity(cas, -5, -3);
		search = index.search(e1).collect(Collectors.toSet());
		assertEquals(0, search.size());
		
		
		// covering all tokens
		e1 = new Entity(cas, 0, 27);
		search = index.search(e1).collect(Collectors.toSet());
		assertEquals(4, search.size());
	}
}
