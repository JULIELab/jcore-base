package de.julielab.jcore.utility.index;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.Token;

public class JCoReMapAnnotationIndexTest {
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

		Entity e1 = new Entity(cas, 7, 13);

		JCoReMapAnnotationIndex.IndexTermGenerator<String> g = t -> Stream.of(t.getCoveredText());
		JCoReMapAnnotationIndex<Token, String> index = new JCoReMapAnnotationIndex<>(TreeMap::new, g, g);
		index.index(cas, Token.type);

		Set<Token> search = index.search(e1).collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertEquals("token2", search.iterator().next().getCoveredText());

		search = index.search("token4").collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertEquals("token4", search.iterator().next().getCoveredText());
	}
	
	@Test
	public void testStringIndex2() throws Exception {
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

		Entity e1 = new Entity(cas, 7, 13);

		JCoReMapAnnotationIndex.IndexTermGenerator<String> g = TermGenerators.nGramTermGenerator(2);
		JCoReMapAnnotationIndex.IndexTermGenerator<String> sg = TermGenerators.exactSuffixTermGenerator(2);
		JCoReMapAnnotationIndex<Token, String> index = new JCoReMapAnnotationIndex<>(HashMap::new, g, sg);
		index.index(cas, Token.type);

		Set<Token> search = index.search(e1).collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertEquals("token2", search.iterator().next().getCoveredText());

		search = index.search("n4").collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertEquals("token4", search.iterator().next().getCoveredText());
	}

}
