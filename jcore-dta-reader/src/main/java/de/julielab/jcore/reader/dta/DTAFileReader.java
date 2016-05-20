/**
 * Copyright (c) 2015, JULIE Lab. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the GNU Lesser
 * General Public License (LGPL) v3.0
 *
 *
 * @author hellrich
 *
 */

package de.julielab.jcore.reader.dta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.ximpleware.ParseException;
import com.ximpleware.VTDNav;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.STTSPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import de.julielab.jcore.types.extensions.dta.DTAFachtext;
import de.julielab.jcore.types.extensions.dta.DTAGebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DTAOther;
import de.julielab.jcore.types.extensions.dta.DWDS1Belletristik;
import de.julielab.jcore.types.extensions.dta.DWDS1Gebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DWDS1Wissenschaft;
import de.julielab.jcore.types.extensions.dta.DWDS1Zeitung;
import de.julielab.jcore.types.extensions.dta.DWDS2Belletristik;
import de.julielab.jcore.types.extensions.dta.DWDS2Gebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DWDS2Roman;
import de.julielab.jcore.types.extensions.dta.DWDS2Traktat;
import de.julielab.jcore.types.extensions.dta.DWDS2Wissenschaft;
import de.julielab.jcore.types.extensions.dta.DWDS2Zeitung;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;
import de.julielab.jcore.types.extensions.dta.Header;
import de.julielab.jcore.types.extensions.dta.PersonInfo;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;

public class DTAFileReader extends CollectionReader_ImplBase {
    private static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
    private static final String CLASIFICATION_DTA_CORPUS = CLASIFICATION
            + "DTACorpus";
    private static final String CLASIFICATION_DTA_SUB = CLASIFICATION
            + "dtasub";
    private static final String CLASIFICATION_DTA_MAIN = CLASIFICATION
            + "dtamain";
    private static final String CLASIFICATION_DWDS1_SUB = CLASIFICATION
            + "dwds1sub";
    private static final String CLASIFICATION_DWDS1_MAIN = CLASIFICATION
            + "dwds1main";
    private static final String CLASIFICATION_DWDS2_SUB = CLASIFICATION
            + "dwds2sub";
    private static final String CLASIFICATION_DWDS2_MAIN = CLASIFICATION
            + "dwds2main";
    static final String COMPONENT_ID = DTAFileReader.class.getCanonicalName();
    static final String DESCRIPTOR_PARAMTER_INPUTFILE = "inputFile";
    static final String DESCRIPTOR_PARAMTER_NORMALIZE = "normalize";

