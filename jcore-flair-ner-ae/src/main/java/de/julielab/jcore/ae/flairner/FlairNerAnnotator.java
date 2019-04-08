package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.ae.annotationadder.AnnotationAdderAnnotator;
import de.julielab.jcore.ae.annotationadder.AnnotationAdderConfiguration;
import de.julielab.jcore.ae.annotationadder.AnnotationAdderHelper;
import de.julielab.jcore.ae.annotationadder.AnnotationListAdder;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ResourceMetaData(name = "JCoRe Flair Named Entity Recognizer", description = "This component starts a child process to a python interpreter and loads a Flair sequence tagging model. Sentences are taken from the CAS, sent to Flair for tagging and the results are written into the CAS. The annotation type to use can be configured. It must be a subtype of de.julielab.jcore.types.EntityMention. The tag of each entity is written to the specificType feature.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Sentence", "de.julielab.jcore.types.Token"})
public class FlairNerAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_ANNOTATION_TYPE = "AnnotationType";
    public static final String PARAM_FLAIR_MODEL = "FlairModel";

    private final static Logger log = LoggerFactory.getLogger(FlairNerAnnotator.class);
    private PythonConnector connector;

    @ConfigurationParameter(name = PARAM_ANNOTATION_TYPE, description = "The UIMA type of which annotations should be created, e.g. de.julielab.jcore.types.EntityMention, of which the given type must be a subclass of. The tag of the entities is written to the specificType feature.")
    private String entityClass;
    @ConfigurationParameter(name = PARAM_FLAIR_MODEL, description = "Path to the Flair sequence tagger model.")
    private String flairModel;
    private AnnotationAdderConfiguration adderConfig;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        entityClass = (String) aContext.getConfigParameterValue(PARAM_ANNOTATION_TYPE);
        flairModel = (String) aContext.getConfigParameterValue(PARAM_FLAIR_MODEL);
        connector = new StdioPythonConnector(flairModel);
        try {
            connector.start();
        } catch (IOException e) {
            log.error("Could not start the python connector", e);
            throw new ResourceInitializationException(e);
        }

        adderConfig = new AnnotationAdderConfiguration();
        adderConfig.setOffsetMode(AnnotationAdderAnnotator.OffsetMode.TOKEN);
        adderConfig.setDefaultUimaType(entityClass);
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        int i = 0;
        final AnnotationIndex<Sentence> sentIndex = aJCas.getAnnotationIndex(Sentence.class);
        Map<String, Sentence> sentenceMap = new HashMap<>();
        for (Sentence sentence : sentIndex) {
            if (sentence.getId() == null)
                sentence.setId("s" + i);
            sentenceMap.put(sentence.getId(), sentence);
        }
        try {
            final AnnotationAdderHelper helper = new AnnotationAdderHelper();
            final Stream<TaggedEntity> taggedEntities = connector.tagSentences(StreamSupport.stream(sentIndex.spliterator(), false));
            for (TaggedEntity entity : (Iterable<TaggedEntity>) () -> taggedEntities.iterator()) {
                final Sentence sentence = sentenceMap.get(entity.getDocumentId());
                int sbegin = sentence.getBegin();
                EntityMention em = (EntityMention) JCoReAnnotationTools.getAnnotationByClassName(aJCas, entityClass);
                helper.setAnnotationOffsets(em, entity, adderConfig);
                em.setBegin(em.getBegin() + sbegin);
                em.setEnd(em.getEnd() + sbegin);
                em.setSpecificType(entity.getTag());
                em.setComponentId(FlairNerAnnotator.class.getSimpleName());
                em.addToIndexes();
            }
        } catch (IOException e) {
            log.error("Could not tag entities", e);
            throw new AnalysisEngineProcessException(e);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Could not create an instance of the entity class {}", entityClass);
            throw new AnalysisEngineProcessException(e);
        } catch (CASException e) {
            log.error("Could not set the entity offsets", e);
            throw new AnalysisEngineProcessException(e);
        }
    }


    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            connector.shutdown();
        } catch (InterruptedException e) {
            log.error("Could not shutdown the python connector", e);
            throw new AnalysisEngineProcessException(e);
        }
    }
}
