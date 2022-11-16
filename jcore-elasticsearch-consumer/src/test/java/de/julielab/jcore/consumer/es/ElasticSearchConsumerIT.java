package de.julielab.jcore.consumer.es;

import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class ElasticSearchConsumerIT {
    public static final String TEST_INDEX = "testindex";
    public static final String TEST_CLUSTER = "testcluster";
    private final static Logger log = LoggerFactory.getLogger(ElasticSearchConsumerIT.class);
    // in case we need to disable X-shield: https://stackoverflow.com/a/51172136/1314955
    @Container
    public static GenericContainer es = new GenericContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(9200)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withEnv("cluster.name", TEST_CLUSTER);

    @BeforeAll
    public static void setup() {
        Slf4jLogConsumer toStringConsumer = new Slf4jLogConsumer(log);
        es.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);
    }

    @Test
    public void testMinimal() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types");
        jCas.setDocumentText("Some text.");
        final Header header = new Header(jCas);
        header.setDocId("987");
        header.addToIndexes();
        final AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ElasticSearchConsumer.class,
                ElasticSearchConsumer.PARAM_INDEX_NAME, TEST_INDEX,
                ElasticSearchConsumer.PARAM_URLS, "http://localhost:" + es.getMappedPort(9200),
                ElasticSearchConsumer.PARAM_FIELD_GENERATORS, new String[]{"de.julielab.jcore.consumer.es.ElasticSearchConsumerIT$MinimalTestFieldGenerator"});
        consumer.process(jCas);
        consumer.collectionProcessComplete();
        Thread.sleep(4000);
        final URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_doc/987");
        final ObjectMapper om = new ObjectMapper();
        final Map<?, ?> map = om.readValue(url.openStream(), Map.class);
        assertEquals(jCas.getDocumentText(), ((Map) map.get("_source")).get("text"));
    }

    @Test
    public void testDeleteDocumentsBeforeIndexing() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types");
        final AnalysisEngine consumer = AnalysisEngineFactory.createEngine(ElasticSearchConsumer.class,
                ElasticSearchConsumer.PARAM_INDEX_NAME, TEST_INDEX,
                ElasticSearchConsumer.PARAM_URLS, "http://localhost:" + es.getMappedPort(9200),
                ElasticSearchConsumer.PARAM_FIELD_GENERATORS, new String[]{"de.julielab.jcore.consumer.es.ElasticSearchConsumerIT$TestFieldGenerator"});
        // The indexing code is put into a lambda so we don't have to repeat ourselves
        Runnable doIndex = () -> {
            try {
//                for (int j = 0; j < 2; ++j) {
                    for (int i = 0; i < 10; i++) {
                        jCas.setDocumentText("Some text.");
                        final Header header = new Header(jCas);
                        header.setDocId(String.valueOf(i));
                        header.addToIndexes();
                        consumer.process(jCas);
                        jCas.reset();
                    }
//                }
                consumer.collectionProcessComplete();
            } catch (AnalysisEngineProcessException e) {
                throw new RuntimeException(e);
            }
        };
        Supplier<Integer> getNumDocuments = () -> {
            try {
                Thread.sleep(3000);
                final URL countUrl = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_count");
                final HttpURLConnection urlConnection = (HttpURLConnection) countUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()))) {
                    bw.write("{\"query\":{\"match_all\":{}}}");
                }
                final String response = IOStreamUtilities.getStringFromInputStream(urlConnection.getInputStream());
                final Matcher matcher = Pattern.compile("count\":([0-9]+)").matcher(response);
                matcher.find();
                return Integer.parseInt(matcher.group(1));
            } catch (InterruptedException| IOException e) {
                throw new RuntimeException(e);
            }
        };

        doIndex.run();
        doIndex.run();
        // we expect 20 document although we have indexed the same documents twice; the reason is that the index
        // document ID is set randomly to simulate the situation where we index individual entities or relations
        // that have a document ID different from the main docId
        assertEquals(20, getNumDocuments.get());

        // now activate delete-before-index. After indexing anew, there should be only 10 documents in the index
        consumer.setConfigParameterValue(ElasticSearchConsumer.PARAM_DELETE_DOCS_BEFORE_INDEXING, true);
        consumer.setConfigParameterValue(ElasticSearchConsumer.PARAM_DOC_ID_FIELD, "docId");
        consumer.reconfigure();
        doIndex.run();
        assertEquals(10, getNumDocuments.get());
    }

    /**
     * This class is passed by name as parameter to the test consumer AE.
     */
    public static class TestFieldGenerator extends FieldGenerator {
        private int internalTestIdCounter = 0;

        public TestFieldGenerator(FilterRegistry filterRegistry) {
            super(filterRegistry);
        }

        @Override
        public Document addFields(JCas aJCas, Document doc) {
            doc.addField("text", new RawToken(aJCas.getDocumentText()));
            doc.addField("docId", new RawToken(JCoReTools.getDocId(aJCas)));
            // some diverging index document ID; we use this to test if the delete-before-index function works
            doc.setId("divergingid" + internalTestIdCounter++);
            return doc;
        }
    }

    /**
     * This class is passed by name as parameter to the test consumer AE.
     */
    public static class MinimalTestFieldGenerator extends FieldGenerator {
        public MinimalTestFieldGenerator(FilterRegistry filterRegistry) {
            super(filterRegistry);
        }

        @Override
        public Document addFields(JCas aJCas, Document doc) {
            final String docId = JCoReTools.getDocId(aJCas);
            doc.setId(docId);
            // we need any field or the document won't be indexed
            doc.addField("text", "Some text.");
            return doc;
        }
    }


}
