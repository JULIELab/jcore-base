package de.julielab.jcore.ae.testae;

import de.julielab.jcore.types.Sentence;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

public class TestAE extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Sentence s = new Sentence(aJCas, 0, 10);
	}

}
