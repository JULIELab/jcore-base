package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.types.Sentence;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.stream.Stream;

public interface PythonConnector {
    NerTaggingResponse tagSentences(Stream<Sentence> sentences) throws IOException;
    void start() throws IOException;
    void shutdown() throws InterruptedException;
}
