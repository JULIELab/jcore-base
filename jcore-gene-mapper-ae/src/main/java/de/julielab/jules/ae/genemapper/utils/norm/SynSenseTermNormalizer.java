/** 
 * TermNormalizer.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.5.1
 * Since version:   1.0
 *
 * Creation date: Dec 1, 2006 
 * 
 * Used by GeneMapper to normalize the query terms before searching
 * for them.
 * 
 * Fundamentally changed since 1.1: token splitting rule!
 **/
package de.julielab.jules.ae.genemapper.utils.norm;

import java.io.BufferedReader;
import java.io.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @deprecated Check if this class is used anywhere (resource scripts?)
 * @author faessler
 *
 */
@Deprecated
public class SynSenseTermNormalizer {

	private final String NON_DESCRIPTIVES_FILE = "/non_descriptives";

	private final String NUMBERPATTERN = "([A-Za-z]+)([0-9]+)";

	private final String CHARPATTERN = "([0-9A-Za-z]+)(high|low|alpha|beta|gamma|delta|epsilon|zeta)([0-9A-Za-z]*)";

	private final String SHORTFORMPATTERN = "((.*[0-9a-z]+)(L|R)|(.*[0-9]+)(l|r)|(r|l|R|L))";

	private final String SHORTFORMEND_WITH_NUMBER_PATTERN = "(.* )(ra|rb|rg|bp)( [0-9]*)?";

	private final String SHORTFORMEND_NO_NUMBER_PATTERN = "(.* )(a|b)";

	private final String TOKENSPLITPATTERN = "(.*[a-z])([A-Z0-9].*)|(.*[A-Z])([0-9].*)|(.*[0-9])([a-zA-Z].*)|(.*[A-Z][A-Z])([a-z].*)";
	
	private final String DOTREMOVAL = "(.*)([a-zA-Z])\\.([a-zA-Z])(.*)";

	private TreeSet<String> nonDescriptives;

	private TreeSet<String> stopwords;

	private HashMap<String, String> plurals;

	private Pattern numberPattern;

	private Pattern charPattern;

	private Pattern shortFormPattern;

	private Pattern shortFormEndWithNumberPattern;

	private Pattern shortFormEndNoNumberPattern;

	private Pattern tokenSplitPattern;

	private Pattern dotRemovalPattern;

	public SynSenseTermNormalizer() throws IOException {
		numberPattern = Pattern.compile(NUMBERPATTERN);
		charPattern = Pattern.compile(CHARPATTERN);
		shortFormPattern = Pattern.compile(SHORTFORMPATTERN);
		tokenSplitPattern = Pattern.compile(TOKENSPLITPATTERN);
		dotRemovalPattern = Pattern.compile(DOTREMOVAL);
		shortFormEndWithNumberPattern = Pattern
				.compile(SHORTFORMEND_WITH_NUMBER_PATTERN);
		shortFormEndNoNumberPattern = Pattern
				.compile(SHORTFORMEND_NO_NUMBER_PATTERN);
		initStopwords();
		initPlurals();
		initNonDescriptives();
	}

	/**
	 * normalize a single synonym
	 * 
	 * @param term
	 * @return
	 */
	public String normalize(String term) {
		
		int termOld = 0;
		ArrayList<String> newTerm = removeStopwords(term);
		newTerm = removeSpecialCharacters(newTerm);

		do { // apply till there are no more changes
			termOld = newTerm.hashCode();
			newTerm = splitAwayCharacterStrings(newTerm);
			newTerm = splitAwayNumbers(newTerm);
			//newTerm = replaceShortForms(newTerm);
			newTerm = specialTokenSplit(newTerm);
			newTerm = transformPlurals(newTerm);
		} while (newTerm.hashCode() != termOld);

		newTerm = replaceRomanNumbers(newTerm);
		//newTerm = replaceKnownAcronyms(newTerm);
		// TODO: apply (porter) stemmer or morphosaurus

		term = ArrayList2String(newTerm);
		term = toLowerCase(term);

		//term = replaceShortFormsAtEnd(term);
		term = removeNonDescriptives(term);
		
		term = term.trim();
		
		
		/*
		// Ananiadou-style morph. Normalization
		term = term.replaceAll("\\-", " ");
		term = toLowerCase(term);
		*/
		return term;
	}

