package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.TextAnnotation;

public class TaggedEntity implements TextAnnotation {
    private String documentId;
    private String tag;
    private int start;
    private int end;

    public TaggedEntity(String sentenceId, String tag, int begin, int end) {
        this.documentId = sentenceId;
        this.tag = tag;
        this.start = begin;
        this.end = end;
    }

    @Override
    public String toString() {
        return "TaggedEntity{" +
                "documentId='" + documentId + '\'' +
                ", tag='" + tag + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

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
}
