package de.julielab.jcore.multiplier.line;

import de.julielab.jcore.types.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Predicate;

@ResourceMetaData(name = "JCoRe Line Multiplier", description = "Splits incoming CAS document texts on line breaks and returns one CAS for each non-blank line.")
public class LineMultiplier extends JCasMultiplier_ImplBase {
    public static final String PARAM_NUM_LINES = "NumberLinesPerCAS";
    private final static Logger log = LoggerFactory.getLogger(LineMultiplier.class);
    @ConfigurationParameter(name = PARAM_NUM_LINES, mandatory = false, defaultValue = "1", description = "The number of lines that should be put into one cas. Defaults to 1.")
    private int numLinesPerCas;
    private Deque<String> lines = new ArrayDeque<>();
    private int numLines = 0;
    private int numCases = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        numLinesPerCas = Optional.ofNullable((Integer) aContext.getConfigParameterValue(PARAM_NUM_LINES)).orElse(1);
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        Arrays.stream(aJCas.getDocumentText().split(System.getProperty("line.separator"))).filter(Predicate.not(String::isBlank)).forEach(lines::add);
        log.info("Got {} lines", lines.size());
    }

    @Override
    public AbstractCas next() {
        JCas cas = getEmptyJCas();
        StringBuilder sb = new StringBuilder();
        String ls = System.getProperty("line.separator");
        for (int i = 0; i < numLinesPerCas && !lines.isEmpty(); i++) {
            sb.append(lines.removeFirst() + ls);
            ++numLines;
        }
        sb.delete(sb.length() - ls.length(), sb.length());
        cas.setDocumentText(sb.toString());
        Header h = new Header(cas);
        h.setDocId("line" + numCases++);
        if (numLines % 1000 == 0)
            log.info("Batch checkpoint: {} lines left", lines.size());
        return cas;
    }


    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return !lines.isEmpty();
    }

}
