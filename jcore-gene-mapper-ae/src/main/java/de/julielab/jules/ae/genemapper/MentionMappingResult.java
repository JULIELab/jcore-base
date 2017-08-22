package de.julielab.jules.ae.genemapper;

import java.util.List;

import de.julielab.jules.ae.genemapper.genemodel.GeneMention;

public class MentionMappingResult implements Comparable<MentionMappingResult> {
	public enum MatchType {
		APPROX, EXACT
	}

	public static final SynHit REJECTION = new SynHit("GENE MENTION REJECTED", 0d, "GENE MENTION REJECTED",
			GeneMapper.class.getSimpleName());

	public SynHit resultEntry;
	public int ambiguityDegree;
	public MatchType matchType;
	public GeneMention mappedMention;
	/**
	 * The database candidates found for the name of this gene mention. This is
	 * a list of database name matches ordered by the score that the specific
	 * database name matches the gene name.
	 * 
	 */
	public List<SynHit> originalCandidates;
	/**
	 * The candidates that have been filtered by some criterion in the attempt
	 * to eliminate bad candidates. The list has to be set via
	 * {@link #setFilteredCandidates(List)} by an external algorithm and will be
	 * null before that.
	 */
	public List<SynHit> filteredCandidates;
	/**
	 * This list contains the same elements as {@link #filteredCandidates} but
	 * sorted for semantic score, after the respective external algorithm in a
	 * disambiguation class has set this field.
	 */
	public List<SynHit> semanticallyOrderedCandidates;
	/**
	 * The best candidate at the current time of the mapping process, defaults
	 * to {@link #REJECTION}.
	 */
	public SynHit bestCandidate = REJECTION;

	private long candidateRetrievalTime;

	private long disambiguationTime;

	/**
	 * The comparison is delegated to the order of the resultEntry SynHits.
	 * Thus, we actually sort by SynHit.
	 */
	@Override
	public int compareTo(MentionMappingResult o) {
		return resultEntry.compareTo(o.resultEntry);
	}

	public void setCandidateRetrievalTime(long candidateRetrievalTime) {
		this.candidateRetrievalTime = candidateRetrievalTime;
	}

	public long getCandidateRetrievalTime() {
		return candidateRetrievalTime;
	}

	public void setDisambiguationTime(long disambiguationTime) {
		this.disambiguationTime = disambiguationTime;

	}

	public long getDisambiguationTime() {
		return disambiguationTime;
	}

}
