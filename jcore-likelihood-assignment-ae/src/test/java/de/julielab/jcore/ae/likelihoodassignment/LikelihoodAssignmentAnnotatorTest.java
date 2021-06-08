
package de.julielab.jcore.ae.likelihoodassignment;

import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Unit tests for jcore-likelihood-assignment-ae.
 *
 */
public class LikelihoodAssignmentAnnotatorTest{
    private static final Logger LOGGER = LoggerFactory
            .getLogger(LikelihoodAssignmentAnnotatorTest.class);
    private static final String DESCRIPTOR = "de.julielab.jcore.ae.likelihoodassignment.desc.jcore-likelihood-assignment-ae";

    private static final String TEST_TEXT = "Mutational p53 oncoprotein may likely block apoptosis in adenocarcinoma.";
    private static final int[] likelihoodBegins = { 27, 31 };
    private static final int[] likelihoodEnds = { 30, 37 };
    private static final String[] likelihoodCats = { "moderate", "high" };
    private static final int[] conceptBegins = { 11, 38 };
    private static final int[] conceptEnds = { 14, 43 };
    private static final int[] sentenceBegins = { 0 };
    private static final int[] sentenceEnds = { 72 };
    private static final String TEST_ASSIGNMENT = "p53;may;moderate;block;may;moderate";

    public void initCas(JCas aJCas) {
        aJCas.reset();
        aJCas.setDocumentText(TEST_TEXT);

        for (int i = 0; i < likelihoodBegins.length; i++) {
            LikelihoodIndicator lh = new LikelihoodIndicator(aJCas);
            lh.setBegin(likelihoodBegins[i]);
            lh.setEnd(likelihoodEnds[i]);
            lh.setLikelihood(likelihoodCats[i]);
            lh.addToIndexes();
        }

        for (int i = 0; i < conceptBegins.length; i++) {
            ConceptMention con = new ConceptMention(aJCas);
            con.setBegin(conceptBegins[i]);
            con.setEnd(conceptEnds[i]);
            con.addToIndexes();
        }

        for (int i = 0; i < sentenceBegins.length; i++) {
            Sentence sent = new Sentence(aJCas);
            sent.setBegin(sentenceBegins[i]);
            sent.setEnd(sentenceEnds[i]);
            sent.addToIndexes();
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void testProcess() throws ResourceInitializationException, IOException, InvalidXMLException {

        XMLInputSource assignmentXML = null;
        ResourceSpecifier assignmentSpec = null;
        AnalysisEngine assignmentAnnotator = AnalysisEngineFactory.createEngine(DESCRIPTOR);

        JCas aJCas = null;
        try {
            aJCas = assignmentAnnotator.newJCas();
        } catch (ResourceInitializationException e) {
            LOGGER.error("testProcess()", e);
        }
        initCas(aJCas);
        try {
            assignmentAnnotator.process(aJCas);
        } catch (Exception e) {
            LOGGER.error("testProcess()", e);
        }

        JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        Iterator conceptIter = indexes.getAnnotationIndex(
                ConceptMention.type).iterator();
        String prediction = getPredictedAssignments(conceptIter);
        LOGGER.debug("testProcess() - Predicted Concept-Likelihood-Assignments: "
                + prediction);
        LOGGER.debug("testProcess() - Wanted Concept-Likelihood-Assignments: " + TEST_ASSIGNMENT);
        assertEquals(TEST_ASSIGNMENT, prediction);
    }

    @SuppressWarnings("rawtypes")
    public String getPredictedAssignments(Iterator conceptIter) {
        String conceptLikelihood = "";

        while (conceptIter.hasNext()) {
            ConceptMention concept = (ConceptMention) conceptIter.next();
            String conceptText = concept.getCoveredText();
            String likelihoodText = concept.getLikelihood().getCoveredText();
            String likelihoodCat = concept.getLikelihood().getLikelihood();
            LOGGER.debug("getPredictedAssignments() - ConceptMention: "
                    + conceptText + " - Likelihood Indicator: " + likelihoodText + " - Category: " + likelihoodCat);
            if (conceptIter.hasNext()) {
                conceptLikelihood = conceptLikelihood + conceptText + ";" + likelihoodText + ";" + likelihoodCat + ";";
            } else {
                conceptLikelihood = conceptLikelihood + conceptText + ";" + likelihoodText + ";" + likelihoodCat;
            }
        }

        return conceptLikelihood;
    }
}
