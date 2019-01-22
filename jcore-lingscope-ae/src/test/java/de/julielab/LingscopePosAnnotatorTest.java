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

import java.util.stream.Collectors;

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

    @Test
    public void testResourcesOnClasspath() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "hedge_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "hedge_models/crf_scope_words_crf_all_both.model", LingscopePosAnnotator.PARAM_LIKELIHOOD_DICT_PATH, "de/julielab/jcore/ae/lingscope/resources/likelihood_neg_invest_dict");
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

    @Test
    public void testNegationOnly() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "hedge_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "hedge_models/crf_scope_words_crf_all_both.model", LingscopePosAnnotator.PARAM_LIKELIHOOD_DICT_PATH, "src/main/resources/de/julielab/jcore/ae/lingscope/resources/likelihood_neg_invest_dict", LingscopePosAnnotator.PARAM_IS_NEGATION_ANNOTATOR, true);
        jCas.setDocumentText("It might be true.");
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        addToken(jCas, 0, 2, "PRP", "It");
        addToken(jCas, 3, 8, "MD", "may");
        addToken(jCas, 9, 11, "VB", "be");
        addToken(jCas, 12, 16, "JJ", "true");
        addToken(jCas, 16, 17, ".", ".");

        engine.process(jCas);

        final LikelihoodIndicator indicator = JCasUtil.selectSingle(jCas, LikelihoodIndicator.class);
        // This is actually a hedge, but we set the annotator to be in negation mode, so it should mark everyting as a negation (not something one would want to do in production, unless really certain that it makes sense)
        assertThat(indicator.getLikelihood()).isEqualTo("negation");

    }

    @Test
    public void testHardCase() throws Exception {
        // The sentence contains pipes which caused errors before it was handeled in the Annotator.
        String sentence = "The CO2 isosteric heat of adsorption (|Q(st)|) and CO2/N2 selectivity (S), obtained from pure gas adsorption isotherms and Ideal Adsorbed Solution Theory (IAST) calculations, are also maximised relative to the neutral framework at this concentration of the alkali metal counter-ion.";
        String tokens = "The CO2 isosteric heat of adsorption ( |Q ( st ) | ) and CO2 / N2 selectivity ( S ) , obtained from pure gas adsorption isotherms and Ideal Adsorbed Solution Theory ( IAST ) calculations , are also maximised relative to the neutral framework at this concentration of the alkali metal counter - ion .";
        String lemmas = "the CO2 isosteric heat of adsorption ( |Q ( st ) | ) and CO2 / N2 selectivity ( s ) , obtain from pure gas adsorption isotherm and ideal adsorbed solution theory ( IAST ) calculation , be also maximize relative to the neutral framework at this concentration of the alkali metal counter - ion .";
        String posTags = "DT NN JJ NN IN NN -LRB- NN -LRB- NN -RRB- NN -RRB- CC NN SYM NN NN -LRB- NN -RRB- , VBN IN JJ NN NN NNS CC JJ JJ NN NNS -LRB- NN -RRB- NNS , VBP RB VBN JJ TO DT JJ NN IN DT NN IN DT NNS JJ NN HYPH NN .";
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        jCas.setDocumentText(sentence);
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();

        fillCasFromStrings(jCas, sentence, tokens, lemmas, posTags);
        assertThat(JCasUtil.select(jCas, Token.class).stream().map(Token::getCoveredText).collect(Collectors.joining(" "))).isEqualTo(tokens);

        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "hedge_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "hedge_models/crf_scope_words_crf_all_both.model", LingscopePosAnnotator.PARAM_LIKELIHOOD_DICT_PATH, "src/main/resources/de/julielab/jcore/ae/lingscope/resources/likelihood_neg_invest_dict", LingscopePosAnnotator.PARAM_IS_NEGATION_ANNOTATOR, true);
        assertThatCode(() -> engine.process(jCas)).doesNotThrowAnyException();
    }

    @Test
    public void testHardCase2() throws Exception {
        // This test fails when we tell the Lingscope annotator that our input would not be tokenized. This quickly causes issues when the external tokenization differs from the one that Lingscope produces.
        String sentence = "Adolescents were divided into 4 groups according to suicide risk severity (grade 1 = depressed without suicidal ideation and without suicide attempts, grade 2 = depressed with suicidal ideations and grade 3 = depressed with suicide attempts; grade 0 = control group).";
        String tokens = "Adolescents were divided into 4 groups according to suicide risk severity ( grade 1  =  depressed without suicidal ideation and without suicide attempts , grade 2  =  depressed with suicidal ideations and grade 3  =  depressed with suicide attempts ; grade 0  =  control group ) .";
        String lemmas = "adolescent be divide into 4 group accord to suicide risk severity ( grade 1  =  depressed without suicidal ideation and without suicide attempt , grade 2  =  depress with suicidal ideation and grade 3  =  depress with suicide attempt ; grade 0  =  control group ) .";
        String posTags = "NNS VBD VBN IN CD NNS VBG TO NN NN NN -LRB- NN NN SYM CC IN JJ NN CC IN NN NNS , NN NN SYM VBN IN JJ NNS CC NN NN SYM VBN IN NN NNS : NN NN SYM NN NN -RRB- .";

        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-mention-types");
        jCas.setDocumentText(sentence);
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();

        fillCasFromStrings(jCas, sentence, tokens, lemmas, posTags);
        assertThat(JCasUtil.select(jCas, Token.class).stream().map(Token::getCoveredText).collect(Collectors.joining(" "))).isEqualTo(tokens);

        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "hedge_models/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "hedge_models/crf_scope_words_crf_all_both.model", LingscopePosAnnotator.PARAM_LIKELIHOOD_DICT_PATH, "src/main/resources/de/julielab/jcore/ae/lingscope/resources/likelihood_neg_invest_dict", LingscopePosAnnotator.PARAM_IS_NEGATION_ANNOTATOR, true);
        assertThatCode(() -> engine.process(jCas)).doesNotThrowAnyException();
    }

    private void fillCasFromStrings(JCas jCas, String sentence, String tokens, String lemmas, String posTags) {
        final String[] tokSplit = tokens.split("\\s");
        final String[] lemSplit = lemmas.split("\\s");
        final String[] posSplit = posTags.split("\\s");
        int pos = 0;
        for (int i = 0; i < lemSplit.length; i++) {
            String token = tokSplit[i];
            String lemma = lemSplit[i];
            String posTag = posSplit[i];
            final int begin = sentence.indexOf(token, pos);
            final int end = begin + token.length();
            pos = end;
            addToken(jCas, begin, end, posTag, lemma);
        }
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
