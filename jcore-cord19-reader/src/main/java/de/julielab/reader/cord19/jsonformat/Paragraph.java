package de.julielab.reader.cord19.jsonformat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class corresponds to the JSON schema
 *
 * <pre>
 *      {
 *                 "text": <str>,
 *                 "cite_spans": [             # list of character indices of inline citations
 *                                             # e.g. citation "[7]" occurs at positions 151-154 in "text"
 *                                             #      linked to bibliography entry BIBREF3
 *                     {
 *                         "start": 151,
 *                         "end": 154,
 *                         "text": "[7]",
 *                         "ref_id": "BIBREF3"
 *                     },
 *                     ...
 *                 ],
 *                 "ref_spans": <list of dicts similar to cite_spans>,     # e.g. inline reference to "Table 1"
 *                 "eq_spans": [],
 *                 "section": "Abstract"
 *        }
 * </pre>
 */
public class Paragraph {
    private String text;
    @JsonProperty("cite_spans")
    private List<CiteSpan> citeSpans;
    @JsonProperty("ref_spans")
    private List<CiteSpan> refSpans;

    public List<CiteSpan> getEqSpans() {
        return eqSpans;
    }

    public void setEqSpans(List<CiteSpan> eqSpans) {
        this.eqSpans = eqSpans;
    }

    @JsonProperty("eq_spans")
    private List<CiteSpan> eqSpans;
    private String section;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<CiteSpan> getCiteSpans() {
        return citeSpans;
    }

    public void setCiteSpans(List<CiteSpan> citeSpans) {
        this.citeSpans = citeSpans;
    }

    public List<CiteSpan> getRefSpans() {
        return refSpans;
    }

    public void setRefSpans(List<CiteSpan> refSpans) {
        this.refSpans = refSpans;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