    private static final String XPATH_TEXT_CORPUS = "/D-Spin/TextCorpus/";
    private static final String XPATH_PROFILE_DESC = "/D-Spin/MetaData/source/CMD/Components/teiHeader/profileDesc/";
    private static final String XPATH_TITLE_STMT = "/D-Spin/MetaData/source/CMD/Components/teiHeader/fileDesc/titleStmt/";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DTAFileReader.class);
    private static final Map<String, Class<? extends DocumentClassification>> DTA_MAPPING = ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
            .put("Belletristik", DTABelletristik.class)
            .put("Fachtext", DTAFachtext.class)
            .put("Gebrauchsliteratur", DTAGebrauchsliteratur.class).build();
    private static final Map<String, Class<? extends DocumentClassification>> DWDS1_MAPPING = ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
            .put("Wissenschaft", DWDS1Wissenschaft.class)
            .put("Gebrauchsliteratur", DWDS1Gebrauchsliteratur.class)
            .put("Belletristik", DWDS1Belletristik.class)
            .put("Zeitung", DWDS1Zeitung.class).build();

    private static final Map<String, Class<? extends DocumentClassification>> DWDS2_MAPPING = ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
            .put("Wissenschaft", DWDS2Wissenschaft.class)
            .put("Gebrauchsliteratur", DWDS2Gebrauchsliteratur.class)
            .put("Belletristik", DWDS2Belletristik.class)
            .put("Zeitung", DWDS2Zeitung.class)
            .put("Traktat", DWDS2Traktat.class).put("Roman", DWDS2Roman.class)
            .build();

    private static void addClassification(final JCas jcas,
            final List<DocumentClassification> classifications,
            final Map<String, String[]> classInfo,
            final String mainClassification, final String subClassification,
            final Class<? extends DocumentClassification> defaultClass,
            final Map<String, Class<? extends DocumentClassification>> classification2class)
            throws NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (classInfo.containsKey(mainClassification)) {
            if (classInfo.get(mainClassification).length != 1)
                throw new IllegalArgumentException(
                        "More than 1 " + mainClassification + " class");
            if (classInfo.get(subClassification) == null)
                throw new IllegalArgumentException(
                        "No " + subClassification + "!");
            if (classInfo.get(subClassification).length != 1)
                throw new IllegalArgumentException(
                        "More than 1 " + subClassification + " class");
            final String mainClass = classInfo.get(mainClassification)[0];
            final String subClass = classInfo.get(subClassification)[0];

            Class<? extends DocumentClassification> aClass = classification2class
                    .get(mainClass);
            if (aClass == null) {
                if (defaultClass == null)
                    throw new IllegalArgumentException(
                            mainClass + " not supported!");
                else {
                    aClass = defaultClass;
                }
            }
            final Constructor<? extends DocumentClassification> constructor = aClass
                    .getConstructor(new Class[] { JCas.class });
            final DocumentClassification classification = constructor
                    .newInstance(jcas);
            classification.setClassification(mainClass);
            classification.setSubClassification(subClass);
            classification.addToIndexes();
            classifications.add(classification);
        }
    }

    /**
     * Checks some assumptions about xml file, e.g., tagset and language
     */
    static boolean formatIsOk(final String xmlFileName, final VTDNav nav) {
        // Tagset <POStags tagset="stts">
        for (final String tagset : mapAttribute2Text(xmlFileName, nav,
                XPATH_TEXT_CORPUS + "POStags", "@tagset").keySet())
            if (!tagset.equals("stts"))
                return false;
        for (final String[] language : mapAttribute2Text(xmlFileName, nav,
                XPATH_PROFILE_DESC + "langUsage/language", ".").values())
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
                        this.fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
                                this.text, JulieXMLConstants.XPATH, "."));
                        this.fields.add(ImmutableMap.of(JulieXMLConstants.NAME,
                                this.attribute, JulieXMLConstants.XPATH,
                                attributeXpath));
                        this.tokenIterator = JulieXMLTools.constructRowIterator(
                                nav, forEachXpath, this.fields, xmlFileName);
                    }

                    @Override
                    public boolean hasNext() {
                        return this.tokenIterator.hasNext();
                    }

                    @Override
                    public String next() {
                        return (String) this.tokenIterator.next()
                                .get(this.attribute);
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
    private static String getEntry(final String id,
            final Map<String, String[]> map1) {
        return getEntry(id, map1, null);
    }

    /**
     * Gets entry for id out of map2 if it is not null and contains exactly 1
     * string, otherwise entry out of map1 if exactly 1 string
     */
    private static String getEntry(final String id,
            final Map<String, String[]> map1,
            final Map<String, String[]> map2) {
        if (map2 != null) {
            final String[] s = map2.get(id);
            if (s != null) {
                if (s.length != 1)
                    throw new IllegalArgumentException(
                            "ID \"" + id + "\" has not exactly one entry!");
                return s[0];
            }
        }
        final String[] s = map1.get(id);
        if (s == null)
            throw new IllegalArgumentException(
                    "ID \"" + id + "\" has no associated entry!");

        if (s.length != 1)
            throw new IllegalArgumentException(
                    "ID \"" + id + "\" has not exactly one entry!");
        return s[0];
    }

    /**
     * Extracts PersonInfo for a PersonType, all already added to indexes
     */
    static FSArray getPersons(final JCas jcas, final VTDNav vn,
            final String xmlFileName, final PersonType personType) {
        final List<PersonInfo> personList = new ArrayList<>();
        final String forEachXpath = XPATH_TITLE_STMT + personType + "/persName";

        final List<Map<String, String>> fields = new ArrayList<>();
        fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "surname",
                JulieXMLConstants.XPATH, "surname"));
        fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "forename",
                JulieXMLConstants.XPATH, "forename"));
        fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "idno",
                JulieXMLConstants.XPATH, "idno/idno[@type='PND']"));
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
            } else {
                attribute2text.put(attributeValue,
                        new String[] { (String) row.get(text) });
            }
        }
        return attribute2text;
    }

    /**
     * Reads document text
     */
    static void readDocument(final JCas jcas, final VTDNav nav,
            final String xmlFileName, final boolean normalize)
            throws ParseException, IOException {
        if (!formatIsOk(xmlFileName, nav))
            throw new IllegalArgumentException(
                    xmlFileName + " does not conform to assumptions!");

        final Map<String, String[]> id2token = mapAttribute2Text(xmlFileName,
                nav, XPATH_TEXT_CORPUS + "tokens/token", "@ID");
        final Map<String, String[]> id2lemma = mapAttribute2Text(xmlFileName,
                nav, XPATH_TEXT_CORPUS + "lemmas/lemma", "@tokenIDs");
        final Map<String, String[]> id2pos = mapAttribute2Text(xmlFileName, nav,
                XPATH_TEXT_CORPUS + "POStags/tag", "@tokenIDs");
        final Map<String, String[]> id2correction = normalize
                ? mapAttribute2Text(xmlFileName, nav,
                        XPATH_TEXT_CORPUS
                                + "orthography/correction[@operation='replace']",
                        "@tokenIDs")
                : null;

        final StringBuilder text = new StringBuilder();
        int sentenceStart = 0;
        for (final String tokenIDs : getAttributeForEach(xmlFileName, nav,
                XPATH_TEXT_CORPUS + "sentences/sentence", "@tokenIDs")) {
            boolean first = true;
            for (final String id : tokenIDs.split(" ")) {
                final String tokenString = getEntry(id, id2token,
                        id2correction);
                final String posString = getEntry(id, id2pos);
                final String lemmaString = getEntry(id, id2lemma);

                if (first) {
                    first = false;
                } else if (!(posString.equals("$,")
                        || posString.equals("$."))) {
                    text.append(" ");
                }

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
            final String xmlFileName) throws NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        final Header h = new Header(jcas);

        // titles
        final Map<String, String[]> titles = mapAttribute2Text(xmlFileName, nav,
                XPATH_TITLE_STMT + "title", "@type");
        h.setTitle(getEntry("main", titles));
        final String[] subTitle = titles.get("sub");
        if (subTitle != null) {
            if (subTitle.length > 1)
                throw new IllegalArgumentException(
                        xmlFileName + " has more than one sub title!");
            h.setSubtitle(subTitle[0]);
        }
        boolean moreThanOne = false;
        for (final String volume : getAttributeForEach(xmlFileName, nav,
                XPATH_TITLE_STMT + "title[@type='volume']", "@n")) {
            if (moreThanOne)
                throw new IllegalArgumentException(
                        xmlFileName + " has more than one volume!");
            h.setVolume(volume);
            moreThanOne = true;
        }

        // author
        h.setAuthors(getPersons(jcas, nav, xmlFileName, PersonType.author));
        h.setEditors(getPersons(jcas, nav, xmlFileName, PersonType.editor));

        // classification
        final Map<String, String[]> classInfo = mapAttribute2Text(xmlFileName,
                nav, XPATH_PROFILE_DESC + "textClass/classCode", "@scheme");

        h.setIsCoreCorpus(classInfo.containsKey(CLASIFICATION_DTA_CORPUS)
                && Arrays.asList(classInfo.get(CLASIFICATION_DTA_CORPUS))
                        .contains("core"));

        final List<DocumentClassification> classifications = new ArrayList<>();
        addClassification(jcas, classifications, classInfo,
                CLASIFICATION_DTA_MAIN, CLASIFICATION_DTA_SUB, DTAOther.class,
                DTA_MAPPING);
        addClassification(jcas, classifications, classInfo,
                CLASIFICATION_DWDS1_MAIN, CLASIFICATION_DWDS1_SUB, null,
                DWDS1_MAPPING);
        addClassification(jcas, classifications, classInfo,
                CLASIFICATION_DWDS2_MAIN, CLASIFICATION_DWDS2_SUB, null,
                DWDS2_MAPPING);
        if (classifications.isEmpty())
            throw new IllegalArgumentException("Missing classification!");
        final FSArray classificationsArray = new FSArray(jcas,
                classifications.size());
        classificationsArray.copyFromArray(
                classifications
                        .toArray(new FeatureStructure[classifications.size()]),
                0, 0, classifications.size());
        h.setClassifications(classificationsArray);

        h.addToIndexes();
    }

    private final List<File> inputFiles = new ArrayList<>();

    private int counter = 0;

    private boolean normalize = false;

    @Override
    public void close() throws IOException {
    }

    @Override
    public void getNext(final CAS aCAS) throws CollectionException {
        try {
            final JCas jcas = aCAS.getJCas();
            final File file = this.inputFiles.get(this.counter);
            final VTDNav nav = JulieXMLTools
                    .getVTDNav(new FileInputStream(file), 1024);
            final String xmlFileName = file.getCanonicalPath();
            readDocument(jcas, nav, xmlFileName, this.normalize);
            readHeader(jcas, nav, xmlFileName);
            this.counter++;
            LOGGER.info("Read file:" + this.counter);
        } catch (final Exception e) {
            throw new CollectionException(e);
        }
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(this.counter,
                this.inputFiles.size(), Progress.ENTITIES) };
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return this.counter < this.inputFiles.size();
    }

    @Override
    public void initialize() throws ResourceInitializationException {
        final String filename = (String) this
                .getConfigParameterValue(DESCRIPTOR_PARAMTER_INPUTFILE);
        final Object o = this
                .getConfigParameterValue(DESCRIPTOR_PARAMTER_NORMALIZE);
        if (o != null) {
            this.normalize = (boolean) o;
        }
        this.normalize = true;

        final File inputFile = new File(filename);

        if (!inputFile.exists())
            throw new IllegalArgumentException(filename + " does not exist!");
        else if (inputFile.isFile()
                && inputFile.getName().endsWith(".tcf.xml")) {
            this.inputFiles.add(inputFile);
        } else {
            final File[] files = inputFile.listFiles();
            if (files == null)
                throw new IllegalArgumentException(
                        "Unsure if " + filename + " is a directroy...");
            for (final File f : files)
                if (f.isFile() && f.getName().endsWith(".tcf.xml")) {
                    this.inputFiles.add(f);
                }
        }

        LOGGER.info("Input contains " + this.inputFiles.size() + " xml files.");

    }

}
