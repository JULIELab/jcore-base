/** 
 * QueryGenerator.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.0 	
 * Since version:   1.5
 *
 * Creation date: Oct 30, 2007 
 * 
 * Builds a boolean query where each token of the search string is added SHOULD (OR).
 **/

package de.julielab.jules.ae.genemapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval.CandidateCacheKey;
import de.julielab.jules.ae.genemapper.index.SynonymIndexFieldNames;

public class QueryGenerator {

	public static BooleanQuery makeDisjunctiveQuery(String searchString) {
		return makeDisjunctiveQuery(searchString, SynonymIndexFieldNames.LOOKUP_SYN_FIELD);
	}

	public static BooleanQuery makeDisjunctiveQuery(String searchString, String field) throws BooleanQuery.TooManyClauses {
		String[] tokens = searchString.split(" ");

		BooleanQuery disjunctiveQuery = new BooleanQuery();
		for (int i = 0; i < tokens.length; i++) {
			Query q = new TermQuery(new Term(field, tokens[i]));
			disjunctiveQuery.add(q, BooleanClause.Occur.SHOULD);
		}
		return disjunctiveQuery;
	}

	public static BooleanQuery makeConjunctiveQuery(String searchString, String field) {
		String[] tokens = searchString.split(" ");

		BooleanQuery disjunctiveQuery = new BooleanQuery();
		for (int i = 0; i < tokens.length; i++) {
			Query q = new TermQuery(new Term(field, tokens[i]));
			disjunctiveQuery.add(q, BooleanClause.Occur.MUST);
		}
		return disjunctiveQuery;
	}

	public static Query makeDisjunctionMaxQuery(CandidateCacheKey key) throws BooleanQuery.TooManyClauses {
		BooleanQuery originalNameQueryDisjunctive = makeDisjunctiveQuery(key.geneName.getText().toLowerCase(),
				SynonymIndexFieldNames.ORIGINAL_NAME);
		BooleanQuery normalizedNameQueryDisjunctive = makeDisjunctiveQuery(key.geneName.getNormalizedText(),
				SynonymIndexFieldNames.LOOKUP_SYN_FIELD);
		BooleanQuery normalizedNameVariantQueryDisjunctive = makeDisjunctiveQuery(
				key.geneName.getNormalizedTextVariant(), SynonymIndexFieldNames.VARIANT_NAME);

		DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery(0f);
		disjunctionMaxQuery.add(originalNameQueryDisjunctive);
		disjunctionMaxQuery.add(normalizedNameQueryDisjunctive);
		disjunctionMaxQuery.add(normalizedNameVariantQueryDisjunctive);

		if (!StringUtils.isBlank(key.taxId)) {
			CachingWrapperFilter taxIdFilter = new CachingWrapperFilter(
					new QueryWrapperFilter(new TermQuery(new Term(SynonymIndexFieldNames.TAX_ID_FIELD, key.taxId))));
			FilteredQuery filteredQuery = new FilteredQuery(disjunctionMaxQuery, taxIdFilter);
			return filteredQuery;
		}
		return disjunctionMaxQuery;
	}

}
