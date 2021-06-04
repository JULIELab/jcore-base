//package de.julielab.jcore.flow.annotationdefined;
//
//import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.analysis_engine.metadata.FixedFlow;
//import org.apache.uima.analysis_engine.metadata.FlowConstraints;
//import org.apache.uima.flow.*;
//import org.apache.uima.jcas.JCas;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Map;
//
///**
// * <p>Note: This flow can only be used in an aggregate analysis engine where the {@link de.julielab.jcore.reader.xmi.XmiDBMultiplier} is the first component.</p>
// * <p>This flow is created by the {@link HashComparisonFlowController} and routes the CAS that was filled by the {@link de.julielab.jcore.reader.xmi.XmiDBMultiplierReader}.
// * This CAS contains an instance of {@link de.julielab.jcore.types.casmultiplier.RowBatch} which contains the information which documents should be read
// * from which database table.</p>
// * <p>Within this flow, the reader CAS is passed to the multiplier, the first component. For CASes created by the multiplier,
// * the method {@link #newCasProduced(JCas, String)} is called for which a new flow concerning the processing order of the
// * multiplier-created CASes within the aggregate is determined.</p>
// */
//public class HashComparisonOuterFlow extends JCasFlow_ImplBase {
//    private final static Logger log = LoggerFactory.getLogger(HashComparisonOuterFlow.class);
//    private String[] fixedFlow;
//    private int currentPosition;
//    private Map<String, String> id2hash;
//    private String documentItemToHash;
//
//    public HashComparisonOuterFlow(Map<String, String> id2hash, String documentItemToHash, FlowConstraints flowConstraints) throws AnalysisEngineProcessException {
//        this.id2hash = id2hash;
//        this.documentItemToHash = documentItemToHash;
//        if (!(flowConstraints instanceof FixedFlow)) {
//            throw new AnalysisEngineProcessException(new IllegalArgumentException("This flow requires the original FixedFlow to know the order of the delegate engines but the given flow is of type " + flowConstraints.getClass()));
//        }
//        FixedFlow fixedFlow = (FixedFlow) flowConstraints;
//        this.fixedFlow = fixedFlow.getFixedFlow();
//        this.currentPosition = 0;
//    }
//
//    @Override
//    protected Flow newCasProduced(JCas newCas, String producedBy) throws AnalysisEngineProcessException {
//        String newHash = getHash(newCas);
//        return new FixedInnerFlow(fixedFlow);
//    }
//
//    private String getHash(JCas newCas) {
//        final String documentText = newCas.getDocumentText();
//        final byte[] sha = DigestUtils.sha256(documentText.getBytes());
//        return Base64.encodeBase64String(sha);
//    }
//
//    public Step next() {
//        Step step = null;
//        for (; currentPosition < fixedFlow.length && step == null; currentPosition++) {
//            String aeKey = fixedFlow[currentPosition];
//
//            // The outer flow only passes the CAS to the CAS multiplier. The multiplier creates more CASes which
//            // are then passed to newCasProduced() and are then routed by the InnerFlow.
//            if (currentPosition == 0) {
//                log.trace("Outer next AE is: " + aeKey);
//                step = new SimpleStep(aeKey);
//            }
//        }
//        if (step == null) {
//            // no appropriate AEs to call - end of flow
//            log.trace("Outer flow Complete.");
//        }
//        return step == null ? new FinalStep() : step;
//    }
//}
