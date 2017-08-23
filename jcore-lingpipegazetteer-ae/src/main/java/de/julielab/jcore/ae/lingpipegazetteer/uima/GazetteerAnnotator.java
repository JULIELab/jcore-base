/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.lingpipegazetteer.uima;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.ibm.icu.text.Transliterator;

import de.julielab.jcore.ae.lingpipegazetteer.chunking.ChunkerProvider;
import de.julielab.jcore.ae.lingpipegazetteer.chunking.OverlappingChunk;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking.NormalizedString;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.mantra.Entity;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.index.IndexTermGenerator;
import de.julielab.jcore.utility.index.JCoReHashMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;
import de.julielab.jcore.utility.index.TermGenerators.LongOffsetIndexTermGenerator;

public class GazetteerAnnotator extends JCasAnnotator_ImplBase {

	private static final String COMPONENT_ID = GazetteerAnnotator.class.getCanonicalName();
	private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerAnnotator.class);
	public static final String CHUNKER_RESOURCE_NAME = "DictionaryChunkerProvider";
	// public final static String PARAM_USE_APPROXIMATE_MATCHING =
	// "UseApproximateMatching";
	public final static String PARAM_CHECK_ACRONYMS = "CheckAcronyms";
	public final static String PARAM_OUTPUT_TYPE = "OutputType";
	/**
	 * Only required to set to false as an annotator parameter when using
	 * approximate matching and the ChunkerProvider is set to CaseSensitive
	 * false. That is because the approximate chunker is always case sensitive.
	 */
	// public final static String PARAM_CASE_SENSITIVE = "CaseSensitive";
	private static final String PARAM_USE_MANTRA_MODE = "MantraMode";
	/**
	 * Parameter to indicate whether text - CAS document text for this class -
	 * should be normalized by completely removing dashes, parenthesis, genitive
	 * 's and perhaps more. This is meant to replace the generation of term
	 * variants and cannot be used together with variation generation. If this
	 * is switched on here, it must also be switched on in the external resource
	 * configuration for the ChunkerProvider! Can only be used with alternative
	 * ChunkerProviderImplAlt implementation.
	 */
	// public final static String PARAM_NORMALIZE_TEXT = "NormalizeText";
	/**
	 * Parameter to indicate whether text - CAS document text for this class -
	 * should be transliterated, i.e. whether accents and other character
	 * variations should be stripped. If this is switched on here, it must also
	 * be switched on in the external resource configuration for the
	 * ChunkerProvider! Can only be used with alternative ChunkerProviderImplAlt
	 * implementation.
	 */
	// public final static String PARAM_TRANSLITERATE_TEXT =
	// "TransliterateText";

	// @ConfigurationParameter(name = PARAM_USE_APPROXIMATE_MATCHING,
	// defaultValue = "false")
	private boolean useApproximateMatching = false;
	@ConfigurationParameter(name = PARAM_USE_MANTRA_MODE, defaultValue = "false")
	private boolean mantraMode = false;

	// needs to be true because of chunker injection:
	@ConfigurationParameter(name = PARAM_CHECK_ACRONYMS, defaultValue = "true")
	private boolean checkAcronyms = true;
	@ConfigurationParameter(name = PARAM_OUTPUT_TYPE)
	private String outputType = null;
	// @ConfigurationParameter(name = PARAM_TRANSLITERATE_TEXT, defaultValue =
	// "false")
	private boolean transliterate;
	// @ConfigurationParameter(name = PARAM_NORMALIZE_TEXT, defaultValue =
	// "false")
	private boolean normalize;

	@ExternalResource(key = CHUNKER_RESOURCE_NAME, mandatory = true)
	private ChunkerProvider provider;
	/**
	 * Removes diacritics and does lower casing
	 */
	private Transliterator transliterator;
	private Chunker gazetteer = null;
	private boolean caseSensitive;
	private TokenizerFactory normalizationTokenFactory;
	private Set<String> stopWords;

	// TODO for debug only
	private static int initializeCount = 0;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("calls to initialize: " + initializeCount);

		super.initialize(aContext);
		LOGGER.info("initialize() - initializing GazetteerAnnotator...");

		try {
			provider = (ChunkerProvider) getContext().getResourceObject(CHUNKER_RESOURCE_NAME);
			gazetteer = provider.getChunker();
			// stopWords = provider.getStopWords();
			String[] stopwordArray = { "a", "about", "above", "across", "after", "afterwards", "again", "against",
					"all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among",
					"amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything",
					"anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become",
					"becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside",
					"besides", "between", "beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot",
					"cant", "co", "computer", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do",
					"done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere",
					"empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except",
					"few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly",
					"forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has",
					"hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers",
					"herself", "high", "him", "himself", "his", "how", "however", "hundred", "i", "ie", "if", "in",
					"inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter",
					"latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill",
					"mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name",
					"namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone",
					"nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only",
					"onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own",
					"part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed",
					"seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere",
					"six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes",
					"somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them",
					"themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein",
					"thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though", "three",
					"through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards",
					"twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we",
					"well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas",
					"whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who",
					"whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet",
					"you", "your", "yours", "yourself", "yourselves", };
			stopWords = new HashSet<>();
			for (String sw : stopwordArray)
				stopWords.add(sw);
		} catch (ResourceAccessException e) {
			LOGGER.error("Exception while initializing", e);
		}

		// gazetteer type
		useApproximateMatching = provider.getUseApproximateMatching();// (Boolean)
																		// aContext.getConfigParameterValue(PARAM_USE_APPROXIMATE_MATCHING);
		LOGGER.info(
				"Use approximate matching (the actual used edit distance is defined by the ChunkerProvider implementation): {}",
				useApproximateMatching);

		// check acronyms
		checkAcronyms = (Boolean) aContext.getConfigParameterValue(PARAM_CHECK_ACRONYMS);
		LOGGER.info(
				"Check for acronyms (found dictionary entries that are abbreviations are only accepted if their long form is an abbreviation of the same type, too): {}",
				checkAcronyms);
		// filter stop words

		Boolean normalizeBoolean = provider.getNormalize();// (Boolean)
															// aContext.getConfigParameterValue(PARAM_NORMALIZE_TEXT);
		normalize = false;
		if (normalizeBoolean != null) {
			normalize = normalizeBoolean;
			normalizationTokenFactory = new IndoEuropeanTokenizerFactory();
		}
		LOGGER.info("Normalize CAS document text (i.e. do stemming and remove possessive 's): {}", normalize);

		Boolean transliterateBoolean = provider.getTransliterate();// (Boolean)
																	// aContext.getConfigParameterValue(PARAM_TRANSLITERATE_TEXT);
		transliterate = false;
		if (transliterateBoolean != null) {
			transliterate = transliterateBoolean;
			transliterator = Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC; Lower");
		}
		LOGGER.info("Transliterate CAS document text (i.e. transform accented characters to their base forms): {}",
				transliterate);

		if (useApproximateMatching) {
			// match case-(in)sensitive
			Boolean caseSensitiveBoolean = provider.getCaseSensitive();// (Boolean)
																		// aContext.getConfigParameterValue(PARAM_CASE_SENSITIVE);
			caseSensitive = false;
			if (caseSensitiveBoolean != null) {
				caseSensitive = caseSensitiveBoolean;
			}
			LOGGER.info("Dictionary matching is case sensitive (text is not lower cased): {}", caseSensitive);
		}

		// define output level
		outputType = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_TYPE);
		if (outputType == null) {
			LOGGER.error("initialize() - output type not specified.");
			throw new ResourceInitializationException();
		}

		mantraMode = aContext.getConfigParameterValue(PARAM_USE_MANTRA_MODE) != null
				? (Boolean) aContext.getConfigParameterValue(PARAM_USE_MANTRA_MODE) : false;
	}

	/**
	 * process the CAS, there are two subroutines: one for exact and one for
	 * approximate matching.
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String docText = aJCas.getDocumentText();
		if (docText == null || docText.length() == 0)
			return;
		if (useApproximateMatching && !transliterate && !caseSensitive)
			docText = docText.toLowerCase();
		NormalizedString normalizedDocText = null;
		if (normalize) {
			normalizedDocText = StringNormalizerForChunking.normalizeString(docText, normalizationTokenFactory, transliterator);
		}
		
		IndexTermGenerator<Long> longOffsetTermGenerator = TermGenerators.longOffsetTermGenerator();
		JCoReHashMapAnnotationIndex<Long, ConceptMention> conceptMentionIndex = new JCoReHashMapAnnotationIndex<>(longOffsetTermGenerator, longOffsetTermGenerator, aJCas, ConceptMention.type);
		JCoReHashMapAnnotationIndex<Long, Abbreviation> abbreviationIndex = new JCoReHashMapAnnotationIndex<>(longOffsetTermGenerator, longOffsetTermGenerator, aJCas, Abbreviation.type);
		
		LOGGER.debug("Performing actual Gazetteer annotation...");
		Chunking chunking;
		if (normalize)
			chunking = gazetteer.chunk(normalizedDocText.string);
		else
			chunking = gazetteer.chunk(docText);
		LOGGER.debug("Gazetteer annotation done.");
		if (useApproximateMatching) {
			/*
			 * handle matches found by approx matching: this means especially
			 * overlapping matches with different scores (doesn't happen with
			 * exact matches)
			 */
			List<Chunk> chunkList = filterChunking(chunking);
			List<OverlappingChunk> overlappingChunks = groupOverlappingChunks(chunkList,
					chunking.charSequence().toString());
			// now add the best chunk of all overlappingChunks to the CAS
			LOGGER.debug("all overlapping chunks:\n");
