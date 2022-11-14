package de.julielab.jcore.ae.annotationremoval;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for jcore-annotation-removal-ae.
 */
public class AnnotationRemovalAnnotatorTest {
    private final static Logger log = LoggerFactory.getLogger(AnnotationRemovalAnnotatorTest.class);

    @Test
    public void testAnnotator() throws Exception {
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.annotationremoval.desc.jcore-annotation-removal-ae",
                AnnotationRemovalAnnotator.PARAM_ANNOTATION_TYPES, new String[]{"de.julielab.jcore.types.Token", "de.julielab.jcore.types.Gene"});
        final JCas jCas = engine.newJCas();
        jCas.setDocumentText("There is a gene in this sentence.");
        addTokens(jCas);
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        new Gene(jCas, 11, 15).addToIndexes();

        // Check that the annotations we just created are actually there.
        assertFalse(JCasUtil.select(jCas, Sentence.class).isEmpty());
        assertFalse(JCasUtil.select(jCas, Token.class).isEmpty());
        assertFalse(JCasUtil.select(jCas, Gene.class).isEmpty());

        engine.process(jCas);

        // And now check that the annotation that should be removed are really gone.
        assertFalse(JCasUtil.select(jCas, Sentence.class).isEmpty());
        assertTrue(JCasUtil.select(jCas, Token.class).isEmpty());
        assertTrue(JCasUtil.select(jCas, Gene.class).isEmpty());
    }

    private void addTokens(JCas jCas) {
        Matcher alphanumericalTokens = Pattern.compile("[A-Za-z0-9]+").matcher(jCas.getDocumentText());
        while (alphanumericalTokens.find()) {
            new Token(jCas, alphanumericalTokens.start(), alphanumericalTokens.end()).addToIndexes();
        }
        Matcher punctuation = Pattern.compile("\\p{Punct}").matcher(jCas.getDocumentText());
        while (alphanumericalTokens.find()) {
            new Token(jCas, punctuation.start(), punctuation.end()).addToIndexes();
        }
    }
}