	/**
	 * normalize all synonyms in a file (biothesaurus) where the first column is
	 * the synonym and the second column is id. all other columns are ignored.
	 * columns have to be tab-separated.
	 * 
	 * @param inputFile
	 *            the input file (biothesaurus)
	 * @param outputFile
	 *            output file for normalized synonyms
	 * @param orgSyn
	 *            whether original (unnormalized) synonym should be written to
	 *            file or not
	 */
	public void normalizeFile(File inputFile, File outputFile, boolean orgSyn) {
		
		int ignoredLines = 0;
		try {
			BufferedReader fileIn = new BufferedReader(
					new FileReader(inputFile));
			// First line contains just headings and we don't want them
			String text = "";
			FileWriter fileOut = new FileWriter(outputFile);
			while ((text = fileIn.readLine()) != null) {
				String[] store = text.split("\t");
				if (store.length != 3) {
					ignoredLines++;
					System.err.println("wrong line format, ignoring line: " + text);
				} else {
					String normalizedSyn = normalize(store[1]);
					//normalizedSyn = normalizedSyn.replaceAll("\\.", "");
					
					if (orgSyn == true) {
						fileOut.write(store[0] + "\t" + normalizedSyn + "\t"
								+ store[1] + "\t" + store[2] + "\n");
					} else {
						fileOut.write(store[0] + "\t" + normalizedSyn + "\t" + store[2] + "\n");
					}
				}
			}
			fileIn.close();
			fileOut.flush();
			fileOut.close();
		} catch (IOException io) {
			io.printStackTrace();
		}
		
		System.out.println("\n\n\ndone");
		System.out.println("number of ignored lines (due to wrong format): " + ignoredLines);
	}

	/**
	 * inserts whitespaces at the following positions: [a-z]->[A-Z0-9]
	 * [A-z]->[0-9] [0-9]->[a-zA-Z]
	 * 
	 * @param newTerm
	 * @return
	 */
	private ArrayList<String> specialTokenSplit(ArrayList<String> newTerm) {
		for (int i = 0; i < newTerm.size(); ++i) {
			String myTerm = newTerm.get(i);
			do {
				newTerm.remove(i);
				newTerm.add(i, myTerm);
				Matcher m = tokenSplitPattern.matcher(myTerm);
				if (m.matches()) {
					if (m.group(1) != null && m.group(2) != null) {
						myTerm = m.group(1) + " " + m.group(2);
					} else if (m.group(3) != null && m.group(4) != null) {
						myTerm = m.group(3) + " " + m.group(4);
					} else if (m.group(5) != null && m.group(6) != null) {
						myTerm = m.group(5) + " " + m.group(6);
					} else if (m.group(7) != null && m.group(8) != null) {
						myTerm = m.group(7) + " " + m.group(8);
					}
					
				}
			} while (!myTerm.equals(newTerm.get(i)));
		}

		ArrayList<String> finalTerms = new ArrayList<String>();
		for (String token : newTerm) {
			if (token.length() > 0) {
				String[] values = token.split(" ");
				for (int i = 0; i < values.length; i++) {
					finalTerms.add(values[i]);
				}
			}
		}
		return finalTerms;
	}

