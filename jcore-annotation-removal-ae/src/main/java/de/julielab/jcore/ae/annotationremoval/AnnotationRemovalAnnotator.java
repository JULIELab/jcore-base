
package de.julielab.jcore.ae.annotationremoval;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ResourceMetaData(name="JCoRe Annotation Removal AE", description = "Removes annotations from the CAS that belong to one of the types specified as a parameter value in the descriptor.", vendor = "JULIE Lab Jena, Germany")
public class AnnotationRemovalAnnotator extends JCasAnnotator_ImplBase {
public static final String PARAM_ANNOTATION_TYPES = "AnnotationTypes";
	private final static Logger log = LoggerFactory.getLogger(AnnotationRemovalAnnotator.class);

	@ConfigurationParameter(name=PARAM_ANNOTATION_TYPES, description="List of qualified UIMA type names for which all annotations should be removed from each CAS.")
	private String[] annotationTypesForRemoval;

	/**
	 * This method is called a single time by the framework at component
	 * creation. Here, descriptor parameters are read and initial setup is done.
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {
		annotationTypesForRemoval = (String[]) aContext.getConfigParameterValue(PARAM_ANNOTATION_TYPES);
		if (annotationTypesForRemoval.length == 0)
			throw new ResourceInitializationException(new IllegalArgumentException("The list of annotations for removal, given through parameter '" + PARAM_ANNOTATION_TYPES + "' is empty."));
	}

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) {
		List<Annotation> removalList = new ArrayList<>();
		for (String annotationTypeName : annotationTypesForRemoval) {
			final Type type = aJCas.getTypeSystem().getType(annotationTypeName);
			aJCas.getAnnotationIndex(type).forEach(removalList::add);
			removalList.forEach(Annotation::removeFromIndexes);
		}
	}

}
