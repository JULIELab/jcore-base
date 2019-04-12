package de.julielab.jcore.consumer.ew;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Encoder {
    public static byte[] encodeTextVectorPair(Pair<String, double[]> pair, ByteBuffer bb) throws IOException {
        return encodeTextVectorPair(pair.getLeft(), pair.getRight(), bb);
    }

    public static byte[] encodeTextVectorPair(String text, double[] vector, ByteBuffer bb) throws IOException {
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

        return Arrays.copyOf(output, output.length);
    }
}
