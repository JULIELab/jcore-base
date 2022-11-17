package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import org.apache.uima.resource.DataResource;

import java.io.IOException;
import java.net.URI;

public interface AnnotationSource<T extends AnnotationData> {
    void loadAnnotations(URI annotationUri) throws IOException;

    void initialize(DataResource dataResource) throws IOException;
    T getAnnotations(String id);
}
