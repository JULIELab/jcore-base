/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.pipe;

import java.util.List;
import java.util.Set;

import banner.tagging.TagFormat;
import banner.types.Sentence;
import banner.types.Token;
import banner.types.Mention.MentionType;
import banner.types.Sentence.OverlapOption;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.TokenSequence;

/**
 * This class is used by the CRFTagger as the base for the feature set.
 * 
 * @author Bob
 */
public class Sentence2TokenSequence extends Pipe
{
	private static final long serialVersionUID = 1L;

	private TagFormat format;
	private Set<MentionType> mentionTypes;
	private OverlapOption sameType;
	private OverlapOption differentType;

	public Sentence2TokenSequence(TagFormat format, Set<MentionType> mentionTypes, OverlapOption sameType, OverlapOption differentType)
	{
		super(null, new LabelAlphabet());
		this.format = format;
		this.mentionTypes = mentionTypes;
		this.sameType = sameType;
		this.differentType = differentType;
	}

	@Override
	public Instance pipe(Instance carrier)
	{
		Sentence sentence = (Sentence) carrier.getData();
		List<Token> tokens = sentence.getTokens();
		int size = tokens.size();
		TokenSequence data = new TokenSequence(size);
		LabelSequence target = new LabelSequence((LabelAlphabet) getTargetAlphabet(), size);
		List<String> labels = sentence.getTokenLabels(format, mentionTypes, sameType, differentType);
		for (int i = 0; i < size; i++)
		{
			String text = tokens.get(i).getText();
			data.add(new cc.mallet.types.Token(text));
			target.add(labels.get(i));
		}
		carrier.setData(data);
		carrier.setTarget(target);
		carrier.setSource(sentence);
		return carrier;
	}
}