	/**
	 * split away any character strings (e.g. "alpha" or "low") if proceeded by
	 * anything else
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> splitAwayCharacterStrings(ArrayList<String> term) {
		for (int i = 0; i < term.size(); i++) {
			Matcher m = charPattern.matcher(term.get(i));
			if (m.matches()) {
				term.set(i, m.group(1));
				++i;
				term.add(i, m.group(2));
				++i;
				if (!m.group(3).equals("")) {
					term.add(i, m.group(3));
				}
			}
		}
		return term;
	}

	/**
	 * splits away short forms for ligand or receptor (others to come) and
	 * replace them by their full form. replacements are allowed iff either "L"
	 * or "R" follow lower-case letters or numbers or if "l" or "r" follow
	 * upper-case letters or numbers or if "r", "R", "l" or "L" are single
	 * tokens
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> replaceShortForms(ArrayList<String> term) {
		for (int i = 0; i < term.size(); i++) {
			Matcher m = shortFormPattern.matcher(term.get(i));
			if (m.matches()) {
				String base = "";
				String substitute = "";
				// upper-case receptor or ligand
				if (m.group(3) != null) {
					base = m.group(2);
					substitute = m.group(3);
					if (substitute.equals("L")) {
						substitute = "ligand";
					} else if (substitute.equals("R")) {
						substitute = "receptor";
					}

				} else if (m.group(5) != null) {
					base = m.group(4);
					substitute = m.group(5);
					if (substitute.equals("l")) {
						substitute = "ligand";
					} else if (substitute.equals("r")) {
						substitute = "receptor";
					}

				} else if (m.group(6) != null) {
					if (m.group(1).toLowerCase().equals("l")) {
						substitute = "ligand";
					} else if (m.group(1).toLowerCase().equals("r")) {
						substitute = "receptor";
					}
				}

				term.set(i, base);
				++i;
				term.add(i, substitute);
			}
		}
		return term;
	}

	/**
	 * replaces short forms at the end of a synonym! this function should be
	 * applied only after token split, replaceShortForms, toLowerCase, trim etc.
	 * 
	 * @param term
	 * @return
	 */
	private String replaceShortFormsAtEnd(String term) {
		String replacement = "";
		Matcher m = shortFormEndWithNumberPattern.matcher(term);
		if (m.matches()) {
			if (m.group(2).equals("ra")) {
				replacement = "receptor alpha";
			} else if (m.group(2).equals("rb")) {
				replacement = "receptor beta";
			} else if (m.group(2).equals("rg")) {
				replacement = "receptor gamma";
			} else if (m.group(2).equals("bp")) {
				replacement = "binding protein";
			} else if (m.group(2).equals("a")) {
				replacement = "alpha";
			} else if (m.group(2).equals("b")) {
				replacement = "beta";
			}
			if (replacement.length() > 0) {
				String number = "";
				if (m.group(3) != null) {
					number = m.group(3);
				}
				return m.group(1) + replacement + number;
			}
		}

		m = shortFormEndNoNumberPattern.matcher(term);
		if (m.matches()) {
			if (m.group(2).equals("a")) {
				replacement = "alpha";
			} else if (m.group(2).equals("b")) {
				replacement = "beta";
			}
			if (replacement.length() > 0) {
				return m.group(1) + replacement;
			}
		}
		return term;
	}

	/**
	 * replace other known acronyms by their full forms. Currently only a rule
	 * for: IL -> interleukin
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> replaceKnownAcronyms(ArrayList<String> term) {
		for (int i = 0; i < term.size(); i++) {
			if (term.get(i).equals("il") || term.get(i).equals("IL")) {
				term.set(i, "interleukin");
			}
		}
		return term;
	}

	/**
	 * at transitions from a sequence of letters to (a sequence of) numbers we
	 * split away the numbers
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> splitAwayNumbers(ArrayList<String> term) {
		for (int i = 0; i < term.size(); ++i) {
			Matcher m = numberPattern.matcher(term.get(i));
			if (m.matches()) {
				term.set(i, m.group(1));
				++i;
				term.add(i, m.group(2));
			}
		}

		return term;
	}

	/**
	 * replaces roman by greek numbers only if synonym contains more than one
	 * token and if roman number is in capital letterns
	 * 
	 * @param synonym
	 * @return
	 */
	private ArrayList<String> replaceRomanNumbers(ArrayList<String> synonym) {
		if (synonym.size() > 1) {
			for (int i = 0; i < synonym.size(); ++i) {
				String token = synonym.get(i);
				if (token.equals("I")) {
					synonym.set(i, "1");
				} else if (token.equals("II")) {
					synonym.set(i, "2");
				} else if (token.equals("III")) {
					synonym.set(i, "3");
				} else if (token.equals("IV")) {
					synonym.set(i, "4");
				}

			}
		}
		return synonym;
	}

