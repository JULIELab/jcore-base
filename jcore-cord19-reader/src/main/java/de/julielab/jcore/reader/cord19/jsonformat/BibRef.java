package de.julielab.jcore.reader.cord19.jsonformat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * This class corresponds to the JSON schema
 * <pre>
 *     {
 *                 "ref_id": <str>,
 *                 "title": <str>,
 *                 "authors": <list of dict>       # same structure as earlier,
 *                                                 # but without `affiliation` or `email`
 *                 "year": <int>,
 *                 "venue": <str>,
 *                 "volume": <str>,
 *                 "issn": <str>,
 *                 "pages": <str>,
 *                 "other_ids": {
 *                     "DOI": [
 *                         <str>
 *                     ]
 *                 }
 *             }
 * </pre>
 */
public class BibRef {
    @JsonProperty("ref_id")
    private String refId;
    private String title;
    private List<Author> authors;
    private int year;
    private String venue;
    private String volume;
    private String issn;
    private String pages;
    @JsonProperty("other_ids")
    private Map<String, Object> otherIds;

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public Map<String, Object> getOtherIds() {
        return otherIds;
    }

    public void setOtherIds(Map<String, Object> otherIds) {
        this.otherIds = otherIds;
    }
}
