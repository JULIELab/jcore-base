package de.julielab.jules.ae.genemapper;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is thought to be used as an overall result when all gene mentions
 * of a document are mapped in a single process. Until now, the GeneMapper does
 * not do this but uses this class in preparation of possible future work.
 * 
 * @author faessler
 *
 */
public class DocumentMappingResult {
	public String docId;
	public List<MentionMappingResult> mentionResults = new ArrayList<>();
	public long candidateRetrievalTime;
	public long disambiguationTime;

	/**
	 * Returns the {@link SynHit}s of all mention mapping results. Please note
	 * that this method is used at several places in a context where the
	 * DocumentMappingResult only contains the result of a single GeneMention.
	 * Thus, as soon as a real document-wide mapping has been implemented, the
	 * semantics of this method could be different at different places (single
	 * MentionMappingResults vs. MentionMappingResults of whole documents).
	 * 
	 * @return
	 */
	public List<SynHit> getAllSynHits() {
		List<SynHit> hits = new ArrayList<>();
		for (MentionMappingResult result : mentionResults)
			hits.add(result.resultEntry);
		return hits;
	}
}
