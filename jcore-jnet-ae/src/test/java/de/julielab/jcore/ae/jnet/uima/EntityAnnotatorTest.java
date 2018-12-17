/** 
 * EntityAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.3.5
 * Since version:   1.0
 *
 * Creation date: Nov 29, 2006 
 * 
 * This is a JUnit test for the EntityAnnotator.
 **/

package de.julielab.jcore.ae.jnet.uima;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.AbbreviationLongform;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.index.JCoReCoverIndex;
import de.julielab.jnet.tagger.Unit;
import junit.framework.TestCase;

public class EntityAnnotatorTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityAnnotatorTest.class);

	private static final String PREFIX = "src/test/resources/de/julielab/jcore/ae/jnet/uima/";

	private static final String DESCRIPTOR_TYPESYSTEM = PREFIX+"tsDescriptor.xml";
	private static final String XMI_FILE = PREFIX+"abbrtest.xmi";
	private static final String ENTITY_ANNOTATOR_DESC = PREFIX+"EntityAnnotatorTest.xml";
	private static final String NEGATIVE_LIST = PREFIX+"negativeList";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// PropertyConfigurator.configure("src/test/java/log4j.properties");
	}

	public void testIgnoreLabel() throws ResourceInitializationException {

		// load AE
		final AnalysisEngine ae = loadAE();

		final JCas jcas = ae.newJCas();

		// define the CAS
		// abbreviation on XX
		// no abbreviation on YY
		jcas.setDocumentText("aXXaYYaZZs");
		final Abbreviation a1 = new Abbreviation(jcas, 1, 3);
		final AbbreviationLongform anno = new AbbreviationLongform(jcas, 0, 1);
		a1.setTextReference(anno);
		a1.setExpan("something");
		a1.addToIndexes();

		final Abbreviation a2 = new Abbreviation(jcas, 7, 10);
		a2.setTextReference(anno);
		a2.setExpan("something");
		a2.addToIndexes();

		// load entity annotator
		final EntityAnnotator entityAnnotator = new EntityAnnotator();
		entityAnnotator.abbrevPattern = Pattern.compile(EntityAnnotator.ABBREV_PATTERN);

		JCoReCoverIndex<Abbreviation> abbreviationIndex = new JCoReCoverIndex<>(jcas, Abbreviation.type);

		// now test ignoreLabel
		// case 1: should NOT be ignored as there is an annotation with full
		// form
		final boolean case1 = entityAnnotator.ignoreLabel(jcas, 1, 3, abbreviationIndex);

		// case 2: should be ignored as the abbreviation is not introduced
		final boolean case2 = entityAnnotator.ignoreLabel(jcas, 4, 6, abbreviationIndex);

		// case 3: should NOT be ignored as abbreviation is introduced
		final boolean case3 = entityAnnotator.ignoreLabel(jcas, 7, 10, abbreviationIndex);

		LOGGER.info("case 1, ignore : " + case1);
		LOGGER.info("case 2, ignore : " + case2);
		LOGGER.info("case 3, ignore : " + case3);

		boolean allOK = false;
		if ((case1 == false) && (case2 == true) && (case3 == false))
			allOK = true;
		assertTrue(allOK);
	}

	/**
	 * test whether Annotator can be initialized properly from given descriptor
	 */
	public void testInitialize() {
		LOGGER.debug("testInitialize()");
		AnalysisEngine entityAnnotator = null;

		XMLInputSource taggerXML = null;
		ResourceSpecifier taggerSpec = null;

		try {
			taggerXML = new XMLInputSource(ENTITY_ANNOTATOR_DESC);
			taggerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(taggerXML);
			entityAnnotator = UIMAFramework.produceAnalysisEngine(taggerSpec);
		} catch (final Exception e) {
			LOGGER.error("testInitialize()", e);
		}

		if (entityAnnotator != null)
			assertTrue(true);
		else
			assertTrue(false);

	}

	/**
	 * test whether process method runs successfully. Output must be checked by
	 * a human manually
	 */
	public void testProcess() throws InvalidXMLException, ResourceInitializationException, IOException, SAXException,
			CASException, AnalysisEngineProcessException {
		LOGGER.debug("testProcess()");

		// instantiate entity annotator
		AnalysisEngine entityAnnotator = null;
		try {
			final XMLInputSource taggerXML = new XMLInputSource(ENTITY_ANNOTATOR_DESC);
			final ResourceSpecifier taggerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(taggerXML);
			entityAnnotator = UIMAFramework.produceAnalysisEngine(taggerSpec);
		} catch (final Exception e) {
			LOGGER.error("testInitialize()", e);
		}

		// get XMI from file
		LOGGER.debug("testCreateUnitSentence() - getting CAS");
		final CAS myCAS = getCasFromXMI(XMI_FILE);

		entityAnnotator.process(myCAS);
	}

	/**
	 * This function tests parts of the process method, including building the
	 * unit sentence and removing duplicates. Prediction is "simulated" (labels
	 * are set).
	 */
	public void testSimulatedProcess() throws IllegalAccessException, NoSuchFieldException,
			ResourceInitializationException, InvalidXMLException, IOException, CASException, SAXException {
		LOGGER.debug("testCreateUnitSentence() - starting");

		// expected result
		final int[] expandedSentLength = new int[] { 11, 12, 5 };
		final int[] originalSentLength = new int[] { 11, 7, 5 };

		// make a new annotator
		final EntityAnnotator annotator = new EntityAnnotator();
		annotator.expandAbbr = true;
		annotator.showSegmentConf = false;
		annotator.consistencyPreservation = null;

		// load old XMI
		LOGGER.debug("testSimulatedProcess() - getting CAS");
		final CAS myCAS = getCasFromXMI(XMI_FILE);
		final JCas aJCas = myCAS.getJCas();

		// iterate over sentences
		LOGGER.debug("testSimulatedProcess() - getting data from CAS");
		final JFSIndexRepository indexes = myCAS.getJCas().getJFSIndexRepository();
		final Iterator<org.apache.uima.jcas.tcas.Annotation> sentenceIter = indexes.getAnnotationIndex(Sentence.type)
				.iterator();

		JCoReCoverIndex<Abbreviation> abbreviationIndex = new JCoReCoverIndex<>(aJCas, Abbreviation.type);
		JCoReCoverIndex<Token> tokenIndex = new JCoReCoverIndex<>(aJCas, Token.type);

		int i = 0;
		boolean allOK = true;
		while (sentenceIter.hasNext()) {
			System.out.println("\n\n** next sentence");
			final Sentence sentence = (Sentence) sentenceIter.next();
			final List<Token> tokenList = tokenIndex.search(sentence).collect(Collectors.toList());
			final ArrayList<HashMap<String, String>> metaList = new ArrayList<HashMap<String, String>>();
			for (@SuppressWarnings("unused")
			final Token token : tokenList)
				metaList.add(new HashMap<String, String>());

			// create unit sentence
			de.julielab.jnet.tagger.Sentence unitSentence = annotator.createUnitSentence(tokenList, aJCas, metaList,
					abbreviationIndex, tokenIndex);

			// simiulate prediction
			for (final Unit unit : unitSentence.getUnits())
				if (unit.getRep().equals("interleukin-2"))
					unit.setLabel("some label");
				else if (unit.getRep().equals("t"))
					unit.setLabel("label 1");
				else if (unit.getRep().equals("cell"))
					unit.setLabel("label 2");
				else if (unit.getRep().equals("leukomia"))
					unit.setLabel("label 3");
				else if (unit.getRep().equals("alpha"))
					unit.setLabel("label alpha");
				else if (unit.getRep().equals("-"))
					unit.setLabel("label alpha");
				else if (unit.getRep().equals("beta"))
					unit.setLabel("label alpha");
				else if (unit.getRep().equals("ceta"))
					unit.setLabel("label alpha");
				else
					unit.setLabel("O");

			// show unit sentences
			System.out.println("\nsentence: " + sentence.getCoveredText());
			System.out.println(unitSentence);
			for (final Unit unit : unitSentence.getUnits())
				System.out.println(unit.toString());

			// test for correct size of unit sentences
			if (unitSentence.getUnits().size() != expandedSentLength[i]) {
				LOGGER.error("testSimulatedProcess() - unexpected sentence length for expanded sentence: "
						+ unitSentence.getUnits().size() + "<->" + expandedSentLength[i]);
				allOK = false;
			}

			// remove duplicates
			unitSentence = annotator.removeDuplicatedTokens(unitSentence);
			System.out.println("\nsentence: " + sentence.getCoveredText());
			System.out.println(unitSentence);
			for (final Unit unit : unitSentence.getUnits())
				System.out.println(unit.toString());

			// test for correct size of this unit sentences
			if (unitSentence.getUnits().size() != originalSentLength[i]) {
				LOGGER.error("testSimulatedProcess() - unexpected sentence length for original sentence: "
						+ unitSentence.getUnits().size() + "<->" + originalSentLength[i]);
				allOK = false;
			}
			i++;
		}
		assertTrue(allOK);
	}

	/**
	 * Test whether adding the predicted labels as entities to CAS works
	 * correctly. Testing correct handling of negative list is included, thus
	 * two entity mentions found should not be added to CAS ("NN" with label
	 * "A", and "UU" with any label).
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testWriteToCAS() throws SecurityException, NoSuchFieldException, ResourceInitializationException,
			InvalidXMLException, IOException, CASException, IllegalArgumentException, IllegalAccessException {
		LOGGER.debug("testWriteToCAS()");

		final CAS cas = CasCreationUtils.createCas(
				UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(ENTITY_ANNOTATOR_DESC)));
		final JCas jcas = cas.getJCas();
		jcas.setDocumentText(" YXWVUTSNNOUU");

		final EntityAnnotator entityAnnotator = new EntityAnnotator();

		// use reflection to get private field
		final Field negativeList = entityAnnotator.getClass().getDeclaredField("negativeList");
		negativeList.setAccessible(true);
		negativeList.set(entityAnnotator, new NegativeList(new File(NEGATIVE_LIST)));

		// this HashMap is a global variable in EntityAnnotator and is normally
		// initialized
		// within the initialize() method. In this test cast we have to build
		// our own.
		final HashMap<String, String> entityHashMap = new HashMap<String, String>();
		entityHashMap.put("A", "de.julielab.jcore.types.EntityMention");
		entityHashMap.put("O", "de.julielab.jcore.types.EntityMention");
		entityHashMap.put("B", "de.julielab.jcore.types.EntityMention");

		// use reflection to get private field
		final Field entityMap = entityAnnotator.getClass().getDeclaredField("entityMap");
		entityMap.setAccessible(true);
		entityMap.set(entityAnnotator, entityHashMap);

		// this could be some prediction
		final de.julielab.jnet.tagger.Sentence unitSentence = new de.julielab.jnet.tagger.Sentence();
		// add mock-up units: with: begin, end, covered text, label
		unitSentence.add(new Unit(1, 2, "Y", "A"));
		unitSentence.add(new Unit(2, 3, "X", "A"));
		unitSentence.add(new Unit(3, 4, "W", "B"));
		unitSentence.add(new Unit(4, 5, "V", "O"));
		unitSentence.add(new Unit(5, 6, "U", "A"));
		unitSentence.add(new Unit(6, 7, "T", "A"));
		unitSentence.add(new Unit(7, 8, "S", "B"));
		unitSentence.add(new Unit(8, 9, "N", "A")); // should be ignored due to
													// negative list
		unitSentence.add(new Unit(9, 10, "N", "A"));// should be ignored due to
													// negative list
		unitSentence.add(new Unit(10, 11, "O", "O"));
		unitSentence.add(new Unit(11, 12, "U", "A"));// should be ignored due to
														// negative list
		unitSentence.add(new Unit(12, 13, "U", "A"));// should be ignored due to
														// negative list
		JCoReCoverIndex<Abbreviation> abbreviationIndex = new JCoReCoverIndex<>(jcas, Abbreviation.type);
		entityAnnotator.writeToCAS(unitSentence, jcas, abbreviationIndex);

		// the expected label result
		final HashMap<String, String> result = new HashMap<String, String>();
		result.put("YX", "A");
		result.put("W", "B");
		result.put("UT", "A");
		result.put("S", "B");

		final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		final FSIterator<org.apache.uima.jcas.tcas.Annotation> entityIter = indexes
				.getAnnotationIndex(EntityMention.type).iterator();

		boolean allOK = true;
		int foundMentions = 0;
		while (entityIter.hasNext()) {
			final EntityMention entity = (EntityMention) entityIter.next();
			LOGGER.debug("testWriteToCAS() - covered Text: " + entity.getCoveredText() + " -> specific type: "
					+ entity.getSpecificType());

			final String trueLabel = result.get(entity.getCoveredText());
			LOGGER.debug("testWriteToCAS() - expected specific type: " + trueLabel);
			// if (!entity.getSpecificType().equals(result.get(i))) {
			if ((trueLabel == null) || !entity.getSpecificType().equals(trueLabel)) {
				LOGGER.debug("testWriteToCAS() - wrong annotation found");
				allOK = false;
				break;
			}
			foundMentions++;

		}
		if (foundMentions < result.size()) {
			LOGGER.debug("testWriteToCAS() - found less annotations than expected");
			allOK = false;
		}
		assertTrue(allOK);
	}

	private AnalysisEngine loadAE() {
		AnalysisEngine entityAnnotator = null;

		XMLInputSource taggerXML = null;
		ResourceSpecifier taggerSpec = null;

		try {
			taggerXML = new XMLInputSource(ENTITY_ANNOTATOR_DESC);
			taggerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(taggerXML);
			entityAnnotator = UIMAFramework.produceAnalysisEngine(taggerSpec);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return entityAnnotator;
	}

	/**
	 * helper function. Reads XMI from file.
	 */
	private CAS getCasFromXMI(final String fileName)
			throws InvalidXMLException, ResourceInitializationException, IOException, SAXException {
		CAS cas = null;

		LOGGER.debug("reading from: " + DESCRIPTOR_TYPESYSTEM);
		final TypeSystemDescription tsDesc = getTypeSystemDescription(DESCRIPTOR_TYPESYSTEM);
		cas = CasCreationUtils.createCas(tsDesc, null, null);
		final FileInputStream fis = new FileInputStream(fileName);
		XmiCasDeserializer.deserialize(fis, cas);
		return cas;
	}

	/**
	 * helper function. Gets TypeSystemDescription from TS-Descriptor
	 */
	public TypeSystemDescription getTypeSystemDescription(final String tsDescriptorName)
			throws InvalidXMLException, IOException, ResourceInitializationException {

		TypeSystemDescription tsDesc = null;
		final XMLParser xmlParser = UIMAFramework.getXMLParser();
		tsDesc = (xmlParser).parseTypeSystemDescription(new XMLInputSource(tsDescriptorName));
		return tsDesc;
	} // of getTypeSystemDescription

}
