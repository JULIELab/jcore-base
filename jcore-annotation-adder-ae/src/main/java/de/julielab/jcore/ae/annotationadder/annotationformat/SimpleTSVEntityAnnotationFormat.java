package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalAnnotation;

public class SimpleTSVEntityAnnotationFormat implements AnnotationFormat<ExternalAnnotation>{
    @Override
    public ExternalAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length != 4)
            throw new IllegalArgumentException("Expected a 4-column format providing document ID, begin, end and UIMA type for the annotation but got " + record.length + " columns: " + data);
        String docId = record[0];
        int begin = Integer.parseInt(record[1]);
        int end = Integer.parseInt(record[2]);
        String type = record[3];
        return new ExternalAnnotation(docId, begin, end, type);
    }
}
