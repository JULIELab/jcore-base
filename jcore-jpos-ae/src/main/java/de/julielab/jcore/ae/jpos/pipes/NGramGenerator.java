/**
 * NGramGenerator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Feb 27, 2008
 *
 * generates different kinds of ngrams
 **/

package de.julielab.jcore.ae.jpos.pipes;

import java.util.ArrayList;

public class NGramGenerator {

	/**
	 * generates ngrams of all sizes specified in ngramSizes
	 *
	 * @param tokens
	 *            tokens of the sentence
	 * @param currPos
	 *            the current position relative to which the ngrams are to be
	 *            build
	 * @param the
	 *            ngramSizs of the ngrams
	 */
	public ArrayList<String> generateTokenNGrams(final String[] tokens,
			final int currPos, final int[] ngramSizes) {
		final ArrayList<String> allNGrams = new ArrayList<String>();
		for (final int ngramSize : ngramSizes)
			allNGrams.addAll(generateTokenNGrams(tokens, currPos, ngramSize));
		return allNGrams;
	}

	/**
	 * generates ngrams of size ngramSize
	 *
	 * @param tokens
	 *            tokens of the sentence
	 * @param currPos
	 *            the current position relative to which the ngrams are to be
	 *            build
	 * @param ngramSize
	 *            the size of the ngrams
	 */
	public ArrayList<String> generateTokenNGrams(final String[] tokens,
			final int currPos, final int ngramSize) {

		if (currPos > (tokens.length - 1))
			return null;

		final int minStart = Math.max(0, (currPos - ngramSize) + 1);
		final int maxStart = Math.min(currPos, tokens.length - 1);

		final ArrayList<String> ngrams = new ArrayList<String>();

		for (int i = minStart; i <= maxStart; i++)
			if ((i + ngramSize) <= tokens.length) {
				final StringBuffer ngram = new StringBuffer();
				for (int j = 0; j < ngramSize; j++)
					ngram.append(tokens[i + j] + " ");
				ngrams.add(ngram.toString().trim());
			}
		return ngrams;
	}

	public static void main(final String[] args) {
		final String[] tokens = new String[] { "0", "1", "2", "3", "4", "5" };
		// System.out.println(generateTokenNGrams(tokens, 2, 3));
		System.out.println((new NGramGenerator()).generateTokenNGrams(tokens,
				2, new int[] { 2, 3, 4 }));
	}
}
