package de.julielab.jcore.consumer.gnp;


import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.io.BioCCollectionReader;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-gnp-bioc-writer.
 */
public class GNormPlusFormatWriterTest {

    private static final Path BASEDIR = Path.of("src", "test", "resources", "testoutput");

    @AfterAll
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

        assertThat(Path.of(BASEDIR.toString(), "bioc_collections_0", "bioc_collection_0.xml")).exists().isNotEmptyFile();
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
                assertThat(Path.of(BASEDIR.toString(), "bioc_collections_"+i, "bioc_collection_"+j+".xml")).exists().isNotEmptyFile();
            }
        }
        // there should only be two files in the last directory
        assertThat(Path.of(BASEDIR.toString(), "bioc_collections_2", "bioc_collection_2.xml")).doesNotExist();

        // the last file should only contain a single document
        BioCCollectionReader reader = new BioCCollectionReader(Path.of(BASEDIR.toString(), "bioc_collections_2", "bioc_collection_1.xml"));
        BioCCollection lastCollection = reader.readCollection();
        assertThat(lastCollection.getDocmentCount()).isEqualTo(1);

    }

}
