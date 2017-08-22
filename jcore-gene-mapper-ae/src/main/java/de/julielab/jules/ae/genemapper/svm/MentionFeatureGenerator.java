package de.julielab.jules.ae.genemapper.svm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanQuery;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.MentionMappingResult.MatchType;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.disambig.YukkaDisambiguation;
import de.julielab.jules.ae.genemapper.eval.tools.EvalToolUtilities;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.ContextUtils;
import de.julielab.jules.ae.genemapper.utils.FeatureUtils;
import de.julielab.jules.ae.genemapper.utils.GeneCandidateRetrievalException;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;

public class MentionFeatureGenerator {
	public static List<Object> getFullMentionStats(boolean isCorrect, List<SynHit> candidates, GeneMention geneMention,
			Set<GeneMention> goldMentionsForPrediction) throws IOException {

		double[] features = getMentionFeatures(candidates, geneMention, 20);

		List<String> goldIds = new ArrayList<>();
		for (GeneMention gold : goldMentionsForPrediction)
			goldIds.add(gold.getId());

		List<Object> stats = new ArrayList<>(features.length + 3);
		stats.add(isCorrect);
		for (int i = 0; i < features.length; i++) {
			double featureValue = features[i];
			stats.add(featureValue);
		}
		stats.add(geneMention.getText());
		stats.add(geneMention.getNormalizedText());
		stats.add(StringUtils.join(goldIds, ", "));

		return stats;
	}

	public static double[] getMentionFeatures(List<SynHit> candidates, GeneMention predictedMention,
			int candidateCutoff) {
		Set<String> predictionTokens = new HashSet<>(Arrays.asList(predictedMention.getNormalizedText().split("\\s")));
		int numTokens = predictionTokens.size();
		int mentionLength = predictedMention.getNormalizedText().length();
		int minTokenOverlap = Integer.MAX_VALUE;
		int maxTokenOverlap = Integer.MIN_VALUE;
		double meanTokenOverlap = 0;

		double minScore = Double.MAX_VALUE;
		double maxScore = Double.MIN_VALUE;
		double meanScore = 0;
		double scoreStdDeviation = 0;

		double minSemScore = Double.MAX_VALUE;
		double maxSemScore = Double.MIN_VALUE;
		double meanSemScore = 0;
		double semScoreStdDeviation = 0;

		double meanNumberCompatibility = 0;
		double meanTokenRatio = 0;
		double meanLengthRatio = 0;

		int numContainedSynonyms = 0;
		int numSynonymsContain = 0;

		int n = Math.min(candidateCutoff, candidates.size());
		for (int i = 0; i < n && i < candidates.size(); ++i) {
			SynHit hit = candidates.get(i);
			double hitScore = Math.min(hit.getMentionScore(), 1d);
			if (hitScore > maxScore)
				maxScore = hitScore;
			if (hitScore < minScore)
				minScore = hitScore;
			meanScore += hitScore;

			double hitSemScore = Math.min(hit.getSemanticScore(), 1d);
			if (hitSemScore > maxSemScore)
				maxSemScore = hitSemScore;
			if (hitSemScore < minSemScore)
				minSemScore = hitSemScore;
			meanSemScore += hitSemScore;

			Multiset<String> synoymTokens = FeatureUtils.getNumberOfCommonTokens(predictedMention.getNormalizedText(),
					hit.getSynonym());
			int tokenOverlap = synoymTokens.size();
			if (tokenOverlap < minTokenOverlap)
				minTokenOverlap = tokenOverlap;
			if (tokenOverlap > maxTokenOverlap)
				maxTokenOverlap = tokenOverlap;
			meanTokenOverlap += tokenOverlap;

			meanNumberCompatibility += FeatureUtils.isNumberCompatible(predictedMention.getNormalizedText(),
					hit.getSynonym()) ? 1 : 0;
			meanTokenRatio += (double) predictedMention.getNormalizedText().split("\\s").length
					/ hit.getSynonym().split("\\s").length;
			meanLengthRatio += (double) predictedMention.getNormalizedText().length() / hit.getSynonym().length();

			if (hit.getMappedMention().contains(hit.getSynonym()))
				numContainedSynonyms = 1;
			if (hit.getSynonym().contains(hit.getMappedMention()))
				numSynonymsContain = 1;
		}
		meanScore /= n;
		meanTokenOverlap /= n;
		meanNumberCompatibility /= n;
		meanTokenRatio /= n;
		meanLengthRatio /= n;

		meanSemScore /= n;

		// get standard deviation (makes only a very small difference)
		for (int i = 0; i < n && i < candidates.size(); ++i) {
			SynHit hit = candidates.get(i);
			double hitScore = Math.min(hit.getMentionScore(), 1d);
			scoreStdDeviation += Math.pow(hitScore - meanScore, 2);

			double hitSemScore = Math.min(hit.getSemanticScore(), 1d);
			semScoreStdDeviation += Math.pow(hitSemScore - meanSemScore, 2);
		}
		scoreStdDeviation /= n;
		scoreStdDeviation = Math.sqrt(scoreStdDeviation);

		semScoreStdDeviation /= n;
		semScoreStdDeviation = Math.sqrt(semScoreStdDeviation);

		// get number of exact hits
		List<SynHit> exactHits = new ArrayList<>();
		for (SynHit candidate : candidates) {
			if (candidate.isExactMatch())
				exactHits.add(candidate);
			else
				break;
		}

		double[] features = new double[] { minScore, maxScore, meanScore, scoreStdDeviation, (double) exactHits.size(),
				(double) numTokens, (double) mentionLength, (double) minTokenOverlap, (double) maxTokenOverlap,
				meanTokenOverlap, meanNumberCompatibility, meanTokenRatio, meanLengthRatio,
				(double) numContainedSynonyms, (double) numSynonymsContain, minSemScore, maxSemScore, meanSemScore,
				semScoreStdDeviation };
		return features;
	}

