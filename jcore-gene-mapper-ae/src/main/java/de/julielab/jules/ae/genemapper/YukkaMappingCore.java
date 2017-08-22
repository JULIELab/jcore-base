package de.julielab.jules.ae.genemapper;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval.CandidateCacheKey;
import de.julielab.jules.ae.genemapper.disambig.YukkaDisambiguation;
import de.julielab.jules.ae.genemapper.disambig.YukkaDisambiguationData;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

/**
 * This is an alternative mapping strategy to the original GeNo mapping. The
 * name Yukka is just some working title for differentiation from other mapping
 * implementations.
 * 
 * Yukka is still a mention-oriented mapping core and does not incorporate
 * information from abbreviations other genes.
 * 
 * @author faessler
 *
 */
public class YukkaMappingCore implements MappingCore {

	private static final Logger log = LoggerFactory.getLogger(YukkaMappingCore.class);

	private YukkaDisambiguation semanticDisambiguation;
	private CandidateRetrieval candidateRetrieval;
	private TermNormalizer normalizer;

	public YukkaMappingCore(GeneMapperConfiguration config) throws GeneMapperException {
		this.normalizer = new TermNormalizer();
		this.candidateRetrieval = new YukkaCandidateRetrieval(config);
		this.semanticDisambiguation = new YukkaDisambiguation(config);
	}

	@Override
	public DocumentMappingResult map(GeneDocument document) throws GeneMapperException {

		DocumentMappingResult documentResult = new DocumentMappingResult();

		for (GeneMention gm : document.getGenes()) {
			assert gm.getContextQuery() != null;
			assert gm.getDocumentContext() != null;
			MentionMappingResult mentionResult = map(gm);

			documentResult.mentionResults.add(mentionResult);
			documentResult.candidateRetrievalTime += mentionResult.getCandidateRetrievalTime();
			documentResult.disambiguationTime += mentionResult.getDisambiguationTime();
		}
		return documentResult;
	}

	@Override
	public MentionMappingResult map(GeneMention predictedMention) throws GeneMapperException {

		MentionMappingResult result = null;

		long candidateRetrievalTime = 0;
		long disambiguationTime = 0;
		long time = System.currentTimeMillis();

		predictedMention.setNormalizer(normalizer);

		log.debug("Searching candidates for {} and organism {}", predictedMention.getText(),
				predictedMention.getTaxonomyId());
		CandidateCacheKey candidateKey = new CandidateCacheKey(predictedMention.getGeneName(),
				predictedMention.getTaxonomyId());
		List<SynHit> candidates = Collections.emptyList();
		candidates = candidateRetrieval.getCandidates(predictedMention);
		time = System.currentTimeMillis() - time;
		candidateRetrievalTime += time;

		if (!candidates.isEmpty()) {
			log.debug("Found {} database candidates for gene mention {} for organism {}.",
					new Object[] { candidates.size(), predictedMention.getText(), predictedMention.getTaxonomyId() });
			YukkaDisambiguationData data = new YukkaDisambiguationData();
			data.candidates = candidates;
			data.candidateKey = candidateKey;
			data.documentContext = predictedMention.getDocumentContext();
			data.geneMention = predictedMention;
			long disambigTime = System.currentTimeMillis();
			result = semanticDisambiguation.disambiguateMention(data);
			disambigTime = System.currentTimeMillis() - disambigTime;
			disambiguationTime += disambigTime;
			log.debug("Adding database ID {} to the list of gene mention {} with offsets {}",
					new Object[] { result.resultEntry.getId(), predictedMention.getText(),
							predictedMention.getBegin() + "-" + predictedMention.getEnd(), });
		} else {
			log.debug("No database candidates found for gene mention {} for organism {}", predictedMention.getText(),
					predictedMention.getTaxonomyId());
			result = new MentionMappingResult();
			result.mappedMention = predictedMention;
			result.resultEntry = MentionMappingResult.REJECTION;
			result.bestCandidate = MentionMappingResult.REJECTION;
		}

		if (result.resultEntry != MentionMappingResult.REJECTION && StringUtils.isBlank(result.resultEntry.getTaxId()))
			log.warn("Gene ID {} has no taxonomy ID set, this should be fixed in the synonym index",
					result.resultEntry.getId());
		if (result.resultEntry != MentionMappingResult.REJECTION)
			log.debug("Decided for ID {} as best result for gene mention {} at offsets {}",
					new Object[] { result.resultEntry.getId(), result.mappedMention.getText(),
							result.mappedMention.getBegin() + "-" + result.mappedMention.getEnd() });
		else
			log.debug("No database entry was accepted for gene mention {} at offsets {}, rejecting the mention.",
					result.mappedMention.getText(),
					result.mappedMention.getBegin() + "-" + result.mappedMention.getEnd());

		result.setCandidateRetrievalTime(candidateRetrievalTime);
		result.setDisambiguationTime(disambiguationTime);
		return result;
	}

	public YukkaDisambiguation getSemanticDisambiguation() {
		return semanticDisambiguation;
	}

	public CandidateRetrieval getCandidateRetrieval() {
		return candidateRetrieval;
	}

	public TermNormalizer getTermNormalizer() {
		return normalizer;
	}

}
