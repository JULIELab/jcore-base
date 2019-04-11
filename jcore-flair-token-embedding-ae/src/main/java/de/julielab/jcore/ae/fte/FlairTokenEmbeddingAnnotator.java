package de.julielab.jcore.ae.fte;

import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.StdioBridge;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
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
@TypeCapability(inputs = {"de.julielab.jcore.types.Sentence", "de.julielab.jcore.types.Token"})
public class FlairTokenEmbeddingAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_EMBEDDING_PATH = "EmbeddingPath";
    public static final String PARAM_COMPUTATION_FILTER = "ComputationFilter";
    private final static Logger log = LoggerFactory.getLogger(FlairTokenEmbeddingAnnotator.class);
    @ConfigurationParameter(name = PARAM_EMBEDDING_PATH, description = "Path to a Flair compatible embedding file. Since flair supports a range of different embeddings, a type prefix is required. The syntax is 'prefix:<path or built-in flair embedding name>. The possible prefixes are 'word', 'char', 'bytepair', 'flair', 'bert', 'elmo'.")
    private String embeddingPath;
    @ConfigurationParameter(name = PARAM_COMPUTATION_FILTER, mandatory = false, description = "This parameter may be set to a fully qualified annotation type. If given, only for documents containing at least one annotation of this type embeddings will be computed.")
    private String computationFilter;

    private StdioBridge<byte[]> flairBridge;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        embeddingPath = (String) aContext.getConfigParameterValue(PARAM_EMBEDDING_PATH);
        computationFilter = (String) aContext.getConfigParameterValue(PARAM_COMPUTATION_FILTER);

        try {
            final Options<byte[]> options = new Options<>(byte[].class);
            String script = IOUtils.toString(getClass().getResourceAsStream("/de/julielab/jcore/ae/fte/python/getEmbeddingScript.py"), StandardCharsets.UTF_8);
            flairBridge = new StdioBridge<>(options, "-u", "-c", script, embeddingPath);
        } catch (IOException e) {
            log.error("Could not create the IO bridge object.", e);
            throw new ResourceInitializationException(e);
        }

    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {

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
        final Map<Sentence, Collection<Token>> tokenBySentence = JCasUtil.indexCovered(aJCas, Sentence.class, Token.class);
        for (Annotation sentence : aJCas.getAnnotationIndex(Sentence.type)) {
            // We will add to this list only if there are only specific tokens we want to set the embedding vectors for.
            // Otherwise, we leave it empty which signals to return the embeddings vectors of all tokens.
            List<Integer> tokenIndicesToSet = filterAnnotationIndex != null ? new ArrayList<>() : Collections.emptyList();
            int tokenIndex = 0;
            for (Token token : tokenBySentence.get(sentence)) {
                if (filterAnnotationIndex != null) {
                    // Check if there are filter annotations overlapping with this token
                    if (!filterAnnotationIndex.searchSubset(token).isEmpty())
                        tokenIndicesToSet.add(tokenIndex);
                }
            }
            ++tokenIndex;
        }


    }

}
