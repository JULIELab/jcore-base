package de.julielab.jcore.utility;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.InternalReference;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

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
	public void testReduce4() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("This sentence\n1\nhas references.");
		InternalReference ref1 = new InternalReference(jcas, 14, 15);
		ref1.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("This sentence\nhas references.", condensedText.getCodensedText());
		assertEquals(0, condensedText.getOriginalOffsetForCondensedOffset(0));
		assertEquals(16, condensedText.getOriginalOffsetForCondensedOffset(14));
	}

	@Test
	public void testReduce5() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Leptin is an adipose-derived protein secreted by adipocytes and is expressed in adipose tissue.\n" +
				"1\n" +
				"It has the role of being a key regulator of several physiological pathways including body weight and regulation of food intake, inflammation, endocrine function, energy homeostasis, bone metabolism and immunity.\n" +
				"2\n" +
				"3\n" +
				"Results from various studies indicate that leptin may play a significant role in bone physiology, independent of the central nervous system.\n");
		InternalReference ref1 = new InternalReference(jcas, 96, 97);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 310, 311);
		ref2.addToIndexes();
		InternalReference ref3 = new InternalReference(jcas, 312, 313);
		ref3.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())));
		assertEquals("Leptin is an adipose-derived protein secreted by adipocytes and is expressed in adipose tissue.\n" +
				"It has the role of being a key regulator of several physiological pathways including body weight and regulation of food intake, inflammation, endocrine function, energy homeostasis, bone metabolism and immunity.\n" +
				"Results from various studies indicate that leptin may play a significant role in bone physiology, independent of the central nervous system.\n", condensedText.getCodensedText());
		assertEquals(98, condensedText.getOriginalOffsetForCondensedOffset(96));
		assertEquals(314, condensedText.getOriginalOffsetForCondensedOffset(308));
	}

	@Test
	public void testReduce6() throws Exception {
		// Test the option to skip internal references that have letters from omission from the condensed text.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Andreeva et al.19 and Xiao et al.20 studied the way of binding of a myosin head to an actin filament by using tryptic digestion of myofibrils and measuring optical polarization and dichroism. They concluded that in the rigor rabbit psoas muscle each myosin head binds to two actin monomers in a thin filament20, suggesting the possibility that the myosin head may first bind to one and then to two monomers in the actin filament19.\n" +
				"Figure 2 shows an example of possible mechanism of how such binding change occurs.");
		InternalReference ref1 = new InternalReference(jcas, 15, 17);
		ref1.addToIndexes();
		InternalReference ref2 = new InternalReference(jcas, 33, 35);
		ref2.addToIndexes();
		InternalReference ref3 = new InternalReference(jcas, 308, 310);
		ref3.addToIndexes();
		InternalReference ref4 = new InternalReference(jcas, 428, 430);
		ref4.addToIndexes();
		InternalReference ref5 = new InternalReference(jcas, 432, 440);
		ref5.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(InternalReference.class.getCanonicalName())), true);
		assertEquals("Andreeva et al. and Xiao et al. studied the way of binding of a myosin head to an actin filament by using tryptic digestion of myofibrils and measuring optical polarization and dichroism. They concluded that in the rigor rabbit psoas muscle each myosin head binds to two actin monomers in a thin filament, suggesting the possibility that the myosin head may first bind to one and then to two monomers in the actin filament.\n" +
				"Figure 2 shows an example of possible mechanism of how such binding change occurs.", condensedText.getCodensedText());
	}


	@Test
	public void testCondensedOffsetsWithinCutawayAnnotations() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Not cut away 1. Cut away 1. Not cut away 2. Cut away 2. Not cut away 3.");
		Annotation cutAwayAnnotation = new Annotation(jcas, 16, 27);
		cutAwayAnnotation.addToIndexes();
		Annotation cutAwayAnnotation2 = new Annotation(jcas, 44, 55);
		cutAwayAnnotation2.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(Annotation.class.getCanonicalName())));
		assertEquals("Not cut away 1. Not cut away 2. Not cut away 3.", condensedText.getCodensedText());
		assertEquals(10, condensedText.getCondensedOffsetForOriginalOffset(10));
		assertEquals(15, condensedText.getCondensedOffsetForOriginalOffset(15));
		assertEquals(16, condensedText.getCondensedOffsetForOriginalOffset(16));
		assertEquals(16, condensedText.getCondensedOffsetForOriginalOffset(17));
		assertEquals(16, condensedText.getCondensedOffsetForOriginalOffset(27));
		assertEquals(19, condensedText.getCondensedOffsetForOriginalOffset(31));
	}

	@Test
	public void testCutAwayAtBeginning() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Cut away. Not cut away.");
		Annotation cutAwayAnnotation = new Annotation(jcas, 0, 9);
		cutAwayAnnotation.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(Annotation.class.getCanonicalName())));
		assertEquals("Not cut away.", condensedText.getCodensedText());
		assertEquals(0, condensedText.getCondensedOffsetForOriginalOffset(3));
		assertEquals(3, condensedText.getCondensedOffsetForOriginalOffset(13));
	}

	@Test
	public void testCutAwayAtEnd() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Not cut away. Cut away.");
		Annotation cutAwayAnnotation = new Annotation(jcas, 14, 23);
		cutAwayAnnotation.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(Annotation.class.getCanonicalName())));
		assertEquals("Not cut away.", condensedText.getCodensedText());
		assertEquals(10, condensedText.getCondensedOffsetForOriginalOffset(10));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(16));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(23));
	}

	@Test
	public void testEmbeddedCutAway() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Not cut away. Cut away. Not cut away.");
		Annotation cutAwayAnnotation = new Annotation(jcas, 14, 23);
		cutAwayAnnotation.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(Annotation.class.getCanonicalName())));
		assertEquals("Not cut away. Not cut away.", condensedText.getCodensedText());
		assertEquals(10, condensedText.getCondensedOffsetForOriginalOffset(10));
		assertEquals(14, condensedText.getCondensedOffsetForOriginalOffset(16));
		assertEquals(14, condensedText.getCondensedOffsetForOriginalOffset(23));
		assertEquals(15, condensedText.getCondensedOffsetForOriginalOffset(25));
	}

	@Test
	public void testEnclosingCutAway() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		jcas.setDocumentText("Cut away. Not cut away. Cut away.");
		Annotation cutAwayAnnotation = new Annotation(jcas, 0, 9);
		cutAwayAnnotation.addToIndexes();
		Annotation cutAwayAnnotation2 = new Annotation(jcas, 24, 33);
		cutAwayAnnotation2.addToIndexes();

		JCoReCondensedDocumentText condensedText = new JCoReCondensedDocumentText(jcas,
				new HashSet<>(Arrays.asList(Annotation.class.getCanonicalName())));
		assertEquals("Not cut away.", condensedText.getCodensedText());
		assertEquals(0, condensedText.getCondensedOffsetForOriginalOffset(10));
		assertEquals(3, condensedText.getCondensedOffsetForOriginalOffset(13));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(27));
		assertEquals(13, condensedText.getCondensedOffsetForOriginalOffset(33));
	}
}
