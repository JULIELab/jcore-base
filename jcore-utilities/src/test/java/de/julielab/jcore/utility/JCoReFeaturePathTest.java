package de.julielab.jcore.utility;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jules.types.ArgumentMention;
import de.julielab.jules.types.AuthorInfo;
import de.julielab.jules.types.ConceptMention;
import de.julielab.jules.types.Gene;
import de.julielab.jules.types.Header;
import de.julielab.jules.types.Lemma;
import de.julielab.jules.types.OntClassMention;
import de.julielab.jules.types.ResourceEntry;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

public class JCoReFeaturePathTest {
	@Test
	public void testTypeInit() throws Exception {
		// Here, only the feature path parsing is tested. No actual feature
		// values are retrieved from annotations.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		TypeSystem ts = jcas.getTypeSystem();
		Type geneType = ts.getType("de.julielab.jules.types.Gene");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList[0]/entryId");
		fp.typeInit(geneType);

		// The path string should be broken down to single elements in an array.
		// The leading slash should be removed
		// before (else we would have a first empty element).
		String[] internalFeaturePath = (String[]) FieldUtils.readField(fp, "featurePath", true);
		assertArrayEquals(new String[] { "resourceEntryList[0]", "entryId" }, internalFeaturePath);

		// Check whether the (recursive) extracted features are the correct
		// ones.
		Feature[] internalFeatures = (Feature[]) FieldUtils.readField(fp, "features", true);
		Type resourceEntryType = ts.getType("de.julielab.jules.types.ResourceEntry");
		assertArrayEquals(new Feature[] { geneType.getFeatureByBaseName("resourceEntryList"), resourceEntryType.getFeatureByBaseName("entryId") },
				internalFeatures);

		// The first feature path element should have an index of 0, the second
		// none, i.e. -1 to indicate that.
		int[] arrayIndexes = (int[]) FieldUtils.readField(fp, "arrayIndexes", true);
		assertArrayEquals(new int[] { 0, Integer.MIN_VALUE }, arrayIndexes);
	}

	@Test
	public void testGetValueAsString() throws Exception {
		// Here we test whether the correct value is retrieved via the feature
		// path.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList[0]/entryId");
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().

		Gene gene = new Gene(jcas);
		ResourceEntry resourceEntry = new ResourceEntry(jcas);
		resourceEntry.setEntryId("EntryId1");
		FSArray resourceEntryArray = new FSArray(jcas, 1);
		resourceEntryArray.set(0, resourceEntry);
		gene.setResourceEntryList(resourceEntryArray);

		assertEquals(resourceEntry.getEntryId(), fp.getValueAsString(gene));

		Token token = new Token(jcas);
		Lemma lemma = new Lemma(jcas);
		lemma.setValue("lemmaValue");
		token.setLemma(lemma);
		JCoReFeaturePath fp2 = new JCoReFeaturePath();
		fp2.initialize("/lemma/value");

		assertEquals(lemma.getValue(), fp2.getValueAsString(token));
	}

	@Test
	public void testGetValueAsStringOnValueArrayWithIndex() throws Exception {
		// Here we test whether the correct value is retrieved via the feature
		// path when the end of the path is an
		// array and we provide a particular array index to retrieve the value
		// from.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/semanticTypes[0]");
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().

		OntClassMention ontClass = new OntClassMention(jcas);
		StringArray semTypes = new StringArray(jcas, 2);
		semTypes.set(0, "type1");
		semTypes.set(1, "type2");
		ontClass.setSemanticTypes(semTypes);

		assertEquals(semTypes.get(0), fp.getValueAsString(ontClass));

		JCoReFeaturePath fp2 = new JCoReFeaturePath();
		fp2.initialize("/semanticTypes[1]");

		assertEquals(semTypes.get(1), fp2.getValueAsString(ontClass));
	}

