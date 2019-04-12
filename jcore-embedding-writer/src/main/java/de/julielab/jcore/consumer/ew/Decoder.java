package de.julielab.jcore.consumer.ew;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Decoder {

    public static Pair<List<String>, List<double[]>> decodeBinaryEmbeddingVectors(InputStream is) throws IOException {
        return decodeBinaryEmbeddingVectors(is, 8192);
    }
    public static Pair<List<String>, List<double[]>> decodeBinaryEmbeddingVectors(InputStream is, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        byte[] currentText;
        double[] currentVector;
        byte[] integerBuffer = new byte[Integer.BYTES];
        byte[] doubleBuffer = new byte[Double.BYTES];

        List<String> expressions = new ArrayList<>();
        List<double[]> vectors = new ArrayList<>();

        boolean textRead = false;
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.limit(bytesRead);
            while (bb.position() < bb.limit()) {
                if (!textRead) {
                    final int textLength = readInt(integerBuffer, bb, is);
                    currentText = new byte[textLength];
                    readNumberOfBytes(currentText, bb, is);

                    expressions.add(new String(currentText, StandardCharsets.UTF_8));
                    textRead = true;
                } else {
                    final int vectorLength = readInt(integerBuffer, bb, is);
                    currentVector = new double[vectorLength];
                    for (int i = 0; i < vectorLength; i++) {
                        currentVector[i] = readDouble(doubleBuffer, bb, is);
                    }
                    vectors.add(currentVector);

                    textRead = false;
                }
            }
        }
        return new ImmutablePair<>(expressions, vectors);
    }

    public static double readDouble(byte[] dest, ByteBuffer bb, InputStream is) throws IOException {
        readNumberOfBytes(dest, bb, is);
        // from https://stackoverflow.com/a/31637248/1314955
        // Makes sense: a double encoded two information, the exponent and the mantissa (significant), both as
        // an integer of itself
        int upper = (((dest[0] & 0xff) << 24)
                + ((dest[1] & 0xff) << 16)
                + ((dest[2] & 0xff) << 8) + ((dest[3] & 0xff) << 0));
        int lower = (((dest[4] & 0xff) << 24)
                + ((dest[5] & 0xff) << 16)
                + ((dest[6] & 0xff) << 8) + ((dest[7] & 0xff) << 0));
        return Double.longBitsToDouble((((long) upper) << 32)
                + (lower & 0xffffffffl));
    }

    public static int readInt(byte[] dest, ByteBuffer bb, InputStream is) throws IOException {
        readNumberOfBytes(dest, bb, is);
        return ((dest[0] & 0xff) << 24) | ((dest[1] & 0xff) << 16) | ((dest[2] & 0xff) << 8) | (dest[3] & 0xff);
    }

    public static void readNumberOfBytes(byte[] dest, ByteBuffer bb, InputStream is) throws IOException {
        int bytesRead = 0;
        while (bytesRead < dest.length) {
            for (; bb.position() < bb.limit() && bytesRead < dest.length; bytesRead++) {
                dest[bytesRead] = bb.get();
            }
            // check if we could read the whole integer. If not, we have exhausted the current buffer and need
            // to read the next chunk of data
            if (bytesRead < dest.length) {
                // read the next chunk of data right into the array wrapped by the passes ByteBuffer.
                // Then reset the ByteBuffer's position so we can start reading from it
                is.read(bb.array());
                bb.position(0);
            }
        }
    }
}
