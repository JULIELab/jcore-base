package de.julielab.jcore.reader.dta;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Test;

import com.ximpleware.ParseException;
import com.ximpleware.VTDNav;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import de.julielab.jcore.types.extensions.dta.DWDS1Belletristik;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;
import de.julielab.jcore.types.extensions.dta.Header;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.FileTooBigException;
import de.julielab.xml.JulieXMLTools;

public class DTAFileReaderTest {
	private static final String TEST_DIR = "src/test/resources/testfiles/";
	static final String TEST_FILE = TEST_DIR + "short-arnim_wunderhorn01_1806.tcf.xml";
	static final String READER_DESC = "src/main/resources/de/julielab/jcore/reader/dta/desc/jcore-dta-reader.xml";

	static private CollectionReader getReader()
			throws ResourceInitializationException, InvalidXMLException, IOException, CASException {
		return JCoReTools.getCollectionReader(READER_DESC);
	}

	static private CollectionReader getReader(String parameter, Object value) throws ResourceInitializationException,
			InvalidXMLException, IOException, CASException, ResourceConfigurationException {
		CollectionReader reader = getReader();
		reader.setConfigParameterValue(parameter, value);
		reader.reconfigure();
		return reader;
	}
	
	private static VTDNav getNav() throws ParseException, FileTooBigException, FileNotFoundException {
		return JulieXMLTools.getVTDNav(new FileInputStream(new File(TEST_FILE)), 1024);
	}

	static private JCas getJCas()
			throws ResourceInitializationException, InvalidXMLException, IOException, CASException {
		return CasCreationUtils.createCas((AnalysisEngineMetaData) getReader().getMetaData()).getJCas();
	}

	public static JCas process(boolean normalize)
			throws ParseException, ResourceInitializationException, InvalidXMLException, CASException, IOException {
		JCas jcas = getJCas();
		VTDNav nav = getNav();
		DTAFileReader.readDocument(jcas, nav, TEST_FILE, normalize);
		DTAFileReader.extractMetaInformation(jcas, nav,
		TEST_FILE);
		return jcas;
	}

	@Test
	public void testDoProcessDirectory() throws ResourceInitializationException, InvalidXMLException, CASException,
			IOException, CollectionException, ResourceConfigurationException {
		CollectionReader reader = getReader(DTAFileReader.DESCRIPTOR_PARAMTER_INPUTFILE, TEST_DIR);
		CAS cas = getJCas().getCas();
		int processed = 0;
		while (reader.hasNext()) {
			reader.getNext(cas);
			processed++;
		}
		assertEquals(1, processed);
	}

	@Test
	public void testDoProcessFile() throws ResourceInitializationException, InvalidXMLException, CASException,
			IOException, CollectionException, ResourceConfigurationException {
		CollectionReader reader = getReader(DTAFileReader.DESCRIPTOR_PARAMTER_INPUTFILE, TEST_FILE);
		CAS cas = getJCas().getCas();
		int processed = 0;
		while (reader.hasNext()) {
			reader.getNext(cas);
			processed++;
		}
		assertEquals(1, processed);
	}

	@Test
	public void testGetDocumentText() {
		String expected = "Des Knaben Wunderhorn."
				+ "\nAlte deutſche Lieder geſammelt von L. A. v. Arnim und Clemens Brentano."
				+ "\nDes Knaben Wunderhorn Alte deutſche Lieder L. Achim v. Arnim." + "\nClemens Brentano."
				+ "\nHeidelberg, beÿ Mohr u. Zimmer.";
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
				+ "\nDes Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim." + "\nClemens Brentano."
				+ "\nHeidelberg, bei Mohr u. Zimmer.";
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
		String expected = "ART NN NN $. ADJA ADJA NN VVPP APPR NE NE APPR NE KON NE NE $. "
				+ "ART NN NN ADJA ADJA NN NE NE APPRART NE $. FM.la FM.la $. NE $, APPR NN APPR NN $.";
		StringBuilder actual = new StringBuilder();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas.getAnnotationIndex(STTSPOSTag.type).iterator();
			while (iter.hasNext())
				actual.append(((STTSPOSTag) iter.next()).getValue()).append(" ");
			assertEquals(expected, actual.substring(0, actual.length() - 1));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testLemmata() {
		String expected = "d Knabe Wunderhorn . alt deutsch Lied sammeln von L. A. v. Arnim und "
				+ "Clemens Brentano . d Knabe Wunderhorn alt deutsch Lied L. Achim v. Arnim . "
				+ "clemens brentano . Heidelberg , bei Mohr u. Zimmer .";
		StringBuilder actual = new StringBuilder();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas.getAnnotationIndex(Lemma.type).iterator();
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
		List<String> expected = Arrays.asList(new String[] { "Des Knaben Wunderhorn.",
				"Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano.",
				"Des Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim.", "Clemens Brentano.",
				"Heidelberg, bei Mohr u. Zimmer." });
		List<String> actual = new ArrayList<>();
		try {
			JCas jcas = process(true);
			FSIterator<Annotation> iter = jcas.getAnnotationIndex(Sentence.type).iterator();
			while (iter.hasNext()) {
				actual.add(iter.next().getCoveredText());
			}
			assertEquals(expected, actual);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testClassification() throws ResourceInitializationException, InvalidXMLException, CASException,
			ParseException, FileTooBigException, FileNotFoundException, IOException {
		JCas jcas = process(true);
		FSIterator<Annotation> i = jcas.getAnnotationIndex(DocumentClassification.type).iterator();
		Set<DocumentClassification> classes = new HashSet<>();
		while(i.hasNext()){
			classes.add((DocumentClassification) i.next());
		}
		assertEquals(2, classes.size());
		
		boolean hasDwds = false;
		for(DocumentClassification dc : classes)
			if(dc instanceof DWDS1Belletristik)
				hasDwds = true;
		assertTrue(hasDwds);
		
		boolean hasDta = false;
		for(DocumentClassification dc : classes)
			if(dc instanceof DTABelletristik)
				hasDta = true;
		assertTrue(hasDta);
	}
	
	@Test
	public void testHeader() throws ResourceInitializationException, InvalidXMLException, CASException,
			ParseException, FileTooBigException, FileNotFoundException, IOException {
		JCas jcas = process(true);
		FSIterator<Annotation> i = jcas.getAnnotationIndex(Header.type).iterator();
		Set<Header> header = new HashSet<>();
		while(i.hasNext()){
			header.add((Header) i.next());
		}
		assertEquals(1, header.size());
	}
	
	@Test
	public void testTitle() throws ResourceInitializationException, InvalidXMLException, CASException,
			ParseException, FileTooBigException, FileNotFoundException, IOException {
		JCas jcas = process(true);
		Header h = (Header) jcas.getAnnotationIndex(Header.type).iterator().next();
		assertEquals("Des Knaben Wunderhorn", h.getTitle());
		assertEquals("Alte deutsche Lieder", h.getSubtitle());
		assertEquals("1", h.getVolume());
	}
}
