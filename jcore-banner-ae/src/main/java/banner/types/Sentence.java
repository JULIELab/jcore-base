/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import banner.tagging.TagFormat;
import banner.tagging.TagPosition;
import banner.tagging.Tagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention.MentionType;

/**
 * This class represents a single sentence, and provides for the text to be tokenized and for mentions.
 * 
 * @author Bob
 */
public class Sentence
{
	private String sentenceId;
	private String documentId;
	private String text;
	private List<Token> tokens;
	private List<Mention> mentions;
	private Map<Mention, EntityIdentification> identifications;

	/**
	 * Creates a new {@link Sentence} with the specified tag and text
	 * 
	 * @param id
	 *        The id for the {@link Sentence}, which may be a unique identifier
	 * @param text
	 *        The text of the sentence
	 */
	public Sentence(String sentenceId, String documentId, String text)
	{
		if (sentenceId == null)
			throw new IllegalArgumentException("sentenceId cannot be null");
		this.sentenceId = sentenceId;
		this.documentId = documentId;
		if (text == null)
			throw new IllegalArgumentException("Text cannot be null");
		text = text.trim();
		if (text.length() == 0)
			throw new IllegalArgumentException("Text must have length greater than 0");
		this.text = text;
		tokens = new ArrayList<Token>();
		mentions = new ArrayList<Mention>();
		identifications = new HashMap<Mention, EntityIdentification>();
	}

	/**
	 * Adds a {@link Token} to this {@link Sentence}. Normally called by instances of {@link Tokenizer}.
	 * 
	 * @param token
	 */
	public void addToken(Token token)
	{
		// Add verification of no token overlap
		if (!token.getSentence().equals(this))
			throw new IllegalArgumentException();
		if (!tokens.contains(token))
			tokens.add(token);
	}

	public int countWhitespace(int index)
	{
		int count = 0;
		index = Math.min(index, text.length());
		for (int i = 0; i < index; i++)
		{
			if (Character.isWhitespace(text.charAt(i)))
				count++;
		}
		return count;
	}

	/**
	 * Adds a {@link Mention} to this Sentence, ignoring any potential overlap with existing {@link Mention}s. Normally called by instance of
	 * {@link Tagger} or post-processors.
	 * 
	 * @param mention
	 */
	public boolean addMention(Mention mention)
	{
		if (!mention.getSentence().equals(this))
			throw new IllegalArgumentException();
		for (Mention previous : mentions)
		{
			if (previous.equals(mention) && mention.getProbability() != null)
			{
				if (previous.getProbability() == null || mention.getProbability() > previous.getProbability())
					previous.setProbability(mention.getProbability());
				return false;
			}
		}
		return mentions.add(mention);
	}

	public boolean removeMention(Mention mention)
	{
		return mentions.remove(mention);
	}

	public void addMentions(List<String> tags, double probability)
	{
		// TODO Add support for OverlapOptions
		// TODO Verify correct transitions & type continuity
		int size = tags.size();
		if (size != tokens.size())
			throw new IllegalArgumentException();
		int startIndex = -1;
		EntityType lastType = null;
		EntityType currentType = null;
		for (int i = 0; i < size; i++)
		{
			String[] split = tags.get(i).split("-");
			TagPosition position = TagPosition.valueOf(split[0]);
			// TODO Verify that the type stays the same
			lastType = currentType;
			if (split.length == 2)
				currentType = EntityType.getType(split[1]);
			if (split.length > 2)
				throw new IllegalArgumentException("Bad tag: " + tags.get(i));
			if (position == TagPosition.O)
			{
				if (startIndex != -1)
				{
					Mention mention = new Mention(this, startIndex, i, lastType, MentionType.Found, probability);
					addMention(mention);
				}
				startIndex = -1;
			}
			else if (position == TagPosition.B)
			{
				if (startIndex != -1)
				{
					Mention mention = new Mention(this, startIndex, i, lastType, MentionType.Found, probability);
					addMention(mention);
				}
				startIndex = i;
			}
			else if (position == TagPosition.W)
			{
				if (startIndex != -1)
				{
					Mention mention = new Mention(this, startIndex, i, lastType, MentionType.Found, probability);
					addMention(mention);
				}
				startIndex = i;
			}
			else
			{
				if (startIndex == -1)
					startIndex = i;
			}
		}
		if (startIndex != -1)
		{
			Mention mention = new Mention(this, startIndex, size, currentType, MentionType.Found, probability);
			addMention(mention);
		}
	}

