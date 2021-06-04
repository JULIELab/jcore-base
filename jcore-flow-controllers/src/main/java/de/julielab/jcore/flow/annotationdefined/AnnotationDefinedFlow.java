package de.julielab.jcore.flow.annotationdefined;

import de.julielab.jcore.types.casflow.ToVisit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Returns steps according an existing {@link ToVisit} annotation of the CAS or, if not present, the default aggregate flow.</p>
 * <p>This is, for example, used by the <tt>XMLDBMultiplier</tt> to let CASes skip large parts of the pipeline when
 * the currently read document already exists in the database.</p>
 */
public class AnnotationDefinedFlow extends JCasFlow_ImplBase {
    private String[] toVisitKeys;
    private String[] fixedFlow;
    private int currentPos;

    /**
     * <p>Creates a flow that follows to entries in {@link ToVisit#getDelegateKeys()} of <tt>toVisit</tt> or, if
     * <tt>toVisit</tt> is null, falls back to the default fixed flow.</p>
     * <p>If <tt>toVisit</tt> is not null but the <tt>delegateKeys</tt> are null or empty, no component in the aggregate using this flow will process the respective CAS.</p>
     * @param toVisit An annotation containing the keys of the delegate AEs to visit. May be null which case the default fixed flow will be used.
     * @param flowConstraints The default fixed flow of the aggregate analysis engine.
     * @throws AnalysisEngineProcessException If <tt>flowConstraints</tt> is not a fixed flow.
     */
    public AnnotationDefinedFlow(@Nullable ToVisit toVisit, FlowConstraints flowConstraints) throws AnalysisEngineProcessException {
        if (!(flowConstraints instanceof FixedFlow))
            throw new AnalysisEngineProcessException(new IllegalArgumentException("This flow requires the FixedFlow to determine the default processing order. However, the flow constraints are of type " + flowConstraints.getClass().getCanonicalName()));
        this.fixedFlow = ((FixedFlow) flowConstraints).getFixedFlow();
        // We have the following cases:
        // 1. There are given keys to visit, use them.
        // 2. There are no keys given but the ToVisit annotation is not null, skip all components.
        // 3. There is not ToVisit annotation at all, use the default fixed flow.
        if(toVisit != null && toVisit.getDelegateKeys() != null)
            toVisitKeys = toVisit.getDelegateKeys().toArray();
        else if (toVisit != null)
            toVisitKeys = new String[0];
        else
            toVisitKeys = null;
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
        // If toVisitKeys was not given, we just use the fixedFlow.
        if ((toVisitKeys == null && currentPos < fixedFlow.length) || (toVisitKeys != null && currentPos < toVisitKeys.length)) {
            String nextAEKey = toVisitKeys != null ? toVisitKeys[currentPos] : fixedFlow[currentPos];
            ++currentPos;
            return new SimpleStep(nextAEKey);
        }
        return new FinalStep();
    }
}
