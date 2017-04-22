package de.julielab.jcore.utility;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import com.google.common.base.Functions;

import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.Token;

public class JCoReAnnotationIndexTest {
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

		JCoReAnnotationIndex.IndexTermGenerator<String> g = t -> Stream.of(t.getCoveredText());
		JCoReAnnotationIndex<Token, String> index = new JCoReAnnotationIndex<>(g, g, (o1, o2) -> {

			if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
				return 0;
			if (o1.getBegin() == o2.getBegin())
				return o1.getEnd() - o2.getEnd();
			return o1.getBegin() - o2.getBegin();
		});
		index.index(cas, Token.type);

		TreeSet<Token> search = index.search(e1);
		System.out.println(search);
	}
}
