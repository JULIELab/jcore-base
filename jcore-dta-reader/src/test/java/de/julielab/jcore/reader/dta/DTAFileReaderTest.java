package de.julielab.jcore.reader.dta;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Test;

import com.ximpleware.ParseException;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReTools;

public class DTAFileReaderTest {
	static final String TEST_FILE = "src/test/resources/testfiles/arnim_wunderhorn01_1806.tcf.xml.short";
	static final String READER_DESC = "src/main/resources/de/julielab/jcore/reader/dta/desc/jcore-dta-reader.xml";

	static private CollectionReader getReader()
			throws ResourceInitializationException, InvalidXMLException,
			IOException, CASException {
		return JCoReTools.getCollectionReader(READER_DESC);
	}

	static private JCas getJCas() throws ResourceInitializationException,
			InvalidXMLException, IOException, CASException {
		return CasCreationUtils.createCas(
				(AnalysisEngineMetaData) getReader().getMetaData()).getJCas();
	}

	static private JCas process(boolean normalize) throws ParseException,
			ResourceInitializationException, InvalidXMLException, CASException,
			IOException {
		JCas jcas = getJCas();
		DTAFileReader.getDocumentText(jcas, TEST_FILE, normalize);
		return jcas;
	}

	@Test
	public void testGetDocumentText() {
		String expected = "Des Knaben Wunderhorn."
				+ "\nAlte deutſche Lieder geſammelt von L. A. v. Arnim und Clemens Brentano."
				+ "\nDes Knaben Wunderhorn Alte deutſche Lieder L. Achim v. Arnim."
				+ "\nClemens Brentano." + "\nHeidelberg, beÿ Mohr u. Zimmer.";
		try {
			JCas jcas = process(false);
			assertEquals(expected, jcas.getDocumentText());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGetDocumentTextWithCorrection() {
		String expected = "Des Knaben Wunderhorn."
				+ "\nAlte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano."
				+ "\nDes Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim."
				+ "\nClemens Brentano." + "\nHeidelberg, bei Mohr u. Zimmer.";
		try {
			JCas jcas = process(true);
			assertEquals(expected, jcas.getDocumentText());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testPOS() {
		String expected = "ART NN NN $. ADJA ADJA NN VVPP APPR NE NE APPR NE KON NE NE $. ART NN NN ADJA ADJA NN NE NE APPRART NE $. FM.la FM.la $. NE $, APPR NN APPR NN $.";
		StringBuilder actual = new StringBuilder();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas.getAnnotationIndex(
					STTSPOSTag.type).iterator();
			while (iter.hasNext())
				actual.append(((STTSPOSTag) iter.next()).getValue())
						.append(" ");
			assertEquals(expected, actual.substring(0, actual.length() - 1));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testLemmata() {
		String expected = "d Knabe Wunderhorn . alt deutsch Lied sammeln von L. A. v. Arnim und Clemens Brentano . d Knabe Wunderhorn alt deutsch Lied L. Achim v. Arnim . clemens brentano . Heidelberg , bei Mohr u. Zimmer .";
		StringBuilder actual = new StringBuilder();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas.getAnnotationIndex(Lemma.type)
					.iterator();
			while (iter.hasNext())
				actual.append(((Lemma) iter.next()).getValue()).append(" ");
			assertEquals(expected, actual.substring(0, actual.length() - 1));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testSentences() {
		List<String> expected = Arrays
				.asList(new String[] {
						"Des Knaben Wunderhorn.",
						"Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano.",
						"Des Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim.",
						"Clemens Brentano.", "Heidelberg, bei Mohr u. Zimmer." });
		List<String> actual = new ArrayList<>();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas
					.getAnnotationIndex(Sentence.type).iterator();
			while (iter.hasNext()) {
				actual.add(iter.next().getCoveredText());
			}
			assertEquals(expected, actual);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
