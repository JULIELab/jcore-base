package de.julielab.jcore.ae.annotationadder.annotationrepresentations;

public class ExternalDocumentClassAnnotation implements AnnotationData {
    private String documentId;
    /**
     * What is the classes name?
     */
    private String documentClass;
    /**
     * Which component did assign this class?
     */
    private String componentId;
    /**
     * How sure was the assigning component of this class assignment?
     */
    private double confidence;

    public ExternalDocumentClassAnnotation(String documentId, String documentClass, double confidence, String componentId) {
        this.documentId = documentId;
        this.documentClass = documentClass;
        this.componentId = componentId;
        this.confidence = confidence;
    }

    public String getDocumentClass() {
        return documentClass;
    }

    public String getComponentId() {
        return componentId;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String getDocumentId() {
        return documentId;
    }

}
