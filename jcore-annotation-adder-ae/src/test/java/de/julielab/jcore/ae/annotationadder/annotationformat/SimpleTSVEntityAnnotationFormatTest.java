package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SimpleTSVEntityAnnotationFormatTest {

    @Test
    void parse() {
        SimpleTSVEntityAnnotationFormat format = new SimpleTSVEntityAnnotationFormat();
        format.hasHeader(true);
        // should be ignored
        assertNull(format.parse("# comment"));
        // should be stored as header but not return something
        assertNull(format.parse("docId\tbegin\tend\ttype\tspecificType\tcomponentId"));
        ExternalTextAnnotation extAnnotation = format.parse("123\t0\t5\tde.julielab.jcore.types.Gene\tprotein\tGoldAnnotation");
        assertEquals("123", extAnnotation.getDocumentId());
        assertEquals(0, extAnnotation.getStart());
        assertEquals(5, extAnnotation.getEnd());
        assertEquals("de.julielab.jcore.types.Gene", extAnnotation.getUimaType());
        assertEquals("protein", extAnnotation.getPayload("specificType"));
        assertEquals("GoldAnnotation", extAnnotation.getPayload("componentId"));
    }
}