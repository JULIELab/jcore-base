/** 
 * SemanticDisambiguation.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6
 * Since version:   1.6
 *
 * Creation date: Feb 4, 2008 
 * 
 * does the semantic disambiguation using the context of each of the candidate ids
 **/

package de.julielab.jules.ae.genemapper.disambig;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wcohen.ss.Jaccard;

import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.index.ContextIndexFieldNames;
import de.julielab.jules.ae.genemapper.utils.ContextUtils;

public class SemanticIndex {

	boolean debug = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(SemanticIndex.class);

	public IndexSearcher searcher = null;

	public SemanticIndex(File indexDir) throws IOException {
		// load index
		// Lucene 5.6
//		IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
//		searcher = new IndexSearcher(reader);
		 
		RAMDirectory ramDir = new RAMDirectory(indexDir);
		 searcher = new IndexSearcher(ramDir);
		LOGGER.info("using " + indexDir.getAbsolutePath() + " as semantic disambiguation index with " + searcher.getIndexReader().numDocs()
				+ " gene entries");
		
		String indexPath = indexDir.getAbsolutePath();
		synchronized (caches) {
			geneContextCache = caches.get(indexPath);
			if (geneContextCache == null) {
				LOGGER.info("Creating new gene context cache for index {}", indexPath);
				geneContextCache = CacheBuilder.newBuilder().maximumSize(10000)
						.expireAfterWrite(10, TimeUnit.MINUTES).build();
				if (null != caches.put(indexPath, geneContextCache))
					throw new IllegalStateException("There already is a candidate index for " + indexPath
							+ " which points to a faulty concurrency implementation");
			} else {
				LOGGER.info("Using existing gene context cache for index {}", indexPath);
			}
		}
	}

	/**
	 * does semantic disambiguation, takes best one
	 */
	public SynHit doDisambiguation(ArrayList<SynHit> disambigList, BooleanQuery contextQuery) throws IOException {
		return doDisambiguation(disambigList, contextQuery, 0.0);
	}

	/**
	 * does semantic disambiguation, takes best one if its semantic score is
	 * above or equals minScore threshold
	 */
	public SynHit doDisambiguation(ArrayList<SynHit> disambigList, BooleanQuery contextQuery, double minContextScore)
			throws IOException {
		ArrayList<SynHit> resultList = new ArrayList<SynHit>();

		HashMap<String, SynHit> hits = new HashMap<String, SynHit>();
		TopDocs foundDocs = getContextForSynHits(disambigList, hits, contextQuery);
		ScoreDoc[] scoredDocs = foundDocs.scoreDocs;

		// String[] ids = new String[disambigList.size()];
		// HashMap<String, SynHit> hits = new HashMap<String, SynHit>();
		// for (int i = 0; i < disambigList.size(); i++) {
		// SynHit hit = disambigList.get(i);
		// String id = hit.getId();
		// // context += " " + hit.getSynonym();
		// hits.put(id, hit);
		// ids[i] = id;
		// }
		//
		// LOGGER.debug("number of IDs: " + ids.length);
		// // BooleanQuery contextQuery = makeContextQuery(context);
		// BooleanQuery q = makeQuery(ids, contextQuery);
		// LOGGER.debug("query: " + q);
		// TopDocs foundDocs = searcher.search(q, null, disambigList.size());
		// ScoreDoc[] scoredDocs = foundDocs.scoreDocs;
		// LOGGER.debug("scoredDocs.length: ", scoredDocs.length);
		for (int i = 0; i < scoredDocs.length; i++) {
			Document d = searcher.doc(scoredDocs[i].doc);

			// set new score to synhit
			String id = d.getField(ContextIndexFieldNames.LOOKUP_ID_FIELD).stringValue();

			/*
			 * // display the bag-of-words of the query document and the context
			 * // and compute Jaccard to show common tokens if(debug) { String
			 * matchedContext = d.getField(
			 * ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD).stringValue();
			 * SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English",
			 * ContextUtils.STOPWORDS); TokenStream stream =
			 * sbAnalyzer.tokenStream("context", new StringReader(context));
			 * context = ""; for (Token t = stream.next(); t != null; t =
			 * stream.next()) { context += " " + t.term(); } context =
			 * context.trim(); //Jaccard jac = new Jaccard(); //double jacScore
			 * = jac.score(context, matchedContext); //SoftTFIDF soft = new
			 * SoftTFIDF(jac); //double semScore = soft.score(context,
			 * matchedContext); //System.out.println(jac.explainScore(context,
			 * matchedContext)); }
			 */
			SynHit hit = hits.get(id);
			hit.setSemanticScore(scoredDocs[i].score);
			hit.setCompareType(SynHit.CompareType.SEMSCORE);
			resultList.add(hit);
			// hit.setSemanticScore(semScore);

			LOGGER.debug("hit: " + hit.toString());
			LOGGER.debug("TFIDF semantic score is: " + scoredDocs[i].score);
			// LOGGER.debug("doDisambiguation() - Jaccard score is: " +
			// jacScore);
		}

		// sort hits by new score
		Collections.sort(resultList);

		// TODO: maybe other scoring is also needed (by Cosinus-Measure...)
		// ids = null;
		// q = null;
		// hits = null;
		// foundDocs = null;
		// scoredDocs = null;

		// return best one
		if (resultList.size() == 0)
			return null;
		SynHit bestHit = resultList.get(0);
		LOGGER.debug("doDisambiguation() - bestHit: " + bestHit);
		if (bestHit.getSemanticScore() >= minContextScore)
			return bestHit;
		else
			return null;
	}

