/** 
 * OpenNLPPOSTaggerAnnotatorTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: buyko
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008 
 * 
 * Test for OpenNLP POS Tagger
 **/

package de.julielab.jcore.ae.opennlp.postag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.julielab.jcore.ae.opennlp.postag.PosTagAnnotator;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class PosTagAnnotatorTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(PosTagAnnotatorTest.class);

	String text = "A study on the Prethcamide";
	String postags = "DT;NN;IN;DT;NN;";

	public void initCas(JCas jcas) {

		jcas.reset();
		jcas.setDocumentText("A study on the Prethcamide");
		Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(text.length());
		s1.addToIndexes();
		Token t1 = new Token(jcas);
		t1.setBegin(0);
		t1.setEnd(1);
		t1.addToIndexes();
		PennBioIEPOSTag pos = new PennBioIEPOSTag(jcas);
		pos.setValue("DT");
		pos.addToIndexes();
		FSArray postags = new FSArray(jcas, 10);
		postags.set(0, pos);
		postags.addToIndexes();
		t1.setPosTag(postags);
		Token t2 = new Token(jcas);
		t2.setBegin(2);
		t2.setEnd(7);
		t2.addToIndexes();
		Token t3 = new Token(jcas);
		t3.setBegin(7);
		t3.setEnd(10);
		t3.addToIndexes();
		Token t4 = new Token(jcas);
		t4.setBegin(11);
		t4.setEnd(14);
		t4.addToIndexes();
		Token t5 = new Token(jcas);
		t5.setBegin(15);
		t5.setEnd(26);
		t5.addToIndexes();

	}

	@Test
	public void testProcess() throws Exception {

		boolean annotationsOK = true;

		XMLInputSource posXML = null;
		ResourceSpecifier posSpec = null;
		AnalysisEngine posAnnotator = null;

		posXML = new XMLInputSource("src/test/resources/PosTagAnnotatorTest.xml");
		posSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(posXML);
		posAnnotator = UIMAFramework.produceAnalysisEngine(posSpec);

		JCas jcas = null;
		jcas = posAnnotator.newJCas();

		// get test cas with sentence annotation
		initCas(jcas);

		posAnnotator.process(jcas, null);

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator tokIter = indexes.getAnnotationIndex(Token.type).iterator();

		String predictedPOSTags = "";

		while (tokIter.hasNext()) {
			Token t = (Token) tokIter.next();

			PennBioIEPOSTag tag = (PennBioIEPOSTag) t.getPosTag().get(0);

			predictedPOSTags = predictedPOSTags + tag.getValue() + ";";

		}
		LOGGER.debug("[testProcess]" + "\n Wanted: " + postags + "\n Predicted: " + predictedPOSTags);

		// compare offsets
		if (!predictedPOSTags.equals(postags)) {
			annotationsOK = false;
		}

		assertTrue(annotationsOK);

	}

	@Test
	public void testUimaFitAndClasspathResourceModel() throws Exception {
		// getting and setting up JCas
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText("This is a nice sentence.");
		new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
		new Token(jCas, 0, 4).addToIndexes();
		new Token(jCas, 5, 7).addToIndexes();
		new Token(jCas, 8, 9).addToIndexes();
		new Token(jCas, 10, 14).addToIndexes();
		new Token(jCas, 15, 23).addToIndexes();
		new Token(jCas, 23, 24).addToIndexes();

		// creating and running the PoS tag annotator
		AnalysisEngine engine =
				AnalysisEngineFactory.createEngine(PosTagAnnotator.class, PosTagAnnotator.PARAM_TAGSET,
						"de.julielab.jcore.types.PennBioIEPOSTag", PosTagAnnotator.PARAM_MODEL_FILE,
						"POSTagPennBioIE-3.0.bin.gz");
		engine.process(jCas.getCas());

		Collection<PennBioIEPOSTag> posTags = JCasUtil.select(jCas, PennBioIEPOSTag.class);
		// Check the generated POS tags. I know, 'nice' is not an NN, this is obviously not a word often used in the
		// training data.
		// And yes, I know, looks overly complex for a string concatenation :-)
		assertEquals("DT VBZ DT NN NN .", StringUtils.join(
				Collections2.<PennBioIEPOSTag, String> transform(posTags, new Function<PennBioIEPOSTag, String>() {
					@Override
					public String apply(PennBioIEPOSTag input) {
						return input.getValue();
					}
				}), " "));
	}

}
