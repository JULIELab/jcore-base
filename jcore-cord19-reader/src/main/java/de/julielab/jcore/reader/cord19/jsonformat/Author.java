package de.julielab.jcore.reader.cord19.jsonformat;

import java.util.List;

/**
 * A class corresponding to the JSON schema
 *
 * <pre>
 *      {
 *              "first": <str>,
 *                  "middle": <list of str>,
 *                  "last": <str>,
 *                  "suffix": <str>,
 *                  "affiliation": <dict>,
 *                  "email": <str>
 *      }
 * </pre>
 */
public class Author {
    private String first;
    private List<String> middle;
    private String last;
    private String suffix;
    private Affiliation affiliation;
    private String email;

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public List<String> getMiddle() {
        return middle;
    }

    public void setMiddle(List<String> middle) {
        this.middle = middle;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Affiliation getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(Affiliation affiliation) {
        this.affiliation = affiliation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
