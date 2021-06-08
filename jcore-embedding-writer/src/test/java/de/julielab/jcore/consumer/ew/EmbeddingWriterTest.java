
package de.julielab.jcore.consumer.ew;

import de.julielab.jcore.types.EmbeddingVector;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-flair-embedding-writer.
 *
 */
public class EmbeddingWriterTest {
    @Test
    public void testGetAverageEmbeddingVector() throws Exception {
        final double[] d1 = {3, 12};
        final double[] d2 = {7, 5};
        final double[] avgVector = VectorOperations.getAverageEmbeddingVector(Stream.of(d1, d2));

        assertThat(avgVector).containsExactly(5, 8.5);
    }

    @Test
    public void testWriterAllTokens() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
        // Note how the tokens are not in ascending text order but in the asserts below, we check in text order.
        // The output file should be sorted.
        jCas.setDocumentText("t1 t3 t2");
        Token t1 = new Token(jCas, 0, 2);
        final EmbeddingVector e1 = new EmbeddingVector(jCas, 0, 2);
        final DoubleArray v1 = new DoubleArray(jCas, 2);
        v1.set(0, 3);
        v1.set(1, 12);
        e1.setVector(v1);
        t1.setEmbeddingVectors(JCoReTools.addToFSArray(null, e1));
        t1.addToIndexes();

        Token t2 = new Token(jCas, 6, 8);
        final EmbeddingVector e3 = new EmbeddingVector(jCas, 6, 8);
        final DoubleArray v3 = new DoubleArray(jCas, 2);
        v3.set(0, 7);
        v3.set(1, 5);
        e3.setVector(v3);
        t2.setEmbeddingVectors(JCoReTools.addToFSArray(null, e3));
        t2.addToIndexes();

        Token t3 = new Token(jCas, 3, 5);
        final EmbeddingVector e2 = new EmbeddingVector(jCas, 3, 5);
        final DoubleArray v2 = new DoubleArray(jCas, 2);
        v2.set(0, 45);
        v2.set(1, 13);
        e2.setVector(v2);
        t3.setEmbeddingVectors(JCoReTools.addToFSArray(null, e2));
        t3.addToIndexes();

        final String outputDir = "src/test/resources/output";
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.ew.desc.jcore-embedding-writer", EmbeddingWriter.PARAM_OUTDIR, outputDir);

        engine.process(jCas);
        engine.collectionProcessComplete();

        // Check if we wrote what we expected to
        final File[] files = new File(outputDir).listFiles(file -> file.getName().startsWith("embeddings-"));
        assertThat(files).withFailMessage("There are multiple files in the test output directory, only one file is expected. You can just delete the whole directory " + outputDir).hasSize(1);
        final Pair<List<String>, List<double[]>> embeddings = Decoder.decodeBinaryEmbeddingVectors(new FileInputStream(files[0]));
        // The output file should be sorted by text
        assertThat(embeddings.getLeft()).containsExactly("t1", "t2", "t3");
        assertThat(embeddings.getRight()).containsExactly(new double[]{3, 12}, new double[]{7, 5}, new double[]{45, 13});
        files[0].delete();
    }
}
