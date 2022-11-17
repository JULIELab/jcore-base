package de.julielab.jcore.multiplier.line;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * Unit tests for jcore-line-multiplier.
 */
public class LineMultiplierTest {
    @Test
    public void process() throws Exception {
        JCas jCas = JCasFactory.createJCas();
        String ls = System.getProperty("line.separator");
        jCas.setDocumentText("line1" + ls + "line2" + ls + ls + ls + "line3");
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(LineMultiplier.class);
        JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
        assertTrue(jCasIterator.hasNext());
        List<String> casTexts = new ArrayList<>();
        while (jCasIterator.hasNext()) {
            JCas newCas = jCasIterator.next();
            casTexts.add(newCas.getDocumentText());
            newCas.release();
        }
        assertThat(casTexts).hasSize(3);
        assertThat(casTexts).containsExactly("line1", "line2", "line3");
    }
}
