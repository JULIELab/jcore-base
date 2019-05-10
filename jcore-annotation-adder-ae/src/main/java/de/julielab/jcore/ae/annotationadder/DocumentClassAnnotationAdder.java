package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalDocumentClassAnnotation;
import de.julielab.jcore.types.AutoDescriptor;
import de.julielab.jcore.types.DocumentClass;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentClassAnnotationAdder implements AnnotationAdder {

    private final static Logger log = LoggerFactory.getLogger(DocumentClassAnnotationAdder.class);

    @Override
    public boolean addAnnotations(AnnotationData data, AnnotationAdderHelper helper, AnnotationAdderConfiguration configuration, JCas jCas) {
        AnnotationList<ExternalDocumentClassAnnotation> annotationList;
        try {
            annotationList = (AnnotationList<ExternalDocumentClassAnnotation>) data;
            if (!annotationList.isEmpty()) {
                ExternalDocumentClassAnnotation ignored = annotationList.get(0);
            }
        } catch (ClassCastException e) {
            log.debug("AnnotationList adder rejected annotation data of class {}", data.getClass().getCanonicalName());
            return false;
        }
        for (ExternalDocumentClassAnnotation a : annotationList) {
            AutoDescriptor ad;
            try {
                ad = JCasUtil.selectSingle(jCas, AutoDescriptor.class);
            } catch (IllegalArgumentException e) {
                ad = new AutoDescriptor(jCas);
                ad.addToIndexes();
            }
            DocumentClass documentClass = new DocumentClass(jCas);
            documentClass.setClassname(a.getDocumentClass());
            documentClass.setConfidence(a.getConfidence());
            documentClass.setComponentId(a.getComponentId());
            FSArray newArray = JCoReTools.addToFSArray(ad.getDocumentClasses(), documentClass, 1);
            ad.setDocumentClasses(newArray);
        }
        return true;
    }
}
