package de.julielab.jules.ae.genemapper.genemodel;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import com.fulmicoton.multiregexp.MultiPatternSearcher;

import de.julielab.jules.ae.genemapper.genemodel.AcronymLongform;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention.GeneTagger;
import de.julielab.jules.ae.genemapper.utils.OffsetMap;
import de.julielab.jules.ae.genemapper.utils.OffsetSet;
import de.julielab.jules.ae.genemapper.utils.SpeciesMultiPatternSearcher;

public class GeneDocument {
	private String documentText;
	private String documentTitle;
	private Set<GeneMention> genes;
	private Set<Acronym> acronyms;
	
	private SpeciesCandidates species;
	
	private OffsetSet sentences;
	private OffsetMap<String> chunks;

	public String getDocumentText() {
		return documentText;
	}

	public void setDocumentText(String documentText) {
		this.documentText = documentText;
	}
	
	public String getDocumentTitle() {
		return documentTitle;
	}

	public void setDocumentTitle(String documentTitle) {
		this.documentTitle = documentTitle;
	}

	public Set<GeneMention> getGenes() {
		return genes;
	}

	public void setGenes(Set<GeneMention> genes) {
		this.genes = genes;
	}

	public Set<Acronym> getAcronyms() {
		return acronyms;
	}

	public void setAcronyms(Set<Acronym> acronyms) {
		this.acronyms = acronyms;
	}

	public SpeciesCandidates getSpecies() {
		return species;
	}

	public void setSpecies(SpeciesCandidates species) {
		this.species = species;
	}
	
	/**
	 * Creates a trivial GeneSets object where each gene is in its own set. From
	 * here, one can begin to agglomerate sets e.g. due to the same name, an
	 * acronym connection or other measures.
	 * 
	 * @return A GeneSets object where each gene has its own set.
	 */
	public GeneSets getGeneSets() {
		GeneSets geneSets = new GeneSets();
		for (GeneMention gm : genes) {
			GeneSet geneSet = new GeneSet();
			geneSet.add(gm);
			geneSets.add(geneSet);
		}
		return geneSets;
	}

	public Set<GeneMention> getGeneMentionsAtOffsets(Range<Integer> offsets) {
		Set<GeneMention> geneMentions = new HashSet<GeneMention>();
		for (GeneMention gene : genes) {
			if (gene.getOffsets().isOverlappedBy(offsets)) {
				geneMentions.add(gene);
			}
		}
		return geneMentions;
	}

	public void unifyGenesPrioritizeTagger(NavigableSet<GeneMention> sortedGenes, GeneTagger tagger) {
		GeneMention otherGene = null;
		for (GeneMention gm : genes) {
			if (sortedGenes.contains(gm)) {
			// As comparison is done via ranges, two genes are equal,
			// if they cover the same range, even if their respective other
			// values are different
				GeneTagger candidateTagger = gm.getTagger();
				if (candidateTagger == tagger) {
					if (sortedGenes.remove(gm)) {
						sortedGenes.add(gm);
					}
				}
			} else if (null != (otherGene = sortedGenes.floor(gm))) {
				if (otherGene.getOffsets().isOverlappedBy(gm.getOffsets())) {
					GeneTagger candidateTagger = gm.getTagger();
					if (candidateTagger == tagger) {
						if (sortedGenes.remove(otherGene)) {
							sortedGenes.add(gm);
						}
					}
				} else {
					sortedGenes.add(gm);
				}
			} else if (null != (otherGene = sortedGenes.ceiling(gm))) {
				if (otherGene.getOffsets().isOverlappedBy(gm.getOffsets())) {
					GeneTagger candidateTagger = gm.getTagger();
					if (candidateTagger == tagger) {
						if (sortedGenes.remove(otherGene)) {
							sortedGenes.add(gm);
						}
					}
				} else {
					sortedGenes.add(gm);
				}
			} else {
				sortedGenes.add(gm);
			}
		}
		//retainAll() is useless on HashSets (optional)
		genes.clear();
		genes.addAll(sortedGenes);	
	}

