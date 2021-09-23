
package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ResourceMetaData(name="JCoRe Annotation Adder", description = "This component helps to import annotations made on the exact CAS document text by an external process back into the CAS. To this end, the component is prepared to read several data formats. Currently, simple offset-based annotations are supported with configurable UIMA types. The component supports character and token based offsets.")
public class AnnotationAdderAnnotator extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(AnnotationAdderAnnotator.class);
    private AnnotationAdderConfiguration adderConfiguration;

    public enum OffsetMode {CHARACTER, TOKEN}
	public static final String KEY_ANNOTATION_SOURCE = "AnnotationSource";
	public static final String PARAM_OFFSET_MODE = "OffsetMode";
	public static final String PARAM_DEFAULT_UIMA_TYPE = "DefaultUimaType";
    public static final String PARAM_PREVENT_PROCESSED_MARK = "PreventProcessedMarkOnDigestMismatch";
	@ExternalResource(key = KEY_ANNOTATION_SOURCE, description = "A provider of annotations to add to the CAS. Must implement the de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider interface.")
    private AnnotationProvider<? extends AnnotationData> annotationProvider;
	@ConfigurationParameter(name=PARAM_OFFSET_MODE, mandatory = false, description = "Determines the interpretation of annotation offsets. Possible values: \"CHARACTER\" and \"TOKEN\". For the TOKEN offset mode, the correct tokenization must be given in the CAS. TOKEN offsets start with 1, CHARACTER offsets are 0-based. Defaults to CHARACTER.", defaultValue = "CHARACTER")
    private OffsetMode offsetMode;
	@ConfigurationParameter(name=PARAM_DEFAULT_UIMA_TYPE, mandatory = false, description = "Most external annotation formats require that the qualified name a UIMA type is provided which reflects the annotation to be created for the respective annotation. With this parameter, a default type can be provided which will be forwarded to the format parser. If the parser supports it, the type can then be omitted from the external annotation source.")
	private String defaultUimaType;
	@ConfigurationParameter(name = PARAM_PREVENT_PROCESSED_MARK, mandatory = false, description = "This setting is only in effect if an input format is used that contains document text SHA256 digests while also writing the annotation results into a JeDIS database. If then a CAS document text, to which annotations should be added, does not match the digest given by an annotation, this CAS will not marked as being finished processing by DBCheckpointAE that may follow in the pipeline. The idea is that the mismatched documents require a reprocessing of the original annotation creation algorithm because their text has been changed relative to the annotation on file. By not setting the document as being finished processed, it is straightforward to process only those documents again that failed to add one or multiple annotations.")
    private boolean preventProcessedOnDigestMismatch;


    private List<AnnotationAdder> annotationAdders = Arrays.asList(new TextAnnotationListAdder(), new DocumentClassAnnotationAdder());

    /**
	 * This method is called a single time by the framework at component
	 * creation. Here, descriptor parameters are read and initial setup is done.
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        offsetMode = OffsetMode.valueOf(Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_OFFSET_MODE)).orElse(OffsetMode.CHARACTER.name()));
        defaultUimaType = (String) aContext.getConfigParameterValue(PARAM_DEFAULT_UIMA_TYPE);
        preventProcessedOnDigestMismatch = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_PREVENT_PROCESSED_MARK)).orElse(false);
        try {
            annotationProvider = (AnnotationProvider<? extends AnnotationData>) aContext.getResourceObject(KEY_ANNOTATION_SOURCE);
        } catch (ResourceAccessException e) {
            log.error("Could not create the annotation provider", e);
            throw new ResourceInitializationException(e);
        }
        adderConfiguration = new AnnotationAdderConfiguration();
        adderConfiguration.setOffsetMode(offsetMode);
        adderConfiguration.setDefaultUimaType(defaultUimaType);
    }

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) {
        final String docId = JCoReTools.getDocId(aJCas);
        if (docId == null)
            log.error("The current document does not have a header. Cannot add external annotations.");
        final AnnotationData annotations = annotationProvider.getAnnotations(docId);
        final AnnotationAdderHelper helper = new AnnotationAdderHelper();
        if (annotations != null) {
            boolean success = false;
            int adderNum = 0;
            // We are now iterating through the available annotation adders for the one that handles the obtained annotation data
            while (adderNum < annotationAdders.size() && !(success = annotationAdders.get(adderNum).addAnnotations(annotations, helper, adderConfiguration, aJCas, preventProcessedOnDigestMismatch))) {
                ++adderNum;
            }
            if (!success)
                throw new IllegalArgumentException("There was no annotation adder to handle the annotation data of class " + annotations.getClass().getCanonicalName());
        } else {
            log.debug("No external annotations were delivered for document ID {}", docId);
        }
    }

}
