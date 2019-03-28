/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.linnaeus;

import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LinnaeusSpeciesAnnotatorTest {
	@Test
	public void testSimpleSpeciesRecognition() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
		jCas.setDocumentText("In this text we talk about humans and mice. Because a mouse is no killifish nor a caenorhabditis elegans. Thus, c. elegans is now abbreviated as well as n. furzeri.");

		AnalysisEngine annotator = AnalysisEngineFactory.createEngine(LinnaeusSpeciesAnnotator.class,
				LinnaeusSpeciesAnnotator.PARAM_CONFIG_FILE, "internal:/linnaeus-properties-test.conf");

		annotator.process(jCas);

		FSIterator<Annotation> orgaIt = jCas.getAnnotationIndex(Organism.type).iterator();

		Organism org;
		ResourceEntry resourceEntry;

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("humans", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		assertEquals("9606", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("mice", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		assertEquals("10090", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("mouse", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		assertEquals("10090", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("killifish", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		// Although in this context I would prefer the ID 105023, i.e. N. Furzeri. But N. Furzeri seems mostly not just
		// be described as "killifish", so we live with that for the moment.
		assertEquals("34780", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("caenorhabditis elegans", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		assertEquals("6239", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("c. elegans", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		// Here, disambiguation for c. elegans works. When the long form is not mentioned, another ID is takes.
		assertEquals("6239", resourceEntry.getEntryId());

		assertTrue(orgaIt.hasNext());
		org = (Organism) orgaIt.next();
		assertEquals("n. furzeri", org.getCoveredText());
		resourceEntry = org.getResourceEntryList(0);
		// But here, this does not cause the above "killifish" mention to get this ID. Perhaps this is even right, I
		// just thought one could refer to N. Furzeri as "killifish". However, perhaps it is just said, that it is some
		// kind of killifish but not named as such. I don't know ;-)
		assertEquals("105023", resourceEntry.getEntryId());
	}

}