	/**
	 * @return the tag for the {@link Sentence}
	 */
	public String getSentenceId()
	{
		return sentenceId;
	}

	public String getDocumentId()
	{
		return documentId;
	}

	/**
	 * @return The text of the {@link Sentence}
	 */
	public String getText()
	{
		return text;
	}

	public int getTokenIndex(int charIndex, boolean returnNextIfBoundary)
	{
		// Find:
		// The token with the highest start that is below the given character
		// index
		// The token with the lowest end that is above the given index
		int startToken = -1;
		int endToken = -1;
		for (int i = 0; i < tokens.size(); i++)
		{
			Token token = tokens.get(i);
			if (token.getStart() <= charIndex)
				if (startToken == -1 || tokens.get(startToken).getStart() <= token.getStart())
					startToken = i;
			if (token.getEnd() > charIndex)
				if (endToken == -1 || tokens.get(endToken).getEnd() > token.getEnd())
					endToken = i;
		}
		if (returnNextIfBoundary)
			return startToken;
		else
			return endToken;
	}

	public String getText(int start, int end)
	{
		return text.substring(start, end);
	}

	/**
	 * @return The {@link List} of {@link Token}s for this {@link Sentence}
	 */
	public List<Token> getTokens()
	{
		return Collections.unmodifiableList(tokens);
	}

	/**
	 * @return The {@link List} of {@link Mention}s for this {@link Sentence}. This list may or may not contain overlaps
	 */
	public List<Mention> getMentions()
	{
		return Collections.unmodifiableList(mentions);
	}

	public List<Mention> getMentions(MentionType mentionType)
	{
		List<Mention> mentions2 = new ArrayList<Mention>();
		for (Mention mention : mentions)
			if (mention.getMentionType().equals(mentionType))
				mentions2.add(mention);
		return Collections.unmodifiableList(mentions2);
	}

	public enum OverlapOption
	{
		Exception, Union, Intersection, LayerInsideOut, LayerOutsideIn, LeftToRight, AsSet, Raw;
	}

