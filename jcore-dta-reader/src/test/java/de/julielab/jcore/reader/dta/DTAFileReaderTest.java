package de.julielab.jcore.reader.dta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;

import com.ximpleware.VTDNav;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import de.julielab.jcore.types.extensions.dta.DWDS1Belletristik;
import de.julielab.jcore.types.extensions.dta.DWDS2Wissenschaft;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;
import de.julielab.jcore.types.extensions.dta.Header;
import de.julielab.jcore.types.extensions.dta.PersonInfo;
import de.julielab.xml.JulieXMLTools;

public class DTAFileReaderTest {
    private static final String TEST_DIR = "src/test/resources/testfiles/";
    static final String TEST_FILE = TEST_DIR + "short-arnim_wunderhorn01_1806.tcf.xml";
    static final String READER_DESC = "src/main/resources/de/julielab/jcore/reader/dta/desc/jcore-dta-reader.xml";

    static private JCas getJCas() throws Exception {
        return CasCreationUtils.createCas((AnalysisEngineMetaData) getReader().getMetaData()).getJCas();
    }

    private static VTDNav getNav() throws Exception {
        return JulieXMLTools.getVTDNav(new FileInputStream(new File(TEST_FILE)), 1024);
    }

    static private CollectionReader getReader() throws Exception {
        return getReader(TEST_FILE);
    }

    static private CollectionReader getReader(final String file) throws Exception {
        final CollectionReaderDescription readerDescription = UIMAFramework.getXMLParser()
                .parseCollectionReaderDescription(new XMLInputSource(READER_DESC));
        final ConfigurationParameterSettings params = readerDescription.getMetaData()
                .getConfigurationParameterSettings();
        params.setParameterValue("inputFile", file);
        return UIMAFramework.produceCollectionReader(readerDescription);
    }

    public static JCas process(final boolean normalize) throws Exception {
        final JCas jcas = getJCas();
        final VTDNav nav = getNav();
        DTAFileReader.readDocument(jcas, nav, TEST_FILE, normalize);
        DTAFileReader.extractMetaInformation(jcas, nav, TEST_FILE);
        return jcas;
    }

    @Test
    public void testClassification() throws Exception {
        final JCas jcas = process(true);
        final FSIterator<Annotation> i = jcas.getAnnotationIndex(DocumentClassification.type).iterator();
        final Set<DocumentClassification> classes = new HashSet<>();
        while (i.hasNext()) {
            classes.add((DocumentClassification) i.next());
        }
        assertEquals(3, classes.size());

        boolean hasDwds = false;
        for (final DocumentClassification dc : classes)
            if (dc instanceof DWDS1Belletristik) {
                hasDwds = true;
            }
        assertTrue(hasDwds);
        
        boolean has2Dwds = false;
        for (final DocumentClassification dc : classes)
            if (dc instanceof DWDS2Wissenschaft) {
                has2Dwds = true;
            }
        assertTrue(has2Dwds);

        boolean hasDta = false;
        for (final DocumentClassification dc : classes)
            if (dc instanceof DTABelletristik) {
                hasDta = true;
            }
        assertTrue(hasDta);
    }

    @Test
    public void testDoProcessDirectory() throws Exception {
        final CollectionReader reader = getReader(TEST_DIR);
        final CAS cas = getJCas().getCas();
        int processed = 0;
        while (reader.hasNext()) {
            reader.getNext(cas);
            processed++;
            cas.reset();
        }
        assertEquals(1, processed);
    }

    @Test
    public void testDoProcessFile() throws Exception {
        final CollectionReader reader = getReader(TEST_FILE);
        final CAS cas = getJCas().getCas();
        int processed = 0;
        while (reader.hasNext()) {
            reader.getNext(cas);
            processed++;
            cas.reset();
        }
        assertEquals(1, processed);
    }

    @Test
    public void testGetDocumentText() {
        final String expected = "Des Knaben Wunderhorn."
                + "\nAlte deutſche Lieder geſammelt von L. A. v. Arnim und Clemens Brentano."
                + "\nDes Knaben Wunderhorn Alte deutſche Lieder L. Achim v. Arnim." + "\nClemens Brentano."
                + "\nHeidelberg, beÿ Mohr u. Zimmer.";
        try {
            final JCas jcas = process(false);
            assertEquals(expected, jcas.getDocumentText());
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetDocumentTextWithCorrection() {
        final String expected = "Des Knaben Wunderhorn."
                + "\nAlte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano."
                + "\nDes Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim." + "\nClemens Brentano."
                + "\nHeidelberg, bei Mohr u. Zimmer.";
        try {
            final JCas jcas = process(true);
            assertEquals(expected, jcas.getDocumentText());
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testHeader() throws Exception {
        final JCas jcas = process(true);
        final FSIterator<Annotation> i = jcas.getAnnotationIndex(Header.type).iterator();
        final Set<Header> header = new HashSet<>();
        while (i.hasNext()) {
            header.add((Header) i.next());
        }
        assertEquals(1, header.size());
        final Header h = header.iterator().next();

        // title
        assertEquals("Des Knaben Wunderhorn", h.getTitle());
        assertEquals("Alte deutsche Lieder", h.getSubtitle());
        assertEquals("1", h.getVolume());

        // persons
        assertEquals(2, h.getAuthors().size());
        assertEquals(6, h.getEditors().size());
        final PersonInfo arnim = (PersonInfo) h.getAuthors().get(0);
        assertEquals("Arnim", arnim.getSurename());
        assertEquals("Achim von", arnim.getForename());
        assertEquals("http://d-nb.info/gnd/118504177", arnim.getIdno());
    }

    @Test
    public void testLemmata() {
        final String expected = "d Knabe Wunderhorn . alt deutsch Lied sammeln von L. A. v. Arnim und "
                + "Clemens Brentano . d Knabe Wunderhorn alt deutsch Lied L. Achim v. Arnim . "
                + "clemens brentano . Heidelberg , bei Mohr u. Zimmer .";
        final StringBuilder actual = new StringBuilder();
        try {
            final JCas jcas = process(true);
            final FSIterator<Annotation> iter = jcas.getAnnotationIndex(Lemma.type).iterator();
            while (iter.hasNext()) {
                actual.append(((Lemma) iter.next()).getValue()).append(" ");
            }
            assertEquals(expected, actual.substring(0, actual.length() - 1));
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testPOS() {
        final String expected = "ART NN NN $. ADJA ADJA NN VVPP APPR NE NE APPR NE KON NE NE $. "
                + "ART NN NN ADJA ADJA NN NE NE APPRART NE $. FM.la FM.la $. NE $, APPR NN APPR NN $.";
        final StringBuilder actual = new StringBuilder();
        try {
            final JCas jcas = process(true);
            final FSIterator<Annotation> iter = jcas.getAnnotationIndex(STTSPOSTag.type).iterator();
            while (iter.hasNext()) {
                actual.append(((STTSPOSTag) iter.next()).getValue()).append(" ");
            }
            assertEquals(expected, actual.substring(0, actual.length() - 1));
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testSentences() {
        final List<String> expected = Arrays.asList(new String[] { "Des Knaben Wunderhorn.",
                "Alte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano.",
                "Des Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim.", "Clemens Brentano.",
                "Heidelberg, bei Mohr u. Zimmer." });
        final List<String> actual = new ArrayList<>();
        try {
            final JCas jcas = process(true);
            final FSIterator<Annotation> iter = jcas.getAnnotationIndex(Sentence.type).iterator();
            while (iter.hasNext()) {
                actual.add(iter.next().getCoveredText());
            }
            assertEquals(expected, actual);
        } catch (final Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
