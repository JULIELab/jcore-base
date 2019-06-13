package de.julielab.jcore.consumer.ew;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.data.Offset;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DecoderTest {
    @Test
    public void testReadDouble() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Double.BYTES);
        bb.putDouble(.42);
        bb.position(0);
        final ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
        final double d = Decoder.readDouble(new byte[Double.BYTES], bb, bais);
        assertThat(d).isEqualTo(.42);
    }

    @Test
    public void testReadInt() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
        bb.putInt(12345678);
        bb.position(0);
        final ByteArrayInputStream bais = new ByteArrayInputStream(bb.array());
        final int d = Decoder.readInt(new byte[Integer.BYTES], bb, bais);
        assertThat(d).isEqualTo(12345678);
    }

    @Test
    public void decodeEmbeddings() throws IOException {
        // We here read a copy of data that is written in the {@link EmbeddingsWriterTest#testWriterAllTokens}
        final Pair<List<String>, List<double[]>> embeddings = Decoder.decodeBinaryEmbeddingVectors(new FileInputStream("src/test/resources/testembeddings.dat"));
        assertThat(embeddings.getLeft()).containsExactly("t1", "t2", "t3");
        assertThat(embeddings.getRight()).containsExactly(new double[]{3, 12}, new double[]{7, 5}, new double[]{45, 13});
    }

    @Test
    public void decodeEmbeddingsSmallerBufferThanData() throws IOException {
        // We here use a smaller input buffer than the number of bytes we need to read to test if the algorithm
        // to read across buffers is working
        // A buffer of 2 is too small for even an integer so this will cause a whole lot of mid-decoding-reads
        final Pair<List<String>, List<double[]>> embeddings = Decoder.decodeBinaryEmbeddingVectors(new FileInputStream("src/test/resources/testembeddings.dat"), 2);
        assertThat(embeddings.getLeft()).containsExactly("t1", "t2", "t3");
        assertThat(embeddings.getRight()).containsExactly(new double[]{3, 12}, new double[]{7, 5}, new double[]{45, 13});
    }

    @Test
    public void testMergeEmbeddingFilesNoGrouping() throws Exception {
        List<ByteBuffer> testDocs = createTestEmbeddingFileData();

        final List<InputStream> inputStreams = testDocs.stream().map(bb -> new ByteArrayInputStream(bb.array())).collect(Collectors.toList());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Decoder.mergeEmbeddingFiles(inputStreams, baos, false);

        final Pair<List<String>, List<double[]>> originalVectors = Decoder.decodeBinaryEmbeddingVectors(new ByteArrayInputStream(baos.toByteArray()));
        List<String> texts = originalVectors.getLeft();
        final List<double[]> vectors = originalVectors.getRight();
        assertThat(texts).hasSize(10);
        assertThat(vectors).hasSize(10);
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            double[] vector = vectors.get(i);

            if (i == 0) {
                assertThat(text).isEqualTo("act1");
                assertThat(vector).isEqualTo(new double[]{.1, .1});
            }
            if (i == 1) {
                assertThat(text).isEqualTo("act1");
                assertThat(vector).isEqualTo(new double[]{.3, .1});
            }
            if (i == 2) {
                assertThat(text).isEqualTo("brca");
                assertThat(vector).isEqualTo(new double[]{.1, .2});
            }
            if (i == 3) {
                assertThat(text).isEqualTo("ckr");
                assertThat(vector).isEqualTo(new double[]{.2, .1});
            }
            if (i == 4) {
                assertThat(text).isEqualTo("dik");
                assertThat(vector).isEqualTo(new double[]{.3, .2});
            }
            if (i == 5) {
                assertThat(text).isEqualTo("fig");
                assertThat(vector).isEqualTo(new double[]{.2, .2});
            }
            if (i == 6) {
                assertThat(text).isEqualTo("il2");
                assertThat(vector).isEqualTo(new double[]{.1, .3});
            }
            if (i == 7) {
                assertThat(text).isEqualTo("il2");
                assertThat(vector).isEqualTo(new double[]{.2, .3});
            }
            if (i == 8) {
                assertThat(text).isEqualTo("il2");
                assertThat(vector).isEqualTo(new double[]{.3, .3});
            }
            if (i == 9) {
                assertThat(text).isEqualTo("mtor");
                assertThat(vector).isEqualTo(new double[]{.1, .4});
            }
        }
    }

    @Test
    public void testMergeEmbeddingFilesWithGrouping() throws Exception {
        List<ByteBuffer> testDocs = createTestEmbeddingFileData();

        final List<InputStream> inputStreams = testDocs.stream().map(bb -> new ByteArrayInputStream(bb.array())).collect(Collectors.toList());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Decoder.mergeEmbeddingFiles(inputStreams, baos, true);

        final Pair<List<String>, List<double[]>> originalVectors = Decoder.decodeBinaryEmbeddingVectors(new ByteArrayInputStream(baos.toByteArray()));
        List<String> texts = originalVectors.getLeft();
        final List<double[]> vectors = originalVectors.getRight();
        assertThat(texts).hasSize(7);
        assertThat(vectors).hasSize(7);
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            double[] vector = vectors.get(i);

            if (i == 0) {
                assertThat(text).isEqualTo("act1");
                assertThat(vector).isEqualTo(new double[]{.2, .1});
            }
            if (i == 1) {
                assertThat(text).isEqualTo("brca");
                assertThat(vector).isEqualTo(new double[]{.1, .2});
            }
            if (i == 2) {
                assertThat(text).isEqualTo("ckr");
                assertThat(vector).isEqualTo(new double[]{.2, .1});
            }
            if (i == 3) {
                assertThat(text).isEqualTo("dik");
                assertThat(vector).isEqualTo(new double[]{.3, .2});
            }
            if (i == 4) {
                assertThat(text).isEqualTo("fig");
                assertThat(vector).isEqualTo(new double[]{.2, .2});
            }
            if (i == 5) {
                assertThat(text).isEqualTo("il2");
                assertThat(vector[0]).isCloseTo(.2, Offset.offset(0.00000000001));
                assertThat(vector[1]).isCloseTo(.3, Offset.offset(0.00000000001));
            }
            if (i == 6) {
                assertThat(text).isEqualTo("mtor");
                assertThat(vector).isEqualTo(new double[]{.1, .4});
            }
        }
    }

    private List<ByteBuffer> createTestEmbeddingFileData() throws IOException {
        String act1File1 = "act1";
        double[] act1VFile1 = {.1, .1};
        final byte[] entry1File1 = Encoder.encodeTextVectorPair(act1File1, act1VFile1, null);
        String brcaFile1 = "brca";
        double[] brcaVFile1 = {.1, .2};
        final byte[] entry2File1 = Encoder.encodeTextVectorPair(brcaFile1, brcaVFile1, null);
        String il2File1 = "il2";
        double[] il2VFile1 = {.1, .3};
        final byte[] entry3File1 = Encoder.encodeTextVectorPair(il2File1, il2VFile1, null);
        String mtorFile1 = "mtor";
        double[] mtorVFile1 = {.1, .4};
        final byte[] entry4File1 = Encoder.encodeTextVectorPair(mtorFile1, mtorVFile1, null);
        ByteBuffer file1 = ByteBuffer.allocate(entry1File1.length + entry2File1.length + entry3File1.length + entry4File1.length);
        file1.put(entry1File1);
        file1.put(entry2File1);
        file1.put(entry3File1);
        file1.put(entry4File1);

        String ckrFile2 = "ckr";
        double[] ckrVFile2 = {.2, .1};
        final byte[] entry1File2 = Encoder.encodeTextVectorPair(ckrFile2, ckrVFile2, null);
        String figFile2 = "fig";
        double[] figVFile2 = {.2, .2};
        final byte[] entry2File2 = Encoder.encodeTextVectorPair(figFile2, figVFile2, null);
        String il2File2 = "il2";
        double[] il2VFile2 = {.2, .3};
        final byte[] entry3File2 = Encoder.encodeTextVectorPair(il2File2, il2VFile2, null);
        ByteBuffer file2 = ByteBuffer.allocate(entry1File2.length + entry2File2.length + entry3File2.length);
        file2.put(entry1File2);
        file2.put(entry2File2);
        file2.put(entry3File2);

        String act1File3 = "act1";
        double[] act1VFile3 = {.3, .1};
        final byte[] entry1File3 = Encoder.encodeTextVectorPair(act1File3, act1VFile3, null);
        String dikFil3 = "dik";
        double[] dikVFile3 = {.3, .2};
        final byte[] entry2File3 = Encoder.encodeTextVectorPair(dikFil3, dikVFile3, null);
        String il2File3 = "il2";
        double[] il2VFile3 = {.3, .3};
        final byte[] entry3File3 = Encoder.encodeTextVectorPair(il2File3, il2VFile3, null);
        ByteBuffer file3 = ByteBuffer.allocate(entry1File3.length + entry2File3.length + entry3File3.length);
        file3.put(entry1File3);
        file3.put(entry2File3);
        file3.put(entry3File3);

        return Arrays.asList(file1, file2, file3);
    }
}
