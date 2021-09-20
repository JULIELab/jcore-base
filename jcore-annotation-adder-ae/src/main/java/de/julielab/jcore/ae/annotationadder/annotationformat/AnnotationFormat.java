package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;

public interface AnnotationFormat<T extends AnnotationData> {
    T parse(String data);

    void withHeader(boolean withHeader);
}
