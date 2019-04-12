package de.julielab.jcore.consumer.ew;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class VectorOperations {
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
