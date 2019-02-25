package de.julielab.jcore.ae.annotationadder;

public class AnnotationAdderConfiguration {
    private AnnotationAdderAnnotator.OffsetMode offsetMode;

    public String getDefaultUimaType() {
        return defaultUimaType;
    }

    public void setDefaultUimaType(String defaultUimaType) {
        this.defaultUimaType = defaultUimaType;
    }

    private String defaultUimaType;

    public AnnotationAdderAnnotator.OffsetMode getOffsetMode() {
        return offsetMode;
    }

    public void setOffsetMode(AnnotationAdderAnnotator.OffsetMode offsetMode) {
        this.offsetMode = offsetMode;
    }
}
