package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import com.pengyifan.bioc.io.BioCCollectionWriter;
import de.julielab.jcore.types.Gene;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
class BioCDocumentPopulatorTest {
    @Test
    public void populate() throws Exception {
        BioCDocumentPopulator populator = new BioCDocumentPopulator(false);
        JCas jCas = TestDocumentGenerator.prepareCas(1);
        BioCDocument biocDoc = populator.populate(jCas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BioCCollection collection = new BioCCollection("UTF-8", "1.0", (new Date()).toString(), true, "jUnit Test", "PubTator.key");
        collection.addDocument(biocDoc);
        BioCCollectionWriter collectionWriter = new BioCCollectionWriter(baos);
        collectionWriter.writeCollection(collection);
        String resultXml = baos.toString(StandardCharsets.UTF_8);
        // Just check that the test text contents are there that are used in TestDocumentGenerator and that
        // there are no duplicates
        assertThat(resultXml).containsOnlyOnce("<text>This is the title of document 1.</text>");
        assertThat(resultXml).containsOnlyOnce("<infon key=\"type\">title</infon>");
        // The abstract should be one single string
        assertThat(resultXml).containsOnlyOnce("<text>This abstract section belongs to document 1. There are certainly some results reported by document 1.</text>");
        assertThat(resultXml).containsOnlyOnce("INTRODUCTION");
        assertThat(resultXml).containsOnlyOnce("<infon key=\"type\">section_title</infon>");
        assertThat(resultXml).contains("<infon key=\"type\">paragraph</infon>");
        assertThat(resultXml).containsOnlyOnce("This is section 1, paragraph 1 of document 1.");
        assertThat(resultXml).containsOnlyOnce("This is a second paragraph in the first section.");
        assertThat(resultXml).containsOnlyOnce("<infon key=\"type\">table_title</infon>");
        assertThat(resultXml).containsOnlyOnce("Tab1.");
        assertThat(resultXml).containsOnlyOnce("This is the table1 caption.");
    }

    @Test
    public void populateWithGenes() throws Exception {
        BioCDocumentPopulator populator = new BioCDocumentPopulator(true);
        JCas jCas = TestDocumentGenerator.prepareCas(1);
        new Gene(jCas, 0, 4).addToIndexes();
        new Gene(jCas, 87, 96).addToIndexes();
        BioCDocument biocDoc = populator.populate(jCas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BioCCollection collection = new BioCCollection("UTF-8", "1.0", (new Date()).toString(), true, "jUnit Test", "PubTator.key");
        collection.addDocument(biocDoc);
        BioCCollectionWriter collectionWriter = new BioCCollectionWriter(baos);
        collectionWriter.writeCollection(collection);
        String resultXml = baos.toString(StandardCharsets.UTF_8);
        assertThat(resultXml).containsOnlyOnce("<annotation id=\"0\">");
        assertThat(resultXml).contains("<infon key=\"type\">Gene</infon>");
        assertThat(resultXml).containsOnlyOnce("<location offset=\"0\" length=\"4\"/>");
        assertThat(resultXml).containsOnlyOnce("<text>This</text>");

        assertThat(resultXml).contains("<annotation id=\"1\">");
        assertThat(resultXml).contains("<infon key=\"type\">Gene</infon>");
        assertThat(resultXml).containsOnlyOnce("<location offset=\"87\" length=\"9\"/>");
        assertThat(resultXml).containsOnlyOnce("<text>certainly</text>");
    }

    @Test
    public void populateWithGeneFamilies() throws Exception {
        BioCDocumentPopulator populator = new BioCDocumentPopulator(true);
        JCas jCas = TestDocumentGenerator.prepareCas(1);
        Gene gene = new Gene(jCas, 0, 4);
        gene.setSpecificType("protein_familiy_or_group");
        gene.addToIndexes();
        BioCDocument biocDoc = populator.populate(jCas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BioCCollection collection = new BioCCollection("UTF-8", "1.0", (new Date()).toString(), true, "jUnit Test", "PubTator.key");
        collection.addDocument(biocDoc);
        BioCCollectionWriter collectionWriter = new BioCCollectionWriter(baos);
        collectionWriter.writeCollection(collection);
        String resultXml = baos.toString(StandardCharsets.UTF_8);
        assertThat(resultXml).containsOnlyOnce("<annotation id=\"0\">");
        assertThat(resultXml).contains("<infon key=\"type\">FamilyName</infon>");
        assertThat(resultXml).containsOnlyOnce("<location offset=\"0\" length=\"4\"/>");
        assertThat(resultXml).containsOnlyOnce("<text>This</text>");
    }

    @Test
    public void populateWithGeneFamilies2() throws Exception {
        BioCDocumentPopulator populator = new BioCDocumentPopulator(true);
        JCas jCas = TestDocumentGenerator.prepareCas(1);
        Gene gene = new Gene(jCas, 0, 4);
        gene.setSpecificType("FamilyName");
        gene.addToIndexes();
        BioCDocument biocDoc = populator.populate(jCas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BioCCollection collection = new BioCCollection("UTF-8", "1.0", (new Date()).toString(), true, "jUnit Test", "PubTator.key");
        collection.addDocument(biocDoc);
        BioCCollectionWriter collectionWriter = new BioCCollectionWriter(baos);
        collectionWriter.writeCollection(collection);
        String resultXml = baos.toString(StandardCharsets.UTF_8);
        assertThat(resultXml).containsOnlyOnce("<annotation id=\"0\">");
        assertThat(resultXml).contains("<infon key=\"type\">FamilyName</infon>");
        assertThat(resultXml).containsOnlyOnce("<location offset=\"0\" length=\"4\"/>");
        assertThat(resultXml).containsOnlyOnce("<text>This</text>");
    }
}