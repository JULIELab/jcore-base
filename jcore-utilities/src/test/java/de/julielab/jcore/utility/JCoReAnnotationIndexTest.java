package de.julielab.jcore.utility;

import static org.junit.Assert.assertEquals;

import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationIndex.IndexTermGenerator;

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
		Supplier<TreeSet<Token>> supplier = () -> new TreeSet<>((o1, o2) -> {

			if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
				return 0;
			if (o1.getBegin() == o2.getBegin())
				return o1.getEnd() - o2.getEnd();
			return o1.getBegin() - o2.getBegin();
		});
		Supplier<TreeSet<Token>> s2 = supplier;
		JCoReAnnotationIndex<Token, String, TreeSet<Token>, TreeSet<Token>> index = new JCoReAnnotationIndex<>(g, g, s2,
				supplier);
		index.index(cas, Token.type);

		TreeSet<Token> search = index.search(e1);
		assertEquals(1, search.size());
		assertEquals("token2", search.iterator().next().getCoveredText());

		search = index.search("token4");
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

		JCoReAnnotationIndex.IndexTermGenerator<String> g = JCoReAnnotationIndex.TermGenerators.nGramTermGenerator(2);
		JCoReAnnotationIndex.IndexTermGenerator<String> sg = JCoReAnnotationIndex.TermGenerators.suffixTermGenerator(2);
		Supplier<TreeSet<Token>> supplier = () -> new TreeSet<>((o1, o2) -> {

			if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
				return 0;
			if (o1.getBegin() == o2.getBegin())
				return o1.getEnd() - o2.getEnd();
			return o1.getBegin() - o2.getBegin();
		});
		Supplier<TreeSet<Token>> s2 = supplier;
		JCoReAnnotationIndex<Token, String, TreeSet<Token>, TreeSet<Token>> index = new JCoReAnnotationIndex<>(g, sg, s2,
				supplier);
		index.index(cas, Token.type);

		TreeSet<Token> search = index.search(e1);
		assertEquals(1, search.size());
		assertEquals("token2", search.iterator().next().getCoveredText());

		search = index.search("n4");
		assertEquals(1, search.size());
		assertEquals("token4", search.iterator().next().getCoveredText());
	}

	@Test
	public void testNGramTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("1234567");
		Annotation a = new Annotation(cas, 0, 7);
		IndexTermGenerator<String> unigramGenerator = JCoReAnnotationIndex.TermGenerators.nGramTermGenerator(1);
		assertEquals("1 2 3 4 5 6 7", unigramGenerator.generateIndexTerms(a).reduce((s1, s2) -> s1 + " " + s2).get());

		IndexTermGenerator<String> bigramGenerator = JCoReAnnotationIndex.TermGenerators.nGramTermGenerator(2);
		assertEquals("12 23 34 45 56 67",
				bigramGenerator.generateIndexTerms(a).reduce((s1, s2) -> s1 + " " + s2).get());

		IndexTermGenerator<String> trigramGenerator = JCoReAnnotationIndex.TermGenerators.nGramTermGenerator(3);
		assertEquals("123 234 345 456 567",
				trigramGenerator.generateIndexTerms(a).reduce((s1, s2) -> s1 + " " + s2).get());
	}
}
