package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.jcore.ae.annotationadder.annotationformat.SimpleTSVEntityAnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

public class InMemoryFileEntityProvider implements SharedResourceObject, AnnotationProvider<AnnotationList> {
    private AnnotationSource<AnnotationList> annotationSource;

    @Override
    public AnnotationList getAnnotations(String id) {
        return annotationSource.getAnnotations(id);
    }

    @Override
    public void load(DataResource dataResource) throws ResourceInitializationException {
        // This logic could be made configurable if required so in the future.
        annotationSource = new FileEntityAnnotationSource(new SimpleTSVEntityAnnotationFormat());
    }
}
