/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.pipe;

import java.util.EnumSet;
import java.util.Set;

import banner.tagging.Tagger;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.Mention.MentionType;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

/**
 * This class is used by the CRFTagger as the base for the feature set.
 * 
 * @author Bob
 */
public class Pretagger extends Pipe
{
	// TODO Is there a way to store the Tagger (with data!) in the model as part of the stream?

	private static final long serialVersionUID = 1L;

	private String prefix;
	private transient Tagger preTagger;

	public Pretagger(String prefix, Tagger preTagger)
	{
		this.prefix = prefix;
		this.preTagger = preTagger;
	}

	public void setPreTagger(Tagger preTagger)
	{
		this.preTagger = preTagger;
	}

	@Override
	public Instance pipe(Instance carrier)
	{
		Sentence sentence = (Sentence) carrier.getSource();
		Sentence preSentence = sentence.copy(true, false);
		preTagger.tag(preSentence);
		TokenSequence ts = (TokenSequence) carrier.getData();
		Set<MentionType> mentionType = EnumSet.of(MentionType.Found);
		for (int i = 0; i < ts.size(); i++)
		{
			Token token = ts.get(i);
			// Add features to token
			for (Mention mention : preSentence.getMentions(preSentence.getTokens().get(i), mentionType))
			{
				String featureName = prefix + mention.getEntityType().getText();
				token.setFeatureValue(featureName, 1);
			}
		}
		return carrier;
	}
}
