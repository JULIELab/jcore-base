package de.julielab.jules.ae.genemapper.disambig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import de.julielab.jules.ae.genemapper.DocumentMappingResult;
import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.GeneMapperConfiguration;
import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.SynHitSet;
import de.julielab.jules.ae.genemapper.genemodel.Acronym;
import de.julielab.jules.ae.genemapper.genemodel.AcronymLongform;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention.GeneTagger;
import de.julielab.jules.ae.genemapper.genemodel.GeneSet;
import de.julielab.jules.ae.genemapper.genemodel.GeneSets;
import de.julielab.jules.ae.genemapper.index.ContextIndexFieldNames;
import de.julielab.jules.ae.genemapper.scoring.TFIDFScorer;
import de.julielab.jules.ae.genemapper.utils.FeatureUtils;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.TFIDFUtils;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class DypsisDisambiguation implements SemanticDisambiguation {

	private enum AgglomerationCandidateType {original, strictlyfiltered, filtered, semantic};
	
	private int maxAgglomerationCandidates = 20;
	AgglomerationCandidateType agglomerationCandidates = AgglomerationCandidateType.filtered;
	
	private static final Logger log = LoggerFactory.getLogger(DypsisDisambiguation.class);

	private TFIDFScorer contextScorer;
	private TermNormalizer normalizer;
	private SemanticIndex semanticIndex;

	Map<String, Double> contextScoreCache;
	String currentDocumentContext = "";

	public DypsisDisambiguation(GeneMapperConfiguration config) throws GeneMapperException {
		try {
			String semIndexFile = config.getProperty(GeneMapperConfiguration.CONTEXT_INDEX);
			if (semIndexFile != null) {
				semanticIndex = new SemanticIndex(new File(semIndexFile));
			} else {
				throw new GeneMapperException("semantic index not specified in configuration file (critical).");
			}

		} catch (IOException | GeneMapperException e) {
			throw new GeneMapperException(e);
		}

		normalizer = new TermNormalizer();
		contextScoreCache = new HashMap<>();
		
		TFIDFUtils tfidfUtils = new TFIDFUtils();
		tfidfUtils.learnFromLuceneIndex(semanticIndex.searcher.getIndexReader(),
				ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD);
		contextScorer = new TFIDFScorer(null, tfidfUtils, null);
	}

	@Override
	public MentionMappingResult disambiguateMention(MentionDisambiguationData disambiguationData)
			throws GeneMapperException {

		throw new NotImplementedException();
	}

	@Override
	public DocumentMappingResult disambiguateDocument(DocumentDisambiguationData disambiguationData)
			throws GeneMapperException {
		DypsisDocumentDisambiguationData data = (DypsisDocumentDisambiguationData) disambiguationData;

		// remove duplicates in a first pass to reduce work in the following
		unifyOverlappingGenesOnNameLevel(data.getGeneDocument(), data.getNameLevelUnificationStrategies());

		filterUnspecifieds(data.getGeneDocument().getGenes());
		removeNondescriptives(data.getGeneDocument().getGenes());
		// For each gene mention, filter away candidates we think won't be good
		// anyway.
		filterCandidates(data.getGeneDocument().getGenes());

		assignSemanticScores(data.getGeneDocument().getGenes());
//		// Now agglomerate the single gene mentions to sets of mentions where we
//		// think that they denote the same gene
		GeneSets geneSets = data.getGeneDocument().getGeneSets();
		
		agglomerateByAcronyms(geneSets, data.getGeneDocument(), maxAgglomerationCandidates);
//		agglomerateByIds(geneSets);

		DocumentMappingResult result = new DocumentMappingResult();
		// All genes in the gene document should now have their final
		// mentionMappingResult set. Return the mention results.
		result.mentionResults = data.getGeneDocument().getGenes().stream().map(g -> g.getMentionMappingResult())
				.collect(Collectors.toList());
		for (MentionMappingResult mentionResult : result.mentionResults) {
			mentionResult.resultEntry = mentionResult.resultEntry == MentionMappingResult.REJECTION
					? MentionMappingResult.REJECTION
					: mentionResult.bestCandidate;
		}
		return result;
	}

	private void removeNondescriptives(Set<GeneMention> genes) {
		for (Iterator<GeneMention> it = genes.iterator(); it.hasNext();) {
			String normalizedText = it.next().getNormalizedText();
			String filteredText = normalizer.removeNonDescriptives(normalizedText);
			filteredText = GeneMapper.removeUnspecifieds(filteredText);
			if (filteredText.equals("") || filteredText.equals("s")) {
				it.remove();
			}
		}
	}

	private void filterUnspecifieds(Set<GeneMention> genes) {
		for (Iterator<GeneMention> it = genes.iterator(); it.hasNext();) {
			String normalizedText = it.next().getNormalizedText();
			String filteredText = GeneMapper.removeNondescriptives(normalizedText);
			filteredText = GeneMapper.removeDomainFamilies(normalizedText);
			filteredText = GeneMapper.removeUnspecifieds(filteredText);
			if (filteredText.equals("") || filteredText.equals("s")) {
				it.remove();
			}
		}
	}

	/**
	 * Assigns the field
	 * <tt>gm.getMentionMappingResult().semanticallyOrderedCandidates</tt>.
	 * 
	 * @param genes
	 *            The genes to score semantically.
	 * @throws GeneMapperException
	 *             In case something unforeseen goes wrong.
	 */
	private void assignSemanticScores(Set<GeneMention> genes) throws GeneMapperException {
		try {
			for (GeneMention gm : genes) {
				// Compare the textual context of the gene mention with its
				// database context information and set the semantic score to
				// its candidates
//				if (null ==  gm.getMentionMappingResult().filteredCandidates ||
//						   gm.getMentionMappingResult().filteredCandidates.isEmpty()) {
//					log.info("Gene mention {} in document {} doesn't have any candidates", gm.getText(), gm.getDocId());
//					continue;
//				} else 
					if (gm.getMentionMappingResult().bestCandidate.equals(MentionMappingResult.REJECTION)) {
					continue;
				}
				scoreSemantically(gm.getDocumentContext(), gm.getMentionMappingResult().filteredCandidates, false);
				// now create a new list with all the filtered candidates but
				// ordered by semantic score, such that the element with the
				// highest score comes first
				List<SynHit> sortedBySemanticScore = gm.getMentionMappingResult().filteredCandidates.stream().map(s -> {
					s.setCompareType(SynHit.CompareType.SEMSCORE);
					return s;
				}).sorted().collect(Collectors.toList());
				// set the semantically ordered list to the mapping result for
				// further processing
				MentionMappingResult result = gm.getMentionMappingResult();
				result.semanticallyOrderedCandidates = sortedBySemanticScore;
				result.bestCandidate = sortedBySemanticScore.get(0);
			}
		} catch (IOException e) {
			throw new GeneMapperException(e);
		}
	}

	/**
	 * For exact matches, removes all non-exact matches. For approximate
	 * matches, filters away candidates where numbers do not match the numbers
	 * in the gene mention, if present.
	 * 
	 * @param genes
	 *            The genes for which candidates should be filtered.
	 */
	private void filterCandidates(Set<GeneMention> genes) {
		for (GeneMention gm : genes) {
			MentionMappingResult mentionMappingResult = gm.getMentionMappingResult();
			List<SynHit> candidates = mentionMappingResult.originalCandidates;
			if (candidates.isEmpty()) {
				log.debug("Gene mention {} in document {} doesn't have any candidates", gm.getText(), gm.getDocId());
				continue;
			}

			ArrayList<SynHit> exactHits = new ArrayList<>();
			for (SynHit candidate : candidates) {
				if (candidate.isExactMatch())
					exactHits.add(candidate);
				else
					break;
			}

			// make a difference between exact and non-exact matches
			List<SynHit> approxMatches = new ArrayList<>();
			if (!exactHits.isEmpty()) {
				mentionMappingResult.filteredCandidates = exactHits;
				mentionMappingResult.matchType = MentionMappingResult.MatchType.EXACT;
				mentionMappingResult.resultEntry = exactHits.get(0);
				mentionMappingResult.ambiguityDegree = exactHits.size();
				gm.setMentionMappingResult(mentionMappingResult);
			} else {
				mentionMappingResult.matchType = MentionMappingResult.MatchType.APPROX;

//				final String normalizedMention = normalizer.normalize(gm.getText());	
				final String normalizedMention = gm.getNormalizedText();	
				SynHit bestHit = candidates.get(0);
				// copy the original candidate list so we can remove filtered
				// candidates
				List<SynHit> filteredCandidates = new ArrayList<>(candidates);
				// performing some filtering, not so special
				for (Iterator<SynHit> it = filteredCandidates.iterator(); it.hasNext();) {
					SynHit next = it.next();
					Multiset<String> mentionNumbers = FeatureUtils.getNumbers(normalizedMention.split("\\s"));
					Multiset<String> synNumbers = FeatureUtils.getNumbers(next.getSynonym().split("\\s"));
					if (!synNumbers.isEmpty() && !Multisets.difference(mentionNumbers, synNumbers).isEmpty())
						it.remove();
				}
				if (filteredCandidates.isEmpty())
					filteredCandidates.add(bestHit);

				approxMatches.addAll(filteredCandidates);

				//List<SynHit> bestScoredApproxHits = approxMatches;
//				double bestMentionScore = filteredCandidates.get(0).getMentionScore();
				double bestMentionScore = candidates.get(0).getMentionScore();
				List<SynHit> bestScoredApproxHits = new ArrayList<>();
				for (SynHit approxHit : filteredCandidates) {
//				for (SynHit approxHit : candidates) {
					if (approxHit.getMentionScore() < bestMentionScore)
						break;
					bestScoredApproxHits.add(approxHit);
				}
				if (bestScoredApproxHits.isEmpty()) {
					bestScoredApproxHits.add(candidates.get(0));
				}
				
				mentionMappingResult.filteredCandidates = bestScoredApproxHits;
				mentionMappingResult.resultEntry = bestScoredApproxHits.get(0);
				mentionMappingResult.ambiguityDegree = bestScoredApproxHits.size();
				gm.setMentionMappingResult(mentionMappingResult);
			}
		}
	}

	private void agglomerateByIds(GeneSets geneSets) {
		// TODO reduce the number of sets in geneSets by merging those sets
		// where the candidate lists
		// Note that each GeneSet knows the ID that each element of the set
		// should get at the end. This must be set to the "correct" ID, where of
		// cause the hard question is, which ID it should be.
		// Also note that we have two rankings of candidates for each gene
		// mention: The "filtered candidates" and the "semantically ordered
		// candidates" found in the gene mention's mentionMappingResult. The
		// first list contains the candidates ordered by mention score, the
		// second is ordered by semantic (context comparison) score.
		HashMap<SynHitSet, HashSet<GeneMention>> synHitMap = new HashMap<>();
		HashMap<GeneMention, Integer> counter = new HashMap<>();
		//Find all sets of GeneMentions, where the set of candidates overlap
		for (GeneSet geneSet : geneSets) {
			for (GeneMention gm : geneSet) {
				List<SynHit> candidates = gm.getMentionMappingResult().filteredCandidates;
				if (null == candidates) {
					continue;
				}
				SynHitSet synHits = new SynHitSet(candidates);
				if (synHitMap.isEmpty()) {
					HashSet<GeneMention> temp = new HashSet<>();
					temp.add(gm);
					synHitMap.put(synHits, temp);
					counter.put(gm, 1);
				} else {
					int count = 0;
					for (Entry<SynHitSet, HashSet<GeneMention>> entry : synHitMap.entrySet()) {
						SynHitSet s = entry.getKey();
						if (s.containsAny(candidates)) {
							s.retainAll(synHits);
							//Could possibly been added to more than one list.
							entry.getValue().add(gm);
							++count;
						}
					}
					
					if (count == 0) {
						HashSet<GeneMention> temp = new HashSet<GeneMention>();
						temp.add(gm);
						synHitMap.put(synHits, temp);
						counter.put(gm, 1);
					} else {
						counter.put(gm, count);
					}
				}
			}			
		}
		
		// Take care of all GeneMentions that ended up in more than one set
		HashMap<GeneMention, List<SynHit>> ambiguous = new HashMap<>();
		for (Iterator<Entry<SynHitSet, HashSet<GeneMention>>> it = synHitMap.entrySet().iterator(); it.hasNext();) {
			Entry<SynHitSet, HashSet<GeneMention>> entry = it.next();
			for (Iterator<GeneMention> gmIt = entry.getValue().iterator(); gmIt.hasNext();) {
				GeneMention gm = gmIt.next();
				int count = counter.get(gm);
				if (count > 1) {
					//Merge SynHit candidates together
					if (ambiguous.containsKey(gm)) {
						List<SynHit> synHits = ambiguous.get(gm);
						synHits.addAll(entry.getKey());
					} else {
						List<SynHit> synHits = new ArrayList<SynHit>();
						synHits.addAll(entry.getKey());
						ambiguous.put(gm, synHits);
					}
					// Entries in the map will have only one candidate set
					gmIt.remove();
				}
			}
		}
		
		geneSets.clear();
		for (Iterator<Entry<GeneMention, List<SynHit>>> it = ambiguous.entrySet().iterator(); it.hasNext();) {
			Entry<GeneMention, List<SynHit>> entry = it.next();
			List<SynHit> synHits = entry.getValue();
			for (SynHit synHit : synHits) {
				synHit.setCompareType(SynHit.CompareType.SCORE);
			}
			Collections.sort(synHits);
			
			GeneSet geneSet = new GeneSet();
			GeneMention gm = entry.getKey();
			MentionMappingResult result = gm.getMentionMappingResult();
			result.filteredCandidates = synHits;
			result.bestCandidate = synHits.get(0);
			geneSet.setSetId(synHits.get(0));
			geneSet.add(gm);
			geneSets.add(geneSet);
		}
		
		// Re-enter all other GeneMentions into the GeneSets again
		for (Iterator<Entry<SynHitSet, HashSet<GeneMention>>> it = synHitMap.entrySet().iterator(); it.hasNext();) {
			Entry<SynHitSet, HashSet<GeneMention>> entry = it.next();
			List<SynHit> synHits = new ArrayList<SynHit>(entry.getKey());
			for (SynHit synHit : synHits) {
				synHit.setCompareType(SynHit.CompareType.SCORE);
			}
			Collections.sort(synHits);
			SynHit bestHit = synHits.get(0);
			
			HashSet<GeneMention> genes = entry.getValue();
			for (GeneMention gm : genes) {
				MentionMappingResult result = gm.getMentionMappingResult();
				result.filteredCandidates = synHits;
				result.bestCandidate = bestHit;
			}
			GeneSet geneSet = new GeneSet(genes, bestHit);
			geneSets.add(geneSet);
		}		
	}

	/**
	 * Merges those gene sets that are connected via acronym resolution.
	 * 
	 * @param geneSets
	 *            The gene sets to agglomerate.
	 * @param geneDocument
	 *            The original gene document having information about all genes,
	 *            acronyms and their positions.
	 * @param maxCandidates 
	 */
	private void agglomerateByAcronymPosition(GeneSets geneSets, GeneDocument geneDocument, int maxCandidates) {
		Set<Acronym> acronyms = geneDocument.getAcronyms();
		if (acronyms.size() == 0) {
			return;
		}
		NavigableSet<Range<Integer>> longforms = new TreeSet<Range<Integer>>(new Comparator<Range<Integer>>() {

		@Override
		public int compare(Range<Integer> gm1, Range<Integer> gm2) {
			if (gm1.getMinimum() == gm2.getMinimum()) {
				if (gm1.getMaximum() == gm2.getMaximum()) {
					return 0;
				} else if (gm1.getMaximum() < gm2.getMaximum()) {
					return -1;
				} else {
					return 1;
				}
			} else if (gm1.getMinimum() < gm2.getMinimum()) {
				return -1;
			} else {
				return 1;
			}
		}
			
		});
		
		HashMap<Range<Integer>, GeneSet> mergedSets = new HashMap<>();
		for (Acronym acronym : acronyms) {
			AcronymLongform longform = acronym.getLongform();
			longforms.add(longform.getOffsets());
		}
		
		Range<Integer> longformOffsets = null;
		for (GeneSet geneSet : geneSets) {
			for (GeneMention gm : geneSet) {
				Range<Integer> offsets = gm.getOffsets();
				if (longforms.contains(offsets)) {
				// As comparison is done via ranges, two genes are equal,
				// if they cover the same range, even if their respective other
				// values are different
					if (mergedSets.containsKey(offsets)) {
						GeneSet gs = mergedSets.get(offsets);
						gs.add(gm);
					} else {
						mergedSets.put(offsets, geneSet);
					}
					continue;
				}
				if (null != (longformOffsets = longforms.floor(offsets))) {
					if (longformOffsets.isOverlappedBy(gm.getOffsets())) {
						if (mergedSets.containsKey(longformOffsets)) {
							GeneSet gs = mergedSets.get(longformOffsets);
							gs.add(gm);
						} else {
							mergedSets.put(longformOffsets, geneSet);
						}
						continue;
					}
				}
				if (null != (longformOffsets = longforms.ceiling(offsets))) {
					if (longformOffsets.isOverlappedBy(gm.getOffsets())) {
						if (mergedSets.containsKey(longformOffsets)) {
							GeneSet gs = mergedSets.get(longformOffsets);
							gs.add(gm);
						} else {
							mergedSets.put(longformOffsets, geneSet);
						}
						continue;
					}
				}
				mergedSets.put(offsets, geneSet);
			}
		}

		geneSets.clear();
		for (Iterator<GeneSet> it = mergedSets.values().iterator(); it.hasNext();) {
			GeneSet geneSet = it.next();

			TreeMap<SynHit, List<Double>> scoringMap = new TreeMap<>(new Comparator<SynHit>() {
				public int compare(SynHit s1, SynHit s2) {
					return s1.getId().compareTo(s2.getId());
				}}
			);
			
			SynHit setId = MentionMappingResult.REJECTION;
			for (GeneMention gm : geneSet) {
				MentionMappingResult mmr = gm.getMentionMappingResult();
				List<SynHit> synHits = mmr.filteredCandidates;
				if (null == synHits) {
					continue;
				}
				int synCounter = 0;

				//Gather the most meaningful candidates in a map (id & scores)
				for (Iterator<SynHit> synIt = synHits.iterator();
						synIt.hasNext() && synCounter < maxCandidates; ++synCounter) {
					SynHit synHit = synIt.next();
					if (scoringMap.containsKey(synHit)) {
						List<Double> scoring = scoringMap.get(synHit);
						scoring.add(Math.log(synHit.getMentionScore()));
					} else {
						List<Double> scoring = new ArrayList<Double>();
						scoring.add(Math.log(synHit.getMentionScore()));
						scoringMap.put(synHit, scoring);
					}
				}
			}
				
			TreeMap<Double, SynHit> scores = new TreeMap<Double, SynHit>();
			//Compute geometric mean of all candidates
			for (Iterator<Entry<SynHit, List<Double>>> scIt = scoringMap.entrySet().iterator(); scIt.hasNext();) {
				double mean = 0.0;
				Entry<SynHit, List<Double>> entry = scIt.next();
				List<Double> indivScores = entry.getValue();
				for (double score : indivScores) {
					mean += score;
				}
				mean /= indivScores.size();
				scores.put(mean, entry.getKey());
			}
			
			if (scores.size() > 0) {
				setId = scores.lastEntry().getValue();
			}
			geneSet.setSetId(setId);
			
			for (GeneMention gm : geneSet) {
				MentionMappingResult mmr = gm.getMentionMappingResult();
				mmr.resultEntry = setId;
			}
			
			geneSets.add(geneSet);
		}
	}

	/**
	 * Merges those gene sets that are connected via acronym resolution.
	 * 
	 * @param geneSets
	 *            The gene sets to agglomerate.
	 * @param geneDocument
	 *            The original gene document having information about all genes,
	 *            acronyms and their positions.
	 * @param maxCandidates 
	 */
	private void agglomerateByAcronyms(GeneSets geneSets, GeneDocument geneDocument, int maxCandidates) {
		Set<Acronym> docAcronyms = geneDocument.getAcronyms();
		if (docAcronyms.size() == 0) {
			return;
		}
		
		HashMap<String, GeneSet> mergedSets = new HashMap<>();
		List<ImmutablePair<String, String>> acronyms = new ArrayList<>();
		for (Acronym a : docAcronyms) {
			String acronym = a.getAcronym();
			if (null == acronym) {
				Range<Integer> range = a.getOffsets();
				acronym = geneDocument.getDocumentText().substring(range.getMinimum(), range.getMaximum());
				//There was at least one acronym that started with a whitespace
				acronym = acronym.trim();
				a.setAcronym(acronym);
			}
			AcronymLongform lf = a.getLongform();
			String longform = lf.getText();
			if (null == longform) {
				Range<Integer> range = lf.getOffsets();
				longform = geneDocument.getDocumentText().substring(range.getMinimum(), range.getMaximum());
			}
			acronyms.add(new ImmutablePair<String, String>(acronym, longform));
			mergedSets.put(longform, new GeneSet());
		}

		for (GeneSet geneSet : geneSets) {
			for (GeneMention gm : geneSet) {
				MentionMappingResult mmr = gm.getMentionMappingResult();
				List<SynHit> candidates;
				switch (agglomerationCandidates) {
					case filtered:
						candidates = mmr.filteredCandidates;
						break;
					case original:
						candidates = mmr.originalCandidates;
						break;
					case semantic:
						candidates = mmr.semanticallyOrderedCandidates;
						break;
					case strictlyfiltered:
						candidates = mmr.filteredCandidates;
						break;
					default:
						candidates = mmr.filteredCandidates;
						break;
				}
				
				if (null == candidates) {
					if (agglomerationCandidates == AgglomerationCandidateType.filtered) {
						candidates = mmr.originalCandidates;
					} else {
						continue;
					}
				}
				
				int synCounter = 0;
				double score = -1.0;
				SynHit bestCandidate = null;
				String matchingAcronym = null;
				for (Iterator<SynHit> synIt = candidates.iterator();
						synIt.hasNext() && synCounter < maxCandidates; ++synCounter) {
					SynHit candidate = synIt.next();
					String mention = candidate.getMappedMention();
					for (ImmutablePair<String, String> acronym : acronyms) {
						if (mention.equalsIgnoreCase(acronym.left) || mention.equalsIgnoreCase(acronym.right)) {
							if (score < candidate.getMentionScore()) {
								score = candidate.getMentionScore();
								bestCandidate = candidate;
								matchingAcronym = acronym.right;
							}
						}
					}
				}
				
				if (null != matchingAcronym) {
					GeneSet g = mergedSets.get(matchingAcronym);
					if (null == g.getSetId()) {
						g.setSetId(bestCandidate);
					}
					gm.getMentionMappingResult().bestCandidate = bestCandidate;
					g.add(gm);
				} else {
					String mention = mmr.bestCandidate.getMappedMention();
					if (mergedSets.containsKey(mention)) {
						GeneSet g = mergedSets.get(mention);
						g.add(gm);
					} else {
						GeneSet g = new GeneSet();
						g.add(gm);
						g.setSetId(mmr.bestCandidate);
						mergedSets.put(mention, g);
					}
				}
			}
		}

		geneSets.clear();
		for (GeneSet gs : mergedSets.values()) {
			geneSets.add(gs);
		}
	}

	public enum NameLevelUnificationStrategy {
		JNET_FIRST, GAZETTEER_FIRST, LONGER_FIRST
	}

	/**
	 * This method is run at the beginning of the disambiguation process. Its
	 * main purpose is to reduce the number of genes by finding overlapping
	 * genes (e.g. due to multiple gene taggers) and, if possible, decide for a
	 * single gene mentions. This is easy in the common case that the names are
	 * just the same. However, sometimes one annotation is embedded into another
	 * or they just overlap without one annotation completely covering the
	 * other. This method is not required to solve all cases but only those that
	 * can be decided on the name level.
	 * 
	 * @param geneDoc
	 *            The document gene information.
	 */
	private void unifyOverlappingGenesOnNameLevel(GeneDocument geneDoc,
			Set<NameLevelUnificationStrategy> unificationStrategies) {
		// TODO Use an iterator to go over all genes in geneDoc and remove
		// duplicates where and how it makes sense (which may be subject to
		// experimentation). At this point, we mostly work with name level
		// information, e.g. mention score, equal names, which component was
		// tagging the gene, but also possibly things like conjunction detection.
		// Not all duplicates must be removed
		// here.
		// remove exact duplicates (same offsets)
		// experiment with existing and new NameLevelUnificationStrategies
		if (geneDoc.getGenes().size() < 2) {
			return;
		}
		
		NavigableSet<GeneMention> sortedGenes = new TreeSet<GeneMention>(new Comparator<GeneMention>() {

			@Override
			public int compare(GeneMention gm1, GeneMention gm2) {
				if (gm1.getBegin() == gm2.getBegin()) {
					if (gm1.getEnd() == gm2.getEnd()) {
						return 0;
					} else if (gm1.getEnd() < gm2.getEnd()) {
						return -1;
					} else {
						return 1;
					}
				} else if (gm1.getBegin() < gm2.getBegin()) {
					return -1;
				} else {
					return 1;
				}
			}
			
		});
		
		if (unificationStrategies.contains(NameLevelUnificationStrategy.JNET_FIRST)) {
			geneDoc.unifyGenesPrioritizeTagger(sortedGenes, GeneTagger.JNET);
		} else if (unificationStrategies.contains(NameLevelUnificationStrategy.GAZETTEER_FIRST)) {
			geneDoc.unifyGenesPrioritizeTagger(sortedGenes, GeneTagger.GAZETTEER);
		} else if (unificationStrategies.contains(NameLevelUnificationStrategy.LONGER_FIRST)) {
			geneDoc.unifyGenesLongerFirst(sortedGenes);
		}
		//TODO: default option?
		
//		geneDoc.removeExactDuplicates(unificationStrategies);
//		GeneMention gm = geneDoc.getGenes().iterator().next();

//		sortedGenes.addAll(geneDoc.getGenes());

		// genes also know their candidates through their MentionMappingResult
		// object. You can get an already sorted list
		// of database candidates with
//		List<SynHit> candidates = gm.getMentionMappingResult().originalCandidates;
		// each SynHit has a "mentionScore" which has been given by our MaxEnt
		// classifier and represents the probability that the GeneMention name
		// indeed matches the candidate name. This might be useful for partially
		// overlapping gene mentions.

		// the gene doc can be asked who overlaps what:
		//Set<GeneMention> geneMentionsAtOffsets = geneDoc.getGeneMentionsAtOffsets(Range.between(1, 5));

		// further helpful methods should just go into the respective classes
	}

	public SynHit scoreSemantically(String documentContext, List<SynHit> matches, boolean resort)
			throws IOException, CorruptIndexException {
		Map<String, String> geneContextById = semanticIndex.retrieveGeneContexts(matches);
		for (SynHit hit : matches) {
			Double contextScore = contextScoreCache.get(hit.getId());
			if (null == contextScore) {
				String geneContext = geneContextById.get(hit.getId());
				contextScore = contextScorer.getNormalizedNameScore(geneContext, documentContext);
				contextScoreCache.put(hit.getId(), contextScore);
			}
			hit.setSemanticScore(contextScore);
			if (resort)
				hit.setCompareType(SynHit.CompareType.SEMSCORE);
		}
		if (resort)
			Collections.sort(matches);

		SynHit hit = null;
		if (!matches.isEmpty())
			hit = matches.get(0);
		return hit;
	}
	
	@Override
	public SemanticIndex getSemanticIndex() {
		return semanticIndex;
	}

	public int getMaxAgglomerationCandidates() {
		return maxAgglomerationCandidates;
	}

	public void setMaxAgglomerationCandidates(int maxCandidates) {
		this.maxAgglomerationCandidates = maxCandidates;
	}

}
