/** 
 * TokenNGramPipe.java
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
 * Creation date: Mar 5, 2008 
 * 
 * This pipe creates token-level ngrams. The instance is assumed to 
 * have a TokenSequence in the data field! 
 **/

package de.julielab.jcore.ae.jnet.tagger;

import java.util.ArrayList;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenNGramPipe extends Pipe {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int[] ngramSizes;

	public TokenNGramPipe(final int[] ngramSizes) {
		this.ngramSizes = ngramSizes;
	}

	@Override
	public Instance pipe(final Instance carrier) {

		final TokenSequence tokenSequence = (TokenSequence) carrier.getData();
		final String[] tokenTexts = new String[tokenSequence.size()];
		for (int i = 0; i < tokenSequence.size(); i++) {
			final Token t = tokenSequence.get(i);
			tokenTexts[i] = t.getText();
		}

		// now make new ngram features
		for (int i = 0; i < tokenSequence.size(); i++) {
			final Token token = tokenSequence.get(i);
			final ArrayList<String> ngrams = (new NGramGenerator())
					.generateTokenNGrams(tokenTexts, i, ngramSizes);
			for (final String ngram : ngrams)
				token.setFeatureValue("TOK_NGRAM=" + ngram, 1.0);
		}

		return carrier;
	}

}
