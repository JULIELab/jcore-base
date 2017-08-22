package de.julielab.jules.ae.genemapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneCandidateRetrievalException;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;

public class YukkaCandidateRetrieval implements CandidateRetrieval {

	private LuceneCandidateRetrieval candidateRetrieval;

	public YukkaCandidateRetrieval(GeneMapperConfiguration config) throws GeneMapperException {
		candidateRetrieval = new LuceneCandidateRetrieval(config);
	}

	@Override
	public List<SynHit> getCandidates(String originalSearchTerm) throws GeneCandidateRetrievalException {
		throw new NotImplementedException();
	}

	@Override
	public List<SynHit> getCandidates(GeneMention geneMention, String organism) throws GeneCandidateRetrievalException {
		return getCandidates(geneMention, organism);
	}

	@Override
	public List<SynHit> getCandidates(String geneMentionText, String organism) throws GeneCandidateRetrievalException {
		throw new NotImplementedException();
	}

	@Override
	public List<SynHit> getCandidates(GeneMention gm) throws GeneCandidateRetrievalException {
		List<SynHit> candidates;
		List<SynHit> originalTextCandidates;
		List<SynHit> removedModCandidates = Collections.emptyList();
		List<SynHit> removedModNormalizedCandidates;
		
		originalTextCandidates = candidateRetrieval.getCandidates(gm);
		candidates = originalTextCandidates;
		if (!originalTextCandidates.isEmpty() && !originalTextCandidates.get(0).isExactMatch()) {
			removedModCandidates = candidateRetrieval
					.getCandidates(GeneMapper.removeModifiers(gm.getGeneName().getText()), gm.getTaxonomyId());
			candidates = removedModCandidates;
			if (!removedModCandidates.isEmpty() && !removedModCandidates.get(0).isExactMatch()) {
				removedModNormalizedCandidates = candidateRetrieval.getCandidates(
						GeneMapper.removeModifiers(gm.getGeneName().getNormalizedText()), gm.getTaxonomyId());
				candidates = removedModNormalizedCandidates;
			}
		}
		if (candidates.isEmpty() &&!removedModCandidates.isEmpty())
			candidates = removedModCandidates;
		if (candidates.isEmpty())
			candidates = originalTextCandidates;
		
		return candidates;
	}

	@Override
	public List<SynHit> getCandidates(GeneMention geneMention, Collection<String> organisms) throws GeneCandidateRetrievalException {
		List<SynHit> allhits = new ArrayList<>();
		for (String org : organisms)
			allhits.addAll(getCandidates(geneMention, org));
		return allhits;
	}

}
