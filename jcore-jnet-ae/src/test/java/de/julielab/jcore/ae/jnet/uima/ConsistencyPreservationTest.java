/** 
 * ConsistencyPreservationTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.3.3
 * Since version:   2.2
 *
 * Creation date: Feb 19, 2007 
 * 
 * JUnit test for ConsistencyPreservation
 **/

package de.julielab.jcore.ae.jnet.uima;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.jnet.uima.ConsistencyPreservation;
import de.julielab.jules.types.Abbreviation;
import de.julielab.jules.types.Annotation;
import de.julielab.jules.types.Disease;
import de.julielab.jules.types.EntityMention;
import de.julielab.jules.types.Gene;
import de.julielab.jules.types.ResourceEntry;

/**
 * Please note that in the original test there were "GoodEntityMentions" and "BadEntityMentions". Both types were only
 * used for this test which caused some problems with type system management. Thus, the "GoodEntityMentions" have been
 * changed to "Gene" and the "BadEntityMentionTypes" have been changed to "Disease".
 * 
 * @author faessler
 * 
 */
public class ConsistencyPreservationTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyPreservationTest.class);

	private static final String PREFIX = "src/test/resources/de/julielab/jcore/ae/jnet/uima/";
	private static final String ENTITY_ANNOTATOR_DESC = PREFIX+"EntityAnnotatorTest.xml";

	private void initJCas4DoAbbreviationBased(final JCas jcas) throws Exception {

		// 012345678901
		jcas.setDocumentText("0ABCD1234");

		// full forms for abbreviations
		final Annotation full1 = new Annotation(jcas, 1, 2);// ->no entity anno
		full1.addToIndexes();
		final Annotation full2 = new Annotation(jcas, 2, 3); // ->has
																// interesting
																// entity anno
		full2.addToIndexes();
		final Annotation full3 = new Annotation(jcas, 3, 4);// ->has
															// uninteresting
															// entity anno
		full3.addToIndexes();
		final Annotation full4 = new Annotation(jcas, 4, 5);// ->no entity anno
		full4.addToIndexes();

		// abbreviations
		final Abbreviation abbr1 = new Abbreviation(jcas, 5, 6); // ->has entity
																	// anno
		abbr1.setTextReference(full1);
		abbr1.addToIndexes();
		final Abbreviation abbr2 = new Abbreviation(jcas, 6, 7); // ->has
																	// uninteresting
																	// entity
																	// anno
		abbr2.setTextReference(full2);
		abbr2.addToIndexes();
		final Abbreviation abbr3 = new Abbreviation(jcas, 7, 8); // ->has no
																	// entity
																	// anno
		abbr3.setTextReference(full3);
		abbr3.addToIndexes();
		final Abbreviation abbr4 = new Abbreviation(jcas, 8, 9); // ->has no
																	// entity
																	// anno
		abbr4.setTextReference(full4);
		abbr4.addToIndexes();

		// some entity mentions
		final Gene e1 = new Gene(jcas, 5, 6); // entity
												// to be
												// considered
												// on
		// abbreviation 1
		e1.addToIndexes();

		final Disease e2 = new Disease(jcas, 6, 7); // entity
													// type
													// on
													// abbreviation
													// 2 of
		// uninteresting type
		e2.addToIndexes();

		final Gene e3 = new Gene(jcas, 2, 3); // entity
												// mention
												// of
												// interest
												// on
		// full form of abbrev 2
		e3.addToIndexes();

		final Disease e4 = new Disease(jcas, 3, 4); // uninteresting
													// entity
													// mention
													// on
		// full form of abbrev 3
		e4.addToIndexes();

		final Gene e5 = new Gene(jcas, 4, 5); // entity
												// mention
												// of
												// interest
												// on
		// full form of abbrev 4
		e5.addToIndexes();
	}

	public void testConsistencyPreservation() throws Exception {
		final String modeString = ConsistencyPreservation.MODE_STRING + "," + ConsistencyPreservation.MODE_ACRO2FULL
				+ "," + ConsistencyPreservation.MODE_FULL2ACRO;
		new ConsistencyPreservation(modeString);
	}

	public void testAcroMatch() throws Exception {
		final String modeString = ConsistencyPreservation.MODE_FULL2ACRO + "," + ConsistencyPreservation.MODE_ACRO2FULL;

		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);

		LOGGER.info("testAcroMatch() - starting...");
		final CAS cas = CasCreationUtils.createCas(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
				new XMLInputSource(ENTITY_ANNOTATOR_DESC)));
		final JCas JCas = cas.getJCas();
		initJCas4DoAbbreviationBased(JCas);

		System.out.println("\n\n------------\ninitial CAS\n------------");
		final int initialAnnoCount = listAnnotations(JCas, new Annotation(JCas));
		System.out.println("\n\n------------\ninitial Good Entity Mentions in CAS\n------------");
		final int initialGoodEntityMentionCount = listAnnotations(JCas, new Gene(JCas));
		System.out.println("\n");

		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jules.types.Gene");

		consistencyPreservation.acroMatch(JCas, entityMentionClassnames);

		System.out.println("\n\n------------\nfinal CAS\n------------");
		final int finalAnnoCount = listAnnotations(JCas, new Annotation(JCas));
		System.out.println("\n\n------------\nfinal Good Entity Mentions in CAS\n------------");
		final int finalGoodEntityMentionCount = listAnnotations(JCas, new Gene(JCas));
		System.out.println("\n");

		/*
		 * what the result should look like: there should be three additional GoodEntityMention annotations. One on
		 * abbrev 2 (6,7) and one on abbreviation 4 (8,9) and one on full 1 (1,2). We test the number of
		 * GoodEntityMention annotations before and after running the ConsistencyPreservation mode.
		 */
		final int expectedInitialAnnoCount = 13;
		final int expectedInitialGoodEntityMentionCount = 3;
		final int expectedFinalAnnoCount = 16;
		final int expectedFinalGoodEntityMentionCount = 6;

		boolean allOK = true;
		if (expectedInitialAnnoCount != initialAnnoCount) {
			LOGGER.error("testAcroMatch() - expectedInitialAnnoCount != initialAnnoCount");
			allOK = false;
		} else if (expectedInitialGoodEntityMentionCount != initialGoodEntityMentionCount) {
			LOGGER.error("testAcroMatch() - expectedInitialGoodEntityMentionCount != initialGoodEntityMentionCount");
			allOK = false;
		} else if (expectedFinalAnnoCount != finalAnnoCount) {
			LOGGER.error("testAcroMatch() - expectedFinalAnnoCount != finalAnnoCount");
			allOK = false;
		} else if (expectedFinalGoodEntityMentionCount != finalGoodEntityMentionCount) {
			LOGGER.error("testAcroMatch() - expectedFinalGoodEntityMentionCount != finalGoodEntityMentionCount");
			allOK = false;
		}

		if (allOK)
			LOGGER.info("testAcroMatch() - test passed successfully!");
		assertTrue(allOK);
	}

	public void testStringMatch() throws Exception {
		LOGGER.info("testStringMatch() -  starting...");
		final CAS cas = CasCreationUtils.createCas(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
				new XMLInputSource(ENTITY_ANNOTATOR_DESC)));
		final JCas JCas = cas.getJCas();

		initJCas4DoStringBased(JCas);
		// set mode string
		final String modeString = ConsistencyPreservation.MODE_STRING;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		System.out.println("CAS before: ");
		listEntityAnnotations(JCas);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jules.types.EntityMention");
		consistencyPreservation.stringMatch(JCas, entityMentionClassnames, 0);
		final ArrayList<String> expectedResults = new ArrayList<String>();
		expectedResults.add("type a");
		expectedResults.add("type b");
		expectedResults.add("type a");
		expectedResults.add("type a");
		expectedResults.add("type b");
		expectedResults.add("type a");
		expectedResults.add("type b");
		// now make sure all annotations are at place (we expect 5 chemokine
		// annotations
		final JFSIndexRepository indexes = JCas.getJFSIndexRepository();
		final Iterator<?> entityIter = indexes.getAnnotationIndex(EntityMention.type).iterator();
		int i = 0;
		boolean allOK = true;
		while (entityIter.hasNext()) {
			final EntityMention c = (EntityMention) entityIter.next(); //
			System.out.println(c + "\n" + expectedResults.get(i));
			if (!c.getSpecificType().equals(expectedResults.get(i)))
				allOK = false;
			i++;
		}
		System.out.println("\n\n---------\ninitial CAS\n--------");
		listEntityAnnotations(JCas);
		if (allOK)
			LOGGER.info("testStringMatch() - test passed successfully!");
		assertTrue(allOK);
	}

	/*
	 * helper functions
	 */

	private void initJCas4DoStringBased(final JCas jcas) throws Exception {

		// 012345678901
		jcas.setDocumentText("ABACDEAFBABD.");
		// a f af a
		final EntityMention e1 = new EntityMention(jcas); // A
		e1.setBegin(0);
		e1.setEnd(1);
		e1.setSpecificType("type a");
		e1.setTextualRepresentation("my entity 1");
		final FSArray resourceEntryList = new FSArray(jcas, 1);
		final ResourceEntry resourceEntry = new ResourceEntry(jcas);
		resourceEntry.setSource("swissprot");
		resourceEntry.setEntryId("P12345");
		resourceEntryList.set(0, resourceEntry);
		e1.setResourceEntryList(resourceEntryList);
		e1.addToIndexes();

		final EntityMention e2 = new EntityMention(jcas); // B
		e2.setBegin(1);
		e2.setEnd(2);
		e2.setTextualRepresentation("my entity no 2");
		e2.setSpecificType("type b");
		e2.addToIndexes();

	}

	private void listEntityAnnotations(final JCas jcas) {
		final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		final Iterator<?> entityIter = indexes.getAnnotationIndex(EntityMention.type).iterator();
		while (entityIter.hasNext()) {
			final EntityMention e = (EntityMention) entityIter.next();
			System.out
					.println(e.getCoveredText() + " (" + e.getBegin() + "-" + e.getEnd() + ") " + e.getSpecificType());

			// System.out.println(e.toString());

		}
	}

	/**
	 * shows all annotations and returns the number of annotations
	 */
	private int listAnnotations(final JCas jcas, final Annotation annoType) {
		int count = 0;
		final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		final Iterator<?> iter = indexes.getAnnotationIndex(annoType.getTypeIndexID()).iterator();
		while (iter.hasNext()) {
			count++;
			final Annotation e = (Annotation) iter.next();
			System.out.println(e.getCoveredText() + " (" + e.getBegin() + "-" + e.getEnd() + ") "
					+ e.getClass().getCanonicalName());

		}
		return count;
	}
}
