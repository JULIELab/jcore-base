package de.julielab.jules.ae.genemapper.eval.tools;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.NavigableSet;

import org.apache.commons.lang3.Range;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;

import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class JCasConverterTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() throws UIMAException, IOException {
		AnalysisEngine sentenceSplitter = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		String text = "This is a test. Offsets should be 0, 15, 16 and 51.";

		jCas.setDocumentText(text);
		sentenceSplitter.process(jCas);
		NavigableSet<Range<Integer>> sentences = JCasConverter.mapSentences2Ranges(jCas);
		@SuppressWarnings("unchecked")
		Range[] ranges = (Range<Integer>[]) sentences.toArray(new Range[0]);
		Range[] expected = {Range.between(0, 15), Range.between(16, 51)};

		assertTrue("Should contain 2 ranges.", sentences.size() == 2);
		assertArrayEquals("", expected, ranges);
	}

}
