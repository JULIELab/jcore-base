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
package de.julielab.jcore.ae.jnet.tagger;

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
