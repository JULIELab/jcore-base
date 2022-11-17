package de.julielab.jcore.flow.annotationdefined;

import de.julielab.jcore.types.casflow.ToVisit;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.JCasFlowController_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * <p>Routes CASes through an aggregate analysis engine according to the {@link ToVisit} annotation present in the CAS.</p>
 * <p>If there is no <tt>ToVisit</tt> annotation, the default (fixed) flow will be used. Thus, the fixed flow constraint
 * must be set on the aggregate engine.</p>
 */
@ResourceMetaData(name = "JCoRe Annotation Defined Flow Controller", description = "This flow controller relies on an annotation of type ToVisit to be present in the CAS. If there is no such annotation, the default fixed flow of the aggregate engine using this flow controller is used. Otherwise, the names of the components to pass the CAS to are taken from the annotation. If the annotation exists but defines to components to be visited by the CAS, no components are visited at all.", vendor = "JULIE Lab, Germany", version = "placeholder")
public class AnnotationDefinedFlowController extends JCasFlowController_ImplBase {
    @Override
    public Flow computeFlow(JCas jCas) throws AnalysisEngineProcessException {
        boolean exists = JCasUtil.exists(jCas, ToVisit.class);
        ToVisit toVisit = exists ? JCasUtil.selectSingle(jCas, ToVisit.class) : null;
        // When toVisit is null, the default, fixed flow is used.
        return new AnnotationDefinedFlow(toVisit, getContext().getAggregateMetadata().getFlowConstraints(), jCas);
    }
}
