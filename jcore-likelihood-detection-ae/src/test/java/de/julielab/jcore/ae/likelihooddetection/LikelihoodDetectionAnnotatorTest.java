package de.julielab.jcore.ae.likelihooddetection;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Unit tests for jcore-likelihood-detection-ae.
 */
public class LikelihoodDetectionAnnotatorTest {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(LikelihoodDetectionAnnotatorTest.class);
    private static final String DESCRIPTOR = "de.julielab.jcore.ae.likelihooddetection.desc.jcore-likelihood-detection-ae";

    private static final String TEST_TEXT = "PML appears to be transcriptionally regulated by class I "
            + "and II interferons, which raises the possibility that interferons modulate the function "
            + "and growth and differentiation potential of normal myeloid cells and precursors.";
    private static final int[] tokenBegins = {0, 4, 12, 15, 18, 36, 46, 49,
            55, 57, 61, 64, 75, 77, 83, 90, 94, 106, 111, 123, 132, 136, 145,
            149, 156, 160, 176, 186, 189, 196, 204, 210, 214, 224};
    private static final int[] tokenEnds = {3, 11, 14, 17, 35, 45, 48, 54, 56,
            60, 63, 75, 76, 82, 89, 93, 105, 110, 122, 131, 135, 144, 148, 155,
            159, 175, 185, 188, 195, 203, 209, 213, 224, 225};
    private static final String[] lemmas = {"pml", "appear", "to", "be",
            "transcriptionally", "regulate", "by", "class", "i", "and", "ii",
            "interferon", ",", "which", "raise", "the", "possibility", "that",
            "interferon", "modulate", "the", "function", "and", "growth",
            "and", "differentiation", "potential", "of", "normal", "myeloid",
            "cell", "and", "precursor", "."};
    private static final String TEST_INDICATORS = "appears;raises the possibility";
    private static final String TEST_CATEGORIES = "moderate;moderate";

    public void initCas(JCas aJCas) {
        aJCas.reset();
        aJCas.setDocumentText(TEST_TEXT);

        for (int i = 0; i < tokenBegins.length; i++) {
            Token tok = new Token(aJCas);
            tok.setBegin(tokenBegins[i]);
            tok.setEnd(tokenEnds[i]);
            Lemma lemma = new Lemma(aJCas);
            lemma.setBegin(tokenBegins[i]);
            lemma.setEnd(tokenEnds[i]);
            lemma.setValue(lemmas[i]);
            tok.setLemma(lemma);
            tok.addToIndexes();
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testProcess() throws ResourceInitializationException, IOException, InvalidXMLException {
        AnalysisEngine likelihoodAnnotator = AnalysisEngineFactory.createEngine(DESCRIPTOR);
        JCas aJCas = null;
        try {
            aJCas = likelihoodAnnotator.newJCas();
        } catch (ResourceInitializationException e) {
            LOGGER.error("testProcess()", e);
        }
        initCas(aJCas);
        try {
            likelihoodAnnotator.process(aJCas);
        } catch (Exception e) {
            LOGGER.error("testProcess()", e);
        }

        JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        Iterator likelihoodIter = indexes.getAnnotationIndex(
                LikelihoodIndicator.type).iterator();
        ArrayList<String> prediction = getPredictedIndicators(likelihoodIter);
        String predictedIndicators = prediction.get(0);
        String predictedCategories = prediction.get(1);
        LOGGER.debug("testProcess() - Predicted Indicators: "
                + predictedIndicators);
        LOGGER.debug("testProcess() - Wanted Indicators: " + TEST_INDICATORS);
        assertEquals(TEST_INDICATORS, predictedIndicators);
        LOGGER.debug("testProcess() - Predicted Categories: "
                + predictedCategories);
        LOGGER.debug("testProcess() - Wanted Indicators: " + TEST_CATEGORIES);
        assertEquals(TEST_CATEGORIES, predictedCategories);
    }

    @SuppressWarnings("rawtypes")
    private ArrayList<String> getPredictedIndicators(Iterator likelihoodIter) {
        ArrayList<String> prediction = new ArrayList<String>();
        String predictedIndicators = "";
        String predictedCategories = "";

        while (likelihoodIter.hasNext()) {
            LikelihoodIndicator indicator = (LikelihoodIndicator) likelihoodIter
                    .next();
            String indicatorText = indicator.getCoveredText();
            String category = indicator.getLikelihood();
            LOGGER.debug("getPredictedIndicators() - Indicator: "
                    + indicatorText + " - Category: " + category);
            if (likelihoodIter.hasNext()) {
                predictedIndicators = predictedIndicators + indicatorText + ";";
                predictedCategories = predictedCategories + category + ";";
            } else {
                predictedIndicators = predictedIndicators + indicatorText;
                predictedCategories = predictedCategories + category;
            }
        }

        prediction.add(predictedIndicators);
        prediction.add(predictedCategories);
        return prediction;
    }

    @Test
    public void test() throws Exception {
        String text = "Genome-wide expression analyses indicate that TAZ/YAP, TEADs, and TGFβ-induced signals coordinate a specific pro-tumorigenic transcriptional program";
        AnalysisEngine likelihoodAnnotator = AnalysisEngineFactory.createEngine(DESCRIPTOR);
        JCas aJCas = null;
        try {
            aJCas = likelihoodAnnotator.newJCas();
        } catch (ResourceInitializationException e) {
            LOGGER.error("testProcess()", e);
        }
        likelihoodAnnotator.process(aJCas);

        final Collection<LikelihoodIndicator> select = JCasUtil.select(aJCas, LikelihoodIndicator.class);
        for (var s : select) {
            System.out.println(s.getCoveredText());
        }
    }
}
