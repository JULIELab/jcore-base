package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import de.julielab.jcore.types.Gene;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotationAdderHelperTest {

    @Test
    void setAnnotationPayloadsToFeatures() throws UIMAException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        Gene gene = new Gene(jCas);
        ExternalTextAnnotation extAnnotation = new ExternalTextAnnotation("1", 0, 1, "dummy");
        extAnnotation.addPayload("specificType", "protein");
        AnnotationAdderHelper helper = new AnnotationAdderHelper();
        helper.setAnnotationPayloadsToFeatures(gene, extAnnotation);
        assertEquals("protein", gene.getSpecificType());
    }
}