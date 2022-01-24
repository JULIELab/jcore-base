package de.julielab.jcore.ae.banner;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Sentence;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.pubmed.InternalReference;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jcore.utility.index.JCoReOverlapAnnotationIndex;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author siddhartha, faessler
 */
@TypeCapability(inputs = "de.julielab.jcore.types.Sentence", outputs = "de.julielab.jcore.types.Gene")
public class BANNERAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_CONFIG_FILE = "ConfigFile";
    public static final String PARAM_TYPE_MAPPING = "TypeMapping";
    private final static Logger log = LoggerFactory.getLogger(BANNERAnnotator.class);
    private Tokenizer tokenizer;
    private DictionaryTagger dictionary;
    private HierarchicalConfiguration config;
    // Dataset dataset;
    private EngLemmatiser lemmatiser;
    private Tagger posTagger;
    private CRFTagger tagger;
    private PostProcessor postProcessor;

    @ConfigurationParameter(name = PARAM_CONFIG_FILE, description = "The XML configuration file for BANNER.")
    private String configFilePath;
    @ConfigurationParameter(name = PARAM_TYPE_MAPPING, mandatory = false, description = "A list of mappings from entity labels to UIMA types in the form <label>=<fully qualified type name>. If not given, all entities will be realized as EntityMention instances.")
    private String[] typeMappings;

    private Map<String, String> typeMap;
    private InputStream modelIs;
    private String modelFilename;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        try {
            configFilePath = (String) aContext.getConfigParameterValue(PARAM_CONFIG_FILE);
            typeMappings = (String[]) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_TYPE_MAPPING))
                    .orElse(new String[0]);
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                log.debug("Found configuration file {}", configFile);
                config = new XMLConfiguration(configFile);
            } else {
                String classpathAddress = configFilePath.startsWith("/") ? configFilePath : "/" + configFilePath;
                log.debug(
                        "Did not find configuration file as regular file at {}. Trying as classpath resource with address {}",
                        configFile, classpathAddress);
                InputStream is = getClass().getResourceAsStream(classpathAddress);
                if (is != null) {
                    log.debug("Found configuration file as classpath resource {}. Loading configuration.",
                            classpathAddress);
                    config = new XMLConfiguration();
                    ((XMLConfiguration) config).load(is);
                } else {
                    throw new ResourceInitializationException(ResourceInitializationException.COULD_NOT_ACCESS_DATA,
                            new Object[]{configFilePath});
                }
            }
            typeMap = Stream.of(typeMappings).map(m -> m.split("\\s*=\\s*"))
                    .collect(Collectors.toMap(s -> s[0], s -> s[1]));

            tokenizer = BANNER.getTokenizer(config);
            dictionary = BANNER.getDictionary(config);
            lemmatiser = BANNER.getLemmatiser(config);
            posTagger = BANNER.getPosTagger(config);
            postProcessor = BANNER.getPostProcessor(config);

            SubnodeConfiguration subConfig = config.configurationAt("banner.eval");
            modelFilename = subConfig.getString("modelFilename");

            if (new File(modelFilename).exists()) {
                modelIs = new FileInputStream(modelFilename);
            } else {
                modelIs = getClass()
                        .getResourceAsStream(modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename);
            }
            if (null == modelIs)
                throw new ResourceInitializationException(ResourceInitializationException.COULD_NOT_ACCESS_DATA,
                        new Object[]{modelFilename});
            log.info("{}: {}", PARAM_CONFIG_FILE, configFilePath);
            log.info("{}: {}", PARAM_TYPE_MAPPING, Arrays.toString(typeMappings));
            log.info("Model: {}", modelFilename);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        if (tagger == null) {
            try {
                // We need to instantiate the tagger in process() because process() is called by the Thread that
                // executes the actual tagging. The initialize() method, on the other hand, is called by the
                // main thread. However, the LemmaPOS class of BANNER maintains an internal map that associates
                // threads with the lemmatiser and the posTagger. This is necessary because even though the
                // model is deserialized multiple times, the FeatureSet#pipe field seems to be always the
                // exact same instance, containing a single instance of LemmaPOS (again, despite reading the model
                // file and deserializing it multiple times). This is why the Thread -> resources map was added.
                tagger = CRFTagger.load(modelIs, lemmatiser, posTagger, dictionary);
            } catch (IOException e) {
                log.error("Could not load the BANNER model at {}", modelFilename, e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        String docId = "<unknown>";
        try {
            docId = JCoReTools.getDocId(jcas);
            JCoReOverlapAnnotationIndex<InternalReference> intRefIndex = new JCoReOverlapAnnotationIndex<>(jcas, InternalReference.type);
            FSIterator<Annotation> sentIt = jcas.getAnnotationIndex(de.julielab.jcore.types.Sentence.type).iterator();
            int geneCount = 0;
            int sentCount = 0;
            while (sentIt.hasNext()) {
                de.julielab.jcore.types.Sentence jcoreSentence = (de.julielab.jcore.types.Sentence) sentIt.next();
                int sentenceBegin = jcoreSentence.getBegin();
                String sentenceId = jcoreSentence.getId() != null ? jcoreSentence.getId() : docId + ": " + sentCount++;
                Sentence sentence = new Sentence(sentenceId, docId, jcoreSentence.getCoveredText());
                try {
                    sentence = BANNER.process(tagger, tokenizer, postProcessor, sentence);
                } catch (Exception e) {
                    log.error("Exception while running BANNER on sentence {}", jcoreSentence.getCoveredText(), e);
                    throw e;
                }
                for (Mention mention : sentence.getMentions()) {
                    EntityType entityType = mention.getEntityType();
                    String typeName = typeMap.getOrDefault(entityType.getText(),
                            EntityMention.class.getCanonicalName());
                    Annotation a = JCoReAnnotationTools.getAnnotationByClassName(jcas, typeName);
                    int originalBegin = sentenceBegin + mention.getStartChar();
                    int originalEnd = sentenceBegin + mention.getEndChar();
                    a.setBegin(originalBegin);
                    a.setEnd(originalEnd);
                    excludeReferenceAnnotationSpans(a, intRefIndex);
                    if (a.getEnd() <= a.getBegin()) {
                        // It seems there was nothing left of a gene mention outside the internal reference; skip
                        continue;
                    }
                    if (a instanceof de.julielab.jcore.types.Annotation) {
                        de.julielab.jcore.types.Annotation jcoreA = (de.julielab.jcore.types.Annotation) a;
                        jcoreA.setId("BANNER, " + docId + ": " + geneCount++);
                        jcoreA.setComponentId(BANNERAnnotator.class.getCanonicalName());
                        jcoreA.setConfidence(String.valueOf(mention.getProbability()));
                    }
                    if (a instanceof EntityMention) {
                        EntityMention e = (EntityMention) a;
                        e.setSpecificType(entityType.getText());
                    }
                    a.addToIndexes();
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred while running the BANNER annotator on document {}.", docId, e);
            throw new AnalysisEngineProcessException(e);
        }
    }

    /**
     * Internal references can actually look like a part of a gene, e.g. "filament19" where "19" is a reference.
     * Exclude those spans from the gene mentions.
     * @param a The gene annotation.
     * @param intRefIndex The reference index.
     */
    private void excludeReferenceAnnotationSpans(Annotation a, JCoReOverlapAnnotationIndex<? extends Annotation> intRefIndex) {
        List<? extends Annotation> annotationsInGene = intRefIndex.search(a);
        for (Annotation overlappingAnnotation : annotationsInGene) {
            if (overlappingAnnotation.getBegin() == a.getBegin()) {
                a.setBegin(overlappingAnnotation.getEnd());
            }
            if (overlappingAnnotation.getEnd() == a.getEnd()) {
                a.setEnd(overlappingAnnotation.getBegin());
            }
            // Set zero-character spans on genes that are completely enclosed by a reference. Those are cases
            // like, for instance, "Supplementary Figs. S12 and S13, Tables S2 and S3" where S12, S13 and even
            // Tables S2 are annotated as genes.
            if (overlappingAnnotation.getBegin() <= a.getBegin() && overlappingAnnotation.getEnd() >= a.getEnd()) {
                a.setBegin(0);
                a.setEnd(0);
            }
        }
    }
}