	/**
	 * transforms some known plural forms into their short forms
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> transformPlurals(ArrayList<String> term) {
		for (int i = 0; i < term.size(); i++) {
			if (plurals.containsKey(term.get(i))) {
				term.set(i, (String) plurals.get(term.get(i)));
			}
		}
		return term;
	}

	private String toLowerCase(String term) {
		term = term.replaceAll("[\\s ]+", " ");
		return term.toLowerCase();
	}

	/**
	 * replaces the following special characters by white space: - any non-word -
	 * except "." if between two numbers
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> removeSpecialCharacters(ArrayList<String> term) {
		ArrayList<String> newTerm = new ArrayList<String>();
		for (String token : term) {
			token = token.replaceAll("[\\W_&&[^\\.]]", " ");
			// token = token.replaceAll("[_]", " ");
			// token = token.replaceAll("[^\\.^\\w]", " ");
			Matcher m = dotRemovalPattern.matcher(token);
			if (m.matches()) {
				token = m.replaceFirst(m.group(1) + m.group(2) + " "
						+ m.group(3) + m.group(4));
			}
			token = token.replaceAll("[ ]+", " ");
			token = token.trim();
			if (token.length() > 0) {
				String[] values = token.split(" ");
				for (int i = 0; i < values.length; i++) {
					newTerm.add(values[i]);
				}
			}
		}
		return newTerm;
	}
	
	
	/**
	 * replaces dots and hyphens by white space:
	 * replication of Tsuruoka et al (2007) normalization
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> removeDotAndHyphen(ArrayList<String> term) {
		ArrayList<String> newTerm = new ArrayList<String>();
		for (String token : term) {
			
			token = token.replaceAll("\\-", " ");
			
			newTerm.add(token);
			
		}
		return newTerm;
	}

	/**
	 * remove stopwords as defined in treeset stopwords only if whole token
	 * equals the stopword
	 * 
	 * @param term
	 * @return
	 */
	private ArrayList<String> removeStopwords(String term) {
		String[] tokens = term.split(" ");
		ArrayList<String> newTerm = new ArrayList<String>(tokens.length);

		// this can actually happen if entity was 'for'
		// (ferredoxin oxidoreductase) See stopwords!
		if (tokens.length == 1) {
			newTerm.add(tokens[0]);
			return newTerm;
		} else {
			for (int i = 0; i < tokens.length; ++i) {
				if (!stopwords.contains(tokens[i])) {
					newTerm.add(tokens[i]);
				}
			}
		}
		return newTerm;
	}

	private String removeNonDescriptives(String term) {
		String[] tokens = term.split(" ");
		ArrayList<String> newTerm = new ArrayList<String>(tokens.length);

		for (int i = 0; i < tokens.length; ++i) {
			if (!nonDescriptives.contains(tokens[i])) {
				newTerm.add(tokens[i]);
			}
		}
		return ArrayList2String(newTerm);
	}

	private void initStopwords() {
		stopwords = new TreeSet<String>();
		stopwords.add("of");
		stopwords.add("for");
		stopwords.add("and");
		stopwords.add("or");
		stopwords.add("the");
		// TODO: remove (un-)defined articles; check POS tag!
	}

	private void initPlurals() {
		plurals = new HashMap<String, String>();
		plurals.put("receptors", "receptor");
		plurals.put("proteins", "protein");
		plurals.put("factors", "factor");
		plurals.put("ligands", "ligand");
		plurals.put("chains", "chain");
		plurals.put("antigens", "antigen");
		plurals.put("genes", "gene");
	}

	private void initNonDescriptives() throws IOException {
		nonDescriptives = new TreeSet<String>();
		
		InputStream in = this.getClass().getResourceAsStream(
				NON_DESCRIPTIVES_FILE);
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader nonDescReader = new BufferedReader(isr);
		
		try {

			String line = "";
			while ((line = nonDescReader.readLine()) != null) {
				nonDescriptives.add(line.trim());				
			}
			nonDescReader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		int c;
		// nonDesc.delete(0, nonDesc.length());
		while ((c = in.read()) != -1) {
			if (c == 10) {
				nonDescriptives.add(nonDesc.toString().toLowerCase());
				nonDesc.delete(0, nonDesc.length());
			} else {
				nonDesc.append((char) c);
			}
		}
		*/
		// System.out.println(nonDescriptives);
	}

	private String ArrayList2String(ArrayList<String> term) {
		StringBuffer transform = new StringBuffer("");
		for (int i = 0; i < term.size(); ++i) {
			transform.append(term.get(i) + " ");
		}
		// There is an entry named '-' in the biothesaurus!
		if (transform.length() != 0) {
			transform.deleteCharAt(transform.length() - 1);
		}
		return transform.toString().trim();
	}

	/**
	 * run the term normalizer on a file to be normalized (biothesaurus?)
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 3) {
			File unnormalizedFile = new File(args[0]);
			File outputFile = new File(args[1]);
			boolean orgSyn = new Boolean(args[2]);
			(new SynSenseTermNormalizer()).normalizeFile(unnormalizedFile, outputFile,
					orgSyn);

		} else {
			System.err
					.println("usage:\nTermNormalizer <inputFil> <outputFile> <printOrgSyn(true/false)>");
			System.exit(-1);
		}
	}

}