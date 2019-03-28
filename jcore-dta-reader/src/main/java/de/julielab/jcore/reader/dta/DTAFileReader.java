/**
 * Copyright (c) 2015, JULIE Lab. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD 2-Clause License
 *
 *
 * @author hellrich
 *
 */

package de.julielab.jcore.reader.dta;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.ximpleware.ParseException;
import com.ximpleware.VTDNav;
import de.julielab.jcore.reader.dta.mapping.MappingService;
import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.extensions.dta.Header;
import de.julielab.jcore.types.extensions.dta.PersonInfo;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DTAFileReader extends CollectionReader_ImplBase {
	static final String COMPONENT_ID = DTAFileReader.class.getCanonicalName();

	public static final String PARAM_INPUTFILE = "inputFile";
	public static final String PARAM_NORMALIZE = "normalize";
	public static final String PARAM_FORMAT_2017 = "format2017";

	private static final String XPATH_TEXT_CORPUS = "/D-Spin/TextCorpus/";
	private static final String XPATH_TEI_HEADER_2016 = "/D-Spin/MetaData/source/CMD/Components/teiHeader/";
	private static final String XPATH_TEI_HEADER_2017 = "/D-Spin/MetaData/source/cmd:CMD/cmd:Components/cmdp:teiHeader/";

	private static final String XPATH_PROFILE_DESC_2016 = XPATH_TEI_HEADER_2016
			+ "profileDesc/";
	private static final String XPATH_PROFILE_DESC_2017 = XPATH_TEI_HEADER_2017
			+ "cmdp:profileDesc/";
	private static final String XPATH_TITLE_STMT_2016 = XPATH_TEI_HEADER_2016
			+ "fileDesc/titleStmt/";
	private static final String XPATH_TITLE_STMT_2017 = XPATH_TEI_HEADER_2017
			+ "cmdp:fileDesc/cmdp:titleStmt/";
	static final String XPATH_PUBLICATION_STMT_2016 = XPATH_TEI_HEADER_2016
			+ "fileDesc/sourceDesc/biblFull/publicationStmt/";
	static final String XPATH_PUBLICATION_STMT_2017 = XPATH_TEI_HEADER_2017
			+ "cmdp:fileDesc/cmdp:sourceDesc/cmdp:biblFull/cmdp:publicationStmt/";
	private static final String XPATH_YEAR_2016 = XPATH_PUBLICATION_STMT_2016 + "date";
	private static final String XPATH_YEAR_2017 = XPATH_PUBLICATION_STMT_2017 + "cmdp:date";
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DTAFileReader.class);
	private static final Joiner NEW_LINE_JOINER = Joiner.on("\n");


	/**
	 * Checks some assumptions about xml file, e.g., tagset and language
	 */
	static boolean formatIsOk(final String xmlFileName, final VTDNav nav, final boolean xpath2017) {
		// Tagset <POStags tagset="stts">
		for (final String tagset : mapAttribute2Text(xmlFileName, nav,
				XPATH_TEXT_CORPUS + "POStags", "@tagset").keySet())
			if (!tagset.equals("stts"))
				return false;
		final String langXpath = xpath2017 ? XPATH_PROFILE_DESC_2017 + "cmdp:langUsage/cmdp:language": XPATH_PROFILE_DESC_2016 + "langUsage/language";
		for (final String[] language : mapAttribute2Text(xmlFileName, nav,
				langXpath , ".").values())
			if ((language.length != 1) || !language[0].equals("German"))
				return false;
		return true;
	}

	/**
	 * Uses JulieXMLTools.constructRowIterator to extract an attribute from each
	 * matched element
	 */
	static Iterable<String> getAttributeForEach(final String xmlFileName,
			final VTDNav nav, final String forEachXpath,
			final String attributeXpath) {
		return new Iterable<String>() {

			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					final String text = "text";
					final String attribute = "attribute";
					final List<Map<String, String>> fields = new ArrayList<>();
					final Iterator<Map<String, Object>> tokenIterator;

					{
						fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
								text, JulieXMLConstants.XPATH, "."));
						fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
								attribute, JulieXMLConstants.XPATH,
								attributeXpath));
						tokenIterator = JulieXMLTools.constructRowIterator(nav,
								forEachXpath, fields, xmlFileName);
					}

					@Override
					public boolean hasNext() {
						return tokenIterator.hasNext();
					}

					@Override
					public String next() {
						return (String) tokenIterator.next().get(attribute);
					}

					@Override
					public void remove() {
						throw new IllegalAccessError();
					}
				};
			}
		};

	}

	/**
	 * Gets entry for id out of map if it contains exactly 1 string
	 */
	private static String getEntry(final String xmlFile, final String id,
			final Map<String, String[]> map1) {
		return getEntry(xmlFile, id, map1, null);
	}

	/**
	 * Gets entry for id out of preferredMap if it is not null and contains
	 * exactly 1 string, otherwise entry out of backupMap if exactly 1 string
	 */
	private static String getEntry(final String xmlFile, final String id,
			final Map<String, String[]> backupMap,
			final Map<String, String[]> preferredMap) {
		if (preferredMap != null) {
			final String[] s = preferredMap.get(id);
			if (s != null) {
				if (s.length != 1)
					throw new IllegalArgumentException("ID \"" + id
							+ "\" has not exactly one entry in " + xmlFile);
				return s[0];
			}
		}
		final String[] s = backupMap.get(id);
		if (s == null)
			throw new IllegalArgumentException("ID \"" + id
					+ "\" has no associated entry in " + xmlFile);

		if (s.length != 1)
			throw new IllegalArgumentException("ID \"" + id
					+ "\" has not exactly one entry in " + xmlFile);
		return s[0];
	}

	/**
	 * Extracts PersonInfo for a PersonType, all already added to indexes
	 */
	static FSArray getPersons(final JCas jcas, final VTDNav vn,
			final String xmlFileName, final PersonType personType, final boolean xpath2017) {
		final List<PersonInfo> personList = new ArrayList<>();
		final String forEachXpath = xpath2017 ? 
				XPATH_TITLE_STMT_2017 + "cmdp:"+personType + "/cmdp:persName" :
					XPATH_TITLE_STMT_2016 + personType + "/persName";

		final List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "surname",
				JulieXMLConstants.XPATH, 
				xpath2017 ? "cmdp:surname" :"surname" ));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "forename",
				JulieXMLConstants.XPATH, xpath2017 ? "cmdp:forename" : "forename"));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "idno",
				JulieXMLConstants.XPATH, xpath2017 ? "cmdp:idno/cmdp:idno[@type='PND']" :" idno/idno[@type='PND']"));
		final Iterator<Map<String, Object>> iterator = JulieXMLTools
				.constructRowIterator(vn, forEachXpath, fields, xmlFileName);
		while (iterator.hasNext()) {
			final PersonInfo person = new PersonInfo(jcas);
			final Map<String, Object> row = iterator.next();
			person.setSurename((String) row.get("surname"));
			person.setForename((String) row.get("forename"));
			person.setIdno((String) row.get("idno"));
			person.addToIndexes();
			personList.add(person);
		}
		final FSArray personArray = new FSArray(jcas, personList.size());
		personArray.copyFromArray(
				personList.toArray(new PersonInfo[personList.size()]), 0, 0,
				personList.size());
		personArray.addToIndexes();
		return personArray;
	}

	/**
	 * Uses JulieXMLTools.constructRowIterator to get all text entries for a
	 * given xpath
	 */
	static List<String> getTexts(final String xmlFileName, final VTDNav nav,
			final String forEachXpath) {
		final List<String> texts = new ArrayList<>();

		final String text = "text";
		final List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, text,
				JulieXMLConstants.XPATH, "."));
		final Iterator<Map<String, Object>> iterator = JulieXMLTools
				.constructRowIterator(nav, forEachXpath, fields, xmlFileName);
		while (iterator.hasNext())
			texts.add((String) iterator.next().get(text));

		return texts;
	}

	/**
	 * Uses JulieXMLTools.constructRowIterator to provide a mapping from an
	 * attribute to text for each matched element
	 */
	static Map<String, String[]> mapAttribute2Text(final String xmlFileName,
			final VTDNav nav, final String forEachXpath,
			final String attributeXpath) {
		final Map<String, String[]> attribute2text = new HashMap<>();

		final String text = "text";
		final String attribute = "attribute";
		final List<Map<String, String>> fields = new ArrayList<>();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, text,
				JulieXMLConstants.XPATH, "."));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, attribute,
				JulieXMLConstants.XPATH, attributeXpath));
		final Iterator<Map<String, Object>> iterator = JulieXMLTools
				.constructRowIterator(nav, forEachXpath, fields, xmlFileName);
		while (iterator.hasNext()) {
			final Map<String, Object> row = iterator.next();
			final String attributeValue = (String) row.get(attribute);
			if (attribute2text.containsKey(attributeValue)) {
				// hopefully rare case
				final String[] old = attribute2text.get(attributeValue);
				final String[] newOne = Arrays.copyOf(old, old.length + 1);
				newOne[old.length] = (String) row.get(text);
				attribute2text.put(attributeValue, newOne);
			} else
				attribute2text.put(attributeValue,
						new String[] { (String) row.get(text) });
		}
		return attribute2text;
	}

	/**
	 * Reads document text
	 */
	static void readDocument(final JCas jcas, final VTDNav nav,
			final String xmlFileName, final boolean normalize)
					throws ParseException, IOException {
		final Map<String, String[]> id2token = mapAttribute2Text(xmlFileName,
				nav, XPATH_TEXT_CORPUS + "tokens/token", "@ID");
		final Map<String, String[]> id2lemma = mapAttribute2Text(xmlFileName,
				nav, XPATH_TEXT_CORPUS + "lemmas/lemma", "@tokenIDs");
		final Map<String, String[]> id2pos = mapAttribute2Text(xmlFileName,
				nav, XPATH_TEXT_CORPUS + "POStags/tag", "@tokenIDs");
		final Map<String, String[]> id2correction = normalize ? mapAttribute2Text(
				xmlFileName, nav, XPATH_TEXT_CORPUS
				+ "orthography/correction[@operation='replace']",
				"@tokenIDs") : null;

				final StringBuilder text = new StringBuilder();
				int sentenceStart = 0;
				for (final String tokenIDs : getAttributeForEach(xmlFileName, nav,
						XPATH_TEXT_CORPUS + "sentences/sentence", "@tokenIDs")) {
					boolean first = true;
					for (final String id : tokenIDs.split(" ")) {
						final String tokenString = getEntry(xmlFileName, id, id2token,
								id2correction);
						if (tokenString.length() == 0)
							continue;
						final String posString = getEntry(xmlFileName, id, id2pos);
						final String lemmaString = getEntry(xmlFileName, id, id2lemma);

						if (first)
							first = false;
						else if (!(posString.equals("$,") || posString.equals("$.")))
							text.append(" ");

						final int begin = text.length();
						text.append(tokenString);
						final int end = text.length();

						final Token token = new Token(jcas, begin, end);
						token.setComponentId(COMPONENT_ID);

						final Lemma lemma = new Lemma(jcas, begin, end);
						lemma.setValue(lemmaString);
						lemma.addToIndexes();
						token.setLemma(lemma);

						final STTSPOSTag tag = new STTSPOSTag(jcas, begin, end);
						tag.setValue(posString);
						tag.setBegin(begin);
						tag.setEnd(end);
						tag.setComponentId(COMPONENT_ID);
						tag.addToIndexes();
						final FSArray postags = new FSArray(jcas, 1);
						postags.set(0, tag);
						token.setPosTag(postags);

						token.addToIndexes();
					}
					final Sentence sentence = new Sentence(jcas, sentenceStart,
							text.length());
					sentence.setComponentId(COMPONENT_ID);
					sentence.addToIndexes();
					text.append("\n");
					sentenceStart = text.length();
				}
				// No final newline
				jcas.setDocumentText(text.subSequence(0, text.length() - 1).toString());
	}

	/**
	 * Extracts meta information from header
	 *
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	static void readHeader(final JCas jcas, final VTDNav nav,
			final String xmlFileName, final boolean xpath2017) throws NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		final Header h = new Header(jcas);

		// titles
		final String titleXpath = xpath2017 ? 
				XPATH_TITLE_STMT_2017 +"cmdp:title"
				: XPATH_TITLE_STMT_2016 + "title";
		final Map<String, String[]> titles = mapAttribute2Text(xmlFileName,
				nav, titleXpath, "@type");
		h.setTitle(getEntry(xmlFileName, "main", titles));
		final String[] subTitle = titles.get("sub");
		if (subTitle != null)
			h.setSubtitle(NEW_LINE_JOINER.join(subTitle));
		boolean moreThanOne = false;
		for (final String volume : getAttributeForEach(xmlFileName, nav,
				titleXpath+"[@type='volume']", "@n")) {
			if (moreThanOne)
				throw new IllegalArgumentException(xmlFileName
						+ " has more than one volume!");
			h.setVolume(volume);
			moreThanOne = true;
		}

		// author
		h.setAuthors(getPersons(jcas, nav, xmlFileName, PersonType.author, xpath2017));
		h.setEditors(getPersons(jcas, nav, xmlFileName, PersonType.editor, xpath2017));

		// classification
		final String classXpath = xpath2017 ? 
				XPATH_PROFILE_DESC_2017 + "cmdp:textClass/cmdp:classCode"
				: XPATH_PROFILE_DESC_2016 + "textClass/classCode";
		final Map<String, String[]> classInfo = mapAttribute2Text(xmlFileName,
				nav, classXpath, "@scheme");

		h.setIsCoreCorpus(MappingService.isCoreCorpus(classInfo));

		final FSArray classifications = MappingService.getClassifications(jcas,
				xmlFileName, classInfo);
		if (classifications == null)
			throw new IllegalArgumentException(xmlFileName
					+ " missing classification!");
		h.setClassifications(classifications);

		//year
		final Map<String, String[]> yearInfo = mapAttribute2Text(xmlFileName,
				nav, (xpath2017 ? XPATH_YEAR_2017 : XPATH_YEAR_2016), "@type");
		String[] year = null;
		if (yearInfo.containsKey("creation"))
			year = yearInfo.get("creation");
		else if (yearInfo.containsKey("publication"))
			year = yearInfo.get("publication");
		if (year == null)
			throw new IllegalArgumentException(xmlFileName
					+ " has no creation/publication year!");
		if (year.length > 1)
			throw new IllegalArgumentException(xmlFileName
					+ " has multiple creation/publication years!");
		h.setYear(year[0]);

		//publication
		final String placeXpath = xpath2017 ? 
				XPATH_PUBLICATION_STMT_2017
				+ "cmdp:pubPlace"
				: XPATH_PUBLICATION_STMT_2016
				+ "pubPlace";
		final List<String> publicationPlaces = DTAFileReader.getTexts(
				xmlFileName, nav, placeXpath);
		final StringArray pubArray = new StringArray(jcas,
				publicationPlaces.size());
		for (int i = 0; i < publicationPlaces.size(); ++i)
			pubArray.set(i, publicationPlaces.get(i));
		pubArray.addToIndexes();
		h.setPublicationPlaces(pubArray);

		final String publisherXpath = xpath2017 ? 
				XPATH_PUBLICATION_STMT_2017 + "cmdp:publisher/cmdp:name"
				: XPATH_PUBLICATION_STMT_2016 + "publisher/name";
		final List<String> publisher = DTAFileReader.getTexts(xmlFileName, nav,
				publisherXpath);
		final StringArray publisherArray = new StringArray(jcas,
				publisher.size());
		for (int i = 0; i < publisher.size(); ++i)
			publisherArray.set(i, publisher.get(i));
		publisherArray.addToIndexes();
		h.setPublishers(publisherArray);

		h.addToIndexes();
	}


	private final List<File> inputFiles = new ArrayList<>();

	private int counter = 0;
	private boolean format2017 = true;
	private boolean normalize = true;

	@Override
	public void close() throws IOException {
	}

	@Override
	public void getNext(final CAS aCAS) throws CollectionException {
		try {
			final JCas jcas = aCAS.getJCas();
			final File file = inputFiles.get(counter);
			final VTDNav nav = JulieXMLTools.getVTDNav(
					new FileInputStream(file), 1024);
			final String xmlFileName = file.getCanonicalPath();
			LOGGER.info("Reading file:" + counter + " - " + xmlFileName);
			if (!formatIsOk(xmlFileName, nav, format2017))
				LOGGER.info("Skipping file:" + counter + " - " + xmlFileName);
			else {
				readDocument(jcas, nav, xmlFileName, normalize);
				readHeader(jcas, nav, xmlFileName,format2017);
				LOGGER.info("Read file:" + counter + " - " + xmlFileName);
			}
			counter++;
		} catch (final Exception e) {
			throw new CollectionException(e);
		}
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(counter, inputFiles.size(),
				Progress.ENTITIES) };
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return counter < inputFiles.size();
	}

	@Override
	public void initialize() throws ResourceInitializationException {
		final String filename = (String) this
				.getConfigParameterValue(PARAM_INPUTFILE);
		
		final Object o = this.getConfigParameterValue(PARAM_NORMALIZE);
		if (o != null)
			normalize = (boolean) o;
		
		final Object o2 = this.getConfigParameterValue(PARAM_FORMAT_2017);
		if (o2 != null)
			format2017 = (boolean) o2;
		
		LOGGER.info("Parameters: inputfile=" +filename + " normalize= " + normalize + " 2017format="+format2017);

		final File inputFile = new File(filename);

		if (!inputFile.exists())
			throw new IllegalArgumentException(filename + " does not exist!");
		else if (inputFile.isFile() && inputFile.getName().endsWith(".tcf.xml"))
			inputFiles.add(inputFile);
		else {
			final File[] files = inputFile.listFiles();
			if (files == null)
				throw new IllegalArgumentException("Unsure if " + filename
						+ " is a directroy...");
			for (final File f : files)
				if (f.isFile() && f.getName().endsWith(".tcf.xml"))
					inputFiles.add(f);
		}

		LOGGER.info("Input contains " + inputFiles.size() + " xml files.");

	}

}
