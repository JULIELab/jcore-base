/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.pipe;

import java.util.List;
import java.util.Map;

import banner.types.Sentence;
import banner.types.Token;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

/**
 * This class is used by the CRFTagger as the base for the feature set.
 * 
 * @author Bob
 */
public class TokenWeight extends Pipe
{
	private static final long serialVersionUID = 1L;

	private transient Map<String, Double> tokenWeights = null;

	public TokenWeight(Map<String, Double> tokenWeights)
	{
		this.tokenWeights = tokenWeights;
	}

	public void setTokenWeights(Map<String, Double> tokenWeights)
	{
		this.tokenWeights = tokenWeights;
	}

	@Override
	public Instance pipe(Instance carrier)
	{
		Sentence sentence = (Sentence) carrier.getSource();
		List<Token> tokens = sentence.getTokens();

		TokenSequence data = (TokenSequence) carrier.getData();
		for (int i = 0; i < tokens.size(); i++)
		{
			Token bannerToken = tokens.get(i);
			String text = bannerToken.getText();
			cc.mallet.types.Token token = data.get(i);

			// Add features to token
			if (tokenWeights != null)
			{
				Double weight = tokenWeights.get(text);
				if (weight == null)
					token.setFeatureValue("TOKENWEIGHT=", 1.0);
				else
					token.setFeatureValue("TOKENWEIGHT=", weight);
			}
			data.add(token);
		}
		return carrier;
	}
}
