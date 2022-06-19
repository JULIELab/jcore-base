package de.julielab.jcore.ae.annotationadder.annotationsources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryFileTextAnnotationProvider extends TextAnnotationProvider {
    private final static Logger log = LoggerFactory.getLogger(InMemoryFileTextAnnotationProvider.class);
    @Override
    void initializeAnnotationSource() {
        annotationSource = new InMemoryAnnotationSource<>(format);
    }

    @Override
    Logger getLogger() {
        return log;
    }
}
