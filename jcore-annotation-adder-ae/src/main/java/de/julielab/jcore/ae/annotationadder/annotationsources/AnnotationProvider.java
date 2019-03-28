package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import org.apache.uima.resource.SharedResourceObject;

public interface AnnotationProvider<T extends AnnotationData> extends SharedResourceObject {
    T getAnnotations(String id);
}
