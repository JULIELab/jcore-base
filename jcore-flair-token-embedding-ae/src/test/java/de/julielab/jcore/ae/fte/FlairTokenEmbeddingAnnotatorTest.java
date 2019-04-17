package de.julielab.jcore.ae.fte;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-flair-token-embedding-ae.
 */
public class FlairTokenEmbeddingAnnotatorTest {
    @Test
    public void testEmbeddingAnnotator() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
        String sentence1 = "Dysregulated inflammation leads to morbidity and mortality in neonates.";
        String sentence2 = "97 healthy subjects were enrolled in the present study.";
        jCas.setDocumentText(sentence1 + " " + sentence2);
        new Sentence(jCas, 0, sentence1.length()).addToIndexes();
        new Sentence(jCas, sentence1.length() + 1, sentence1.length() + 1 + sentence2.length()).addToIndexes();
        addTokens(jCas);

        final String embeddingPath = "flair:src/test/resources/gene_small_best_lm.pt";
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.fte.desc.jcore-flair-token-embedding-ae", FlairTokenEmbeddingAnnotator.PARAM_EMBEDDING_PATH, embeddingPath);

        engine.process(jCas);

        final Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
        assertThat(tokens).hasSize(20);
        for (Token t : tokens) {
            assertThat(t.getEmbeddingVectors()).isNotNull().hasSize(1);
            assertThat(t.getEmbeddingVectors(0).getVector()).hasSize(1024);
            assertThat(t.getEmbeddingVectors(0).getSource()).isEqualTo(embeddingPath);
        }
        engine.collectionProcessComplete();
    }

    @Test
    public void testEmbeddingAnnotatorWithFilterAnnotation() throws Exception {
        // Here we test the setting of only individual tokens
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
        String sentence1 = "Dysregulated inflammation leads to morbidity and mortality in neonates.";
        String sentence2 = "97 healthy subjects were enrolled in the present study.";
        jCas.setDocumentText(sentence1 + " " + sentence2);
        new Sentence(jCas, 0, sentence1.length()).addToIndexes();
        new Sentence(jCas, sentence1.length() + 1, sentence1.length() + 1 + sentence2.length()).addToIndexes();
        addTokens(jCas);
        new Gene(jCas, 13, 25).addToIndexes();
        // This annotation spans two tokens
        new Gene(jCas, 75, 91).addToIndexes();

        final String embeddingPath = "flair:src/test/resources/gene_small_best_lm.pt";
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.fte.desc.jcore-flair-token-embedding-ae", FlairTokenEmbeddingAnnotator.PARAM_EMBEDDING_PATH, embeddingPath, FlairTokenEmbeddingAnnotator.PARAM_COMPUTATION_FILTER, "de.julielab.jcore.types.Gene");

        engine.process(jCas);

        final Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
        assertThat(tokens).hasSize(20);
        for (Token t : tokens) {
            if (t.getBegin() == 13 || t.getBegin() == 75 || t.getBegin() == 83) {
                assertThat(t.getEmbeddingVectors()).isNotNull().hasSize(1);
                assertThat(t.getEmbeddingVectors(0).getVector()).hasSize(1024);
                assertThat(t.getEmbeddingVectors(0).getSource()).isEqualTo(embeddingPath);
            } else {
                assertThat(t.getEmbeddingVectors()).isNull();
            }
        }
        engine.collectionProcessComplete();
    }

    private void addTokens(JCas jCas) {
        // Tokens for sentence 1
        new Token(jCas, 0, 12).addToIndexes();
        new Token(jCas, 13, 25).addToIndexes();
        new Token(jCas, 26, 31).addToIndexes();
        new Token(jCas, 32, 34).addToIndexes();
        new Token(jCas, 35, 44).addToIndexes();
        new Token(jCas, 45, 48).addToIndexes();
        new Token(jCas, 49, 58).addToIndexes();
        new Token(jCas, 59, 61).addToIndexes();
        new Token(jCas, 62, 70).addToIndexes();
        new Token(jCas, 70, 71).addToIndexes();
        // Tokens for sentence 2
        new Token(jCas, 72, 74).addToIndexes();
        new Token(jCas, 75, 82).addToIndexes();
        new Token(jCas, 83, 91).addToIndexes();
        new Token(jCas, 92, 96).addToIndexes();
        new Token(jCas, 97, 105).addToIndexes();
        new Token(jCas, 106, 108).addToIndexes();
        new Token(jCas, 109, 112).addToIndexes();
        new Token(jCas, 113, 120).addToIndexes();
        new Token(jCas, 121, 126).addToIndexes();
        new Token(jCas, 126, 127).addToIndexes();
    }
}
