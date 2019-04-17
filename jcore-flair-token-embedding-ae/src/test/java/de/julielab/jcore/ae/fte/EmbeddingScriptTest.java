package de.julielab.jcore.ae.fte;

import com.google.gson.Gson;
import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.ResultDecoders;
import de.julielab.ipc.javabridge.StdioBridge;
import org.assertj.core.data.Offset;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingScriptTest {
    private static final String SCRIPT_PATH = "src/main/resources/de/julielab/jcore/ae/fte/python/getEmbeddingScript.py";

    @Test
    public void testPythonEmbeddingScriptSimple() throws Exception {
        // Here we send a single sentence and do not specify specific words to return embedding vectors for,
        // so we expect vectors for all words to be returned.
        List<Map<String, Object>> sentences = new ArrayList<>();
        Map<String, Object> sentence = new HashMap<>();
        sentence.put("sentence", "Dysregulated inflammation leads to morbidity and mortality in neonates .");
        sentences.add(sentence);

        Gson gson = new Gson();
        final String sentencesJson = gson.toJson(sentences);
        Options<byte[]> options = new Options<>(byte[].class);
        options.setExecutable("python");
        options.setExternalProgramTerminationSignal("exit");
        options.setExternalProgramReadySignal("Script is ready");
        options.setTerminationSignalFromErrorStream("SyntaxError");
        StdioBridge<byte[]> bridge = new StdioBridge<>(options, "-u", SCRIPT_PATH, "flair:src/test/resources/gene_small_best_lm.pt");
        bridge.start();
        final Stream<byte[]> response = bridge.sendAndReceive(sentencesJson);
        final double[][] vectors = response.map(ResultDecoders.decodeVectors).findAny().get();
        bridge.stop();

        assertThat(vectors).hasSize(10);
        for (double[] vector : vectors) {
            // The vectors should all have a dimensionality of 1024
            assertThat(vector.length).isEqualTo(1024);
        }

        // Those values were output using print(token.embedding.numpy(), file=sys.stderr) in the script
        assertThat(vectors[0][0]).isCloseTo(1.8812446e-01, Offset.offset(0.00000001));
        assertThat(vectors[0][1]).isCloseTo(-1.0531080e-01, Offset.offset(0.00000001));
        assertThat(vectors[0][2]).isCloseTo(-1.6942955e-04, Offset.offset(0.00000001));
        assertThat(vectors[0][1021]).isCloseTo(5.4023921e-04, Offset.offset(0.00000001));
        assertThat(vectors[0][1022]).isCloseTo(-2.4853314e-03, Offset.offset(0.00000001));
        assertThat(vectors[0][1023]).isCloseTo(3.7118504e-04, Offset.offset(0.00000001));
    }

    @Test
    public void testPythonEmbeddingScriptSpecificVectorsResponse() throws Exception {
        // Here we send a single sentence and want the vectors of a two token back.
        List<Map<String, Object>> sentences = new ArrayList<>();
        Map<String, Object> sentence = new HashMap<>();
        sentence.put("sentence", "Dysregulated inflammation leads to morbidity and mortality in neonates .");
        sentence.put("tokenIndicesToReturn", new int[]{2, 7});
        sentences.add(sentence);

        Gson gson = new Gson();
        final String sentencesJson = gson.toJson(sentences);
        Options<byte[]> options = new Options<>(byte[].class);
        options.setExecutable("python");
        options.setExternalProgramTerminationSignal("exit");
        options.setExternalProgramReadySignal("Script is ready");
        options.setTerminationSignalFromErrorStream("SyntaxError");
        StdioBridge<byte[]> bridge = new StdioBridge<>(options, "-u", SCRIPT_PATH, "flair:src/test/resources/gene_small_best_lm.pt");
        bridge.start();
        final Stream<byte[]> response = bridge.sendAndReceive(sentencesJson);
        final double[][] vectors = response.map(ResultDecoders.decodeVectors).findAny().get();
        bridge.stop();

        assertThat(vectors).hasSize(2);
        for (int i = 0; i < vectors.length; i++) {
            // The vectors should all have a dimensionality of 1024
            assertThat(vectors[i].length).isEqualTo(1024);
        }

        // Those values were output using print(token.embedding.numpy(), file=sys.stderr) in the script
        assertThat(vectors[0][0]).isCloseTo(-0.16511102, Offset.offset(0.00000001));
        assertThat(vectors[0][1]).isCloseTo(0.59705925, Offset.offset(0.00000001));
        assertThat(vectors[0][2]).isCloseTo(-0.00156581, Offset.offset(0.00000001));

        assertThat(vectors[1][0]).isCloseTo(4.8699617e-01, Offset.offset(0.00000001));
        assertThat(vectors[1][1]).isCloseTo(2.7841574e-01, Offset.offset(0.00000001));
        assertThat(vectors[1][2]).isCloseTo(-3.3565212e-04, Offset.offset(0.00000001));
    }

    @Test
    public void testPythonEmbeddingScriptMultipleSentences() throws Exception {
        // Here we send two sentences of which we want to vectors for the first and all vectors of the second
        // sentence back.
        List<Map<String, Object>> sentences = new ArrayList<>();
        Map<String, Object> sentence = new HashMap<>();
        sentence.put("sentence", "Dysregulated inflammation leads to morbidity and mortality in neonates .");
        sentence.put("tokenIndicesToReturn", new int[]{2, 7});
        sentences.add(sentence);
        sentence = new HashMap<>();
        sentence.put("sentence", "97 healthy subjects were enrolled in the present study .");
        sentences.add(sentence);

        Gson gson = new Gson();
        final String sentencesJson = gson.toJson(sentences);
        Options<byte[]> options = new Options<>(byte[].class);
        options.setExecutable("python");
        options.setExternalProgramTerminationSignal("exit");
        options.setExternalProgramReadySignal("Script is ready");
        options.setTerminationSignalFromErrorStream("SyntaxError");
        StdioBridge<byte[]> bridge = new StdioBridge<>(options, "-u", SCRIPT_PATH, "flair:src/test/resources/gene_small_best_lm.pt");
        bridge.start();
        final Stream<byte[]> response = bridge.sendAndReceive(sentencesJson);
        final double[][] vectors = response.map(ResultDecoders.decodeVectors).findAny().get();
        bridge.stop();

        assertThat(vectors).hasSize(12);

    }
}
