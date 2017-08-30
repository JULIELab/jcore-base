/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.types;

/**
 * Represents a single token of text. Note that the token does not know what Mention it is a part of, if any.
 * 
 * @author Bob
 */
public class Token implements Comparable<Token>
{

	private Sentence sentence;
	private int start;
	private int end;

	/**
	 * The token is from character start to character end - 1
	 * 
	 * @param sentence
	 * @param start
	 * @param end
	 */
	public Token(Sentence sentence, int start, int end)
	{
		if (sentence == null)
			throw new IllegalArgumentException();
		if (start < 0)
			throw new IllegalArgumentException("Start may not be less than 0: " + start);
		this.sentence = sentence;
		this.start = start;
		this.end = end;
		if (length() < 1)
			throw new IllegalArgumentException("End must be greater than start; start: " + start + " end: " + end);
	}

	/**
	 * @return The {@link Sentence} for this {@link Token}
	 */
	public Sentence getSentence()
	{
		return sentence;
	}

	/**
	 * @return The text for this token
	 */
	public String getText()
	{
		return sentence.getText(start, end);
	}

	/**
	 * @return The start index for this token, inclusive
	 */
	public int getStart()
	{
		return getStart(false);
	}

	/**
	 * @return The end index for this token, exclusive
	 */
	public int getEnd()
	{
		return getEnd(false);
	}

	/**
	 * @return The start index for this token, inclusive
	 */
	public int getStart(boolean ignoreWhitespace)
	{
		int value = start;
		if (ignoreWhitespace)
			value -= sentence.countWhitespace(start);
		return value;
	}

	/**
	 * @return The end index for this token, exclusive
	 */
	public int getEnd(boolean ignoreWhitespace)
	{
		int value = end;
		if (ignoreWhitespace)
			value -= sentence.countWhitespace(end) - 1;
		return value;
	}

	public boolean contains(int index)
	{
		return index >= start && index < end;
	}

	/**
	 * @return The number of characters in this token
	 */
	public int length()
	{
		return end - start;
	}

	// ----- Object overrides -----

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + sentence.hashCode();
		result = PRIME * result + start;
		result = PRIME * result + end;
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
		final Token other = (Token)obj;
		if (!sentence.equals(other.sentence))
			return false;
		if (start != other.start)
			return false;
		if (end != other.end)
			return false;
		return true;
	}

	public int compareTo(Token token2)
	{
		Integer compare = token2.start - start;
		if (compare != 0)
			return compare;
		return token2.end - end;
	}

	@Override
	public String toString()
	{
		return sentence.getText(start, end);
	}

}
