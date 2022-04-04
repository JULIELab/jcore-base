package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCAnnotation;
import com.pengyifan.bioc.BioCDocument;
import com.pengyifan.bioc.BioCLocation;
import com.pengyifan.bioc.BioCPassage;
import de.julielab.jcore.types.*;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts text passages from the CAS and adds them to a new BioCDocument.
 */
public class BioCDocumentPopulator {
    private final static Logger log = LoggerFactory.getLogger(BioCDocumentPopulator.class);
    private boolean addGenes;

    public BioCDocumentPopulator(boolean addGenes) {
        this.addGenes = addGenes;
    }

    public BioCDocument populate(JCas jCas) {
        BioCDocument doc = new BioCDocument(JCoReTools.getDocId(jCas));
        AnnotationIndex<Zone> zoneIndex = jCas.getAnnotationIndex(Zone.type);
        int annotationId = 0;
        for (Zone z : zoneIndex) {
            if (z.getEnd() - z.getBegin() <= 0)
                continue;
            BioCPassage p = null;
            if (z instanceof Title) {
                Title t = (Title) z;
                String titleType;
                String titleTypeString = t.getTitleType();
                if (titleTypeString == null)
                    titleTypeString = "other";
                switch (titleTypeString) {
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
                        log.debug("Unhandled title type {}", titleTypeString);
                        titleType = "other_title";
                        break;
                }
                if (titleType != null) {
                    p = getPassageForAnnotation(t);
                    p.putInfon("type", titleType);
                    doc.addPassage(p);
                }
            } else if (z instanceof AbstractText) {
                AbstractText at = (AbstractText) z;
                p = getPassageForAnnotation(at);
                p.putInfon("type", "abstract");
                doc.addPassage(p);
            } else if (z instanceof Paragraph) {
                Paragraph pa = (Paragraph) z;
                p = getPassageForAnnotation(pa);
                p.putInfon("type", "paragraph");
                doc.addPassage(p);
            } else if (z instanceof Caption) {
                Caption c = (Caption) z;
                p = getPassageForAnnotation(c);
                if (c.getCaptionType() == null)
                    throw new IllegalArgumentException("The captionType feature is null for " + c);
                p.putInfon("type", c.getCaptionType());
                doc.addPassage(p);
            }
            if (addGenes) {
                annotationId = addGenesToPassage(jCas, z, p, annotationId);
            }
        }
        return doc;
    }

    private int addGenesToPassage(JCas jCas, Zone z, BioCPassage p, int annotationId) {
        if (p != null) {
            Iterable<Gene> geneIt = JCasUtil.subiterate(jCas, Gene.class, z, false, true);
            for (Gene g : geneIt) {
                BioCAnnotation annotation = new BioCAnnotation(String.valueOf(annotationId++));
                annotation.setText(g.getCoveredText());
                String type = "Gene";
                String specificType = g.getSpecificType() != null ? g.getSpecificType().toLowerCase() : null;
                // 'familiy' is an entity name typo in the ProGene corpus
                if (specificType != null && (specificType.contains("familiy") || specificType.contains("family") || specificType.contains("complex")))
                    type = "FamilyName";
                else if (specificType != null && specificType.contains("domain"))
                    type = "DomainMotif";
                annotation.putInfon("type", type);
                annotation.addLocation(new BioCLocation(g.getBegin(), g.getEnd() - g.getBegin()));
                p.addAnnotation(annotation);
            }
        }
        return annotationId;
    }

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
