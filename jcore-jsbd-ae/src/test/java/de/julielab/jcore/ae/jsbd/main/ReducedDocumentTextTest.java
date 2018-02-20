package de.julielab.jcore.ae.jsbd.main;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.InternalReference;

public class ReducedDocumentTextTest {
	@Test
	public void testReduce() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("This sentence1 has references.2");
		InternalReference ref1 = new InternalReference(jcas, 13, 14);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 30, 31);
		ref2.addToIndexes();

		ReducedDocumentText reducedText = new ReducedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("This sentence has references.", reducedText.getReducedText());
		assertEquals(0, reducedText.getOriginalOffsetForReducedOffset(0));
		assertEquals(13, reducedText.getOriginalOffsetForReducedOffset(13));
		assertEquals(15, reducedText.getOriginalOffsetForReducedOffset(14));
		assertEquals(30, reducedText.getOriginalOffsetForReducedOffset(29));
	}
	@Test
	public void testReduce2() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("This sentence1 has references2.");
		InternalReference ref1 = new InternalReference(jcas, 13, 14);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 29, 30);
		ref2.addToIndexes();

		ReducedDocumentText reducedText = new ReducedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("This sentence has references.", reducedText.getReducedText());
		assertEquals(0, reducedText.getOriginalOffsetForReducedOffset(0));
		assertEquals(13, reducedText.getOriginalOffsetForReducedOffset(13));
		assertEquals(15, reducedText.getOriginalOffsetForReducedOffset(14));
		assertEquals(31, reducedText.getOriginalOffsetForReducedOffset(29));
	}
}
