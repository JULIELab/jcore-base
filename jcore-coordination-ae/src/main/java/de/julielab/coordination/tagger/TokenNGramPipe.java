/** 
 * TokenNGramPipe.java
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
 * Creation date: Mar 5, 2008 
 * 
 * This pipe creates token-level ngrams. The instance is assumed to 
 * have a TokenSequence in the data field! 
 **/

package de.julielab.coordination.tagger;

import java.util.ArrayList;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;

public class TokenNGramPipe extends Pipe {

	private int[] ngramSizes;

	public TokenNGramPipe(int[] ngramSizes) {
		this.ngramSizes = ngramSizes;
	}

	public Instance pipe(Instance carrier) {

		TokenSequence tokenSequence = (TokenSequence) carrier.getData();
		String[] tokenTexts = new String[tokenSequence.size()];
		for (int i = 0; i < tokenSequence.size(); i++) {
			Token t = tokenSequence.getToken(i);
			tokenTexts[i] = t.getText();
		}

		// now make new ngram features
		for (int i = 0; i < tokenSequence.size(); i++) {
			Token token = tokenSequence.getToken(i);
			ArrayList<String> ngrams = NGramGenerator.generateTokenNGrams(
					tokenTexts, i, ngramSizes);
			for (String ngram : ngrams) {
				token.setFeatureValue("TOK_NGRAM=" + ngram, 1.0);
			}
		}

		return carrier;
	}

}
