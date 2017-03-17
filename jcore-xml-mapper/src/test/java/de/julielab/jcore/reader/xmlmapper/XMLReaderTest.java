/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.reader.xmlmapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.AuthorInfo;
import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.DBInfo;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.Keyword;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.types.pubmed.ManualDescriptor;

/**
 * Test for class MedlineReader
 * 
 * @author muelhausen
 */
public class XMLReaderTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLReaderTest.class);
	private static final boolean DEBUG_MODE = true;
	/**
	 * Path to the XMLReader descriptor for Medline (multiple files)
	 */
	private static final String DESC_XML_READER = "src/test/resources/XMLReaderDescriptor_medline.xml";
	/**
	 * Path to the XMLReader descriptor for Medline with a single file parameter
	 */
	private static final String DESC_XML_READER_SINGLE_FILE = "src/test/resources/XMLReaderDescriptor_medline_singleFile.xml";
	/**
	 * Path to the XMLReader descriptor for Medline without inputDir parameter
	 * (and no single file attribute)
	 */
	private static final String DESC_XML_READER_MISSING_INPUT_DIR = "src/test/resources/XMLReaderDescriptor_medline_missingInputDir.xml";
	/**
	 * Test data
	 */
	private static final String[] EXPECTED_KEYWORDS = { "NASA", "Birth Rate", "Doe v. Bolton" };
	/**
	 * Test data
	 */
	private static final String EXPECTED_TITLE = "Effects of hydrogen peroxide on DNA and plasma membrane integrity of human spermatozoa.";
	/**
	 * Test data
	 */
	private static final String EXPECTED_TITLE_2 = "The case for treatment guidelines.";
	/**
	 * Test data
	 */
	private static final String EXPECTED_ABSTRACT_TEXT = "OBJECTIVE: To evaluate the effects of oxidative stress on DNA and plasma "
			+ "membrane integrity of human spermatozoa. DESIGN: Prospective cohort study. SETTING: University-based, tertiary-care"
			+ " infertility center. PATIENT(S): Men (n = 10) undergoing infertility investigation. INTERVENTION(S): "
			+ "Purified populations of sperm with high motility were separated using Percoll density gradients. "
			+ "Then, spermatozoa were incubated with 0, 10, 100, and 200 microM hydrogen peroxide (H(2)O(2)) "
			+ "under capacitating conditions.";
	/**
	 * Test data
	 */
	private static final String EXPECTED_DOCUMENT_TEXT = EXPECTED_TITLE + "\n" + EXPECTED_ABSTRACT_TEXT;
	/**
	 * Test data, format: LastName, ForeName, Initials, Affiliation
	 */
	private static final String[][] EXPECTED_AUTHORS = {
			{ "Kemal Duru", "N", "N",
					"Department of Obstetrics and Gynecology," + " GATA School of Medicine, Ankara, Turkey." },
			{ "Morshedi", "M", "M", "" }, { "Oehninger", "S", "S", "" } };
	/**
	 * Test data, format: DataBankName, AccessionNumber, AccessionNumber,
	 * AccessionNumber
	 */
	private static final String[][] EXPECTED_DB_INFO = { { "GENBANK", "AF078607", "AF078608", "AF078609" },
			{ "GENBANKClinicalTrials.gov", "NTC00078607", "", "" },
			{ "ISRCTN", "ISRCTN0000607", "ISRCTN0078608", "" } };
	/**
	 * Test data, format: DescriptorName, isMajorTopic_descriptor,
	 * QualifierName, isMajorTopic_qualifier
	 */
	private static final String[][] EXPECTED_MESH_HEADINGS = { { "Annexin A5", "N", "metabolism", "N" },
			{ "Cohort Studies", "N", "", "" }, { "DNA", "N", "drug effects", "Y" } };
	/**
	 * Test data
	 */
	private static final String[] EXPECTED_GENE_SYMBOLS = { "pyrB", "Ghox-lab", "polC" };
	/**
	 * Test data, format: RegistryNumber, NameOfSubstance
	 */
	private static final String[][] EXPECTED_CHEMICALS = { { "0", "Annexin A5" }, { "0", "Phosphatidylserines" } };
	/**
	 * Test data, format: citationStatus, language, pmid
	 */
	private static final String[] EXPECTED_HEADER = { "MEDLINE", "eng", "11119751.xml" };
	/**
	 * test data, format: issn, volume, issue, title
	 */
	private static final String[] EXPECTED_JOURNAL = { "0015-0282", "74", "6", "Fertility and sterility.",
			"Fertil Steril", "1200-7" };
	/**
	 * test data, format: year, month, day <MedlineDate>2000
	 * Spring-Summer</MedlineDate>
	 */
	private static final int[] EXPECTED_DATE_1 = { 2000, 0, 0 };
	/**
	 * test data, format: year, month, day <MedlineDate>2000 Dec
	 * 23-30</MedlineDate>
	 */
	private static final int[] EXPECTED_DATE_2 = { 2000, 12, 0 };
	/**
	 * test data, format: year, month, day <MedlineDate>2000 Oct-2001
	 * Mar</MedlineDate>
	 */
	private static final int[] EXPECTED_DATE_3 = { 2001, 3, 0 };
	private static final String[] EXPECTED_FORE_NAMES = { "F", "L G" };
	private static final String DIR_CAS_OUTPUT = "src/test/resources/xmi-output/";
	private static final String EXPECTED_JOURNAL_TITLE = "Digestive and liver disease : official journal of the Italian Society of Gastroenterology and the Italian Association for the Study of the Liver";
	private static final String[] EXPECTED_PUBTYPES = { "Case Reports", "Journal Article", "Review" };
	private static final String EXPECTED_DOI = "10.1016/j.ijom.2006.12.012";
	/**
	 * Used for building the filename for serializing the CAS if PMID is not
	 * found in the CAS
	 */
	private static int fileNameCount = 0;

	/**
	 * Default constructor
	 */
	public XMLReaderTest() {
		super();
		if (DEBUG_MODE) {
			LOGGER.info("XMLReader test is in DEBUG_MODE");
		}
	}

	/**
	 * Object to be tested
	 */
	private CollectionReader medlineReader;
	/**
	 * CAS array with CAS objects that where processed by the medlineReader
	 */
	private ArrayList<CAS> cases = new ArrayList<CAS>();

	@Test
	public void testSingleEntityData() throws Throwable {
		medlineReader = createCollectionReader("src/test/resources/XMLReaderDescriptor_medline_singleFile2.xml");
		try {
			assertTrue(medlineReader.hasNext());
			while (medlineReader.hasNext()) {
				CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData) medlineReader.getMetaData());
				medlineReader.getNext(cas);
				if (DEBUG_MODE) {
					serializeCas(cas);
				}
				assertTrue("test documenttext", cas.getDocumentText() != null && cas.getDocumentText().length() > 0);
				assertEquals(
						"Mitigation of graft-versus-host disease in rats treated with allogeneic and xenogeneic antilymphocytic sera.\nThis is a very short test abstract.",
						cas.getDocumentText());
				Iterator<Annotation> iter = getTypeIterator(cas, EntityMention.type);
				int counter = 0;
				String[] types = new String[] { ":::diso:2,3", ":::spe", ":::pgn" };
				String[] texts = new String[] { "graft-versus-host disease", "rats", "sera" };
				assertTrue("No entity mentions found in the CAS", iter.hasNext());
				while (iter.hasNext()) {
					EntityMention text = (EntityMention) iter.next();
					String coveredText = text.getCoveredText();
					if (coveredText.length() == 0)
						fail("no text in entity mention");
					else {
						assertEquals(types[counter], text.getSpecificType());
						assertEquals(texts[counter], coveredText);
					}
					counter++;
				}
			}

		} catch (CollectionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test the reading of a single file instead of a directory
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNextCas_singleFile() throws Exception {
		medlineReader = createCollectionReader(DESC_XML_READER_SINGLE_FILE);
		CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData) medlineReader.getMetaData());
		try {
			medlineReader.getNext(cas);
		} catch (CollectionException e) {
			LOGGER.error(e.getMessage(), e);
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			e.printStackTrace();
		}
		checkDocText(cas);
		checkTitle(cas);
		checkAbstractText(cas);
		checkAuthors(cas);
		checkPubDate(cas);
		checkPubTypeList(cas);
		checkHeader(cas);
		checkMeSHList(cas);
	}

	@Test
	public void testUnicodeCharOutsideBMP() throws Exception {
		medlineReader = createCollectionReader(
				"src/test/resources/XMLReaderDescriptor_medline_Unicode_outside_BMP.xml");
		CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData) medlineReader.getMetaData());
		medlineReader.getNext(cas);
		JCas jCas = cas.getJCas();
		Header header = (Header) jCas.getAnnotationIndex(Header.type).iterator().next();
		AuthorInfo authorInfo = header.getAuthors(0);
		System.out.println(authorInfo.getAffiliation().charAt(61));
	}

	private void checkHeader(CAS cas) {
		Iterator<Annotation> typeIterator = getTypeIterator(cas, Header.type);
		int i = 0;
		while (typeIterator.hasNext()) {
			Header annotation = (Header) typeIterator.next();
			assertEquals(5, annotation.getAuthors().size());
			assertEquals("MEDLINE", annotation.getCitationStatus());
			assertEquals("19754895", annotation.getDocId());
			assertEquals("eng", annotation.getLanguage());
			assertEquals("x.xml",
					annotation.getSource().substring(annotation.getSource().lastIndexOf(File.separator) + 1));
			assertEquals(4, annotation.getPubTypeList().size());
			i++;
		}
		if (i != 1) {
			fail("wrong header count should be 1 but is" + i);
		}
	}

	private void checkMeSHList(CAS cas) {
		// fail("es werden die booleans von den nachfolgenden werten genommen");
		String[] meshList = new String[] { "Animalsfalsenullfalse", "Calcium Channels\n			falsemetabolismtrue",
				"Membrane Proteins\n			falsemetabolismtrue", "Signal Transduction\n			falsenullfalse",
				"T-Lymphocytes\n			falsemetabolismtrue", "Protein Binding\n			falsenullfalse",
				"Humansfalsenullfalse", "Calciumfalsemetabolismfalse" };
		Iterator<Annotation> typeIterator = getTypeIterator(cas, MeshHeading.type);
		int i = 0;
		while (typeIterator.hasNext()) {
			MeshHeading annotation = (MeshHeading) typeIterator.next();
			assertEquals(meshList[i++], annotation.getDescriptorName() + annotation.getDescriptorNameMajorTopic()
					+ annotation.getQualifierName() + annotation.getQualifierNameMajorTopic());
		}
	}

	private void checkPubTypeList(CAS cas) {
		String[] journalStrings = new String[] { "Journal Article1600-065X2311Immunological reviewsImmunol Rev148-59",
				"Research Support, N.I.H., Intramural\n			1600-065X2311Immunological reviewsImmunol Rev148-59",
				"Review1600-065X2311Immunological reviewsImmunol Rev148-59",
				"Research Support, N.I.H., Extramural\n			1600-065X2311Immunological reviewsImmunol Rev148-59" };
		Iterator<Annotation> typeIterator = getTypeIterator(cas, Journal.type);
		int i = 0;
		while (typeIterator.hasNext()) {
			Journal annotation = (Journal) typeIterator.next();
			assertEquals(journalStrings[i++],
					annotation.getName() + annotation.getISSN() + annotation.getVolume() + annotation.getIssue()
							+ annotation.getTitle() + annotation.getShortTitle() + annotation.getPages());
		}
	}

	private void checkPubDate(CAS cas) {
		Iterator<Annotation> typeIterator = getTypeIterator(cas, Date.type);
		while (typeIterator.hasNext()) {
			Date annotation = (Date) typeIterator.next();
			assertEquals(2009, annotation.getYear());
			assertEquals(9, annotation.getMonth());
			assertEquals(0, annotation.getDay());
		}
	}

	private void checkAuthors(CAS cas) {
		Iterator<Annotation> typeIterator = getTypeIterator(cas, AuthorInfo.type);
		int i = 0;
		HashMap<String, String[]> authors = new HashMap<String, String[]>();
		while (typeIterator.hasNext()) {
			AuthorInfo annotation = (AuthorInfo) typeIterator.next();
			assertEquals(
					"Laboratory of Cellular and Molecular Biology, Center for\n			Cancer Research, National Cancer Institute, Bethesda, MD 20892, USA.\n			barrv@mail.nih.gov",
					annotation.getAffiliation());
			authors.put(annotation.getInitials(), new String[] { annotation.getLastName(), annotation.getForeName() });
		}
		for (i = 0; i < this.authors.length; i++) {
			assertEquals(this.authors[i][0], authors.get(this.authors[i][2])[0]);
			assertEquals(this.authors[i][1], authors.get(this.authors[i][2])[1]);
		}
	}

	private void checkAbstractText(CAS cas) {
		String text = "";
		Iterator<Annotation> typeIterator = getTypeIterator(cas, AbstractText.type);
		if (typeIterator.hasNext()) {
			AbstractText abstractText = (AbstractText) typeIterator.next();
			System.out.println("hier: " + abstractText.getBegin() + " " + abstractText.getEnd());
			text = abstractText.getCoveredText();
		}
		assertEquals(abstractText, text);
	}

	private void checkTitle(CAS cas) {
		String title = "";
		Iterator<Annotation> typeIterator = getTypeIterator(cas, Title.type);
		if (typeIterator.hasNext()) {
			title = ((Title) typeIterator.next()).getCoveredText();
		}
		assertEquals(abstractTitle, title);
	}

	private void checkDocText(CAS cas) {
		assertEquals(abstractTitle + "\n" + abstractText, cas.getDocumentText());
	}

	String abstractText = "In the last few years, great progress has been made in\n				understanding how stromal interacting molecule 1 (STIM1), a protein\n				containing a calcium sensor that is located in the endoplasmic\n				reticulum, and Orai1, a protein that forms a calcium channel in the\n				plasma membrane, interact and give rise to store-operated calcium\n				entry. Pharmacological depletion of calcium stores leads to the\n				formation of clusters containing STIM and Orai that appear to be\n				sites for calcium influx. Similar puncta are also produced in\n				response to physiological stimuli in immune cells. In T cells\n				engaged with antigen-presenting cells, clusters containing STIM and\n				Orai accumulate at the immunological synapse. We recently discovered\n				that in activated T cells, STIM1 and Orai1 also accumulate in\n				cap-like structures opposite the immune synapse at the distal pole\n				of the cell. Both caps and puncta are long-lived stable structures\n				containing STIM1 and Orai1 in close proximity. The function of\n				puncta as sites of calcium influx is clear. We speculate that the\n				caps may provide a secondary site of calcium entry. Alternatively,\n				they may serve as a source of preformed channel complexes that move\n				to new immune synapses as T cells repeatedly engage\n				antigen-presenting cells.";
	String abstractTitle = "Formation of STIM and Orai complexes: puncta and distal\n			caps.";

	String[][] authors = new String[][] { new String[] { "Barr", "Valarie A", "VA" },
			new String[] { "Bernot", "Kelsie M", "KM" }, new String[] { "Shaffer", "Meredith H", "MH" },
			new String[] { "Burkhardt", "Janis K", "JK" }, new String[] { "Samelson", "Lawrence E", "LE" } };

	/**
	 * Test main functionality of the {@link CollectionReader}
	 * 
	 * @throws Exception
	 */

	public void estGetNextCas() throws Exception {
		medlineReader = createCollectionReader(DESC_XML_READER);
		processAllCases();
		checkAllDocumentTexts();
		checkElements();
	}

	/**
	 * Test if a missing inputDirectroy parameter in the descriptor causes an
	 * {@link ResourceInitializationException}
	 */
	@Test
	public void testMissingInputDirectory() {
		try {
			medlineReader = getCollectionReader(DESC_XML_READER_MISSING_INPUT_DIR);
			fail("Expected exception was not thrown");
		} catch (Exception e) {
			assertTrue("Exception should be an instance of ResourceInitializationException , but was "
					+ e.getClass().getName(), e instanceof ResourceInitializationException);
		}
	}

	// TODO write test for initialization of a single file reader with no
	// parameter values
	/**
	 * Processes all CASes by the medlineReader
	 */
	private void processAllCases() {
		try {
			while (medlineReader.hasNext()) {
				CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData) medlineReader.getMetaData());
				medlineReader.getNext(cas);
				if (DEBUG_MODE) {
					serializeCas(cas);
				}
				cases.add(cas);
			}
		} catch (CollectionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write CAS to XMI
	 * 
	 * @param cas
	 */
	private void serializeCas(CAS cas) {
		try {
			String filename = getPMID(cas);
			if (filename == null) {
				filename = "" + fileNameCount++;
			}
			File outputDir = new File(DIR_CAS_OUTPUT);
			if (!outputDir.exists())
				outputDir.mkdirs();
			String filePath = DIR_CAS_OUTPUT + filename + ".xmi";
			LOGGER.debug("Writing file " + filePath);
			XmiCasSerializer.serialize(cas, new FileOutputStream(new File(filePath)));
		} catch (FileNotFoundException e) {
			LOGGER.error("", e);
		} catch (SAXException e) {
			LOGGER.error("", e);
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
	 * @throws Exception
	 *             e.g. ResourceInitializationException
	 */
	private CollectionReader getCollectionReader(String descAnalysisEngine) throws Exception {
		CollectionReader collectionReader = null;
		XMLInputSource source;
		source = new XMLInputSource(descAnalysisEngine);
		ResourceSpecifier resourceSpecifier = (ResourceSpecifier) UIMAFramework.getXMLParser().parse(source);
		collectionReader = UIMAFramework.produceCollectionReader(resourceSpecifier);
		return collectionReader;
	}

	/**
	 * Test if the CAS returned by the collectionReader has a non-empty document
	 * text returns
	 */
	private void checkAllDocumentTexts() {
		for (int i = 0; i < cases.size(); i++) {
			LOGGER.trace(i + ". checking Docuemnt Text of cas with pmid: " + getPMID(cases.get(i)));
			String text = cases.get(i).getDocumentText();
			// assertTrue(((text == null) ? "null" : text), (text != null) &&
			// (!text.equals("")));
			assertNotNull(text);
			assertTrue(!"".equals(text));
		}
	}

	/**
	 * Checks all Elements
	 */
	private void checkElements() {
		int checkCount = 0;
		for (int i = 0; i < cases.size(); i++) {
			CAS cas = cases.get(i);
			// check medline XML with all items
			String pmid = getPMID(cas);
			if (pmid.equals("11119751")) {
				checkCount++;
				assertTrue("Invalid keyWordList", checkKeywords(cas, EXPECTED_KEYWORDS));
				assertTrue("Invalid Authors", checkAuthors(cas, EXPECTED_AUTHORS));
				assertTrue("Invalid DBInfoList", ckeckDBInfos(cas, EXPECTED_DB_INFO));
				assertTrue("Invalid MeshHeading", checkMeshHeadings(cas, EXPECTED_MESH_HEADINGS));
				assertTrue("Invalid GeneSymbol", checkGeneSymbols(cas, EXPECTED_GENE_SYMBOLS));
				assertTrue("Invalid Chemical", checkChemicals(cas, EXPECTED_CHEMICALS));
				assertTrue("Invalid Header", checkHeader(cas, EXPECTED_HEADER));
				assertTrue("Invalid ManualDescriptor", checkManualDescriptor(cas));
				assertTrue("Invalid Journal", ckeckJournal(cas, EXPECTED_JOURNAL));
				assertTrue("Invalid DocumentText", checkDocumentText(cas, EXPECTED_DOCUMENT_TEXT));
				assertTrue("Invalid AbstractText", checkAbstractText(cas, EXPECTED_ABSTRACT_TEXT));
				assertTrue("Invalid Title", checkTitle(cas, EXPECTED_TITLE));
				assertTrue("Sentences Found", !checkSentences(cas));
			}
			// check medline XML without most lists (gene, keywords,...)
			if (pmid.equals("11119751-a")) {
				checkCount++;
				assertTrue("Invalid Authors", checkAuthors(cas, EXPECTED_AUTHORS));
				assertTrue("Sentences Found", !checkSentences(cas));
			}
			// check medline XML with pub date: <MedlineDate>2000
			// Spring-Summer</MedlineDate>
			if (pmid.equals("11119751-b")) {
				checkCount++;
				assertTrue("Invalid Authors", checkAuthors(cas, EXPECTED_AUTHORS));
				assertTrue("Invalid GeneSymbol", checkGeneSymbols(cas, EXPECTED_GENE_SYMBOLS));
				assertTrue("Invalid Journal", ckeckJournal(cas, EXPECTED_JOURNAL));
				assertTrue("Invalid PubDate", checkPubDate(cas, EXPECTED_DATE_1));
				assertTrue("Sentences Found", !checkSentences(cas));
			}
			// check medline XML with pub date: <MedlineDate>2000 Dec
			// 23-30</MedlineDate>
			if (pmid.equals("11119751-c")) {
				checkCount++;
				assertTrue("Invalid Authors", checkAuthors(cas, EXPECTED_AUTHORS));
				assertTrue("Invalid GeneSymbol", checkGeneSymbols(cas, EXPECTED_GENE_SYMBOLS));
				assertTrue("Invalid Journal", ckeckJournal(cas, EXPECTED_JOURNAL));
				assertTrue("Invalid PubDate", checkPubDate(cas, EXPECTED_DATE_2));
				assertTrue("Sentences Found", !checkSentences(cas));
			}
			// check medline XML pub date: <MedlineDate>2000 Oct-2001
			// Mar</MedlineDate>
			if (pmid.equals("11119751-d")) {
				assertTrue("Sentences Found", !checkSentences(cas));
				checkCount++;
				assertTrue("Invalid Authors", checkAuthors(cas, EXPECTED_AUTHORS));
				assertTrue("Invalid GeneSymbol", checkGeneSymbols(cas, EXPECTED_GENE_SYMBOLS));
				assertTrue("Invalid Journal", ckeckJournal(cas, EXPECTED_JOURNAL));
				assertTrue("Invalid PubDate", checkPubDate(cas, EXPECTED_DATE_3));
				assertTrue("Sentences Found", !checkSentences(cas));
			}
			if (pmid.equals("8045680")) {
				checkCount++;
				assertTrue("No Sentences Found", checkSentences(cas));
				// assertTrue("Invalid Header", checkHeader(cas,
				// EXPECTED_HEADER_OTHER_LANGUAGE));
			}
			if (pmid.equals("12626969")) {
				checkCount++;
				assertTrue("No Sentences Found", checkSentences(cas));
				// assertTrue("Invalid Header", checkHeader(cas,
				// EXPECTED_HEADER_OTHER_LANGUAGE));
			}
			if (pmid.equals("11119751-e")) {
				assertTrue("Sentences Found", !checkSentences(cas));
				checkCount++;
				// assertTrue("Invalid Header", checkHeader(cas,
				// EXPECTED_HEADER_OTHER_LANGUAGE));
			}
			// test the case that only a title is found and no abstractText
			// (documentText should be equal to title in this case)
			if (pmid.equals("17276851")) {
				assertTrue("Sentences Found", !checkSentences(cas));
				checkCount++;
				assertTrue("Invalid Document Title", checkTitle(cas, EXPECTED_TITLE_2));
				assertTrue("Invalid Document Text", checkDocumentText(cas, EXPECTED_TITLE_2));
			}
			// PubMed has changed the XML element ForeName to FirstName, but
			// foreName should still
			// be supported
			if (pmid.equals("18439884")) {
				assertTrue("Sentences Found", !checkSentences(cas));
				checkCount++;
				assertTrue("Invalid foreName", checkForeNames(cas, EXPECTED_FORE_NAMES));
				checkJournalTitle(cas, EXPECTED_JOURNAL_TITLE);
			}
			if (pmid.equals("17306504")) {
				assertTrue("Sentences Found", !checkSentences(cas));
				checkCount++;
				assertTrue("Invalid pubTypeList", checkPubTypeList(cas, EXPECTED_PUBTYPES));
				assertTrue("Invalid DOI", checkDoi(cas, EXPECTED_DOI));
			}
		}
		assertEquals(11, checkCount);
	}

	private boolean checkDoi(CAS cas, String expectedDoi) {
		Iterator<Annotation> iter = getTypeIterator(cas, Header.type);
		while (iter.hasNext()) {
			Header header = (Header) iter.next();
			String doi = header.getDoi();
			if (doi.equals(expectedDoi)) {
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
	 * Check if the Document Text contains both the Title and the AbstractText
	 * devided by a new line character
	 * 
	 * @param cas
	 * @param documentText
	 * @return
	 */
	private boolean checkDocumentText(CAS cas, String documentText) {
		return cas.getDocumentText().equals(documentText);
	}

	/**
	 * Check if the correct abstract text is contained in the CAS
	 * 
	 * @param cas
	 *            The CAS
	 * @param abstractTextString2
	 *            The correct abstract text
	 * @return true if the correct abstract text is contained in the CAS
	 */
	private boolean checkAbstractText(CAS cas, String abstractTextString2) {
		Iterator<Annotation> iter = getTypeIterator(cas, AbstractText.type);
		AbstractText abstractText = (AbstractText) iter.next();
		return abstractText.getCoveredText().equals(abstractTextString2);
	}

	/**
	 * Check if the correct title is contained in the CAS
	 * 
	 * @param cas
	 *            The CAS
	 * @param title
	 *            The correct title
	 * @return true if the correct title is contained in the CAS
	 */
	private boolean checkTitle(CAS cas, String expectedTitle) {
		Iterator<Annotation> iter = getTypeIterator(cas, Title.type);
		Title title = (Title) iter.next();
		return title.getCoveredText().equals(expectedTitle);
	}

	/**
	 * Checks if the PubDate in the cas equals the date in the dateValues
	 * 
	 * @param cas
	 *            the CAS to be checked
	 * @param dateValues
	 *            test data
	 * @return true, if pubDate in CAS equals the test data date
	 */
	private boolean checkPubDate(CAS cas, int[] dateValues) {
		Iterator<Annotation> iter = getTypeIterator(cas, Journal.type);
		Journal journal = (Journal) iter.next();
		Date date = journal.getPubDate();
		if (date.getDay() != dateValues[2] || date.getMonth() != dateValues[1] || date.getYear() != dateValues[0]) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the PMID of the medline arcticle contained in the CAS
	 * 
	 * @param cas
	 *            the CAS
	 * @return The PMID of the medline article included in the CAS
	 */
	private String getPMID(CAS cas) {
		Iterator<Annotation> iter = getTypeIterator(cas, Header.type);
		Header header = (Header) iter.next();
		String docId = header.getDocId();
		return docId != null ? docId : "";
	}

	/**
	 * Checks if ManualDescriptor is contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @return true, if the cas has a manual descriptor with all possible
	 *         FSArrays
	 */
	private boolean checkManualDescriptor(CAS cas) {
		Iterator<Annotation> iter = getTypeIterator(cas, ManualDescriptor.type);
		ManualDescriptor manualDescriptor = (ManualDescriptor) iter.next();
		if (manualDescriptor.getMeSHList() == null || manualDescriptor.getChemicalList() == null
				|| manualDescriptor.getKeywordList() == null || manualDescriptor.getDBInfoList() == null
				|| manualDescriptor.getGeneSymbolList() == null) {
			return false;
		}
		// it is not correct if there is more than one header
		if (iter.hasNext()) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if Header is contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param headerData
	 *            test data for the header Type
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkHeader(CAS cas, String[] headerData) {
		Iterator<Annotation> iter = getTypeIterator(cas, Header.type);
		Header header = (Header) iter.next();
		if (header.getAuthors() == null || header.getPubTypeList() == null
				|| !header.getCitationStatus().equals(headerData[0]) || !header.getLanguage().equals(headerData[1])
				|| !header.getSource().contains(headerData[2])) {
			return false;
		}
		// it is wrong if there is more than one header
		if (iter.hasNext()) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if Journal is contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param journalData
	 *            test data
	 * @return true, if cas contains only a Journal as PubType and if this
	 *         Journal has the correct values
	 */
	private boolean ckeckJournal(CAS cas, String[] journalData) {
		Iterator<Annotation> iter = getTypeIterator(cas, Journal.type);
		Journal journal = (Journal) iter.next();
		if (!journal.getISSN().equals(journalData[0]) || !journal.getVolume().equals(journalData[1])
				|| !journal.getIssue().equals(journalData[2]) || !journal.getTitle().equals(journalData[3])
				|| !journal.getShortTitle().equals(journalData[4]) || !journal.getPages().equals(journalData[5])) {
			return false;
		}
		if (iter.hasNext()) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if all Chemicals are contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param chemicals
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkChemicals(CAS cas, String[][] chemicals) {
		Iterator<Annotation> iter = getTypeIterator(cas, Chemical.type);
		int countChemicals = 0;
		while (iter.hasNext()) {
			Chemical chemical = (Chemical) iter.next();
			boolean chemicalIsContained = false;
			for (int i = 0; i < chemicals.length; i++) {
				if (chemicalEqualsStringArray(chemical, chemicals[i])) {
					chemicalIsContained = true;
				}
			}
			if (!chemicalIsContained) {
				return false;
			}
			countChemicals++;
		}
		if (chemicals.length != countChemicals) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if Chemical object has the same content as the String array String
	 * array format: RegistryNumber, NameOfSubstance
	 * 
	 * @param chemical
	 *            Chemical to be compared
	 * @param strings
	 *            test data
	 * @return true, if test data and Chemical are equal
	 */
	private boolean chemicalEqualsStringArray(Chemical chemical, String[] strings) {
		if (!chemical.getRegistryNumber().equals(strings[0]) || !chemical.getNameOfSubstance().equals(strings[1])) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if all GeneSymbols are contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param geneSymbols
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkGeneSymbols(CAS cas, String[] geneSymbols) {
		Iterator<Annotation> iter = getTypeIterator(cas, ManualDescriptor.type);
		while (iter.hasNext()) {
			ManualDescriptor manualDescriptor = (ManualDescriptor) iter.next();
			StringArray geneSymbolList = manualDescriptor.getGeneSymbolList();
			for (int i = 0; i < geneSymbols.length; i++) {
				if (!isContained(geneSymbolList.get(i), geneSymbols)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Checks if all MeshHeadings are contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param meshHeadings
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkMeshHeadings(CAS cas, String[][] meshHeadings) {
		int countMeshHeadings = 0;
		Iterator<Annotation> iter = getTypeIterator(cas, MeshHeading.type);
		while (iter.hasNext()) {
			MeshHeading meshHeading = (MeshHeading) iter.next();
			boolean meshHeadingIsContained = false;
			for (int i = 0; i < meshHeadings.length; i++) {
				if (meshHeadingEqualsStringArray(meshHeading, meshHeadings[i])) {
					meshHeadingIsContained = true;
					break;
				}
			}
			if (!meshHeadingIsContained) {
				return false;
			}
			countMeshHeadings++;
		}
		if (countMeshHeadings != meshHeadings.length) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if MeshHeading object has the same content as the String array
	 * String array format: DescriptorName, isMajorTopic_descriptor,
	 * QualifierName, isMajorTopic_qualifier
	 * 
	 * @param meshHeading
	 *            the MeshHeading to be compared
	 * @param strings
	 *            test data
	 * @return true, if equal
	 */
	private boolean meshHeadingEqualsStringArray(MeshHeading meshHeading, String[] strings) {
		if (!strings[0].equals("") && !meshHeading.getDescriptorName().equals(strings[0])) {
			return false;
		}
		if (!strings[2].equals("") && !meshHeading.getQualifierName().equals(strings[2])) {
			return false;
		}
		if (!strings[1].equals("") && (meshHeading.getDescriptorNameMajorTopic() && strings[1].equals("N"))) {
			return false;
		}
		if (!strings[1].equals("") && !meshHeading.getDescriptorNameMajorTopic() && strings[1].equals("Y")) {
			return false;
		}
		if (!strings[3].equals("") && meshHeading.getQualifierNameMajorTopic() && strings[3].equals("N")) {
			return false;
		}
		if (!strings[3].equals("") && !meshHeading.getQualifierNameMajorTopic() && strings[3].equals("Y")) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if all DBInfos are contained correctly in the CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param dbInfos
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean ckeckDBInfos(CAS cas, String[][] dbInfos) {
		int countDBInfos = 0;
		Iterator<Annotation> iter = getTypeIterator(cas, DBInfo.type);
		while (iter.hasNext()) {
			DBInfo dbInfo = (DBInfo) iter.next();
			boolean dbInfoIsContained = false;
			for (int i = 0; i < dbInfos.length; i++) {
				if (dbInfoEqualsStringArray(dbInfo, dbInfos[i])) {
					dbInfoIsContained = true;
				}
			}
			if (!dbInfoIsContained) {
				return false;
			}
			countDBInfos++;
		}
		if (countDBInfos != dbInfos.length) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if DBInfo object has the same content as the String array
	 * 
	 * @param dbInfo
	 *            the DBInfo to be compared
	 * @param strings
	 *            test data
	 * @return true, if the content of the dbInfo equals the test data
	 */
	private boolean dbInfoEqualsStringArray(DBInfo dbInfo, String[] strings) {
		if (!dbInfo.getName().equals(strings[0])) {
			return false;
		}
		for (int i = 1; i < strings.length; i++) {
			// if the accessionNumber is not present (empty string), it can not
			// be
			// checked if it is contained in the StringArray
			// so the iteration will be continued in this case
			if (strings[i].equals("")) {
				continue;
			}
			if (!stringArrayContainsString(dbInfo.getAcList(), strings[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if a StringArray contains a String
	 * 
	 * @param array
	 *            the array that could contain the string
	 * @param string
	 *            the string that could be contained in the array
	 * @return true, if string is contained in the array
	 */
	private boolean stringArrayContainsString(StringArray array, String string) {
		for (int i = 0; i < array.size(); i++) {
			if (array.get(i).equals(string)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if there Are Sentences given
	 * 
	 * @param cas
	 * @return
	 */
	private boolean checkSentences(CAS cas) {
		Iterator<Annotation> iter = getTypeIterator(cas, Sentence.type);
		int count = 0;
		while (iter.hasNext()) {
			Sentence s = (Sentence) iter.next();
			assertTrue("Sentence has an ID", s.getId() != null);
			assertTrue("Sentence has an Begin", s.getBegin() >= 0);
			assertTrue("Sentence has an End", s.getEnd() >= 0);
			// System.out.println("id = " + s.getId() + "begin = " +
			// s.getBegin() + " end = " + s.getEnd());
			// System.out.println(s.getCoveredText());
			count++;
		}
		if (count == 0)
			return false;
		return true;
	}

	/**
	 * Checks if all authors are contained correctly in CAS
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param authors
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkAuthors(CAS cas, String[][] authors) {
		int countAuthors = 0;
		Iterator<Annotation> iter = getTypeIterator(cas, AuthorInfo.type);
		while (iter.hasNext()) {
			AuthorInfo authorInfo = (AuthorInfo) iter.next();
			boolean authorIsContained = false;
			for (int i = 0; i < authors.length; i++) {
				if (authorEqualsStringArray(authorInfo, authors[i])) {
					authorIsContained = true;
				}
			}
			if (!authorIsContained) {
				return false;
			}
			countAuthors++;
		}
		if (authors.length != countAuthors) {
			return false;
		}
		return true;
	}

	/**
	 * Check if foreName was correctly parsed (PubMed changed firstName to
	 * foreName, but both should be supported)
	 * 
	 * @param cas
	 * @param foreName
	 * @return
	 */
	private boolean checkForeNames(CAS cas, String[] foreNames) {
		Iterator<Annotation> iter = getTypeIterator(cas, AuthorInfo.type);
		Set<String> set = new HashSet<String>(Arrays.asList(foreNames));
		// the variable success garantees that the check fails if the iterator
		// is empty
		boolean success = false;
		int foreNameCount = 0;
		while (iter.hasNext()) {
			success = true;
			AuthorInfo authorInfo = (AuthorInfo) iter.next();
			foreNameCount++;
			if (!set.contains(authorInfo.getForeName())) {
				// if (!authorInfo.getForeName().equals(foreNames[i++])) {
				LOGGER.info("Found " + authorInfo.getForeName() + " which was not expected");
				return false;
			}
		}
		assertEquals(foreNames.length, foreNameCount);
		return success;
	}

	/**
	 * Checks if AuthorInfo Object has the same content as the String array
	 * 
	 * @param authorInfo
	 *            the AuthorInfo the be compared
	 * @param strings
	 *            test data
	 * @return true if AuthorInfo and String array have the same content
	 */
	private boolean authorEqualsStringArray(AuthorInfo authorInfo, String[] strings) {
		// if affiliation is not set it is null, not "" (empty String) !
		if (authorInfo.getAffiliation() != null && strings[3].equals("")) {
			return false;
		}
		if (!authorInfo.getLastName().equals(strings[0]) || !authorInfo.getForeName().equals(strings[1])
				|| !authorInfo.getInitials().equals(strings[2])) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if the keyword list is contained in the cas
	 * 
	 * @param cas
	 *            the CAS to be tested
	 * @param keywords
	 *            test data
	 * @return true, if test data is correctly contained in CAS
	 */
	private boolean checkKeywords(CAS cas, String[] keywords) {
		int countKeywords = 0;
		Iterator<Annotation> iter = getTypeIterator(cas, Keyword.type);
		while (iter.hasNext()) {
			Keyword keyword = (Keyword) iter.next();
			if (!isContained(keyword.getName(), keywords)) {
				return false;
			}
			countKeywords++;
		}
		if (countKeywords != keywords.length) {
			return false;
		}
		return true;
	}

	/**
	 * Gets an Iterator over the the CAS for the specific type
	 * 
	 * @param cas
	 *            the CAS
	 * @param type
	 *            the type
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
	 * @param string
	 *            the string that is tested to be contained
	 * @param array
	 *            the array that contains the string or not
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

	private CollectionReader createCollectionReader(String descriptor) throws Exception {
		ResourceSpecifier resourceSpecifier;
		CollectionReader xmlReader = null;
		resourceSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(new XMLInputSource(descriptor));
		xmlReader = UIMAFramework.produceCollectionReader(resourceSpecifier);
		return xmlReader;
	}

	@Test
	public void testStructuredAbstract() throws UIMAException, IOException {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
				"de.julielab.jcore.types.jcore-document-structure-types");
		CollectionReader reader = CollectionReaderFactory.createReader(XMLReader.class, XMLReader.PARAM_INPUT_FILE,
				"src/test/resources/doc_medline_test_structured_abstract.xml", XMLReader.RESOURCE_MAPPING_FILE,
				"src/test/resources/newMappingFile.xml");
		assertTrue(reader.hasNext());
		reader.getNext(jCas.getCas());
		// System.out.println(jCas.getDocumentText());
		// TODO handle structured abstracts, i.e. directly read the structures
		// and also give them the appropriate annotations. This will require
		// modifications to the mapping file. Take care to make it possible that
		// there possibly are no structured parts in the abstract (there still
		// exists both).
	}
}
