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
package de.julielab.jcore.ae.lingpipegazetteer.uima;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;

import de.julielab.jcore.ae.lingpipegazetteer.chunking.ChunkerProviderImplAlt;
import de.julielab.jcore.ae.lingpipegazetteer.chunking.OverlappingChunk;
import de.julielab.jcore.ae.lingpipegazetteer.uima.GazetteerAnnotator;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.OntClassMention;

public class GazetteerAnnotatorTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerAnnotatorTest.class);

	private static final String TEXT_PLAIN = "src/test/resources/bio_text.txt";

	private static final String EXACT_ANNOTATOR_DESC = "src/test/resources/ExactGazetteerAnnotatorTest.xml";
	private static final String APPROX_ANNOTATOR_DESC = "src/test/resources/ApproxGazetteerAnnotatorTest.xml";

	/**
	 * tests whether reading the dictionary works
	 */
	/*
	 * public void testReadDictionary() throws AnalysisEngineProcessException,
	 * IOException { LOGGER .info(
	 * "testReadDictionary() - tests for errors when loading and initializing dictionary..."
	 * ); long t1 = System.currentTimeMillis(); GazetteerAnnotator annotator =
	 * new GazetteerAnnotator(); annotator.readDictionary(new File(DICT_FILE));
	 * long t2 = System.currentTimeMillis(); LOGGER.info(
	 * "testReadDictionary() - building dictionary took: " + (t2 - t1) / 1000 +
	 * " secs"); }
	 */
	public void setAbbreviations(JCas myCAS) {

		Abbreviation abbr = new Abbreviation(myCAS, 41, 46);
		abbr.setDefinedHere(true);
		abbr.setExpan("killer cell lectin-like receptor G1");
		Annotation v = new Annotation(myCAS, 4, 39);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(myCAS, 289, 294);
		abbr.setDefinedHere(false);
		abbr.setExpan("killer cell lectin-like receptor G1");
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(myCAS, 428, 433);
		abbr.setDefinedHere(false);
		abbr.setExpan("killer cell lectin-like receptor G1");
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(myCAS, 563, 568);
		abbr.setDefinedHere(true);
		abbr.setExpan("killer cell lectin-like receptor G2");
		v = new Annotation(myCAS, 526, 561);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(myCAS, 679, 684);
		abbr.setDefinedHere(false);
		abbr.setExpan("killer cell lectin-like receptor G2");
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(myCAS, 789, 793);
		abbr.setDefinedHere(true);
		abbr.setExpan("immunoreceptor tyrosine-based inhibitory motif");
		v = new Annotation(myCAS, 741, 787);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();
	}

	/**
	 * tests whether the expected number of entities is found for both exact and
	 * approximate matching
	 */
	public void testProcess() throws AnalysisEngineProcessException, CASException, ResourceConfigurationException,
			InvalidXMLException, ResourceInitializationException, IOException, SAXException {
		AnalysisEngine gazetteerAnnotator = null;

		XMLInputSource taggerXML = null;
		ResourceSpecifier taggerSpec = null;

		try {
			taggerXML = new XMLInputSource(EXACT_ANNOTATOR_DESC);
			taggerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(taggerXML);
			gazetteerAnnotator = UIMAFramework.produceAnalysisEngine(taggerSpec);
		} catch (Exception e) {
			LOGGER.error("testInitialize()", e);
		}

		/*
		 * test exact matching first
		 * 
		 * test also checks whether an acronym match in dictionary has a
		 * respective full form with the same label (=specificType) we expect
		 * four new entities in cas
		 */
		LOGGER.info("testProcess() - testing process for EXACT matching (6 matches expected)...");
		JCas myCAS = gazetteerAnnotator.newJCas();
		myCAS.setDocumentText(readFile2String(new File(TEXT_PLAIN)));
		setAbbreviations(myCAS);

		/*
		 * FSIterator cursor = myCAS.getAnnotationIndex().iterator();
		 * while(cursor.hasNext()) { FeatureStructure fs =
		 * (FeatureStructure)cursor.next(); System.err.println("fs in test: " +
		 * fs); }
		 */
		gazetteerAnnotator.setConfigParameterValue("UseApproximateMatching", false);
		gazetteerAnnotator.setConfigParameterValue("CreateNewDictionary", true);
		gazetteerAnnotator.reconfigure();
		gazetteerAnnotator.process(myCAS);
		JFSIndexRepository indexes = myCAS.getJFSIndexRepository();
		FSIterator<org.apache.uima.jcas.tcas.Annotation> entityIter = indexes.getAnnotationIndex(EntityMention.type)
				.iterator();

		int entCount = 0;
		LOGGER.debug("\n\n+++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++\n");
		while (entityIter.hasNext()) {
			EntityMention e = (EntityMention) entityIter.next();
			LOGGER.debug("entity: " + e.getCoveredText() + "\n" + e);
			entCount++;
		}
		assertEquals(6, entCount);

	}

	@Test
	public void testProcessWithApproximateMatching() throws Exception {
		/*
		 * then test approximate matching
		 * 
		 * we expect 15 new entities in cas!!! This is two more than in earlier
		 * versions. This is because in the text, there is the word "KLRG"
		 * without specifying whether KLRG1 or KLRG2 is meant. Due to the
		 * approximate algorithm, both KLRG1 and 2 match. Since we no longer
		 * reject such cases, we now have both annotations. The other additional
		 * entity comes from the fact that we now use the exact match strategy
		 * to annotation acronyms. Thus, we annotate too much. But the
		 * runtime performance of the overlap strategy is so bad that we will have to
		 * deal with it.
		 */

		AnalysisEngine gazetteerAnnotator = null;
		try {
			XMLInputSource taggerXML = new XMLInputSource(APPROX_ANNOTATOR_DESC);
			ResourceSpecifier taggerSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(taggerXML);
			gazetteerAnnotator = UIMAFramework.produceAnalysisEngine(taggerSpec);
		} catch (Exception e) {
			LOGGER.error("testInitialize()", e);
		}

		LOGGER.info("testProcess() - testing process for APPROX matching (13 matches expected)...");
		JCas myCAS = gazetteerAnnotator.newJCas();
		myCAS.reset();
		myCAS.setDocumentText(readFile2String(new File(TEXT_PLAIN)));
		setAbbreviations(myCAS);
		// gazetteerAnnotator.setConfigParameterValue("UseApproximateMatching",true);
		// gazetteerAnnotator.reconfigure();
		gazetteerAnnotator.process(myCAS);
		JFSIndexRepository indexes = myCAS.getJFSIndexRepository();
		FSIterator<org.apache.uima.jcas.tcas.Annotation> entityIter = indexes.getAnnotationIndex(EntityMention.type)
				.iterator();

		int entCount = 0;
		LOGGER.debug("\n\n+++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++\n");
		Set<String> referenceSet = new HashSet<>();
		while (entityIter.hasNext()) {
			EntityMention e = (EntityMention) entityIter.next();
			referenceSet.add(e.getCoveredText());
			LOGGER.debug("entity: " + e.getCoveredText() + "\n" + e);
			entCount++;
		}
		assertEquals(15, entCount);

		System.out.println(referenceSet);
	}

	@Test
	public void testProcessWithNormalizationAndApproximateMatching() throws Exception {

		/*
		 * Now the same as above but with the alternative method of normalizing
		 * text and dictionary
		 * 
		 * we expect 8 new entities in cas!!!
		 */

		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/normalizegazetteer.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine gazetteerAnnotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_CHECK_ACRONYMS, false, GazetteerAnnotator.PARAM_OUTPUT_TYPE,
				"de.julielab.jcore.types.OntClassMention", GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);

		JCas myCAS = gazetteerAnnotator.newJCas();
		myCAS.reset();
		myCAS.setDocumentText(readFile2String(new File(TEXT_PLAIN)));
		setAbbreviations(myCAS);
		gazetteerAnnotator.process(myCAS);
		JFSIndexRepository indexes = myCAS.getJFSIndexRepository();
		FSIterator<org.apache.uima.jcas.tcas.Annotation> entityIter = indexes.getAnnotationIndex(OntClassMention.type)
				.iterator();

		int entCount = 0;
		LOGGER.debug("\n\n+++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++ OUTPUTTING ENTITIES +++\n");
		Set<String> comparisonSet = new HashSet<>();
		while (entityIter.hasNext()) {
			OntClassMention e = (OntClassMention) entityIter.next();
			comparisonSet.add(e.getCoveredText());
			LOGGER.debug("entity: " + e.getCoveredText() + "\n" + e);
			entCount++;
		}
		assertEquals(8, entCount);
	}

	public static String readFile2String(File myFile) throws FileNotFoundException, IOException {
		StringBuffer buffer = new StringBuffer();
		try (BufferedReader br = new BufferedReader(new FileReader(myFile))) {
			String line = "";
			while ((line = br.readLine()) != null) {
				buffer.append(line + " ");
			}
			return buffer.toString().trim();
		}
	}

	@Test
	public void testAnnotatorWithTextNormalization()
			throws ResourceInitializationException, AnalysisEngineProcessException {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/normalizegazetteer.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);
		JCas jCas = annotator.newJCas();

		jCas.setDocumentText("SHP-1 and killer cell lectin like receptor G2 are evil.");
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		assertTrue("There are no entity annotations in the CAS.", it.hasNext());
		EntityMention em = (EntityMention) it.next();
		assertEquals("Start wrong: ", new Integer(0), new Integer(em.getBegin()));
		assertEquals("End wrong: ", new Integer(5), new Integer(em.getEnd()));
		assertEquals("Wrong type: ", "SHP-1", em.getSpecificType());

		assertTrue("The secnond entity annotations is missing.", it.hasNext());
		em = (EntityMention) it.next();
		assertEquals("Start wrong: ", new Integer(10), new Integer(em.getBegin()));
		assertEquals("End wrong: ", new Integer(45), new Integer(em.getEnd()));
		assertEquals("Wrong type: ", "KLRG2", em.getSpecificType());

		assertFalse("There are too many annotations.", it.hasNext());

		jCas.reset();
		jCas.setDocumentText(
				"The chemical was ((2-n-butyl-6,7-dichloro-2-cyclopentyl-2,3-dihydro-1-oxo-1H-inden-5-yl)oxy)acetic acid.");
		annotator.process(jCas);
		it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		assertTrue("There are no entity annotations in the CAS.", it.hasNext());
		em = (EntityMention) it.next();
		assertEquals("Start wrong: ", new Integer(17), new Integer(em.getBegin()));
		assertEquals("End wrong: ", new Integer(103), new Integer(em.getEnd()));
		assertEquals("Wrong type: ", "CHEM", em.getSpecificType());

		assertFalse("There are too many annotations.", it.hasNext());

		jCas.reset();
		jCas.setDocumentText(
				"The chemical was ((2-n butyl-6,7-dichloro-2 cyclopentyl-2,3-dihydro 1-oxo-1H-inden-5 yl)oxy)acetic acid.");
		annotator.process(jCas);
		it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		assertTrue("There are no entity annotations in the CAS.", it.hasNext());
		em = (EntityMention) it.next();
		assertEquals("Start wrong: ", new Integer(17), new Integer(em.getBegin()));
		assertEquals("End wrong: ", new Integer(103), new Integer(em.getEnd()));
		assertEquals("Wrong type: ", "CHEM", em.getSpecificType());

		assertFalse("There are too many annotations.", it.hasNext());

		jCas.reset();
		jCas.setDocumentText(
				"The chemical was ((3-n-butyl-6,7-dichloro-2-cyclopentyl-2,3-dihydro-1-oxo-1H-inden-5-yl)oxy)acetic acid.");
		annotator.process(jCas);
		it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		assertFalse("There is an annotation in CAS although there shouldnt be.", it.hasNext());

		jCas.reset();
		jCas.setDocumentText("Test-dosing unit KLRg1 killer cell lectin like receptor G2 Parkinson's Disease");
		annotator.process(jCas);
		it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		Integer counter = 0;
		while (it.hasNext()) {
			System.out.println(it.next().getCoveredText());
			counter++;
		}
		assertEquals("Wrong entity count: ", new Integer(4), counter);

	}

	@Test
	public void testAnnotateAcronymsWithFullFormEntity() throws Exception {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/normalizegazetteer.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);
		JCas jCas = annotator.newJCas();

		jCas.setDocumentText("Here we have the short form isnotin dictionary (TSFOTEINITD) indeed.");
		Abbreviation abbr;
		Annotation v;
		abbr = new Abbreviation(jCas, 52, 63);
		abbr.setDefinedHere(true);
		abbr.setExpan("short form isnotin dictionary");
		v = new Annotation(jCas, 17, 50);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();

		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		Integer counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals("Wrong entity count: ", new Integer(1), counter);

		jCas.reset();
		jCas.setDocumentText(
				"Herein we report the cDNA cloning of a human brain 25 kDa lysophospholipid-specific lysophospholipase (hLysoPLA).");

		// This is just as JAcro does it: Two acronyms with different long forms
		abbr = new Abbreviation(jCas, 103, 111);
		abbr.setDefinedHere(true);
		abbr.setExpan("human brain 25 kDa lysophospholipid-specific lysophospholipase");
		v = new Annotation(jCas, 39, 101);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();

		abbr = new Abbreviation(jCas, 104, 111);
		abbr.setDefinedHere(true);
		abbr.setExpan("lysophospholipase");
		v = new Annotation(jCas, 84, 101);
		v.addToIndexes();
		abbr.setTextReference(v);
		abbr.addToIndexes();

		annotator.process(jCas);

		it = jCas.getAnnotationIndex(EntityMention.type).iterator();

		counter = 0;
		while (it.hasNext()) {
			EntityMention next = (EntityMention) it.next();
			counter++;
			if (counter == 1) {
				assertEquals("lysophospholipid-specific lysophospholipase", next.getCoveredText());
			} else if (counter == 2) {
				assertEquals("hLysoPLA", next.getCoveredText());
			}
			assertEquals("GENE", next.getSpecificType());
		}
		assertEquals("Wrong entity count: ", new Integer(1), counter);
	}

	@Test
	public void testAnnotatorWithTextNormalizationMuh()
			throws ResourceInitializationException, AnalysisEngineProcessException {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/normalizegazetteer.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);
		JCas jCas = annotator.newJCas();

		jCas.setDocumentText("We shall now describe our system setup followed by our proposed solution, which is a fully distributed and absolute localization solution specifically designed for both one-hop and multi-hop WSNs. Our considered WSN consists of Ns number of sensors randomly placed onto a map of predefined size with Nb number of beacons. Let 𝕊 and 𝔹 be the sets describing all sensors and beacons respectively, where each sensor is noted as Sensori, i ∈ 𝕊 and each beacon is noted as Beaconj, j ∈ 𝔹. Each node either a sensor or a beacon is noted as Nodep, p ∈ 𝕊 ∪ 𝔹, and vector V⃗p is used to represent the coordinate of Nodep. Beacons are placed onto the map with fixed coordinates V⃗j, where j ∈ 𝔹. We assume that each beacon is aware of its own absolute location. Whereas each sensor is unaware of its own location, and is configured with an initial guess of location unrelated to its actual deployed location. The two-dimensional (2-D) localization problem is the estimation of Ns unknown-location coordinates V⃗i, where i ∈ 𝕊.\n");
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(EntityMention.type).iterator();
while (it.hasNext()) {
	Annotation annotation = (Annotation) it.next();
	System.out.println(annotation.getCoveredText());
}
	}

	@Test
	public void testSontesthalt() throws Exception {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/normalizegazetteer.eg.testdict.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);

		JCas jCas = annotator.newJCas();

		jCas.setDocumentText(
				"Identification of cDNAs encoding two human alpha class glutathione transferases (Gsta4) and the heterologous expression of GSTA4-4.");
		// Acronym acro = new Acronym(jCas, 81, 86);
		// acro.addToIndexes();
		// Annotation longform = new Annotation(jCas, 55, 79);
		// longform.addToIndexes();
		// acro.setTextReference(longform);
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it2 = jCas.getAnnotationIndex(EntityMention.type).iterator();
		while (it2.hasNext()) {
			EntityMention em = (EntityMention) it2.next();
			System.out.println(em.getCoveredText() + " " + em.getSpecificType());
		}

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it = jCas.getAnnotationIndex(EntityMention.type).iterator();
		// TODO we would like to find this hit; one would need a lemmatizer for
		// this because a stemmer doesn't work so
		// well in gene evaluation tests
		// assertTrue(it.hasNext());
		// assertEquals("glutathione transferases", it.next().getCoveredText());
		assertTrue(it.hasNext());
		assertEquals("Gsta4", it.next().getCoveredText());
		assertTrue(it.hasNext());
		assertEquals("GSTA4-4", it.next().getCoveredText());

		jCas.reset();
		jCas.setDocumentText("... dual-specificity Yak1-related kinase proteins.");
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it3 = jCas.getAnnotationIndex(EntityMention.type).iterator();
		while (it3.hasNext()) {
			EntityMention em = (EntityMention) it3.next();
			System.out.println(em.getCoveredText() + " " + em.getSpecificType());
		}

		it = jCas.getAnnotationIndex(EntityMention.type).iterator();
		assertTrue(it.hasNext());
		assertEquals("Yak1", it.next().getCoveredText());
	}

	@Test
	public void testApproximate() throws Exception {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/testApproximate.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);

		JCas jCas = annotator.newJCas();

		jCas.setDocumentText(
				"One component of this intercellular to be the 'AC-to-VU' signal from the presumptive AC that causes the other cell> to become a VU.");
		// Acronym acro = new Acronym(jCas, 81, 86);
		// acro.addToIndexes();
		// Annotation longform = new Annotation(jCas, 55, 79);
		// longform.addToIndexes();
		// acro.setTextReference(longform);
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it2 = jCas.getAnnotationIndex(EntityMention.type).iterator();
		int counter = 0;
		while (it2.hasNext()) {
			EntityMention em = (EntityMention) it2.next();
			LOGGER.debug(em.getCoveredText() + " " + em.getSpecificType());
			counter++;
		}
		assertEquals(2, counter);

	}

	@Test
	public void testGroupOvecrlappingChunks() {
		String chunkedText = "PTH-related peptide (PTHrP) is a secreted protein produced by breast cancer cells both in vivo and in vitro. Because of its structural similarity to PTH at the amino terminus, the two proteins interact with a common cell surface receptor, the PTH/PTHrP receptor. When overproduced by tumor cells, PTHrP enters the circulation, giving rise to the common paraneoplastic syndrome of humoral hypercalcemia of malignancy. Although initially discovered in malignancies, PTHrP is now known to be produced by most cells and tissues in the body. It acts as an autocrine and paracrine mediator of cell proliferation and differentiation, effects which are mediated via the PTH/PTHrP receptor. Recent evidence also has shown that, directly after translation, PTHrP is able to enter the nucleus and/or nucleolus and influence cell cycle progression and apoptosis. In this study, we have either overproduced PTHrP or inhibited endogenous PTHrP production in the breast cancer cell line, MCF-7. Overexpression of PTHrP was associated with an increase in mitogenesis, whereas inhibiting endogenous PTHrP production resulted in decreased cell proliferation. The overexpressed peptide targeted to the perinuclear space. In contrast, PTHrP interaction with the cell surface PTH/PTHrP receptor resulted in decreased cell proliferation in the same cell line. This latter effect is dependent on interaction with the receptor, in that exogenously added PTHrP moieties known not to interact with the receptor had no effect on cell growth. Furthermore, neutralization of added peptide with an anti-PTHrP antiserum completely abolished the growth inhibitory effects. In contrast, this antibody has no effect on the increased proliferation rate of the MCF-7 transfectants that overexpress PTHrP, compared with control cells. The net effect of autocrine/paracrine and intracrine effects of PTHrP in MCF-7 cells overproducing the peptide is accelerated cell growth. These findings have critical implications regarding the role of PTHrP in breast cancer, and they suggest that controlling PTHrP production in breast cancer may be useful therapeutically. ";
		List<Chunk> chunking = new ArrayList<>();
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(662, 665, 0d));
		chunking.add(ChunkFactory.createChunk(666, 671, 0d));
		chunking.add(ChunkFactory.createChunk(662, 680, 0d));

		List<OverlappingChunk> overlappingChunks = GazetteerAnnotator.groupOverlappingChunks(chunking, chunkedText);
		Set<Chunk> bestChunks = new HashSet<>();
		for (OverlappingChunk overlappingChunk : overlappingChunks) {
			List<Chunk> bestChunkList = overlappingChunk.getBestChunks();
			assertEquals(1, bestChunkList.size());
			Chunk bestChunk = bestChunkList.get(0);
			assertFalse(
					"Duplicate best chunk: " + bestChunk + " (\""
							+ chunkedText.subSequence(bestChunk.start(), bestChunk.end()) + "\")",
					bestChunks.contains(bestChunk));
			bestChunks.add(bestChunk);
		}
	}

	@Test
	public void testFilterParenthesis() {
		String chunkText;
		boolean filtered;

		chunkText = "glutathione transferases (";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = ") transferases";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "BTM) alpha";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "interleukin-2 (IL-2";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "interleukin-2 [";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "] interleukin-2";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "interleukin-2 {IL-2";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "interleukin-2 } IL-2";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "BMT {mobile unit]";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);
		chunkText = "GSTA4)";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertTrue(filtered);

		chunkText = "glutathione transferases";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertFalse(filtered);
		chunkText = "Di(hydroxy)-transferase";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertFalse(filtered);
		chunkText = "4[mg]";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertFalse(filtered);
		chunkText = "tri{ferrus} iron";
		filtered = GazetteerAnnotator.filterParenthesis(chunkText);
		assertFalse(filtered);
	}

	@Test
	public void testReadCompressedDictionary() throws Exception {
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				ChunkerProviderImplAlt.class, new File("src/test/resources/testCompressedDict.properties"));
		TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescription("de.julielab.jcore.types.jcore-semantics-mention-types");

		AnalysisEngine annotator = AnalysisEngineFactory.createPrimitive(GazetteerAnnotator.class, tsDesc,
				GazetteerAnnotator.PARAM_OUTPUT_TYPE, "de.julielab.jcore.types.EntityMention",
				GazetteerAnnotator.CHUNKER_RESOURCE_NAME, extDesc);

		JCas jCas = annotator.newJCas();

		jCas.setDocumentText("In this sentence, there is a troll.");
		annotator.process(jCas);

		FSIterator<org.apache.uima.jcas.tcas.Annotation> it2 = jCas.getAnnotationIndex(EntityMention.type).iterator();
		int counter = 0;
		while (it2.hasNext()) {
			EntityMention em = (EntityMention) it2.next();
			LOGGER.debug(em.getCoveredText() + " " + em.getSpecificType());
			counter++;
		}
		assertEquals(1, counter);
	}

}
