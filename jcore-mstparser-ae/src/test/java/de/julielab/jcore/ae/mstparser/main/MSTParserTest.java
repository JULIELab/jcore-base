/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.mstparser.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.DependencyRelation;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import junit.framework.TestCase;

/**
 * This is the JUnit test for the MST Parser Annotator.
 *
 * @author Lichtenwald
 */
public class MSTParserTest extends TestCase {
    private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";

    public static final String PARAM_MAX_NUM_TOKENS = "MaxNumTokens";

    public static final int MAX_NUM_TOKENS = 5;

    static final String TEST_XMI = "src/test/resources/de/julielab/jcore/ae/mstparser/data/input/news_text_stp.xmi";

    static final String DESCRIPTOR_MST_PARSER = "src/test/resources/de/julielab/jcore/ae/mstparser/desc/MSTParserDescriptorTest.xml";

    public static final String OUTPUT_DIR = "src/test/resources/de/julielab/jcore/ae/mstparser/data/output";

    private static final Logger LOGGER = LoggerFactory.getLogger(MSTParserTest.class);

    /*--------------------------------------------------------------------------------------------*/

    @Ignore
//    public void testCAS() throws Exception {
//        // String[] heads = new String[] { "have", "Migrants", "drown", "coast", "off", "40", "40", "migrants", "have",
//        // "have", "drowned", "Sea", "Sea", "in", "drowned", "coast", "coast", "off", "coast", "of", "drowned",
//        // "drowned", "officials", "have", "corpses", "were", "were", "found", "washed", "found", "shore", "on",
//        // "shore", "city", "near", "city", "of", "were", "people", "were", "were", "rescued", "rescued", "s", "s",
//        // "s", "officials", "agency", "agency", "s", "were", "say", "boat", "say", "say", "was", "overloaded",
//        // "overloaded", "60", "60", "people", "carrying", "overloaded", "sank", "overloaded", "sank", "on",
//        // "Saturday", // actually should be 'sank'
//        // "weather", "Saturday", // actually should be 'on'
//        // "say", "Sea", "Sea", "is", "route", "route", "is", "route", "immigrants", "for", "immigrants", "trying",
//        // "to", "Union", "Union", "for", "is", "smugglers", "smugglers", "take", "take", "take", "take", "coast",
//        // "coast", "from", "coast", "islands", "islands", "to", "islands", "on", "fragile", "boats", "on", "take",
//        // "were", "Two", "of", "those", "were", "be", "reported", "be", "were" };
//        String[] heads = new String[] { "have", "Migrants", "drown", "coast", "off", "drown", // "40",
//                "40", "migrants", "have", "have", "drowned", "Sea", "Sea", "in", "Sea", // "drowned",
//                "coast", "coast", "off", "coast", "of", "have", // "drowned",
//                "have", // "drowned",
//                "officials", "have", "corpses", "were", "were", "found", "washed", "were", // "found",
//                "shore", "on", "shore", "city", "near", "city", "of", "were", "people", "were", "were", "were",//"Turkey", // "rescued",
//                "Turkey", // "rescued",
//                "Turkey", // "s",
//                "agency", // "s",
//                "agency", // "s",
//                "agency", // "officials",
//                "agency", "agency", "rescued", // "s",
//                "were", "say", "boat", "was", // "say",
//                "say", "was", "than", // "overloaded",
//                "than", // "overloaded",
//                "than", // "60",
//                "overloaded", // "60",
//                "people", "than", // "carrying",
//                "people", // "overloaded",
//                "sank", "than", // "overloaded",
//                "sank", "on", "was", // "Saturday",
//                "weather", "in", // "Saturday",
//                "was", // "say",
//                "Sea", "Sea", "is", "route", "route", "is", "route", "immigrants", "for", "immigrants", "trying",
//                "Union", // "to",
//                "Union", "Union", "trying", // "for",
//                "is", "say", // "smugglers",
//                "smugglers", "take", "smugglers", // "take",
//                "take", "take", "coast", "coast", "from", "coast", "islands", "islands", "to", "islands", "on", "take", // "fragile",
//                "boats", "take", // "on",
//                "take", "Two", // "were",
//                "rescued", // "Two",
//                "of", "of", // "those",
//                "were", "be", "reported", "of", // "be",
//                "Two", // "were"
//        };
//
//        String[] coveredTexts = new String[] { "Migrants", "", "", "", "", "", "", "", "drown", "off", "Turkish",
//                "coast", "At", "least", "40", "migrants", "drowned", "in", "the", "Aegean", "Sea", "off", "the",
//                "western", "coast", "of", "Turkey", ",", "officials", "say", ".", "The", "corpses", "found", "washed",
//                "up", "on", "the", "shore", "near", "the", "city", "of", "Izmir", ".", "Six", "people", "rescued", ",",
//                "officials", "told", "Turkey", "'", "s", "Anatolia", "news", "agency", ".", "They", "the", "boat",
//                "was", "overloaded", ",", "carrying", "more", "than", "60", "people", "when", "it", "sank", "on",
//                "Saturday", "in", "poor", "weather", ".", "The", "Aegean", "Sea", "a", "major", "route", "for",
//                "illegal", "immigrants", "trying", "to", "enter", "the", "European", "Union", ".", "Correspondents",
//                "say", "smugglers", "often", "them", "from", "the", "Turkish", "coast", "to", "nearby", "Greek",
//                "islands", "on", "fragile", ",", "overcrowded", "boats", ".", "Two", "of", "those", "rescued",
//                "reported", "to", "be", "Palestinians", "." };
//
//        XMLInputSource descriptor = new XMLInputSource(DESCRIPTOR_MST_PARSER);
//        ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
//
//        long t1 = System.currentTimeMillis();
//        LOGGER.info("creating analysis engine the first time ...");
//        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
//        LOGGER.info("... loading completed");
//        CAS cas = ae.newCAS();
//        FileInputStream fis = new FileInputStream(TEST_XMI);
//        XmiCasDeserializer.deserialize(fis, cas);
//        JCas jcas = cas.getJCas();
//        ae.process(jcas);
//
//        AnnotationIndex annotationIndex = jcas.getAnnotationIndex();
//        FSIterator iterator = jcas.getJFSIndexRepository().getAnnotationIndex(DependencyRelation.type).iterator();
//        int head = 0;
//        int text = 0;
//        while (iterator.hasNext()) {
//            DependencyRelation next = (DependencyRelation) iterator.next();
//            // assertEquals(coveredTexts[text], next.getCoveredText());
//            if (next.getHead() != null) {
//                System.out.println(heads[head].equals(next.getHead().getCoveredText()) + ": " + next.getCoveredText()
//                        + "/" + heads[head] + "/" + next.getHead().getCoveredText());
//                assertEquals(heads[head], next.getHead().getCoveredText());
//                head++;
//            }
//            text++;
//
//        }
//
//    }

