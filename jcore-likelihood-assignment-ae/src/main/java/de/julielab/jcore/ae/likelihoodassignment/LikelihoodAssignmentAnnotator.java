
package de.julielab.jcore.ae.likelihoodassignment;

import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

@ResourceMetaData(name="JCoRe Likelihood Assignment AE", description = "Analysis Engine to assign likelihood indicators to their corresponding entities and events.")
@TypeCapability(inputs="de.julielab.jcore.types.LikelihoodIndicator")
public class LikelihoodAssignmentAnnotator extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LikelihoodAssignmentAnnotator.class);

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

	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

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
		assignLikelihood(aJCas);
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
				if (multipleLikelihood = true) {
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
		FSIterator conceptIt = aJCas.getAnnotationIndex(ConceptMention.type)
				.iterator();
		FSIterator likelihoodIt = aJCas.getAnnotationIndex(
				LikelihoodIndicator.type).iterator();

		sentMap = new TreeMap<Integer, Integer>();
		while (sentIt.hasNext()) {
			Sentence sent = (Sentence) sentIt.next();
			int sentBegin = sent.getBegin();
			int sentEnd = sent.getEnd();
			sentMap.put(sentBegin, sentEnd);
		}

		conceptMap = new TreeMap<Integer, ArrayList<ConceptMention>>();
		while (conceptIt.hasNext()) {
			ConceptMention concept = (ConceptMention) conceptIt.next();
			int conceptBegin = concept.getBegin();
			if (conceptMap.containsKey(conceptBegin)) {
				ArrayList<ConceptMention> conceptList = conceptMap
						.get(conceptBegin);
				conceptList.add(concept);
				conceptMap.put(conceptBegin, conceptList);
			} else {
				ArrayList<ConceptMention> conceptList = new ArrayList<ConceptMention>();
				conceptList.add(concept);
				conceptMap.put(conceptBegin, conceptList);
			}
		}

		likelihoodMap = new TreeMap<Integer, LikelihoodIndicator>();
		while (likelihoodIt.hasNext()) {
			LikelihoodIndicator likelihood = (LikelihoodIndicator) likelihoodIt
					.next();
			int likelihoodBegin = likelihood.getBegin();
			likelihoodMap.put(likelihoodBegin, likelihood);
		}
	}

}
