package de.julielab.jcore.reader.xmi.flowcontroller;

import de.julielab.jcore.types.casflow.ToVisit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;

/**
 * <p>Returns steps according an existing {@link ToVisit} annotation of the CAS or, if not present, the default aggregate flow.</p>
 */
public class AnnotationDefinedFlow extends JCasFlow_ImplBase {
    private String[] toVisitKeys;
    private String[] fixedFlow;
    private int currentPos;

    public AnnotationDefinedFlow(ToVisit toVisit, FlowConstraints flowConstraints) throws AnalysisEngineProcessException {
        if (!(flowConstraints instanceof FixedFlow))
            throw new AnalysisEngineProcessException(new IllegalArgumentException("This flow requires the FixedFlow to determine the default processing order. However, the flow constraints are of type " + flowConstraints.getClass().getCanonicalName()));
        this.fixedFlow = toVisit != null ? ((FixedFlow) flowConstraints).getFixedFlow() : null;
        this.toVisitKeys = toVisit.getDelegateKeys().toArray();
        this.currentPos = 0;
    }

    /**
     * <p>Routes the CAS to the next component defined by the CAS'es {@link ToVisit} annotation or,
     * if <tt>ToVisit</tt> was not found, to the next component as defined by the default fixed flow.</p>
     *
     * @return The next component to visit or the next default flow component.
     */
    @Override
    public Step next() {
        String nextAEKey = toVisitKeys != null ? toVisitKeys[currentPos] : fixedFlow[currentPos];
        ++currentPos;
        return new SimpleStep(nextAEKey);
    }
}
