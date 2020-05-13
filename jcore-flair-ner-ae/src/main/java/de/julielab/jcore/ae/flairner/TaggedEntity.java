package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.TextAnnotation;

public class TaggedEntity implements TextAnnotation {
    private String documentId;
    private String tag;
    private double labelConfidence;
    private int start;
    private int end;

    /**
     * The confidence that the given label is correct.
     * @return
     */
    public Double getLabelConfidence() {
        return labelConfidence;
    }

    public TaggedEntity(String sentenceId, String tag, double labelConfidence, int begin, int end) {
        this.documentId = sentenceId;
        this.tag = tag;
        this.labelConfidence = labelConfidence;
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
