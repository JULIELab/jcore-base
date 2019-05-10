package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalDocumentClassAnnotation;

public class DocumentClassAnnotationFormat implements AnnotationFormat<ExternalDocumentClassAnnotation> {
    @Override
    public ExternalDocumentClassAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 4)
            throw new IllegalArgumentException("Expected a 4-column format providing class assignment confidence, document ID, the class assigned and the assigning component ID and UIMA type (optional if the default type is set to the AnnotationAdderAnnotator) for the annotation but got " + record.length + " columns: " + data);
        double confidence = Double.valueOf(record[0]);
        String docId = record[1];
        String documentClass = record[2].intern();
        String componentId = record[3].intern();
        String type = null;
        if (record.length > 4)
            type = record[4].intern();
        return new ExternalDocumentClassAnnotation(docId, documentClass, confidence, componentId, type);
    }
}
