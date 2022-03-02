package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCDocument;
import com.pengyifan.bioc.BioCPassage;
import de.julielab.jcore.types.*;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts text passages from the CAS and adds them to a new BioCDocument.
 */
public class BioCDocumentPopulator {
    private final static Logger log = LoggerFactory.getLogger(BioCDocumentPopulator.class);

    public BioCDocument populate(JCas jCas) {
        BioCDocument doc = new BioCDocument(JCoReTools.getDocId(jCas));
        AnnotationIndex<Zone> zoneIndex = jCas.getAnnotationIndex(Zone.type);
        for (Zone z : zoneIndex) {
            if (z.getEnd() - z.getBegin() <= 0)
                continue;
            if (z instanceof Title) {
                Title t = (Title) z;
                String titleType;
                if (t.getTitleType() == null)
                    throw new IllegalArgumentException("The titleType feature was not set for " + t);
                switch (t.getTitleType()) {
                    case "document":
                        titleType = "title";
                        break;
                    case "section":
                        titleType = "section_title";
                        break;
                    case "figure":
                        titleType = "figure_title";
                        break;
                    case "table":
                        titleType = "table_title";
                        break;
                    case "abstractSection":
                        // abstract sections are part of the AbstractText which is handled below
                        titleType = "null";
                        break;
                    default:
                        log.debug("Unhandled title type {}", t.getTitleType());
                        titleType = "other_title";
                        break;
                }
                if (titleType != null) {
                    BioCPassage p = getPassageForAnnotation(t);
                    p.putInfon("type", titleType);
                    doc.addPassage(p);
                }
            } else if (z instanceof AbstractText) {
                AbstractText at = (AbstractText) z;
                BioCPassage p = getPassageForAnnotation(at);
                p.putInfon("type", "abstract");
                doc.addPassage(p);
            } else if (z instanceof Paragraph) {
                Paragraph pa = (Paragraph) z;
                BioCPassage p = getPassageForAnnotation(pa);
                p.putInfon("type", "paragraph");
                doc.addPassage(p);
            } else if (z instanceof Caption) {
                Caption c = (Caption) z;
                BioCPassage p = getPassageForAnnotation(c);
                if (c.getCaptionType() == null)
                    throw new IllegalArgumentException("The captionType feature is null for " + c);
                p.putInfon("type", c.getCaptionType());
                doc.addPassage(p);
            }
        }
        return doc;
    }

//    private BioCPassage getPassageForAbstract(AbstractText at) {
//        FSArray structuredAbstractParts = at.getStructuredAbstractParts();
//        boolean foundAbstractParts = false;
//        if (structuredAbstractParts != null) {
//            for (int i = 0; i < structuredAbstractParts.size(); ++i) {
//                AbstractSection as = (AbstractSection) structuredAbstractParts.get(i);
//
//            }
//        }
//        return null;
//    }

    /**
     * Creates a BioCPassage with offset and text corresponding to the passed annotation <tt>a</tt>.
     *
     * @param a The annotation to create a BioCPassage for.
     * @return A BioCPassage corresponding to <tt>a</tt> in offset and text.
     */
    private BioCPassage getPassageForAnnotation(Annotation a) {
        BioCPassage p = new BioCPassage();
        p.setOffset(a.getBegin());
        // GNormPlus doesn't seem to handle newlines well. It resulted in missing annotations when testing if the
        // output format is handled well by GNormPlus.
        p.setText(a.getCoveredText().replaceAll("\n", " "));
        return p;
    }
}
