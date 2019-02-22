package de.julielab.jcore.ae.annotationadder.annotationrepresentations;

public class ExternalAnnotation implements AnnotationData {
    private String documentId;
    private int start;
    private int end;
    private String uimaType;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getUimaType() {
        return uimaType;
    }

    public void setUimaType(String uimaType) {
        this.uimaType = uimaType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public ExternalAnnotation(String documentId, int start, int end, String uimaType) {

        this.documentId = documentId;
        this.start = start;
        this.end = end;
        this.uimaType = uimaType;
    }
}
