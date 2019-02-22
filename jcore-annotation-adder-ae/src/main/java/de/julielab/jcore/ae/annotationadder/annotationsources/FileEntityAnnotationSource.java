package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.ae.annotationadder.annotationformat.AnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalAnnotation;
import org.apache.uima.resource.DataResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class FileEntityAnnotationSource implements AnnotationSource<AnnotationList> {
    private final static Logger log = LoggerFactory.getLogger(FileEntityAnnotationSource.class);
    private AnnotationFormat<ExternalAnnotation> format;
    private Map<String, AnnotationList> entitiesByDocId;

    public FileEntityAnnotationSource(AnnotationFormat<ExternalAnnotation> format) {
        this.format = format;
    }

    public void loadAnnotations(File annotationfile) {
        try (BufferedReader br = FileUtilities.getReaderFromFile(annotationfile)) {
            entitiesByDocId = br.lines().map(format::parse).collect(Collectors.groupingBy(ExternalAnnotation::getDocumentId, Collectors.toCollection(AnnotationList::new)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(DataResource dataResource) {
        log.info("Loading entity annotations from {}", dataResource.getUri());
        loadAnnotations(new File(dataResource.getUri()));
    }

    @Override
    public AnnotationList getAnnotations(String id) {
        return entitiesByDocId.get(id);
    }
}
