/** 
 * ContextUtils.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.3.1
 * Since version:   1.0
 *
 * Creation date: ?
 * 
 **/

package de.julielab.jules.ae.genemapper.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jules.ae.genemapper.index.ContextIndexFieldNames;

/**
 * @author jwermter
 * 
 */
public class ContextUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextUtils.class);

	public static final String[] STOPWORDS = { "a", "about", "above", "across", "after", "afterwards", "again",
			"against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among",
			"amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anyway",
			"anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes",
			"becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between",
			"beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "computer", "con",
			"could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg",
			"eight", "either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every",
			"everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire",
			"first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further",
			"get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby",
			"herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if",
			"in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter",
			"latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine",
			"more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither",
			"never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now",
			"nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise",
			"our", "ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather",
			"re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show",
			"side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime",
			"sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them",
			"themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon",
			"these", "they", "thick", "thin", "third", "this", "those", "though", "three", "through", "throughout",
			"thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un",
			"under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever",
			"when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon",
			"wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why",
			"will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves",
	/*
	 * "protein", "precursor", "gene", "proteins", "genes", "proteins", "precusors", "precursors",
	 */
	};

	public static String makeContextTypes(String context) throws IOException {

		SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English", STOPWORDS);
		TokenStream ts = sbAnalyzer.tokenStream("context", new StringReader(context));
		String token = "";
		HashSet<String> contextTypes = new HashSet<String>();

		for (Token t = ts.next(); t != null; t = ts.next()) {
			token = t.termText();
			contextTypes.add(token);
		}

		// TODO use StringBuilder!!
		Iterator<String> it = contextTypes.iterator();
		context = "";
		while (it.hasNext()) {
			token = it.next();
			context += " " + token;
		}

		return context.trim();
	}

	public static String makeContextTokens(String context) throws IOException {

		SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English", STOPWORDS);
		TokenStream ts = sbAnalyzer.tokenStream("context", new StringReader(context));
		String token = "";
		HashSet<String> contextTypes = new HashSet<String>();
		String result = "";

		// TODO use StringBuilder!!
		for (Token t = ts.next(); t != null; t = ts.next()) {
			token = t.termText();
			result += " " + token;
		}

		return result.trim();
	}

	/**
	 * makes the context query. Returns null if no context was found.
	 * 
	 * @param aJCas
	 * @param abstractContextScope
	 * @return a boolean query to be processed by Lucene
	 * @throws IOException
	 */
	public static BooleanQuery makeContextQuery(JCas aJCas, String abstractContextScope) throws IOException {

		String contextString = "";

		if (abstractContextScope == null) {
			// if no special context scope is specified, just take the whole document text
			contextString = aJCas.getDocumentText();
		} else {
			// check for specified abstract type
			Type abstractType = aJCas.getTypeSystem().getType(abstractContextScope);
			if (abstractType != null) {
				FSIterator iter = aJCas.getAnnotationIndex(abstractType).iterator();
				if (iter.isValid()) {
					AnnotationFS fs = (AnnotationFS) iter.get();
					try {
						contextString = fs.getCoveredText();
					} catch (RuntimeException e) {
						LOGGER.error("makeContextQuery() -  "
								+ "could not access the text covered by the abstract type scope!", e);
					}
				} else {
					// if the abstractContextScope cannot be found -- we through an error for information
					LOGGER.error("makeContextQuery() - no annotation of type " + abstractContextScope
							+ " found in document. or respective type system "
							+ "not specified in GeneMapper's descriptor. Will result in empty context "
							+ "query (thus no mapping performed)!");
				}
			}
		}

		if (contextString == null || contextString.length() == 0) {

			LOGGER.error("ContextString for the Query is empty");
			return null;
		}

		LOGGER.debug("semantic index search context: " + contextString);
		return makeContextQuery(contextString.trim());
	}

	/**
	 * makes the context query. Returns null if no context was found.
	 * 
	 * @param aJCas
	 * @param abstractContextScope
	 * @return a boolean query to be processed by Lucene
	 * @throws IOException
	 */
	public static BooleanQuery makeContextQuery(JCas aJCas, String abstractContextScope, int windowSize,
			EntityMention entity) throws IOException {
		LOGGER.debug("Making context query");
		String contextString = makeContext(aJCas, abstractContextScope, windowSize, entity);

		return makeContextQuery(contextString.toString().trim());

	}
	
	public static String makeContext(JCas aJCas, String abstractContextScope, int windowSize,
			EntityMention entity) {
		LOGGER.debug("Making context");
		StringBuilder contextString = new StringBuilder();

		try {
			/**
			 * get FS iterator
			 */
			FSIterator<Annotation> cursor = aJCas.getAnnotationIndex(de.julielab.jcore.types.Token.type).iterator();
			if (!cursor.hasNext()) {
				FSIterator<Annotation> headerIt = aJCas.getAnnotationIndex(Header.type).iterator();
				Header header = null;
				if (headerIt.hasNext())
					header = (Header) headerIt.next();
				String docId = header != null ? header.getDocId() : "<unknown>";
				LOGGER.warn("The document with ID {} does not have any tokens. Cannot create context on token basis, resorting to character-based context.", docId);
				int characterWindowSize = 5 * windowSize;
				LOGGER.warn("Converting token window size of {} to character window size of {} (larger by factor 5)", windowSize, characterWindowSize);
				// now we just get the size that we want before the entity begin and after the entity end
				int contextAffixSize = (characterWindowSize - (entity.getEnd() - entity.getBegin())) / 2;
				int contextStart = Math.max(0, entity.getBegin() - contextAffixSize);
				int contextEnd = Math.min(aJCas.getDocumentText().length(), entity.getEnd() + contextAffixSize + 1);
				return aJCas.getDocumentText().substring(contextStart, contextEnd).trim();
			}

			/**
			 * set cursor on token of a gene to be disambiguated
			 */
			de.julielab.jcore.types.Token actual_token = null;
			ArrayList<de.julielab.jcore.types.Token> entityTokens = (ArrayList<de.julielab.jcore.types.Token>) UIMAUtils
					.getAnnotations(aJCas, entity, de.julielab.jcore.types.Token.class);

			if (entityTokens != null && entityTokens.size() > 0) {
				actual_token = entityTokens.get(0);
			} else {
				actual_token = UIMAUtils.getContainingAnnotation(aJCas, entity, de.julielab.jcore.types.Token.class);
			}
			if (actual_token == null)
				actual_token = (de.julielab.jcore.types.Token) JCoReAnnotationTools.getOverlappingAnnotation(aJCas,
						de.julielab.jcore.types.Token.class.getCanonicalName(), entity.getBegin(), entity.getEnd());
			cursor.moveTo(actual_token);

			/**
			 * scope size for a context before
			 */
			int stopNumber = 0;
			int tokensBefore = 0;
			int maxClauseCount = BooleanQuery.getMaxClauseCount();
			if (windowSize > 0) {
				tokensBefore = windowSize / 2;
				stopNumber = windowSize;
			} else {
				tokensBefore = maxClauseCount / 2;
				stopNumber = maxClauseCount;
			}

			/**
			 * move cursor to scope before the token to disambiguate
			 */
			while (cursor.isValid() && tokensBefore > 0) {
				tokensBefore--;
				cursor.moveToPrevious();
			}

			if (!cursor.isValid()) {
				cursor.moveToFirst();
			}

			int stop = 0;
			while (cursor.hasNext() && stop < stopNumber) {
				de.julielab.jcore.types.Token token = (de.julielab.jcore.types.Token) cursor.next();
				contextString.append(token.getCoveredText());
				contextString.append(" ");
				stop++;

			}

		} catch (Exception e) {
			String docId = null;
			FSIterator<Annotation> it = aJCas.getAnnotationIndex(Header.type).iterator();
			if (it.hasNext()) {
				Header h = (Header) it.next();
				docId = h.getDocId();
			}

			LOGGER.error("Error while running MakeContextQuery on document with ID {} on entity {}: {}", new Object[] {
					docId, entity, e });
			e.printStackTrace();
		}

		if (contextString == null || contextString.length() == 0) {

			LOGGER.error("ContextString for the Query is empty");
			return null;
		}

		LOGGER.debug("semantic index search context: " + contextString);

		LOGGER.debug("----" + contextString + "-----");
		
		return contextString.toString();
	}

	private static void between(int begin, int end) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * builds the Lucene context query a given text
	 * 
	 * @param context
	 *            the document text
	 * @return
	 * @throws IOException
	 */
	public static BooleanQuery makeContextQuery(String context) throws IOException {

		BooleanQuery contextQuery = new BooleanQuery();
		SnowballAnalyzer sbAnalyzer = new SnowballAnalyzer("English", ContextUtils.STOPWORDS);
		TokenStream stream = sbAnalyzer.tokenStream(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, new StringReader(
				context));

		int maxClauseCount = contextQuery.getMaxClauseCount();

		for (Token t = stream.next(); t != null; t = stream.next()) {
			Query q = new TermQuery(new Term(ContextIndexFieldNames.LOOKUP_CONTEXT_FIELD, t.termText()));
			if (contextQuery.getClauses().length < maxClauseCount - 1) {
				contextQuery.add(q, BooleanClause.Occur.SHOULD);
			} else {
				LOGGER.warn("makeContextQuery() - context too long, cut after " + maxClauseCount + " tokens");
				break;
			}
		}
		LOGGER.debug("makeContextQuery() - query for disambiguation: " + contextQuery);

		return contextQuery;
	}

}
