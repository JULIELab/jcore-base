package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.DocumentIds;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class DBMultiplier extends JCasMultiplier_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        DocumentIds documentIds = JCasUtil.selectSingle(aJCas, DocumentIds.class);
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