	public void unifyGenesLongerFirst(NavigableSet<GeneMention> sortedGenes) {
		GeneMention otherGene = null;
		for (GeneMention gm : genes) {
			if (sortedGenes.contains(gm)) {
				continue;
			} else if (null != (otherGene = sortedGenes.floor(gm))) {
				if (otherGene.getOffsets().isOverlappedBy(gm.getOffsets())) {
					int gmLength = gm.getText().length();
					int otherLength = otherGene.getText().length();
					if (gmLength > otherLength) {
						if (sortedGenes.remove(otherGene)) {
							sortedGenes.add(gm);
						}
					} 
				} else {
					sortedGenes.add(gm);
				}
			} else if (null != (otherGene = sortedGenes.ceiling(gm))) {
				if (otherGene.getOffsets().isOverlappedBy(gm.getOffsets())) {
					int gmLength = gm.getText().length();
					int otherLength = otherGene.getText().length();
					if (gmLength > otherLength) {
						if (sortedGenes.remove(otherGene)) {
							sortedGenes.add(gm);
						}
					} 
				} else {
					sortedGenes.add(gm);
				}
			} else {
				sortedGenes.add(gm);
			}
		}
		//retainAll() is useless on HashSets (optional)
		genes.clear();
		genes.addAll(sortedGenes);	
	}

	public AcronymLongform getAcronymLongformAndOffsets(Acronym acronym) {
		AcronymLongform longform = acronym.getLongform();
		if (null == longform.getText()) {
			Range<Integer> range = longform.getOffsets();
			longform.setText(this.getDocumentText().substring(range.getMinimum(), range.getMaximum()));
		}
		return longform;
	}

	/**
	 * Removes all prefixes belonging to a species, e.g. "human FGF-22" will
	 * be turned into "FGF-22"
	 * @param searcher A MultiPatternSearcher containing a compiled multi-regex
	 * of all species to be considered.
	 */
	public void removeSpeciesMention(MultiPatternSearcher searcher) {
		for (GeneMention gm : genes) {
			String text = gm.getText();
			MultiPatternSearcher.Cursor cursor = searcher.search(text);
			if (cursor.next()) {
				int start = cursor.start();
				if (start == 0) {
					int end = cursor.end();
					gm.setText(text.substring(end));
					Range<Integer> offsets = gm.getOffsets();
					int newBegin = offsets.getMinimum() + end;
					gm.setOffsets(Range.between(newBegin, offsets.getMaximum()));
				}
			}
		}
	}

	public void removeSpeciesMention() {
		removeSpeciesMention(SpeciesMultiPatternSearcher.searcher);
	}

	public NavigableSet<Range<Integer>> getSentences() {
		return sentences;
	}
	
	public void setSentences(OffsetSet sentences) {
		this.sentences = sentences;
	}
	
	public OffsetMap<String> getChunks() {
		return chunks;
	}

	public void setChunks(OffsetMap<String> chunks) {
		this.chunks = chunks;
	}	

	public Map<String, CandidateReliability> inferSpecies(GeneMention gm) {
		Range<Integer> geneOffsets = gm.getOffsets();
		Range<Integer> sentence = sentences.floor(geneOffsets);
		NavigableMap<Range<Integer>, String> sentenceChunks = chunks.restrictTo(sentence);
		Map<String, CandidateReliability> mentions;
		OffsetMap<SpeciesMention> candidates = species.getTextCandidates();

		if (null != candidates) {
			NavigableMap<Range<Integer>, SpeciesMention> sentenceSpecies = candidates.restrictTo(sentence);
			if ((mentions = speciesInNounPhrase(geneOffsets, sentenceSpecies, sentenceChunks)) != null) {
				return mentions;
			}
			//Search within the sentence before this.
			sentence = sentences.lower(sentence);
			//Might have already been the first sentence.
			if (null != sentence) {
				sentenceSpecies = candidates.restrictTo(sentence);			
				if ((mentions = speciesInSentence(sentenceSpecies, sentence, CandidateReliability.PREVIOUS)) != null) {
					return mentions;
				}
			}
		}
		Set<String> titleCandidates = species.getTitleCandidates();
		if (!titleCandidates.isEmpty()) {
			mentions = new TreeMap<>();
			for (String taxId : titleCandidates) {
				mentions.put(taxId, CandidateReliability.TITLE);
			}
			return mentions;
		}
		//First sentence
		if (null != candidates) {
			NavigableMap<Range<Integer>, SpeciesMention> sentenceSpecies = candidates.restrictTo(sentence);
			sentence = sentences.iterator().next();
			sentenceSpecies = candidates.restrictTo(sentence);			
			if ((mentions = speciesInSentence(sentenceSpecies, sentence, CandidateReliability.FIRST)) != null) {
				return mentions;
			}
			//Anywhere in the abstract
			if (!candidates.isEmpty()) {
				mentions = new TreeMap<>();
				for (SpeciesMention s : candidates.values()) {
					mentions.put(s.getTaxId(), CandidateReliability.ANYWHERE);
				}
				return mentions;
			}
		}
		
		return null;
	}

