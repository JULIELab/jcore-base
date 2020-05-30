package de.julielab.jcore.reader.cord19.jsonformat;

/**
 * This class represent JSON objects in the following form:
 *
 * <pre>
 *     "FIGREF0": {
 *                 "text": <str>,                  # figure caption text
 *                 "type": "figure"
 *             },
 *             ...
 *             "TABREF13": {
 *                 "text": <str>,                  # table caption text
 *                 "type": "table"
 *             }
 * </pre>
 */
public class TabFigRef {
    private String text;
    private String type;
    private String latex;
    private String html;

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getLatex() {
        return latex;
    }

    public void setLatex(String latex) {
        this.latex = latex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
