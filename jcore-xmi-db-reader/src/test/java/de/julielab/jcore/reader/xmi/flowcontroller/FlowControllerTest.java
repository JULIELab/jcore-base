package de.julielab.jcore.reader.xmi.flowcontroller;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FlowControllerTest {
    @Test
    public void testFlowController() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        RowBatch rowBatch = new RowBatch(jCas);
        for (int i = 0; i < 10; i++) {
            StringArray id = new StringArray(jCas, 1);
            id.set(0, String.valueOf(i));
            rowBatch.setIdentifiers(JCoReTools.addToFSArray(rowBatch.getIdentifiers(), id));
        }
        rowBatch.addToIndexes();

        FlowControllerDescription flowControllerDescription = FlowControllerFactory.createFlowControllerDescription(HashComparisonFlowController.class);
        AnalysisEngineDescription multiplierDesc = AnalysisEngineFactory.createEngineDescription(TestMultiplier.class);
        AnalysisEngineDescription testAeDesc1 = AnalysisEngineFactory.createEngineDescription(TestAE.class, "name", "TestAE 1");
        AnalysisEngineDescription testAeDesc2 = AnalysisEngineFactory.createEngineDescription(TestAE.class, "name", "TestAE 2");
        AnalysisEngineDescription aaeWithFlowController = AnalysisEngineFactory.createEngineDescription(flowControllerDescription, multiplierDesc, testAeDesc1, testAeDesc2);
        AnalysisEngine aae = AnalysisEngineFactory.createEngine(aaeWithFlowController);

        aae.process(jCas);
    }

    public static class TestAE extends JCasAnnotator_ImplBase {
        private final static Logger log = LoggerFactory.getLogger(TestAE.class);

        @ConfigurationParameter(name = "name")
        private String name;

        @Override
        public void initialize(UimaContext context) throws ResourceInitializationException {
            name = (String) context.getConfigParameterValue("name");
        }

        @Override
        public void process(JCas jCas) throws AnalysisEngineProcessException {
            log.debug("Running AE: {}", name);
            log.debug("JCas text: " + jCas.getDocumentText());
        }
    }

    public static class TestMultiplier extends JCasMultiplier_ImplBase {
        private List<String> idsToRead = new ArrayList<>();
        private int currentIndex;
        @Override
        public void process(JCas jCas) throws AnalysisEngineProcessException {
            RowBatch rowbatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            idsToRead.clear();
            currentIndex = 0;
            for (int i = 0; i < rowbatch.getIdentifiers().size() && rowbatch.getIdentifiers(i) != null; i++) {
                // In this test, the document IDs consist only of a single element
                idsToRead.add(rowbatch.getIdentifiers(i).get(0));
            }
        }

        @Override
        public boolean hasNext() throws AnalysisEngineProcessException {
            return currentIndex < idsToRead.size();
        }

        @Override
        public AbstractCas next() throws AnalysisEngineProcessException {
            JCas emptyJCas = getEmptyJCas();
            Header header = new Header(emptyJCas);
            String docId = idsToRead.get(currentIndex);
            header.setDocId(docId);
            header.addToIndexes();
            emptyJCas.setDocumentText("ID: " + docId);
            ++currentIndex;
            return emptyJCas;
        }
    }

}
