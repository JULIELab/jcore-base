package de.julielab.jcore.multiplier.testmultiplier;

import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.jcas.JCas;

public class TestMultiplier extends JCasMultiplier_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return false;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        return null;
    }
}
