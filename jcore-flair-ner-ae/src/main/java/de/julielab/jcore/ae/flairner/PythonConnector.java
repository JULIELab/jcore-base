package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.types.Sentence;

import java.io.IOException;
import java.util.stream.Stream;

public interface PythonConnector {
    Stream<TaggedEntity> tagSentences(Stream<Sentence> sentences) throws IOException;
    void start() throws IOException;
    void shutdown() throws InterruptedException;
}
