
package de.julielab.jcore.consumer.neo4jrelations;

import de.julielab.neo4j.plugins.Indexes;
import de.julielab.neo4j.plugins.concepts.ConceptManager;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.rule.Neo4jRule;


/**
 * Unit tests for jcore-neo4j-relations-consumer.
 *
 */
public class Neo4jRelationsConsumerIntegrationTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withUnmanagedExtension("/concepts", ConceptManager.class).withFixture(graphDatabaseService -> {
                new Indexes(null).createIndexes(graphDatabaseService);
                return null;
            });

    @Test
    public void insertEventMentions() throws UIMAException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");

    }
}
