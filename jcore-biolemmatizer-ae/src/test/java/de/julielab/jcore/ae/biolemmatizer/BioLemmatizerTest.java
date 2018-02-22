
package de.julielab.jcore.ae.biolemmatizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Token;
/**
 * Unit tests for jcore-de.julielab.jcore.ae.biolemmatizer-ae.
 * @author 
 *
 */
public class BioLemmatizerTest{
	
	@Test
	public void testBioLemmatizer() {
		try {
			JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
			jCas.setDocumentText("Three horses were going contemplatively around bushy bushes.");
			BioLemmatizer biolemm = new BioLemmatizer();
			AnalysisEngine bioLemmatizer = AnalysisEngineFactory.createEngine(biolemm.getClass());
			Token t;
			POSTag pos;
			
			t = new Token(jCas, 0, 5);
			FSArray posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 0, 5);
			pos.setValue("DT");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 6, 12);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 6, 12);
			pos.setValue("NNS");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 13, 17);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 13, 17);
			pos.setValue("VBD");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 18, 23);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 18, 23);
			pos.setValue("VBG");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 24, 39);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 24, 39);
			pos.setValue("RB");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 40, 46);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 40, 46);
			pos.setValue("IN");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 47, 52);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 47, 52);
			pos.setValue("JJ");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 53, 59);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 53, 59);
			pos.setValue("NNS");
			t.setPosTag(0, pos);
			t.addToIndexes();
			t = new Token(jCas, 59, 60);
			posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			pos = new POSTag(jCas, 59, 60);
			pos.setValue(".");
			t.setPosTag(0, pos);
			t.addToIndexes();

			bioLemmatizer.process(jCas);
			
			FSIterator<Annotation> iterator = jCas.getAnnotationIndex(Token.type).iterator();
			while (iterator.hasNext()) {
				Token token = (Token) iterator.next();
				assertNotNull(token.getLemma());
				assertNotNull(token.getLemma().getValue());
				assertEquals(token.getBegin(), token.getLemma().getBegin());
				assertEquals(token.getEnd(), token.getLemma().getEnd());
				String expectedLemma;
				switch (token.getCoveredText()) {
				case "Three":
					expectedLemma = "three";
					break;
				case "horses":
					expectedLemma = "horse";
					break;
				case "were":
					expectedLemma = "be";
					break;
				case "going":
					expectedLemma = "go";
					break;
				case "contemplatively":
					expectedLemma = "contemplative";
					break;
				case "around":
					expectedLemma = "around";
					break;
				case "bushy":
					expectedLemma = "bushy";
					break;
				case "bushes":
					expectedLemma = "bush";
					break;
				case ".":
					expectedLemma = ".";
					break;
				default:
					throw new IllegalStateException(
							"Coding error in the test, all words should be covered or we could miss errors.");
				}
				assertEquals(token.getLemma().getValue(), expectedLemma);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testBioLemmatizerWithoutPOS() {
		try {
			JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
			jCas.setDocumentText("Three horses were going contemplatively around bushy bushes.");
			BioLemmatizer biolemm = new BioLemmatizer();
			AnalysisEngine bioLemmatizer = AnalysisEngineFactory.createEngine(biolemm.getClass());
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

			bioLemmatizer.process(jCas);
			
			FSIterator<Annotation> iterator = jCas.getAnnotationIndex(Token.type).iterator();
			while (iterator.hasNext()) {
				Token token = (Token) iterator.next();
				assertNotNull(token.getLemma());
				assertNotNull(token.getLemma().getValue());
				assertEquals(token.getBegin(), token.getLemma().getBegin());
				assertEquals(token.getEnd(), token.getLemma().getEnd());
				String expectedLemma;
				switch (token.getCoveredText()) {
				case "Three":
					expectedLemma = "three";
					break;
				case "horses":
					expectedLemma = "horse";
					break;
				case "were":
					expectedLemma = "be";
					break;
				case "going":
					expectedLemma = "go";
					break;
				case "contemplatively":
					expectedLemma = "contemplative";
					break;
				case "around":
					expectedLemma = "around";
					break;
				case "bushy":
					expectedLemma = "bushy";
					break;
				case "bushes":
					expectedLemma = "bush";
					break;
				case ".":
					expectedLemma = ".";
					break;
				default:
					throw new IllegalStateException(
							"Coding error in the test, all words should be covered or we could miss errors.");
				}
				assertEquals(token.getLemma().getValue(), expectedLemma);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
