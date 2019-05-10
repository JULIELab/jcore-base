package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationformat.SimpleTSVEntityAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;

public class InMemoryFileTextAnnotationProvider implements AnnotationProvider<AnnotationList> {
    private AnnotationSource<AnnotationList> annotationSource;

    @Override
    public AnnotationList<ExternalTextAnnotation> getAnnotations(String id) {
        return annotationSource.getAnnotations(id);
    }

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        // This logic could be made configurable if required so in the future.
        annotationSource = new FileAnnotationSource(new SimpleTSVEntityAnnotationFormat());
        annotationSource.initialize(dataResource);
    }



}
