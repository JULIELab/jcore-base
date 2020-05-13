package de.julielab.jcore.ae.annotationadder;

public class AnnotationOffsetException extends Exception {
    public AnnotationOffsetException() {
    }

    public AnnotationOffsetException(String message) {
        super(message);
    }

    public AnnotationOffsetException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnnotationOffsetException(Throwable cause) {
        super(cause);
    }

    public AnnotationOffsetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
