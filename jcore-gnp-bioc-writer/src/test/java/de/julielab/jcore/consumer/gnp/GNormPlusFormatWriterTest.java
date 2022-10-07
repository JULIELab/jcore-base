package de.julielab.jcore.consumer.gnp;


import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.io.BioCCollectionReader;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-gnp-bioc-writer.
 */
public class GNormPlusFormatWriterTest {

    private static final Path BASEDIR = Path.of("src", "test", "resources", "testoutput");

//    @AfterAll
    public static void cleanFinally() {
        FileUtils.deleteQuietly(BASEDIR.toFile());
    }

    @BeforeEach
    public void cleanOutput() {
        FileUtils.deleteQuietly(BASEDIR.toFile());
    }

    private AnalysisEngine getWriterInstance(int docsPerFile, int filesPerDir) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngine(GNormPlusFormatWriter.class, GNormPlusFormatWriter.PARAM_BASE_DIR, BASEDIR.toString(), GNormPlusFormatWriter.PARAM_NUM_DOCS_PER_FILE, docsPerFile, GNormPlusFormatWriter.PARAM_NUM_FILES_PER_DIR, filesPerDir);
    }

    @Test
    public void process1() throws Exception {
        // write a single document
        JCas jCas = TestDocumentGenerator.prepareCas(1);
        AnalysisEngine writer = getWriterInstance(1, 1);
        writer.process(jCas);
        writer.collectionProcessComplete();

        assertThat(Path.of(BASEDIR.toString(), "bioc_collections_0", "bioc_collection_0_0.xml")).exists().isNotEmptyFile();
    }

    @Test
    public void process2() throws Exception {
        // write a single document
        JCas jCas = TestDocumentGenerator.createTestJCas();
        AnalysisEngine writer = getWriterInstance(2, 3);
        for (int i = 0; i < 15; ++i) {
            TestDocumentGenerator.prepareCas(jCas, i);
            writer.process(jCas);
            jCas.reset();
        }
        writer.collectionProcessComplete();

        assertThat(Files.list(BASEDIR)).hasSize(3);
        for (int i : List.of(0, 1, 2)) {
            List<Integer> fileIndices = i < 2 ? List.of(0, 1, 2) : List.of(0,1);
            for (int j : fileIndices) {
                assertThat(Path.of(BASEDIR.toString(), "bioc_collections_"+i, "bioc_collection_"+i+"_"+j+".xml")).exists().isNotEmptyFile();
            }
        }
        // there should only be two files in the last directory
        assertThat(Path.of(BASEDIR.toString(), "bioc_collections_2", "bioc_collection_2_2.xml")).doesNotExist();

        // the last file should only contain a single document
        BioCCollectionReader reader = new BioCCollectionReader(Path.of(BASEDIR.toString(), "bioc_collections_2", "bioc_collection_2_1.xml"));
        BioCCollection lastCollection = reader.readCollection();
        assertThat(lastCollection.getDocmentCount()).isEqualTo(1);

    }

    @Test
    public void omitEmptyDocuments() throws Exception {
        // GNormPlus doesn't handle documents well which do not have any passage. Then, at some later document in the same collection, array out of bounds exceptions appear.
        // Make sure we just don't write empty documents. They wouldn't have any annotations anyway.
        JCas jCas = TestDocumentGenerator.createTestJCas();
        Header h = new Header(jCas);
        h.setDocId("1");
        h.addToIndexes();
        AnalysisEngine writer = getWriterInstance(1, 1);
        writer.process(jCas);
        jCas.reset();
        jCas.setDocumentText("Hello.");
        Header h2 = new Header(jCas);
        h2.setDocId("2");
        h2.addToIndexes();
        Title title = new Title(jCas, 0, 6);
        title.setTitleType("document");
        title.addToIndexes();
        writer.process(jCas);
        writer.collectionProcessComplete();
        // assert that no empty documents were written into the collection
        assertThat(Files.lines(Path.of(BASEDIR.toString(), "bioc_collections_0", "bioc_collection_0_0.xml")).map(String::trim).collect(Collectors.joining())).doesNotContain("</id></document>");
        assertThat(Files.lines(Path.of(BASEDIR.toString(), "bioc_collections_0", "bioc_collection_0_0.xml")).map(String::trim).collect(Collectors.joining())).contains("<document><id>2</id><passage>");
    }

    @Test
    public void omitEmptyDocuments2() throws Exception {
        // Additionally to not writing empty documents, we also don't want to write empty collections. This, too, causes out of bounds errors in GNormPlus.
        JCas jCas = TestDocumentGenerator.createTestJCas();
        Header h = new Header(jCas);
        h.setDocId("1");
        h.addToIndexes();
        AnalysisEngine writer = getWriterInstance(1, 1);
        writer.process(jCas);
        // assert that no empty documents were written into the collection
        assertThat(Path.of(BASEDIR.toString(), "bioc_collections_0", "bioc_collection_0_0.xml")).doesNotExist();
    }
}
