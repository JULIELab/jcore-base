package de.julielab.jules.ae.genemapper.scoring;

import org.apache.commons.lang.StringUtils;

import de.julielab.jules.ae.genemapper.GeneMapper;

public class LevenshteinScorer extends Scorer {

	@Override
	public double getScore(String term1, String term2) throws RuntimeException {
		
		if (isPerfectMatch(term1,term2)) {
			return PERFECT_SCORE;
		}
		
		double distance = StringUtils.getLevenshteinDistance(term1, term2);
		double normalizedDistance = distance / Math.max((double) term1.length(), (double) term2.length());
		return 1 - normalizedDistance;
	}

	@Override
	public String info() {
		return "Normalized Levenshtein Similarity Scorer";
	}

	@Override
	public int getScorerType() {
		return GeneMapper.LEVENSHTEIN_SCORER;
	}

}
