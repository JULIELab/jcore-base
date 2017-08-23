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
package de.julielab.jnet.tagger;

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
