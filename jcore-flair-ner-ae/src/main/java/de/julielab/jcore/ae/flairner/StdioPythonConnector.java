package de.julielab.jcore.ae.flairner;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.StdioBridge;
import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StdioPythonConnector implements PythonConnector {
    private final static Logger log = LoggerFactory.getLogger(StdioPythonConnector.class);
    private final StdioBridge<byte[]> bridge;

    public StdioPythonConnector(String languageModelPath, String pythonExecutable, FlairNerAnnotator.StoreEmbeddings storeEmbeddings, int gpuNum) throws IOException {
        Options params = new Options(byte[].class);
        params.setExecutable(pythonExecutable);
        params.setExternalProgramReadySignal("Ready for tagging.");
        params.setExternalProgramTerminationSignal("exit");
        params.setTerminationSignalFromErrorStream("SyntaxError");
        String script = IOStreamUtilities.getStringFromInputStream(getClass().getResourceAsStream("/de/julielab/jcore/ae/flairner/python/nerScript.py"));
        bridge = new StdioBridge<>(params, "-u", "-c", script, languageModelPath, storeEmbeddings.name(), String.valueOf(gpuNum));
    }

    @Override
    public NerTaggingResponse tagSentences(Stream<Sentence> sentences) throws AnalysisEngineProcessException {
        try {
            final StringWriter sw = new StringWriter();
            final JsonGenerator generator = new JsonFactory().createGenerator(sw);
            generator.writeStartArray();
            sentences.forEach(sentence -> {
                try {
                    final JCas jCas = sentence.getCAS().getJCas();
                    final FSIterator<Token> tokensInSentence = jCas.<Token>getAnnotationIndex(Token.type).subiterator(sentence);
                    final String tokenizedSentenceText = StreamSupport.stream(Spliterators.spliteratorUnknownSize(tokensInSentence, 0), false).map(Annotation::getCoveredText).collect(Collectors.joining(" "));
                    if (!tokenizedSentenceText.isBlank()) {
                        generator.writeStartObject();
                        generator.writeFieldName("sid");
                        generator.writeString(sentence.getId());
                        generator.writeFieldName("text");
                        generator.writeString(tokenizedSentenceText);
                        generator.writeEndObject();
                    }
                } catch (CASException e) {
                    log.error("Could not retrieve the JCas from the CAS", e);
                } catch (IOException e) {
                    log.error("Could not write JSON", e);
                }
            });

            generator.writeEndArray();
            generator.close();
            List<TokenEmbedding> embeddings = new ArrayList<>();
            final Iterator<byte[]> bytesIt;
            bytesIt = bridge.sendAndReceive(sw.toString()).iterator();

            final List<TaggedEntity> taggedEntities = new ArrayList<>();
            while (bytesIt.hasNext()) {
                byte[] sentenceResponseBytes = bytesIt.next();
                final ByteBuffer bb = ByteBuffer.wrap(sentenceResponseBytes);
                final int numEntities = bb.getInt();
                for (int i = 0; i < numEntities; i++) {
                    final int taggedEntityResponseLength = bb.getInt();
                    byte[] taggedEntityRepsonseBytes = new byte[taggedEntityResponseLength];
                    bb.get(taggedEntityRepsonseBytes);
                    final String taggedEntityString = new String(taggedEntityRepsonseBytes, StandardCharsets.UTF_8);
                    final String[] taggedEntityRecord = taggedEntityString.split("\\t");
                    taggedEntities.add(new TaggedEntity(taggedEntityRecord[0], taggedEntityRecord[1], Integer.valueOf(taggedEntityRecord[2]), Integer.valueOf(taggedEntityRecord[3])));
                }
                final int numEmbeddingVectors = bb.getInt();
                final int vectorLength = bb.getInt();
                for (int i = 0; i < numEmbeddingVectors; i++) {
                    final int sentenceIdLength = bb.getInt();
                    final byte[] sentenceIdBytes = new byte[sentenceIdLength];
                    bb.get(sentenceIdBytes);
                    final String sid = new String(sentenceIdBytes, StandardCharsets.UTF_8);
                    final int tokenId = bb.getInt();
                    double[] vector = new double[vectorLength];
                    for (int j = 0; j < vectorLength; j++) {
                        vector[j] = bb.getDouble();
                    }
                    final TokenEmbedding tokenEmbedding = new TokenEmbedding(sid, tokenId, vector);
                    embeddings.add(tokenEmbedding);
                }
            }
            return new NerTaggingResponse(taggedEntities, embeddings);
        } catch (InterruptedException e) {
            log.error("Python communication was interrupted", e);
            throw new AnalysisEngineProcessException(e);
        } catch (IOException e) {
            log.error("IOException occurred", e);
            throw new AnalysisEngineProcessException(e);
        }
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
