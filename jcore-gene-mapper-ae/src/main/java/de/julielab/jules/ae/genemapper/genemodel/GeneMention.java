package de.julielab.jules.ae.genemapper.genemodel;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.apache.lucene.search.Query;

import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

/**
 * A basic "gene mention" that most of all contains the text of the mention.
 * However, we might also need other information, i.e. offsets.
 * 
 * @author faessler
 *
 */
public class GeneMention {
	/**
	 * Constant meaning that no ID is given for a GeneMention.
	 */
	public static final String NOID = "NoId";
	private String docId;

	private GeneName geneName;

	private String id = NOID;

	private String mappedSynonym;

	private TermNormalizer normalizer;

	private Range<Integer> offsets;

	private String text;

	private String taxonomyId;
	
	private Map<String, CandidateReliability> taxonomyCandidates;

	private String documentContext;

	private Query contextQuery;

	private GeneTagger tagger;

	private MentionMappingResult mentionMappingResult;

	public enum GeneTagger {
		JNET, GAZETTEER
	}

	public String getTaxonomyId() {
		return taxonomyId;
	}

	public void setTaxonomyId(String taxonomyId) {
		this.taxonomyId = taxonomyId;
	}
	
	public Set<String> getTaxonomyIds() {
		return taxonomyCandidates != null ? taxonomyCandidates.keySet() : Collections.emptySet();
	}

	public void setTaxonomyIds(Map<String, CandidateReliability> taxonomyCandidates) {
		this.taxonomyCandidates = taxonomyCandidates;
	}
	
	public GeneMention() {
	}

	public GeneMention(GeneMention gm) {
		this.docId = gm.docId;
		this.id = gm.id;
		this.offsets = gm.offsets;
		this.text = gm.text;
		this.mappedSynonym = gm.mappedSynonym;
	}

	public GeneMention(String text) {
		this.text = text;
	}

	public GeneMention(String text, TermNormalizer normalizer) {
		this.text = text;
		this.setNormalizer(normalizer);
	}

	public String getDocumentContext() {
		return documentContext;
	}

	public void setDocumentContext(String documentContext) {
		this.documentContext = documentContext;
	}

	public Query getContextQuery() {
		return contextQuery;
	}

	public void setContextQuery(Query contextQuery) {
		this.contextQuery = contextQuery;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneMention other = (GeneMention) obj;
		if (docId == null) {
			if (other.docId != null)
				return false;
		} else if (!docId.equals(other.docId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (offsets == null) {
			if (other.offsets != null)
				return false;
		} else if (!offsets.equals(other.offsets))
			return false;
		if (tagger == null) {
			if (other.tagger != null)
				return false;
		} else if (!tagger.equals(other.tagger))
			return false;
		return true;
	}

	public int getBegin() {
		return offsets.getMinimum();
	}

	public String getDocId() {
		return docId;
	}

	public int getEnd() {
		return offsets.getMaximum();
	}

	public GeneName getGeneName() {
		if (geneName == null && getNormalizer() == null)
			throw new IllegalStateException(
					"This GeneMention has not set a TermNormalizer and thus cannot create a GeneName instance.");
		if (geneName == null)
			geneName = new GeneName(text, normalizer);
		return geneName;
	}

	/**
	 * The gene ID of this gene mention. This is mostly used for evaluation and
	 * might not be set in a variety of situations. During the mapping process
	 * the final mapped ID of this mention is determined by the mention mapping
	 * result, see {@link #getMentionMappingResult()}.
	 * 
	 * @return The gene ID of this mention, if set.
	 */
	public String getId() {
		return id;
	}

	public String getMappedSynonym() {
		return mappedSynonym;
	}

	public TermNormalizer getNormalizer() {
		return normalizer;
	}

	public Range<Integer> getOffsets() {
		return offsets;
	}

	public String getText() {
		return text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((docId == null) ? 0 : docId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((offsets == null) ? 0 : offsets.hashCode());
		return result;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setMappedSynonym(String mappedSynonym) {
		this.mappedSynonym = mappedSynonym;
	}

	public void setNormalizer(TermNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public void setOffsets(Range<Integer> offsets) {
		this.offsets = offsets;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return "GeneMention [text=" + text + ", offsets=" + offsets + ", docId=" + docId + ", id=" + id
				+ ", mappedSynonym=" + mappedSynonym + "]";
	}

	public String getNormalizedText() {
		return getGeneName().getNormalizedText();
	}

	public String getNormalizedTextVariant() {
		return getGeneName().getNormalizedTextVariant();
	}

	public GeneTagger getTagger() {
		return tagger;
	}

	public void setTagger(GeneTagger tagger) {
		this.tagger = tagger;
	}

	public void setMentionMappingResult(MentionMappingResult mentionMappingResult) {
		this.mentionMappingResult = mentionMappingResult;
	}

	/**
	 * @return The object representing the result of the mapping process for
	 *         this particular gene mention.
	 */
	public MentionMappingResult getMentionMappingResult() {
		return mentionMappingResult;
	}

}
