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
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;

import java.io.File;
import java.io.FileInputStream;
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
import com.ximpleware.VTDNav;

public class DTAFileReader extends CollectionReader_ImplBase {

	static final String COMPONENT_ID = DTAFileReader.class.getCanonicalName();

	static final String DESCRIPTOR_PARAMTER_INPUTFILE = "inputFile";
	static final String DESCRIPTOR_PARAMTER_NORMALIZE = "normalize";

	private final List<File> inputFiles = new ArrayList<>();

	private int counter = 0;

	private boolean normalize = false;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DTAFileReader.class);

	public void initialize() throws ResourceInitializationException {

		String filename = (String) getConfigParameterValue(DESCRIPTOR_PARAMTER_INPUTFILE);
		Object o = getConfigParameterValue(DESCRIPTOR_PARAMTER_NORMALIZE);
		if (o != null)
			normalize = (boolean) o;
		normalize = true;

		File inputFile = new File(filename);

		if (!inputFile.exists()) {
			new Exception(filename + " does not exist!");
		} else if (inputFile.isFile()
				&& inputFile.getName().endsWith(".tcf.xml"))
			inputFiles.add(inputFile);
		else {
			for (File f : inputFile.listFiles())
				if (f.isFile()
						&& f.getName().endsWith(".tcf.xml"))
					inputFiles.add(f);
		}

		LOGGER.info("Input contains " + inputFiles.size() + " xml files.");

	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		try {
			JCas jcas = aCAS.getJCas();
			readDocument(jcas, inputFiles.get(counter), normalize);
			counter++;
			LOGGER.info("Read file:" + counter);
		} catch (CASException | ParseException | IOException e) {
			throw new CollectionException(e);
		}
	}

	static Iterable<String> getAttributeForEach(final String xmlFileName,
			final VTDNav nav, final String forEachXpath,
			final String attributeXpath) {
		return new Iterable<String>() {

			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					final String text = "text";
					final String attribute = "attribute";
					final List<Map<String, String>> fields = new ArrayList<>();
					final Iterator<Map<String, Object>> tokenIterator;

					{
						fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
								text, JulieXMLConstants.XPATH, "."));
						fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
								attribute, JulieXMLConstants.XPATH,
								attributeXpath));
						tokenIterator = JulieXMLTools.constructRowIterator(nav,
								forEachXpath, fields, xmlFileName);
					}

					@Override
					public boolean hasNext() {
						return tokenIterator.hasNext();
					}

					@Override
					public String next() {
						return (String) tokenIterator.next().get(attribute);
					}

					@Override
					public void remove() {
						throw new IllegalAccessError();
					}
				};
			}
		};

	}

	static Map<String, String> mapAttribute2Text(String xmlFileName,
			VTDNav nav, String forEachXpath, String attributeXpath,
			String conditionAttributeXpath, String conditionAttributeValue) {
		Map<String, String> attribute2text = new HashMap<>();

		final String text = "text";
		final String attribute = "attribute";
		final String conditionAttribute = "conditionAttribute";
		List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, text,
				JulieXMLConstants.XPATH, "."));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, attribute,
				JulieXMLConstants.XPATH, attributeXpath));
		if (conditionAttributeXpath != null && conditionAttributeValue != null)
			fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
					conditionAttribute, JulieXMLConstants.XPATH,
					conditionAttributeXpath));
		Iterator<Map<String, Object>> tokenIterator = JulieXMLTools
				.constructRowIterator(nav, forEachXpath, fields, xmlFileName);
		while (tokenIterator.hasNext()) {
			Map<String, Object> token = tokenIterator.next();
			if (conditionAttributeXpath == null
					|| token.get(conditionAttribute).equals(
							conditionAttributeValue))
				attribute2text.put((String) token.get(attribute),
						(String) token.get(text));
		}

		return attribute2text;
	}

	static Map<String, String> mapAttribute2Text(String xmlFileName,
			VTDNav nav, String forEachXpath, String attributeXpath) {
		return mapAttribute2Text(xmlFileName, nav, forEachXpath,
				attributeXpath, null, null);
	}

	static void readDocument(JCas jcas, File file, boolean normalize)
			throws ParseException, IOException {

		VTDNav nav = JulieXMLTools.getVTDNav(new FileInputStream(file), 1024);
		String xmlFileName = file.getCanonicalPath();
		if (!formatOk(xmlFileName, nav))
			throw new IllegalArgumentException(xmlFileName
					+ " does not conform to assumptions!");

		//<token ID="w1">Des</token>
		Map<String, String> id2token = mapAttribute2Text(xmlFileName, nav,
				"/D-Spin/TextCorpus/tokens/token", "@ID");

		//<lemmas>
		//<lemma tokenIDs="w1">d</lemma>
		Map<String, String> id2lemma = mapAttribute2Text(xmlFileName, nav,
				"/D-Spin/TextCorpus/lemmas/lemma", "@tokenIDs");

		//<tag tokenIDs="w1">ART</tag>  
		//TODO: check if STTS <POStags tagset="stts">
		Map<String, String> id2pos = mapAttribute2Text(xmlFileName, nav,
				"/D-Spin/TextCorpus/POStags/tag", "@tokenIDs");

		//<orthography>
		// <correction tokenIDs="w6" operation="replace">deutsche</correction>
		Map<String, String> id2correction = normalize ? mapAttribute2Text(
				xmlFileName, nav, "/D-Spin/TextCorpus/orthography/correction",
				"@tokenIDs", "@operation", "replace") : null;

		StringBuilder text = new StringBuilder();
		int sentenceStart = 0;
		for (String tokenIDs : getAttributeForEach(xmlFileName, nav,
				"/D-Spin/TextCorpus/sentences/sentence", "@tokenIDs")) {
			boolean first = true;
			for (String id : ((String) tokenIDs).split(" ")) {
				if (!id2token.containsKey(id))
					throw new IllegalArgumentException("Token ID \"" + id
							+ "\" has no associated token!");
				if (!id2pos.containsKey(id))
					throw new IllegalArgumentException("Token \""
							+ id2token.get(id) + "\" with ID \"" + id
							+ "\" has no POS information!");
				if (!id2lemma.containsKey(id))
					throw new IllegalArgumentException("Token \""
							+ id2token.get(id) + "\" with ID \"" + id
							+ "\" has no lemma information!");
				if (first)
					first = false;
				else if (!(id2pos.get(id).equals("$,") || id2pos.get(id)
						.equals("$."))) {
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
		return counter < inputFiles.size();
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(counter, inputFiles.size(),
				Progress.ENTITIES) };
	}

	@Override
	public void close() throws IOException {
	}

	static boolean formatOk(String xmlFileName, VTDNav nav) {
		// Tagset <POStags tagset="stts">
		for (String tagset : mapAttribute2Text(xmlFileName, nav,
				"/D-Spin/TextCorpus/POStags", "@tagset").keySet())
			if (!tagset.equals("stts"))
				return false;
		return true;
	}
}
