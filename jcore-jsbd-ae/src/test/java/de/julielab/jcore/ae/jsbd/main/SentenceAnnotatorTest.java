/**
 * SentenceAnnotatorTest.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: tomanek
 * <p>
 * Current version: 2.2
 * Since version:   1.0
 * <p>
 * Creation date: Nov 29, 2006
 * <p>
 * This is a JUnit test for the SentenceAnnotator.
 **/

package de.julielab.jcore.ae.jsbd.main;

import de.julielab.jcore.ae.jsbd.types.TestScope;
import de.julielab.jcore.types.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Range;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

public class SentenceAnnotatorTest {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SentenceAnnotatorTest.class);

    private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";

    // uncomment to test with/without scope
    // private static final String DESCRIPTOR =
    // "src/test/resources/de/julielab/jcore/ae/jsbd/desc/SentenceAnnotatorTest.xml";
    private static final String DESCRIPTOR = "src/test/resources/de/julielab/jcore/ae/jsbd/desc/SentenceAnnotator_with-scope_Test.xml";

    // last sentence has no EOS symbol to test that also this is handled
    // correctly
    private static final String[] TEST_TEXT = {"First sentence. Second \t sentence! \n    Last sentence?",
            "Hallo, jemand da? Nein, niemand.", "A test. It can't be just one sentence. Testing the test.", ""};

    private static final String[] TEST_TEXT_OFFSETS = {"0-15;16-34;40-54", "0-17;18-32", "0-7;8-38;39-56", ""};

    private static final int[] endOffsets = {54, 32, 27, 0};

    /**
     * Use the model in resources, split the text in TEST_TEXT and compare the
     * split result against TEST_TEXT_OFFSETS
     */
    @Test
    public void testProcess() {

        boolean annotationsOK = true;

        XMLInputSource sentenceXML = null;
        ResourceSpecifier sentenceSpec = null;
        AnalysisEngine sentenceAnnotator = null;

        try {
            sentenceXML = new XMLInputSource(DESCRIPTOR);
            sentenceSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(sentenceXML);
            sentenceAnnotator = UIMAFramework.produceAnalysisEngine(sentenceSpec);
        } catch (Exception e) {
            LOGGER.error("testProcess()", e);
        }

        for (int i = 0; i < TEST_TEXT.length; i++) {

            JCas jcas = null;
            try {
                jcas = sentenceAnnotator.newJCas();
            } catch (ResourceInitializationException e) {
                LOGGER.error("testProcess()", e);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("testProcess() - testing text: " + TEST_TEXT[i]);
            }
            jcas.setDocumentText(TEST_TEXT[i]);

            // make one test scope ranging over complete document text
            // annotations for the processing scope
            TestScope scope1 = new TestScope(jcas, 0, endOffsets[i]);
            scope1.addToIndexes();
            // TestScope scope2 = new TestScope(jcas,37,54);


            try {
                sentenceAnnotator.process(jcas, null);
            } catch (Exception e) {
                LOGGER.error("testProcess()", e);
            }

            // get the offsets of the sentences
            JFSIndexRepository indexes = jcas.getJFSIndexRepository();
            Iterator sentIter = indexes.getAnnotationIndex(Sentence.type).iterator();

            String predictedOffsets = getPredictedOffsets(i, sentIter);

            // compare offsets
            if (!predictedOffsets.equals(TEST_TEXT_OFFSETS[i])) {
                annotationsOK = false;
                continue;
            }
        }
        assertTrue(annotationsOK);
    }


    private String getPredictedOffsets(int i, Iterator sentIter) {
        String predictedOffsets = "";
        while (sentIter.hasNext()) {
            Sentence s = (Sentence) sentIter.next();
            LOGGER.debug("sentence: " + s.getCoveredText() + ": " + s.getBegin() + " - " + s.getEnd());
            predictedOffsets += (predictedOffsets.length() > 0) ? ";" : "";
            predictedOffsets += s.getBegin() + "-" + s.getEnd();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("testProcess() - predicted: " + predictedOffsets);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("testProcess() - wanted: " + TEST_TEXT_OFFSETS[i]);
        }
        return predictedOffsets;
    }

    @Test
    public void testUimaFitIntegration() throws UIMAException, IOException {
        AnalysisEngine sentenceAE = AnalysisEngineFactory.createEngine(SentenceAnnotator.class,
                SentenceAnnotator.PARAM_MODEL_FILE, "de/julielab/jcore/ae/jsbd/model/test-model.gz",
                SentenceAnnotator.PARAM_POSTPROCESSING, "biomed");
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
        String abstractText = FileUtils.readFileToString(new File("src/test/resources/test-abstract.txt"), "UTF-8");
        cas.setDocumentText(abstractText);
        sentenceAE.process(cas);
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        for (Sentence sentence : sentences) {
            System.out.println(sentence.getCoveredText());
        }
        assertEquals(14, sentences.size());
    }

    @Test
    public void testModelClassPathResource() throws Exception {
        AnalysisEngine sentenceAE = AnalysisEngineFactory.createEngine(SentenceAnnotator.class,
                SentenceAnnotator.PARAM_MODEL_FILE, "de/julielab/jcore/ae/jsbd/model/test-model.gz",
                SentenceAnnotator.PARAM_POSTPROCESSING, "biomed");
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
        String abstractText = FileUtils.readFileToString(new File("src/test/resources/test-abstract.txt"), "UTF-8");
        cas.setDocumentText(abstractText);
        sentenceAE.process(cas);
        Collection<Sentence> sentences = JCasUtil.select(cas, Sentence.class);
        System.out.println(sentences.size());
        for (Sentence sentence : sentences) {
            System.out.println(sentence.getCoveredText());
        }
        assertEquals(14, sentences.size());
    }

    @Test
    public void testSentenceDelimiterTypes() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-types");

        jCas.setDocumentText("Introduction " + "We here show good results. This is a figure caption "
                + "And this is a paragraph without a fullstop for some reason " + "Conclusion "
                + "We are the greatest.");
        Title t1 = new Title(jCas, 0, 12);
        Caption c = new Caption(jCas, 40, 64);
        Paragraph p = new Paragraph(jCas, 65, 123);
        Title t2 = new Title(jCas, 124, 134);
        t1.addToIndexes();
        c.addToIndexes();
        p.addToIndexes();
        t2.addToIndexes();
        assertEquals("Introduction", t1.getCoveredText());
        assertEquals("This is a figure caption", c.getCoveredText());
        assertEquals("And this is a paragraph without a fullstop for some reason", p.getCoveredText());
        assertEquals("Conclusion", t2.getCoveredText());

        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz", SentenceAnnotator.PARAM_SENTENCE_DELIMITER_TYPES,
                new LinkedHashSet<Object>(
                        Arrays.asList(Title.class.getName(), Caption.class.getName(), Paragraph.class.getName())));

        jsbd.process(jCas.getCas());

        Set<Range<Integer>> expectedSpans = new HashSet<>();
        expectedSpans.add(Range.between(0, 12));
        expectedSpans.add(Range.between(13, 39));
        expectedSpans.add(Range.between(40, 64));
        expectedSpans.add(Range.between(65, 123));
        expectedSpans.add(Range.between(124, 134));
        expectedSpans.add(Range.between(135, 155));

        FSIterator<Annotation> it = jCas.getAnnotationIndex(Sentence.type).iterator();
        assertTrue(it.hasNext());
        while (it.hasNext()) {
            Annotation sentence = it.next();
            Range<Integer> sentenceRange = Range.between(sentence.getBegin(), sentence.getEnd());
            assertTrue(expectedSpans.remove(sentenceRange), "Range " + sentenceRange + " was not expected");
        }
        assertTrue(expectedSpans.isEmpty());
    }

    @Test
    public void testSentenceWhitespaces() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-types");

        // This text is taken from pmid 23092121
        jCas.setDocumentText("  : We present a theoretical study of the electronic subband structure and collective electronic excitation associated with plasmon and surface plasmon modes in metal-based hollow nanosphere. The dependence of the electronic subband energy on the sample parameters of the hollow nanosphere is examined.");

        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz");

        jsbd.process(jCas.getCas());


        Sentence sentence = JCasUtil.select(jCas, Sentence.class).iterator().next();
        assertFalse(sentence.getCoveredText().startsWith(" "));
    }

    @Test
    public void testTrailingNewline() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-types");

        // This text is taken from PMC3408706. Note the "paragraph separator" at the end
        jCas.setDocumentText("In1 the next step, we plan to use higher level QM/MM methods to calculate the energy barrier of the reaction catalyzed by endonuclease APE1, in compliance with the mechanism proposed, and to screen for effective inhibitors with the use of the constructed mechanistic full-atomic model of the enzyme.    \u2029");
        new InternalReference(jCas, 2, 3).addToIndexes();

        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz", SentenceAnnotator.PARAM_CUT_AWAY_TYPES, new String[]{InternalReference.class.getCanonicalName()});

        jsbd.process(jCas.getCas());


        Sentence sentence = JCasUtil.select(jCas, Sentence.class).iterator().next();
        assertFalse(sentence.getCoveredText().endsWith("\u2029"));
    }

    @Test
    public void testSplitAtNewlines() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-types");

        String ls = System.getProperty("line.separator");
        jCas.setDocumentText("line1" + ls + "line2" + ls + "line3");

        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz", SentenceAnnotator.PARAM_ALWAYS_SPLIT_NEWLINE, true);

        jsbd.process(jCas.getCas());


        Collection<String> sentences = JCasUtil.select(jCas, Sentence.class).stream().map(Annotation::getCoveredText).collect(Collectors.toList());
        assertThat(sentences).containsExactly("line1", "line2", "line3");
    }


    @Test
    public void testErrordoc() throws Exception {
        // The XMI document uses here is from PMC and is an example of a source of error the previously occurred.
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");

        XmiCasDeserializer.deserialize(new FileInputStream(Path.of("src", "test", "resources", "errordocs", "PMC5478802.xmi").toFile()), jCas.getCas());
        JCasUtil.select(jCas, Sentence.class).forEach(Annotation::removeFromIndexes);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz",
                SentenceAnnotator.PARAM_MAX_SENTENCE_LENGTH, 1000,
                SentenceAnnotator.PARAM_SENTENCE_DELIMITER_TYPES, new String[]{
                        "de.julielab.jcore.types.Title", "de.julielab.jcore.types.AbstractText", "de.julielab.jcore.types.AbstractSectionHeading", "de.julielab.jcore.types.AbstractSection", "de.julielab.jcore.types.Section", "de.julielab.jcore.types.Paragraph", "de.julielab.jcore.types.Zone", "de.julielab.jcore.types.Caption", "de.julielab.jcore.types.Figure", "de.julielab.jcore.types.Table"},
                SentenceAnnotator.PARAM_CUT_AWAY_TYPES, new String[]{de.julielab.jcore.types.pubmed.InternalReference.class.getCanonicalName()}
        );

        assertThatCode(() -> jsbd.process(jCas.getCas())).doesNotThrowAnyException();
    }

    @Test
    public void testErrordoc2() throws Exception {
        // This XMI file has larger cut away types where an original offset request actually lies inside of a
        // cut away annotation. This case led to errors prior to a respective bug fix in the
        // JCoReCondensedDocumentText
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");

        XmiCasDeserializer.deserialize(new FileInputStream(Path.of("src", "test", "resources", "errordocs", "PMC8205280.xmi").toFile()), jCas.getCas());
        JCasUtil.select(jCas, Sentence.class).forEach(Annotation::removeFromIndexes);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine(SentenceAnnotator.class, SentenceAnnotator.PARAM_MODEL_FILE,
                "de/julielab/jcore/ae/jsbd/model/test-model.gz",
                SentenceAnnotator.PARAM_MAX_SENTENCE_LENGTH, 1000,
                SentenceAnnotator.PARAM_SENTENCE_DELIMITER_TYPES, new String[]{
                        "de.julielab.jcore.types.Title", "de.julielab.jcore.types.AbstractText", "de.julielab.jcore.types.AbstractSectionHeading", "de.julielab.jcore.types.AbstractSection", "de.julielab.jcore.types.Section", "de.julielab.jcore.types.Paragraph", "de.julielab.jcore.types.Zone", "de.julielab.jcore.types.Caption", "de.julielab.jcore.types.Figure", "de.julielab.jcore.types.Table"},
                SentenceAnnotator.PARAM_CUT_AWAY_TYPES, new String[]{de.julielab.jcore.types.pubmed.InternalReference.class.getCanonicalName()}
        );

        assertThatCode(() -> jsbd.process(jCas.getCas())).doesNotThrowAnyException();
    }

}

