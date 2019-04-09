package de.julielab.jcore.ae.flairner;

import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.StdioBridge;
import de.julielab.jcore.types.Sentence;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

public class StdioPythonConnector implements PythonConnector {
    private final static Logger log = LoggerFactory.getLogger(StdioPythonConnector.class);
    private final StdioBridge<String> bridge;

    public StdioPythonConnector(String languageModelPath) throws IOException {
        Options params = new Options(String.class);
        params.setExecutable("python");
        params.setExternalProgramTerminationSignal("exit");
        params.setMultilineResponseDelimiter("tagging finished");
        String script = IOUtils.toString(getClass().getResourceAsStream("/de/julielab/jcore/ae/flairner/python/nerScript.py"), StandardCharsets.UTF_8);
        bridge = new StdioBridge<>(params, "-u", "-c", script, languageModelPath);
    }

    @Override
    public Stream<TaggedEntity> tagSentences(Stream<Sentence> sentences) throws IOException {
        return sentences.flatMap(sentence -> {
            try {
                return bridge.sendAndReceive(sentence.getId() + "\t" + sentence.getCoveredText());
            } catch (InterruptedException e) {
                log.error("Python communication was interrupted", e);
            }
            return null;
        })
                .filter(Objects::nonNull)
                .map(entityrecord -> entityrecord.split("\t"))
                .map(split -> new TaggedEntity(split[0], split[1], Integer.valueOf(split[2]), Integer.valueOf(split[3])));
    }

    @Override
    public void start() throws IOException {
        bridge.start();
    }

    @Override
    public void shutdown() throws InterruptedException {
        bridge.stop();
    }
}
