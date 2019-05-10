package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import org.apache.uima.jcas.JCas;

public class DocumentClassAdder implements AnnotationAdder {
    @Override
    public boolean addAnnotations(AnnotationData data, AnnotationAdderHelper helper, AnnotationAdderConfiguration configuration, JCas jCas) {

        return false;
    }
}
