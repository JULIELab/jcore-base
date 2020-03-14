
package de.julielab.jcore.multiplier.line;

import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Predicate;

@ResourceMetaData(name="JCoRe Line Multiplier", description = "Splits incoming CAS document texts on line breaks and returns one CAS for each non-blank line.")
public class LineMultiplier extends JCasMultiplier_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(LineMultiplier.class);
	private Deque<String> lines = new ArrayDeque<>();

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
		Arrays.stream(aJCas.getDocumentText().split(System.getProperty("line.separator"))).filter(Predicate.not(String::isBlank)).forEach(lines::add);
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas cas = getEmptyJCas();
		cas.setDocumentText(lines.removeFirst());
		return cas;
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return !lines.isEmpty();
	}

}
