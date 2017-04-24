package de.julielab.jcore.utility.index;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.NavigableSet;

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

		// this entity overlaps token1 and token2
		Entity e1 = new Entity(cas, 3, 10);

		JCoReSetAnnotationIndex<Annotation> index = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator());
		index.index(cas, Token.type);

		NavigableSet<Annotation> search = index.search(e1);
		System.out.println(search);
		assertEquals(2, search.size());
		assertTrue(search.contains(t1));
		assertTrue(search.contains(t2));
	}
}
