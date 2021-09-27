package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.java.utilities.UriUtilities;
import de.julielab.jcore.ae.annotationadder.annotationformat.AnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import org.apache.uima.resource.DataResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileAnnotationSource<T extends AnnotationData> implements AnnotationSource<AnnotationList<T>> {
    private final static Logger log = LoggerFactory.getLogger(FileAnnotationSource.class);
    private AnnotationFormat<T> format;
    private Map<String, AnnotationList<T>> entitiesByDocId;

    public FileAnnotationSource(AnnotationFormat<T> format) {
        this.format = format;
    }

    @Override
    public void loadAnnotations(URI annotationUri) throws IOException {
        try (BufferedReader br = UriUtilities.getReaderFromUri(annotationUri)) {
            entitiesByDocId = br.lines().map(format::parse).filter(Objects::nonNull).collect(Collectors.groupingBy(AnnotationData::getDocumentId, Collectors.toCollection(AnnotationList::new)));
        }
        if (log.isTraceEnabled())
            log.trace("Loaded {} entity annotations for {} document IDs.", entitiesByDocId.values().stream().flatMap(AnnotationList::stream).count(), entitiesByDocId.size());
    }

    @Override
    public void initialize(DataResource dataResource) throws IOException {
        log.info("Loading entity annotations from {}", dataResource.getUri());
        loadAnnotations(dataResource.getUri());
    }

    @Override
    public AnnotationList<T> getAnnotations(String id) {
        return entitiesByDocId.get(id);
    }
}
