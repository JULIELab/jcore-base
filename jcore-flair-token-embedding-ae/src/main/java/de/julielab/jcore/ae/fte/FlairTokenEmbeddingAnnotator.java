package de.julielab.jcore.ae.fte;

import com.google.gson.Gson;
import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.ResultDecoders;
import de.julielab.ipc.javabridge.StdioBridge;
import de.julielab.jcore.types.EmbeddingVector;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReAnnotationIndex;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import de.julielab.jcore.utility.index.JCoReSetAnnotationIndex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ResourceMetaData(name = "JCoRe Flair Token Embedding Annotator", description = "Adds the Flair compatible embedding vectors to the token annotations.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Sentence", "de.julielab.jcore.types.Token"}, outputs = {"de.julielab.jcore.types.EmbeddingVector"})
public class FlairTokenEmbeddingAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_EMBEDDING_PATH = "EmbeddingPath";
    public static final String PARAM_COMPUTATION_FILTER = "ComputationFilter";
    public static final String PARAM_EMBEDDING_SOURCE  = "EmbeddingSource";
    public static final String PARAM_PYTHON_EXECUTABLE = "PythonExecutable";
    private final static Logger log = LoggerFactory.getLogger(FlairTokenEmbeddingAnnotator.class);
    /**
     * The number of documents after the embedding computation time is output
     */
    private static final int TIME_OUTPUT_INTERVAL = 1000;
    @ConfigurationParameter(name = PARAM_EMBEDDING_PATH, description = "Path to a Flair compatible embedding file. Since flair supports a range of different embeddings, a type prefix is required. The syntax is 'prefix:<path or built-in flair embedding name>. The possible prefixes are 'word', 'char', 'bytepair', 'flair', 'bert', 'elmo'.")
    private String embeddingPath;
    @ConfigurationParameter(name = PARAM_COMPUTATION_FILTER, mandatory = false, description = "This parameter may be set to a fully qualified annotation type. If given, only for documents containing at least one annotation of this type embeddings will be retrieved from the computing flair python script. However, for contextualized embeddings, all embedding vectors are computed anyway and the the I/O cost is minor in comparison to the embedding computation. Thus, setting this parameter will most probably only result in small time savings.")
    private String computationFilter;
    @ConfigurationParameter(name=PARAM_EMBEDDING_SOURCE, mandatory =  false, description = "The value of this parameter will be set to the source feature of the EmbeddingVector annotation instance created on the tokens. If left blank, the value of the " + PARAM_EMBEDDING_PATH + " will be used.")
    private String embeddingSource;
    @ConfigurationParameter(name=PARAM_PYTHON_EXECUTABLE, mandatory = false, description = "The path to the python executable. Required is a python verion >=3.6.")
    private String pythonExecutable;
    private StdioBridge<byte[]> flairBridge;
    private Gson gson;
    private long embeddingRequestTime;
    private long embeddingRequestTimeForLastInterval;
    private int docsProcessed;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        embeddingPath = (String) aContext.getConfigParameterValue(PARAM_EMBEDDING_PATH);
        computationFilter = (String) aContext.getConfigParameterValue(PARAM_COMPUTATION_FILTER);
        embeddingSource = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_EMBEDDING_SOURCE)).orElse(embeddingPath);

        Optional<String>  pythonExecutableOpt = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_PYTHON_EXECUTABLE));
        if (!pythonExecutableOpt.isPresent()) {
            log.debug("No python executable given in the component descriptor, trying to read PYTHON environment variable." );
            final String pythonExecutableEnv = System.getenv("PYTHON");
            if (pythonExecutableEnv != null) {
                pythonExecutable = pythonExecutableEnv;
                log.info("Python executable: {} (from environment variable PYTHON).", pythonExecutable);
            }
        } else {
            pythonExecutable = pythonExecutableOpt.get();
            log.info("Python executable: {} (from descriptor)", pythonExecutable);
        }
        if (pythonExecutable == null) {
            pythonExecutable = "python3.6";
            log.info("Python executable: {} (default)", pythonExecutable);
        }

        try {
            final Options<byte[]> options = new Options<>(byte[].class);
            options.setExecutable(pythonExecutable);
            options.setExternalProgramTerminationSignal("exit");
            options.setExternalProgramReadySignal("Script is ready");
            options.setTerminationSignalFromErrorStream("SyntaxError");
            String script = IOUtils.toString(getClass().getResourceAsStream("/de/julielab/jcore/ae/fte/python/getEmbeddingScript.py"), StandardCharsets.UTF_8);
            flairBridge = new StdioBridge<>(options, "-u", "-c", script, embeddingPath);
            flairBridge.start();
        } catch (IOException e) {
            log.error("Could not create the IO bridge object.", e);
            throw new ResourceInitializationException(e);
        }
        gson = new Gson();
        docsProcessed = 0;
        embeddingRequestTime = 0;
        embeddingRequestTimeForLastInterval = 0;
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {

        List<Token> tokenToAddEmbeddingsTo = new ArrayList<>();
        JCoReSetAnnotationIndex<Annotation> filterAnnotationIndex = null;
        if (!StringUtils.isBlank(computationFilter)) {
            Type type = aJCas.getTypeSystem().getType(computationFilter);
            if (type == null) {
                throw new AnalysisEngineProcessException(new IllegalArgumentException("The type " + computationFilter + " was not found in the type system."));
            }
            if (!aJCas.getAnnotationIndex(type).iterator().hasNext())
                return;
            filterAnnotationIndex = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator(), aJCas, type);
        }
        final String json = constructEmbeddingRequest(aJCas, tokenToAddEmbeddingsTo, filterAnnotationIndex);
        try {
            long time = System.currentTimeMillis();
            final Optional<double[][]> any = flairBridge.sendAndReceive(json).map(ResultDecoders.decodeVectors).findAny();
            time = System.currentTimeMillis() - time;
            log.trace("Sending and receiving token embeddings took {} ms", time);
            embeddingRequestTime += time;
            embeddingRequestTimeForLastInterval += time;
            writeEmbeddingsToCas(aJCas, tokenToAddEmbeddingsTo, any);
        } catch (InterruptedException e) {
            log.error("Computation of embedding vectors was interrupted", e);
            throw new AnalysisEngineProcessException(e);
        }

        ++docsProcessed;
        if (docsProcessed % TIME_OUTPUT_INTERVAL == 0) {
            if (log.isDebugEnabled())
                log.debug("Embedding computation for the last {} documents took {}ms (avg: {}ms). Total time for all {} processed documents until here: {}ms ({}s)", TIME_OUTPUT_INTERVAL, embeddingRequestTimeForLastInterval, embeddingRequestTimeForLastInterval / TIME_OUTPUT_INTERVAL, docsProcessed, embeddingRequestTime, embeddingRequestTime / 60);
            embeddingRequestTimeForLastInterval = 0;
        }

    }

    private void writeEmbeddingsToCas(JCas aJCas, List<Token> tokenToAddEmbeddingsTo, Optional<double[][]> embeddingOptional) {
        if (embeddingOptional.isPresent()) {
            final double[][] embeddingVectors = embeddingOptional.get();
            for (int i = 0; i < tokenToAddEmbeddingsTo.size(); i++) {
                Token token = tokenToAddEmbeddingsTo.get(i);
                double[] embedding = embeddingVectors[i];
                final DoubleArray embeddingArray = new DoubleArray(aJCas, embedding.length);
                embeddingArray.copyFromArray(embedding, 0, 0, embedding.length);
                final EmbeddingVector casEmbedding = new EmbeddingVector(aJCas, token.getBegin(), token.getEnd());
                casEmbedding.setSource(embeddingSource);
                casEmbedding.setVector(embeddingArray);
                token.setEmbeddingVectors(JCoReTools.addToFSArray(token.getEmbeddingVectors(), casEmbedding));
            }
        }
    }

    private String constructEmbeddingRequest(JCas aJCas, List<Token> tokenToAddEmbeddingsTo, JCoReSetAnnotationIndex<Annotation> filterAnnotationIndex) {
        final Map<Sentence, Collection<Token>> tokenBySentence = JCasUtil.indexCovered(aJCas, Sentence.class, Token.class);
        List<Map<String, Object>> sentencesAndIndices = new ArrayList<>();
        for (Annotation sentence : aJCas.getAnnotationIndex(Sentence.type)) {
            // We will add to this list only if there are only specific tokens we want to set the embedding vectors for.
            // Otherwise, we leave it empty which signals to return the embeddings vectors of all tokens.
            List<Integer> tokenIndicesToSet = filterAnnotationIndex != null ? new ArrayList<>() : Collections.emptyList();
            int tokenIndex = 0;
            StringBuilder sentenceTextSb = new StringBuilder();
            for (Token token : tokenBySentence.get(sentence)) {
                sentenceTextSb.append(token.getCoveredText()).append(" ");
                if (filterAnnotationIndex != null) {
                    // Check if there are filter annotations overlapping with this token
                    if (!filterAnnotationIndex.searchSubset(token).isEmpty()) {
                        tokenIndicesToSet.add(tokenIndex);
                        tokenToAddEmbeddingsTo.add(token);
                    }
                } else {
                    tokenToAddEmbeddingsTo.add(token);
                }
                ++tokenIndex;
            }
            sentenceTextSb.deleteCharAt(sentenceTextSb.length()-1);
            Map<String, Object> sentenceAndIndices = new HashMap<>();
            sentenceAndIndices.put("sentence", sentenceTextSb.toString());
            sentenceAndIndices.put("tokenIndicesToReturn", tokenIndicesToSet);
            sentencesAndIndices.add(sentenceAndIndices);
        }
        return gson.toJson(sentencesAndIndices);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        if (log.isDebugEnabled())
            log.debug("The total time for embedding computation, including I/O, was {}ms ({}s)", embeddingRequestTime, embeddingRequestTime / 1000);
        try {
            flairBridge.stop();
        } catch (InterruptedException | IOException e) {
            log.error("Exception when trying shut down IO bridge to the python embedding computation script", e);
            throw new AnalysisEngineProcessException(e);
        }
    }
}
