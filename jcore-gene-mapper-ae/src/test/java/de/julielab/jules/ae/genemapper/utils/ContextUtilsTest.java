package de.julielab.jules.ae.genemapper.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;

import org.apache.lucene.search.BooleanQuery;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.EntityMention;

public class ContextUtilsTest {
	@Test
	public void testMakeContextQuery() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		// This document caused errors in the ContextUtils previously. This test
		// makes sure the fix is working.
		XmiCasDeserializer.deserialize(new FileInputStream("src/test/resources/10747952.xmi"), jcas.getCas());
		FSIterator<Annotation> it = jcas.getAnnotationIndex(EntityMention.type).iterator();
		while (it.hasNext()) {
			EntityMention em = (EntityMention) it.next();
			BooleanQuery contextQuery = ContextUtils.makeContextQuery(jcas, null, 50, em);
			assertNotNull(contextQuery);
		}
	}

	@Test
	public void testMakeContextWithoutTokens() throws Exception {
		// Normally, the context utils use the tokens in the CAS index to create
		// the index. If there are no tokens, just use the text. This is tested
		// here.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("Entity at begin, entity in the middle and at the end also an entity");
		EntityMention e1 = new EntityMention(jcas, 0, 6);
		e1.addToIndexes();
		EntityMention e2 = new EntityMention(jcas, 17, 23);
		e2.addToIndexes();
		EntityMention e3 = new EntityMention(jcas, 61, 67);
		e3.addToIndexes();

		assertEquals("Entity at begin,", ContextUtils.makeContext(jcas, null, 5, e1));
		assertEquals("t begin, entity in the mi", ContextUtils.makeContext(jcas, null, 5, e2));
		assertEquals("also an entity", ContextUtils.makeContext(jcas, null, 5, e3));
	}

}
