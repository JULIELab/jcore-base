
package biolemmatizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Token;

/**
 * Unit tests for jcore-biolemmatizer-ae.
 * 
 * @author
 *
 */
public class BioLemmatizerTest {

	@Test
	public void testBioLemmatizer() {
		try {
			JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
			jCas.setDocumentText("Three horses were going contemplatively around bushy bushes.");
			AnalysisEngine bioLemmatizer = AnalysisEngineFactory
					.createEngine("biolemmatizer.desc.jcore-biolemmatizer-ae");
			TypeSystem typeSystem = jCas.getTypeSystem();
			Feature posFeat = typeSystem.getFeatureByFullName("de.julielab.jcore.types.POSTag");
			Token t;
			POSTag pos;
			t = new Token(jCas, 0, 5);
			// t.setFeatureValueFromString(posFeat, "DT");
			// Die obige Zeile kann nicht funktionieren, denn:
			assertNull(t.getPosTag());
			// Logisch, schließlich haben wir kein POSTag Array gesetzt (bemerke, dass
			// "getPosTag()" ein FSArray zurück gibt! Es sind also eigentlich potentiell
			// mehrere POSTags)
			// Also erstellen wir erstmal ein FSArray:
			FSArray posTagArray = new FSArray(jCas, 1);
			t.setPosTag(posTagArray);
			// Und ein POSTag:
			pos = new POSTag(jCas, 0, 5);
			pos.setValue("DT");
			// pos.addToIndexes();
			// Und jetzt klappt auch das Setzen:
			t.setPosTag(0, pos);
			assertEquals("DT", t.getPosTag(0).getValue());
			t.addToIndexes();

			// Der Rest des Codes ist von mir unverändert. Du wirst festellen, dass der Test
			// bis hierhin läuft. Erst unten bei "t.setFeatureValueFromString(posFeat,
			// "NNS");" hängt es wieder, weil dem neuen Token noch kein array für die
			// POSTags gesetzt wurde.
			// -----------------------------
			t = new Token(jCas, 6, 12);
			t.setFeatureValueFromString(posFeat, "NNS");
			// pos = new POSTag(jCas, 6, 12);
			// pos.setValue("NNS");
			// pos.addToIndexes();
			// t.setPosTag(1, pos);
			t.addToIndexes();
			t = new Token(jCas, 13, 17);
			t.setFeatureValueFromString(posFeat, "VBD");
			// pos = new POSTag(jCas, 13, 17);
			// pos.setValue("VBD");
			// pos.addToIndexes();
			// t.setPosTag(2, pos);
			t.addToIndexes();
			t = new Token(jCas, 18, 23);
			t.setFeatureValueFromString(posFeat, "VBG");
			// pos = new POSTag(jCas, 18, 23);
			// pos.setValue("VBG");
			// pos.addToIndexes();
			// t.setPosTag(3, pos);
			t.addToIndexes();
			t = new Token(jCas, 24, 39);
			t.setFeatureValueFromString(posFeat, "RB");
			// pos = new POSTag(jCas, 24, 39);
			// pos.setValue("RB");
			// pos.addToIndexes();
			// t.setPosTag(4, pos);
			t.addToIndexes();
			t = new Token(jCas, 40, 46);
			t.setFeatureValueFromString(posFeat, "IN");
			// pos = new POSTag(jCas, 40, 46);
			// pos.setValue("IN");
			// pos.addToIndexes();
			// t.setPosTag(5, pos);
			t.addToIndexes();
			t = new Token(jCas, 47, 52);
			t.setFeatureValueFromString(posFeat, "JJ");
			// pos = new POSTag(jCas, 47, 52);
			// pos.setValue("JJ");
			// pos.addToIndexes();
			// t.setPosTag(6, pos);
			t.addToIndexes();
			t = new Token(jCas, 53, 59);
			t.setFeatureValueFromString(posFeat, "NNS");
			// pos = new POSTag(jCas, 53, 59);
			// pos.setValue("NNS");
			// pos.addToIndexes();
			// t.setPosTag(7, pos);
			t.addToIndexes();
			t = new Token(jCas, 59, 60);
			t.setFeatureValueFromString(posFeat, ".");
			// pos = new POSTag(jCas, 59, 60);
			// pos.setValue(".");
			// t.setPosTag(8, pos);
			t.addToIndexes();
			// PennBioIEPOSTag pos = new PennBioIEPOSTag(jCas);
			// pos.setValue("DT");
			// t.setPosTag(0, pos);
			// t.setPosTag(0, "DT");
			// t.setPosTag(1, "NNS");
			// t.setPosTag(2, "VBD");
			// t.setPosTag(3, "VBG");
			// t.setPosTag(4, "RB");
			// t.setPosTag(5, "IN");
			// t.setPosTag(6, "JJ");
			// t.setPosTag(7, "NNS");
			// t.setPosTag(8, ".");
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
					expectedLemma = "Three";
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
					expectedLemma = "bush";
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
