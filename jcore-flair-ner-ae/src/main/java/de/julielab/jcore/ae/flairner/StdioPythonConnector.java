package de.julielab.jcore.ae.flairner;

import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.StdioBridge;
import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StdioPythonConnector implements PythonConnector {
    private final static Logger log = LoggerFactory.getLogger(StdioPythonConnector.class);
    private final StdioBridge<String> bridge;

    public StdioPythonConnector(String languageModelPath, String pythonExecutable) throws IOException {
        Options params = new Options(String.class);
        params.setExecutable(pythonExecutable);
        params.setExternalProgramReadySignal("Ready for tagging.");
        params.setExternalProgramTerminationSignal("exit");
        params.setMultilineResponseDelimiter("tagging finished");
        params.setTerminationSignalFromErrorStream("SyntaxError");
        String script = IOStreamUtilities.getStringFromInputStream(getClass().getResourceAsStream("/de/julielab/jcore/ae/flairner/python/nerScript.py"));
        bridge = new StdioBridge<>(params, "-u", "-c", script, languageModelPath);
    }

    @Override
    public Stream<TaggedEntity> tagSentences(Stream<Sentence> sentences) {
        return sentences.flatMap(sentence -> {
            try {
                final JCas jCas = sentence.getCAS().getJCas();
                final FSIterator<Token> tokensInSentence = jCas.<Token>getAnnotationIndex(Token.type).subiterator(sentence);
                final String tokenizedSentenceText = StreamSupport.stream(Spliterators.spliteratorUnknownSize(tokensInSentence, 0), false).map(Annotation::getCoveredText).collect(Collectors.joining(" "));
                return bridge.sendAndReceive(sentence.getId() + "\t" + tokenizedSentenceText);
            } catch (InterruptedException e) {
                log.error("Python communication was interrupted", e);
            } catch (CASException e) {
                log.error("Could not retrieve the JCas from the CAS", e);
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
        try {
            bridge.stop();
        } catch (IOException e) {
            log.error("Exception while stopping external process", e);
        }
    }
}