	private Map<String, CandidateReliability> speciesInSentence(
			NavigableMap<Range<Integer>, SpeciesMention> sentenceSpecies, Range<Integer> sentence, CandidateReliability order) {
		//No use in trying any further
		if (sentenceSpecies.isEmpty()) {
			return null;
		} else {
			Map<String, CandidateReliability> mentionMap = new TreeMap<>();
			for (SpeciesMention s : sentenceSpecies.values()) {
				mentionMap.put(s.getTaxId(), order);
			}
			return mentionMap;
		}
	}

	/**
	 * Searches for species mentions in compound nouns, enclosing phrases and the whole sentence, in this order.
	 * @param geneOffsets
	 * @param sentenceSpecies
	 * @param sentenceChunks
	 * @return A map of the taxonomy IDs of all mentioned species and their corresponding reliability. 
	 */
	private Map<String, CandidateReliability> speciesInNounPhrase(Range<Integer> geneOffsets,
			NavigableMap<Range<Integer>, SpeciesMention> sentenceSpecies,
			NavigableMap<Range<Integer>, String> sentenceChunks) {
		//No use in trying any further
		if (sentenceSpecies.isEmpty()) {
			return null;
		}
		
		NavigableMap<Range<Integer>, SpeciesMention> mentions = 
				sentenceSpecies.subMap(sentenceChunks.floorKey(geneOffsets), true,
									   Range.between(geneOffsets.getMaximum(), geneOffsets.getMaximum()), true);
		//Mention in this (compound) noun
		if (!mentions.isEmpty()) {
			Map<String, CandidateReliability> mentionMap = new TreeMap<>();
			for (SpeciesMention s : mentions.values()) {
				mentionMap.put(s.getTaxId(), CandidateReliability.COMPOUND);
			}
			return mentionMap;
		}
		
		Range<Integer> candidate = sentenceSpecies.floorKey(geneOffsets);
		Map<String, CandidateReliability> mentionMap = new TreeMap<>();
		if (null == candidate) {
			//This sentence, but not in front of gene mention
			for (SpeciesMention s : sentenceSpecies.values()) {
				mentionMap.put(s.getTaxId(), CandidateReliability.SENTENCE);
			}
			return mentionMap;
		} else {
			//TODO: Compare NP chunks
			Entry<Range<Integer>, String> chunk = sentenceChunks.floorEntry(geneOffsets);
			int start = -1;
			while (chunk.getValue().equals("ChunkNP")) {
				start = chunk.getKey().getMinimum();
				chunk = sentenceChunks.lowerEntry(chunk.getKey());
			}
			if (start != -1) {
				mentions = 
						sentenceSpecies.subMap(Range.between(start, start), true,
											   Range.between(geneOffsets.getMaximum(), geneOffsets.getMaximum()), true);
				for (SpeciesMention s : mentions.values()) {
					mentionMap.put(s.getTaxId(), CandidateReliability.PHRASE);
				}
				return mentionMap;
			}
			
			mentions = sentenceSpecies.headMap(Range.between(geneOffsets.getMinimum(), geneOffsets.getMinimum()), true);
			for (SpeciesMention s : mentions.values()) {
				mentionMap.put(s.getTaxId(), CandidateReliability.SENTENCE);
			}
			return mentionMap;
		}
	}

	// TODO create analogous methods for acronyms and full forms
	// take care to make it efficiently, e.g. by using TreeSets

}
