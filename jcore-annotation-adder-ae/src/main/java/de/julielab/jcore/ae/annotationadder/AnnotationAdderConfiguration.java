package de.julielab.jcore.ae.annotationadder;

public class AnnotationAdderConfiguration {
    private AnnotationAdderAnnotator.OffsetMode offsetMode;
    private String defaultUimaType;
    private boolean splitTokensAtWhitespace;

    public boolean isSplitTokensAtWhitespace() {
        return splitTokensAtWhitespace;
    }

    /**
     * <p>Useful for results of external programs that interpret all whitespaces as token boundaries.
     * Sets whether or not to split the CAS tokens at whitespaces for the sake of token-offset calculation. This does not
     * mean that the tokenization in the CAS is changed. It is just a measure to adapt to external annotation results
     * that work with token offsets</p>
     * @param splitTokensAtWhitespace
     */
    public void setSplitTokensAtWhitespace(boolean splitTokensAtWhitespace) {
        this.splitTokensAtWhitespace = splitTokensAtWhitespace;
    }

    public String getDefaultUimaType() {
        return defaultUimaType;
    }

    public void setDefaultUimaType(String defaultUimaType) {
        this.defaultUimaType = defaultUimaType;
    }

    public AnnotationAdderAnnotator.OffsetMode getOffsetMode() {
        return offsetMode;
    }

    public void setOffsetMode(AnnotationAdderAnnotator.OffsetMode offsetMode) {
        this.offsetMode = offsetMode;
    }
}