	/**
	 * This static map is supposed to make candidate caches available for all
	 * instances of this class across the JVM. This is important since we often
	 * use multiple gene-mapper instances in the same pipeline. It can save a
	 * lot of time and also space.
	 */
	private static ConcurrentHashMap<String, Cache<String, String>> caches = new ConcurrentHashMap<>();
	private Cache<String, String> geneContextCache;
	
	public Map<String, String> retrieveGeneContexts(List<SynHit> candidates)
			throws IOException, CorruptIndexException {
		Map<String, String> docContextById = new HashMap<>();
		// retrieve the contexts of the candidates
		// First check what we have in cache
		List<SynHit> candidatesNotInCache = new ArrayList<>();
		for (SynHit candidate : candidates) {
			String context = geneContextCache.getIfPresent(candidate.getId());
			if (context == null)
				candidatesNotInCache.add(candidate);
			else
				docContextById.put(candidate.getId(), context);
		}

		// load the rest
		if (!candidatesNotInCache.isEmpty()) {
			HashMap<String, SynHit> hitsById = new HashMap<String, SynHit>();
			// here we can cut the number of candidates we continue to work
			// with, e.g. for training data
			// ScoreDoc[] scoreDocs =
			// mapper.getSemanticDisambiguation().getSemanticIndex().getContextForSynHits(
			// candidates.subList(0, Math.min(candidates.size(),
			// candidateCutoff)), hitsById,
			// contextQuery).scoreDocs;
			ScoreDoc[] scoreDocs = getContextForSynHits(candidatesNotInCache, hitsById,
					null).scoreDocs;
			// organize the contexts by ID
			for (ScoreDoc scoreDoc : scoreDocs) {
				Document doc = getSemanticIndexSearcher().doc(scoreDoc.doc);
				String geneId = doc.getField(ContextIndexFieldNames.LOOKUP_ID_FIELD).stringValue();
				String geneContext = doc.getField(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD).stringValue();
				docContextById.put(geneId, geneContext);
				// cache the contexts we have retrieved
				geneContextCache.put(geneId, geneContext);
			}
		}
		return docContextById;
	}
	
	public TopDocs getContextForSynHits(List<SynHit> hits, Map<String, SynHit> id2Hit, BooleanQuery contextQuery)
			throws IOException {
		String[] ids = new String[hits.size()];
		for (int i = 0; i < hits.size(); i++) {
			SynHit hit = hits.get(i);
			String id = hit.getId();
			// context += " " + hit.getSynonym();
			id2Hit.put(id, hit);
			ids[i] = id;
		}

		LOGGER.debug("number of IDs: " + ids.length);
		// BooleanQuery contextQuery = makeContextQuery(context);
		Query q = makeQuery(ids, contextQuery);
		LOGGER.debug("query: " + q);
		TopDocs foundDocs = searcher.search(q, null, hits.size());
		ScoreDoc[] scoredDocs = foundDocs.scoreDocs;
		LOGGER.debug("scoredDocs.length: {}", scoredDocs.length);
		return foundDocs;
	}
	
	public BooleanQuery makeContextQuery(String context) throws IOException {

		BooleanQuery contextQuery = new BooleanQuery();
		SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English",
						ContextUtils.STOPWORDS);
		TokenStream stream = sbAnalyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
					new StringReader(context));

					for (Token t = stream.next(); t != null; t = stream.next()) {
						Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, t.term()));
						contextQuery.add(q, BooleanClause.Occur.SHOULD);
					}
					//LOGGER.debug("makeQuery() - query for disambiguation: " + contextQuery);

					return contextQuery;
		
		
		// lucene 5.6
