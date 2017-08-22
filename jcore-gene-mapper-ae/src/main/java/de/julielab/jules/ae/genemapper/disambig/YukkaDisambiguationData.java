package de.julielab.jules.ae.genemapper.disambig;

import java.util.List;

import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval.CandidateCacheKey;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;

public class YukkaDisambiguationData implements MentionDisambiguationData {

	public GeneMention geneMention;
	public List<SynHit> candidates;
	public CandidateCacheKey candidateKey;
	/**
	 * @deprecated The gene mentions know their context
	 */
	@Deprecated
	public String documentContext;
}
