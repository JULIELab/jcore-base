/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.types;

import java.util.List;

import banner.tagging.Tagger;

/**
 * Instances of this class represent the mention of an entity within a {@link Sentence}. Mentions are defined in terms of full tokens, and therefore
 * finding mentions (the job of a {@link Tagger}) requires tokenization first.
 * 
 * @author Bob
 */
public class Mention implements Comparable<Mention>
{

	private Sentence sentence;
	private int start;
	private int end;
	private EntityType entityType;
	private MentionType mentionType;
	private Double probability;

	public Mention(Sentence sentence, int start, int end, EntityType entityType, MentionType mentionType)
	{
		this(sentence, start, end, entityType, mentionType, null);
	}

	public Mention(Sentence sentence, int start, int end, EntityType entityType, MentionType mentionType, Double probability)
	{
		if (sentence == null)
			throw new IllegalArgumentException();
		this.sentence = sentence;
		if (start < 0 || start >= sentence.getTokens().size())
			throw new IllegalArgumentException();
		this.start = start;
		if (end <= 0 || end > sentence.getTokens().size())
			throw new IllegalArgumentException();
		this.end = end;
		if (length() <= 0)
			throw new IllegalArgumentException("Illegal length - start: " + start + " end: " + end);
		if (entityType == null)
			throw new IllegalArgumentException();
		this.entityType = entityType;
		if (mentionType == null)
			throw new IllegalArgumentException();
		this.mentionType = mentionType;
		setProbability(probability);
	}

	/**
	 * @return A {@link EntityType} indicating the type of entity being mentioned
	 */
	public EntityType getEntityType()
	{
		return entityType;
	}

	public MentionType getMentionType()
	{
		return mentionType;
	}

	/**
	 * @return The {@link Sentence} containing this {@link Mention}
	 */
	public Sentence getSentence()
	{
		return sentence;
	}

	/**
	 * @return The {@link Token}s which comprise this {@link Mention}
	 */
	public List<Token> getTokens()
	{
		return sentence.getTokens().subList(start, end);
	}

	/**
	 * @return The number of tokens this {@link Mention} contains
	 */
	public int length()
	{
		return end - start;
	}

	/**
	 * @return The original text of this {@link Mention}
	 */
	public String getText()
	{
		return sentence.getText(getStartChar(), getEndChar());
	}

	public boolean contains(int tokenIndex)
	{
		return tokenIndex >= start && tokenIndex < end;
	}

	/**
	 * @return The index of the last token in this {@link Mention}
	 */
	public int getEnd()
	{
		return end;
	}

	/**
	 * @return The index of the first token in this {@link Mention}
	 */
	public int getStart()
	{
		return start;
	}

	public int getStartChar()
	{
		return sentence.getTokens().get(start).getStart(false);
	}

	public int getEndChar()
	{
		return sentence.getTokens().get(end - 1).getEnd(false);
	}

	public int getStartChar(boolean ignoreWhitespace)
	{
		return sentence.getTokens().get(start).getStart(ignoreWhitespace);
	}

	public int getEndChar(boolean ignoreWhitespace)
	{
		return sentence.getTokens().get(end - 1).getEnd(ignoreWhitespace);
	}

	/**
	 * Determines whether this {@link Mention} overlaps the specified {@link Mention}
	 * 
	 * @param mention2
	 * @return <code>true</code> if this Mention overlaps with the specified {@link Mention}, <code>false</code> otherwise
	 */
	public boolean overlaps(Mention mention2)
	{
		return sentence.equals(mention2.sentence) && end > mention2.start && start < mention2.end;
	}

	public Double getProbability()
	{
		return probability;
	}

	public void setProbability(Double probability)
	{
		if (mentionType.equals(MentionType.Found))
		{
			if (probability != null)
			{
				if (probability <= 0.0)
					throw new IllegalArgumentException("Probability must be greater than 0.0: " + probability);
				if (probability > 1.0)
				{
					// FIXME Fix rounding error
					if (probability < 1.000001)
						probability = 1.0;
					else
						throw new IllegalArgumentException("Probability may not exceed 1.0: " + probability);
				}
			}
		}
		else
		{
			if (probability != null)
				throw new IllegalArgumentException();
		}
		this.probability = probability;
	}

	public Mention copy(Sentence sentence2)
	{
		return new Mention(sentence2, start, end, entityType, mentionType, probability);
	}

	// ----- Object overrides -----

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder();
		result.append("Mention text=\"" + getText());
		result.append("\" start=" + start);
		result.append(" end=" + end);
		result.append(" type=" + entityType.getText());
		result.append(" prob=" + probability);
		return result.toString();
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + end;
		result = PRIME * result + sentence.hashCode();
		result = PRIME * result + start;
		result = PRIME * result + entityType.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Mention other = (Mention)obj;
		if (!sentence.equals(other.sentence))
			return false;
		if (!entityType.equals(other.entityType))
			return false;
		if (start != other.start)
			return false;
		if (end != other.end)
			return false;
		return true;
	}

	public int compareTo(Mention mention2)
	{
		Integer compare = start - mention2.start;
		if (compare != 0)
			return compare;
		compare = end - mention2.end;
		if (compare != 0)
			return compare;
		return entityType.getText().compareTo(mention2.entityType.getText());
	}

	public enum MentionType
	{
		Found, Required, Allowed;
	}
}
