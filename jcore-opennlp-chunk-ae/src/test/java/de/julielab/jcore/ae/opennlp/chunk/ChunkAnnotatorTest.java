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
package de.julielab.jcore.ae.opennlp.chunk;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;

import de.julielab.jcore.types.Chunk;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkAnnotatorTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChunkAnnotatorTest.class);

	protected void setUp() throws Exception {
		super.setUp();
	}

	String text = "A study on the Prethcamide hydroxylation system in rat hepatic microsomes .";

	String pos_tags = "DT NN IN DT NN NN NN IN NN JJ NNS .";

	String chunks = "ChunkNP,ChunkPP,ChunkNP,ChunkPP,ChunkNP,";

	private void initCas(JCas jcas) {

		jcas.reset();
		jcas.setDocumentText(text);

		Sentence s = new Sentence(jcas);
		s.setBegin(0);
		s.setEnd(text.length());
		s.addToIndexes(jcas);

		String[] tokens = text.split(" ");
		String[] pos = pos_tags.split(" ");
		int j = 0;
		for (int i = 0; i < tokens.length; i++) {
			Token token = new Token(jcas);
			token.setBegin(j);
			token.setEnd(j + tokens[i].length());
			j = j + tokens[i].length() + 1;
			token.addToIndexes(jcas);
			PennBioIEPOSTag posTag = new PennBioIEPOSTag(jcas);
			posTag.setValue(pos[i]);
			posTag.addToIndexes(jcas);
			FSArray postags = new FSArray(jcas, 10);
			postags.set(0, posTag);
			postags.addToIndexes(jcas);
			token.setPosTag(postags);
		}
	}

	public void testProcess() {

		XMLInputSource chunkerXML = null;
		ResourceSpecifier chunkerSpec = null;
		AnalysisEngine chunkerAnnotator = null;

		try {
			chunkerXML = new XMLInputSource(
					"src/test/resources/ChunkAnnotatorTest.xml");
			chunkerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					chunkerXML);
			chunkerAnnotator = UIMAFramework.produceAnalysisEngine(chunkerSpec);
		} catch (Exception e) {

			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		JCas jcas = null;
		try {
			jcas = chunkerAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		// get test cas with sentence/token/pos annotation
		initCas(jcas);

		try {
			chunkerAnnotator.process(jcas, null);
		} catch (Exception e) {
			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator chunkIter = indexes.getAnnotationIndex(Chunk.type)
				.iterator();

		String predictedChunks = "";

		while (chunkIter.hasNext()) {
			Chunk t = (Chunk) chunkIter.next();
			predictedChunks = predictedChunks + t.getType().getShortName()
					+ ",";
		}
		LOGGER.debug("[testProcess: ] Wanted:" + chunks + "\nPredicted:"
				+ predictedChunks);

		// compare offsets
		assertEquals(chunks, predictedChunks);
		
		

	}
	
	public void testProcessWithDefaultMappings() {

		XMLInputSource chunkerXML = null;
		ResourceSpecifier chunkerSpec = null;
		AnalysisEngine chunkerAnnotator = null;

		try {
			chunkerXML = new XMLInputSource(
					"src/test/resources/ChunkAnnotatorTestDefaultMappings.xml");
			chunkerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					chunkerXML);
			chunkerAnnotator = UIMAFramework.produceAnalysisEngine(chunkerSpec);
		} catch (Exception e) {

			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		JCas jcas = null;
		try {
			jcas = chunkerAnnotator.newJCas();
		} catch (ResourceInitializationException e) {
			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		// get test cas with sentence/token/pos annotation
		initCas(jcas);

		try {
			chunkerAnnotator.process(jcas, null);
		} catch (Exception e) {
			LOGGER.error("[testProcess: ] " + e.getMessage());
			e.printStackTrace();
		}

		// get the offsets of the sentences
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator chunkIter = indexes.getAnnotationIndex(Chunk.type)
				.iterator();

		String predictedChunks = "";

		while (chunkIter.hasNext()) {
			Chunk t = (Chunk) chunkIter.next();
			predictedChunks = predictedChunks + t.getType().getShortName()
					+ ",";
		}
		LOGGER.debug("[testProcess: ] Wanted:" + chunks + "\nPredicted:"
				+ predictedChunks);

		// compare offsets
		assertEquals(chunks, predictedChunks);

	}


}
