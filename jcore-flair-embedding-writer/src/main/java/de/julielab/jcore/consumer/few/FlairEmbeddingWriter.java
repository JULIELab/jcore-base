
package de.julielab.jcore.consumer.few;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceMetaData(name = "JCoRe Flair Embedding Writer", description = "Given a Flair compatible embedding and a UIMA annotation type, this component prints the embeddings of words annotated with the annotation to a file.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Token"})
public class FlairEmbeddingWriter extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(FlairEmbeddingWriter.class);


	public static final String PARAM_ANNOTATION_TYPE = "AnnotationType";

	@ConfigurationParameter(name=PARAM_ANNOTATION_TYPE, description = "Fully qualified type name to output embeddings for.")
    private String annotationType;

    /**
	 * This method is called a single time by the framework at component
	 * creation. Here, descriptor parameters are read and initial setup is done.
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {

        annotationType = (String) aContext.getConfigParameterValue(PARAM_ANNOTATION_TYPE);
    }

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        final Type type = aJCas.getTypeSystem().getType(annotationType);
        if (type == null) {
            throw new AnalysisEngineProcessException(new IllegalArgumentException("The type " + annotationType + " was not found in the type system."));
        }
        if (aJCas.getAnnotationIndex(type).iterator().hasNext()) {
        }
    }

}