//			Set<Chunk> bestChunksSet = new HashSet<>();
			for (OverlappingChunk overlappingChunk : overlappingChunks) {
				// show chunks
				LOGGER.debug(overlappingChunk.toStringAll());
				List<Chunk> bestChunks = overlappingChunk.getBestChunks();
				LOGGER.debug("Found {} best chunks.", bestChunks.size());
				for (int i = 0; i < bestChunks.size(); i++) {
					Chunk bestChunk = bestChunks.get(i);
					LOGGER.debug("Nr. " + i + " best chunk: " + bestChunk.start() + " - " + bestChunk.end() + ": "
							+ bestChunk.score() + " ; type: " + bestChunk.type());
					// TODO this check and the corresponding set may be removed
					// when this exception hasn't been thrown
					// in a
					// while. Its currently just to be sure, this should not
					// happen any more since the chunks are sorted
					// by
					// offset in the grouping method.
//					if (bestChunksSet.contains(bestChunk)) {
//						throw new IllegalStateException("Duplicate best chunk: " + bestChunk);
//					}
//					bestChunksSet.add(bestChunk);
					// add 2 cas
					add2Cas(aJCas, bestChunk, normalizedDocText, conceptMentionIndex, abbreviationIndex);
				}
			}
			// for (Chunk chunk : chunking.chunkSet()) {
			// add2Cas(aJCas, chunk, normalizedDocText);
			// }
		} else {
			for (Chunk chunk : chunking.chunkSet()) {
				add2Cas(aJCas, chunk, normalizedDocText, conceptMentionIndex, abbreviationIndex);
			}
		}
		if (checkAcronyms && !mantraMode) {
			LOGGER.debug("process() - checking acronyms");
			annotateAcronymsWithFullFormEntity(aJCas, conceptMentionIndex);
		}
	}

	private List<Chunk> filterChunking(Chunking chunking) {
		// ChunkingImpl newChunking = new ChunkingImpl(chunking.charSequence());
		List<Chunk> newChunking = new ArrayList<>(chunking.chunkSet().size());
		for (Chunk chunk : chunking.chunkSet()) {
			String chunkText = chunking.charSequence().subSequence(chunk.start(), chunk.end()).toString();
			if (filterParenthesis(chunkText))
				continue;
			if (filterPunctuationArtifacts(chunkText))
				continue;
			if (filterStopwords(chunkText))
				continue;
			newChunking.add(chunk);
		}
		return newChunking;
	}

	private boolean filterPunctuationArtifacts(String chunkText) {
		if (chunkText.startsWith("-"))
			return true;
		if (chunkText.endsWith("-"))
			return true;
		return false;
	}

	private boolean filterStopwords(String chunkText) {
		if (stopWords.contains(chunkText.toLowerCase()))
			return true;
		if (chunkText.contains(" ")) {
			String[] words = chunkText.split(" ");
			int stopWordCounter = 0;
			for (String word : words) {
				if (stopWords.contains(word.toLowerCase()))
					stopWordCounter++;
			}
			if (Math.ceil(words.length / 2.0) <= stopWordCounter) {
				LOGGER.debug("Filtering due to high stop word occurrences: {}", chunkText);
				return true;
			}
		}
		return false;
	}

	static boolean filterParenthesis(String chunkText) {
		Stack<Character> parenthesisStack = new Stack<>();
		// Map<ParenthesisType, Integer> pMap = new HashMap<>();
		for (int i = 0; i < chunkText.length(); i++) {
			char current = chunkText.charAt(i);
			if (isParentheses(current)) {
				if (isOpenedParentheses(current)) {
					parenthesisStack.add(current);
				} else {
					if (parenthesisStack.isEmpty())
						return true;
					if (!isParenthesisCounterpart(parenthesisStack.pop(), current))
						return true;
				}
			}
		}
		if (!parenthesisStack.isEmpty())
			return true;
		return false;
	}

	private static boolean isParenthesisCounterpart(Character char1, Character char2) {
		ParenthesisType char1ParenthesisType = getParenthesisType(char2);
		ParenthesisType char2ParenthesisType = getParenthesisType(char1);
		if (char1ParenthesisType == ParenthesisType.NONE || char2ParenthesisType == ParenthesisType.NONE)
			throw new IllegalArgumentException("The two characters '" + char1 + "' and '" + char2
					+ "' were given in order to determine whether they are compatible parenthesis counterparts, but at least one of those characters is no parentheses.");
		return char1ParenthesisType.equals(char2ParenthesisType);
	}

	// enum ParenthesesType {
	// ROUND_CLOSED {
	// @Override
	// boolean isOpen() {
	// return false;
	// }
	//
	// },
	// BRACKET_CLOSED {
	// @Override
	// boolean isOpen() {
	// return false;
	// }
	// },
	// CURLY_CLOSED {
	// @Override
	// boolean isOpen() {
	// return false;
	// }
	//
	// },
	// ROUND_OPENED {
	// @Override
	// boolean isOpen() {
	// return true;
	// }
	// },
	// BRACKET_OPENED {
	// @Override
	// boolean isOpen() {
	// return true;
	// }
	// },
	// CURLY_OPENED {
	// @Override
	// boolean isOpen() {
	// return true;
	// }
	// };
	// abstract boolean isOpen();
	//
	// boolean isClose() {
	// return !isOpen();
	// };
	// }

	enum ParenthesisType {
		ROUND, BRACKET, CURLY, NONE
	}

	static ParenthesisType getParenthesisType(char current) {
		switch (current) {
		case '(':
		case ')':
			return ParenthesisType.ROUND;
		case '[':
		case ']':
			return ParenthesisType.BRACKET;
		case '{':
		case '}':
			return ParenthesisType.CURLY;
		default:
			return ParenthesisType.NONE;
		}
	}

	static boolean isParentheses(char current) {
		return isOpenedParentheses(current) || isClosedParentheses(current);
	}

	static boolean isOpenedParentheses(char current) {
		switch (current) {
		case '(':
		case '[':
		case '{':
			return true;
		default:
			return false;
		}
	}

	static boolean isClosedParentheses(char current) {
		switch (current) {
		case ')':
		case ']':
		case '}':
			return true;
		default:
			return false;
		}
	}

	static List<OverlappingChunk> groupOverlappingChunks(List<Chunk> chunkList, String chunkedText) {
		// sort chunkList so the grouping works as intended
		Collections.sort(chunkList, new Comparator<Chunk>() {

			@Override
			public int compare(Chunk o1, Chunk o2) {
				return o1.start() - o2.start();
			}

		});
		// group overlapping chunks
		List<OverlappingChunk> overlappingChunks = new ArrayList<OverlappingChunk>();
		for (Chunk chunk : chunkList) {
			// for debugging
			// System.out.println("chunking.add(ChunkFactory.createChunk(" +
			// chunk.start() + ", " + chunk.end() +
			// ", 0d));");
			boolean added = false;
			for (OverlappingChunk over : overlappingChunks) {
				if (over.isOverlappingSpan(chunk.start(), chunk.end())) {
					over.addChunk(chunk.start(), chunk.end(), chunk);
					added = true;
				}
			}
			if (!added) {
				overlappingChunks.add(new OverlappingChunk(chunk.start(), chunk.end(), chunk, chunkedText));
				added = true;
			}
		}
		return overlappingChunks;
	}

	// ------------ INFO ..........
	// String text = aJCas.getDocumentText();
	// int start = chunk.start();
	// int end = chunk.end();
	// String type = chunk.type();
	// double score = chunk.score();
	// String phrase = text.substring(start, end);
	// System.out.println(" found phrase=|" + phrase + "|"
	// + " start=" + start + " end=" + end + " type=" + type
	// + " score=" + score);
	// ------------ INFO ..........
	/**
	 * checks whether a chunk (= dictionary match) is an acronym. If yes, checks
	 * whether respective full form (obtained via abbr textReference) is
	 * ConceptMention and has same specificType as chunk If these conditions are
	 * not fulfilled, no entity annotation will be made.
	 * @param abbreviationIndex 
	 * @param conceptMentionIndex 
	 */
	private boolean isAcronymWithSameFullFormSpecificType(JCas aJCas, Chunk chunk, NormalizedString normalizedDocText, JCoReHashMapAnnotationIndex<Long, ConceptMention> conceptMentionIndex, JCoReHashMapAnnotationIndex<Long, Abbreviation> abbreviationIndex) {
//		Annotation anno;
		int start;
		int end;
		if (normalize) {
			try {
				start =  normalizedDocText.getOriginalOffset(chunk.start());
				end = normalizedDocText.getOriginalOffset(chunk.end());
			} catch (Exception e) {
				System.out.println("Text: " + normalizedDocText);
				System.out.println("Chunk: " + chunk);
				System.out.println("Chunk end: " + chunk.end());
				System.out.println("Normalized Text: " + normalizedDocText.string.substring(chunk.start(), chunk.end()));
				throw e;
			}
//			anno = new Annotation(aJCas, start, end);
		} else {
//			anno = new Annotation(aJCas, chunk.start(), chunk.end());
			start = chunk.start();
			end = chunk.end();
		}
		
		 LongOffsetIndexTermGenerator longOffsetTermGenerator = TermGenerators.longOffsetTermGenerator();
//		anno.addToIndexes(aJCas);
		// Retrieves potential abbr annotation
		Abbreviation abbr = abbreviationIndex.getFirst(longOffsetTermGenerator.forOffsets(start, end));//AnnotationRetrieval.getMatchingAnnotation(aJCas, anno, Abbreviation.class);
//		anno.removeFromIndexes();
		// check whether it's an abbr
		String chunktext = null;
		if (LOGGER.isDebugEnabled())
		chunktext = aJCas.getDocumentText().substring(start, end);
		if (abbr == null) {
			LOGGER.debug(chunk + " chunk \"{}\" is not an abbreviation\n", chunktext);
			return true;
		}
		// checks whether respective full form is ConceptMention
		Annotation textRef = abbr.getTextReference();
		ConceptMention em = conceptMentionIndex.getFirst(textRef);//AnnotationRetrieval.getMatchingAnnotation(aJCas, textRef, ConceptMention.class);
		if (em == null) {
			LOGGER.debug(
					chunk + " chunk \"{}\" is an abbreviation but respective full \"{}\" form is no ConceptMention\n",
					chunktext, textRef.getCoveredText());
			return false;
		}
		// // checks whether specificType of full form and of chunk are the same
		// String specType = em.getSpecificType();
		// if (specType.equals(chunk.type())) {
		// LOGGER.debug(chunk
		// + " chunk is an abbreviation and respective full form is
		// ConceptMention with same specificType\n");
		// return true;
		// }

		// checks whether full form annotation matches the type to be annotated
		// here
		String emType = em.getClass().getCanonicalName();
		if (emType.equals(outputType)) {
			LOGGER.debug(
					chunk + " chunk \"{}\" is an abbreviation and respective full form \"{}\" is ConceptMention with same type as OutputType\n",
					chunktext, em.getCoveredText());
			return true;
		}

		LOGGER.debug(
				chunk + " chunk \"{}\" is an abbreviation but respective full form \"{}\" is ConceptMention without the correct OutputType (is: {}; OutputType: {})\n",
				new Object[] { chunktext, em.getCoveredText(), emType, outputType });
		return false;
	}

	/**
	 * adds a chunk as an annotation to the CAS
	 * 
	 * @param normalizedDocText
	 * @param abbreviationIndex 
	 * @param conceptMentionIndex 
	 */
	private void add2Cas(JCas aJCas, Chunk chunk, NormalizedString normalizedDocText, JCoReHashMapAnnotationIndex<Long, ConceptMention> conceptMentionIndex, JCoReHashMapAnnotationIndex<Long, Abbreviation> abbreviationIndex)
			throws AnalysisEngineProcessException {
		// System.out.println("CHUNK: start=" + chunk.start() + " end=" +
		// chunk.end());
		// if checkAcronyms, then check acronyms for compliant full forms (=
		// with same specificType)
		if (checkAcronyms && !isAcronymWithSameFullFormSpecificType(aJCas, chunk, normalizedDocText, conceptMentionIndex, abbreviationIndex)) {
			return;
		}

		int start = normalize ? normalizedDocText.getOriginalOffset(chunk.start()) : chunk.start();
		int end = normalize ? normalizedDocText.getOriginalOffset(chunk.end()) : chunk.end();

		try {
			if (mantraMode) {
				// the "type" string is used to transport all data needed for
				// the MAN-XML format
				for (String term : chunk.type().split("@@TERM@@")) {
					// @@ is used to separate source, cui, type(s) and group (in
					// this order!)
					String[] info = term.split("@@");
					Entity newEntity = (Entity) JCoReAnnotationTools.getAnnotationByClassName(aJCas,
							"de.julielab.jcore.types.mantra.Entity");
					newEntity.setBegin(start);
					newEntity.setEnd(end);
					newEntity.setComponentId(COMPONENT_ID);
					newEntity.setConfidence(chunk.score() + "");

					// mantra specific
					newEntity.setSource(info[0]);
					newEntity.setCui(info[1]);
					newEntity.setSemanticType(info[2]);
					newEntity.setSemanticGroup(info[3]);

					newEntity.addToIndexes();
				}
			} else {
				ConceptMention newEntity = (ConceptMention) JCoReAnnotationTools.getAnnotationByClassName(aJCas,
						outputType);
				newEntity.setBegin(start);
				newEntity.setEnd(end);

				// String entityText = newEntity.getCoveredText();
				// if (stopWords.contains(entityText.toLowerCase()))
				// return;
				// if (entityText.contains(" ")) {
				// String[] words = entityText.split(" ");
				// int stopWordCounter = 0;
				// for (String word : words) {
				// if (stopWords.contains(word.toLowerCase()))
				// stopWordCounter++;
				// }
				// if (words.length == stopWordCounter)
				// return;
				// }

				newEntity.setSpecificType(chunk.type());
				newEntity.setComponentId(COMPONENT_ID);
				newEntity.setConfidence(chunk.score() + "");
				newEntity.addToIndexes();
				
				conceptMentionIndex.index(newEntity);
			}
		} catch (Exception e) {
			LOGGER.error("process() - could not generate output type: " + e.getMessage());
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void annotateAcronymsWithFullFormEntity(JCas aJCas, JCoReHashMapAnnotationIndex<Long, ConceptMention> conceptMentionIndex) throws AnalysisEngineProcessException {

		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		FSIterator<Annotation> abbrevIter = indexes.getAnnotationIndex(Abbreviation.type).iterator();
		IndexTermGenerator<Long> longOffsetTermGenerator = TermGenerators.longOffsetTermGenerator();

		// loop over all abbreviations
		while (abbrevIter.hasNext()) {
			Abbreviation abbrev = (Abbreviation) abbrevIter.next();
			Annotation fullFormAnnotation = abbrev.getTextReference();
			LOGGER.debug("annotateAcronymsWithFullFormEntity() - checking abbreviation: " + abbrev.getCoveredText());
			ConceptMention emFullform =null;// AnnotationRetrieval.getMatchingAnnotation(aJCas, fullFormAnnotation,
//					ConceptMention.class);
			emFullform = conceptMentionIndex.getFirst(fullFormAnnotation);

			// The following code was once introduced for gene tagging. There,
			// the acronym fullforms sometimes miss minor parts of an annotated
			// gene, leading to non-annotated acronyms that would have been
			// correct.
			// However, for general-purpose concept recognition this approach
			// can be quite harmful. Example: "Anaphase-promoting complex (APC)"
			// where only "anaphase" is recognized as concept. Now, "APC" would
			// be annotated as an acronym for "anaphase". Here, a better
			// recognition of the abbreviation span is required.
			// ConceptMention emFullform = null;
			// List<ConceptMention> conceptsInFullform =
			// JCoReAnnotationTools.getIncludedAnnotations(aJCas,
			// fullFormAnnotation,
			// ConceptMention.class);
			// if (conceptsInFullform.size() == 1) {
			// emFullform = conceptsInFullform.get(0);
			// LOGGER.debug("Found a single ConceptMention included in the full
			// form: {}", emFullform.getCoveredText());
			// } else if (conceptsInFullform.size() > 1) {
			// // If there are multiple ConceptMentions found in the full form,
			// take that largest right-most candidate.
			// int maxSize = -1;
			// for (ConceptMention em : conceptsInFullform) {
			// int emSize = em.getEnd() - em.getBegin();
			// if (emSize > maxSize) {
			// emFullform = em;
			// maxSize = emSize;
			// }
			// }
			// LOGGER.debug("Found multiple ConceptMentions included in the full
			// form \"{}\", returning the longest.",
			// fullFormAnnotation.getCoveredText());
			// if (LOGGER.isTraceEnabled()) {
			// LOGGER.trace("All found ConceptMentions:");
			// for (ConceptMention cm : conceptsInFullform) {
			// LOGGER.trace("Text: {}; offsets: {}-{}",
			// new Object[] { cm.getCoveredText(), cm.getBegin(), cm.getEnd()
			// });
			// }
			// }
			// } else {
			// LOGGER.debug("No ConceptMention in the span of acronym fullform
			// \"{}\" found.",
			// fullFormAnnotation.getCoveredText());
			// }

			String type = null;
			if (emFullform != null)
				type = emFullform.getClass().getCanonicalName();

			ConceptMention emAcronym = null;//AnnotationRetrieval.getMatchingAnnotation(aJCas, abbrev, ConceptMention.class);
			emAcronym = conceptMentionIndex.getFirst(abbrev);
			// This is really slow, really a pain with full texts.
			// It was originally introduced to push recall for gene recognition.
			// So now we will lose (a bit) of recognition performance there.
			// ConceptMention emAcronym =
			// JCoReAnnotationTools.getPartiallyOverlappingAnnotation(aJCas,
			// abbrev,
			// ConceptMention.class);

			// if type of the entity is equal to the output type for this
			// annotator
			if (type != null && type.equals(outputType)) {
				if (emFullform == null) {
					LOGGER.debug(
							"annotateAcronymsWithFullFormEntity() - fullform of abbreviation has no ConceptMention\n");
					continue;
				}
				if (emFullform.getComponentId() != null && emFullform.getComponentId().equals(COMPONENT_ID)
						&& (emAcronym == null
								|| !emAcronym.getClass().getName().equals(emFullform.getClass().getName()))) {

					try {
						LOGGER.debug("annotateAcronymsWithFullFormEntity() - fullform of abbreviation ("
								+ abbrev.getCoveredText() + " [begin=" + abbrev.getBegin() + "; end=" + abbrev.getEnd()
								+ "]) has ConceptMention: " + emFullform.toString());
						ConceptMention newEntityOnAcronym = (ConceptMention) JCoReAnnotationTools
								.getAnnotationByClassName(aJCas, outputType);
						newEntityOnAcronym.setBegin(abbrev.getBegin());
						newEntityOnAcronym.setEnd(abbrev.getEnd());
						newEntityOnAcronym.setTextualRepresentation(newEntityOnAcronym.getCoveredText());
						newEntityOnAcronym.setSpecificType(emFullform.getSpecificType());
						newEntityOnAcronym.setComponentId(COMPONENT_ID + "+acronym");
						newEntityOnAcronym.setConfidence(emFullform.getConfidence() + "");
						newEntityOnAcronym.addToIndexes();

					} catch (Exception e) {
						LOGGER.error("process() - could not generate output type: " + e.getMessage());
						e.printStackTrace();
						throw new AnalysisEngineProcessException(AnalysisEngineProcessException.ANNOTATOR_EXCEPTION,
								null);
					}

				} else {
					if (emAcronym == null)
						LOGGER.debug("annotateAcronymsWithFullFormEntity() - emAcronym != null");
					else if (emAcronym.getClass().getName().equals(emFullform.getClass().getName()))
						LOGGER.debug("annotateAcronymsWithFullFormEntity() - emAcroType="
								+ emAcronym.getClass().getCanonicalName() + " == emFullformType="
								+ emFullform.getClass().getCanonicalName());
				}

			}
		}
	}

}