    /*--------------------------------------------------------------------------------------------*/
    // /**
    // * Initialize the jcas.
    // *
    // * @param JCas
    // * jcas which is to be initialized
    // */
    // public void initCas(JCas jcas) {
    // jcas.reset();
    // } // of initCas

    public void testThreads() throws Exception {
        try {
            int count = 3;
            TestThread[] t = new TestThread[count];
            for (int i = 0; i < count; i++) {
                t[i] = new TestThread();
            }
            for (int i = 0; i < count; i++) {
                t[i].start();
            }
            TestThread x = new TestThread();
            x.run();
            Thread.sleep(5000);
        } catch (RuntimeException e) {
            fail("Errorin Threads");
        }
    }

    // public void testSharedResource() throws Exception {
    // LOGGER.info("testing loading of model as shared resource ...");
    // XMLInputSource descriptor = new XMLInputSource(DESCRIPTOR_MST_PARSER);
    // ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
    //
    // long t1 = System.currentTimeMillis();
    // LOGGER.info("creating analysis engine the first time ...");
    // AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
    // LOGGER.info("... loading completed");
    // CAS cas = ae.newCAS();
    // FileInputStream fis = new FileInputStream(TEST_XMI);
    // XmiCasDeserializer.deserialize(fis, cas);
    // JCas jcas = cas.getJCas();
    // ae.process(jcas);
    //
    // long t2 = System.currentTimeMillis();
    // AnalysisEngine ae2 = UIMAFramework.produceAnalysisEngine(specifier);
    // ae2.process(jcas);
    //
    // long t3 = System.currentTimeMillis();
    // AnalysisEngine ae3 = UIMAFramework.produceAnalysisEngine(specifier);
    // ae3.process(jcas);
    // long t4 = System.currentTimeMillis();
    //
    // assertTrue("actually the first AE creation should have taken longer due to model loading", t2 - t1 > t4 - t3);
    // }

    /*--------------------------------------------------------------------------------------------*/
    /**
     * This is the first method to be called if the test is executed.
     *
     * @throws IOException
     * @throws InvalidXMLException
     * @throws ResourceInitializationException
     * @throws CASException
     * @throws AnalysisEngineProcessException
     * @throws SAXException
     */
    public void testProcess() throws IOException, InvalidXMLException, ResourceInitializationException, CASException,
            AnalysisEngineProcessException, SAXException {
        XMLInputSource descriptor = new XMLInputSource(DESCRIPTOR_MST_PARSER);
        ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);

