package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;

public class SimpleTSVEntityAnnotationFormat implements AnnotationFormat<ExternalTextAnnotation> {
    private String[] header;
    private boolean withHeader;

    @Override
    public ExternalTextAnnotation parse(String data) {
            if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 3)
            throw new IllegalArgumentException("Expected a 3 or 4-column format providing document ID, begin, end and UIMA type (optional if the default type is set to the AnnotationAdderAnnotator) for the annotation but got " + record.length + " columns: " + data);
        if (withHeader && header == null) {
            header = record;
            return null;
        }
        String docId = record[0];
        int begin = Integer.parseInt(record[1]);
        int end = Integer.parseInt(record[2]);
        String type = null;
        if (record.length > 3)
            type = record[3];
        ExternalTextAnnotation externalTextAnnotation = new ExternalTextAnnotation(docId, begin, end, type);
        if (record.length > 4) {
            if (header != null) {
                for (int i = 4; i < record.length; i++)
                    externalTextAnnotation.addPayload(header[i], record[i]);
            }
        }
        return externalTextAnnotation;
    }

    @Override
    public void hasHeader(boolean withHeader) {
        this.withHeader = withHeader;
    }

    @Override
    public void setColumnNames(String[] header) {
        this.header = header;
    }
}
