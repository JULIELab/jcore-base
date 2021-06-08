/** 
 * OpennlpChunkerTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: buyko
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008 
 * 
 * /Test for OpenNLP Chunker
 **/

package de.julielab.jcore.ae.opennlp.chunk;

import de.julielab.jcore.types.Chunk;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChunkAnnotatorTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChunkAnnotatorTest.class);


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

	@Test
	public void testProcess() {

		XMLInputSource chunkerXML = null;
		ResourceSpecifier chunkerSpec = null;
		AnalysisEngine chunkerAnnotator = null;

		try {
			chunkerXML = new XMLInputSource("src/test/resources/ChunkAnnotatorTest.xml");
			chunkerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(chunkerXML);
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
		Iterator chunkIter = indexes.getAnnotationIndex(Chunk.type).iterator();

		String predictedChunks = "";

		while (chunkIter.hasNext()) {
			Chunk t = (Chunk) chunkIter.next();
			predictedChunks = predictedChunks + t.getType().getShortName() + ",";
		}
		LOGGER.debug("[testProcess: ] Wanted:" + chunks + "\nPredicted:" + predictedChunks);

		// compare offsets
		assertEquals(chunks, predictedChunks);

	}
	@Test
	public void testProcessWithDefaultMappings() {

		XMLInputSource chunkerXML = null;
		ResourceSpecifier chunkerSpec = null;
		AnalysisEngine chunkerAnnotator = null;

		try {
			chunkerXML = new XMLInputSource("src/test/resources/ChunkAnnotatorTestDefaultMappings.xml");
			chunkerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(chunkerXML);
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
		Iterator chunkIter = indexes.getAnnotationIndex(Chunk.type).iterator();

		String predictedChunks = "";

		while (chunkIter.hasNext()) {
			Chunk t = (Chunk) chunkIter.next();
			predictedChunks = predictedChunks + t.getType().getShortName() + ",";
		}
		LOGGER.debug("[testProcess: ] Wanted:" + chunks + "\nPredicted:" + predictedChunks);

		// compare offsets
		assertEquals(chunks, predictedChunks);

	}
	@Test
	public void testPunctuation() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
		AnalysisEngine chunker = AnalysisEngineFactory.createEngine("ChunkAnnotatorTest");
		jcas.setDocumentText("A novel candidate oncogene, MCT-1, is involved in cell cycle progression.");
		new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();
		Token t = new Token(jcas, 0, 1);
		Token t1 = new Token(jcas, 2, 7);
		Token t2 = new Token(jcas, 8, 17);
		Token t3 = new Token(jcas, 18, 26);
		Token t4 = new Token(jcas, 26, 27);
		Token t5 = new Token(jcas, 28, 33);
		Token t6 = new Token(jcas, 33, 34);
		Token t7 = new Token(jcas, 35, 37);
		Token t8 = new Token(jcas, 38, 46);
		Token t9 = new Token(jcas, 47, 49);
		Token t10 = new Token(jcas, 50, 54);
		Token t11 = new Token(jcas, 55, 60);
		Token t12 = new Token(jcas, 61, 72);
		Token t13 = new Token(jcas, 72, 73);
		BiConsumer<Token, String> ps = (token, tag) -> {
			FSArray a = new FSArray(jcas, 1);
			PennBioIEPOSTag p = new PennBioIEPOSTag(jcas);
			p.setValue(tag);
			a.set(0, p);
			token.setPosTag(a);
			token.addToIndexes();
		};
		ps.accept(t, "DT");
		ps.accept(t1, "JJ");
		ps.accept(t2, "NN");
		ps.accept(t3, "NN");
		ps.accept(t4, ",");
		ps.accept(t5, "NN");
		ps.accept(t6, ",");
		ps.accept(t7, "VBZ");
		ps.accept(t8, "VBN");
		ps.accept(t9, "IN");
		ps.accept(t10, "NN");
		ps.accept(t11, "NN");
		ps.accept(t12, "NN");
		ps.accept(t13, ".");

		chunker.process(jcas.getCas());
		
		Set<String> chunks = JCasUtil.select(jcas, Chunk.class).stream().map(c -> c.getCoveredText() + " " + c.getClass().getSimpleName()).collect(Collectors.toSet());
		assertTrue(chunks.contains("A novel candidate oncogene ChunkNP"));
		assertTrue(chunks.contains("MCT-1 ChunkNP"));
		assertTrue(chunks.contains("is involved ChunkVP"));
		assertTrue(chunks.contains("in ChunkPP"));
		assertTrue(chunks.contains("cell cycle progression ChunkNP"));
	}

}
