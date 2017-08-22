package de.julielab.jules.ae.genemapper;

import java.util.Collection;
import java.util.List;

import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval.CandidateCacheKey;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneCandidateRetrievalException;

public interface CandidateRetrieval {
	List<SynHit> getCandidates(String originalSearchTerm) throws GeneCandidateRetrievalException;
	List<SynHit> getCandidates(GeneMention geneMention) throws GeneCandidateRetrievalException;
	List<SynHit> getCandidates(GeneMention geneMention, String organism) throws GeneCandidateRetrievalException;
	List<SynHit> getCandidates(GeneMention geneMention, Collection<String> organisms) throws GeneCandidateRetrievalException;
	List<SynHit> getCandidates(String geneMentionText, String organism) throws GeneCandidateRetrievalException;
//	List<SynHit> getCandidates(CandidateCacheKey key) throws GeneCandidateRetrievalException;
}
