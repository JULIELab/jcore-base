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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StdioPythonConnector implements PythonConnector {
    private final static Logger log = LoggerFactory.getLogger(StdioPythonConnector.class);
    private final StdioBridge<byte[]> bridge;

    public StdioPythonConnector(String languageModelPath, String pythonExecutable, FlairNerAnnotator.StoreEmbeddings storeEmbeddings) throws IOException {
        Options params = new Options(byte[].class);
        params.setExecutable(pythonExecutable);
        params.setExternalProgramReadySignal("Ready for tagging.");
        params.setExternalProgramTerminationSignal("exit");
//        params.setMultilineResponseDelimiter("tagging finished");
        params.setTerminationSignalFromErrorStream("SyntaxError");
        String script = IOStreamUtilities.getStringFromInputStream(getClass().getResourceAsStream("/de/julielab/jcore/ae/flairner/python/nerScript.py"));
        bridge = new StdioBridge<>(params, "-u", "-c", script, languageModelPath, storeEmbeddings.name());
    }

    @Override
    public Stream<TaggedEntity> tagSentences(Stream<Sentence> sentences) {
        final Stream<byte[]> byteResponse = sentences.flatMap(sentence -> {
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
                .filter(Objects::nonNull);

        final Iterator<byte[]> bytesIt = byteResponse.iterator();
        final Stream.Builder<TaggedEntity> taggedEntityStreamBuilder = Stream.builder();
        while (bytesIt.hasNext()) {
            byte[] sentenceResponseBytes = bytesIt.next();
            final ByteBuffer bb = ByteBuffer.wrap(sentenceResponseBytes);
            final int numEntities = bb.getInt();
            for (int i = 0; i < numEntities; i++) {
                final int taggedEntityResponseLength = bb.getInt();
                byte[] taggedEntityRepsonseBytes = new byte[taggedEntityResponseLength];
                bb.get(taggedEntityRepsonseBytes);
                final ByteArrayInputStream bais = new ByteArrayInputStream(taggedEntityRepsonseBytes);
                try {
                    final String taggedEntityString = IOStreamUtilities.getStringFromInputStream(bais);
                    final String[] taggedEntityRecord = taggedEntityString.split("\\t");
                    taggedEntityStreamBuilder.accept(new TaggedEntity(taggedEntityRecord[0], taggedEntityRecord[1], Integer.valueOf(taggedEntityRecord[2]), Integer.valueOf(taggedEntityRecord[3])));
                } catch (IOException e) {
                    log.error("Could not convert the tagged entity response bytes into a string.");
                }
            }
        }
        return taggedEntityStreamBuilder.build();
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
