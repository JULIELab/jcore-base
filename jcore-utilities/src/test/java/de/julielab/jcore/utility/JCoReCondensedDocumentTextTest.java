package de.julielab.jcore.utility;

import de.julielab.jcore.types.InternalReference;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JCoReCondensedDocumentTextTest {
	@Test
	public void testReduce() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("This sentence1 has references.2");
		InternalReference ref1 = new InternalReference(jcas, 13, 14);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 30, 31);
		ref2.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("This sentence has references.", condensedText.getCodensedText());
		assertEquals(0, condensedText.getOriginalOffsetForCondensedOffset(0));
		assertEquals(13, condensedText.getOriginalOffsetForCondensedOffset(13));
		assertEquals(15, condensedText.getOriginalOffsetForCondensedOffset(14));
		assertEquals(30, condensedText.getOriginalOffsetForCondensedOffset(29));
		
		assertEquals(0, condensedText.getCondensedOffsetForOriginalOffset(0));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(13));
		assertEquals(14, condensedText.getCondensedOffsetForOriginalOffset(15));
		assertEquals(29, condensedText.getCondensedOffsetForOriginalOffset(30));
		assertEquals(29, condensedText.getCondensedOffsetForOriginalOffset(31));
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

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("This sentence has references.", condensedText.getCodensedText());
		assertEquals(0, condensedText.getOriginalOffsetForCondensedOffset(0));
		assertEquals(13, condensedText.getOriginalOffsetForCondensedOffset(13));
		assertEquals(15, condensedText.getOriginalOffsetForCondensedOffset(14));
		assertEquals(31, condensedText.getOriginalOffsetForCondensedOffset(29));
		
		assertEquals(0, condensedText.getCondensedOffsetForOriginalOffset(0));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(13));
		assertEquals(14, condensedText.getCondensedOffsetForOriginalOffset(15));
		assertEquals(28, condensedText.getCondensedOffsetForOriginalOffset(30));
		assertEquals(29, condensedText.getCondensedOffsetForOriginalOffset(31));
	}

	@Test
	public void testReduce3() throws Exception {
		// Here we also add commas as cut away characters, offering the possibility to remove enumerations of
		// references completely.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("This sentence has multiple references.2,5;42 This is a second sentence.7,8");
		InternalReference ref1 = new InternalReference(jcas, 38, 39);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 40, 41);
		ref2.addToIndexes();
		InternalReference ref3 = new InternalReference(jcas, 42, 44);
		ref3.addToIndexes();
		InternalReference ref4 = new InternalReference(jcas, 71, 72);
		ref4.addToIndexes();
		InternalReference ref5 = new InternalReference(jcas, 73, 74);
		ref5.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())), Set.of(',', ';'));
		assertEquals("This sentence has multiple references. This is a second sentence.", condensedText.getCodensedText());
	}

	@Test
	public void testErrorDoc() throws Exception{
		// The XMI document uses here is from PMC and is an example of a source of error the previously occurred.
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
				"de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");

		XmiCasDeserializer.deserialize(new FileInputStream(Path.of("src", "test", "resources", "PMC5478802.xmi").toFile()), jCas.getCas());
		JCoReCondensedDocumentText text = new JCoReCondensedDocumentText(jCas, Set.of(de.julielab.jcore.types.pubmed.InternalReference.class.getCanonicalName()), Set.of(','));
		Set<String> sentenceBoundaryTypes = Set.of("de.julielab.jcore.types.Title", "de.julielab.jcore.types.AbstractText", "de.julielab.jcore.types.AbstractSectionHeading", "de.julielab.jcore.types.AbstractSection", "de.julielab.jcore.types.Section", "de.julielab.jcore.types.Paragraph", "de.julielab.jcore.types.Zone", "de.julielab.jcore.types.Caption", "de.julielab.jcore.types.Figure", "de.julielab.jcore.types.Table");
//		Set<String> sentenceBoundaryTypes = Set.of("de.julielab.jcore.types.Section");
		JCoReAnnotationIndexMerger indexMerger = new JCoReAnnotationIndexMerger(sentenceBoundaryTypes, false,
				null, jCas);

		while (indexMerger.incrementAnnotation()) {
			Annotation a = (Annotation) indexMerger.getAnnotation();
			System.out.println(a.getCoveredText());
			System.out.println("--");
			int begin = a.getBegin();
			int condensedBegin = text.getCondensedOffsetForOriginalOffset(begin);
			int end = a.getEnd();
			int condensedEnd = text.getCondensedOffsetForOriginalOffset(end);
			if (condensedEnd > text.getCodensedText().length())
				System.out.println();
			System.out.println(text.getCodensedText().substring(condensedBegin, condensedEnd));
			System.out.println(begin + " - " + end + ", " + condensedBegin + " - " + condensedEnd);
			System.out.println();
		}
	}
}
