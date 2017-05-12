package de.julielab.jcore.utility.index;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.Token;

public class JCoReTreeMapAnnotationIndexTest {
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

		JCoReTreeMapAnnotationIndex<Long, Token> index = new JCoReTreeMapAnnotationIndex<>(
				Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(),
				TermGenerators.longOffsetTermGenerator());
		index.index(cas, Token.type);

		Set<Token> search = index.search(e1).collect(Collectors.toSet());
		assertEquals(1, search.size());
		assertEquals("token2", search.iterator().next().getCoveredText());

		search = index.searchFuzzy((0l << 32) | 27).collect(Collectors.toSet());
		assertEquals(4, search.size());
		
		search = index.searchFuzzy((10l << 32) | 17).collect(Collectors.toSet());
		assertEquals(2, search.size());
	}
}
