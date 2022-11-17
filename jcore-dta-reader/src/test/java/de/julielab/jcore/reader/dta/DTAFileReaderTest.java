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
package de.julielab.jcore.reader.dta;

import com.ximpleware.VTDNav;
import de.julielab.jcore.reader.dta.util.DTAUtils;
import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.extensions.dta.*;
import de.julielab.xml.JulieXMLTools;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DTAFileReaderTest {

	public enum Version {
		v2016("2016_format"), v2017("2017_format");
		private static final String TEST_DIR = "src/test/resources/testfiles_";
		private static final String TEST_FILE_NAME = "/short-arnim_wunderhorn01_1806.tcf.xml";
		private final String versionString;

		String getTestDir() {
			return TEST_DIR + versionString;
		}

		String getTestFileForVersion() {
			return TEST_DIR + versionString + TEST_FILE_NAME;
		}

		Version(String versionString) {
			this.versionString = versionString;
		}
	}

	private static VTDNav getNav(String testFile) throws Exception {
		return JulieXMLTools.getVTDNav(new FileInputStream(new File(testFile)),
				1024);
	}

	public static JCas process(final boolean normalize, Version version)
			throws Exception {
		final String testFile = version.getTestFileForVersion();
		final JCas jcas = JCasFactory.createJCas();
		final VTDNav nav = getNav(testFile);
		DTAFileReader.readDocument(jcas, nav, testFile, normalize);
		DTAFileReader.readHeader(jcas, nav, testFile, version == Version.v2017);
		return jcas;
	}

	private boolean containsClassification(final FSArray classes,
			final Class<? extends DocumentClassification> dc) {
		for (int i = 0; i < classes.size(); ++i)
			if (dc.isInstance(classes.get(i)))
				return true;
		return false;
	}

	@Test
	public void testDoProcessDirectory() throws Exception {
		
		for (Version v : Version.values()) {
			CollectionReader reader = DTAUtils.getReader(v.getTestDir(), true, v == Version.v2017);
			CAS cas = JCasFactory.createJCas().getCas();
			int processed = 0;
			while (reader.hasNext()) {
				reader.getNext(cas);
				processed++;
				cas.reset();
			}
			assertEquals(1, processed);
		}
	}

	@Test
	public void testGetDocumentText() {
		final String expected = "Des Knaben Wunderhorn."
				+ "\nAlte deutſche Lieder geſammelt von L. A. v. Arnim und Clemens Brentano."
				+ "\nDes Knaben Wunderhorn Alte deutſche Lieder L. Achim v. Arnim."
				+ "\nClemens Brentano."
				+ "\nHeidelberg, beÿ Mohr u. Zimmer.bar";
		for (Version version : Version.values()) {
			try {
				final JCas jcas = process(false, version);
				assertEquals(expected, jcas.getDocumentText());
			} catch (final Exception e) {
				e.printStackTrace();
				fail();
			}
		}
	}

	@Test
	public void testGetDocumentTextWithCorrection() {
		final String expected = "Des Knaben Wunderhorn."
				+ "\nAlte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano."
				+ "\nDes Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim."
				+ "\nClemens Brentano." + "\nHeidelberg, bei Mohr u. Zimmer.";
		for (Version version : Version.values()) {
			try {
				final JCas jcas = process(true, version);
				assertEquals(expected, jcas.getDocumentText());
			} catch (final Exception e) {
				e.printStackTrace();
				fail();
			}
		}
	}

	@Test
	public void testHeader() throws Exception {
		for (Version version : Version.values()) {
			final JCas jcas = process(true, version);
			final FSIterator<Annotation> i = jcas
					.getAnnotationIndex(Header.type).iterator();
			final Set<Header> header = new HashSet<>();
			while (i.hasNext())
				header.add((Header) i.next());
			assertEquals(1, header.size());
			final Header h = header.iterator().next();

			// title
			assertEquals("Des Knaben Wunderhorn", h.getTitle());
			assertEquals("Alte deutsche Lieder", h.getSubtitle());
			assertEquals("1", h.getVolume());

			// persons
			assertEquals(2, h.getAuthors().size());
			assertEquals(version == Version.v2017 ? 8 : 6,
					h.getEditors().size());
			final PersonInfo arnim = (PersonInfo) h.getAuthors().get(0);
			assertEquals("Arnim", arnim.getSurename());
			assertEquals("Achim von", arnim.getForename());
			assertEquals("http://d-nb.info/gnd/118504177", arnim.getIdno());

			//classification
			final FSArray classes = h.getClassifications();
			assertEquals(version == Version.v2017 ? 2 : 3, classes.size());
			assertTrue(containsClassification(classes, DTABelletristik.class));
			assertTrue(
					containsClassification(classes, DWDS1Belletristik.class));
			if (version == Version.v2017)
				assertFalse(containsClassification(classes,
						DWDS2Wissenschaft.class));
			else
				assertTrue(containsClassification(classes,
						DWDS2Wissenschaft.class));
			assertTrue(h.getIsCoreCorpus());

			//year
			assertEquals("1806", h.getYear());

			//publisher
			assertEquals("Heidelberg", h.getPublicationPlaces(0));
			assertEquals("Frankfurt", h.getPublicationPlaces(1));
			assertEquals("Mohr u: Zimmer", h.getPublishers().get(0));
		}
	}

	@Test
	public void testLemmata() {
		final String expected = "d Knabe Wunderhorn . alt deutsch Lied sammeln von L. A. v. Arnim und "
				+ "Clemens Brentano . d Knabe Wunderhorn alt deutsch Lied L. Achim v. Arnim . "
				+ "clemens brentano . Heidelberg , bei Mohr u. Zimmer .";
		for (Version version : Version.values()) {
			StringBuilder actual = new StringBuilder();
			try {
				final JCas jcas = process(true, version);
				final FSIterator<Annotation> iter = jcas
						.getAnnotationIndex(Lemma.type).iterator();
				while (iter.hasNext())
					actual.append(((Lemma) iter.next()).getValue()).append(" ");
				assertEquals(expected,
						actual.substring(0, actual.length() - 1));
			} catch (final Exception e) {
				e.printStackTrace();
				fail();
			}
		}
	}

	@Test
	public void testPOS() {
		final String expected = "ART NN NN $. ADJA ADJA NN VVPP APPR NE NE APPR NE KON NE NE $. "
				+ "ART NN NN ADJA ADJA NN NE NE APPRART NE $. FM.la FM.la $. NE $, APPR NN APPR NN $.";
		for (Version version : Version.values()) {
			StringBuilder actual = new StringBuilder();
			try {
				final JCas jcas = process(true, version);
				final FSIterator<Annotation> iter = jcas
						.getAnnotationIndex(STTSPOSTag.type).iterator();
				while (iter.hasNext())
					actual.append(((STTSPOSTag) iter.next()).getValue())
							.append(" ");
				assertEquals(expected,
						actual.substring(0, actual.length() - 1));
			} catch (final Exception e) {
				e.printStackTrace();
				System.out.println(version);
				fail();
			}
		}
	}

	@Test
	public void testSentences() {
		final List<String> expected = Arrays.asList(new String[] {
				"Des Knaben Wunderhorn.",
				"Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano.",
				"Des Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim.",
				"Clemens Brentano.", "Heidelberg, bei Mohr u. Zimmer." });
		for (Version version : Version.values()) {
			List<String> actual = new ArrayList<>();
			try {
				final JCas jcas = process(true, version);
				final FSIterator<Annotation> iter = jcas
						.getAnnotationIndex(Sentence.type).iterator();
				while (iter.hasNext())
					actual.add(iter.next().getCoveredText());
				assertEquals(expected, actual);
			} catch (final Exception e) {
				e.printStackTrace();
				fail();
			}
		}
	}
}
