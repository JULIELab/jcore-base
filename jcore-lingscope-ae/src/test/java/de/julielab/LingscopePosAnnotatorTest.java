package de.julielab;

import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;


public class LingscopePosAnnotatorTest {
    @Test
    public void testIfItRuns() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(LingscopePosAnnotator.class, LingscopePosAnnotator.PARAM_CUE_MODEL, "src/test/resources/baseline_cue_all_both.model", LingscopePosAnnotator.PARAM_SCOPE_MODEL, "src/test/resources/crf_scope_words_crf_all_both.model");
        jCas.setDocumentText("The patient denied leg pain but complained about a headache.");
        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        final PennBioIEPOSTag p1 = new PennBioIEPOSTag(jCas, 0, 3);
        p1.setValue("DT");
        p1.addToIndexes();
        final PennBioIEPOSTag p2 = new PennBioIEPOSTag(jCas, 4, 11);
        p2.setValue("NN");
        p2.addToIndexes();
        final PennBioIEPOSTag p3 = new PennBioIEPOSTag(jCas, 12, 18);
        p3.setValue("VBD");
        p3.addToIndexes();
        final PennBioIEPOSTag p4 = new PennBioIEPOSTag(jCas, 19, 22);
        p4.setValue("NN");
        p4.addToIndexes();
        final PennBioIEPOSTag p5 = new PennBioIEPOSTag(jCas, 23, 27);
        p5.setValue("NN");
        p5.addToIndexes();
        final PennBioIEPOSTag p6 = new PennBioIEPOSTag(jCas, 28, 31);
        p6.setValue("CC");
        p6.addToIndexes();
        final PennBioIEPOSTag p7 = new PennBioIEPOSTag(jCas, 32, 42);
        p7.setValue("VBD");
        p7.addToIndexes();
        final PennBioIEPOSTag p8 = new PennBioIEPOSTag(jCas, 43, 48);
        p8.setValue("IN");
        p8.addToIndexes();
        final PennBioIEPOSTag p9 = new PennBioIEPOSTag(jCas, 49, 50);
        p9.setValue("DT");
        p9.addToIndexes();
        final PennBioIEPOSTag p10 = new PennBioIEPOSTag(jCas, 51, 59);
        p10.setValue("NN");
        p10.addToIndexes();
        final PennBioIEPOSTag p11 = new PennBioIEPOSTag(jCas, 59, 60);
        p11.setValue(".");
        p11.addToIndexes();

        engine.process(jCas);


    }
}
