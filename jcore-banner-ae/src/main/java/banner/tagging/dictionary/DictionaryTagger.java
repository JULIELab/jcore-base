/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;

import banner.tagging.Tagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.EntityType;
import banner.types.Sentence;
import banner.types.Token;
import banner.types.Mention.MentionType;
import banner.util.Trie;

/**
 * This class represents a very simple dictionary-based tagger. All text subsequences which match an entry will be tagged, without regard to the
 * context.
 * 
 * @author Bob
 */
public class DictionaryTagger implements Tagger
{
	// TODO Add ability to do fuzzy / prefix / suffix searches
	// TODO Add ability to associate text with both a type and an identifier

	private Tokenizer tokenizer;
	private boolean filterContainedMentions;
	protected Trie<String, Set<EntityType>> entities;
	protected Trie<String, Boolean> notInclude;
	private boolean normalizeMixedCase;
	private boolean normalizeDigits;
	private boolean generate2PartVariations;
	private boolean dropEndParentheticals;

	/**
	 * Creates a new {@link DictionaryTagger}
	 */
	public DictionaryTagger()
	{
		entities = new Trie<String, Set<EntityType>>();
		notInclude = new Trie<String, Boolean>();
	}

	// TODO Determine how to combine this with loading
	public void configure(HierarchicalConfiguration config, Tokenizer tokenizer)
	{
		HierarchicalConfiguration localConfig = config.configurationAt(this.getClass().getName());
		filterContainedMentions = localConfig.getBoolean("filterContainedMentions", false);
		normalizeMixedCase = localConfig.getBoolean("normalizeMixedCase", false);
		normalizeDigits = localConfig.getBoolean("normalizeDigits", false);
		generate2PartVariations = localConfig.getBoolean("generate2PartVariations", false);
		dropEndParentheticals = localConfig.getBoolean("dropEndParentheticals", false);
		this.tokenizer = tokenizer;
	}

	public void load(HierarchicalConfiguration config) throws IOException
	{
		HierarchicalConfiguration localConfig = config.configurationAt(this.getClass().getName());
		String dictionaryFilename = "/dict/single.txt";
		if (dictionaryFilename == null)
			throw new IllegalArgumentException("Must specify dictionary filename");
		String dictionaryTypeName = localConfig.getString("dictionaryType");
		if (dictionaryTypeName == null)
			throw new IllegalArgumentException("Must specify dictionary type");
		String delimiter = localConfig.getString("delimiter");
		int column = localConfig.getInt("column", -1);
		if (delimiter != null && column == -1)
			throw new IllegalArgumentException("Must specify column if delimiter specified");
		EntityType dictionaryType = EntityType.getType(dictionaryTypeName);

		// Load data
		 java.util.Scanner s = new java.util.Scanner(getClass().getResourceAsStream(dictionaryFilename)).useDelimiter("\\A");
		
		while (s.hasNext())
		{	String line = s.nextLine();
			line = line.trim();
			if (line.length() > 0)
			{
				if (delimiter == null)
				{
					add(line, dictionaryType);
				}
				else
				{
					// TODO Performance - don't use split
					String[] split = line.split(delimiter);
					add(split[column], dictionaryType);
				}
			}
		}
		s.close();
	}

	protected List<String> process(String input)
	{
		if (input == null)
			throw new IllegalArgumentException();
		List<String> tokens = tokenizer.getTokens(input);
		for (int i = 0; i < tokens.size(); i++)
			tokens.set(i, transform(tokens.get(i)));
		return tokens;
	}

	protected String transform(String str)
	{
		// This has been optimized for very fast operation
		String result = str;
		if (normalizeMixedCase || normalizeDigits)
		{
			char[] chars = str.toCharArray();
			if (normalizeMixedCase)
			{
				boolean hasUpper = false;
				boolean hasLower = false;
				for (int i = 0; i < chars.length && (!hasUpper || !hasLower); i++)
				{
					hasUpper |= Character.isUpperCase(chars[i]);
					hasLower |= Character.isLowerCase(chars[i]);
				}
				if (hasUpper && hasLower)
					for (int i = 0; i < chars.length; i++)
						chars[i] = Character.toLowerCase(chars[i]);
			}
			// Note that this only works on single digits
			if (normalizeDigits)
				for (int i = 0; i < chars.length; i++)
					if (Character.isDigit(chars[i]))
						chars[i] = '0';
			result = new String(chars);
		}
		return result;
	}

	/**
	 * Adds a single entry to the dictionary. The text is processed by the tokenizer and the resulting tokens are stored.
	 * 
	 * @param text
	 *        The text to find
	 * @param type
	 *        The {@link EntityType} to tag the text with
	 */
	public void add(String text, EntityType type)
	{
		add(text, Collections.singleton(type));
	}

	public void add(String text, Collection<EntityType> types)
	{
		// TODO Make configurable
		// if (text.length() == 1)
		// return;
		// TODO Add ability to not add items over N (eg 10) tokens long
		List<String> tokens = process(text);
		add(tokens, types);
		if (generate2PartVariations)
		{
			if (tokens.size() == 1 && tokens.get(0).matches("[A-Za-z]+[0-9]+"))
			{
				int split = 0;
				String token = tokens.get(0);
				while (Character.isLetter(token.charAt(split)))
					split++;
				add2Part(token.substring(0, split), token.substring(split, token.length()), types);
			}
			if (tokens.size() == 2)
			{
				add2Part(tokens.get(0), tokens.get(1), types);
			}
			if (tokens.size() == 3 && (tokens.get(1).equals("-") || tokens.get(1).equals("/")))
			{
				add2Part(tokens.get(0), tokens.get(2), types);
			}
		}
		// TODO These lines add GENE recall but drop precision
		// if (tokens.size() > 1 && tokens.get(tokens.size() -
		// 1).equals("homolog"))
		// add(tokens.subList(0, tokens.size() - 1), types);
	}

