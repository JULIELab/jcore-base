package de.julielab.jcore.ae.annotationadder.annotationsources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2TextAnnotationProvider extends TextAnnotationProvider {
    private final static Logger log = LoggerFactory.getLogger(H2TextAnnotationProvider.class);
    @Override
    void initializeAnnotationSource() {
        annotationSource = new H2AnnotationSource<>(format);
    }

    @Override
    Logger getLogger() {
        return log;
    }
}
