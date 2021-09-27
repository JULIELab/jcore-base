package de.julielab.jcore.ae.annotationadder;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class TextAnnotationListAdder implements AnnotationAdder {
    private final static Logger log = LoggerFactory.getLogger(TextAnnotationListAdder.class);

    @Override
    public boolean addAnnotations(AnnotationData data, AnnotationAdderHelper helper, AnnotationAdderConfiguration configuration, JCas jCas, boolean preventProcessedOnDigestMismatch) {
        AnnotationList<ExternalTextAnnotation> annotationList;
        try {
            annotationList = (AnnotationList<ExternalTextAnnotation>) data;
            if (!annotationList.isEmpty()) {
                // Try to provoke a ClassCastException to make sure we are handling the right data.
                ExternalTextAnnotation ignored = annotationList.get(0);
            }
        } catch (ClassCastException e) {
            log.debug("AnnotationList adder rejected annotation data of class {}", data.getClass().getCanonicalName());
            return false;
        }
        String jCasDocTextSha = null;
        boolean shaMismatchWasReported = false;
        for (ExternalTextAnnotation a : annotationList) {
            String uimaType;
            if (a.getUimaType() != null && jCas.getTypeSystem().getType(a.getUimaType()) != null)
                uimaType = a.getUimaType();
            else if (configuration.getDefaultUimaType() != null)
                uimaType = configuration.getDefaultUimaType();
            else
                throw new IllegalArgumentException("Missing annotation type: Neither the annotation of document " + a.getDocumentId() + " with offsets " + a.getStart() + "-" + a.getEnd() + " provides a type nor is the default type set.");
            if (jCas.getTypeSystem().getType(uimaType) == null)
                throw new IllegalArgumentException("The entity annotation type " + uimaType + " does not exist in the type system.");
            try {
                // The sha check is supposed to compare the document text on which the annotation was made with the
                // document text the current CAS has. If they differ, the annotations will most likely have
                // offset discrepancies which is why they won't be added and a warning will be issued.
                final String shaFromAnnotation = (String) a.getPayload("sha");
                boolean shaMatches = true;
                if (shaFromAnnotation != null) {
                    if (jCasDocTextSha == null) {
                        final byte[] bytes = DigestUtils.sha256(jCas.getDocumentText());
                        jCasDocTextSha = Base64.encodeBase64String(bytes);
                    }
                    shaMatches = jCasDocTextSha.equals(shaFromAnnotation);
                }
                if (shaMatches) {
                    // ExternalAnnotations with offsets < 0 are not actual mentions but non-entity lines just to check
                    // that the SHA was the same as it was at time of the original entity tagging.
                    if (a.getStart() >= 0) {
                        final Annotation annotation = JCoReAnnotationTools.getAnnotationByClassName(jCas, uimaType);
                        helper.setAnnotationOffsetsRelativeToDocument(annotation, a, configuration);
                        helper.setAnnotationPayloadsToFeatures(annotation, a);
                        log.trace("Adding annotation of type {} with offsets {}-{} to document with ID {}", uimaType, annotation.getBegin(), annotation.getEnd(), annotationList.getDocId());
                        annotation.addToIndexes();
                    } else {
                        log.trace("ExternalAnnotation for document {} has no entity offsets or offsets < 0, not adding anything to the CAS.", a.getDocumentId());
                    }
                } else {
                    if (!shaMismatchWasReported) {
                        log.warn("The document with ID '{}' has a differing document text hash from a given annotation. The annotation will not be added to the document. Annotation hash: {}, current document text hash: {}", annotationList.getDocId(), shaFromAnnotation, jCasDocTextSha);
                        shaMismatchWasReported = true;
                        if (preventProcessedOnDigestMismatch) {
                            try {
                                final DBProcessingMetaData dbProcessingMetaData = JCasUtil.selectSingle(jCas, DBProcessingMetaData.class);
                                dbProcessingMetaData.setDoNotMarkAsProcessed(true);
                            } catch (IllegalArgumentException e) {
                                log.error("Could not acquire the DBProcessingMetaData to exclude the hash-mismatched document from being marked as being processed in the JeDIS database. This annotation should have been set by the jcore-(xmi)-db-reader. Does this pipeline read from a JeDIS subset table?");
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | CASException | AnnotationOffsetException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
