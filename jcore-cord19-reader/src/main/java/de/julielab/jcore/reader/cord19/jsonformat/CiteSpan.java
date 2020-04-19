package de.julielab.jcore.reader.cord19.jsonformat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class corresponds to the JSON schema
 *
 * <pre>
 *      {
 *          "start": 151,
 *          "end": 154,
 *          "text": "[7]",
 *          "ref_id": "BIBREF3"
 *      }
 * </pre>
 */
public class CiteSpan {
    private int start;
    private int end;
    private String text;
    @JsonProperty("ref_id")
    private String refId;

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }
}
