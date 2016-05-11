/**
 * Copyright (c) 2015, JULIE Lab. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the GNU Lesser
 * General Public License (LGPL) v3.0
 *
 * 
 * @author hellrich
 *
 */

package de.julielab.jcore.reader.dta;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.xml.FileTooBigException;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.ximpleware.ParseException;

public class DTAFileReader extends CollectionReader_ImplBase {
	
	static final String COMPONENT_ID = DTAFileReader.class.getCanonicalName();

	public static final String DESCRIPTOR_PARAMTER_INPUTFILE = "inputFile";

	private String filename;

	private File inputFile;

	private int fileCount = 0;

	private int counter = 0;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DTAFileReader.class);

	public DTAFileReader() {

	}

	public void initialize() throws ResourceInitializationException {

		filename = (String) getConfigParameterValue(DESCRIPTOR_PARAMTER_INPUTFILE);

		inputFile = new File(filename);

		if (!inputFile.exists()) {
			new Exception("DIRECTORY_NOT_FOUND!");
		}

		LOGGER.info("Input file contains " + fileCount + " documents.");

	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {

		JCas jcas = null;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			try {
				throw new Exception("could not get jcas", e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		LOGGER.info("Document-Number:" + counter);

		counter++;

	}

	static Map<String, String> getMapping(String xmlFile, String forEachXpath,
			String attributeXpath) {
		Map<String, String> attribute2text = new HashMap<>();

		final String text = "text";
		final String attribute = "attribute";
		List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, text,
				JulieXMLConstants.XPATH, "."));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, attribute,
				JulieXMLConstants.XPATH, attributeXpath));

		Iterator<Map<String, Object>> tokenIterator = JulieXMLTools
				.constructRowIterator(xmlFile, 1024, forEachXpath, fields,
						false);
		while (tokenIterator.hasNext()) {
			Map<String, Object> token = tokenIterator.next();
			attribute2text.put((String) token.get(attribute),
					(String) token.get(text));
		}

		return attribute2text;
	}

	static void getDocumentText(JCas jcas, String xmlFile,
			boolean normalize) throws ParseException, FileTooBigException,
			FileNotFoundException {

		//<token ID="w1">Des</token>
		Map<String, String> id2token = getMapping(xmlFile,
				"/D-Spin/TextCorpus/tokens/token", "@ID");

		//<lemmas>
		//<lemma tokenIDs="w1">d</lemma>
		Map<String, String> id2lemma = getMapping(xmlFile,
				"/D-Spin/TextCorpus/lemmas/lemma", "@tokenIDs");

		//<tag tokenIDs="w1">ART</tag>  
		//TODO: check if STTS <POStags tagset="stts">
		Map<String, String> id2pos = getMapping(xmlFile,
				"/D-Spin/TextCorpus/POStags/tag", "@tokenIDs");

		//<orthography>
		// <correction tokenIDs="w6" operation="replace">deutsche</correction>
		Map<String, String> id2correction = normalize ? getMapping(xmlFile,
				"/D-Spin/TextCorpus/orthography/correction", "@tokenIDs")
				: null;

		StringBuilder text = new StringBuilder();
		List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "tokenIDs",
				JulieXMLConstants.XPATH, "@tokenIDs"));
		Iterator<Map<String, Object>> sentenceIterator = JulieXMLTools
				.constructRowIterator(xmlFile, 1024,
						"/D-Spin/TextCorpus/sentences/sentence", fields, false);
		int sentenceStart = 0;
		while (sentenceIterator.hasNext()) {
			boolean first = true;
			for (Object tokenIDs : sentenceIterator.next().values())
				for (String id : ((String) tokenIDs).split(" ")) {
					if (!id2token.containsKey(id))
						throw new IllegalArgumentException("Token ID \""
								+ id + "\" has no associated token!");
					if (!id2pos.containsKey(id))
						throw new IllegalArgumentException("Token \""
								+ id2token.get(id) + "\" with ID \""
								+ id + "\" has no POS information!");
					if (!id2lemma.containsKey(id))
						throw new IllegalArgumentException("Token \""
								+ id2token.get(id) + "\" with ID \""
								+ id + "\" has no lemma information!");
					if (first)
						first = false;
					else if (!(id2pos.get(id).equals("$,") || id2pos.get(
							id).equals("$."))) {
						text.append(" ");
					}

					int begin = text.length();
					if (normalize && id2correction.containsKey(id))
						text.append(id2correction.get(id));
					else
						text.append(id2token.get(id));
					int end = text.length();
					
					Token token = new Token(jcas, begin, end);
					token.setComponentId(COMPONENT_ID);

					Lemma lemma = new Lemma(jcas, begin, end);
					lemma.setValue(id2lemma.get(id));
					lemma.addToIndexes();
					token.setLemma(lemma);
					
					STTSPOSTag tag = new STTSPOSTag(jcas, begin, end);
					tag.setValue(id2pos.get(id));
					tag.setBegin(begin);
					tag.setEnd(end);
					tag.setComponentId(COMPONENT_ID);
		            tag.addToIndexes();
		            FSArray postags = new FSArray(jcas, 1);
					postags.set(0, tag);
					token.setPosTag(postags);
					
					token.addToIndexes();
				}
			Sentence sentence = new Sentence(jcas, sentenceStart, text.length());
			sentence.setComponentId(COMPONENT_ID);
			sentence.addToIndexes();
			text.append("\n");
			sentenceStart = text.length();
		}
		jcas.setDocumentText(text.subSequence(0, text.length() - 1).toString()); //No final newline
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return counter < fileCount;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(counter, fileCount,
				Progress.ENTITIES) };
	}

	@Override
	public void close() throws IOException {
	}

}
