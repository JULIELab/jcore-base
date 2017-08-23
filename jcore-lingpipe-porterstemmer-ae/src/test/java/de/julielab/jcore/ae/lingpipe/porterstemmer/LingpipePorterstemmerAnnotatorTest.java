/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.lingpipe.porterstemmer;

import static org.junit.Assert.*;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.Token;

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
