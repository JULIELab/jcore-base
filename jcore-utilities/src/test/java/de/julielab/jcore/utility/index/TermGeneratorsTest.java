package de.julielab.jcore.utility.index;

import static org.junit.Assert.*;

import java.util.function.BinaryOperator;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.utility.index.JCoReMapAnnotationIndex.IndexTermGenerator;

public class TermGeneratorsTest {
	
	private BinaryOperator<String> concatentor = (s1, s2) -> s1 + " " + s2;
	@Test
	public void testNGramTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("1234567");
		Annotation a = new Annotation(cas, 0, 7);
		IndexTermGenerator<String> unigramGenerator = TermGenerators.nGramTermGenerator(1);
		assertEquals("1 2 3 4 5 6 7", unigramGenerator.generateIndexTerms(a).reduce(concatentor).get());

		IndexTermGenerator<String> bigramGenerator = TermGenerators.nGramTermGenerator(2);
		assertEquals("12 23 34 45 56 67",
				bigramGenerator.generateIndexTerms(a).reduce(concatentor).get());

		IndexTermGenerator<String> trigramGenerator = TermGenerators.nGramTermGenerator(3);
		assertEquals("123 234 345 456 567",
				trigramGenerator.generateIndexTerms(a).reduce(concatentor).get());
	}
	
	@Test
	public void testEdgeNGramTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 9);
		IndexTermGenerator<String> unigramGenerator = TermGenerators.edgeNGramTermGenerator(5);
		assertEquals("1 12 123 1234 12345", unigramGenerator.generateIndexTerms(a).reduce(concatentor).get());
	}
	
	@Test
	public void testPrefixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertEquals("1", TermGenerators.prefixTermGenerator(1).generateIndexTerms(a).reduce(concatentor).get());
		assertEquals("1", TermGenerators.prefixTermGenerator(3).generateIndexTerms(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 9);
		assertEquals("12345", TermGenerators.prefixTermGenerator(5).generateIndexTerms(a).reduce(concatentor).get());
	}
	
	@Test
	public void testExactPrefixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertFalse(TermGenerators.exactPrefixTermGenerator(3).generateIndexTerms(a).reduce(concatentor).isPresent());
		assertEquals("1", TermGenerators.exactPrefixTermGenerator(1).generateIndexTerms(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 9);
		assertEquals("12345", TermGenerators.exactPrefixTermGenerator(5).generateIndexTerms(a).reduce(concatentor).get());
	}
	
	@Test
	public void testSuffixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertEquals("1", TermGenerators.suffixTermGenerator(1).generateIndexTerms(a).reduce(concatentor).get());
		assertEquals("1", TermGenerators.suffixTermGenerator(3).generateIndexTerms(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 11);
		assertEquals("34567", TermGenerators.suffixTermGenerator(5).generateIndexTerms(a).reduce(concatentor).get());
	}
	
	@Test
	public void testExactSuffixTermGenerator() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		cas.setDocumentText("tra 1234567");
		Annotation a = new Annotation(cas, 4, 5);
		assertFalse(TermGenerators.exactSuffixTermGenerator(3).generateIndexTerms(a).reduce(concatentor).isPresent());
		assertEquals("1", TermGenerators.exactSuffixTermGenerator(1).generateIndexTerms(a).reduce(concatentor).get());
		
		a = new Annotation(cas, 4, 11);
		assertEquals("34567", TermGenerators.exactSuffixTermGenerator(5).generateIndexTerms(a).reduce(concatentor).get());
	}
}
