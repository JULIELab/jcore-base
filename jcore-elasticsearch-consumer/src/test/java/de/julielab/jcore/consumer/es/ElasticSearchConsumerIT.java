package de.julielab.jcore.consumer.es;

import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.Header;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ElasticSearchConsumerIT {
    public static final String TEST_INDEX = "testindex";
    public static final String TEST_CLUSTER = "testcluster";
    private final static Logger log = LoggerFactory.getLogger(ElasticSearchConsumerIT.class);
    // in case we need to disable X-shield: https://stackoverflow.com/a/51172136/1314955
    @ClassRule
    public static GenericContainer es = new GenericContainer("docker.elastic.co/elasticsearch/elasticsearch:7.0.1")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(9200)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withEnv("cluster.name", TEST_CLUSTER);

    @BeforeClass
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
                ElasticSearchConsumer.PARAM_FIELD_GENERATORS, new String[]{"de.julielab.jcore.consumer.es.ElasticSearchConsumerIT$TestFieldGenerator"});
        consumer.process(jCas);
        consumer.collectionProcessComplete();
        final URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_doc/987");
        final ObjectMapper om = new ObjectMapper();
        final Map<?,?> map = om.readValue(url.openStream(), Map.class);
        assertEquals(jCas.getDocumentText(), ((Map)map.get("_source")).get("text"));
    }

    /**
     * This class is passed by name as parameter to the test consumer AE.
     */
    public static class TestFieldGenerator extends FieldGenerator {
        public TestFieldGenerator(FilterRegistry filterRegistry) {
            super(filterRegistry);
        }

        @Override
        public Document addFields(JCas aJCas, Document doc) {
            doc.addField("text", new RawToken(aJCas.getDocumentText()));
            return doc;
        }
    }


}
