package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import com.pengyifan.bioc.io.BioCCollectionWriter;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
class BioCDocumentPopulatorTest {
    @Test
    public void populate() throws Exception {
        BioCDocumentPopulator populator = new BioCDocumentPopulator();
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
        assertThat(resultXml).containsOnlyOnce("<text>BACKGROUND This abstract section belongs to document 1.\nRESULTS There are certainly some results reported by document 1.</text>");
        assertThat(resultXml).containsOnlyOnce("INTRODUCTION");
        assertThat(resultXml).containsOnlyOnce("<infon key=\"type\">section_title</infon>");
        assertThat(resultXml).contains("<infon key=\"type\">paragraph</infon>");
        assertThat(resultXml).containsOnlyOnce("This is section 1, paragraph 1 of document 1.");
        assertThat(resultXml).containsOnlyOnce("This is a second paragraph in the first section.");
        assertThat(resultXml).containsOnlyOnce("<infon key=\"type\">table_title</infon>");
        assertThat(resultXml).containsOnlyOnce("Tab1.");
        assertThat(resultXml).containsOnlyOnce("This is the table1 caption.");
    }
}