	public List<String> getTokenLabels(TagFormat format, Set<MentionType> mentionTypes, OverlapOption sameType, OverlapOption differentType)
	{
		List<String> labels = new ArrayList<String>();
		if (sameType.equals(OverlapOption.Exception))
		{
			if (!differentType.equals(OverlapOption.Exception))
				throw new IllegalArgumentException("Not implemented");
			for (int i = 0; i < tokens.size(); i++)
			{
				List<Mention> tokenMentions = getMentions(tokens.get(i), mentionTypes);
				if (tokenMentions.size() == 0)
					labels.add(TagPosition.O.name());
				else if (tokenMentions.size() == 1)
					labels.add(TagPosition.getPositionText(format, tokenMentions.get(0), i));
				else
					throw new IllegalArgumentException("Sentence " + sentenceId + " contains overlapping mentions");
			}
		}
		else if (sameType.equals(OverlapOption.Union))
		{
			if (!differentType.equals(OverlapOption.Exception))
				throw new IllegalArgumentException("Not implemented");
			Sentence union = copy(true, true);
			Set<Mention> handledMentions = new HashSet<Mention>();
			Set<Mention> unhandledMentions = new HashSet<Mention>(union.mentions);
			while (unhandledMentions.size() > 0)
			{
				// Get an unhandled mention
				Mention mention = unhandledMentions.iterator().next();
				handledMentions.add(mention);
				if (mentionTypes.contains(mention.getMentionType()))
				{
					// Get all overlapping mentions with the same entity type
					Set<Mention> overlapping = new HashSet<Mention>();
					for (Mention mention2 : union.mentions)
						if (mention.overlaps(mention2) && mention.getEntityType().equals(mention2.getEntityType()))
							overlapping.add(mention2);
					// Handle overlaps
					if (overlapping.size() > 1)
					{
						// TODO Does not handle probability
						int start = Integer.MAX_VALUE;
						int end = Integer.MIN_VALUE;
						for (Mention mention2 : overlapping)
						{
							start = Math.min(start, mention2.getStart());
							end = Math.max(end, mention2.getEnd());
							union.removeMention(mention2);
						}
						union.addMention(new Mention(union, start, end, mention.getEntityType(), mention.getMentionType()));
					}
				}
				// Get list of unhandled mentions
				unhandledMentions = new HashSet<Mention>(union.getMentions());
				unhandledMentions.removeAll(handledMentions);
			}
			for (int i = 0; i < tokens.size(); i++)
			{
				List<Mention> tokenMentions = union.getMentions(union.tokens.get(i), mentionTypes);
				if (tokenMentions.size() == 0)
					labels.add(TagPosition.O.name());
				else if (tokenMentions.size() == 1)
					labels.add(TagPosition.getPositionText(format, tokenMentions.get(0), i));
				else
					throw new IllegalArgumentException("Sentence " + sentenceId + " contains overlapping mentions");
			}
		}
		else if (sameType.equals(OverlapOption.Raw))
		{
			if (!differentType.equals(OverlapOption.Raw))
				throw new IllegalArgumentException("Not implemented");
			for (int i = 0; i < tokens.size(); i++)
			{
				List<Mention> tokenMentions = getMentions(tokens.get(i), mentionTypes);
				if (tokenMentions.size() == 0)
					labels.add(TagPosition.O.name());
				else if (tokenMentions.size() == 1)
					labels.add(TagPosition.getPositionText(format, tokenMentions.get(0), i));
				else
				{
					StringBuilder label = new StringBuilder();
					Iterator<Mention> mentionIterator = tokenMentions.iterator();
					label.append(mentionIterator.next());
					while (mentionIterator.hasNext())
					{
						label.append("&");
						label.append(mentionIterator.next());
					}
					labels.add(label.toString());
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("Not implemented");
		}
		return Collections.unmodifiableList(labels);
	}

	public List<Mention> getMentions(Token token, Set<MentionType> mentionTypes)
	{
		ArrayList<Mention> mentionsForToken = new ArrayList<Mention>();
		for (Mention mention : mentions)
			if (mentionTypes.contains(mention.getMentionType()) && mention.getTokens().contains(token))
				mentionsForToken.add(mention);
		return Collections.unmodifiableList(mentionsForToken);
	}

	public EntityIdentification setIdentification(Mention mention, EntityIdentification identification)
	{
		if (!mention.getSentence().equals(this))
			throw new IllegalArgumentException();
		return identifications.put(mention, identification);
	}

	public EntityIdentification getIdentification(Mention mention)
	{
		return identifications.get(mention);
	}

	public Sentence copy(boolean includeTokens, boolean includeMentions)
	{
		Sentence sentence2 = new Sentence(sentenceId, documentId, text);
		if (includeTokens)
		{
			for (Token token : tokens)
				sentence2.tokens.add(new Token(sentence2, token.getStart(), token.getEnd()));
		}
		if (includeMentions)
		{
			for (Mention mention : mentions)
				sentence2.mentions.add(mention.copy(sentence2));
		}
		return sentence2;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + sentenceId.hashCode();
		result = PRIME * result + ((documentId == null) ? 0 : documentId.hashCode());
		result = PRIME * result + text.hashCode();
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
		final Sentence other = (Sentence)obj;
		if (!sentenceId.equals(other.sentenceId))
			return false;
		if (documentId == null)
		{
			if (other.documentId != null)
				return false;
		}
		else if (!documentId.equals(other.documentId))
			return false;
		if (!text.equals(other.text))
			return false;
		return true;
	}
}
