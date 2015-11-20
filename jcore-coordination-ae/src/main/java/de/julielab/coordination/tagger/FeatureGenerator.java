/** 
 * FeatureGenerator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Nov 1, 2006 
 * 
 * Generating features for given text. Text is given as an ArrayList of Sentence objects that is,
 * an ArrayList of ArrayLists of Unit objects.
 **/

package de.julielab.coordination.tagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.PrintTokenSequenceFeatures;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.pipe.tsf.LexiconMembership;
import edu.umass.cs.mallet.base.pipe.tsf.OffsetConjunctions;
import edu.umass.cs.mallet.base.pipe.tsf.RegexMatches;
import edu.umass.cs.mallet.base.pipe.tsf.TokenTextCharNGrams;
import edu.umass.cs.mallet.base.pipe.tsf.TokenTextCharPrefix;
import edu.umass.cs.mallet.base.pipe.tsf.TokenTextCharSuffix;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.LabelAlphabet;

class FeatureGenerator {

	static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

	// all upper case letters (consider different languages, too)
	static String CAPS = "A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÄÖÜ";

	// all lower case letters (consider different languages, too)
	static String LOW = "a-zàèìòùáéíóúçñïäöü";

	static public InstanceList createFeatureData(ArrayList<Sentence> sentences,
			LabelAlphabet dict, Properties featureConfig) {

		ArrayList<Pipe> pipeParam = new ArrayList<Pipe>();

		// base pipe
		pipeParam.add(new BasePipe(featureConfig));

		// default surface patterns
		pipeParam.add(new RegexMatches("INITLOWCAPS_ANYTHING_NONUMBER", Pattern
				.compile("[" + LOW + "][" + CAPS + "][^0-9]*")));
		pipeParam.add(new RegexMatches("INITLOWCAPS_ANYTHING_WITHNUMBER",
				Pattern.compile("[" + LOW + "][" + CAPS + "].*[0-9].*")));
		pipeParam.add(new RegexMatches("INITCAPS", Pattern.compile("[" + CAPS
				+ "].*")));
		pipeParam.add(new RegexMatches("INITCAPSALPHA", Pattern.compile("["
				+ CAPS + "][" + LOW + "].*")));
		pipeParam.add(new RegexMatches("ALLCAPS", Pattern.compile("[" + CAPS
				+ "]+")));
		pipeParam.add(new RegexMatches("CAPSMIX", Pattern.compile("[" + CAPS
				+ LOW + "]+")));
		pipeParam
				.add(new RegexMatches("HASDIGIT", Pattern.compile(".*[0-9].*")));
		pipeParam
				.add(new RegexMatches("SINGLEDIGIT", Pattern.compile("[0-9]")));
		pipeParam.add(new RegexMatches("DOUBLEDIGIT", Pattern
				.compile("[0-9][0-9]")));
		pipeParam.add(new RegexMatches("NATURALNUMBER", Pattern
				.compile("[0-9]+")));
		pipeParam.add(new RegexMatches("REALNUMBER", Pattern
				.compile("[-0-9]+[.,]+[0-9.,]+")));
		pipeParam.add(new RegexMatches("HASDASH", Pattern.compile(".*-.*")));
		pipeParam.add(new RegexMatches("INITDASH", Pattern.compile("-.*")));
		pipeParam.add(new RegexMatches("ENDDASH", Pattern.compile(".*-")));
		pipeParam.add(new RegexMatches("ALPHANUMERIC", Pattern.compile(".*["
				+ CAPS + LOW + "].*[0-9].*")));
		pipeParam.add(new RegexMatches("ALPHANUMERIC", Pattern
				.compile(".*[0-9].*[" + CAPS + LOW + "].*")));
		pipeParam.add(new RegexMatches("IS_PUNCTUATION_MARK", Pattern
				.compile("[,.;:?!]")));

		pipeParam.add(new RegexMatches("IS_MINUSDASHSLASH", Pattern
				.compile("[-_/]")));

		// bio surface patterns
		if (FeatureConfiguration.featureActive(featureConfig,
				"feat_bioregexp_enabled")) {
			pipeParam.add(new RegexMatches("ROMAN", Pattern
					.compile("[IVXDLCM]+")));
			pipeParam.add(new RegexMatches("HASROMAN", Pattern
					.compile(".*\\b[IVXDLCM]+\\b.*")));
			pipeParam.add(new RegexMatches("GREEK", Pattern.compile(GREEK)));
			pipeParam.add(new RegexMatches("HASGREEK", Pattern.compile(".*\\b"
					+ GREEK + "\\b.*")));
		}

		// prefix and suffix
		int[] prefixSizes = FeatureConfiguration.getIntArray(featureConfig,
				"prefix_sizes");
		if (prefixSizes != null) {
			for (int i = 0; i < prefixSizes.length; i++) {
				pipeParam
						.add(new TokenTextCharPrefix("PREFIX=", prefixSizes[i]));
			}
		}
		int[] suffixSizes = FeatureConfiguration.getIntArray(featureConfig,
				"suffix_sizes");
		if (suffixSizes != null) {
			for (int i = 0; i < suffixSizes.length; i++) {
				pipeParam
						.add(new TokenTextCharSuffix("SUFFIX=", suffixSizes[i]));

			}
		}

		// lexicon membership
		for (String key : FeatureConfiguration.getLexiconKeys(featureConfig)) {
			File lexFile = new File(featureConfig.getProperty(key));
			try {
				pipeParam.add(new LexiconMembership(key + "_membership",
						lexFile, true));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		// offset conjunction
		int[][] offset = FeatureConfiguration
				.offsetConjFromConfig(featureConfig
						.getProperty("offset_conjunctions"));
		if (offset != null) {
			pipeParam.add(new OffsetConjunctions(offset));
		}

		// token ngrams
		int[] tokenNGrams = FeatureConfiguration.getIntArray(featureConfig,
				"token_ngrams");
		if (tokenNGrams != null) {
			pipeParam.add(new TokenNGramPipe(tokenNGrams));
		}

		// character ngrams
		int[] charNGrams = FeatureConfiguration.getIntArray(featureConfig,
				"char_ngrams");
		if (charNGrams != null) {
			pipeParam.add(new TokenTextCharNGrams("CHAR_NGRAM=", charNGrams));
		}

		// un-comment this for printing out the generated features
		// pipeParam.add(new PrintTokenSequenceFeatures());

		pipeParam.add(new TokenSequence2FeatureVectorSequence(true, true));
		Pipe[] pipeParamArray = new Pipe[pipeParam.size()];
		pipeParam.toArray(pipeParamArray);
		Pipe myPipe = new SerialPipes(pipeParamArray);
		myPipe.setTargetAlphabet(dict);
		InstanceList data = new InstanceList(myPipe);
		data.add(new SentencePipeIterator(sentences));

		return data;
	}

}
