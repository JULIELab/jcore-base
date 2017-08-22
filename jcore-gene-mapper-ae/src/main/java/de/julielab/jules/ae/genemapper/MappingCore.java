package de.julielab.jules.ae.genemapper;

import de.julielab.jules.ae.genemapper.disambig.SemanticDisambiguation;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public interface MappingCore {
	MentionMappingResult map(GeneMention geneMention) throws GeneMapperException;

	SemanticDisambiguation getSemanticDisambiguation();
	CandidateRetrieval getCandidateRetrieval();
	TermNormalizer getTermNormalizer();

	DocumentMappingResult map(GeneDocument document) throws GeneMapperException;
}
