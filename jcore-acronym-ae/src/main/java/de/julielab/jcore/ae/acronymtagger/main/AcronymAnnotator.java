/**
 * AcronymAnnotator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tusche
 *
 * Current version: 2.0
 *
 * Creation date: 14.01.2007
 *
 * Finds fullforms for acronyms using the Schwartz/Hearst algorithm
 * if no fullform was found, then the acronym can be looked in an
 * -->AcronymList
 * 
 * An acronym has the form [foo] or (foo), where foo contains at least two
 * letters, including at least two uppercase letter. Minus (-) and digits
 * are allowed, too.
 * 
 * still to do (strange):
 * merge ABBREVIATION_PATTERN1 and ABBREVIATION_PATTERN2
 * 
 * new in 2.0: acronym-list removed!!
 *  
 **/

package de.julielab.jcore.ae.acronymtagger.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.acronymtagger.entries.AcronymEntry;
import de.julielab.jcore.ae.acronymtagger.entries.FullformEntry;
import de.julielab.jules.types.Abbreviation;
import de.julielab.jules.types.Annotation;
import de.julielab.jules.types.Sentence;

/**
 * Finds fullforms for acronyms using the Schwartz/Hearst algorithm if no fullform was found, then the acronym can be
 * looked in an -->AcronymList
 * 
 * @author tusche, wermter, tomanek
 * 
 */
public class AcronymAnnotator extends JCasAnnotator_ImplBase {

	private static final String COMPONENT_ID = "de.julielab.jules.ae.acronymtagger.AcronymAnnotator";

	public static final String PARAM_ACROLIST = "AcroList";
	// path to an additional list of acronyms for lookup

	public static final String PARAM_CONSISTENCY_ANNO = "ConsistencyAnno";
	// extra annotation of every other shortform occurence?

	public static final String PARAM_MAXLENGTH_FACTOR = "MaxLength";
	// how far back shall we loog to find the start of a fullform?

	@ConfigurationParameter(name = PARAM_MAXLENGTH_FACTOR, defaultValue = "5")
	int MAXLENGTHFACTOR;
	// will be set by initialize() according to descriptor entry
	// via PARAM_MAXLENGTH_FACTOR

	private static final String[] STOPWORDS = { "a", "about", "above", "across", "after", "afterwards", "again",
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
			"herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "i", "ie",
			"if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter",
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
			"will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", };

	private static String ABBREVIATION = "[\\(\\[][-\\w]*?([A-Z]-?\\w|\\w-?[A-Z])[-\\w]*?[\\)\\]]";
	private final Pattern ABBR_PATTERN = Pattern.compile(ABBREVIATION);
	private static String EMBEDDED_ABBR = "[\\(\\[][a-z]+?([A-Z]-?\\w|\\w-?[A-Z])[-\\w]*?[\\)\\]]";
	private final Pattern EMBEDDED_ABBR_PATTERN = Pattern.compile(EMBEDDED_ABBR);
	private static String LONG_FORM_IN_PARENTHESIS = "[\\(\\[]\\w+ (\\w+[ \\)])+";
	private final Pattern LONG_FORM_IN_PARENTHESIS_PATTERN = Pattern.compile(LONG_FORM_IN_PARENTHESIS);

	// private final Pattern ABBR_IN_SQUARE_BRACKETS = Pattern.compile(SQUARE_BRACKET_ABBREVIATION);

	@ConfigurationParameter(name = PARAM_CONSISTENCY_ANNO, defaultValue = "true")
	private boolean consistencyAnno = false;

	private HashMap<String, String> acro2fullForm;

	@ConfigurationParameter(name = PARAM_ACROLIST, mandatory = false)
	private String acroList;
	// private HashMap<String, String> acro2pattern;

	private static final Logger LOGGER = LoggerFactory.getLogger(AcronymAnnotator.class);

	// ---------------------------UIMA FUNCTIONALITY--------------------------------------------------

	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("[JACRO] initializing AcronymAnnotator...");

