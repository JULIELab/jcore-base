package de.julielab.jcore.ae.annotationadder.annotationrepresentations;

import java.util.ArrayList;

public class AnnotationList extends ArrayList<ExternalAnnotation> implements AnnotationData {

    private String docId;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    @Override

    public String getDocumentId() {
        return docId;
    }
}