	@Test
	public void testGetValueAsStringOnValueArrayWithReverseIndex() throws Exception {
		// Here we test whether the correct value is retrieved via the feature
		// path when the end of the path is an
		// array and we provide a particular array index to retrieve the value
		// from.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/semanticTypes[-1]");
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().

		OntClassMention ontClass = new OntClassMention(jcas);
		StringArray semTypes = new StringArray(jcas, 2);
		semTypes.set(0, "type1");
		semTypes.set(1, "type2");
		ontClass.setSemanticTypes(semTypes);

		assertEquals(semTypes.get(1), fp.getValueAsString(ontClass));

		JCoReFeaturePath fp2 = new JCoReFeaturePath();
		fp2.initialize("/semanticTypes[-2]");

		assertEquals(semTypes.get(0), fp2.getValueAsString(ontClass));
	}

	@Test
	public void testGetValueAsStringOnFSArrayWithReverseIndex() throws Exception {
		// Here we test whether the correct value is retrieved via the feature
		// path when the end of the path is an
		// array and we provide a particular array index to retrieve the value
		// from.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/authors[-1]");
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().

		Header header = new Header(jcas);
		FSArray authors = new FSArray(jcas, 2);
		AuthorInfo a1 = new AuthorInfo(jcas);
		a1.setForeName("Heinz");
		AuthorInfo a2 = new AuthorInfo(jcas);
		a2.setForeName("Karl");
		authors.set(0, a1);
		authors.set(1, a2);
		header.setAuthors(authors);

		assertEquals(authors.get(1), fp.getValue(header, 0));

		JCoReFeaturePath fp2 = new JCoReFeaturePath();
		fp2.initialize("/authors[-2]");

		assertEquals(authors.get(0), fp2.getValue(header, 0));
	}

	@Test
	public void testGetValueAsStringArrayOnValueArrayWithoutIndex() throws Exception {
<<<<<<< HEAD
		// Here we test whether the correct value is retrieved via the feature
		// path when the end of the path is an
		// array and we don't deliver an index. This means that we want to get
		// all values.
=======
		// Here we test whether the correct value is retrieved via the feature path when the end of the path is an
		// array and we don't deliver an index. This means that we want to get all values.
>>>>>>> 7ad7536ab9d5d42fc932c6811c7ffbe15d509c29

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/semanticTypes");
<<<<<<< HEAD
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().
=======
		// We don't need to call fp.typeInit() because this is done automatically in fp.getValueAsString().
>>>>>>> 7ad7536ab9d5d42fc932c6811c7ffbe15d509c29

		OntClassMention ontClass = new OntClassMention(jcas);
		StringArray semTypes = new StringArray(jcas, 2);
		semTypes.set(0, "type1");
		semTypes.set(1, "type2");
		ontClass.setSemanticTypes(semTypes);

		// Check if array return type is handled correctly.
		assertEquals("type1, type2", fp.getValueAsString(ontClass));
		String[] stringArray = fp.getValueAsStringArray(ontClass);
		assertEquals(semTypes.get(0), stringArray[0]);
		assertEquals(semTypes.get(1), stringArray[1]);

		JCoReFeaturePath fp2 = new JCoReFeaturePath();
		fp2.initialize("/semanticTypes[1]");

		assertEquals(semTypes.get(1), fp2.getValueAsString(ontClass));
	}

	@Test
	public void testGetValueAsStringOnSubtype() throws Exception {
		// Here we have the following case: Some annotation type - e.g. the
		// ArgumentMention - defines a feature of
		// another annotation type, e.g. "Annotation" as it happens with the
		// "ref" feature of the type
		// ArgumentMention. Now we happen to know that the actual type of "ref"
		// in a particular use case - for example
		// Protein-Protein-Interaction (PPI) relations - is always "Gene", a far
		// subtype of Annotation. Thus, in the
		// feature path we want to access the feature "specificType" of the
		// Genes. The problem is now that the feature
		// type of "ref" in the type system is Annotation, thus there is no
		// feature "specific" type. This case must be
		// resolved at runtime and is tested here.

		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/ref/specificType");
		// We don't need to call fp.typeInit() because this is done
		// automatically in fp.getValueAsString().

		ArgumentMention argumentMention = new ArgumentMention(jcas);
		Gene gene = new Gene(jcas);
		gene.setSpecificType("specificTypeA");
		argumentMention.setRef(gene);

		assertEquals(gene.getSpecificType(), fp.getValueAsString(argumentMention));
	}

