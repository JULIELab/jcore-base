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
package de.julielab.jcore.ae.opennlp.token;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class TokenAnnotatorTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TokenAnnotatorTest.class);

	String offsets = "0-4;4-5;6-8;9-12;13-18;18-19;20-22;23-24;25-28";

	public void initCas(JCas jcas) {
		jcas.reset();
		jcas.setDocumentText("CD44, at any stage, is a XYZ");
		Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(28);
		s1.addToIndexes();
	}

	public void testProcess() throws Exception {

		boolean annotationsOK = true;

		XMLInputSource tokenXML = null;
		ResourceSpecifier tokenSpec = null;
		AnalysisEngine tokenAnnotator = null;

		tokenXML = new XMLInputSource(
				"src/test/resources/TokenAnnotatorTest.xml");
		tokenSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
				tokenXML);
		tokenAnnotator = UIMAFramework.produceAnalysisEngine(tokenSpec);

		JCas jcas = null;
		jcas = tokenAnnotator.newJCas();

		// get test cas with sentence annotation
		initCas(jcas);

		try {
			tokenAnnotator.process(jcas, null);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<Annotation> tokIter = indexes.getAnnotationIndex(Token.type).iterator();

		String predictedOffsets = "";

		while (tokIter.hasNext()) {
			Token t = (Token) tokIter.next();
			System.out.println("OUT: " + t.getCoveredText() + ": "
					+ t.getBegin() + " - " + t.getEnd());
			predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
			predictedOffsets += t.getBegin() + "-" + t.getEnd();
		}

		LOGGER.debug("\npredicted: " + predictedOffsets);
		LOGGER.debug("   wanted: " + offsets);

		// compare offsets
		if (!predictedOffsets.equals(offsets)) {
			annotationsOK = false;
		}

		assertTrue(annotationsOK);

	}
}
