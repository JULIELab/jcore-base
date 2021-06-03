package de.julielab.jcore.reader.xmi.flowcontroller;

import de.julielab.jcore.types.casflow.ToVisit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.JCasFlowController_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * <p>Routes CASes through an aggregate analysis engine according to the {@link ToVisit} annotation present in the CAS.</p>
 * <p>If there is not <tt>ToVisit</tt> annotation, the default (fixed) flow will be used. Thus, the fixed flow constraint
 * must be set on the aggregate engine.</p>
 */
public class AnnotationDefinedFlowController extends JCasFlowController_ImplBase {
    @Override
    public Flow computeFlow(JCas jCas) throws AnalysisEngineProcessException {
        boolean exists = JCasUtil.exists(jCas, ToVisit.class);
        ToVisit toVisit = exists ? JCasUtil.selectSingle(jCas, ToVisit.class) : null;
        // When toVisit is null, the default, fixed flow is used.
        return new AnnotationDefinedFlow(toVisit, getContext().getAggregateMetadata().getFlowConstraints());
    }
}