	private void add2Part(String part1, String part2, Collection<EntityType> types)
	{
		List<String> tokens = new ArrayList<String>();
		tokens.add(part1 + part2);
		tokens.add(part2);
		add(tokens, types);
		tokens = new ArrayList<String>();
		tokens.add(part1);
		tokens.add(part2);
		add(tokens, types);
		tokens.add(1, "-");
		add(tokens, types);
		tokens.set(1, "/");
		add(tokens, types);
	}

	public boolean add(List<String> tokens, Collection<EntityType> types)
	{
		if (tokens.size() == 0)
			throw new IllegalArgumentException("Number of tokens must be greater than zero");
		// Verify that the sequence to be added is not listed as not included
		Boolean value = notInclude.getValue(tokens);
		if (value != null)
			return false;
		// If configured, drop parenthetical phrases at the end of the sequence
		if (dropEndParentheticals && tokens.get(tokens.size() - 1).equals(")"))
		{
			int openParen = tokens.size() - 1;
			while (openParen > 0 && !tokens.get(openParen).equals("("))
				openParen--;
			if (openParen <= 0)
				return false;
			tokens = tokens.subList(0, openParen);
		}
		Set<EntityType> currentTypes = entities.getValue(tokens);
		if (currentTypes == null)
		{
			currentTypes = new HashSet<EntityType>(1);
			entities.add(tokens, currentTypes);
		}
		return currentTypes.addAll(types);
	}

	public void tag(Sentence sentence)
	{
		List<Token> tokens = sentence.getTokens();
		// Lookup mentions
		List<Mention> mentions = new LinkedList<Mention>();
		for (int startIndex = 0; startIndex < tokens.size(); startIndex++)
		{
			Trie<String, Set<EntityType>> t = entities;
			for (int currentIndex = startIndex; currentIndex < tokens.size() && t != null; currentIndex++)
			{
				Set<EntityType> entityTypes = t.getValue();
				if (entityTypes != null)
					for (EntityType entityType : entityTypes)
						mentions.add(new Mention(sentence, startIndex, currentIndex, entityType, MentionType.Found));
				Token currentToken = tokens.get(currentIndex);
				t = t.getChild(transform(currentToken.getText()));
			}
		}

		// Add mentions found

		// Iterator<Mention> mentionIterator = mentions.iterator();
		// while (mentionIterator.hasNext())
		// {
		// Mention mention = mentionIterator.next();
		// boolean contained = false;
		// for (Mention mention2 : mentions)
		// contained |= !mention2.equals(mention) && mention2.contains(mention);
		// if (!filterContainedMentions || !contained)
		// sentence.addMention(mention);
		// }

		if (filterContainedMentions)
		{
			while (!mentions.isEmpty())
			{
				Mention mention1 = mentions.remove(0);
				int start = mention1.getStart();
				int end = mention1.getEnd();
				ArrayList<Mention> adjacentMentions = new ArrayList<Mention>();
				Iterator<Mention> mentionIterator = mentions.iterator();
				boolean changed = true;
				while (changed)
				{
					changed = false;
					while (mentionIterator.hasNext())
					{
						Mention mention2 = mentionIterator.next();
						boolean adjacent = (end >= mention2.getStart()) && (start <= mention2.getEnd());
						if (mention1.getEntityType().equals(mention2.getEntityType()) && adjacent)
						{
							adjacentMentions.add(mention2);
							mentionIterator.remove();
							start = Math.min(start, mention2.getStart());
							end = Math.max(end, mention2.getEnd());
							changed = true;
						}
					}
				}
				sentence.addMention(new Mention(sentence, start, end, mention1.getEntityType(), MentionType.Found));
			}
		}
		else
		{
			for (Mention mention : mentions)
				sentence.addMention(mention);
		}

		// System.out.println(sentence.getText());
		// for (Mention mention : sentence.getMentions())
		// System.out.println("\t" + mention.getText());
	}

	public void suppress(String text)
	{
		notInclude.add(process(text), Boolean.TRUE);
	}

	/**
	 * @return The number of entries in this dictionary
	 */
	public int size()
	{
		// TODO PERFORMANCE This is a very intensive operation due to having to
		// search the entire tree!
		return entities.size();
	}

	public Tokenizer getTokenizer()
	{
		return tokenizer;
	}

	public void setTokenizer(Tokenizer tokenizer)
	{
		this.tokenizer = tokenizer;
	}

	public boolean isFilterContainedMentions()
	{
		return filterContainedMentions;
	}

	public void setFilterContainedMentions(boolean filterContainedMentions)
	{
		this.filterContainedMentions = filterContainedMentions;
	}

	public boolean isNormalizeMixedCase()
	{
		return normalizeMixedCase;
	}

	public void setNormalizeMixedCase(boolean normalizeMixedCase)
	{
		this.normalizeMixedCase = normalizeMixedCase;
	}

	public boolean isNormalizeDigits()
	{
		return normalizeDigits;
	}

	public void setNormalizeDigits(boolean normalizeDigits)
	{
		this.normalizeDigits = normalizeDigits;
	}

	public boolean isGenerate2PartVariations()
	{
		return generate2PartVariations;
	}

	public void setGenerate2PartVariations(boolean generate2PartVariations)
	{
		this.generate2PartVariations = generate2PartVariations;
	}

	public boolean isDropEndParentheticals()
	{
		return dropEndParentheticals;
	}

	public void setDropEndParentheticals(boolean dropEndParentheticals)
	{
		this.dropEndParentheticals = dropEndParentheticals;
	}

}
