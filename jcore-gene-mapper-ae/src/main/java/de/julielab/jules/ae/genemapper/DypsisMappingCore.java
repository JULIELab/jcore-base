package de.julielab.jules.ae.genemapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.disambig.DypsisDisambiguation;
import de.julielab.jules.ae.genemapper.disambig.DypsisDocumentDisambiguationData;
import de.julielab.jules.ae.genemapper.genemodel.CandidateReliability;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

/**
 * This mapping core is named after the Dypsis lutescens, "Goldfruchtpalme" is
 * is supposed to augment the older YukkaMappingCore with a document oriented
 * integrity approach.
 * 
 * @author faessler
 *
 */
public class DypsisMappingCore implements MappingCore {

	private static final Logger log = LoggerFactory.getLogger(YukkaMappingCore.class);

	private DypsisDisambiguation semanticDisambiguation;
	private CandidateRetrieval candidateRetrieval;
	private TermNormalizer normalizer;

	public DypsisMappingCore(GeneMapperConfiguration config) throws GeneMapperException {
		this.normalizer = new TermNormalizer();
		this.candidateRetrieval = new YukkaCandidateRetrieval(config);
		this.semanticDisambiguation = new DypsisDisambiguation(config);
	}

	@Override
	public DocumentMappingResult map(GeneDocument document) throws GeneMapperException {

		for (GeneMention gm : document.getGenes()) {
			Map<String, CandidateReliability> species = document.inferSpecies(gm);
			
			assert gm.getContextQuery() != null;
			assert gm.getDocumentContext() != null;

			gm.setNormalizer(normalizer);

			log.debug("Searching candidates for {} and organism {}", gm.getText(), gm.getTaxonomyId());
			List<SynHit> candidates = Collections.emptyList();
			candidates = candidateRetrieval.getCandidates(gm);
			MentionMappingResult mentionMappingResult = new MentionMappingResult();
			mentionMappingResult.mappedMention = gm;
			mentionMappingResult.originalCandidates = candidates;
			if (candidates != null && !candidates.isEmpty()) {
				mentionMappingResult.bestCandidate = candidates.get(0);
			} else {
				mentionMappingResult.bestCandidate = MentionMappingResult.REJECTION;
				mentionMappingResult.resultEntry = MentionMappingResult.REJECTION;
				System.out.println(gm.getText() + " " + gm.getTaxonomyId());
			}

			gm.setMentionMappingResult(mentionMappingResult);
		}

		DypsisDocumentDisambiguationData data = new DypsisDocumentDisambiguationData();
		data.setGeneDocument(document);
		data.addNameLevelUnificationStrategy(DypsisDisambiguation.NameLevelUnificationStrategy.JNET_FIRST);
		DocumentMappingResult result = semanticDisambiguation.disambiguateDocument(data);

		return result;
	}

	@Override
	public MentionMappingResult map(GeneMention predictedMention) throws GeneMapperException {
		throw new NotImplementedException();
	}

	public DypsisDisambiguation getSemanticDisambiguation() {
		return semanticDisambiguation;
	}

	public CandidateRetrieval getCandidateRetrieval() {
		return candidateRetrieval;
	}

	public TermNormalizer getTermNormalizer() {
		return normalizer;
	}

}
