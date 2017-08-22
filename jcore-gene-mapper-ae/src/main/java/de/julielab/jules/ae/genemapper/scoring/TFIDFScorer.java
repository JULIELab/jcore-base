package de.julielab.jules.ae.genemapper.scoring;

import org.apache.commons.lang.NotImplementedException;

import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.utils.TFIDFUtils;

public class TFIDFScorer extends Scorer {

	private TFIDFUtils tfidfOriginalName;
	private TFIDFUtils tfidfNormalizedVariant;
	private TFIDFUtils tfidfNormalizedName;

	public TFIDFScorer(TFIDFUtils tfidfOriginalName, TFIDFUtils tfidfNormalizedName,
			TFIDFUtils tfidfNormalizedVariant) {
		this.tfidfOriginalName = tfidfOriginalName;
		this.tfidfNormalizedName = tfidfNormalizedName;
		this.tfidfNormalizedVariant = tfidfNormalizedVariant;
	}

	@Override
	public double getScore(String term1, String term2) throws RuntimeException {

		throw new NotImplementedException(
				"This method is not implemented by this scorer. Refer to the other scoring methods.");
	}

	public double getOriginalNameScore(String term1, String term2) {
		if (isPerfectMatch(term1, term2))
			return PERFECT_SCORE;
		return tfidfOriginalName.score(term1, term2);
	}
	
	public double getNormalizedNameScore(String term1, String term2) {
		if (isPerfectMatch(term1, term2))
			return PERFECT_SCORE;
		return tfidfNormalizedName.score(term1, term2);
	}
	
	public double getNormalizedVariantScore(String term1, String term2)  {
		if (isPerfectMatch(term1, term2))
			return PERFECT_SCORE;
		return tfidfNormalizedVariant.score(term1, term2);
	}

	@Override
	public String info() {
		return "Secondstring TFIDF Scorer";
	}

	@Override
	public int getScorerType() {
		return GeneMapper.TFIDF;
	}

}
