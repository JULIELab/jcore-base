/** 
 * CoordinationAnnotatorTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: Lichtenwald
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 09.04.2008 
 * 
 * Test for the wrapper for the baseline (prediction of EEEs, coordination elements (conjunctions, conjuncts, antecedents) and resolved ellipses
 * The tested sentence is: Almost all of these mutations occur in X, Y, and Z cells; simple upstream and downstream sequence elements are indicated by negative elements.
 * This sentence contains two EEEs:
 * 1) X, Y, and Z cells (entity: variation-location)
 * 2) simple upstream and downstream sequence elements (entity: DNA)
 **/

package de.julielab.jcore.ae.coordination.main;


import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.FSIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.coordination.main.CoordinationAnnotatorTest;
import de.julielab.jules.types.GeniaPOSTag;
import de.julielab.jules.types.POSTag;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.CoordinationElement;
import de.julielab.jules.types.Token;
import de.julielab.jules.types.EntityMention;
import de.julielab.jules.types.EEE;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class CoordinationAnnotatorTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationAnnotatorTest.class);

	private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";

	public final static String ML = "ml";

	public final static String BASELINE = "baseline";

	public final static String ENTITY = "entity";

	public final static String PHRASE = "phrase";

	public final static String COORDINATION = "coordination";

	public final static String MODE = "mode";

	public final static String OBJECT_OF_ANALYSIS = "objectOfAnalysis";

	public final static String RESULT_OF_ANALYSIS = "resultOfAnalysis";

	public final static String USED_CONJUNCTIONS_FILE_NAME = "usedConjunctionsFileName";

	public final static String USED_CONJUNCTIONS = "usedConjunctions";

	public final static String NULL_STRING = "";

	public static String VALUE_MODE = NULL_STRING;

	public static String VALUE_OBJECT_OF_ANALYSIS = NULL_STRING;

	public static String VALUE_RESULT_OF_ANALYSIS = NULL_STRING;

	private static final String text = "Almost all of these mutations occur in X, Y, and Z cells; simple upstream and downstream sequence elements are indicated by negative elements.";

	private static final String EEE1 = "X, Y, and Z cells";

	private static final String EEE2 = "simple upstream and downstream sequence elements";

	private static final String ellipsis1 = "X cells, Y cells, and Z cells";

	private static final String ellipsis2 = "simple upstream sequence elements and simple downstream sequence elements";

	private static final String coordinationLabels1 = "conjunct,conjunction,conjunct,conjunction,conjunction,conjunct,antecedent";

	private static final String coordinationLabels2 = "antecedent,conjunct,conjunction,conjunct,antecedent,antecedent";

	private static int entityMentionNumber = 5;

	public static final String OUTPUT_DIR = "src/test/resources";

	private static final String TEST_DESC = "src/test/resources/desc/CoordinationAnnotatorDescriptor.xml";

	/*--------------------------------------------------------------------------------------------*/
	protected void setUp() throws Exception {
		super.setUp();
	} // of setUp

	/*--------------------------------------------------------------------------------------------*/
	public void initCas(JCas jcas) {
		jcas.reset();

		/*--------------------------*/
		/* Initialize the sentence. */
		/*--------------------------*/
		jcas.setDocumentText(text);
		Sentence sentence = new Sentence(jcas);
		sentence.setBegin(0);
		sentence.setEnd(text.length());
		sentence.addToIndexes();

		/*--------------------------*/
		/* Initialize the 27 tokens. */
		/*--------------------------*/

		/*--------------*/
		/* Almost|RB */
		/*--------------*/
		initializeToken(0, 6, "RB", jcas);

		/*--------------*/
		/* all|DT */
		/*--------------*/
		initializeToken(7, 10, "DT", jcas);

		/*----------*/
		/* of|IN */
		/*----------*/
		initializeToken(11, 13, "IN", jcas);

		/*--------------*/
		/* these|DT */
		/*--------------*/
		initializeToken(14, 19, "DT", jcas);

		/*------------------*/
		/* mutations|NNS */
		/*------------------*/
		initializeToken(20, 29, "NNS", jcas);

		/*--------------*/
		/* occur|VBP */
		/*--------------*/
		initializeToken(30, 35, "VBP", jcas);

		/*--------------*/
		/* in|IN */
		/*--------------*/
		initializeToken(36, 38, "IN", jcas);

		/*----------*/
		/* X|NN */
		/*----------*/
		initializeToken(39, 40, "NN", jcas);

		/*--------------*/
		/* ,|, */
		/*--------------*/
		initializeToken(40, 41, ",", jcas);

		/*--------------*/
		/* Y|NN */
		/*--------------*/
		initializeToken(42, 43, "NN", jcas);

		/*--------------*/
		/* ,|, */
		/*--------------*/
		initializeToken(43, 44, ",", jcas);

		/*--------------*/
		/* and|CC */
		/*--------------*/
		initializeToken(45, 48, "CC", jcas);

		/*--------------*/
		/* Z|NN */
		/*--------------*/
		initializeToken(49, 50, "NN", jcas);

		/*------------------*/
		/* cells|NNS */
		/*------------------*/
		initializeToken(51, 56, "NNS", jcas);

		/*--------------*/
		/* ;|; */
		/*--------------*/
		initializeToken(56, 57, ";", jcas);

		/*------------------*/
		/* simple|JJ */
		/*--------------*/
		initializeToken(58, 64, "JJ", jcas);

		/*------------------*/
		/* upstream|JJ */
		/*------------------*/
		initializeToken(65, 73, "JJ", jcas);

		/*--------------*/
		/* and|CC */
		/*--------------*/
		initializeToken(74, 77, "CC", jcas);

		/*----------------------*/
		/* downstream|JJ */
		/*----------------------*/
		initializeToken(78, 88, "JJ", jcas);

		/*------------------*/
		/* sequence|NN */
		/*------------------*/
		initializeToken(89, 97, "NN", jcas);

		/*------------------*/
		/* elements|NNS */
		/*------------------*/
		initializeToken(98, 106, "NNS", jcas);

		/*--------------*/
		/* are|VBP */
		/*--------------*/
		initializeToken(107, 110, "VBP", jcas);

		/*----------------------*/
		/* indicated|VBD */
		/*----------------------*/
		initializeToken(111, 120, "VBD", jcas);

		/*--------------*/
		/* by|IN */
		/*--------------*/
		initializeToken(121, 123, "IN", jcas);

		/*------------------*/
		/* negative|JJ */
		/*------------------*/
		initializeToken(124, 132, "JJ", jcas);

		/*------------------*/
		/* elements|NNS */
		/*------------------*/
		initializeToken(133, 141, "NNS", jcas);

		/*--------------*/
		/* .|. */
		/*--------------*/
		initializeToken(141, 142, ".", jcas);

		/*------------------------------*/
		/* Initialize the entityMentions. */
		/*------------------------------*/
		initializeEntityMention(20, 29, "variation-event", jcas, null);
		initializeEntityMention(39, 56, "variation-location", jcas, null);
		initializeEntityMention(58, 106, "DNA", jcas, null);
	} // of initCas

	/*--------------------------------------------------------------------------------------------*/
	public void initializeToken(int begin, int end, String posTagValue, JCas jcas) {
		GeniaPOSTag posTag = null;
		FSArray posTagFSArray = null;
		Token token = new Token(jcas);
		token.setBegin(begin);
		token.setEnd(end);
		posTag = new GeniaPOSTag(jcas);
		posTag.setBegin(begin);
		posTag.setEnd(end);
		posTag.setValue(posTagValue);
		posTag.addToIndexes();
		posTagFSArray = new FSArray(jcas, 1);
		posTagFSArray.set(0, posTag);
		posTagFSArray.addToIndexes();
		token.setPosTag(posTagFSArray);
		token.addToIndexes();
	} // of initializeToken

	/*--------------------------------------------------------------------------------------------*/
	public void initializeEntityMention(int begin, int end, String specificType, JCas jcas, String textualRepresentation) {
		EntityMention entityMention = new EntityMention(jcas);
		entityMention.setBegin(begin);
		entityMention.setEnd(end);
		entityMention.setSpecificType(specificType);
		entityMention.addToIndexes();
		entityMention.setTextualRepresentation(textualRepresentation);
	} // of initializeEntityMention

	/*--------------------------------------------------------------------------------------------*/
	public void testProcess() {
		XMLInputSource descriptor = null;
		ResourceSpecifier specifier = null;
		AnalysisEngine ae = null;

		try {
			descriptor = new XMLInputSource(TEST_DESC);
			specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
			ae = UIMAFramework.produceAnalysisEngine(specifier);
		} // of try
		catch (Exception e) {
			LOGGER.error("TEST PROCESS: " + e.getMessage());
		} // of catch

		JCas jcas = null;

		try {
			jcas = ae.newJCas();
		} // of try
		catch (ResourceInitializationException e) {
			LOGGER.error("TEST PROCESS: " + e.getMessage());
		} // of catch

		initCas(jcas);

		try {
			ae.process(jcas);

			FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + File.separator + "CoordinationAnnotatorTest.xmi");
			XmiCasSerializer.serialize(jcas.getCas(), fos);

			if (ae.getConfigParameterValue(MODE).equals(BASELINE)) {
				assertTrue("Invalid JCas!", checkJCas(jcas, ae));
			} // of if

		} // of try
		catch (Exception e) {
			LOGGER.error("TEST PROCESS: " + e.getMessage());
			e.printStackTrace();
		} // of catch

	} // of testProcess

	/*---------------------------------------------------------------------------*/
	private boolean checkJCas(JCas jcas, AnalysisEngine ae) {
		boolean valid = true;

		if (ae.getConfigParameterValue(RESULT_OF_ANALYSIS).equals(COORDINATION)) {
			if (!checkEEEs(jcas)) {
				return false;
			} // of if
		} // of if

		if (ae.getConfigParameterValue(RESULT_OF_ANALYSIS).equals(ENTITY)) {
			if (!checkEntityMentions(jcas)) {
				return false;
			} // of if
		} // of if

		if (!checkCoordinationelements(jcas)) {
			return false;
		} // of if

		return valid;
	} // of checkJCas

	/*---------------------------------------------------------------------------*/
	private boolean checkCoordinationelements(JCas jcas) {
		AnnotationIndex coordElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
				CoordinationElement.type);
		FSIterator coordElIterator = coordElIndex.iterator();
		int coordElCounter = 0;

		while (coordElIterator.hasNext()) {
			CoordinationElement coordEl = (CoordinationElement) coordElIterator.next();

			if (coordEl.getCat().equals(NULL_STRING)) {
				return false;
			}
			coordElCounter++;
		} // of while

		System.out.println("COORDINATION ELEMENT NUMBER: " + coordElCounter);

		// why there should be 13 coordination elements? Well, each token within the EEE will become a coordination
		// element. Since there are two EEEs, one with 7 tokens (X, Y, and Z cells) and the other with 6 tokens
		// (simple upstream and downstream sequence elements), there are in total 13 coordination elements
		if (!(coordElCounter == 13)) {
			return false;
		} // of if

		return true;
	} // checkCoordinationelements

	/*---------------------------------------------------------------------------*/
	private boolean checkEntityMentions(JCas jcas) {
		AnnotationIndex entityMentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
				EntityMention.type);
		FSIterator entityMentionIterator = entityMentionIndex.iterator();
		int entityMentionCounter = 0;

		while (entityMentionIterator.hasNext()) {
			EntityMention entityMention = (EntityMention) entityMentionIterator.next();
			System.out.println("ENTITY MENTION (covered text)          : " + entityMention.getCoveredText());
			System.out.println("ENTITY MENTION (textual representation): " + entityMention.getTextualRepresentation());
			System.out.println("");
			entityMentionCounter++;
		} // of while

		System.out.println("ENTITY MENTION NUMBER: " + entityMentionCounter);

		// why there must be 6 entity mentons? In this test, there were 3 entity mentions written to the Cas. Two of
		// them were identified to be EEEs. One of the EEE contains 3 conjuncts, the other EEE contains 2 conjuncts.
		// For each conjunct, a new entity mention was created and written to the Cas after the computation, the EEE
		// were removed from the Cas since there were only auxiliary constructs. So in total, there are 3 + 3 + 2 - 2
		// entity mentions
		if (entityMentionCounter == 6) {
			return true;
		} else {
			return false;
		} // of if
	} // checkEntityMentions

	/*---------------------------------------------------------------------------*/
	private boolean checkEEEs(JCas jcas) {
		boolean eeeValid = true;
		AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		ArrayList<EEE> eeeArrayList = new ArrayList<EEE>();
		FSIterator eeeIterator = eeeIndex.iterator();

		while (eeeIterator.hasNext()) {
			EEE eee = (EEE) eeeIterator.next();

			if (!eee.getElliptical()) {
				return false;
			}

			if (eee.getResolved() == NULL_STRING) {
				return false;
			}

			eeeArrayList.add(eee);
		} // of while

		System.out.println("EEE NUMBER: " + eeeArrayList.size());

		if (!checkEEE(eeeArrayList.get(0), EEE1, ellipsis1))
			return false;
		if (!checkEEE(eeeArrayList.get(1), EEE2, ellipsis2))
			return false;

		return eeeValid;
	} // of testEEEs

	/*---------------------------------------------------------------------------*/

	private boolean checkEEE(EEE eee, String coveredText, String ellipsis) {
		boolean eeeValid = true;
		if (!eee.getCoveredText().equals(coveredText)) {
			return false;
		} // of if
		
		if (!eee.getResolved().equals(ellipsis)) {
			return false;
		} // of if
		
		System.out.println("EEE: " + eee.getCoveredText());
		return eeeValid;
	} // of checkEEE

	/*---------------------------------------------------------------------------*/

} // of class CoordinationAnnotatorTest
