package de.julielab.jcore.consumer.ew;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
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
}
