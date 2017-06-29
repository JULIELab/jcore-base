package de.julielab.jcore.consumer.cas2conll.test;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.consumer.cas2conll.ConllConsumer;

public class ConllConsumerTest {

	@Test
	public void testProcessEmptyCAS() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ConllConsumer.class,
				ConllConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");

		consumer.process(cas);
	}

	@Test
	public void testProcess() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ConllConsumer.class,
				ConllConsumer.PARAM_OUTPUT_DIR, "src/test/resources/data");
		
		cas.setDocumentText("This is a simple test. Only to test it.");
		
		consumer.process(cas);

	}

}
