package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import org.apache.uima.resource.DataResource;

public interface AnnotationSource<T extends AnnotationData> {
    void initialize(DataResource dataResource);
    T getAnnotations(String id);
}
