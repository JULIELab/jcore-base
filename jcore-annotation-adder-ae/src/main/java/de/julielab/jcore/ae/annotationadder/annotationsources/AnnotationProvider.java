package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;

public interface AnnotationProvider<T extends AnnotationData> {
    T getAnnotations(String id);
}
