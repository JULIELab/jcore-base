package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;

public class SimpleTSVEntityWithDocumentTextShaAnnotationFormat implements AnnotationFormat<ExternalTextAnnotation> {
    @Override
    public ExternalTextAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 4)
            throw new IllegalArgumentException("Expected a 4 or 5-column format providing document ID, begin, end, sha-hash and UIMA type (optional if the default type is set to the AnnotationAdderAnnotator) for the annotation but got " + record.length + " columns: " + data);
        String docId = record[0];
        int begin = Integer.parseInt(record[1]);
        int end = Integer.parseInt(record[2]);
        String sha = record[3];
        String type = null;
        if (record.length > 4)
            type = record[4];
        final ExternalTextAnnotation externalTextAnnotation = new ExternalTextAnnotation(docId, begin, end, type);
        externalTextAnnotation.addPayload("sha", sha);
        return externalTextAnnotation;
    }
}
