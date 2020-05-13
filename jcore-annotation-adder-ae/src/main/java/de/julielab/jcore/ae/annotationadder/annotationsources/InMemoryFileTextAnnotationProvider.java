package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationformat.AnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationformat.SimpleTSVEntityAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class InMemoryFileTextAnnotationProvider implements AnnotationProvider<AnnotationList> {
    public static final String PARAM_ANNOTATION_FORMAT = "AnnotationFormatClass";
    private final static Logger log = LoggerFactory.getLogger(InMemoryFileTextAnnotationProvider.class);
    private AnnotationSource<AnnotationList> annotationSource;

    @Override
    public AnnotationList<ExternalTextAnnotation> getAnnotations(String id) {
        return annotationSource.getAnnotations(id);
    }

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        final ConfigurationParameterSettings parameterSettings = dataResource.getMetaData().getConfigurationParameterSettings();
        final String formatClassName = (String) Optional.ofNullable(parameterSettings.getParameterValue(PARAM_ANNOTATION_FORMAT)).orElse(SimpleTSVEntityAnnotationFormat.class.getCanonicalName());
        AnnotationFormat<ExternalTextAnnotation> format;
        try {
            format = (AnnotationFormat<ExternalTextAnnotation>) Class.forName(formatClassName).getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Could not instantiate class {}", formatClassName);
            throw new ResourceInitializationException(e);
        }
        annotationSource = new FileAnnotationSource(format);
        annotationSource.initialize(dataResource);
    }


}