//		BooleanQuery contextQuery = new BooleanQuery();
//		EnglishAnalyzer analyzer = new EnglishAnalyzer(
//				CharArraySet.copy(new HashSet<String>(Arrays.asList(ContextUtils.STOPWORDS))));
//		TokenStream ts = analyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, new StringReader(context));
//		CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
//		ts.reset();
//		while (ts.incrementToken()) {
//			Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, cattr.toString()));
//			contextQuery.add(q, BooleanClause.Occur.SHOULD);
//		}
//		analyzer.close();
//
//		// LOGGER.debug("makeQuery() - query for disambiguation: " +
//		// contextQuery);
//
//		return contextQuery;
	}

	public Query makeQuery(String[] allowedIDs, BooleanQuery contextQuery) throws IOException {

		// TODO: context of types?
		// context = ContextUtils.makeContextTypes(context);

		// BooleanQuery contextQuery = new BooleanQuery();

		// ids
		BooleanQuery idQuery = new BooleanQuery();
		for (int i = 0; i < allowedIDs.length; i++) {
			String id = allowedIDs[i];
			Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_ID_FIELD, id));
			// TODO: check whether id is the right field to serch in
			idQuery.add(q, BooleanClause.Occur.SHOULD);
			LOGGER.debug("makeQuery() - id added to idQuery: " + id);
		}

		// idQuery.add(contextQuery, BooleanClause.Occur.MUST);
		Query compositeQuery;
		if (null != contextQuery)
		compositeQuery = (BooleanQuery) contextQuery.clone();
		else
			compositeQuery = new MatchAllDocsQuery();
