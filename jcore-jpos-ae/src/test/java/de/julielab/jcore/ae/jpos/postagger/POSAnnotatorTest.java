/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.ae.jpos.postagger;

import static org.junit.Assert.assertEquals;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;

import de.julielab.jcore.types.STTSMedPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class POSAnnotatorTest {

	String text = "Der kleine Baum";
	String postags = "ART;ADJA;NN;";

	public void initCas(final JCas jcas) {
		jcas.reset();
		jcas.setDocumentText(text);
		final Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(text.length());
		s1.addToIndexes();

		final Token t1 = new Token(jcas);
		t1.setBegin(0);
		t1.setEnd(3);
		t1.addToIndexes();
		// STTSMedPOSTag pos = new STTSMedPOSTag(jcas);
		// pos.setValue("ART");
		// pos.addToIndexes();
		// FSArray postags = new FSArray(jcas, 10);
		// postags.set(0, pos);
		// postags.addToIndexes();
		// t1.setPosTag(postags);

		final Token t2 = new Token(jcas);
		t2.setBegin(4);
		t2.setEnd(10);
		t2.addToIndexes();
		// STTSMedPOSTag pos2 = new STTSMedPOSTag(jcas);
		// pos2.setValue("ADJA");
		// pos2.addToIndexes();
		// FSArray postags2 = new FSArray(jcas, 10);
		// postags2.set(0, pos2);
		// postags2.addToIndexes();
		// t2.setPosTag(postags2);

		final Token t3 = new Token(jcas);
		t3.setBegin(11);
		t3.setEnd(15);
		t3.addToIndexes();
		// STTSMedPOSTag pos3 = new STTSMedPOSTag(jcas);
		// pos3.setValue("NN");
		// pos3.addToIndexes();
		// FSArray postags3 = new FSArray(jcas, 10);
		// postags3.set(0, pos3);
		// postags3.addToIndexes();
		// t3.setPosTag(postags3);
	}

	@Test
	public void testProcess() throws Exception {

		XMLInputSource posXML = null;
		ResourceSpecifier posSpec = null;
		AnalysisEngine posAnnotator = null;

		posXML = new XMLInputSource(
				"src/test/resources/POSTagAnnotatorTest.xml");
		posSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(posXML);
		posAnnotator = UIMAFramework.produceAnalysisEngine(posSpec);

		final JCas jcas = posAnnotator.newJCas();
		// get test cas with sentence annotation
		initCas(jcas);

		posAnnotator.process(jcas, null);

		// get the offsets of the sentences
		final JFSIndexRepository indexes = jcas.getJFSIndexRepository();

		FSIterator<Annotation> tokIter = indexes.getAnnotationIndex(Token.type)
				.iterator();

		String predictedPOSTags = "";
		while (tokIter.hasNext()) {
			final Token t = (Token) tokIter.next();

			final STTSMedPOSTag tag = (STTSMedPOSTag) t.getPosTag().get(0);

			predictedPOSTags = predictedPOSTags + tag.getValue() + ";";
		}
		assertEquals(postags, predictedPOSTags);

		tokIter = indexes.getAnnotationIndex(STTSMedPOSTag.type).iterator();
		predictedPOSTags = "";
		while (tokIter.hasNext()) {
			final STTSMedPOSTag t = (STTSMedPOSTag) tokIter.next();
			predictedPOSTags = predictedPOSTags + t.getValue() + ";";
		}
		assertEquals(postags, predictedPOSTags);

	}
}
