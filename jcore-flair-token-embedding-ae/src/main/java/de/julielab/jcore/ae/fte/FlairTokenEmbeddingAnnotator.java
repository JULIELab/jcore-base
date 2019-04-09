package de.julielab.jcore.ae.fte;

import de.julielab.ipc.javabridge.Options;
import de.julielab.ipc.javabridge.StdioBridge;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ResourceMetaData(name = "JCoRe Flair Token Embedding Annotator", description = "Adds the Flair compatible embedding vectors to the token annotations.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Sentence", "de.julielab.jcore.types.Token"})
public class FlairTokenEmbeddingAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_EMBEDDING_PATH = "EmbeddingPath";
    public static final String PARAM_COMPUTATION_FILTER = "ComputationFilter";
    private final static Logger log = LoggerFactory.getLogger(FlairTokenEmbeddingAnnotator.class);
    @ConfigurationParameter(name = PARAM_EMBEDDING_PATH, description = "Path to a Flair compatible embedding file.")
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
            String script = IOUtils.toString(getClass().getResourceAsStream("/de/julielab/jcore/ae/fte/python/getEmbeddingScript.py"));
            flairBridge = new StdioBridge<>(options, "-u", "-c", script);
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
        if (!StringUtils.isBlank(computationFilter)) {
            Type type = aJCas.getTypeSystem().getType(computationFilter);
            if (type == null) {
                throw new AnalysisEngineProcessException(new IllegalArgumentException("The type " + computationFilter + " was not found in the type system."));
            }
            if (!aJCas.getAnnotationIndex(type).iterator().hasNext())
                return;
        }


    }

}