	@Test
	public void testGetValueAsStringArrayOnGene() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		Gene gene = new Gene(jcas);
		FSArray resourceEntryList = new FSArray(jcas, 2);
		ResourceEntry entry1 = new ResourceEntry(jcas);
		entry1.setEntryId("id1");
		ResourceEntry entry2 = new ResourceEntry(jcas);
		entry2.setEntryId("id2");
		resourceEntryList.set(0, entry1);
		resourceEntryList.set(1, entry2);
		gene.setResourceEntryList(resourceEntryList);

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList[0]/entryId");

		assertEquals(entry1.getEntryId(), fp.getValueAsString(gene));

		fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList/entryId");

		assertEquals("id1, id2", fp.getValueAsString(gene));

	}

	@Test
	public void testReplacePrimitiveValue() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		ConceptMention cm = new ConceptMention(jcas);
		cm.setTextualRepresentation("originalValue");

		Map<String, String> replacements = new HashMap<>();
		replacements.put("originalValue", "replacementValue");

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/textualRepresentation", replacements);

		assertEquals("originalValue", cm.getTextualRepresentation());
		assertEquals("originalValue", fp.getValueAsString(cm));
		assertEquals("replacementValue", fp.getValueAsString(cm, true));
		assertEquals("replacementValue", fp.getValueAsString(cm));
		assertEquals("replacementValue", cm.getTextualRepresentation());

		// doing a replacement again should have no effect
		assertEquals("replacementValue", fp.getValueAsString(cm, true));
		assertEquals("replacementValue", fp.getValueAsString(cm));
	}

	@Test
	public void testReplaceNotMappedPrimitiveValue() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		ConceptMention cm = new ConceptMention(jcas);
		cm.setTextualRepresentation("originalValue");

		ConceptMention cm2 = new ConceptMention(jcas);
		cm2.setTextualRepresentation("unknownValue");

		Map<String, String> replacements = new HashMap<>();
		replacements.put("originalValue", "replacementValue");

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/textualRepresentation", replacements);
		fp.setReplaceUnmappedValues(true);
		fp.setDefaultReplacementValue("not-mapped");

		assertEquals("originalValue", cm.getTextualRepresentation());
		assertEquals("originalValue", fp.getValueAsString(cm));
		assertEquals("replacementValue", fp.getValueAsString(cm, true));

		assertEquals("unknownValue", cm2.getTextualRepresentation());
		assertEquals("unknownValue", fp.getValueAsString(cm2));
		assertEquals("not-mapped", fp.getValueAsString(cm2, true));
	}

	@Test
	public void testReplaceNotMappedPrimitiveValueWithNull() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		ConceptMention cm = new ConceptMention(jcas);
		cm.setTextualRepresentation("unknownValue");

		Map<String, String> replacements = new HashMap<>();
		replacements.put("originalValue", "replacementValue");

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/textualRepresentation", replacements);
		fp.setReplaceUnmappedValues(true);
		// here, we do NOT set a default-replacement value
		// fp.setNullReplacementValue("not-mapped");

		assertEquals("unknownValue", cm.getTextualRepresentation());
		assertEquals("unknownValue", fp.getValueAsString(cm));
		assertEquals(null, fp.getValueAsString(cm, true));
		assertNotSame("null", fp.getValueAsString(cm, true));
	}

	@Test
	public void testReplaceAllArrayElements() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		StringArray sa = new StringArray(jcas, 3);
		sa.set(0, "entry1");
		sa.set(1, "entry2");
		sa.set(2, "entry3");
		OntClassMention ocm = new OntClassMention(jcas);
		ocm.setSemanticTypes(sa);

		Map<String, String> replacements = new HashMap<>();
		replacements.put("entry1", "replacement1");
		replacements.put("entry2", "replacement2");
		replacements.put("entry3", "replacement3");

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/semanticTypes", replacements);

		assertEquals("entry1, entry2, entry3", fp.getValueAsString(ocm));
		assertEquals("replacement1, replacement2, replacement3", fp.getValueAsString(ocm, true));
	}

	@Test
	public void testReplaceAllArrayElementsFromFile() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		StringArray sa = new StringArray(jcas, 3);
		sa.set(0, "entry1");
		sa.set(1, "entry2");
		sa.set(2, "entry3");
		OntClassMention ocm = new OntClassMention(jcas);
		ocm.setSemanticTypes(sa);

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.loadReplacementsFromFile("src/test/resources/testReplacementFile.map");
		fp.initialize("/semanticTypes");

		assertEquals("entry1, entry2, entry3", fp.getValueAsString(ocm));
		assertEquals("replacement1, replacement2, replacement3", fp.getValueAsString(ocm, true));

	}

	@Test
	public void testReplaceSingleArrayElement() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		StringArray sa = new StringArray(jcas, 3);
		sa.set(0, "entry1");
		sa.set(1, "entry2");
		sa.set(2, "entry3");
		OntClassMention ocm = new OntClassMention(jcas);
		ocm.setSemanticTypes(sa);

		Map<String, String> replacements = new HashMap<>();
		replacements.put("entry1", "replacement1");
		replacements.put("entry2", "replacement2");
		replacements.put("entry3", "replacement3");

		// test single array elements
		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/semanticTypes[1]", replacements);

		assertEquals("replacement2", fp.getValueAsString(ocm, true));

		fp.initialize("/semanticTypes");
		assertEquals("entry1, replacement2, entry3", fp.getValueAsString(ocm));
	}

	/**
	 * 'Deep' means a recursive structure, a FeatureStructure containing another
	 * FeatureStructure.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReplaceValueOnDeepFeatureStructure() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		Gene gene = new Gene(jcas);
		FSArray resourceEntryList = new FSArray(jcas, 2);
		ResourceEntry entry1 = new ResourceEntry(jcas);
		entry1.setEntryId("id1");
		ResourceEntry entry2 = new ResourceEntry(jcas);
		entry2.setEntryId("id2");
		resourceEntryList.set(0, entry1);
		resourceEntryList.set(1, entry2);
		gene.setResourceEntryList(resourceEntryList);

		Map<String, String> replacements = new HashMap<>();
		replacements.put("id1", "tid1");
		replacements.put("id2", "tid2");

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList/entryId", replacements);

		assertEquals("tid1, tid2", fp.getValueAsString(gene, true));

	}

	@Test
	public void testGetCoveredText() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("IL-2 mTOR");
		Gene g = new Gene(jcas, 0, 4);
		ResourceEntry re = new ResourceEntry(jcas, 0, 4);
		FSArray resourceEntryList = new FSArray(jcas, 1);
		resourceEntryList.set(0, re);
		g.setResourceEntryList(resourceEntryList);
		g.addToIndexes();

		JCoReFeaturePath fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList:coveredText()");
		assertEquals("IL-2", fp.getValueAsString(g));

		ResourceEntry re2 = new ResourceEntry(jcas, 5, 9);
		resourceEntryList = new FSArray(jcas, 2);
		resourceEntryList.set(0, re);
		resourceEntryList.set(1, re2);
		g.setResourceEntryList(resourceEntryList);

		fp = new JCoReFeaturePath();
		fp.initialize("/resourceEntryList:coveredText()");
		assertEquals("IL-2, mTOR", fp.getValueAsString(g));
	}
}
