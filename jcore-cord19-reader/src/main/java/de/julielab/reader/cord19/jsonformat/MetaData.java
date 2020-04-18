package de.julielab.reader.cord19.jsonformat;

import java.util.List;

/**
 * A class corresponding to this JSON schema:
 *
 * <pre>
 *     "metadata": {
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
 * </pre>
 */
public class MetaData {
    private String title;
    private List<Author> authors;

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
}