	public static double[] getCandidateFeatures(SynHit candidate, GeneMention predictedMention) {
		Set<String> predictionTokens = new HashSet<>(Arrays.asList(predictedMention.getNormalizedText().split("\\s")));
		int numTokens = predictionTokens.size();
		int mentionLength = predictedMention.getNormalizedText().length();
		int synonymLength = candidate.getSynonym().length();
		int tokenOverlap = FeatureUtils
				.getNumberOfCommonTokens(predictedMention.getNormalizedText(), candidate.getSynonym()).size();

		double mentionScore = candidate.getMentionScore();
		double semScore = candidate.getSemanticScore();
		double isNumberCompatible = FeatureUtils.isNumberCompatible(predictedMention.getNormalizedText(),
				candidate.getSynonym()) ? 1 : 0;
		double tokenRatio = (double) predictedMention.getNormalizedText().split("\\s").length
				/ candidate.getSynonym().split("\\s").length;
		double lengthRatio = (double) predictedMention.getNormalizedText().length() / candidate.getSynonym().length();
		double containsSynonym = candidate.getMappedMention().contains(candidate.getSynonym()) ? 1 : 0;
		double synonymContains = candidate.getSynonym().contains(candidate.getMappedMention()) ? 1 : 0;

		double isExactMatch = candidate.isExactMatch() ? 1 : 0;

		double[] features = new double[] { (double) numTokens, (double) mentionLength, (double) synonymLength,
				(double) tokenOverlap, mentionScore, semScore, isNumberCompatible, tokenRatio, lengthRatio,
				containsSynonym, synonymContains, isExactMatch };
		return features;
	}

	public static SVMTrainData getFeatureMatrixForCandidateData(GeneMapper mapper,
			Multimap<String, GeneMention> goldGenes, Multimap<String, GeneMention> predictedGenes,
			Map<String, String> documentContexts, HashSet<MatchType> matchTypes, int candidateCutoff)
			throws IOException, CorruptIndexException, GeneMapperException {

		List<double[]> featureList = new ArrayList<>(predictedGenes.keySet().size());
		List<Double> labelList = new ArrayList<>(featureList.size());

		int numMentionsRejected = 0;
		List<GeneMention> geneMentions = new ArrayList<>(predictedGenes.keySet().size());
		for (String docId : predictedGenes.keySet()) {
			Collection<GeneMention> predictedMentions = predictedGenes.get(docId);

			String documentContext = documentContexts.get(docId);
			BooleanQuery contextQuery = ContextUtils.makeContextQuery(documentContext);

			Set<GeneMention> goldGeneMentions = new HashSet<>(goldGenes.get(docId));

			for (GeneMention predictedMention : predictedMentions) {

				// we can use the map method directly, despite the fact that it
				// might use the classifier we are trying to train; the best
				// candidate is chosen before the rejection model is applied
				MentionMappingResult mentionMappingResult = mapper.map(predictedMention, contextQuery, documentContext);
				if (mentionMappingResult.bestCandidate != MentionMappingResult.REJECTION) {

					if (!matchTypes.contains(mentionMappingResult.matchType))
						continue;

					Set<GeneMention> goldMentionsForPrediction = EvalToolUtilities
							.getGeneMentionsAtPosition(predictedMention, goldGeneMentions);

					Set<String> goldIds = EvalToolUtilities.getIdsOfMentions(goldMentionsForPrediction);

					double[] candidateFeatures = getCandidateFeatures(mentionMappingResult.bestCandidate,
							predictedMention);
					featureList.add(candidateFeatures);
					labelList.add(goldIds.contains(mentionMappingResult.bestCandidate.getId()) ? 1d : -1d);
					geneMentions.add(predictedMention);

				} else {
					++numMentionsRejected;
				}
			}
		}

		double[][] featureMatrix = new double[featureList.size()][];
		double[] labels = new double[labelList.size()];

		for (int i = 0; i < featureList.size(); ++i) {
			featureMatrix[i] = featureList.get(i);
			labels[i] = labelList.get(i);
		}

		SVMTrainData svmTrainData = new SVMTrainData(labels, featureMatrix);
		svmTrainData.geneList = geneMentions;
		svmTrainData.numMentionsRejected = numMentionsRejected;
		return svmTrainData;
	}

