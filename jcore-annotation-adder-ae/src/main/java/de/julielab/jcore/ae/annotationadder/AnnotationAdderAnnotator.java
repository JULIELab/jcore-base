
package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AnnotationAdderAnnotator extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(AnnotationAdderAnnotator.class);
    private AnnotationAdderConfiguration adderConfiguration;

    public enum OffsetMode {CHARACTER, TOKEN}
	public static final String KEY_ANNOTATION_SOURCE = "AnnotationSource";
	public static final String PARAM_OFFSET_MODE = "OffsetMode";
	@ExternalResource(key = KEY_ANNOTATION_SOURCE, description = "A provider of annotations to add to the CAS. Must implement the de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider interface.")
    private AnnotationProvider<? extends AnnotationData> annotationProvider;
	@ConfigurationParameter(name=PARAM_OFFSET_MODE, mandatory = false, description = "Determines the interpretation of annotation offsets. Possible values: \"CHARACTER\" and \"TOKEN\". For the TOKEN offset mode, the correct tokenization must be given in the CAS. Defaults to CHARACTER.", defaultValue = "CHARACTER")
    private OffsetMode offsetMode;

    private List<AnnotationAdder> annotationAdders = Arrays.asList(new AnnotationListAdder());

    /**
	 * This method is called a single time by the framework at component
	 * creation. Here, descriptor parameters are read and initial setup is done.
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        offsetMode = OffsetMode.valueOf(Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_OFFSET_MODE)).orElse(OffsetMode.CHARACTER.name()));
        try {
            annotationProvider = (AnnotationProvider<? extends AnnotationData>) aContext.getResourceObject(KEY_ANNOTATION_SOURCE);
        } catch (ResourceAccessException e) {
            e.printStackTrace();
        }
        adderConfiguration = new AnnotationAdderConfiguration();
        adderConfiguration.setOffsetMode(offsetMode);
    }

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        final String docId = JCoReTools.getDocId(aJCas);
        final AnnotationData annotations = annotationProvider.getAnnotations(docId);
        final AnnotationAdderHelper helper = new AnnotationAdderHelper();
        boolean success = false;
        int adderNum = 0;
        // We are now iterating through the available annotation adders for the one that handles the obtained annotation data
        while (adderNum < annotationAdders.size() && !(success = annotationAdders.get(adderNum).addAnnotations(annotations, helper, adderConfiguration, aJCas))) {
            ++adderNum;
        }
        if (!success)
            throw new IllegalArgumentException("There was no annotation adder to handle the annotation data of class " + annotations.getClass().getCanonicalName());
    }

}