		try {

			setAcroList(aContext);

			consistencyAnno = ((Boolean) aContext.getConfigParameterValue(PARAM_CONSISTENCY_ANNO)).booleanValue();

			MAXLENGTHFACTOR = ((Integer) aContext.getConfigParameterValue(PARAM_MAXLENGTH_FACTOR)).intValue();

			LOGGER.info(" done");

		} catch (AnnotatorContextException e) {
			throw new ResourceInitializationException();
		} catch (AnnotatorConfigurationException e) {
			throw new ResourceInitializationException();
		} catch (ResourceProcessException e) {
			throw new ResourceInitializationException();
		}

	}

	private void setAcroList(UimaContext aContext) throws AnnotatorConfigurationException, AnnotatorContextException,
			ResourceProcessException, ResourceInitializationException {

		File listFile;
		String acro = "";
		String fullForm = "";
		String pattern = "";
		acro2fullForm = new HashMap<String, String>();
		// acro2pattern = new HashMap<String, String>();
		acroList = (String) aContext.getConfigParameterValue(PARAM_ACROLIST);

		if (acroList != null) {
			InputStream acroListInputStream = null;
			// if acroList is set, make the respective object
			listFile = new File(acroList);
			if (listFile.exists()) {
				LOGGER.debug("Acronym file at {} exists and will be used.", acroList);
				try {
					acroListInputStream = new FileInputStream(listFile);
				} catch (FileNotFoundException e) {
					throw new ResourceInitializationException(e);
				}
			} else {
				// perhaps the parameter value actually doesn't point to a file but to a classpath resource
				String cpResource = acroList.startsWith("/") ? acroList : "/" + acroList;
				LOGGER.debug("Acronym file at {} does not exist. Searching in the classpath for resource {}", acroList, cpResource);
				acroListInputStream = getClass().getResourceAsStream(cpResource);
			}

			if (null == acroListInputStream)
				throw new ResourceInitializationException(ResourceInitializationException.COULD_NOT_ACCESS_DATA,
						new Object[] { acroList });
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(acroListInputStream))) {

				String line = "";
				while ((line = br.readLine()) != null) {
					String[] pair = line.split("\t");
					if (pair.length != 2)
						throw new ResourceProcessException(ResourceProcessException.RESOURCE_DATA_NOT_VALID,
								new String[] { "faulty line in acroList: " + line });
					acro = pair[0];
					fullForm = pair[1];
					acro2fullForm.put(acro, fullForm.toLowerCase());
					pattern = fullForm + " (" + acro + ")";
					// acro2pattern.put(acro, pattern);
				}

			} catch (IOException e) {
				LOGGER.error("setAcroList() - specified acroList file cannot be read: " + e.getMessage());
				throw new AnnotatorConfigurationException();
			}
			LOGGER.debug("setAcroList() - using acronym list: " + listFile);
		}
	}

	public void process(JCas aJCas) {
		LOGGER.info("[JACRO] processing document...");
		try {
			JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
			FSIterator sentenceIter = indexes.getAnnotationIndex(Sentence.type).iterator();
			String sentenceText;

			while (sentenceIter.hasNext()) {
				Sentence sentence = (Sentence) sentenceIter.next();
				sentenceText = sentence.getCoveredText();
				annotate(sentenceText, aJCas, sentence.getBegin());
			}

			// if extra annotation is whished, do so :-)
			if (consistencyAnno) {
				ConsistencyAnnotator ca = new ConsistencyAnnotator();
				ca.consistencyAnnotate(aJCas);
			}

		} catch (StringIndexOutOfBoundsException e) {
			LOGGER.error("typical Error in AcronymAnnotator.process() : StringIndexOutOfBounds");
		}
	}

	// -----------------------------------------------------------------------------------------------

	/**
	 * Called by process(..) for every String in the document - finds all fullforms in the sentence and makes
	 * annotations in the accoriding JCas
	 * 
	 * @param sentence
	 *            the sentence (as string) where the acronyms are searched
	 * @param aJCas
	 *            our JCas, annotations are made here
	 * @param beginSent
	 *            position of the sentence in the text
	 */
	private void annotate(String sentence, JCas aJCas, int beginSent) {
		try {

			Matcher abbrMatcher = ABBR_PATTERN.matcher(sentence);
			processAllMatches(abbrMatcher, aJCas, sentence, beginSent, false);
			abbrMatcher = EMBEDDED_ABBR_PATTERN.matcher(sentence);
			processAllMatches(abbrMatcher, aJCas, sentence, beginSent, true);
			processLongFormInParantheses(LONG_FORM_IN_PARENTHESIS_PATTERN.matcher(sentence), sentence, aJCas, beginSent);

		} catch (Exception e) {
			LOGGER.error("annotate(String sentence, JCas aJCas, int offset)", e);
		}
	}

	/**
	 * A very simple algorithm for the other way round: Here we recognize when the acronym stands BEFORE an expression
	 * parentheses which is the long form of the acronym. This algorithm only checks whether the token before an
	 * expression ,where multiple words are within parentheses, matches the first characters of the tokens in the
	 * parentheses.
	 * 
	 * @param matcher
	 * @param sentence
	 * @param aJCas
	 * @param beginSent
	 */
	private void processLongFormInParantheses(Matcher matcher, String sentence, JCas aJCas, int beginSent) {
		int pos = 0;
		while (matcher.find(pos)) {
			// +1 and -1 for removing parentheses
			int fullformBegin = matcher.start() + 1;
			int fullformEnd = matcher.end() - 1;
			pos = fullformEnd;

			int previousTokenEnd = getNextToken(sentence, fullformBegin);
			int previousTokenStart = getNextToken(sentence, previousTokenEnd - 1) + 1;
			String previousToken = sentence.substring(previousTokenStart, previousTokenEnd);
			String fullform = matcher.group();
			fullform = fullform.substring(1, fullform.length() - 1);

			StringBuilder acronymBuilder = new StringBuilder();
			for (int i = 0; i < fullform.length(); i++) {
				char currentChar = fullform.charAt(i);
				if (i == 0 || (' ' == fullform.charAt(i - 1) && ' ' != currentChar)) {
					acronymBuilder.append(currentChar);
				}
			}
			String derivedAcronym = acronymBuilder.toString();

			if (derivedAcronym.equalsIgnoreCase(previousToken)) {
				LOGGER.debug("identified full form: " + fullform + " for abbreviation: " + previousToken);
				Abbreviation a = new Abbreviation(aJCas, previousTokenStart + beginSent, previousTokenEnd + beginSent);
				a.setExpan(fullform);
				a.setDefinedHere(true);
				// Annotation anno = new Annotation(aJCas, ffStart, ffEnd);
				Annotation anno = new Annotation(aJCas, beginSent + fullformBegin, beginSent + fullformEnd);
				anno.setComponentId(COMPONENT_ID);
				anno.addToIndexes();
				a.setTextReference(anno);
				a.setComponentId(COMPONENT_ID);
				a.addToIndexes();
			}
		}

	}

	/**
	 * checks whether an acronym candidate has more upper case than lower case characters
	 * 
	 * @param acronym
	 *            the acronym candidate to be checked
	 * 
	 * @return true if num(upperCase) > num(lowerCase)
	 */
	private boolean hasMoreThanOneUpperCase(String acronym) {

		StringBuffer sb = new StringBuffer(acronym);
		char c;
		int numUpper = 0;
		int numLower = 0;

		for (int i = 0; i < sb.length(); i++) {
			c = sb.charAt(i);
			if ((c > 64 && c < 91) || (c > 191 && c < 215) || (c > 215 && c < 223)) {
				numUpper++;
			} else if ((c > 96 && c < 123) || (c > 212 && c < 247) || (c > 248 && c < 256)) {
				numLower++;
			}
		}
		return numUpper > 1;
	}

	/**
	 * processes all acronyms found by a given matcher, called by annotate(..)
	 * 
	 * @param matcher
	 *            the (acronym) Matcher
	 * @param aJCas
	 *            annotations are made here
	 * @param sentence
	 *            the sentence to be processed
	 * @param beginSent
	 *            of the sentence in the text --> acronym postion in jCas = acronym position in sentence + offset
	 */
	private void processAllMatches(Matcher matcher, JCas aJCas, String sentence, int beginSent, boolean embedded) {

		String searchText = "";
		String fullform = "";
		String acronym = "";
		int pos, acroStart, acroEnd, searchTextBegin;
		int ffStart, ffEnd, searchResult;
		Abbreviation a;

		pos = 0;
		while (matcher.find(pos)) {

			if (embedded)
				acroStart = getEmbeddedAcroStart(sentence, matcher.start() + 2);
			else
				acroStart = matcher.start() + 1;
			acroEnd = matcher.end() - 1;
			acronym = sentence.substring(acroStart, acroEnd);

			if (!hasMoreThanOneUpperCase(acronym)) {
				pos = matcher.end() + 1;
				if (pos >= sentence.length() || pos < 0) {
					break;
				}
				continue;
			}

			searchTextBegin = getPotFullformStart(sentence, acroStart, acronym.length());
			searchText = sentence.substring(searchTextBegin, acroStart);

			if (searchText.length() != 0) {

				// --> call findFullformStart to look for the fullform!
				searchResult = findFullformStart(" " + searchText, acronym);

				if (searchResult == -1) {
					// found nothing - last chance: look it up in the list:
					// removed in version 2.0
				} else {
					// found something - simply make the annotation!

					ffStart = searchTextBegin + searchResult;

					// ffEnd: always start looking at non-embeddded acroStart!
					ffEnd = getFfEnd(sentence, matcher.start() + 1);

					// use the unnormalized full form in when AE is used in a typical pipeline
					fullform = sentence.substring(ffStart, ffEnd);

					// the normalized full form might be used, when AE is used to create a list of all acronyms
					// fullform = dict.normalize(sentence.substring(ffStart, ffEnd), true);
					LOGGER.debug("processAllMatches() - identified full form: " + fullform
							+ " for abbreviation: "
							+ acronym.toString());
					a = new Abbreviation(aJCas, acroStart + beginSent, acroEnd + beginSent);
					a.setExpan(fullform);
					a.setDefinedHere(true);
					// Annotation anno = new Annotation(aJCas, ffStart, ffEnd);
					Annotation anno = new Annotation(aJCas, beginSent + ffStart, beginSent + ffEnd);
					anno.setComponentId(COMPONENT_ID);
					anno.addToIndexes();
					a.setTextReference(anno);
					a.setComponentId(COMPONENT_ID);
					a.addToIndexes();
				}
			}
			pos = matcher.end() + 1;
			// make sure pos is not bigger than sentence.length or smaller than sentence begin
			if (pos >= sentence.length() || pos < 0) {
				break;
			}

		}

	}

	private int getEmbeddedAcroStart(String sentence, int acroStart) {

		int origAcroStart = acroStart;

		while (acroStart < sentence.length()) {
			char c = sentence.charAt(acroStart);
			if (c > 64 && c < 91) {
				return acroStart;
			} else
				acroStart++;
		}

		return origAcroStart;
	}

	/**
	 * checks whether there is a whitespace between acronym and preceeding full form; returns the full form end index
	 * accordingly. This also captures cases such as: "... Bundesrepublik Deutschland(BRD) ..."
	 * 
	 * @param sentence
	 *            the sentence where the acronym is found
	 * @param acroStart
	 *            the start index of the acronym (typically after the initial parentheses "("
	 * @return the end index of the full form
	 */
	private int getFfEnd(String sentence, int acroStart) {

		char c = sentence.charAt(acroStart - 2);
		if (Character.isWhitespace(c))
			return acroStart - 2;
		else
			return acroStart - 1;

	}

	/**
	 * Finds the startposition of the fullform to the given acronym (Schwartz/Hearst-Algorithm)
	 * 
	 * @param potFF
	 *            potential fullform (the string that should contain the fullform to the acronym 'acro'
	 * @param acro
	 *            the acronym
	 * @return the beginning of the fullform or -1 if nothing was found
	 */
	private int findFullformStart(String potFF, String acro) {
		int shortIndex = acro.length() - 1;
		int longIndex = potFF.length() - 1;
		char curCharShort, curCharLong;
		String fullForm = "";

		LOGGER.debug("findFullformStart() -- acro: " + acro);
		LOGGER.debug("findFullformStart() -- potential FF: " + potFF);

		if (acro2fullForm.containsKey(acro)) {
			fullForm = acro2fullForm.get(acro);
			// System.err.println(potFF + " - " + fullForm);
			int start = potFF.toLowerCase().indexOf(fullForm);
			if (start != -1) {
				return --start;
			}
		}

		while (shortIndex >= 0) {

			curCharShort = acro.charAt(shortIndex);
			curCharLong = potFF.charAt(longIndex);

			// "normalization" of current characters
			if (Character.isLetter(curCharShort))
				curCharShort = Character.toLowerCase(curCharShort);
			if (Character.isLetter(curCharLong))
				curCharLong = Character.toLowerCase(curCharLong);

			// skip special characters in the shortform
			if (Character.isWhitespace(curCharShort) || Character.isDigit(curCharShort)
					|| curCharShort == '-'
					|| curCharShort == '+') {
				shortIndex--;
				continue;
			}

			while ((longIndex >= 0 && curCharShort != curCharLong) || (longIndex > 0 && shortIndex == 0 &&
			// fill in 'stopmarks' for fullforms here:
			!(Character.isWhitespace(potFF.charAt(longIndex - 1)) || potFF.charAt(longIndex - 1) == '-'
					|| potFF.charAt(longIndex - 1) == ')'
					|| potFF.charAt(longIndex - 1) == '/' || potFF.charAt(longIndex - 1) == '"'))) {
				longIndex--;
				if (longIndex >= 0) {
					curCharLong = potFF.charAt(longIndex);
					if (Character.isLetter(curCharLong))
						curCharLong = Character.toLowerCase(curCharLong);
				}
			}

			// error if no match for the current character found in the
			// fullform
			if (longIndex <= 0 && shortIndex >= 0) {
				return -1;
			}

			longIndex--; // else a match was found --> go on
			shortIndex--;
		}

		return longIndex;
	}

	/**
	 * looks for the 'best' position in the sentence to start looking for a fullform
	 * 
	 * @param sentence
	 * @param acroStart
	 * @param maxTokens
	 * @return
	 */
	private int getPotFullformStart(String sentence, int acroStart, int acroLength) {

		/*
		 * idea: go MAXLENGTHFACTOR * acroLength tokens backwards and ignore stopwords
		 */

		int aTokens = 0; // number of tokens

		String s = sentence.substring(0, acroStart);
		int oldp = s.length() - 1;
		int p = getNextToken(s, oldp);
		int i;
		String token;

		// BUGFIX: acronyms should at most start at position 2
		if (acroStart >= 2 && s.charAt(acroStart - 2) == ' ')
			aTokens--;

		while (p != -1 && aTokens != MAXLENGTHFACTOR * acroLength) {

			if (p == 0) {
				token = s.substring(0, oldp);
			} else {
				token = s.substring(p + 1, oldp);
			}
			i = 0;
			// while (i < STOPWORDS.length) {
			//
			// if (token.equals(STOPWORDS[i])) {
			// aTokens--;
			// break;
			// }
			// i++;
			// }

			if (Arrays.binarySearch(STOPWORDS, token) >= 0) {
				aTokens--;
			}

			aTokens++;
			if (aTokens == acroLength + 2)
				break;
			oldp = p;
			p = getNextToken(s, oldp - 1);
		}

		if (p == 0) {
			return p;
		}

		return p + 1;
	}

	/**
	 * returns the position of the next token BEFORE the current position
	 * 
	 * @param s
	 *            a (sub)sentence
	 * @param index
	 *            the startposition to look (backwards) for the next token
	 * @return the position of the first token BEFORE index (or -1, if nothing was found)
	 */
	int getNextToken(String s, int index) {
		if (index == 0 || index == -1) {
			return -1;
		}
		int p = index;
		while (p != 0 && s.charAt(p) != ' ') {
			p--;
		}
		return p;
	}

	/**
	 * if called, it looks for the 'best' fullform in the AcronymEntry (an AE contains 1 shortform + many according
	 * fullforms) until now, the 'best' fullform is the one that has the highest count
	 * 
	 * @param ae
	 *            the Acronym Entry that contains the list of fullforms
	 * @return the best fullform
	 */

	private String getBestFullformFromDict(AcronymEntry ae) {
		Set<Entry<String, FullformEntry>> fullforms = ae.getAllFullforms();
		FullformEntry fEntry;
		String fName = "";
		int temp = 0;
		for (Map.Entry<String, FullformEntry> entry : fullforms) {
			fEntry = entry.getValue();
			if (fEntry.count > temp) {
				temp = fEntry.count;
				fName = entry.getKey();
			}
		}
		return fName;
	}

	/*
	 * 
	 * private boolean isActualAcronym(String documenttext, int b, int e) {
	 * 
	 * String text = documenttext; int begin = b-2; int end = e+2;
	 * 
	 * if(begin < 0) { begin++; if(begin < 0) begin++; } if(end > text.length()) { end--; if(end > text.length()) end--;
	 * }
	 * 
	 * String pattern = "^.* .{2,} .*$"; String fragment = text.substring(begin, end);
	 * 
	 * if(fragment.matches(pattern)) return true;
	 * 
	 * return false; }
	 */
}
