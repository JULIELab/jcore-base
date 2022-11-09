package de.julielab.jcore.ae.likelihoodassignment;

import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReAnnotationIndexMerger;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@ResourceMetaData(name = "JCoRe Likelihood Assignment AE", description = "Analysis Engine to assign likelihood indicators to their corresponding entities and events.")
@TypeCapability(inputs = "de.julielab.jcore.types.LikelihoodIndicator")
public class LikelihoodAssignmentAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_ASSIGNMENT_STRATEGY = "AssignmentStrategy";
    public static final String PARAM_CONCEPT_TYPE_NAME = "ConceptTypeName";
    public static final String STRATEGY_ALL = "all";
    public static final String STRATEGY_NEXT_CONCEPT = "next-concept";
    private static final Logger LOGGER = LoggerFactory
            .getLogger(LikelihoodAssignmentAnnotator.class);
    @ConfigurationParameter(name = PARAM_ASSIGNMENT_STRATEGY, mandatory = false, defaultValue = STRATEGY_NEXT_CONCEPT, description = "There are two available assignment strategies for likelihood indicators to ConceptMentions, '" + STRATEGY_ALL + "' and '" + STRATEGY_NEXT_CONCEPT + "'. The first, 'all', assigns the lowest likelihood indicator in a sentence to all ConceptMention in this sentence. The second assigns a likelihood indicator only to the directly following ConceptMention in the same sentence. The latter strategy fares a bit better in evaluations carried out for the publication of this approach. Defaults to '" + STRATEGY_NEXT_CONCEPT + "'.")
    private String assignmentStrategy;
    @ConfigurationParameter(name = PARAM_CONCEPT_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.ConceptMention", description = "The qualified UIMA type name for the concept annotation for which likelihood assignment should be performed. Must be a subclass of de.julielab.jcore.types.ConceptMention. Defaults to de.julielab.jcore.types.ConceptMention.")
    private String conceptTypeName;
    /**
     * Maps sentence ends to sentence begins.
     */
    private TreeMap<Integer, Integer> sentMap;
    /**
     * Maps concept mentions to their begins.
     */
    private TreeMap<Integer, ArrayList<ConceptMention>> conceptMap;
    /**
     * Maps likelihood indicators to their begins.
     */
    private TreeMap<Integer, LikelihoodIndicator> likelihoodMap;

    /**
     * Quantifies likelihood values.
     */
    private HashMap<String, Integer> likelihoodValueMap;
    private ConceptMention conceptTypeTemplate;

    public void initialize(UimaContext aContext)
            throws ResourceInitializationException {
        super.initialize(aContext);

        assignmentStrategy = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ASSIGNMENT_STRATEGY)).orElse("next-concept");
        conceptTypeName = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_CONCEPT_TYPE_NAME)).orElse(ConceptMention.class.getCanonicalName());

        // ordinal scale for likelihood indicators;
        // used when there are multiple occurrences (the lowest category is
        // chosen)
        likelihoodValueMap = new HashMap<>();
        likelihoodValueMap.put("negation", 1);
        likelihoodValueMap.put("low", 2);
        likelihoodValueMap.put("investigation", 3);
        likelihoodValueMap.put("moderate", 4);
        likelihoodValueMap.put("high", 5);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (conceptTypeTemplate == null) {
            try {
                conceptTypeTemplate = (ConceptMention) JCoReAnnotationTools.getAnnotationByClassName(aJCas, conceptTypeName);
            } catch (Exception e) {
                LOGGER.error("Could not obtain the specified concept UIMA type with name " + conceptTypeName + ".", e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        // We have two strategies available for the assignment of likelhood indicators to ConceptMentions.
        // Either the original one, implemented in 'assignLikelihood', where likelihood indicators in a sentences are
        // assigned to all ConceptMentions in the same sentence or a simplified one that, according to
        // Christine Engelmann, actually fared a bit better in evaluations, were a likelihood indicator is only
        // assigned to the next following ConceptMention, implemented in 'assignLikelihoodToNextConceptMention'.
        if (assignmentStrategy.equalsIgnoreCase(STRATEGY_NEXT_CONCEPT))
            assignLikelihoodToNextConceptMention(aJCas);
        else if (assignmentStrategy.equalsIgnoreCase(STRATEGY_ALL))
            assignLikelihood(aJCas);
        else
            throw new AnalysisEngineProcessException(new IllegalArgumentException("The " + PARAM_ASSIGNMENT_STRATEGY + " parameter requires one of two values, " + STRATEGY_ALL + " or " + STRATEGY_NEXT_CONCEPT + " but was set to " + assignmentStrategy + "."));
    }

    /**
     * <p>Simple assignment strategy that sets the direct nearest previous likelihood indicator to each ConceptMention.</p>
     * <p>No other ConceptMention must stand in between because then, a previous ConceptMention would be assigned the
     * likelihood indicator.</p>
     * <p>This strategy was proposed by Christine Engelmann because it fared a bit better in her evaluations than
     * the alternative strategy implemented in {@link #assignLikelihood(JCas)}.</p>
     *
     * @param aJCas The CAS to do likelihood assignment in.
     * @throws AnalysisEngineProcessException If the creation of the {@link JCoReAnnotationIndexMerger}, that is used internally, fails.
     */
    private void assignLikelihoodToNextConceptMention(JCas aJCas) throws AnalysisEngineProcessException {
        // create default likelihood indicator for assertions (has begin = 0 and
        // end = 0)
        LikelihoodIndicator assertionIndicator = new LikelihoodIndicator(aJCas);
        assertionIndicator.setLikelihood("assertion");
        assertionIndicator.setComponentId(this.getClass().getName());
        assertionIndicator.addToIndexes();

        for (Sentence sentence : aJCas.<Sentence>getAnnotationIndex(Sentence.type)) {
            // We use the annotation merger that gives us a sorted sequence of annotations of specified types.
            // Then, we must only assign for each concept the directly preceding likelihood annotation, if there is one.
            JCoReAnnotationIndexMerger merger;
            try {
                merger = new JCoReAnnotationIndexMerger(Set.of(JCasUtil.getAnnotationType(aJCas, conceptTypeTemplate.getClass()), JCasUtil.getAnnotationType(aJCas, LikelihoodIndicator.class)), true, sentence, aJCas);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Could not create JCoReAnnotationIndexMerger", e);
                throw new AnalysisEngineProcessException(e);
            }
            LikelihoodIndicator previousLikelihood = null;
            boolean previousLikelihoodConsumed = false;
            int lastAssignedCmBegin = 0;
            int lastAssignedCmEnd = 0;
            while (merger.incrementAnnotation()) {
                final Annotation annotation = (Annotation) merger.getAnnotation();
                ConceptMention cm = null;
                if (conceptTypeTemplate.getClass().isAssignableFrom(annotation.getClass())) {
                    cm = (ConceptMention) annotation;
                    // default likelihood is assertion
                    cm.setLikelihood(assertionIndicator);
                }
                // check if there is a likelihood anntotion preceeding the ConceptMention in this sentence without
                // another ConceptMention in between - except when multiple ConceptMentions exist in the same offsets
                // which is possible for EventMentions that exist on the EventTrigger annotation. The trigger may
                // refer to multiple events.
                if (cm != null && (previousLikelihood != null && (!previousLikelihoodConsumed || (lastAssignedCmBegin == cm.getBegin() && lastAssignedCmEnd == cm.getEnd())))) {
                    cm.setLikelihood(previousLikelihood);
                    // this likelihood indicator has been "consumed"
                    previousLikelihoodConsumed = true;
                    lastAssignedCmBegin = cm.getBegin();
                    lastAssignedCmEnd = cm.getEnd();
                }
                if (annotation instanceof LikelihoodIndicator) {
                    previousLikelihood = (LikelihoodIndicator) annotation;
                    previousLikelihoodConsumed = false;
                }
            }
        }
    }

    /**
     * If a sentence contains a likelihood indicator, this indicator is assigned
     * to all concept mentions occurring in the sentence. If a sentence does not
     * contain a likelihood indicator, the default likelihood category (i.e.
     * 'assertion') is assigned to all concept mentions occurring in the
     * sentence. In case of multiple likelihood indicators the lowest likelihood
     * category is chosen.
     *
     * @param aJCas
     */
    private void assignLikelihood(JCas aJCas) {
        buildTreeMaps(aJCas);

        // create default likelihood indicator for assertions (has begin = 0 and
        // end = 0)
        LikelihoodIndicator assertionIndicator = new LikelihoodIndicator(aJCas);
        assertionIndicator.setLikelihood("assertion");
        assertionIndicator.setComponentId(this.getClass().getName());
        assertionIndicator.addToIndexes();

        // iterate over sentences
        for (int sentBegin : sentMap.keySet()) {
            int sentEnd = sentMap.get(sentBegin);
            boolean sentHasLikelihood = false;
            boolean multipleLikelihood = false;
            Integer firstLikelihoodBegin = 0;
            Integer lastLikelihoodBegin = 0;

            // determine whether the sentence contains a likelihood indicator at
            // all and whether it even contains multiple likelihood indicators
            firstLikelihoodBegin = likelihoodMap.ceilingKey(sentBegin);
            if (firstLikelihoodBegin != null) {
                if (firstLikelihoodBegin > sentEnd) {
                    sentHasLikelihood = false;
                } else {
                    sentHasLikelihood = true;
                }
            }
            if (sentHasLikelihood == true) {
                lastLikelihoodBegin = likelihoodMap.floorKey(sentEnd);
                if (firstLikelihoodBegin == lastLikelihoodBegin) {
                    multipleLikelihood = false;
                } else {
                    multipleLikelihood = true;
                }
            }

            // determine which likelihood category to assign to concept mentions
            // in the sentence and create the corresponding likelihood indicator
            LikelihoodIndicator assignedLikelihood = null;
            if (sentHasLikelihood == true) {
                if (multipleLikelihood == true) {
                    // determine the lowest likelihood category in the sentence
                    NavigableMap<Integer, LikelihoodIndicator> likelihoodSubMap = likelihoodMap
                            .subMap(firstLikelihoodBegin, true,
                                    lastLikelihoodBegin, true);
                    int currentLikelihoodValue = 100;
                    for (int i : likelihoodSubMap.keySet()) {
                        LikelihoodIndicator likelihood = likelihoodSubMap
                                .get(i);
                        String likelihoodCat = likelihood.getLikelihood();
                        int likelihoodValue = likelihoodValueMap
                                .get(likelihoodCat);
                        if (likelihoodValue < currentLikelihoodValue) {
                            assignedLikelihood = likelihood;
                            currentLikelihoodValue = likelihoodValue;
                        }
                    }
                } else {
                    LikelihoodIndicator likelihood = likelihoodMap
                            .get(firstLikelihoodBegin);
                    assignedLikelihood = likelihood;
                }
            } else {
                assignedLikelihood = assertionIndicator;
            }

            // get all events in the sentence and assign the corresponding
            // likelihood indicator
            if (conceptMap.ceilingKey(sentBegin) != null) {
                int firstConceptBegin = conceptMap.ceilingKey(sentBegin);
                if (firstConceptBegin > sentEnd) {
                    continue;
                } else {
                    int lastConceptBegin = conceptMap.floorKey(sentEnd);
                    NavigableMap<Integer, ArrayList<ConceptMention>> conceptSubMap = conceptMap
                            .subMap(firstConceptBegin, true, lastConceptBegin,
                                    true);
                    for (int i : conceptSubMap.keySet()) {
                        ArrayList<ConceptMention> conceptList = conceptSubMap
                                .get(i);
                        for (ConceptMention concept : conceptList) {
                            concept.setLikelihood(assignedLikelihood);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void buildTreeMaps(JCas aJCas) {
        FSIterator sentIt = aJCas.getAnnotationIndex(Sentence.type).iterator();
        FSIterator conceptIt = aJCas.getAnnotationIndex(conceptTypeTemplate.type)
                .iterator();
        FSIterator likelihoodIt = aJCas.getAnnotationIndex(
                LikelihoodIndicator.type).iterator();

        sentMap = new TreeMap<>();
        while (sentIt.hasNext()) {
            Sentence sent = (Sentence) sentIt.next();
            int sentBegin = sent.getBegin();
            int sentEnd = sent.getEnd();
            sentMap.put(sentBegin, sentEnd);
        }

        conceptMap = new TreeMap<>();
        while (conceptIt.hasNext()) {
            ConceptMention concept = (ConceptMention) conceptIt.next();
            int conceptBegin = concept.getBegin();
            if (conceptMap.containsKey(conceptBegin)) {
                ArrayList<ConceptMention> conceptList = conceptMap
                        .get(conceptBegin);
                conceptList.add(concept);
                conceptMap.put(conceptBegin, conceptList);
            } else {
                ArrayList<ConceptMention> conceptList = new ArrayList<>();
                conceptList.add(concept);
                conceptMap.put(conceptBegin, conceptList);
            }
        }

        likelihoodMap = new TreeMap<>();
        while (likelihoodIt.hasNext()) {
            LikelihoodIndicator likelihood = (LikelihoodIndicator) likelihoodIt
                    .next();
            int likelihoodBegin = likelihood.getBegin();
            likelihoodMap.put(likelihoodBegin, likelihood);
        }
    }

}
