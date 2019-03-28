package de.julielab.jcore.consumer.topics;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.DocumentTopics;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for jcore-topics-writer.
 */
public class TopicsWriterTest {

    @BeforeClass
    @AfterClass
    public static void setup() {
        FileUtils.deleteQuietly(new File("src/test/resources/output"));
    }

    @Test
    public void testWriteTopics() throws UIMAException, IOException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types");
        AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.topics.desc.jcore-topics-writer", TopicsWriter.PARAM_OUTPUT_DIR, "src/test/resources/output");
        DocumentTopics documentTopics = new DocumentTopics(jCas);
        DoubleArray doubles = new DoubleArray(jCas, 3);
        doubles.set(0, 0.1);
        doubles.set(1, 0.2);
        doubles.set(2, 0.3);
        documentTopics.setWeights(doubles);
        documentTopics.setModelID("mod1");
        documentTopics.setModelVersion("1.0");
        documentTopics.addToIndexes();

        documentTopics = new DocumentTopics(jCas);
        doubles = new DoubleArray(jCas, 3);
        doubles.set(0, 0.5);
        doubles.set(1, 0.6);
        doubles.set(2, 0.7);
        documentTopics.setWeights(doubles);
        documentTopics.setModelID("mod1");
        documentTopics.setModelVersion("1.0");
        documentTopics.addToIndexes();

        documentTopics = new DocumentTopics(jCas);
        doubles = new DoubleArray(jCas, 2);
        doubles.set(0, 0.8);
        doubles.set(1, 0.9);
        documentTopics.setWeights(doubles);
        documentTopics.setModelID("mod2");
        documentTopics.setModelVersion("3.2");
        documentTopics.addToIndexes();

        assertThatCode(() -> engine.process(jCas)).doesNotThrowAnyException();
        engine.collectionProcessComplete();
        File outputDir = new File("src/test/resources/output");
        assertThat(outputDir).exists();
        assertThat(outputDir.listFiles((f) -> !f.getName().equals(".DS_Store")).length).isEqualTo(2);
        final File mod1 = outputDir.listFiles((f) -> f.getName().startsWith("mod1"))[0];
        assertThat(mod1.getName()).contains("mod1").contains("1.0");
        List<String> lines = FileUtilities.getReaderFromFile(mod1).lines().collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(2);
        assertThat(lines.get(0)).isEqualTo("doc0	0.1	0.2	0.3");
        assertThat(lines.get(1)).isEqualTo("doc0	0.5	0.6	0.7");

        final File mod2 = outputDir.listFiles((f) -> f.getName().startsWith("mod2"))[0];
        assertThat(mod2.getName()).contains("mod2").contains("3.2");
        lines = FileUtilities.getReaderFromFile(mod2).lines().collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(1);
        assertThat(lines.get(0)).isEqualTo("doc0	0.8	0.9");
    }
}
