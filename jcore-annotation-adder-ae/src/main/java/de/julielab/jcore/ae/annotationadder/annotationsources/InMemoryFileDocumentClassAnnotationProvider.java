package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationformat.DocumentClassAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationformat.SimpleTSVEntityAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalDocumentClassAnnotation;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;

public class InMemoryFileDocumentClassAnnotationProvider implements AnnotationProvider<AnnotationList> {
    private AnnotationSource<AnnotationList<ExternalDocumentClassAnnotation>> annotationSource;

    @Override
    public AnnotationList<ExternalDocumentClassAnnotation> getAnnotations(String id) {
        return annotationSource.getAnnotations(id);
    }

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        // This logic could be made configurable if required so in the future.
        annotationSource = new FileAnnotationSource(new DocumentClassAnnotationFormat());
        annotationSource.initialize(dataResource);
    }



}
