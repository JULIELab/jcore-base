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
package de.julielab.jcore.ae.opennlpparser.main;

import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Constituent;
import de.julielab.jcore.types.GENIAConstituent;
import de.julielab.jcore.types.PTBConstituent;
import de.julielab.jcore.types.PennBioIEConstituent;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import junit.framework.TestCase;

public class ParseAnnotatorTest extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseAnnotatorTest.class);

    private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // set log4j properties file
        // PropertyConfigurator.configure(LOGGER_PROPERTIES);
    }

    String text = "A study on the Prethcamide hydroxylation system in rat hepatic microsomes .";

    String wantedCons = "NP NP PP NP NP PP NP ";

    public void initCas(JCas jcas) {

        jcas.reset();
        jcas.setDocumentText(text);

        Sentence s = new Sentence(jcas);
        s.setBegin(0);
        s.setEnd(text.length());
        s.addToIndexes(jcas);

        String[] tokens = text.split(" ");
        int j = 0;
        for (int i = 0; i < tokens.length; i++) {
            Token token = new Token(jcas);
            token.setBegin(j);
            token.setEnd(j + tokens[i].length());
            j = j + tokens[i].length() + 1;
            token.addToIndexes(jcas);
        }
    }

    public void testProcess() {

        boolean annotationsOK = true;

        XMLInputSource parserXML = null;
        ResourceSpecifier parserSpec = null;
        AnalysisEngine parserAnnotator = null;

        try {
            parserXML = new XMLInputSource(
                    "src/test/resources/de/julielab/jcore/ae/opennlpparser/desc/jcore-opennlpparser-test.xml");
            parserSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(parserXML);
            parserAnnotator = UIMAFramework.produceAnalysisEngine(parserSpec);
        } catch (Exception e) {
            LOGGER.error("[testProcess]" + e.getMessage());
            e.printStackTrace();
        }

        JCas jcas = null;
        try {
            jcas = parserAnnotator.newJCas();
        } catch (ResourceInitializationException e) {
            LOGGER.error("[testProcess]" + e.getMessage());
            e.printStackTrace();
        }

        // get test cas with sentence/token/pos annotation
        initCas(jcas);

        try {
            parserAnnotator.process(jcas, null);
        } catch (Exception e) {
            LOGGER.error("[testProcess]" + e.getMessage());
            e.printStackTrace();
        }

        // get the offsets of the sentences
        JFSIndexRepository indexes = jcas.getJFSIndexRepository();
        Iterator consIter = indexes.getAnnotationIndex(Constituent.type).iterator();

        StringBuffer predictedCons = new StringBuffer();
        while (consIter.hasNext()) {
            String label = null;
            Constituent cons = (Constituent) consIter.next();
            Class c = cons.getClass();
            if (c.equals(GENIAConstituent.class)) {
                label = ((GENIAConstituent) cons).getCat();
            }
            if (c.equals(PennBioIEConstituent.class)) {
                label = ((PennBioIEConstituent) cons).getCat();
            }
            if (c.equals(PTBConstituent.class)) {
                label = ((PTBConstituent) cons).getCat();
            }

            predictedCons = predictedCons.append(label + " ");

        }

        if (wantedCons.equals(predictedCons.toString())) {
            assertTrue(annotationsOK);
        }
        LOGGER.debug("[testProcess: ]wantedCons: " + wantedCons + "\n" + "predictedCons: " + predictedCons);

    }

}
