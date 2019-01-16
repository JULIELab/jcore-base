package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.*;
import de.julielab.jcore.es.test.ESConsumerTestType;
import de.julielab.jcore.es.test.EntityTestType;
import de.julielab.jcore.es.test.ParagraphTestType;
import de.julielab.jcore.es.test.SentenceTestType;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;

public class AbstractFieldGeneratorTest extends AbstractFieldGenerator {

	@Test
	public void testCreatePreanalyzedToken() {
		PreanalyzedToken token = createPreanalyzedToken("testterm", 0, 1, 2, null, "atype", 8);
		assertEquals("testterm", token.term);
		assertEquals(0, token.start);
		assertEquals(1, token.end);
		assertEquals(2, token.positionIncrement);
		assertNull(token.payload);
		assertEquals("atype", token.type);
		assertEquals("8", token.flags);
	}

	@Test
	public void testCreatePreanalyzedTokenForTokenSequence() {
		List<IToken> list = new ArrayList<>();
		// try to create a token list that begins with a token that has position
		// increment 0
		PreanalyzedToken token = createPreanalyzedTokenInTokenSequence(list, "aterm", 0, 0, 0,
				null, null, 0);
		assertEquals(1, token.positionIncrement);
		list.add(token);
		// exact same call as above, again position increment zero, but this
		// time in the non-empty ist
		token = createPreanalyzedTokenInTokenSequence(list, "aterm", 0, 0, 0, null, null, 0);
		assertEquals(0, token.positionIncrement);
	}

	@Test
	public void testCreateRawFieldValueForAnnotation() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.consumer.es.testTypes");
		jCas.setDocumentText("text");

		ESConsumerTestType a = new ESConsumerTestType(jCas, 0, 4);
		a.setStringFeature("testvalue");
		StringArray sa = new StringArray(jCas, 2);
		sa.set(0, "saElement1");
		sa.set(1, "saElement2");
		a.setStringArrayFeature(sa);

		IFieldValue value = createRawFieldValueForAnnotation(a, "/stringFeature", null);
		assertEquals(RawToken.class, value.getClass());
		assertEquals("testvalue", ((RawToken) value).token);

