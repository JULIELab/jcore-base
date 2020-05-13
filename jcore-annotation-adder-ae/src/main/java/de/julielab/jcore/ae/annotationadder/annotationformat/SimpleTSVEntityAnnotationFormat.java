package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;

public class SimpleTSVEntityAnnotationFormat implements AnnotationFormat<ExternalTextAnnotation> {
    @Override
    public ExternalTextAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 3)
            throw new IllegalArgumentException("Expected a 3 or 4-column format providing document ID, begin, end and UIMA type (optional if the default type is set to the AnnotationAdderAnnotator) for the annotation but got " + record.length + " columns: " + data);
        String docId = record[0];
        int begin = Integer.parseInt(record[1]);
        int end = Integer.parseInt(record[2]);
        String type = null;
        if (record.length > 3)
            type = record[3];
        return new ExternalTextAnnotation(docId, begin, end, type);
    }
}
