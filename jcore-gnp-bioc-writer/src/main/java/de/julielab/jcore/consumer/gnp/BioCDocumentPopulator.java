package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCDocument;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.Zone;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;

/**
 * Extracts text passages from the CAS and adds them to a new BioCDocument.
 */
public class BioCDocumentPopulator {
    public BioCDocument populate(JCas jCas) {
        AnnotationIndex<Zone> zoneIndex = jCas.getAnnotationIndex(Zone.type);
        for (Zone z : zoneIndex) {
            if (z instanceof Title) {
                // only document title; other titles should be accessed via features of the zone body
            }
            else if (z instanceof AbstractText) {
                // don't check for structured parts; for GNormPlus the only important thing is title, abstract, body
            } else if (z instanceof Section) {
                // handle headings
            }
        }
        return null;
    }
}
