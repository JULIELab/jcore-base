package de.julielab.jcore.ae.annotationadder.annotationrepresentations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.julielab.jcore.ae.annotationadder.annotationsources.TextAnnotationProvider.*;

public class ExternalTextAnnotation implements TextAnnotation {
    private String documentId;
    private int start;
    private int end;
    private String uimaType;
    private boolean payloadFeatureValues;
    private Map<String, Object> payload;

    public ExternalTextAnnotation(String documentId, int start, int end, String uimaType) {

        this.documentId = documentId;
        this.start = start;
        this.end = end;
        this.uimaType = uimaType;
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

    public void addPayload(String key, Object value) {
        if (payload == null)
            payload = new HashMap<>();
        payload.put(key, value);
    }

    public Map<String, Object> getAllFieldValuesAsMap() {
        final Map<String, Object> values = new HashMap<>();
        values.put(COL_BEGIN, start);
        values.put(COL_END, end);
        values.put(COL_UIMA_TYPE, uimaType);
        values.put(COL_DOC_ID, documentId);
        if (payload != null)
            values.putAll(payload);
        return values;
    }

    public Object getPayload(String key) {
        return payload != null ? payload.get(key) : null;
    }

    public Collection<String> getPayloadKeys() {
        return payload != null ? payload.keySet() : Collections.emptySet();
    }

    public boolean isPayloadFeatureValues() {
        return payloadFeatureValues;
    }

    public void setPayloadFeatureValues(boolean payloadFeatureValues) {
        this.payloadFeatureValues = payloadFeatureValues;
    }
}
