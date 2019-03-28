/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.jpos.postagger;

/**
 * POSAnnotator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: hellrich
 *
 * Current version: 0.0.1
 *
 * Creation date: Sep 11, 2014
 *
 * This is an UIMA wrapper for the JULIE POSTagger.
 * Based on Katrin Tomanek's JNET
 **/

import de.julielab.jcore.ae.jpos.tagger.POSTagger;
import de.julielab.jcore.ae.jpos.tagger.Unit;
import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class POSAnnotator extends JCasAnnotator_ImplBase {

    private static final String COMPONENT_ID = "de.julielab.jcore.ae.jpos.postagger.POSAnnotator";

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(POSAnnotator.class);

    private POSTagger tagger;

    Properties featureConfig = null;
    // ArrayList<String> activatedMetas = null;
    // ArrayList<FSIterator<org.apache.uima.jcas.tcas.Annotation>>
    // annotationIterators = null;
    // ArrayList<String> valueMethods = null;

    private String postagset;

    /**
     * Initialisiation of UIMA-JNET. Reads in and checks descriptor's parameters.
     *
     * @throws ResourceInitializationException
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        LOGGER.info("initialize() - initializing JPOS...");

        // invoke default initialization
        super.initialize(aContext);

        // get modelfilename from parameters
        try {

            // compulsory params
            setModel(aContext);
            postagset = (String) aContext.getConfigParameterValue("tagset");

        } catch (final AnnotatorContextException e) {
            e.printStackTrace();
            throw new ResourceInitializationException();
        } catch (final AnnotatorConfigurationException e) {
            e.printStackTrace();
            throw new ResourceInitializationException();
        } catch (final AnnotatorInitializationException e) {
            e.printStackTrace();
            throw new ResourceInitializationException();
        }
    }

    /**
     * set and load the JPOS model
     */
    private void setModel(final UimaContext aContext)
            throws AnnotatorConfigurationException, AnnotatorContextException, AnnotatorInitializationException {

        // get model filename
        String modelFilename = "";
        final Object o = aContext.getConfigParameterValue("ModelFilename");
        if (o != null) {
            modelFilename = (String) o;
        } else {
            LOGGER.error("setModel() - descriptor incomplete, no model file specified!");
            throw new AnnotatorConfigurationException();
        }

        // produce an instance of JPOS with this model
        try {
            LOGGER.debug("setModel() -  loading JPOS model...");
            final File modelFile = new File(modelFilename);

            InputStream is;
            if (!modelFile.exists()) {
                // perhaps the parameter value does not point to a file but to a classpath resource
                LOGGER.debug("no such model file, trying to load model from classpath resource...");
                String resourceLocation = modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename;
                is = getClass().getResourceAsStream(resourceLocation);
            } else {
                is = new FileInputStream(modelFile);
            }

            tagger = POSTagger.readModel(is);
        } catch (final Exception e) {
            LOGGER.error("setModel() - Could not load JPOS model from " + new File(modelFilename).getAbsolutePath()
                    + ": " + e.getMessage(), e);
            throw new AnnotatorInitializationException();
        }
    }

    /**
     * process current CAS. In case, abbreviation expansion is turned on, the abbreviation is replaced by its full form
     * which is used during prediction. The labels of this full form are then applied to the original, short form.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {

        LOGGER.info("process() - processing next document");

        final JFSIndexRepository indexes = aJCas.getJFSIndexRepository();

        // get all sentences and tokens
        final Iterator<org.apache.uima.jcas.tcas.Annotation> sentenceIter = indexes.getAnnotationIndex(Sentence.type)
                .iterator();

        // do entity recognition over single sentences
        while (sentenceIter.hasNext()) {
            final Sentence sentence = (Sentence) sentenceIter.next();

            // get tokens for this sentence
            @SuppressWarnings("unchecked")
            final ArrayList<Token> tokenList = (ArrayList<Token>) UIMAUtils.getAnnotations(aJCas, sentence,
                    new Token(aJCas, 0, 0).getClass());

            // make the Sentence object
            final de.julielab.jcore.ae.jpos.tagger.Sentence unitSentence = createUnitSentence(tokenList, aJCas);

            LOGGER.debug("process() - original sentence: " + sentence.getCoveredText());
            final StringBuffer unitS = new StringBuffer();
            for (final Unit unit : unitSentence.getUnits()) {
                unitS.append(unit.getRep() + " ");
            }
            LOGGER.debug("process() - sentence for prediction: " + unitSentence.toString());

            // predict with JPOS
            try {
                tagger.predictForUIMA(unitSentence);
            } catch (final IllegalStateException e) {
                LOGGER.error("process() - predicting with JPOS failed: " + e.getMessage());
                throw new AnalysisEngineProcessException();
            }

            LOGGER.debug("process() - sentence with labels: " + unitSentence.toString());

            // write predicted labels to CAS
            writeToCAS(unitSentence, aJCas, tokenList);

        }

    }

    /**
     * Takes all info about meta data and generates the corresponding unit sequence represented by a Sentence object.
     * Abbreviation is expanded when specified in descriptor. Only abbreviations which span over single tokens can be
     * interpreted here. Other case (which is very rare and thus probably not relevant) is ignored!
     *
     * @param tokenList
     *            a list of Token objects of the current sentence
     * @param JCas
     *            the CAS we are working on
     * @param metaList
     *            a Arraylist of meta-info HashMaps which specify the meta information of the respective token
     * @return an array of two sequences of units containing all available meta data for the corresponding tokens. In
     *         the first sequence, abbreviations are expanded to their fullform. In the second sequence, the tokens are
     *         of their original form.
     */
    protected de.julielab.jcore.ae.jpos.tagger.Sentence createUnitSentence(final ArrayList<Token> tokenList, final JCas JCas) {

        final de.julielab.jcore.ae.jpos.tagger.Sentence unitSentence = new de.julielab.jcore.ae.jpos.tagger.Sentence();

        for (int i = 0; i < tokenList.size(); i++) {
            final Token token = tokenList.get(i);
            final String tokenRepresentation = token.getCoveredText();

            // now make JPOS Unit object for this token and add to Sentence
            if (tokenRepresentation != null) {
                if (tokenRepresentation.equals(token.getCoveredText())) {
                    // no abbrevs were expanded here
                    final Unit unit = new Unit(token.getBegin(), token.getEnd(), tokenRepresentation);
                    unitSentence.add(unit);
                }
            }
        }
        return unitSentence;
    }

    /**
     * creates the respective uima annotations from JPOS's predictions. Therefore, we loop over JPOS's Sentence objects
     * which contain predictions/labels for each Unit (i.e., for each token).
     *
     * @param unitSentence
     *            the current Sentence object
     * @param aJCas
     *            the cas to write the annotation to
     * @param tokenList
     * @throws AnalysisEngineProcessException
     */
    public void writeToCAS(final de.julielab.jcore.ae.jpos.tagger.Sentence unitSentence, final JCas aJCas,
            final ArrayList<Token> tokenList) throws AnalysisEngineProcessException {

        if (tokenList.size() != unitSentence.size()) {
            LOGGER.error("process() - writing results to CAS failed: " + tokenList
                    + "\n is incompatible in length with\n" + tokenList);
            throw new AnalysisEngineProcessException();
        }

        for (int i = 0; i < unitSentence.size(); i++) {
            try {
                final Class<?>[] parameterTypes = new Class[] { JCas.class };
                final Class<?> myNewClass = Class.forName(postagset);
                final Constructor<?> myConstructor = myNewClass.getConstructor(parameterTypes);
                final POSTag pos = (POSTag) myConstructor.newInstance(aJCas);
                pos.setBegin(unitSentence.get(i).begin);
                pos.setEnd(unitSentence.get(i).end);
                pos.setValue(unitSentence.get(i).getLabel());
                pos.setComponentId(COMPONENT_ID);
                pos.addToIndexes();

                if (tokenList.get(i).getPosTag() == null) {
                    tokenList.get(i).setPosTag(new FSArray(aJCas, 1));
                }
                tokenList.get(i).setPosTag(JCoReTools.addToFSArray(tokenList.get(i).getPosTag(), pos));
            } catch (final Exception e) {
                LOGGER.error("error storing results in CAS:\n" + e);
                e.printStackTrace();
            }
        }
    }
}
