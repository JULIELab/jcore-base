package de.julielab.jcore.ae.ecn;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ECNumberAnnotator extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(ECNumberAnnotator.class);
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		log.info("The method de.julielab.jcore.ae.ecn.ECNumberAnnotator#process(JCas) has to be implemented");
	}

}
