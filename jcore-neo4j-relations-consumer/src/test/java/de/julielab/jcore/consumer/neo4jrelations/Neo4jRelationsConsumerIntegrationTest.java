
package de.julielab.jcore.consumer.neo4jrelations;

import de.julielab.jcore.types.pubmed.Header;
import de.julielab.neo4j.plugins.Indexes;
import de.julielab.neo4j.plugins.concepts.ConceptLookup;
import de.julielab.neo4j.plugins.concepts.ConceptManager;
import de.julielab.neo4j.plugins.datarepresentation.*;
import de.julielab.neo4j.plugins.datarepresentation.constants.FacetConstants;
import de.julielab.neo4j.plugins.datarepresentation.util.ConceptsJsonSerializer;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.test.server.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static de.julielab.jcore.consumer.neo4jrelations.Neo4jRelationsConsumerTest.addFlattenedRelation1ToCas;
import static de.julielab.jcore.consumer.neo4jrelations.Neo4jRelationsConsumerTest.addFlattenedRelation2ToCas;
import static de.julielab.neo4j.plugins.constants.semedico.SemanticRelationConstants.PROP_DOC_IDS;
import static de.julielab.neo4j.plugins.datarepresentation.constants.ConceptConstants.PROP_SRC_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

/**
 * Unit tests for jcore-neo4j-relations-consumer.
 *
 */
public class Neo4jRelationsConsumerIntegrationTest {
    private final static Logger log = LoggerFactory.getLogger(Neo4jRelationsConsumerIntegrationTest.class);
    @ClassRule
    public static Neo4jRule neo4j = new Neo4jRule()
            .withUnmanagedExtension("/concepts", ConceptManager.class).withFixture(graphDatabaseService -> {
                new Indexes(null).createIndexes(graphDatabaseService);
                return null;
            });

    @BeforeClass
    public static void beforeClass() throws Exception {
        ImportFacet facet = new ImportFacet(new ImportFacetGroup("FG"), "myfacet", "myfacet", "myfacet", FacetConstants.SRC_TYPE_HIERARCHICAL);
        ImportConcept c11 = new ImportConcept("concept11", new ConceptCoordinates("id11", "source11", CoordinateType.SRC));
        ImportConcept c12 = new ImportConcept("concept12", new ConceptCoordinates("id12", "source12", CoordinateType.SRC));
        ImportConcept c13 = new ImportConcept("concept13", new ConceptCoordinates("id13", "source13", CoordinateType.SRC));
        ImportConcept c21 = new ImportConcept("concept21", new ConceptCoordinates("id21", "source21", CoordinateType.SRC));
        ImportConcept c22 = new ImportConcept("concept22", new ConceptCoordinates("id22", "source22", CoordinateType.SRC));
        ImportConcepts importConcepts = new ImportConcepts(Stream.of(c11, c12, c13, c21, c22), facet);
        String uri = neo4j.httpURI().resolve("concepts/" + ConceptManager.CM_REST_ENDPOINT+"/"+ConceptManager.INSERT_CONCEPTS).toString();
        log.debug("Sending concepts to {}", uri);
        HTTP.Response response = HTTP.POST(uri, ConceptsJsonSerializer.toJsonTree(importConcepts));
        log.debug("Response to test concepts import: {}", response);
        assertEquals(200, response.status());
    }

    @Test
    public void insertEventMentions() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.extensions.jcore-document-meta-extension-types", "de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
        Header h = new Header(jCas);
        h.setDocId("testdoc");
        h.addToIndexes();
        addFlattenedRelation1ToCas(jCas);
        // Here is a duplicate. It should be recognized and just be counted up
        addFlattenedRelation2ToCas(jCas);
        addFlattenedRelation2ToCas(jCas);

        AnalysisEngine engine = AnalysisEngineFactory.createEngine(
                "de.julielab.jcore.consumer.neo4jrelations.desc.jcore-neo4j-relations-consumer",
                Neo4jRelationsConsumer.PARAM_URL, neo4j.httpURI().resolve("concepts/" + ConceptManager.CM_REST_ENDPOINT+"/"+ConceptManager.INSERT_IE_RELATIONS).toString(),
                Neo4jRelationsConsumer.PARAM_ID_PROPERTY, "sourceIds");

        engine.process(jCas);
        engine.collectionProcessComplete();

        GraphDatabaseService graphDb = neo4j.databaseManagementService().database(DEFAULT_DATABASE_NAME);
        try (Transaction tx = graphDb.beginTx()) {
            Node id11 = ConceptLookup.lookupSingleConceptBySourceId(tx, "id11");
            // There should be connections to 12 and 13.
            assertThat(id11.getRelationships(RelationshipType.withName("regulation"))).hasSize(2);
            assertThat(id11.getRelationships(RelationshipType.withName("regulation"))).flatExtracting(r -> List.of((String[]) r.getProperty(PROP_DOC_IDS))).containsExactly("testdoc", "testdoc");
            assertThat(id11.getRelationships(RelationshipType.withName("regulation"))).extracting(r -> r.getOtherNode(id11).getProperty(PROP_SRC_IDS+0)).containsExactlyInAnyOrder("id12", "id13");

            Node id13 = ConceptLookup.lookupSingleConceptBySourceId(tx, "id13");
            // There should be connections to 11 and 12.
            assertThat(id13.getRelationships(RelationshipType.withName("regulation"))).hasSize(2);
            assertThat(id13.getRelationships(RelationshipType.withName("regulation"))).flatExtracting(r -> List.of((String[]) r.getProperty(PROP_DOC_IDS))).containsExactly("testdoc", "testdoc");
            assertThat(id13.getRelationships(RelationshipType.withName("regulation"))).extracting(r -> r.getOtherNode(id13).getProperty(PROP_SRC_IDS+0)).containsExactlyInAnyOrder("id11", "id12");

            Node id22 = ConceptLookup.lookupSingleConceptBySourceId(tx, "id22");
            // There should be connections to 21
            assertThat(id22.getRelationships(RelationshipType.withName("regulation"))).hasSize(1);
            assertThat(id22.getRelationships(RelationshipType.withName("regulation"))).flatExtracting(r -> List.of((String[]) r.getProperty(PROP_DOC_IDS))).containsExactly("testdoc");
            assertThat(id22.getRelationships(RelationshipType.withName("regulation"))).extracting(r -> r.getOtherNode(id22).getProperty(PROP_SRC_IDS+0)).containsExactlyInAnyOrder("id21");
        }
    }
}
