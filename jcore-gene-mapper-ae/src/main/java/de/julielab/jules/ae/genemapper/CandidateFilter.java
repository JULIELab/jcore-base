/** 
 * CandidateFilter.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.0 	
 * Since version:   1.6
 *
 * Creation date: Feb 19, 2008 
 * 
 * a class to filtered out candidates found by lucene, mostly FALSE POSITIVES
 * 
 * TODO: add a rule that difference is only in 
 * GREEK or MODIFIER or one has NUMBER/GREEK/MODIFIER and the other doesnt 
 **/

package de.julielab.jules.ae.genemapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.utils.Utils;
import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class CandidateFilter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CandidateFilter.class);

	private static final String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

	private static final String SUB_GREEK = "(beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

	private static String MODIFIER = "(receptor|cofactor|factor|tranporter|regulator|inhibitor|activator|suppressor|enhancer|repressor|adaptor|interactor|modulator|mediator|inducer|effector|coactivator|supressor|integrator|facilitator|binder|terminator|acceptor|responder|proactivator|exchanger|enhancer|adapter|responder|modifier|ligand)";

	private static String NON_DESCRIPTIVE = "(construct|fragment|antigen|precursor|proteins?|chain|domain|gene|homolog|homologue|isoform|isolog|isotype|motif|ortholog|precursor|precursors|product|sequence|subtype|subunit)";

	public static String VOID_SPECS = "^(human|h) ";

	public Pattern patternVoidSpecs = Pattern.compile(VOID_SPECS + ".*");

	public String NON_DESC = "(promoter|onco protein|oncoprotein|proto oncogene|protooncogene|protease|binding site|transcript|element|construct|si rna|prem rna|pre m rna|m rna ?s?|rna|locus|gene product|product|reporter gene|reporter|gene|protein|c dna|molecule|pseudogene|autoantigen|peptide|polypeptide|enzyme)$";

	public Pattern patternNonDesc;

	// public static String DOMAINS = "^(gat|miu|w|ef
	// h|cue|btb|arm|tsnare|uba|uev|uim|sh 2|ptb|c 1|c
	// 2|ferm|calm|glue|gram|beach|bar|bx|ph|fyve|evh 1|ww|sh
	// 3|gyf|bir|traf|ded|death|card|bh|bh 1|bh 2|bh 3|bh 4|polo box|ff|brct|wd
	// 40|mh 2|fha|14 3 3|adf|gel|dh|ch|fh 2|eg|cc|vhl|vhl
	// beta|vhs|ring|hect|mbt|pwwp|spry|swirm|tudor|pumilo|puf|dep|pas|mh
	// 1|lrr|iq|heat|grip|tubby|share|tpr|tir|start|socs box|sam|rgs|pbz|pd
	// 1|lim|f box|enth|ef hand|shadow chromo|chromo|bromo|arm|ank)
	// (domains|motif|repeat)s?";
	public String DOMAIN_FAMILIES = "^.*(acceptors|acid|activators|adapters|adaptors|antibodi|antibody|binders|binding|binding site|binding sites|box|boxe|channel|channels|chromosome|coactivators|cofactors|complex|domain|dyneins|effectors|element|enhancers|epitope|erythrocyte|exchangers|exon|facilitators|factors|familie|family|filament|finger|helicases|histone|histones|homeodomain|inducers|inhibitors|integrators|interactors|intron|kinases|kinesins|lectins|ligands|mediators|member|membrane|modifiers|modulators|motif|myosins|proactivators|proteases|proteasome|proteins|reductases|region|regulators|repeat|repressors|residue|responders|sequence|site|subdomain|subfamily|subunits|superfamily|suppressors|supressors|syndrome|tail|terminal|terminators|terminus|tranporters|transferases|zinc finger)e?s?";

	//public Pattern patternDomainFamilies = Pattern
		//	.compile(DOMAIN_FAMILIES);

	public Pattern patternDomainFamilies;
	
	private static final String UNSPECIFIEDS_FILE = "/unspecified_proteins";

	public String UNSPECIFIEDS;

	public Pattern patternUnspecifieds;

	private static final String PREMOD_FILE = "/premodifiers";
	

	public String PREMODS;

	public Pattern patternPreMods;

	private Pattern num;

	private Pattern singChar;

	private Pattern specWords;

	public CandidateFilter() throws IOException {

		initUnspecifieds();
		initPreModifiers();
		patternDomainFamilies = Pattern.compile(DOMAIN_FAMILIES);
		patternNonDesc = Pattern.compile(".* " + NON_DESC);
		num = Pattern.compile("[0-9]*");
		singChar = Pattern.compile("([a-z]|[0-9])");
		specWords = Pattern.compile("(" + GREEK + "|" + MODIFIER + "|"
				+ "|" + NON_DESCRIPTIVE + ")");
	}

	public static void main(String[] args) throws IOException {

		String a = "il receptor 1 beta";
		String b = "il receptor 1";

		CandidateFilter cf = new CandidateFilter();
		// System.out.println("numn: " + getNumberOfOccurrences(a, "[0-9]+"));
		System.out.println("diff num: "
				+ cf.onlyDifferentTypes(a, b, "([0-9]+)"));
		System.out.println("diff greek: " + cf.onlyDifferentTypes(a, b, GREEK));
		System.out.println("has one greek type: "
				+ cf.differInTypeOfOneTerm(a, b, SUB_GREEK));

		a = "il receptor 3";
		b = "il receptor 1";
		System.out.println("has one number type: "
				+ cf.differInTypeOfOneTerm(a, b, "([02-9]|[1-9]{2,})"));
	}

	/**
	 * checks whether both terms differ only in the type, i.e. one has an
	 * instance of that type, and the other doesn't
	 * 
	 */
	private boolean differInTypeOfOneTerm(String searchTerm, String foundTerm,
			String type) {
		if (searchTerm.equals(foundTerm)) {
			return false;
		}
		TreeSet<String> s1 = getSet(searchTerm.split("\\s+"));
		TreeSet<String> s2 = getSet(foundTerm.split("\\s+"));
		TreeSet<String> diff = new TreeSet<String>();

		if (s1.size() == s2.size() + 1) {
			diff = s1;
			diff.removeAll(s2);
		} else if (s1.size() == s2.size() + 1) {
			diff = s2;
			diff.removeAll(s1);
		}
		if (diff.size() == 1) {
			String diffToken = diff.first();
			Pattern pat = Pattern.compile(type);
			Matcher m = pat.matcher(diffToken);
			// System.out.println("difftoken: " + diffToken);
			if (m.matches()) {
				// System.out.println("difference in type");
				return true;
			} else {
				// System.out.println("difference NOT in type");
				return false;
			}

		}
		return false;

	}

	private TreeSet<String> getSet(String[] array) {
		TreeSet<String> mySet = new TreeSet<String>();
		for (int i = 0; i < array.length; i++) {
			mySet.add(array[i]);
		}
		return mySet;
	}

	private int getNumberOfOccurrences(String term, String occurrence) {
		String[] tokens = term.split("\\s+");
		int num = 0;
		Pattern p = Pattern.compile(occurrence);
		for (int i = 0; i < tokens.length; i++) {
			Matcher m = p.matcher(tokens[i]);
			if (m.matches()) {
				num++;
			}
		}
		return num;
	}

	/**
	 * checks whether two terms differ in everything except a type. Such a type
	 * may only occure once in each term. A type is e.g.: "([0-9]+)"
	 * 
	 * It is important to put square brackets around the type!
	 */
	private boolean onlyDifferentTypes(String searchTerm, String foundTerm,
			String type) {
		String query = "([a-z0-9 ]*?) ?" + type + " ?([a-z0-9 ]*?)";
		Pattern num = Pattern.compile(query);

		if (getNumberOfOccurrences(searchTerm, type) == 1
				&& getNumberOfOccurrences(foundTerm, type) == 1) {
			Matcher m1 = num.matcher(searchTerm);
			Matcher m2 = num.matcher(foundTerm);
			if (m1.matches() && m2.matches()) {
				if ((!m1.group(2).equals(m2.group(2)))
						&& (m1.group(1).equals(m2.group(1)))
						&& (m1.group(3).equals(m2.group(3)))) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * method to filtered out some hits by some rules
	 * 
	 * rule 1: if overlap is only constituted by numbers
	 * 
	 * @return
	 */
	public boolean filterOut(String searchTerm, String foundTerm) {
		TreeSet<String> commonWords = Utils.getCommonWords(searchTerm
				.split(" "), foundTerm.split(" "));

		boolean allNumbers = true;
		for (String c : commonWords) {
			Matcher m = num.matcher(c);
			if (!m.matches()) {
				allNumbers = false;
				break;
			}
		}
		if (allNumbers) {
			LOGGER.debug("filtered out because: overlap only numbers: '"
					+ searchTerm + "' <-> '" + foundTerm + "'");
			return allNumbers;
		}

		boolean singleCharDigits = true;
		for (String c : commonWords) {
			Matcher m = singChar.matcher(c);
			if (!m.matches()) {
				singleCharDigits = false;
				break;
			}
		}

		if (singleCharDigits) {
			LOGGER
					.debug("filtered out because: overlap only single characters or single digits: '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return singleCharDigits;
		}

		boolean onlySpecialWords = true;
		for (String c : commonWords) {
			Matcher m = specWords.matcher(c);
			if (!m.matches()) {
				onlySpecialWords = false;
				break;
			}
		}

		if (onlySpecialWords) {
			LOGGER
					.debug("filtered out because: overlap consists only of special words (greek, modifiers, non-descriptive): '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return onlySpecialWords;
		}

		// check whether difference is only in different numbers
		if (onlyDifferentTypes(searchTerm, foundTerm, "([0-9]+)") == true) {
			LOGGER
					.debug("filtered out because: terms differ in one number only: '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return true;
		}

		// check whether difference is only in different GREEKs
		if (onlyDifferentTypes(searchTerm, foundTerm, GREEK) == true) {
			LOGGER
					.debug("filtered out because: terms differ in one greek token only: '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return true;
		}

		// only difference: one has a number and the other doesn't (1 is
		// excluded)
		if (differInTypeOfOneTerm(searchTerm, foundTerm, "([02-9]|[1-9]{2,})") == true) {
			LOGGER
					.debug("filtered out because: one has a number and the other doesn't (1 is excluded): '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return true;
		}

		// only difference: one has a greek and the other doesn't (alpha is
		// excluded)
		if (differInTypeOfOneTerm(searchTerm, foundTerm, SUB_GREEK) == true) {
			LOGGER
					.debug("filtered out because: one has a greek and the other doesn't (alpha is excluded): '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return true;
		}

		// only difference: one has a modifier and the other doesn't
		if (differInTypeOfOneTerm(searchTerm, foundTerm, MODIFIER) == true) {
			LOGGER
					.debug("filtered out because: one has a modifier and the other doesn't: '"
							+ searchTerm + "' <-> '" + foundTerm + "'");
			return true;
		}

		return false;
	}

	public void initUnspecifieds() throws IOException {

		TermNormalizer normalizer = new TermNormalizer();
		InputStream in = this.getClass().getResourceAsStream(UNSPECIFIEDS_FILE);
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(isr);
		UNSPECIFIEDS = "^(";

		try {

			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("##"))
					continue;
				line = line.trim();
				line = normalizer.normalize(line);
				UNSPECIFIEDS += line.trim() + "|";
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		UNSPECIFIEDS = UNSPECIFIEDS.substring(0, UNSPECIFIEDS.length() - 1)
				+ ")e?s?$";
		UNSPECIFIEDS = UNSPECIFIEDS.trim();
		patternUnspecifieds = Pattern.compile(UNSPECIFIEDS);
		LOGGER.debug("Initializing unspecified proteins pattern from file: "
				+ patternUnspecifieds);
	}

	public void initPreModifiers() throws IOException {

		InputStream in = this.getClass().getResourceAsStream(PREMOD_FILE);
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(isr);
		PREMODS = "^(";

		try {

			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("##"))
					continue;
				PREMODS += line.trim() + "|";
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// PREMODS = PREMODS.trim();
		PREMODS = PREMODS.substring(0, PREMODS.length() - 1) + ") ";
		patternPreMods = Pattern.compile(PREMODS + ".*");
		LOGGER.debug("Initializing protein void premodifiers from file: "
				+ patternPreMods);
	}

}
