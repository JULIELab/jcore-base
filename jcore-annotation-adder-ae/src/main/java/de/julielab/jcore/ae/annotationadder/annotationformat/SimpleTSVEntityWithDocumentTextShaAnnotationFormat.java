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
        String docId = record[0].intern();
        int begin = !record[1].isBlank() ? Integer.parseInt(record[1]) : -1;
        int end = !record[2].isBlank() ? Integer.parseInt(record[2]) : -1;
        String sha = record[3].intern();
        String type = null;
        if (record.length > 4)
            type = record[4].intern();
        final ExternalTextAnnotation externalTextAnnotation = new ExternalTextAnnotation(docId, begin, end, type);
        externalTextAnnotation.addPayload("sha", sha);
        return externalTextAnnotation;
    }

    @Override
    public void withHeader(boolean withHeader) {
        // does nothing
    }
}