// TODO actually, the IDS should be a filter (quicker, gets cached in Lucene)
//		compositeQuery.add(idQuery, BooleanClause.Occur.MUST);
		CachingWrapperFilter idFilter = new CachingWrapperFilter(
				new QueryWrapperFilter(idQuery));
		FilteredQuery filteredQuery = new FilteredQuery(compositeQuery, idFilter);
		idQuery = null;
		// tokens of context
		/*
		 * TokenStream stream =
		 * sbAnalyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
		 * new StringReader(context));
		 * 
		 * 
		 * for (Token t = stream.next(); t != null; t = stream.next()) { Query q
		 * = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
		 * t.term())); contextQuery.add(q, BooleanClause.Occur.SHOULD); }
		 * //LOGGER.debug("makeQuery() - query for disambiguation: " +
		 * contextQuery);
		 */
		return filteredQuery;
	}

	/**
	 * @param allowedID
	 *            -- only one id is passed
	 * @param contextQuery
	 *            -- the context query
	 * @return BooleanQuery
	 * @throws IOException
	 */
	public BooleanQuery makeQuery(String allowedID, BooleanQuery contextQuery) throws IOException {

		// TODO: context of types?
		// context = ContextUtils.makeContextTypes(context);

		// BooleanQuery contextQuery = new BooleanQuery();

		// ids
		BooleanQuery idQuery = new BooleanQuery();
		Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_ID_FIELD, allowedID));
		// TODO: check whether id is the right field to serch in
		idQuery.add(q, BooleanClause.Occur.SHOULD);
		LOGGER.debug("makeQuery() - id added to idQuery: " + allowedID);

		// idQuery.add(contextQuery, BooleanClause.Occur.MUST);
		BooleanQuery compositeQuery = (BooleanQuery) contextQuery.clone();

		compositeQuery.add(idQuery, BooleanClause.Occur.MUST);
		idQuery = null;
		// tokens of context
		/*
		 * TokenStream stream =
		 * sbAnalyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
		 * new StringReader(context));
		 * 
		 * 
		 * for (Token t = stream.next(); t != null; t = stream.next()) { Query q
		 * = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
		 * t.term())); contextQuery.add(q, BooleanClause.Occur.SHOULD); }
		 * //LOGGER.debug("makeQuery() - query for disambiguation: " +
		 * contextQuery);
		 */
		return compositeQuery;
	}

	/**
	 * does semantic disambiguation on each {@link SynHit} separately, takes
	 * best one if its semantic score is above or equals minScore threshold
	 */
	public SynHit doSeparateDisambiguation(ArrayList<SynHit> disambigList, String context, double minContextScore)
			throws IOException {

		String[] ids = new String[disambigList.size()];
		HashMap<String, SynHit> hits = new HashMap<String, SynHit>();
		for (int i = 0; i < disambigList.size(); i++) {
			SynHit hit = disambigList.get(i);
			String id = hit.getId();
			// context += " " + hit.getSynonym();
			// hits.put(id, hit);
			// ids[i] = id;

			SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English",
					ContextUtils.STOPWORDS);
			TokenStream stream = sbAnalyzer.tokenStream("context",
					new StringReader(context));
			context = "";
			for (Token t = stream.next(); t != null; t = stream.next()) {
				context += " " + t.term();
			}
			
			// lucene 5.6
//			EnglishAnalyzer analyzer = new EnglishAnalyzer(
//					CharArraySet.copy(new HashSet<String>(Arrays.asList(ContextUtils.STOPWORDS))));
//			TokenStream ts = analyzer.tokenStream("context", new StringReader(context));
//			CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
//			ts.reset();
//			context = "";
//			while (ts.incrementToken())
//				context += " " + cattr.toString();
//			
			
			
			context = context.trim();

			BooleanQuery q = makeSeparateQuery(id, context);
			TopDocs foundDocs = searcher.search(q, null, disambigList.size());
			double semScore = foundDocs.getMaxScore();

			ScoreDoc[] scoredDocs = foundDocs.scoreDocs;

			for (int j = 0; scoredDocs.length > 0 && j < 1; j++) {
				Document d = searcher.doc(scoredDocs[j].doc);
				// set new score to synhit
				// String id =
				// d.getField(ContextIndexFieldNames.LOOKUP_ID_FIELD).stringValue();
				String matchedContext = d.getField(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD).stringValue();

				Jaccard jac = new Jaccard();
				// double semScore = jac.score(context, matchedContext);
				// SoftTFIDF soft = new SoftTFIDF(jac);
				// double semScore = soft.score(context, matchedContext);
				System.out.println(jac.explainScore(context, matchedContext));
				// SynHit hit = hits.get(id);
				// hit.setScore(scoredDocs[i].score);
				// hit.setSemanticScore(scoredDocs[i].score);
			}
			hit.setSemanticScore(semScore);
			// hit.setCompareType(4);

			LOGGER.debug("doSeparateDisambiguation() - next hit: " + hit.toString());
			LOGGER.debug("doSeparateDisambiguation() - semantic score is: " + semScore);
			LOGGER.debug("doSeaprateDisambiguation() - semantic score is: " + semScore);

		}
		for (SynHit hit : disambigList) {
			// double score = scorer.getScore(normalizedSearchTerm,
			// hit.getSynonym());
			// hit.setScore(score);
			hit.setCompareType(SynHit.CompareType.SEMSCORE);
		}

		// sort hits by new score
		Collections.sort(disambigList);

		// TODO: maybe other scoring is also needed (by Cosinus-Measure...)

		// return best one
		SynHit bestHit = disambigList.get(0);
		if (bestHit.getSemanticScore() >= minContextScore)
			return bestHit;
		else
			return null;
	}

	/**
	 * generates a boolean query from the context and a single ids: the id has
	 * to match and the context is combined by OR.
	 * 
	 * 
	 * TODO requires testing!
	 */
	public BooleanQuery makeSeparateQuery(String id, String context) throws IOException {
		SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English",
				ContextUtils.STOPWORDS);

		// TODO: context of types?
		// context = ContextUtils.makeContextTypes(context);

		BooleanQuery contextQuery = new BooleanQuery();

		// id
		
		Query idQuery = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_ID_FIELD, id));
			// TODO: check whether id is the right field to serch in
		contextQuery.add(idQuery, BooleanClause.Occur.MUST);

		// tokens of context
		TokenStream stream = sbAnalyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD,
				new StringReader(context));

		for (Token t = stream.next(); t != null; t = stream.next()) {
			Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, t.term()));
			contextQuery.add(q, BooleanClause.Occur.SHOULD);
		}
		// LOGGER.debug("makeQuery() - query for disambiguation: " +
		// contextQuery);

		return contextQuery;
		
		// lucene 5.6
//		EnglishAnalyzer analyzer = new EnglishAnalyzer(
//				CharArraySet.copy(new HashSet<String>(Arrays.asList(ContextUtils.STOPWORDS))));
//
//		// TODO: context of types?
//		// context = ContextUtils.makeContextTypes(context);
//
//		BooleanQuery contextQuery = new BooleanQuery();
//
//		// id
//
//		Query idQuery = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_ID_FIELD, id));
//		// TODO: check whether id is the right field to serch in
//		contextQuery.add(idQuery, BooleanClause.Occur.MUST);
//
//		// tokens of context
//		TokenStream ts = analyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, new StringReader(context));
//		CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
//		ts.reset();
//		while (ts.incrementToken()) {
//			Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, cattr.toString()));
//			contextQuery.add(q, BooleanClause.Occur.SHOULD);
//		}
//		// LOGGER.debug("makeQuery() - query for disambiguation: " +
//		// contextQuery);
//
//		return contextQuery;
	}

	public IndexSearcher getSemanticIndexSearcher() {
		return searcher;
	}

}
