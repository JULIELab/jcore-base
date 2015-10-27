/** 
 * OpenNLPSentenceDetectorTest.java
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
 * Test for OpenNLP Sentence Annotator
 **/

package de.julielab.jcore.ae.jsentsplit;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Sentence;

public class SentenceAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SentenceAnnotatorTest.class);
	
	String text = "First sentence. Second sentence!";

	String offsets = "0-15;16-32;";

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testProcess() {

		XMLInputSource sentenceXML = null;
		ResourceSpecifier sentenceSpec = null;
		AnalysisEngine sentenceAnnotator = null;

		try {
			sentenceXML = new XMLInputSource(
					"src/test/resources/SentenceAnnotatorTest.xml");
			sentenceSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					sentenceXML);
			sentenceAnnotator = UIMAFramework
					.produceAnalysisEngine(sentenceSpec);
		} catch (Exception e) {
			e.printStackTrace();
		}

		JCas jcas = null;
		try {
			jcas = sentenceAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();

		}
		jcas.setDocumentText(text);

		try {
			sentenceAnnotator.process(jcas, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator sentIter = indexes.getAnnotationIndex(Sentence.type)
				.iterator();

		String predictedOffsets = "";

		while (sentIter.hasNext()) {
			Sentence s = (Sentence) sentIter.next();
			predictedOffsets += s.getBegin() + "-" + s.getEnd() + ";";
		}

		LOGGER.debug("\npredicted: " + predictedOffsets);
		LOGGER.debug("wanted: " + offsets);

		// compare offsets
		assertTrue(predictedOffsets.equals(offsets));

	}

}