		value = createRawFieldValueForAnnotation(a, "/stringArrayFeature", null);
		assertEquals(ArrayFieldValue.class, value.getClass());
		ArrayFieldValue array = (ArrayFieldValue) value;
		assertEquals(2, array.size());
		assertEquals(RawToken.class, array.get(0).getClass());
		assertEquals("saElement1", ((RawToken) array.get(0)).token);
		assertEquals("saElement2", ((RawToken) array.get(1)).token);
	}

	@Test
	public void testCreatePreanalyzedFieldValueForAnnotation() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.consumer.es.testTypes");
		jCas.setDocumentText("text");

		ESConsumerTestType a = new ESConsumerTestType(jCas, 0, 4);
		a.setStringFeature("testvalue");
		StringArray sa = new StringArray(jCas, 2);
		sa.set(0, "saElement1");
		sa.set(1, "saElement2");
		a.setStringArrayFeature(sa);

		// The token for the single string feature should have the annotation's
		// start and end values.
		IFieldValue value = createPreanalyzedFieldValueForAnnotation(a, "/stringFeature", null);
		assertEquals(PreanalyzedFieldValue.class, value.getClass());
		PreanalyzedFieldValue preAnVal = (PreanalyzedFieldValue) value;
		assertEquals(1, preAnVal.tokens.size());
		assertEquals(0, preAnVal.tokens.get(0).start);
		assertEquals(4, preAnVal.tokens.get(0).end);

		// For the array value we expect two field values, each of which has one
		// token which in turn should have the
		// annotation's start and end values.
		value = createPreanalyzedFieldValueForAnnotation(a, "/stringArrayFeature", null);
		assertEquals(ArrayFieldValue.class, value.getClass());
		ArrayFieldValue array = (ArrayFieldValue) value;
		assertEquals(2, array.size());
		for (IFieldValue arrayVal : array) {
			assertEquals(PreanalyzedFieldValue.class, arrayVal.getClass());
			PreanalyzedFieldValue preVal = (PreanalyzedFieldValue) arrayVal;
			assertEquals(1, preVal.tokens.size());
			assertEquals(0, preVal.tokens.get(0).start);
			assertEquals(4, preVal.tokens.get(0).end);

		}
	}

	@Test
	public void testGetTokensForAnnotationIndexesPreanalyzedTokens() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.consumer.es.testTypes");
		jCas.setDocumentText("text");

		ESConsumerTestType a = new ESConsumerTestType(jCas, 0, 3);
		a.setStringFeature("one");
		StringArray sa = new StringArray(jCas, 2);
		sa.set(0, "saElement1");
		sa.set(1, "saElement2");
		a.setStringArrayFeature(sa);
		a.addToIndexes();

		ESConsumerTestType a2 = new ESConsumerTestType(jCas, 4, 7);
		a2.setStringFeature("two");
		a2.addToIndexes();

		// at the same position as a2
		EntityTestType e = new EntityTestType(jCas, 4, 7);
		e.setEntityType("someentity");
		e.addToIndexes();

		FeaturePathSet featurePathSet = new FeaturePathSet(ESConsumerTestType.type, Arrays.asList(
				"/stringFeature", "/stringArrayFeature"), null, null);
		FeaturePathSet entityFeaturePathSet = new FeaturePathSet(EntityTestType.type,
				Arrays.asList("/entityType"), null, null);
		FeaturePathSets sets = new FeaturePathSets();
		sets.add(featurePathSet);
		sets.add(entityFeaturePathSet);

		List<PreanalyzedToken> fieldValues = getTokensForAnnotationIndexes(sets, null, true,
				PreanalyzedToken.class, null, null, jCas);
		// For each of the above set annotation values we should get one token
		assertEquals(5, fieldValues.size());
		assertEquals(PreanalyzedToken.class, fieldValues.get(0).getClass());
		PreanalyzedToken t1 = (PreanalyzedToken) fieldValues.get(0);
		PreanalyzedToken t2 = (PreanalyzedToken) fieldValues.get(1);
		PreanalyzedToken t3 = (PreanalyzedToken) fieldValues.get(2);
		PreanalyzedToken t4 = (PreanalyzedToken) fieldValues.get(3);
		PreanalyzedToken t5 = (PreanalyzedToken) fieldValues.get(4);
		// values from a
		assertEquals("one", t1.term);
		assertEquals(1, t1.positionIncrement);
		assertEquals("saElement1", t2.term);
		assertEquals(0, t2.positionIncrement);
		assertEquals("saElement2", t3.term);
		assertEquals(0, t3.positionIncrement);
		// values from a1
		assertEquals("two", t4.term);
		assertEquals(1, t4.positionIncrement);
		// values from e
		assertEquals("someentity", t5.term);
		assertEquals(0, t5.positionIncrement);

	}

	@Test
	public void testCoveringAnnotationSameOffsets() throws Exception {
		// Here we have a sentence and a paragraph of the same size. Since
		// internally, we ignore type priorities, they shouldn't matter. First,
		// test the priorities in the intuitive way, Paragraph < Sentence, which
		// actually means "a paragraph is conceptually larger than a sentence".
		TypeSystemDescription tsDesc;
		TypePriorities typePrios;
		AnalysisEngine ae;
		JCas jCas;
		ParagraphTestType p;
		SentenceTestType s;
		FeaturePathSets featurePathSets;
		List<RawToken> tokens;

		tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.consumer.es.testTypes");
		typePrios = TypePrioritiesFactory.createTypePriorities(ParagraphTestType.class,
				SentenceTestType.class);
		ae = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, tsDesc, typePrios);
		jCas = ae.newJCas();
		jCas.setDocumentText("A paragraph and also a sentence.");
		p = new ParagraphTestType(jCas, 0, 32);
		s = new SentenceTestType(jCas, 0, 32);
		p.addToIndexes();
		s.addToIndexes();

		featurePathSets = new FeaturePathSets(new FeaturePathSet(SentenceTestType.type, null, null,
				null));

		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false, RawToken.class, p,
				null, jCas);
		assertEquals(1, tokens.size());
		assertEquals(jCas.getDocumentText(), tokens.get(0).token);

		// When we change the priorities, we still should retrieve the covered
		// annotation, even if now the "narrower" annotation "contains" the
		// "large" annotation.

		tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.consumer.es.testTypes");
		typePrios = TypePrioritiesFactory.createTypePriorities(SentenceTestType.class,
				ParagraphTestType.class);
		ae = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, tsDesc, typePrios);
		jCas = ae.newJCas();
		jCas.setDocumentText("A paragraph and also a sentence.");
		p = new ParagraphTestType(jCas, 0, 32);
		s = new SentenceTestType(jCas, 0, 32);
		p.addToIndexes();
		s.addToIndexes();

		featurePathSets = new FeaturePathSets(new FeaturePathSet(SentenceTestType.type, null, null,
				null));

		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false, RawToken.class, p,
				null, jCas);
		assertEquals(1, tokens.size());

	}

	@Test
	public void testCoveringAnnotationSameOffsets2() throws Exception {
		// Check that we also get the covering annotation itself if we
		// specifically look for the its type
		TypeSystemDescription tsDesc;
		TypePriorities typePrios;
		AnalysisEngine ae;
		JCas jCas;
		ParagraphTestType p;
		SentenceTestType s;
		FeaturePathSets featurePathSets;
		List<RawToken> tokens;

		tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.consumer.es.testTypes");
		typePrios = TypePrioritiesFactory.createTypePriorities(ParagraphTestType.class,
				SentenceTestType.class);
		ae = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, tsDesc, typePrios);
		jCas = ae.newJCas();
		jCas.setDocumentText("A paragraph and also a sentence.");
		p = new ParagraphTestType(jCas, 0, 32);
		s = new SentenceTestType(jCas, 0, 32);
		p.addToIndexes();
		s.addToIndexes();

		featurePathSets = new FeaturePathSets(new FeaturePathSet(SentenceTestType.type, null, null,
				null));

		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false, RawToken.class, s,
				null, jCas);
		assertEquals(1, tokens.size());
		assertEquals(jCas.getDocumentText(), tokens.get(0).token);

	}

	@Test
	public void testCoveringAnnotation() throws Exception {
		// Test that we really get all the covered annotations.
		TypeSystemDescription tsDesc;
		TypePriorities typePrios;
		AnalysisEngine ae;
		JCas jCas;
		ParagraphTestType p;
		SentenceTestType s1;
		SentenceTestType s2;
		FeaturePathSets featurePathSets;
		List<RawToken> tokens;

		tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.consumer.es.testTypes");
		typePrios = TypePrioritiesFactory.createTypePriorities(ParagraphTestType.class,
				SentenceTestType.class);
		ae = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, tsDesc, typePrios);
		jCas = ae.newJCas();
		jCas.setDocumentText("A first sentence. And a second sentence.");
		p = new ParagraphTestType(jCas, 0, 40);
		s1 = new SentenceTestType(jCas, 0, 17);
		s2 = new SentenceTestType(jCas, 18, 40);
		p.addToIndexes();
		s1.addToIndexes();
		s2.addToIndexes();

		featurePathSets = new FeaturePathSets(new FeaturePathSet(SentenceTestType.type, null, null,
				null));

		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false, RawToken.class, p,
				null, jCas);
		assertEquals(2, tokens.size());
		assertEquals("A first sentence.", tokens.get(0).token);
		assertEquals("And a second sentence.", tokens.get(1).token);
	}

	@Test
	public void testTokenOffsetsInCoveringAnnotations() throws Exception {
		// Test that for preanalyzed tokens, their offsets are correctly adapted
		// relative to their covering annotation
		TypeSystemDescription tsDesc;
		TypePriorities typePrios;
		AnalysisEngine ae;
		JCas jCas;
		ParagraphTestType p1;
		ParagraphTestType p2;
		SentenceTestType s1;
		SentenceTestType s2;
		FeaturePathSets featurePathSets;
		List<PreanalyzedToken> tokens;

		tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.consumer.es.testTypes");
		typePrios = TypePrioritiesFactory.createTypePriorities(ParagraphTestType.class,
				SentenceTestType.class);
		ae = AnalysisEngineFactory.createEngine(NoOpAnnotator.class, tsDesc, typePrios);
		jCas = ae.newJCas();
		jCas.setDocumentText("A first sentence. And a second sentence.");
		// The first sentence lies in the first paragraph, the second sentence
		// in the second paragraph. We want to build one field for each
		// paragraph. After creating preanalyzed tokens, both
		// sentences should have a start offset of 0, since they are both the
		// beginning of their respective paragraphs.
		p1 = new ParagraphTestType(jCas, 0, 17);
		p2 = new ParagraphTestType(jCas, 18, 40);
		s1 = new SentenceTestType(jCas, 0, 17);
		s2 = new SentenceTestType(jCas, 18, 40);
		p1.addToIndexes();
		s1.addToIndexes();
		s2.addToIndexes();

		featurePathSets = new FeaturePathSets(new FeaturePathSet(SentenceTestType.type, null, null,
				null));

		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false,
				PreanalyzedToken.class, p1, null, jCas);
		assertEquals(1, tokens.size());
		assertEquals(0, tokens.get(0).start);
		assertEquals(17, tokens.get(0).end);
		tokens = getTokensForAnnotationIndexes(featurePathSets, null, false,
				PreanalyzedToken.class, p2, null, jCas);
		assertEquals(1, tokens.size());
		assertEquals(0, tokens.get(0).start);
		assertEquals(22, tokens.get(0).end);
	}
}
