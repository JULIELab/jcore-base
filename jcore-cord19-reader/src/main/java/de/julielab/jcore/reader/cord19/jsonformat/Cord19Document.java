package de.julielab.jcore.reader.cord19.jsonformat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class to reflect documents that fit the JSON schema at https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/2020-03-13/json_schema.txt:
 *
 * <pre>
 *       {
 *         "paper_id": <str>,                      # 40-character sha1 of the PDF
 *         "metadata": {
 *         "title": <str>,
 *                 "authors": [                        # list of author dicts, in order
 *         {
 *             "first": <str>,
 *                 "middle": <list of str>,
 *                 "last": <str>,
 *                 "suffix": <str>,
 *                 "affiliation": <dict>,
 *                 "email": <str>
 *         },
 *             ...
 *         ],
 *         "abstract": [                       # list of paragraphs in the abstract
 *         {
 *             "text": <str>,
 *                 "cite_spans": [             # list of character indices of inline citations
 *                                             # e.g. citation "[7]" occurs at positions 151-154 in "text"
 *                                             #      linked to bibliography entry BIBREF3
 *             {
 *                 "start": 151,
 *                     "end": 154,
 *                     "text": "[7]",
 *                     "ref_id": "BIBREF3"
 *             },
 *                     ...
 *                 ],
 *             "ref_spans": <list of dicts similar to cite_spans>,     # e.g. inline reference to "Table 1"
 *             "section": "Abstract"
 *         },
 *             ...
 *         ],
 *         "body_text": [                      # list of paragraphs in full body
 *                                             # paragraph dicts look the same as above
 *         {
 *             "text": <str>,
 *                 "cite_spans": [],
 *             "ref_spans": [],
 *             "eq_spans": [],
 *             "section": "Introduction"
 *         },
 *             ...
 *         {
 *                 ...,
 *             "section": "Conclusion"
 *         }
 *         ],
 *         "bib_entries": {
 *             "BIBREF0": {
 *                 "ref_id": <str>,
 *                         "title": <str>,
 *                         "authors": <list of dict>       # same structure as earlier,
 *                                                 # but without `affiliation` or `email`
 *                 "year": <int>,
 *                 "venue": <str>,
 *                         "volume": <str>,
 *                         "issn": <str>,
 *                         "pages": <str>,
 *                         "other_ids": {
 *                     "DOI": [
 *                         <str>
 *                     ]
 *                 }
 *             },
 *             "BIBREF1": {},
 *             ...
 *             "BIBREF25": {}
 *         },
 *         "ref_entries":
 *         "FIGREF0": {
 *             "text": <str>,                  # figure caption text
 *             "type": "figure"
 *         },
 *             ...
 *         "TABREF13": {
 *             "text": <str>,                  # table caption text
 *             "type": "table"
 *         }
 *     },
 *         "back_matter": <list of dict>           # same structure as body_text
 *     }
 * }
 * </pre>
 */
public class Cord19Document {
    @JsonProperty("paper_id")
    private String paperId;
    private MetaData metadata;
    @JsonProperty("abstract")
    private List<Paragraph> abstr;
    @JsonProperty("body_text")
    private List<Paragraph> body;
    @JsonProperty("bib_entries")
    private Map<String, BibRef> bibEntries;
    @JsonProperty("ref_entries")
    private Map<String, TabFigRef> refEntries;

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData metadata) {
        this.metadata = metadata;
    }

    public List<Paragraph> getAbstr() {
        return abstr != null ? abstr : Collections.emptyList();
    }

    public void setAbstr(List<Paragraph> abstr) {
        this.abstr = abstr;
    }

    public List<Paragraph> getBody() {
        return body != null ? body : Collections.emptyList();
    }

    public void setBody(List<Paragraph> body) {
        this.body = body;
    }

    public Map<String, BibRef> getBibEntries() {
        return bibEntries;
    }

    public void setBibEntries(Map<String, BibRef> bibEntries) {
        this.bibEntries = bibEntries;
    }

    public Map<String, TabFigRef> getRefEntries() {
        return refEntries;
    }

    public void setRefEntries(Map<String, TabFigRef> refEntries) {
        this.refEntries = refEntries;
    }

    public List<Paragraph> getBackMatter() {
        return backMatter;
    }

    public void setBackMatter(List<Paragraph> backMatter) {
        this.backMatter = backMatter;
    }

    @JsonProperty("back_matter")
    private List<Paragraph> backMatter;
}
