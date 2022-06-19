package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationformat.AnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationformat.SimpleTSVEntityAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public abstract class TextAnnotationProvider implements AnnotationProvider<AnnotationList> {
    public static final String PARAM_ANNOTATION_FORMAT = "AnnotationFormatClass";
    public static final String PARAM_INPUT_HAS_HEADER = "InputHasHeader";
    public static final String PARAM_COLUMN_NAMES = "ColumnNames";
    public static final String COL_DOC_ID = "docId";
    public static final String COL_BEGIN = "beginOffset";
    public static final String COL_END = "endOffset";
    public static final String COL_UIMA_TYPE = "uimaType";
    protected Logger log;
    protected AnnotationSource<AnnotationList<ExternalTextAnnotation>> annotationSource;
    protected AnnotationFormat<ExternalTextAnnotation> format;

    @Override
    public AnnotationList<ExternalTextAnnotation> getAnnotations(String id) {
        return annotationSource.getAnnotations(id);
    }

    abstract void initializeAnnotationSource();

    abstract Logger getLogger();

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        final ConfigurationParameterSettings parameterSettings = dataResource.getMetaData().getConfigurationParameterSettings();
        final String formatClassName = (String) Optional.ofNullable(parameterSettings.getParameterValue(PARAM_ANNOTATION_FORMAT)).orElse(SimpleTSVEntityAnnotationFormat.class.getCanonicalName());
        final boolean hasHeader = (boolean) Optional.ofNullable(parameterSettings.getParameterValue(PARAM_INPUT_HAS_HEADER)).orElse(false);
        final String[] columnNames = (String[])parameterSettings.getParameterValue(PARAM_COLUMN_NAMES);
        try {
            format = (AnnotationFormat<ExternalTextAnnotation>) Class.forName(formatClassName).getDeclaredConstructor().newInstance();
            format.hasHeader(hasHeader);
            format.setColumnNames(columnNames);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            getLogger().error("Could not instantiate class {}", formatClassName);
            throw new ResourceInitializationException(e);
        }
        initializeAnnotationSource();
        try {
            annotationSource.initialize(dataResource);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }
}
