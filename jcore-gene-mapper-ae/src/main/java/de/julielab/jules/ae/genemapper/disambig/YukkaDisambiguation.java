package de.julielab.jules.ae.genemapper.disambig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import de.julielab.jules.ae.genemapper.DocumentMappingResult;
import de.julielab.jules.ae.genemapper.GeneMapperConfiguration;
import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval.CandidateCacheKey;
import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.index.ContextIndexFieldNames;
import de.julielab.jules.ae.genemapper.scoring.TFIDFScorer;
import de.julielab.jules.ae.genemapper.svm.MentionFeatureGenerator;
import de.julielab.jules.ae.genemapper.svm.SVM;
import de.julielab.jules.ae.genemapper.svm.SVMModel;
import de.julielab.jules.ae.genemapper.utils.FeatureUtils;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.TFIDFUtils;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class YukkaDisambiguation implements SemanticDisambiguation {
	private static final Logger log = LoggerFactory.getLogger(YukkaDisambiguation.class);

	private TFIDFScorer contextScorer;
	private TermNormalizer normalizer;
	private SemanticIndex semanticIndex;

	Map<String, Double> contextScoreCache;
	String currentDocumentContext = "";

	private double approxContextThreshold;
	private double exactContextThreshold;

	private SVMModel svmMentionsExactModel;
	private SVMModel svmCandidatesExactModel;
	private SVMModel svmMentionsApproxModel;
	private SVMModel svmCandidatesApproxModel;

	public YukkaDisambiguation(GeneMapperConfiguration config) throws GeneMapperException {
		try {
			String semIndexFile = config.getProperty(GeneMapperConfiguration.CONTEXT_INDEX);
			if (semIndexFile != null) {
				semanticIndex = new SemanticIndex(new File(semIndexFile));
			} else {
				throw new GeneMapperException("semantic index not specified in configuration file (critical).");
			}

			String exactContextThreshold = config.getProperty("exact_context_threshold");
			if (null != exactContextThreshold)
				this.setExactContextThreshold(Double.parseDouble(exactContextThreshold));

			String approxContextThreshold = config.getProperty("approx_context_threshold");
			if (null != approxContextThreshold)
				this.approxContextThreshold = Double.parseDouble(approxContextThreshold);

			String exactMentionsFilterModel = config.getProperty("exact_mentions_filter_svm");
			if (null != exactMentionsFilterModel)
				svmMentionsExactModel = SVM.readModel(exactMentionsFilterModel);

			String exactCandidatesFilterModel = config.getProperty("exact_candidates_filter_svm");
			if (null != exactCandidatesFilterModel)
				svmCandidatesExactModel = SVM.readModel(exactCandidatesFilterModel);

			String approxMentionsFilterModel = config.getProperty("approx_mentions_filter_svm");
			if (null != approxMentionsFilterModel)
				svmMentionsApproxModel = SVM.readModel(approxMentionsFilterModel);

			String approxCandidatesFilterModel = config.getProperty("approx_candidates_filter_svm");
			if (null != approxCandidatesFilterModel)
				svmCandidatesApproxModel = SVM.readModel(approxCandidatesFilterModel);

			TFIDFUtils tfidfUtils = new TFIDFUtils();
			tfidfUtils.learnFromLuceneIndex(semanticIndex.searcher.getIndexReader(),
					ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD);
			contextScorer = new TFIDFScorer(null, tfidfUtils, null);

			log.info("  * exact_context_threshold: " + exactContextThreshold);
			log.info("  * approx_context_threshold: " + approxContextThreshold);
			log.info("  * exact_mentions_filter_svm: " + exactMentionsFilterModel);
			log.info("  * exact_candidates_filter_svm: " + exactCandidatesFilterModel);
			log.info("  * approx_mentions_filter_svm: " + approxMentionsFilterModel);
			log.info("  * approx_candidates_filter_svm: " + approxCandidatesFilterModel);

		} catch (IOException | GeneMapperException | ClassNotFoundException e) {
			throw new GeneMapperException(e);
		}

		normalizer = new TermNormalizer();
		contextScoreCache = new HashMap<>();
	}

	@Override
	public MentionMappingResult disambiguateMention(MentionDisambiguationData disambiguationData) throws GeneMapperException {

		YukkaDisambiguationData data = (YukkaDisambiguationData) disambiguationData;
		GeneMention geneMention = data.geneMention;
		// make a copy because we will filter on the candidates list later on
		List<SynHit> candidates = new ArrayList<>(data.candidates);
		String documentContext = data.documentContext;
		CandidateCacheKey candidateKey = data.candidateKey;

		if (!currentDocumentContext.equals(documentContext)) {
			contextScoreCache.clear();
			currentDocumentContext = documentContext;
		}

		MentionMappingResult mentionMappingResult = new MentionMappingResult();
		mentionMappingResult.mappedMention = geneMention;
		mentionMappingResult.originalCandidates = candidates;
		mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
		mentionMappingResult.bestCandidate = MentionMappingResult.REJECTION;

		try {
			ArrayList<SynHit> exactHits = new ArrayList<>();
			for (SynHit candidate : candidates) {
				if (candidate.isExactMatch())
					exactHits.add(candidate);
				else
					break;
			}

			// make a difference between exact and non-exact matches
			List<SynHit> approxMatches = new ArrayList<>();
			if (!exactHits.isEmpty()) {
				// candidates = exactHits;
				mentionMappingResult.filteredCandidates = exactHits;
				mentionMappingResult.matchType = MentionMappingResult.MatchType.EXACT;

				SynHit bestSemanticHit = scoreSemantically(documentContext, exactHits, true);
				mentionMappingResult.resultEntry = bestSemanticHit;
				mentionMappingResult.ambiguityDegree = exactHits.size();
			} else {
				mentionMappingResult.matchType = MentionMappingResult.MatchType.APPROX;

				final String normalizedMention = normalizer.normalize(geneMention.getText());
				SynHit bestHit = candidates.get(0);
				// performing some filtering, not so special
				for (Iterator<SynHit> it = candidates.iterator(); it.hasNext();) {
					SynHit next = it.next();
					Multiset<String> mentionNumbers = FeatureUtils.getNumbers(normalizedMention.split("\\s"));
					Multiset<String> synNumbers = FeatureUtils.getNumbers(next.getSynonym().split("\\s"));
					if (!synNumbers.isEmpty() && !Multisets.difference(mentionNumbers, synNumbers).isEmpty())
						it.remove();
				}
				if (candidates.isEmpty())
					candidates.add(bestHit);

				mentionMappingResult.filteredCandidates = candidates;

				approxMatches.addAll(candidates);

				// List<SynHit> bestScoredApproxHits = approxMatches;
				double bestMentionScore = candidates.get(0).getMentionScore();
				List<SynHit> bestScoredApproxHits = new ArrayList<>();
				for (SynHit approxHit : candidates) {
					if (approxHit.getMentionScore() < bestMentionScore)
						break;
					bestScoredApproxHits.add(approxHit);
				}

				SynHit bestSemanticHit = scoreSemantically(documentContext, bestScoredApproxHits, true);
				mentionMappingResult.ambiguityDegree = bestScoredApproxHits.size();
				mentionMappingResult.resultEntry = bestSemanticHit;
			}

			if (mentionMappingResult.resultEntry != MentionMappingResult.REJECTION) {

				// apply thresholds and possibly reject the whole gene mention
				switch (mentionMappingResult.matchType) {
				case APPROX:

					if (svmMentionsApproxModel != null) {
						List<SynHit> candidatesSublist = data.candidates.subList(0,
								Math.min(20, data.candidates.size()));
						scoreSemantically(documentContext, candidatesSublist, false);
						double[] features = MentionFeatureGenerator.getMentionFeatures(candidatesSublist,
								mentionMappingResult.mappedMention, 20);
						double outcome = SVM.predict(features, svmMentionsApproxModel);
						if (outcome < 0) {
							mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
							log.debug("Rejected approximately matched gene {} with offsets {} on mention level by SVM for organism {}.", new Object[] {
									geneMention.getText(), geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId});
						}
					}

					if (mentionMappingResult.resultEntry != MentionMappingResult.REJECTION) {
						// only now the best candidate is not REJECTION
						mentionMappingResult.bestCandidate = mentionMappingResult.resultEntry;
						if (svmCandidatesApproxModel != null && svmCandidatesApproxModel.svmModel != null) {
							double[] candidateFeatures = MentionFeatureGenerator.getCandidateFeatures(
									mentionMappingResult.resultEntry, mentionMappingResult.mappedMention);
							double candidateOutcome = SVM.predict(candidateFeatures, svmCandidatesApproxModel);
							if (candidateOutcome < 0) {
								mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
								log.debug("Rejected approximately matched gene {} with offsets {} on candidate level by SVM for organism {}.", new Object[] {
										geneMention.getText(), geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId});
							}
						}
					}

					if (mentionMappingResult.resultEntry.getSemanticScore() < approxContextThreshold) {
						mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
						log.debug(
								"Rejected approximately matched gene {} with offsets {} for organism {} because its semantic score of {} is below the threshold of {}",
								new Object[] { geneMention.getText(),geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId,
										mentionMappingResult.resultEntry.getSemanticScore(), approxContextThreshold });
					}

					break;
				case EXACT:

					if (svmMentionsExactModel != null) {
						List<SynHit> candidatesSublist = data.candidates.subList(0,
								Math.min(20, data.candidates.size()));
						scoreSemantically(documentContext, candidatesSublist, false);
						double[] features = MentionFeatureGenerator.getMentionFeatures(candidatesSublist,
								mentionMappingResult.mappedMention, 20);
						double outcome = SVM.predict(features, svmMentionsExactModel);
						if (outcome < 0) {
							mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
							log.debug("Rejected exactly matched gene {} with offsets {} on mention level by SVM for organism {}.", new Object[] {
									geneMention.getText(), geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId});
						}
					}

					if (mentionMappingResult.resultEntry != MentionMappingResult.REJECTION) {
						// only now the best candidate is not REJECTION
						mentionMappingResult.bestCandidate = mentionMappingResult.resultEntry;
						if (svmCandidatesExactModel != null && svmCandidatesExactModel.svmModel != null) {
							double[] candidateFeatures = MentionFeatureGenerator.getCandidateFeatures(
									mentionMappingResult.resultEntry, mentionMappingResult.mappedMention);
							double candidateOutcome = SVM.predict(candidateFeatures, svmCandidatesExactModel);
							if (candidateOutcome < 0) {
								mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
								log.debug("Rejected exactly matched gene {} with offsets {} on candidate level by SVM for organism {}.", new Object[] {
										geneMention.getText(), geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId});
							}
						}
					}

					if (mentionMappingResult.resultEntry.getSemanticScore() < exactContextThreshold) {
						mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
						log.debug(
								"Rejected exactly matched gene {} with offsets {} for organism {} because its semantic score of {} is below the threshold of {}",
								new Object[] { geneMention.getText(),geneMention.getBegin() + "-" + geneMention.getEnd(), candidateKey.taxId,
										mentionMappingResult.resultEntry.getSemanticScore(), approxContextThreshold });
					}
					break;
				}
			}
			if (mentionMappingResult.resultEntry != MentionMappingResult.REJECTION)
				log.debug("Accepted gene mention {} with offsets {} and assigned ID {}", new Object[] {geneMention.getText(),geneMention.getBegin() + "-" + geneMention.getEnd(), mentionMappingResult.resultEntry.getId(), });
			return mentionMappingResult;
		} catch (IOException e) {
			throw new GeneMapperException(e);
		}
	}

	public double getApproxContextThreshold() {
		return approxContextThreshold;
	}

	public double getExactContextThreshold() {
		return exactContextThreshold;
	}

	public SemanticIndex getSemanticIndex() {
		return semanticIndex;
	}

	public SVMModel getSvmCandidatesApproxModel() {
		return svmCandidatesApproxModel;
	}

	public SVMModel getSvmCandidatesExactModel() {
		return svmCandidatesExactModel;
	}

	public SVMModel getSvmMentionsApproxModel() {
		return svmMentionsApproxModel;
	}

	public SVMModel getSvmMentionsExactModel() {
		return svmMentionsExactModel;
	}

	public SynHit scoreSemantically(String documentContext, List<SynHit> matches,
			boolean resort) throws IOException, CorruptIndexException {
		Map<String, String> geneContextById = semanticIndex.retrieveGeneContexts(matches);
		for (SynHit hit : matches) {
			Double contextScore = contextScoreCache.get(hit.getId());
			if (null == contextScore) {
				contextScore = contextScorer.getNormalizedNameScore(geneContextById.get(hit.getId()), documentContext);
				contextScoreCache.put(hit.getId(), contextScore);
			}
			hit.setSemanticScore(contextScore);
			if (resort)
				hit.setCompareType(SynHit.CompareType.SEMSCORE);
		}
		if (resort)
			Collections.sort(matches);

		SynHit hit = null;
		if (!matches.isEmpty())
			hit = matches.get(0);
		return hit;
	}

	public void setApproxContextThreshold(double approxContextThreshold) {
		this.approxContextThreshold = approxContextThreshold;
	}

	public void setExactContextThreshold(double exactContextThreshold) {
		this.exactContextThreshold = exactContextThreshold;
	}

	public void setSemanticIndex(SemanticIndex semanticIndex) {
		this.semanticIndex = semanticIndex;
	}

	public void setSvmCandidatesApproxModel(SVMModel svmCandidatesApproxModel) {
		this.svmCandidatesApproxModel = svmCandidatesApproxModel;
	}

	public void setSvmCandidatesExactModel(SVMModel svmCandidatesExactModel) {
		this.svmCandidatesExactModel = svmCandidatesExactModel;
	}

	public void setSvmMentionsApproxModel(SVMModel svmMentionsApproxModel) {
		this.svmMentionsApproxModel = svmMentionsApproxModel;
	}

	public void setSvmMentionsExactModel(SVMModel svmMentionsExactModel) {
		this.svmMentionsExactModel = svmMentionsExactModel;
	}

	@Override
	public DocumentMappingResult disambiguateDocument(DocumentDisambiguationData disambiguationData)
			throws GeneMapperException {
		throw new NotImplementedException();
	}

}
