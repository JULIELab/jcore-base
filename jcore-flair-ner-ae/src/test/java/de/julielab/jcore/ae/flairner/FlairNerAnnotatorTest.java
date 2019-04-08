
package de.julielab.jcore.ae.flairner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
/**
 * Unit tests for jcore-flair-ner-ae.
 *
 */
public class FlairNerAnnotatorTest{
    @Test
    public void testAnnotator() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        s.addToIndexes();
        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
        }
        assertThat(foundGenes).containsExactly("SUB1 homolog", "HIV-1", "VSV-G", "HIV-1");
    }

    private void addTokens(JCas jCas) {
        final String documentText = jCas.getDocumentText();
        Matcher m = Pattern.compile("[^ ]+").matcher(documentText);
        while (m.find()) {
            new Token(jCas, m.start(), m.end()).addToIndexes();
        }
    }
}
