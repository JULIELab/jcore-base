package de.julielab.jcore.ae.mutationfinder;

import de.julielab.jcore.types.PointMutation;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
public class MutationAnnotatorTest {

    @Test
    public void testAnnotator() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine annotator = AnalysisEngineFactory.createEngine(MutationAnnotator.class);
        jCas.setDocumentText("A covalently bound catalytic intermediate in Escherichia coli asparaginase: crystal structure of a Thr-89-Val mutant.");
        annotator.process(jCas);
        final Collection<PointMutation> mutations = JCasUtil.select(jCas, PointMutation.class);
        assertThat(mutations).hasSize(1);
        assertThat(mutations.stream().findAny().get().getCoveredText()).isEqualTo("Thr-89-Val");
        assertThat(mutations.stream().findAny().get().getSpecificType()).isEqualTo("T89V");
    }
}
