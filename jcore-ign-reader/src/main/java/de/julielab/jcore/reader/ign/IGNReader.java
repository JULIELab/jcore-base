package de.julielab.jcore.reader.ign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bioc.BioCAnnotation;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.BioCSentence;
import bioc.io.BioCDocumentReader;
import bioc.io.BioCFactory;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.pubmed.Header;

/**
 * The IGNReader reads corpus files in BioC-format.<br>
 * There are XML files comprising the actual text (as well as passage and
 * sentence annotations) and there are separate XML files comprising the
 * annotations.
 * 
 * @author engelmann
 * 
 */
public class IGNReader extends CollectionReader_ImplBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(IGNReader.class);

	/**
	 * String parameter indicating path to the directory containing files in
	 * BioC-format that comprise the actual text.
	 */
	public static final String PARAM_INPUTDIR_TEXT = "InputDirectoryText";
	/**
	 * String parameter indicating path to the directory containing files in
	 * BioC-format that comprise the annotations.
	 */
	public static final String PARAM_INPUTDIR_ANNO = "InputDirectoryAnnotations";
	/**
	 * optional Parameter providing the file path to a file mapping the article
	 * ids to the corresponding publication years
	 */
	public static final String PUBLICATION_DATES_FILE = "PublicationDatesFile";

	@ConfigurationParameter(name = PARAM_INPUTDIR_TEXT, description = "Directory containing files in BioC-format that comprise the actual text.")
	private File dirTextFiles;
	@ConfigurationParameter(name = PARAM_INPUTDIR_ANNO, description = "Directory containing files in BioC-format that comprise the annotations.")
	private File dirAnnoFiles;
	@ConfigurationParameter(name = PUBLICATION_DATES_FILE, defaultValue = "/de/julielab/jcore/reader/ign/pubdates/IGN_publicationDates", description = "File containing a mapping between article ids and publication years.")
	private String publicationDatesFile;
	/**
	 * Maps BioCDocuments for annotations to pmid.
	 */
	HashMap<String, BioCDocument> mapAnnoFiles = new HashMap<String, BioCDocument>();
	/**
	 * List of BioCDocuments for text.
	 */
	List<BioCDocument> biocDocuments = new ArrayList<BioCDocument>();

	private int currentIndex;

	private Map<String, String> pubDates;

	@Override
	public void initialize() throws ResourceInitializationException {
		LOGGER.info("initialize() - Initializing BioC Reader...");

		dirTextFiles = new File(((String) getConfigParameterValue(PARAM_INPUTDIR_TEXT)));
		if (!dirTextFiles.exists() || !dirTextFiles.isDirectory()) {
			LOGGER.error("Input directory of files comprising the text doesn't exist or is not a directory ({}).",
					dirTextFiles.getAbsolutePath());
		}
		String configParameterValue = (String) getConfigParameterValue(PARAM_INPUTDIR_ANNO);
		if (null != configParameterValue)
			dirAnnoFiles = new File(((String) configParameterValue));
		if (null != configParameterValue && (!dirAnnoFiles.exists() || !dirAnnoFiles.isDirectory())) {
			LOGGER.error(
					"Input directory of files comprising the annotations doesn't exist or is not a directory ({}).",
					configParameterValue);
			throw new ResourceInitializationException(
					new FileNotFoundException("Annotation input directory not found: " + configParameterValue));
		}
		if (getConfigParameterValue(PUBLICATION_DATES_FILE) != null)
			publicationDatesFile = ((String) getConfigParameterValue(PUBLICATION_DATES_FILE)).trim();

		try {
			pubDates = readIgnPubDates(publicationDatesFile);

			BioCFactory biocFactory = BioCFactory.newFactory(BioCFactory.STANDARD);
			FileReader reader = null;
			BioCDocumentReader biocReader = null;

			// create BioCDocuments for text and annotations
			if (null != configParameterValue) {
				File[] arrayAnnoFiles = dirAnnoFiles.listFiles();
				for (int i = 0; i < arrayAnnoFiles.length; i++) {
					File annoFile = arrayAnnoFiles[i];
					reader = new FileReader(annoFile);
					biocReader = biocFactory.createBioCDocumentReader(reader);
					BioCDocument annoDoc = biocReader.readDocument();
					String pmid = annoDoc.getID();
					mapAnnoFiles.put(pmid, annoDoc);
				}
			}

			File[] arrayTextFiles = dirTextFiles.listFiles();
			for (int i = 0; i < arrayTextFiles.length; i++) {
				File textFile = arrayTextFiles[i];
				reader = new FileReader(textFile);
				biocReader = biocFactory.createBioCDocumentReader(reader);
				BioCDocument textDoc = biocReader.readDocument();
				biocDocuments.add(textDoc);
			}
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

		currentIndex = 0;
	}

	@Override
	public void getNext(CAS aCas) throws IOException, CollectionException {
		JCas aJCas;
		try {
			aJCas = aCas.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}

		BioCDocument textDoc = biocDocuments.get(currentIndex++);
		String pmid = textDoc.getID();
		LOGGER.info("getNext(CAS) - Reading text for PMID " + pmid);

		// set pmid and text
		String text = "";
		List<BioCPassage> passageListText = textDoc.getPassages();
		for (BioCPassage passage : passageListText) {
			List<BioCSentence> sentList = passage.getSentences();
			for (BioCSentence sent : sentList) {
				String textPart = sent.getText();
				// what follows is an awkward offset matching between text and
				// annotation
				text = text + textPart + " ";
			}
		}
		Header header = new Header(aJCas);
		header.setDocId(pmid);

		// A. Rubruck
		// added the optional function to find and set the publication date
		// set publication date
		addDateForID(header, aJCas, pmid);

		header.addToIndexes();
		aJCas.setDocumentText(text);

		// read and set (in our case Gene) annotations
		if (null != mapAnnoFiles && !mapAnnoFiles.isEmpty()) {
			LOGGER.info("getNext(CAS) - Reading annotations for PMID " + pmid);
			BioCDocument annoDoc = mapAnnoFiles.get(pmid);
			List<BioCPassage> passageListAnno = annoDoc.getPassages();
			for (BioCPassage passage : passageListAnno) {
				List<BioCAnnotation> annos = passage.getAnnotations();
				for (BioCAnnotation anno : annos) {
					Map<String, String> infons = anno.getInfons();
					String egId = infons.get("entrez_id");
					String taxId = infons.get("taxonomy_id");
					List<BioCLocation> locs = anno.getLocations();
					if (locs.size() > 1) {
						LOGGER.warn(
								"Discontinuous annotation! Will be ignored, as only the first location is considered.");
					}
					// take only first location
					BioCLocation loc = locs.get(0);
					int begin = loc.getOffset();
					// what follows is an awkward offset matching between text
					// and
					// annotation
					if (!(begin == 0)) {
						begin++;
						if (text.charAt(begin - 1) != ' ') {
							begin--;
						}
					}
					int end = begin + loc.getLength();

					GeneResourceEntry resEntry = new GeneResourceEntry(aJCas);
					resEntry.setBegin(begin);
					resEntry.setEnd(end);
					resEntry.setEntryId(egId);
					resEntry.setSource("NCBI Gene");
					resEntry.setTaxonomyId(taxId);
					FSArray resList = new FSArray(aJCas, 1);
					resList.set(0, resEntry);
					Gene gene = new Gene(aJCas);
					gene.setBegin(begin);
					gene.setEnd(end);
					gene.setResourceEntryList(resList);
					// gene.setSpecies(taxId);
					// has been changed from string to stringArray
					StringArray s1 = new StringArray(aJCas, 1);
					s1.set(0, taxId);
					gene.setSpecies(s1);
					gene.addToIndexes();
				}
			}
		}
	}

	/**
	 * if publicationDatesFile is available, this method retrieves the
	 * publication date for the given article id and adds it to the passed
	 * header
	 * 
	 * @param header
	 * @param id
	 */
	private void addDateForID(Header header, JCas jCas, String id) {
		if (pubDates.isEmpty())
			return;
		Journal pubType = new Journal(jCas);
		FSArray pubTypeList = new FSArray(jCas, 1);
		pubTypeList.set(0, pubType);
		header.setPubTypeList(pubTypeList);
		Date pubDate = new Date(jCas);
		// extract year and month from token of format
		// 2002-01
		String dateString = pubDates.get(id);
		int year = 0;
		int month = 0;
		try {
			year = Integer.parseInt(dateString.substring(0, 4));
			month = Integer.parseInt(dateString.substring(5));
		} catch (NumberFormatException e) {
		}
		if (month != 0)
			pubDate.setMonth(month);
		if (year != 0) {
			pubDate.setYear(year);
			pubType.setPubDate(pubDate);
			LOGGER.debug("pubmed-id: {}, publication date: {}-{}", header.getDocId(), year, month);
		}
	}

	/**
	 * if publicationDatesFile is available, this method retrieves the
	 * publication date for the given article id and adds it to the passed
	 * header
	 * 
	 * @param header
	 * @param id
	 * @throws FileNotFoundException
	 */
	private Map<String, String> readIgnPubDates(String publicationDatesFilePath) throws FileNotFoundException {
		Map<String, String> pubDates = new HashMap<>();
		if (publicationDatesFilePath != null) {
			String publicationDatesFileResource = publicationDatesFilePath.startsWith("/") ? publicationDatesFilePath
					: "/" + publicationDatesFilePath;
			InputStream is = getClass().getResourceAsStream(publicationDatesFileResource);
			if (null == is) {
				File f = new File(publicationDatesFilePath);
				if (f.exists()) {
					LOGGER.debug("Loading IGN publication dates from file {}", f);
					is = new FileInputStream(f);
				}
			} else {
				LOGGER.debug("Loading resource \"{}\" from the classpath", publicationDatesFileResource);
			}
			if (null == is) {
				LOGGER.warn("Could not find {}. Publication dates will not be annotated.", publicationDatesFilePath);
			} else {
				// read the file and search for the given id
				try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
					String line = "";
					while ((line = br.readLine()) != null) {
						// split line at whitespace
						String[] tokens = line.split("\\s+");
						if ((tokens.length != 2) || (tokens[1].length() != 7))
							throw new IllegalArgumentException(
									"Format error in IGN publication date file. Make sure that there are two tab- or whitespace separated columns, first the PubMed ID, second the date and that the date is exactly of length 8, e.g. 2001-05. The errorneous line was: "
											+ line);
						pubDates.put(tokens[0].trim(), tokens[1].trim());
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else
			LOGGER.debug("Since the pubmedID2publication file is not given, publication dates will not be annotated.");
		return pubDates;
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return currentIndex < biocDocuments.size();
	}

	@Override
	public Progress[] getProgress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}
}
