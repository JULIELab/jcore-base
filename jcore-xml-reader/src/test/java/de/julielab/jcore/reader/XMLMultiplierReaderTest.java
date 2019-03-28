/**
 * XMLMultiplierReaderTest.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: muehlhausen
 * <p>
 * Current version: 1.9
 * Since version:   1.0
 * <p>
 * Creation date: Dec 11, 2006
 * <p>
 * Test for class de.julielab.jcore.reader.XMLReader
 **/

package de.julielab.jcore.reader;

import de.julielab.jcore.reader.xml.XMLMultiplierReader;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.casmultiplier.JCoReURI;
import de.julielab.jcore.types.pubmed.Header;
import junit.framework.TestCase;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Test for class XML Reader
 */
public class XMLMultiplierReaderTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLMultiplierReaderTest.class);

    private static final boolean DEBUG_MODE = true;

    /**
     * Path to the MedlineReader descriptor without inputDir parameter (and no single file attribute)
     */
    private static final String DESC_XML_MULTIPLIER_READER_DIR = "src/test/resources/XMLMultiplierReader";


    private static final String DIR_CAS_OUTPUT = "src/test/resources/medlineXML/";
    /**
     * Object to be tested
     */
    private CollectionReader xmlMultiplierReader;
    /**
     * CAS array with CAS objects that where processed by the medlineReader
     */
    private ArrayList<CAS> cases = new ArrayList<CAS>();

    /**
     * Default constructor
     */
    public XMLMultiplierReaderTest() {
        super();
        if (DEBUG_MODE) {
            LOGGER.info("XMLMultiplierReader test is in DEBUG_MODE !!!!!!!!!!!!");
        }
    }

    public void testZipInput() throws UIMAException, IOException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types",
                "org.apache.uima.ducc.FlowControllerTS");
        CollectionReader reader = CollectionReaderFactory.createReader(XMLMultiplierReader.class,
                XMLMultiplierReader.PARAM_INPUT_DIR, "src/test/resources/zipped",
                XMLMultiplierReader.PARAM_FILE_NAME_REGEX, new String[]{".*\\.txt"},
                XMLMultiplierReader.PARAM_SEARCH_IN_ZIP, true);
        Set<String> expectedFileNames = new HashSet<>(Arrays.asList("file.txt", "file1.txt", "file2.txt", "file3.txt", "fileindir1.txt", "fileindir2.txt"));
        Set<String> foundFileNames = new HashSet<>();
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            JCoReURI jCoReURI = JCasUtil.selectSingle(jCas, JCoReURI.class);
            boolean found = false;
            for (Iterator<String> it = expectedFileNames.iterator(); it.hasNext(); ) {
                String fileName = it.next();
                if (jCoReURI.getUri().endsWith(fileName)) {
                    found = true;
                    assertTrue("File name " + fileName + " was already found", foundFileNames.add(fileName));
                }
            }
            assertTrue("The URI " + jCoReURI.getUri()+ " was not matched by any expected file names", found);
            jCas.reset();
        }
        assertThat(expectedFileNames).isEqualTo(foundFileNames);
    }

    /**
     * Test the reading of a single file instead of a directory
     *
     * @throws ResourceInitializationException
     */
    public void testGetNextCas_singleFile() throws Exception {
        xmlMultiplierReader = CollectionReaderFactory.createReader(DESC_XML_MULTIPLIER_READER_DIR,
                XMLMultiplierReader.PARAM_INPUT_FILE, "src/test/resources/pubmedXML/pubmedsample18n0001.xml.gz");

        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types",   "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types",  "org.apache.uima.ducc.FlowControllerTS");
        xmlMultiplierReader.getNext(cas.getCas());
        FSIterator<Annotation> it = cas.getAnnotationIndex(JCoReURI.type).iterator();
        if (it.hasNext()) {
            JCoReURI fileType = (JCoReURI) it.next();
            System.out.println(fileType.getUri());
        }

    }

    public void testGetNextCas_directory() throws Exception {
        xmlMultiplierReader = CollectionReaderFactory.createReader(DESC_XML_MULTIPLIER_READER_DIR,
                XMLMultiplierReader.PARAM_INPUT_DIR, "src/test/resources/pubmedXML/");
        File directory = new File("src/test/resources/pubmedXML/");
        int i = 0;
        assertTrue(xmlMultiplierReader.hasNext());
        while (xmlMultiplierReader.hasNext()) {
            JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types",
                    "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types",
                    "org.apache.uima.ducc.FlowControllerTS");
            xmlMultiplierReader.getNext(cas.getCas());
            FSIterator<Annotation> it = cas.getAnnotationIndex(JCoReURI.type).iterator();
            assertTrue(it.hasNext());
            if (it.hasNext()) {
                JCoReURI fileType = (JCoReURI) it.next();
                assertEquals(directory.listFiles()[i].toURI().toString(), fileType.getUri());
                i++;
            }
        }

    }

    /**
     * Create a {@link CollectionReader}
     *
     * @param descAnalysisEngine
     * @return
     * @throws ResourceInitializationException
     * @throws IOException
     * @throws InvalidXMLException
     * @throws Exception                       e.g. ResourceInitializationException
     */
    private CollectionReader getCollectionReader(String descAnalysisEngine) throws Exception {
        CollectionReader collectionReader = null;
        XMLInputSource source;
        source = new XMLInputSource(descAnalysisEngine);
        ResourceSpecifier resourceSpecifier = (ResourceSpecifier) UIMAFramework.getXMLParser().parse(source);
        collectionReader = UIMAFramework.produceCollectionReader(resourceSpecifier);
        return collectionReader;
    }


    private boolean checkDoi(CAS cas, String expectedDoi) {
        Iterator<Annotation> iter = getTypeIterator(cas, Header.type);
        while (iter.hasNext()) {
            Header header = (Header) iter.next();
            if (header.getDoi().equals(expectedDoi)) {
                return true;
            } else {
                LOGGER.warn("Expected DOI: " + expectedDoi + ", actual:" + header.getDoi());
            }
        }
        return false;
    }

    private boolean checkPubTypeList(CAS cas, String[] expectedPubTypes) {
        Iterator<Annotation> iter = getTypeIterator(cas, Journal.type);
        assertTrue(iter.hasNext());
        List<String> expectedPubTypeList = Arrays.asList(expectedPubTypes);
        int checkCount = 0;
        while (iter.hasNext()) {
            Journal journal = (Journal) iter.next();
            if (!expectedPubTypeList.contains(journal.getName())) {
                return false;
            }
            checkCount++;
        }
        if (checkCount != expectedPubTypes.length) {
            LOGGER.warn("Did not found all expected PubTypes. expected: " + expectedPubTypes.length + ", actual:"
                    + checkCount);
            return false;
        }
        return true;
    }

    private void checkJournalTitle(CAS cas, String expectedJournalTitle) {
        Iterator<Annotation> iter = getTypeIterator(cas, Journal.type);
        assertTrue(iter.hasNext());
        while (iter.hasNext()) {
            Journal journal = (Journal) iter.next();
            assertEquals(expectedJournalTitle, journal.getTitle());
        }
    }

    /**
     * Checks if Header is contained correctly in the CAS
     *
     * @param cas        the CAS to be tested
     * @param headerData test data for the header Type
     * @return true, if test data is correctly contained in CAS
     */
    private boolean checkHeader(CAS cas, String[] headerData) {

        Iterator<Annotation> iter = getTypeIterator(cas, Header.type);

        Header header = (Header) iter.next();
        if (header.getAuthors() == null || header.getPubTypeList() == null
                || !header.getCitationStatus().equals(headerData[0]) || !header.getLanguage().equals(headerData[1])
                || !header.getSource().endsWith(headerData[2])) {

            return false;
        }
        // it is wrong if there is more than one header
        if (iter.hasNext()) {
            return false;
        }
        return true;
    }


    /**
     * Gets an Iterator<Annotation> over the the CAS for the specific type
     *
     * @param cas  the CAS
     * @param type the type
     * @return the iterator
     */
    private Iterator<Annotation> getTypeIterator(CAS cas, int type) {

        Iterator<Annotation> iter = null;
        try {
            iter = cas.getJCas().getJFSIndexRepository().getAnnotationIndex(type).iterator();
        } catch (CASException e) {
            e.printStackTrace();
        }
        return iter;
    }

    /**
     * Checks if a String is contained in an array of Strings
     *
     * @param string the string that is tested to be contained
     * @param array  the array that contains the string or not
     * @return true if the string is contained in the array
     */
    private boolean isContained(String string, String[] array) {
        boolean isContained = false;
        for (int i = 0; i < array.length; i++) {
            if (string.equals(array[i])) {
                isContained = true;
                break;
            }
        }
        return isContained;
    }
}