	public static SVMTrainData getFeatureMatrixForMentionData(GeneMapper mapper,
			Multimap<String, GeneMention> goldGenes, Multimap<String, GeneMention> predictedGenes,
			Map<String, String> documentContexts, Set<MentionMappingResult.MatchType> matchTypes, int candidateCutoff)
			throws IOException, CorruptIndexException, GeneCandidateRetrievalException {

		List<double[]> featureList = new ArrayList<>(predictedGenes.keySet().size());
		List<Double> labelList = new ArrayList<>(featureList.size());

		YukkaDisambiguation semanticDisambiguation = (YukkaDisambiguation) mapper.getMappingCore()
				.getSemanticDisambiguation();

		int numMentionsWithoutCandidates = 0;
		List<GeneMention> geneMentions = new ArrayList<>(predictedGenes.keySet().size());
		for (String docId : predictedGenes.keySet()) {
			// mentions
			Collection<GeneMention> predictedMentions = predictedGenes.get(docId);

			String documentContext = documentContexts.get(docId);
			BooleanQuery contextQuery = ContextUtils.makeContextQuery(documentContext);

			Set<GeneMention> goldGeneMentions = new HashSet<>(goldGenes.get(docId));

			for (GeneMention predictedMention : predictedMentions) {
				predictedMention.setNormalizer(mapper.getMappingCore().getTermNormalizer());
				List<SynHit> candidates = mapper.getMappingCore().getCandidateRetrieval()
						.getCandidates(predictedMention);

				if (candidates.isEmpty()) {
					++numMentionsWithoutCandidates;
					continue;
				}

				MatchType matchType = candidates.get(0).isExactMatch() ? MatchType.EXACT : MatchType.APPROX;
				if (!matchTypes.contains(matchType))
					continue;

				if (candidateCutoff > 0)
					candidates = candidates.subList(0, Math.min(candidateCutoff, candidates.size()));

				semanticDisambiguation.scoreSemantically(documentContext, candidates, false);

				Set<GeneMention> goldMentionsForPrediction = EvalToolUtilities
						.getGeneMentionsAtPosition(predictedMention, goldGeneMentions);
				// if doing a filter classifier on basis of these data, one
				// could try the filteredCandidates as well as the
				// originalCandidates
				double[] features = MentionFeatureGenerator.getMentionFeatures(candidates, predictedMention,
						candidateCutoff);
				featureList.add(features);
				labelList.add(goldMentionsForPrediction.isEmpty() ? -1d : 1d);

				geneMentions.add(predictedMention);
			}
		}

		double[][] featureMatrix = new double[featureList.size()][];
		double[] labels = new double[labelList.size()];

		for (int i = 0; i < featureList.size(); ++i) {
			featureMatrix[i] = featureList.get(i);
			labels[i] = labelList.get(i);
		}

		SVMTrainData svmTrainData = new SVMTrainData(labels, featureMatrix);
		svmTrainData.geneList = geneMentions;
		svmTrainData.numMentionsRejected = numMentionsWithoutCandidates;
		return svmTrainData;
	}

	public static SVMTrainData getFeatureMatrixForMentionData(GeneMapper mapper, String goldFile,
			String predictedMentionsFile, String pubmedDir, Set<MatchType> matchTypes, int candidateCutoff) throws IOException, GeneCandidateRetrievalException {
		Multimap<String, GeneMention> goldGenes = EvalToolUtilities.readMentionsWithOffsets(goldFile);
		Multimap<String, GeneMention> predictedMentions = EvalToolUtilities
				.readMentionsWithOffsets(predictedMentionsFile);
		Map<String, String> documentContexts = EvalToolUtilities.readGeneContexts(pubmedDir);

		return getFeatureMatrixForMentionData(mapper, goldGenes, predictedMentions, documentContexts, matchTypes, candidateCutoff);
	}

	public static SVMTrainData getFeatureMatrixForCandidateData(GeneMapper mapper, String goldFile,
			String predictedMentionsFile, String pubmedDir, HashSet<MatchType> matchTypes, int candidateCutoff) throws IOException, GeneMapperException {
		Multimap<String, GeneMention> goldGenes = EvalToolUtilities.readMentionsWithOffsets(goldFile);
		Multimap<String, GeneMention> predictedMentions = EvalToolUtilities
				.readMentionsWithOffsets(predictedMentionsFile);
		Map<String, String> documentContexts = EvalToolUtilities.readGeneContexts(pubmedDir);

		return getFeatureMatrixForCandidateData(mapper, goldGenes, predictedMentions, documentContexts, matchTypes, candidateCutoff);
		
	}

}