        CAS cas = ae.newCAS();
        FileInputStream fis = new FileInputStream(TEST_XMI);
        XmiCasDeserializer.deserialize(fis, cas);
        JCas jcas = cas.getJCas();
        ae.process(jcas);

        FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + File.separator + "test.xmi");
        XmiCasSerializer.serialize(jcas.getCas(), fos);

        assertTrue("Invalid JCas!", checkAnnotations(jcas, null));
    } // of testProcess

    public void testProcessWithNumTokensRestriction()
            throws IOException, InvalidXMLException, ResourceInitializationException, CASException,
            AnalysisEngineProcessException, SAXException, ResourceConfigurationException {
        XMLInputSource descriptor = new XMLInputSource(DESCRIPTOR_MST_PARSER);
        ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
        ae.setConfigParameterValue(PARAM_MAX_NUM_TOKENS, MAX_NUM_TOKENS);
        ae.reconfigure();
        CAS cas = ae.newCAS();
        FileInputStream fis = new FileInputStream(TEST_XMI);
        XmiCasDeserializer.deserialize(fis, cas);
        JCas jcas = cas.getJCas();
        ae.process(jcas);
        FileOutputStream fos = new FileOutputStream(OUTPUT_DIR + File.separator + "test.xmi");
        XmiCasSerializer.serialize(jcas.getCas(), fos);
        assertTrue("Invalid JCas!", checkAnnotations(jcas, MAX_NUM_TOKENS));
    }

    /**
     * First, checks if every token in the JCas has a non-empty depRel FSArray. Second, checks if in every sentence
     * there is only one token with a depRel.label 'null' or <no-type>. Third, checks if in every sentence there is only
     * one token with a depRel.head 'null'.
     *
     * @param jcas
     *            JCas which Tokens will be checked
     * @param maxNumTokens
     *            Value of configuration parameter 'MaxNumTokens' or 'null'
     * @return true if MSTParser annotations in jcas are complete and valid
     */
    private boolean checkAnnotations(JCas jcas, Integer maxNumTokens) {
        // the first sentence has the index 0!
        AnnotationIndex sentenceIndex = jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
        AnnotationIndex tokenIndex = jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
        FSIterator sentenceIterator = sentenceIndex.iterator();
        // iterate over sentences
        while (sentenceIterator.hasNext()) {
            Sentence sentence = (Sentence) sentenceIterator.next();
            FSIterator tokenIterator = tokenIndex.subiterator(sentence);
            int nullAndDummyLabelCounter = 0;
            int nullHeadCounter = 0;
            int tokenCounter = 0;
            boolean foundDepRelNull = false; // will be true if in a sentence at least one token.getDepRel() == null
            boolean foundDepRel = false; // will be true if in a sentence at least one token.getDepRel() != null
            // iterate over tokens
            while (tokenIterator.hasNext()) {
                Token token = (Token) tokenIterator.next();
                tokenCounter++;
                FSArray depRelationFSArray = token.getDepRel();
                if (depRelationFSArray == null) {
                    foundDepRelNull = true;
                } else {
                    foundDepRel = true;
                    // get first available DependencyRelation annotation of depRel array
                    DependencyRelation depRelation = (DependencyRelation) depRelationFSArray.get(0);
                    if (depRelation.getLabel() == null || depRelation.getLabel().equals("<no-type>")) {
                        nullAndDummyLabelCounter++;
                    }
                    if (depRelation.getHead() == null) {
                        nullHeadCounter++;
                    }
                }
            }
            // only consider cases where no maxNumTokens has been defined or where sentence has less tokens than
            // maxNumTokens
            if (maxNumTokens == null || tokenCounter <= maxNumTokens) {
                if (nullAndDummyLabelCounter != 1) {
                    LOGGER.error("There were " + nullAndDummyLabelCounter + " 'null' or '<no-type>' labels in sentence "
                            + sentence.getCoveredText());
                    return false;
                }
                if (nullHeadCounter != 1) {
                    LOGGER.error(
                            "There were " + nullHeadCounter + " head tokens in sentence " + sentence.getCoveredText());
                    return false;
                }
                if (foundDepRelNull) {
                    LOGGER.error("Found token with missing depRel feature");
                    return false;
                }
            } else if (foundDepRel) {
                LOGGER.error("Found token.depRel in sentence that MST Parser should have skipped since it has > "
                        + maxNumTokens + " tokens");
                return false;
            }
        }
        return true;
    }

} // of MSTParserTest
