package de.julielab.jcore.flow.annotationdefined;

import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.casflow.ToVisit;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
public class AnnotationDefinedFlowControllerTest {
    @Test
    public void testFlowControllerSingleKey() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-casflow-types");
        ToVisit toVisit = new ToVisit(jCas);
        StringArray toVisitKeys = new StringArray(jCas, 1);
        toVisitKeys.set(0, "TestAE 2");
        toVisit.setDelegateKeys(toVisitKeys);
        toVisit.addToIndexes();

        AnalysisEngine aae = createTestAAE();

        aae.process(jCas);

        FSIterator<Token> it = jCas.<Token>getAnnotationIndex(Token.type).iterator();
        assertThat(it).toIterable().extracting(Token::getComponentId).containsExactly("TestAE 2");
    }

    @Test
    public void testFlowControllerNoKey() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-casflow-types");
        ToVisit toVisit = new ToVisit(jCas);
        StringArray toVisitKeys = new StringArray(jCas, 0);
        toVisit.setDelegateKeys(toVisitKeys);
        toVisit.addToIndexes();

        AnalysisEngine aae = createTestAAE();

        aae.process(jCas);

        FSIterator<Token> it = jCas.<Token>getAnnotationIndex(Token.type).iterator();
        assertThat(it).isExhausted();
    }

    @Test
    public void testFlowControllerNullKey() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-casflow-types");
        ToVisit toVisit = new ToVisit(jCas);
        toVisit.addToIndexes();

        AnalysisEngine aae = createTestAAE();

        aae.process(jCas);

        FSIterator<Token> it = jCas.<Token>getAnnotationIndex(Token.type).iterator();
        assertThat(it).isExhausted();
    }

    @Test
    public void testFlowControllerNoVisitAnnotation() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-casflow-types");

        AnalysisEngine aae = createTestAAE();

        aae.process(jCas);

        FSIterator<Token> it = jCas.<Token>getAnnotationIndex(Token.type).iterator();
        assertThat(it).toIterable().extracting(Token::getComponentId).containsExactly("TestAE 1", "TestAE 2", "TestAE 3");
    }

    private AnalysisEngine createTestAAE() throws ResourceInitializationException {
        FlowControllerDescription flowControllerDescription = FlowControllerFactory.createFlowControllerDescription(AnnotationDefinedFlowController.class);
        AnalysisEngineDescription testAeDesc1 = AnalysisEngineFactory.createEngineDescription(TestAE.class, "name", "TestAE 1");
        AnalysisEngineDescription testAeDesc2 = AnalysisEngineFactory.createEngineDescription(TestAE.class, "name", "TestAE 2");
        AnalysisEngineDescription testAeDesc3 = AnalysisEngineFactory.createEngineDescription(TestAE.class, "name", "TestAE 3");
        AnalysisEngineDescription aaeWithFlowController = AnalysisEngineFactory.createEngineDescription(asList(testAeDesc1, testAeDesc2, testAeDesc3), asList("TestAE 1", "TestAE 2", "TestAE 3"), null, null,
                flowControllerDescription);
        AnalysisEngine aae = AnalysisEngineFactory.createEngine(aaeWithFlowController);
        return aae;
    }

    public static class TestAE extends JCasAnnotator_ImplBase {
        @ConfigurationParameter(name = "name")
        private String name;

        @Override
        public void initialize(UimaContext context) {
            name = (String) context.getConfigParameterValue("name");
        }

        @Override
        public void process(JCas jCas) {
            // Indicate that this jCas was processed by this component.
            Token token = new Token(jCas);
            token.setComponentId(name);
            token.addToIndexes();
        }
    }
}
