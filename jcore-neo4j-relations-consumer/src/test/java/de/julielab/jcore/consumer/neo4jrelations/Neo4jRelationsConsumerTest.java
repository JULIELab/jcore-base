
package de.julielab.jcore.consumer.neo4jrelations;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.IOException;


/**
 * Unit tests for jcore-neo4j-relations-consumer.
 *
 */
public class Neo4jRelationsConsumerTest {


    @Test
    public void insertEventMentions() throws UIMAException, IOException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
        AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.neo4jrelations.desc.jcore-neo4j-relations-consumer", Neo4jRelationsConsumer.PARAM_URL, "");


    }
}
