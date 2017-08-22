package de.julielab.jules.ae.genemapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.julielab.jules.ae.genemapper.SynHit.CompareType;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.genemodel.GeneName;
import de.julielab.jules.ae.genemapper.index.SynonymIndexFieldNames;
import de.julielab.jules.ae.genemapper.scoring.JaroWinklerScorer;
import de.julielab.jules.ae.genemapper.scoring.LevenshteinScorer;
import de.julielab.jules.ae.genemapper.scoring.MaxEntScorer;
import de.julielab.jules.ae.genemapper.scoring.Scorer;
import de.julielab.jules.ae.genemapper.scoring.SimpleScorer;
import de.julielab.jules.ae.genemapper.scoring.TFIDFScorer;
import de.julielab.jules.ae.genemapper.scoring.TokenJaroSimilarityScorer;
import de.julielab.jules.ae.genemapper.utils.GeneCandidateRetrievalException;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.TFIDFUtils;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class LuceneCandidateRetrieval implements CandidateRetrieval {
	public Scorer getScorer() {
		return scorer;
	}

	public static final String LOGGER_NAME_CANDIDATES = "de.julielab.jules.ae.genemapper.candidates";

	public static final int SIMPLE_SCORER = 0;
	public static final int TOKEN_JAROWINKLER_SCORER = 1;
	public static final int MAXENT_SCORER = 2;
	public static final int JAROWINKLER_SCORER = 3;
	public static final int LEVENSHTEIN_SCORER = 4;
	public static final int TFIDF = 5;
	public static final int LUCENE_SCORER = 10;

	/**
	 * default model for MaxEntScorer
	 */
	public static final String MAXENT_SCORER_MODEL = "/genemapper_jules_mallet.mod";

	// the model to be loaded for MaxEnt scorer
	// (can be specified in properties file)
	private String maxEntModel = MAXENT_SCORER_MODEL;

	private static final Logger log = LoggerFactory.getLogger(CandidateRetrieval.class);
	public static final Logger candidateLog = LoggerFactory.getLogger(LOGGER_NAME_CANDIDATES);

	/**
	 * the maximal number of hits lucene returns for a query
	 */
	private static final int LUCENE_MAX_HITS = 100;

	private TermNormalizer normalizer;
	private IndexSearcher mentionIndexSearcher;
	private Scorer scorer;

	/**
	 * This static map is supposed to make candidate caches available for all
	 * instances of this class across the JVM. This is important since we often
	 * use multiple gene-mapper instances in the same pipeline. It can save a
	 * lot of time and also space.
	 */
	private static ConcurrentHashMap<String, LoadingCache<CandidateCacheKey, ArrayList<SynHit>>> caches = new ConcurrentHashMap<>();

	private LoadingCache<CandidateCacheKey, ArrayList<SynHit>> candidateCache;

	public static class CandidateCacheKey {
		/**
		 * Gets gene candidates based only on the name making no restrictions on
		 * species.
		 * 
		 * @param geneName
		 */
		public CandidateCacheKey(GeneName geneName) {
			this(geneName, null);
		}

		@Override
		public String toString() {
			return "CandidateCacheKey [geneName=" + geneName + ", taxId=" + taxId + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((geneName == null) ? 0 : geneName.hashCode());
			result = prime * result + ((taxId == null) ? 0 : taxId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CandidateCacheKey other = (CandidateCacheKey) obj;
			if (geneName == null) {
				if (other.geneName != null)
					return false;
			} else if (!geneName.equals(other.geneName))
				return false;
			if (taxId == null) {
				if (other.taxId != null)
					return false;
			} else if (!taxId.equals(other.taxId))
				return false;
			return true;
		}

		/**
		 * Gets gene candidates accordingi to <tt>geneName</tt> but restricted
		 * to species with taxonomy ID <tt>taxId</tt>.
		 * 
		 * @param geneName
		 * @param taxId
		 */
		public CandidateCacheKey(GeneName geneName, String taxId) {
			this.geneName = geneName;
			this.taxId = taxId;
		}

		public GeneName geneName;
		public String taxId;

	}

	@Deprecated
	public LuceneCandidateRetrieval(IndexSearcher mentionIndexSearcher, Scorer scorer) throws IOException {
		this.mentionIndexSearcher = mentionIndexSearcher;
		this.scorer = scorer;
		normalizer = new TermNormalizer();
	}

	public LuceneCandidateRetrieval(GeneMapperConfiguration config) throws GeneMapperException {
		// lucene mention index
		String mentionIndex = config.getProperty("mention_index");
		if (mentionIndex == null) {
			throw new GeneMapperException("mention index not specified in configuration file (critical).");
		}

		try {
			RAMDirectory ramDir = new RAMDirectory(mentionIndex);
			log.debug("mention index loaded.");
			mentionIndexSearcher = new IndexSearcher(ramDir);

			// scorer type
			String scorerType = config.getProperty("scorer_type");
			if (scorerType != null) {
				this.setScorerType((new Integer(scorerType)).intValue());
			}

			// maxent model
			String maxEntModel = config.getProperty("maxent_model");
			if (maxEntModel != null) {
				this.maxEntModel = maxEntModel;
			}

			this.normalizer = new TermNormalizer();
		} catch (IOException e) {
			throw new GeneMapperException(e);
		}

		// get an existing candidate cache for the given mention index or create
		// it; the lock avoids the creation of multiple caches due to
		// concurrency issues
		synchronized (caches) {
			candidateCache = caches.get(mentionIndex);
			if (null == candidateCache) {
				log.info("Creating new gene candidate cache for index {}", mentionIndex);
				candidateCache = CacheBuilder.newBuilder().maximumSize(1000000).expireAfterWrite(60, TimeUnit.MINUTES)
						.build(new CacheLoader<CandidateCacheKey, ArrayList<SynHit>>() {
							public ArrayList<SynHit> load(CandidateCacheKey key) throws IOException, BooleanQuery.TooManyClauses {
								return getCandidatesFromIndexWithoutCache(key);
							}
						});
				if (null != caches.put(mentionIndex, candidateCache))
					throw new IllegalStateException("There already is a candidate index for " + mentionIndex
							+ " which points to a faulty concurrency implementation");
			} else {
				log.info("Using existing gene candidate cache for index {}", mentionIndex);
			}
		}
	}

	public void setScorerType(int type) {
		if (type == SIMPLE_SCORER) {
			scorer = new SimpleScorer();
		} else if (type == TOKEN_JAROWINKLER_SCORER) {
			scorer = new TokenJaroSimilarityScorer();
		} else if (type == MAXENT_SCORER) {
			if (!maxEntModel.equals(MAXENT_SCORER_MODEL)) {
				// InputStream in =
				// this.getClass().getResourceAsStream(MAXENT_SCORER_MODEL);
				scorer = new MaxEntScorer(new File(maxEntModel));
			} else {
				InputStream in = this.getClass().getResourceAsStream(MAXENT_SCORER_MODEL);
				scorer = new MaxEntScorer(in);
			}
		} else if (type == JAROWINKLER_SCORER) {
			scorer = new JaroWinklerScorer();
		} else if (type == LUCENE_SCORER) {
			scorer = null;
		} else if (type == LEVENSHTEIN_SCORER) {
			scorer = new LevenshteinScorer();
		} else if (type == TFIDF) {
			TFIDFUtils tfidfNormalizedName = new TFIDFUtils();
			TFIDFUtils tfidfOriginalName = new TFIDFUtils();
			TFIDFUtils tfidfNormalizedVariant = new TFIDFUtils();
			tfidfOriginalName.learnFromLuceneIndex(mentionIndexSearcher.getIndexReader(),
					SynonymIndexFieldNames.ORIGINAL_NAME);
			tfidfNormalizedName.learnFromLuceneIndex(mentionIndexSearcher.getIndexReader(),
					SynonymIndexFieldNames.LOOKUP_SYN_FIELD);
			tfidfNormalizedVariant.learnFromLuceneIndex(mentionIndexSearcher.getIndexReader(),
					SynonymIndexFieldNames.VARIANT_NAME);
			scorer = new TFIDFScorer(tfidfOriginalName, tfidfNormalizedName, tfidfNormalizedVariant);
		} else {
			log.warn("setScorerType() - unknown scorer type, keeping default scorer.");
			// fall back: set default
			scorer = new SimpleScorer();
		}
	}

	public String getScorerInfo() {
		if (scorer == null) {
			return "Lucene Score (unnormalized)";
		} else {
			return scorer.info();
		}
	}

	public int getScorerType() {
		return scorer.getScorerType();
	}

	@Override
	public List<SynHit> getCandidates(String originalSearchTerm) throws GeneCandidateRetrievalException {
		GeneMention geneMention = new GeneMention(originalSearchTerm, normalizer);
		return getCandidates(geneMention);
	}

	@Override
	public List<SynHit> getCandidates(GeneMention geneMention) throws GeneCandidateRetrievalException {
		return getCandidates(geneMention, geneMention.getTaxonomyIds());
	}

//	public ArrayList<SynHit> getCandidates(CandidateCacheKey key) throws GeneCandidateRetrievalException {
//
//		try {
//			TopDocs foundDocs = getCandidatesFromIndex(key);
//			// 2. assign score
//			ArrayList<SynHit> scoredHits = new ArrayList<SynHit>();
//			try {
//				scoredHits = scoreHits(foundDocs, key.geneName);
//			} catch (IOException e) {
//				e.printStackTrace();
//				log.error("getCandidates() - error scoring hits: " + e.getMessage());
//			}
//			// 3. combine single hits to candidate clusters
//			ArrayList<SynHit> hits = combineHits(scoredHits);
//			// 4. sort by SynHit's score (lucene score)
//			Collections.sort(hits);
//
//			// -------- TODO testing
//			// if (!ApproximateMatchUtils.hasExactHits(hits)) {
//			// GeneMention mention = new GeneMention();
//			// mention.text = geneMention.getMention();
//			//
//			// hits = (ArrayList<SynHit>)
//			// ApproximateMatchUtils.seekExactHitCandidates(mention, hits,
//			// this);
//			// }
//			// -------- TODO testing
//			log.debug("Returning {} candidates for gene mention {}", hits.size(), key.geneName.getText());
//			// scoredHits = null;
//			return hits;
//			// return scoredHits;
//		} catch (ExecutionException e) {
//			throw new GeneCandidateRetrievalException(e);
//		}
//	}
	
	@Override
	public List<SynHit> getCandidates(GeneMention geneMention, Collection<String> organisms)
			throws GeneCandidateRetrievalException {
		try {
			ArrayList<SynHit> hits = new ArrayList<>();
			CandidateCacheKey key = new CandidateCacheKey(geneMention.getGeneName());
			if (organisms.isEmpty()) {
				hits = getCandidatesFromIndex(key);
				log.debug("Returning {} candidates for gene mention {}", hits.size(), key.geneName.getText());
			}
			for (String taxonomyId : organisms) {
				key.taxId = taxonomyId;
				hits.addAll(getCandidatesFromIndex(key));
//			TopDocs foundDocs = getCandidatesFromIndex(key);
				// 2. assign score
//			List<SynHit> scoredHits = new ArrayList<SynHit>();
//				scoredHits = scoreHits(foundDocs, key.geneName);
				// 3. combine single hits to candidate clusters
//			ArrayList<SynHit> hits = combineHits(scoredHits);
				// 4. sort by SynHit's score (lucene score)

				// -------- TODO testing
				// if (!ApproximateMatchUtils.hasExactHits(hits)) {
				// GeneMention mention = new GeneMention();
				// mention.text = geneMention.getMention();
				//
				// hits = (ArrayList<SynHit>)
				// ApproximateMatchUtils.seekExactHitCandidates(mention, hits,
				// this);
				// }
				// -------- TODO testing
				log.debug("Returning {} candidates for gene mention {} for taxonomy ID {}", new Object[] {hits.size(), key.geneName.getText(), organisms});
			}
			hits = combineHits(hits);
			hits.stream().forEach(h -> h.setCompareType(CompareType.SCORE));
			Collections.sort(hits);
			return hits;
		} catch (ExecutionException e) {
			throw new GeneCandidateRetrievalException(e);
		}
	}

	private ArrayList<SynHit> getCandidatesFromIndex(CandidateCacheKey key) throws ExecutionException {
		return candidateCache.get(key);
	}

	private ArrayList<SynHit> getCandidatesFromIndexWithoutCache(CandidateCacheKey key)
			throws IOException, BooleanQuery.TooManyClauses {
		// 1. retrieve Lucene hits
		// Query searchQuery = QueryGenerator.makeDisjunctiveQuery(geneMention);
		Query searchQuery = QueryGenerator.makeDisjunctionMaxQuery(key);
		Filter filter = null;
		// if (!StringUtils.isBlank(key.taxId)) {
		// CachingWrapperFilter taxIdFilter = new CachingWrapperFilter(
		// new QueryWrapperFilter(new TermQuery(new
		// Term(IndexGenerator.TAX_ID_FIELD, key.taxId))));
		// filter = taxIdFilter;
		//// FilteredQuery filteredQuery = new
		// FilteredQuery(disjunctionMaxQuery, taxIdFilter);
		// }
		TopDocs foundDocs = mentionIndexSearcher.search(searchQuery, filter, LUCENE_MAX_HITS);
		log.debug("searching with query: " + searchQuery + "; found hits: " + foundDocs.totalHits);
		return scoreHits(foundDocs, key.geneName);
//		return foundDocs;
		// // assign score
		// ArrayList<SynHit> scoredHits = new ArrayList<SynHit>();
		// try {
		// scoredHits = scoreHits(foundDocs,
		// geneMention.getNormalizedMention());
		// } catch (Exception e) {
		// e.printStackTrace();
		// LOGGER.error("getCandidates() - error scoring hits: " +
		// e.getMessage());
		// }
		// return scoredHits;
	}

	/**
	 * calculate score for each hit
	 * 
	 * @param hits
	 * @param normalizedSearchTerm
	 * @throws IOException
	 * @throws CorruptIndexException
	 * @throws Exception
	 */
	private ArrayList<SynHit> scoreHits(TopDocs foundDocs, GeneName geneName)
			throws CorruptIndexException, IOException {
		ArrayList<SynHit> allHits = new ArrayList<SynHit>();

		String originalMention = geneName.getText().toLowerCase();
		String normalizedMention = geneName.getNormalizedText();
		String normalizedMentionVariant = geneName.getNormalizedTextVariant();

		ScoreDoc[] scoredDocs = foundDocs.scoreDocs;
		log.debug("ordering candidates for best match to this reference term: " + originalMention + " for top "
				+ scoredDocs.length + " candidates");
		candidateLog.trace("Search term: " + normalizedMention);
		for (int i = 0; i < scoredDocs.length; i++) {
			int docID = scoredDocs[i].doc;
			Document d = mentionIndexSearcher.doc(docID);
			Field idField = d.getField(SynonymIndexFieldNames.ID_FIELD);
			Field originalNameField = d.getField(SynonymIndexFieldNames.ORIGINAL_NAME);
			Field normalizedVariantField = d.getField(SynonymIndexFieldNames.VARIANT_NAME);
			if (null == idField && GeneMapper.LEGACY_INDEX_SUPPORT)
				idField = d.getField(SynonymIndexFieldNames.ID_FIELD_LEGACY);
			String id = idField.stringValue();
			String indexNormalizedName = d.getField(SynonymIndexFieldNames.LOOKUP_SYN_FIELD).stringValue();
			String indexOriginalName = null;
			String indexNormalizedVariant = null;
			if (null != originalNameField)
				indexOriginalName = originalNameField.stringValue();
			if (null != normalizedVariantField)
				indexNormalizedVariant = normalizedVariantField.stringValue();
			if (i < 5)
				candidateLog.trace(
						"Synonym: " + indexNormalizedName + ", original name: " + indexOriginalName + " (" + id + ")");
			// Organism identifier from the NCBI Taxonomy
			String taxId = "";
			if (d.getField(SynonymIndexFieldNames.TAX_ID_FIELD) != null)
				taxId = d.getField(SynonymIndexFieldNames.TAX_ID_FIELD).stringValue();
			// int idSenseFreq = (new
			// Integer(d.getField(IndexGenerator.ID_SENSE_FREQ).stringValue())).intValue();
			// int synSenseFreq = (new
			// Integer(d.getField(IndexGenerator.SYN_SENSE_FREQ).stringValue())).intValue();
			int idSenseFreq = 0;
			int synSenseFreq = 0;
			double score = 0;
			// just set a reasonable default value
			String bestMentionVariant = normalizedMention;
			String bestIndexVariant = indexNormalizedName;
			if (scorer == null || scorer.getScorerType() == GeneMapper.LUCENE_SCORER) {
				// use Lucene scoring
				if (indexNormalizedName.equals(normalizedMention)) {
					// exact matches get perfect score
					score = Scorer.PERFECT_SCORE;
					// score = scoredDocs[i].score;
				} else {
					// approximate matches get lucene score
					score = scoredDocs[i].score;
				}
				// Actually, using the DisMax query, another index field might
				// have given the best hit; but we can't say which. The
				// normalized mention is a reasonable choice because the very
				// most good hits stem from normalized variants.
				bestMentionVariant = normalizedMention;
			} else if (scorer.getScorerType() == GeneMapper.TFIDF) {
				TFIDFScorer tfidfScorer = (TFIDFScorer) scorer;
				double currentScore;
				if ((currentScore = tfidfScorer.getOriginalNameScore(originalMention, indexOriginalName)) > score) {
					score = currentScore;
					bestMentionVariant = originalMention;
					bestIndexVariant = indexOriginalName;
				}
				if ((currentScore = tfidfScorer.getNormalizedNameScore(normalizedMention,
						indexNormalizedName)) > score) {
					score = currentScore;
					bestMentionVariant = normalizedMention;
					bestIndexVariant = indexNormalizedName;
				}
				if ((currentScore = tfidfScorer.getNormalizedVariantScore(normalizedMentionVariant,
						indexNormalizedVariant)) > score) {
					score = currentScore;
					bestMentionVariant = normalizedMentionVariant;
					bestIndexVariant = indexNormalizedVariant;
				}
				// if (normalizedMention.equals("cxcr 4 chemokine receptor")) {
				// System.out.println("Original: " + indexOriginalName + " - " +
				// tfidfScorer.getOriginalNameScore(originalMention,
				// indexOriginalName));
				// System.out.println("Normalized: " + indexNormalizedName+ " -
				// " +tfidfScorer.getNormalizedNameScore(normalizedMention,
				// indexNormalizedName));
				// System.out.println("Variant: " + indexNormalizedVariant+ " -
				// "
				// +tfidfScorer.getNormalizedVariantScore(normalizedMentionVariant,
				// indexNormalizedVariant));
				// }
			} else {
				// use external scoring
				// preliminary experiments: for ME, to take the maximum hurts,
				// only use the score of the normalized terms. For the other
				// scorers the maximum seems to be OK. Overall, the pure Lucene
				// score ranks approximate hits far best and exact hits still
				// very good.
				double currentScore;
				if ((currentScore = scorer.getScore(originalMention, indexOriginalName)) > score) {
					score = currentScore;
					bestMentionVariant = originalMention;
					bestIndexVariant = indexOriginalName;
				}
				if ((currentScore = scorer.getScore(normalizedMention, indexNormalizedName)) > score) {
					score = currentScore;
					bestMentionVariant = normalizedMention;
					bestIndexVariant = indexNormalizedName;
				}
				if ((currentScore = scorer.getScore(normalizedMentionVariant, indexNormalizedVariant)) > score) {
					score = currentScore;
					bestMentionVariant = normalizedMentionVariant;
					bestIndexVariant = indexNormalizedVariant;
				}
			}

			// now make a new synhit object
			// EF CHANGE: added taxId
			// TODO write source into the index (NCBI Gene or UniProt)
			SynHit m = new SynHit(bestIndexVariant, score, id, GeneMapper.SOURCE_DEFINITION, taxId, idSenseFreq,
					synSenseFreq);
			m.setMappedMention(bestMentionVariant);
			allHits.add(m);
		}
		scoredDocs = null;
		return allHits;
	}

	/**
	 * Combines all hits with same ID: only best hit is maintained, all others
	 * are removed.
	 * 
	 */
	private ArrayList<SynHit> combineHits(ArrayList<SynHit> allHits) {
		log.debug("Collapsing hits with the same ID to the entry with the highest mention score");
		HashMap<String, SynHit> hitList = new HashMap<String, SynHit>();
		for (SynHit currHit : allHits) {
			String id = currHit.getId();
			if (id.length() == 0) {
				// TODO: does this happen at all ???
				log.warn("combineHits() - hits with empty ID, ignoring!");
			} else {
				if (hitList.containsKey(id)) {
					// if ID already contained check whether current score is
					// higher
					SynHit oldHit = hitList.get(id);
					if (currHit.getMentionScore() >= oldHit.getMentionScore()) {
						hitList.put(id, currHit);
					}
				} else {
					// just add new hit if ID is not yet contained
					hitList.put(id, currHit);
				}
			}
		}
		// now convert into an ArrayList
		ArrayList<SynHit> combinedHits = new ArrayList<SynHit>();
		for (Iterator<String> iter = hitList.keySet().iterator(); iter.hasNext();) {
			String id = iter.next();
			SynHit currHit = hitList.get(id);
			combinedHits.add(currHit);
		}

		hitList = null;
		return combinedHits;
	}

	// @Override
	// public List<SynHit> getCandidates(CandidateCacheKey key) {
	// try {
	// ArrayList<SynHit> candidates = candidateCache.get(key);
	// ArrayList<SynHit> copy = new ArrayList<>(candidates.size());
	// for (SynHit candidate : candidates) {
	// copy.add((SynHit) candidate.clone());
	// }
	// return copy;
	// } catch (UncheckedExecutionException e) {
	// throw (BooleanQuery.TooManyClauses) e.getCause();
	// } catch (ExecutionException e) {
	// e.printStackTrace();
	// } catch (CloneNotSupportedException e) {
	// e.printStackTrace();
	// }
	// return null;
	// }

	@Override
	public List<SynHit> getCandidates(GeneMention geneMention, String organism) throws GeneCandidateRetrievalException {
		return getCandidates(geneMention, Arrays.asList(organism));
	}

	@Override
	public List<SynHit> getCandidates(String geneMentionText, String organism) throws GeneCandidateRetrievalException {
		return getCandidates(new GeneMention(geneMentionText, normalizer), Arrays.asList(organism));
	}

}
