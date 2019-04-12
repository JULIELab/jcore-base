package de.julielab.jcore.consumer.ew;

import de.julielab.jcore.types.Token;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public class Encoder {
    public static byte[] encodeTextVectorPair(Pair<String, double[]> pair, ByteBuffer bb, boolean gzip) throws IOException {
        return encodeTextVectorPair(pair.getLeft(), pair.getRight(), bb, gzip);
    }

    public static byte[] encodeTextVectorPair(String text, double[] vector, ByteBuffer bb, boolean gzip) throws IOException {
        final byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int requiredBytes = Integer.BYTES * 2 + textBytes.length + vector.length * Double.BYTES;
        if (bb == null || bb.capacity() < requiredBytes)
            bb = ByteBuffer.allocate(requiredBytes);
        bb.position(0);

        bb.putInt(textBytes.length);
        bb.put(textBytes);
        bb.putInt(vector.length);
        for (double d : vector)
            bb.putDouble(d);

        byte[] output = bb.array();
        if (gzip) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream os = new GZIPOutputStream(baos);
            os.write(output);
            os.close();
            output = baos.toByteArray();
        }

        return Arrays.copyOf(output, output.length);
    }

    public static double[] getAverageEmbeddingVector(Stream<double[]> vectors) {
        final MutablePair<Integer, double[]> vectorAvg = vectors.collect(() -> new MutablePair<>(0, null), (avg, d) -> {
            avg.setLeft(avg.getLeft() + 1);
            double[] vector = avg.getRight();
            if (vector == null) {
                vector = Arrays.copyOf(d, d.length);
                avg.setRight(vector);
            } else {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] += d[i];
                }
            }
        }, (p1, p2) -> {
            p1.setLeft(p1.getLeft() + p2.getLeft());
            double[] vectorSum = p1.getRight();
            for (int i = 0; i < vectorSum.length; i++)
                vectorSum[i] += p2.getRight()[i];
        });
        return DoubleStream.of(vectorAvg.getRight()).map(d -> d / vectorAvg.getLeft()).toArray();
    }
}
