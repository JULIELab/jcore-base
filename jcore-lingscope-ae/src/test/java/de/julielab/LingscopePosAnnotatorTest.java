package de.julielab;

import de.julielab.jcore.ae.lingscope.LingscopePosAnnotator;
import de.julielab.jcore.types.*;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class LingscopePosAnnotatorTest {
    // NOTE: To write tests here, http://nactem7.mib.man.ac.uk/geniatagger/ can be extremely helpful (automatic POS tagging)
    @Test
    public void testWithoutLikelihoodDict() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "src/test/resources/negation_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "src/test/resources/negation_models/crf_scope_words_crf_all_both.model");
        jCas.setDocumentText("The patient denied leg pain but complained about a headache.");
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        addToken(jCas, 0, 3, "DT", "The");
        addToken(jCas, 4, 11, "NN", "patient");
        addToken(jCas, 12, 18, "VBD", "deny");
        addToken(jCas, 19, 22, "NN", "leg");
        addToken(jCas, 23, 27, "NN", "pain");
        addToken(jCas, 28, 31, "CC", "but");
        addToken(jCas, 32, 42, "VBD", "complain");
        addToken(jCas, 43, 48, "IN", "about");
        addToken(jCas, 49, 50, "DT", "a");
        addToken(jCas, 51, 59, "NN", "headache");
        addToken(jCas, 59, 60, ".", ".");

        engine.process(jCas);

        final LikelihoodIndicator indicator = JCasUtil.selectSingle(jCas, LikelihoodIndicator.class);
        assertThat(indicator.getBegin()).isEqualTo(12);
        assertThat(indicator.getEnd()).isEqualTo(18);
        assertThat(indicator.getLikelihood()).isNull();

        final Scope scope = JCasUtil.selectSingle(jCas, Scope.class);
        assertThat(scope.getCue() == indicator);
        assertThat(scope.getBegin()).isEqualTo(12);
        // That the scope is this long is actually an error of the CRF
        assertThat(scope.getEnd()).isEqualTo(59);

    }

    @Test
    public void testWithLikelihoodDict() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "src/test/resources/hedge_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "src/test/resources/hedge_models/crf_scope_words_crf_all_both.model", LingscopePosAnnotator.PARAM_LIKELIHOOD_DICT_PATH, "src/main/resources/de/julielab/jcore/ae/lingscope/resources/likelihood_neg_invest_dict");
        jCas.setDocumentText("It might be true.");
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        addToken(jCas, 0, 2, "PRP", "It");
        addToken(jCas, 3, 8, "MD", "may");
        addToken(jCas, 9, 11, "VB", "be");
        addToken(jCas, 12, 16, "JJ", "true");
        addToken(jCas, 16, 17, ".", ".");

        engine.process(jCas);

        final LikelihoodIndicator indicator = JCasUtil.selectSingle(jCas, LikelihoodIndicator.class);
        assertThat(indicator.getBegin()).isEqualTo(3);
        assertThat(indicator.getEnd()).isEqualTo(8);
        assertThat(indicator.getLikelihood()).isEqualTo("moderate");

        final Scope scope = JCasUtil.selectSingle(jCas, Scope.class);
        assertThat(scope.getCue() == indicator);
        assertThat(scope.getBegin()).isEqualTo(3);
        // That the scope is this long is actually an error of the CRF
        assertThat(scope.getEnd()).isEqualTo(16);

    }

    private void addToken(JCas jCas, int begin, int end, String posTag, String lemma) {
        final Token token = new Token(jCas, begin, end);
        final PennBioIEPOSTag p1 = new PennBioIEPOSTag(jCas, begin, end);
        p1.setValue(posTag);
        token.setPosTag(JCoReTools.addToFSArray(null, p1));
        final Lemma lemmaAnnotation = new Lemma(jCas, begin, end);
        lemmaAnnotation.setValue(lemma);
        token.setLemma(lemmaAnnotation);
        token.addToIndexes();
    }
}
