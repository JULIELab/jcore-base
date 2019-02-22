package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import org.apache.uima.jcas.JCas;

public interface AnnotationAdder {
    /**
     * Tries to add the given annotation data to the given cas. Returns <tt>true</tt> if succeeding, false otherwise.
     *
     * @param data The annotation data to add to the CAS. The AnnotationAdder must check if it handles the given data and return <tt>true</tt> or <tt>false</tt> accordingly.
     * @param helper This object serves as utility method object as well as cache for data that would otherwise be computed multiple times.
     * @param configuration Configuration relevant to adding annotations such as the offset mode.
     * @param jCas The CAS to add the annotations to.
     * @return <tt>true</tt> if the data was accepted, <tt>false</tt> otherwise.
     */
    boolean addAnnotations(AnnotationData data, AnnotationAdderHelper helper, AnnotationAdderConfiguration configuration, JCas jCas);

}
