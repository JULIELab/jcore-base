/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.lingpipe.porterstemmer;

import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LingpipePorterstemmerAnnotatorTest {
	@Test
	public void testAnnotator() throws Exception {
		AnalysisEngine stemmerAnnotator = AnalysisEngineFactory
				.createEngine("de.julielab.jcore.ae.lingpipe.porterstemmer.desc.jcore-lingpipe-porterstemmer-ae");
		JCas jCas = stemmerAnnotator.newJCas();
		jCas.setDocumentText("Three horses were going contemplatively around bushy bushes.");
		Token t;
		t = new Token(jCas, 0, 5);
		t.addToIndexes();
		t = new Token(jCas, 6, 12);
		t.addToIndexes();
		t = new Token(jCas, 13, 17);
		t.addToIndexes();
		t = new Token(jCas, 18, 23);
		t.addToIndexes();
		t = new Token(jCas, 24, 39);
		t.addToIndexes();
		t = new Token(jCas, 40, 46);
		t.addToIndexes();
		t = new Token(jCas, 47, 52);
		t.addToIndexes();
		t = new Token(jCas, 53, 59);
		t.addToIndexes();
		t = new Token(jCas, 59, 60);
		t.addToIndexes();
		stemmerAnnotator.process(jCas);
		FSIterator<Annotation> iterator = jCas.getAnnotationIndex(Token.type).iterator();
		while (iterator.hasNext()) {
			Token token = (Token) iterator.next();
			assertNotNull(token.getStemmedForm());
			assertNotNull(token.getStemmedForm().getValue());
			assertEquals(token.getBegin(), token.getStemmedForm().getBegin());
			assertEquals(token.getEnd(), token.getStemmedForm().getEnd());
			String expectedStem;
			switch (token.getCoveredText()) {
			case "Three":
				expectedStem = "Three";
				break;
			case "horses":
				expectedStem = "hors";
				break;
			case "were":
				expectedStem = "were";
				break;
			case "going":
				expectedStem = "go";
				break;
			case "contemplatively":
				expectedStem = "contempl";
				break;
			case "around":
				expectedStem = "around";
				break;
			case "bushy":
				expectedStem = "bushi";
				break;
			case "bushes":
				expectedStem = "bush";
				break;
			case ".":
				expectedStem = ".";
				break;
			default:
				throw new IllegalStateException(
						"Coding error in the test, all words should be covered or we could miss errors.");
			}
			assertEquals(token.getStemmedForm().getValue(), expectedStem);
		}
	}
}
