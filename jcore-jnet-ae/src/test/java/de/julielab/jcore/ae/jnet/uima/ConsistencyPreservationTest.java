/** 
 * ConsistencyPreservationTest.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
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

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Disease;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.JCoReUtilitiesException;
import junit.framework.TestCase;

/**
 * Please note that in the original test there were "GoodEntityMentions" and
 * "BadEntityMentions". Both types were only used for this test which caused
 * some problems with type system management. Thus, the "GoodEntityMentions"
 * have been changed to "Gene" and the "BadEntityMentionTypes" have been changed
 * to "Disease".
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
		final CAS cas = CasCreationUtils.createCas(
				UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(ENTITY_ANNOTATOR_DESC)));
		final JCas JCas = cas.getJCas();
		initJCas4DoAbbreviationBased(JCas);

		System.out.println("\n\n------------\ninitial CAS\n------------");
		final int initialAnnoCount = listAnnotations(JCas, new Annotation(JCas));
		System.out.println("\n\n------------\ninitial Good Entity Mentions in CAS\n------------");
		final int initialGoodEntityMentionCount = listAnnotations(JCas, new Gene(JCas));
		System.out.println("\n");

		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");

		consistencyPreservation.acroMatch(JCas, entityMentionClassnames);

		System.out.println("\n\n------------\nfinal CAS\n------------");
		final int finalAnnoCount = listAnnotations(JCas, new Annotation(JCas));
		System.out.println("\n\n------------\nfinal Good Entity Mentions in CAS\n------------");
		final int finalGoodEntityMentionCount = listAnnotations(JCas, new Gene(JCas));
		System.out.println("\n");

		/*
		 * what the result should look like: there should be three additional
		 * GoodEntityMention annotations. One on abbrev 2 (6,7) and one on
		 * abbreviation 4 (8,9) and one on full 1 (1,2). We test the number of
		 * GoodEntityMention annotations before and after running the
		 * ConsistencyPreservation mode.
		 */
		final int expectedInitialAnnoCount = 13;
		final int expectedInitialGoodEntityMentionCount = 3;
		final int expectedFinalAnnoCount = 16;
		final int expectedFinalGoodEntityMentionCount = 6;

		assertEquals(expectedInitialAnnoCount, initialAnnoCount);
		assertEquals(expectedInitialGoodEntityMentionCount, initialGoodEntityMentionCount);
		assertEquals(expectedFinalAnnoCount, finalAnnoCount);
		assertEquals(expectedFinalGoodEntityMentionCount, finalGoodEntityMentionCount);

	}

	public void testStringMatch() throws Exception {
		LOGGER.info("testStringMatch() -  starting...");
		final CAS cas = CasCreationUtils.createCas(
				UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(ENTITY_ANNOTATOR_DESC)));
		final JCas JCas = cas.getJCas();

		initJCas4DoStringBased(JCas);
		// set mode string
		final String modeString = ConsistencyPreservation.MODE_STRING;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		System.out.println("CAS before: ");
		listEntityAnnotations(JCas);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.EntityMention");
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

	public void testStringMatch2() throws Exception {
		// This test checks whether the consistence preservation algorithm
		// correctly detects already existing annotations even when there are
		// multiple annotations of the sought entity type at the same location
		// but and the ones with the correct specific type are not up front
		// (that was an issue in the past).
		String text = "BMP-6 inhibits growth of mature B cells";
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText(text);
		// g1 is not relevant to the consistency preseveration and should be
		// ignored
		Gene g1 = new Gene(jCas, 0, 5);
		Gene g2 = new Gene(jCas, 0, 5);
		Gene g3 = new Gene(jCas, 32, 33);

		g1.setSpecificType("DECOY");
		g2.setSpecificType("GENE");
		g3.setSpecificType("GENE");
		g1.addToIndexes();
		g2.addToIndexes();
		g3.addToIndexes();

		final String modeString = ConsistencyPreservation.MODE_STRING;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");
		consistencyPreservation.stringMatch(jCas, entityMentionClassnames, 0);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(Gene.type).iterator();
		// we only should find the original three annotations, nothing should
		// have been added
		int count = 0;
		while (it.hasNext()) {
			@SuppressWarnings("unused")
			Gene g = (Gene) it.next();
			count++;
		}
		assertEquals(3, count);
	}

	public void testStringMatch3() throws Exception {
		// This test checks whether the consistence preservation algorithm
		// correctly detects already existing annotations even when there are
		// multiple annotations of the sought entity type at the same location
		// but and the ones with the correct specific type are not up front
		// (that was an issue in the past).
		String text = "BMP-6 inhibits growth of mature B cells. Also, BMP-6 has other effects on something.";
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText(text);
		// g1 is not relevant to the consistency preseveration and should be
		// ignored
		Gene g1 = new Gene(jCas, 0, 5);
		Gene g2 = new Gene(jCas, 0, 5);
		Gene g3 = new Gene(jCas, 32, 33);

		g1.setSpecificType("DECOY");
		g2.setSpecificType("GENE");
		g3.setSpecificType("GENE");
		g1.addToIndexes();
		g2.addToIndexes();
		g3.addToIndexes();

		final String modeString = ConsistencyPreservation.MODE_STRING;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");
		consistencyPreservation.stringMatch(jCas, entityMentionClassnames, 0);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(Gene.type).iterator();
		// we should find the original three annotations and an extension to the
		// second mention of BMP-6 for GENE and DECOY
		int count = 0;
		while (it.hasNext()) {
			@SuppressWarnings("unused")
			Gene g = (Gene) it.next();
			count++;
		}
		assertEquals(5, count);
	}

	public void testStringMatchTokenBoundaries() throws Exception {
		// This test checks whether the consistency preservation algorithm
		// sticks to token boundaries if the respective mode is on
		// the MODE_STRING ConsistencyPreservation would annotate "upregulated"
		// as a gene in this case (happens all the time in full texts)
		String text = "There is a gene reg. It is upregulated.";
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText(text);
		// g1 is not relevant to the consistency preseveration and should be
		// ignored
		Gene g1 = new Gene(jCas, 16, 19);
		g1.addToIndexes();

		new Token(jCas, 0, 5).addToIndexes();
		new Token(jCas, 6, 8).addToIndexes();
		new Token(jCas, 9, 10).addToIndexes();
		new Token(jCas, 11, 15).addToIndexes();
		new Token(jCas, 16, 19).addToIndexes();
		new Token(jCas, 19, 20).addToIndexes();
		new Token(jCas, 21, 23).addToIndexes();
		new Token(jCas, 24, 26).addToIndexes();
		new Token(jCas, 27, 38).addToIndexes();
		new Token(jCas, 38, 39).addToIndexes();

		final String modeString = ConsistencyPreservation.MODE_STRING_TOKEN_BOUNDARIES;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");
		consistencyPreservation.stringMatch(jCas, entityMentionClassnames, 0);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(Gene.type).iterator();
		// there should be nothing more than the original gene
		int count = 0;
		while (it.hasNext()) {
			@SuppressWarnings("unused")
			Gene g = (Gene) it.next();
			count++;
		}
		assertEquals(1, count);
	}

	public void testStringMatchTokenBoundaries2() throws Exception {
		// Test for multi token entities
		String text = "This is BCA alpha. But we haven't annotated BCA alpha in all cases. Also not some other BCA.";
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText(text);

		// BCA alpha
		Gene g1 = new Gene(jCas, 8, 17);
		g1.setSpecificType("type1");
		g1.addToIndexes();

		// BCA alpha
		Gene g2 = new Gene(jCas, 8, 17);
		g2.setSpecificType("type2");
		g2.addToIndexes();

		// This is only the "alpha". We want to check that this is recognized as overlapping with the consistency candidate "BCA alpha" (second mention)
		Gene g3 = new Gene(jCas, 48, 53);
		g3.setSpecificType("type2");
		g3.addToIndexes();
		
		// We use "organism" because we need a type that is not a ascendant or descendant of Gene since then we would run into subsumption issues
		Organism o1 = new Organism(jCas, 8, 17);
		o1.setSpecificType("type1");
		o1.addToIndexes();

		new Token(jCas, 0, 4).addToIndexes();
		new Token(jCas, 5, 7).addToIndexes();
		new Token(jCas, 8, 11).addToIndexes();
		new Token(jCas, 12, 17).addToIndexes();
		new Token(jCas, 17, 18).addToIndexes();
		new Token(jCas, 19, 22).addToIndexes();
		new Token(jCas, 23, 25).addToIndexes();
		new Token(jCas, 26, 33).addToIndexes();
		new Token(jCas, 34, 43).addToIndexes();
		new Token(jCas, 44, 47).addToIndexes();
		new Token(jCas, 48, 53).addToIndexes();
		new Token(jCas, 54, 56).addToIndexes();
		new Token(jCas, 57, 60).addToIndexes();
		new Token(jCas, 61, 66).addToIndexes();
		new Token(jCas, 66, 67).addToIndexes();
		new Token(jCas, 68, 72).addToIndexes();
		new Token(jCas, 73, 76).addToIndexes();
		new Token(jCas, 77, 81).addToIndexes();
		new Token(jCas, 82, 87).addToIndexes();
		new Token(jCas, 88, 91).addToIndexes();
		new Token(jCas, 91, 92).addToIndexes();

		final String modeString = ConsistencyPreservation.MODE_STRING_TOKEN_BOUNDARIES;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");
		entityMentionClassnames.add("de.julielab.jcore.types.Organism");
		consistencyPreservation.stringMatch(jCas, entityMentionClassnames, 0);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(Gene.type).iterator();
		// there should be nothing more than the original gene
		int count1 = 0;
		int count2 = 0;
		while (it.hasNext()) {
			Gene g = (Gene) it.next();
			if (g.getSpecificType().equals("type1"))
				count1++;
			else if (g.getSpecificType().equals("type2"))
				count2++;
			else
				fail("Invalid specific type");
		}
		assertEquals(2, count1);
		assertEquals(2, count2);
		
		it = jCas.getAnnotationIndex(Organism.type).iterator();
		int oCount = 0;
		while (it.hasNext()) {
			@SuppressWarnings("unused")
			Organism em = (Organism) it.next();
			oCount++;
		}
		assertEquals(2, oCount);
	}
	
	public void testStringMatchTokenBoundaries3() throws Exception {
		// Test for multi token entities with correct prefix but wrong ending
		String text = "Group 1. And Group B.";
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jCas.setDocumentText(text);

		Gene g1 = new Gene(jCas, 0, 7);
		g1.setSpecificType("type1");
		g1.addToIndexes();


		new Token(jCas, 0, 5).addToIndexes();
		new Token(jCas, 6, 7).addToIndexes();
		new Token(jCas, 7, 8).addToIndexes();
		new Token(jCas, 9, 12).addToIndexes();
		new Token(jCas, 13, 18).addToIndexes();
		new Token(jCas, 19, 20).addToIndexes();
		new Token(jCas, 20, 21).addToIndexes();
		
		final String modeString = ConsistencyPreservation.MODE_STRING_TOKEN_BOUNDARIES;
		final ConsistencyPreservation consistencyPreservation = new ConsistencyPreservation(modeString);
		final TreeSet<String> entityMentionClassnames = new TreeSet<String>();
		entityMentionClassnames.add("de.julielab.jcore.types.Gene");
		consistencyPreservation.stringMatch(jCas, entityMentionClassnames, 0);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(Gene.type).iterator();
		// there should be nothing more than the original gene
		int count1 = 0;
		while (it.hasNext()) {
			Gene g = (Gene) it.next();
			if (g.getSpecificType().equals("type1"))
				count1++;
		}
		assertEquals(1, count1);
		
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
