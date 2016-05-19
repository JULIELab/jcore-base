package de.julielab.jcore.reader.dta.util;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAUtils {
    public static boolean hasAnyClassification(final JCas jcas, final Class<?>... classes) {
        final FSIterator<Annotation> it = jcas.getAnnotationIndex(DocumentClassification.type).iterator();
        while (it.hasNext()) {
            final DocumentClassification classification = (DocumentClassification) it.next();
            for (final Class<?> c : classes)
                if (c.isInstance(classification))
                    return true;
        }
        return false;
    }
}
