package de.julielab.jcore.ae.annotationadder.annotationrepresentations;

import java.util.ArrayList;
import java.util.Collection;

public class AnnotationList<T extends AnnotationData> extends ArrayList<T> implements AnnotationData {
    @Override
    public boolean add(T t) {
        setDocId(t.getDocumentId());
        return super.add(t);
    }

    @Override
    public void add(int index, T element) {
        setDocId(element.getDocumentId());
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (c != null)
            c.stream().findAny().ifPresent(annotation -> setDocId(annotation.getDocumentId()));
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (c != null)
            c.stream().findAny().ifPresent(annotation -> setDocId(annotation.getDocumentId()));
        return super.addAll(index, c);
    }

    private String docId;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        if (docId != null && this.docId != null && !docId.equals(this.docId))
            throw new IllegalArgumentException("This annotation list already contains annotations for document with ID " + this.docId + " but the document ID should now be set to " + docId + ".");
        this.docId = docId;
    }

    @Override
    public String getDocumentId() {
        return docId;
    }
}
