
package de.julielab.jcore.consumer.neo4jrelations;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.ext.FlattenedRelation;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.neo4j.plugins.datarepresentation.ImportIERelation;
import de.julielab.neo4j.plugins.datarepresentation.ImportIERelationArgument;
import de.julielab.neo4j.plugins.datarepresentation.ImportIERelationDocument;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-neo4j-relations-consumer.
 *
 */
public class Neo4jRelationsConsumerTest {


    @Test
    public void insertEventMentions() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
        Header h = new Header(jCas);
        h.setDocId("testdoc");
        h.addToIndexes();
        Neo4jRelationsConsumer engine = new Neo4jRelationsConsumer();
        engine.initialize(UimaContextFactory.createUimaContext(Neo4jRelationsConsumer.PARAM_URL, "", Neo4jRelationsConsumer.PARAM_ID_PROPERTY, "sourceIds"));
        addFlattenedRelation1ToCas(jCas);
        // Here is a duplicate. It should be recognized and just be counted up
        addFlattenedRelation2ToCas(jCas);
        addFlattenedRelation2ToCas(jCas);

        Method m = Neo4jRelationsConsumer.class.getDeclaredMethod("convertRelations", JCas.class);
        m.setAccessible(true);
        ImportIERelationDocument relations = (ImportIERelationDocument) m.invoke(engine, jCas);
        assertThat(relations).extracting(ImportIERelationDocument::getRelations).isNotNull();
        assertThat(relations.getRelations()).hasSize(1);
        List<ImportIERelation> regulations = relations.getRelations().get("regulation");
        assertThat(regulations).hasSize(2);
        assertThat(regulations.get(0)).extracting(ImportIERelation::getCount).isEqualTo(1);
        assertThat(regulations.get(1)).extracting(ImportIERelation::getCount).isEqualTo(2);
        assertThat(regulations).flatExtracting(ImportIERelation::getArgs).flatExtracting(ImportIERelationArgument::getId).containsExactlyInAnyOrder("id11", "id12", "id13", "id21", "id22");
        assertThat(regulations).flatExtracting(ImportIERelation::getArgs).flatExtracting(ImportIERelationArgument::getSource).containsExactlyInAnyOrder("source11", "source12", "source13", "source21", "source22");
    }

    @Test
    public void insertEventMentionsGlobalSource() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.extensions.jcore-semantics-mention-extension-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types");
        Header h = new Header(jCas);
        h.setDocId("testdoc");
        h.addToIndexes();
        Neo4jRelationsConsumer engine = new Neo4jRelationsConsumer();
        engine.initialize(UimaContextFactory.createUimaContext(Neo4jRelationsConsumer.PARAM_URL, "", Neo4jRelationsConsumer.PARAM_ID_PROPERTY, "sourceIds", Neo4jRelationsConsumer.PARAM_SOURCE, "globalSource"));
        addFlattenedRelation1ToCas(jCas);
        addFlattenedRelation2ToCas(jCas);

        Method m = Neo4jRelationsConsumer.class.getDeclaredMethod("convertRelations", JCas.class);
        m.setAccessible(true);
        ImportIERelationDocument relations = (ImportIERelationDocument) m.invoke(engine, jCas);
        assertThat(relations).extracting(ImportIERelationDocument::getRelations).isNotNull();
        assertThat(relations.getRelations()).hasSize(1);
        List<ImportIERelation> regulations = relations.getRelations().get("regulation");
        assertThat(regulations).hasSize(2);
        // With the global source set, the individual sources are left out
        assertThat(regulations).flatExtracting(ImportIERelation::getArgs).flatExtracting(ImportIERelationArgument::getSource).containsExactlyInAnyOrder(null, null, null, null, null);
    }

    /**
     * Adds a FlattenedRelation with three arguments.
     * @param jCas The CAS.
     */
    public static void addFlattenedRelation1ToCas(JCas jCas) {
        FlattenedRelation fr = new FlattenedRelation(jCas);
        EventMention rootEm = new EventMention(jCas);
        rootEm.setSpecificType("regulation");
        fr.setRootRelation(rootEm);

        ArgumentMention am1 = new ArgumentMention(jCas);
        ConceptMention cm1 = new ConceptMention(jCas);
        ResourceEntry re1 = new ResourceEntry(jCas);
        re1.setEntryId("id11");
        re1.setSource("source11");
        cm1.setResourceEntryList(JCoReTools.addToFSArray(null, re1));
        am1.setRef(cm1);

        ArgumentMention am2 = new ArgumentMention(jCas);
        ConceptMention cm2 = new ConceptMention(jCas);
        ResourceEntry re2 = new ResourceEntry(jCas);
        re2.setEntryId("id12");
        re2.setSource("source12");
        cm2.setResourceEntryList(JCoReTools.addToFSArray(null, re2));
        am2.setRef(cm2);

        ArgumentMention am3 = new ArgumentMention(jCas);
        ConceptMention cm3 = new ConceptMention(jCas);
        ResourceEntry re3 = new ResourceEntry(jCas);
        re3.setEntryId("id13");
        re3.setSource("source13");
        cm3.setResourceEntryList(JCoReTools.addToFSArray(null, re3));
        am3.setRef(cm3);

        fr.setArguments(JCoReTools.addToFSArray(null, List.of(am1, am2, am3)));
        fr.addToIndexes();
    }

    /**
     * Adds a FlattenedRelation with two arguments.
     * @param jCas The CAS.
     */
    public static void addFlattenedRelation2ToCas(JCas jCas) {
        FlattenedRelation fr = new FlattenedRelation(jCas);
        EventMention rootEm = new EventMention(jCas);
        rootEm.setSpecificType("regulation");
        fr.setRootRelation(rootEm);

        ArgumentMention am1 = new ArgumentMention(jCas);
        ConceptMention cm1 = new ConceptMention(jCas);
        ResourceEntry re1 = new ResourceEntry(jCas);
        re1.setEntryId("id21");
        re1.setSource("source21");
        cm1.setResourceEntryList(JCoReTools.addToFSArray(null, re1));
        am1.setRef(cm1);

        ArgumentMention am2 = new ArgumentMention(jCas);
        ConceptMention cm2 = new ConceptMention(jCas);
        ResourceEntry re2 = new ResourceEntry(jCas);
        re2.setEntryId("id22");
        re2.setSource("source22");
        cm2.setResourceEntryList(JCoReTools.addToFSArray(null, re2));
        am2.setRef(cm2);

        fr.setArguments(JCoReTools.addToFSArray(null, List.of(am1, am2)));
        fr.addToIndexes();
    }

}
