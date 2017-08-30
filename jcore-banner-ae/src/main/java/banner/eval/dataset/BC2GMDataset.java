/**
 * 
 */
package banner.eval.dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.HierarchicalConfiguration;

import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.EntityType;
import banner.types.Sentence;
import banner.types.Token;
import banner.types.Mention.MentionType;

/**
 * @author bob
 */
public class BC2GMDataset extends Dataset
{
	public BC2GMDataset(Tokenizer tokenizer)
	{
		super();
		this.tokenizer = tokenizer;
	}

	public BC2GMDataset()
	{
		super();
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		HierarchicalConfiguration localConfig = config.configurationAt(this.getClass().getPackage().getName());
		String sentenceFilename = localConfig.getString("sentenceFilename");
		String mentionsFilename = localConfig.getString("mentionTestFilename");
		String alternateMentionsFilename = localConfig.getString("mentionAlternateFilename");
		load(sentenceFilename, mentionsFilename, alternateMentionsFilename);
	}

	public void load(String sentenceFilename, String mentionsFilename, String alternateMentionsFilename)
	{
		try
		{
			BufferedReader mentionTestFile = new BufferedReader(new FileReader(mentionsFilename));
			HashMap<String, LinkedList<Tag>> tags = getTags(mentionTestFile);
			mentionTestFile.close();
			HashMap<String, LinkedList<Tag>> alternateTags = null;
			if (alternateMentionsFilename != null)
			{
				BufferedReader mentionAlternateFile = new BufferedReader(new FileReader(alternateMentionsFilename));
				alternateTags = new HashMap<String, LinkedList<Tag>>(getAlternateTags(mentionAlternateFile));
				mentionAlternateFile.close();
			}

			Pattern ws = Pattern.compile("\\s+");
			BufferedReader sentenceFile = new BufferedReader(new FileReader(sentenceFilename));
			String line = sentenceFile.readLine();
			while (line != null)
			{
				Matcher matcher = ws.matcher(line);
				matcher.find();
				String id = line.substring(0, matcher.start()).trim();
				String sentenceText = line.substring(matcher.end()).trim();
				Sentence sentence = getSentence(id, sentenceText, tokenizer, tags);
				if (alternateTags != null)
					addAlternateMentions(sentence, alternateTags);
				sentences.add(sentence);
				line = sentenceFile.readLine();
			}
			sentenceFile.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected HashMap<String, LinkedList<Tag>> getTags(BufferedReader tagFile) throws IOException
	{
		EntityType type = EntityType.getType("GENE");
		HashMap<String, LinkedList<Tag>> tags = new HashMap<String, LinkedList<Tag>>();

		String line = tagFile.readLine();
		while (line != null)
		{
			String[] split = line.split("\\s|\\|");
			LinkedList<Tag> tagList = tags.get(split[0]);
			if (tagList == null)
				tagList = new LinkedList<Tag>();
			Tag tag = new Tag(type, Integer.parseInt(split[1]), Integer.parseInt(split[2]));
			Iterator<Tag> tagIterator = tagList.iterator();
			boolean add = true;
			while (tagIterator.hasNext() && add)
			{
				Tag tag2 = tagIterator.next();
				// FIXME Determine what to do for when A.contains(B) or
				// B.contains(A)
				if (tag.contains(tag2))
					tagIterator.remove();
				// add = false;
				else if (tag2.contains(tag))
					add = false;
				// tagIterator.remove();
				// else
				// assert !tag.overlaps(tag2);
			}
			if (add)
			{
				tagList.add(tag);
				tags.put(split[0], tagList);
			}
			line = tagFile.readLine();
		}
		return tags;
	}

	protected HashMap<String, LinkedList<Tag>> getAlternateTags(BufferedReader tagFile) throws IOException
	{
		HashMap<String, LinkedList<Tag>> tags = new HashMap<String, LinkedList<Tag>>();

		String line = tagFile.readLine();
		while (line != null)
		{
			String[] split = line.split(" |\\|");
			LinkedList<Tag> tagList = tags.get(split[0]);
			if (tagList == null)
				tagList = new LinkedList<Tag>();
			EntityType type = EntityType.getType("GENE");
			Tag tag = new Tag(type, Integer.parseInt(split[1]), Integer.parseInt(split[2]));
			tagList.add(tag);
			tags.put(split[0], tagList);
			line = tagFile.readLine();
		}
		return tags;
	}

	protected Sentence getSentence(String id, String sentenceText, Tokenizer tokenizer, HashMap<String, LinkedList<Tag>> tags)
	{
		Sentence sentence = new Sentence(id, null, sentenceText);
		tokenizer.tokenize(sentence);
		List<Token> tokens = sentence.getTokens();
		LinkedList<Tag> tagList = tags.get(id);
		if (tagList != null)
			for (Tag tag : tagList)
			{
				int start = getTokenIndex(tokens, tag.start);
				assert start >= 0;
				int end = getTokenIndex(tokens, tag.end);
				assert end >= start;
				sentence.addMention(new Mention(sentence, start, end + 1, tag.type, MentionType.Required));
			}
		return sentence;
	}

	protected void addAlternateMentions(Sentence sentence, HashMap<String, LinkedList<Tag>> tags)
	{
		List<Token> tokens = sentence.getTokens();
		LinkedList<Tag> tagList = tags.get(sentence.getSentenceId());
		if (tagList != null)
			for (Tag tag : tagList)
			{
				int start = getTokenIndex(tokens, tag.start);
				assert start >= 0;
				int end = getTokenIndex(tokens, tag.end);
				assert end >= start;
				sentence.addMention(new Mention(sentence, start, end + 1, tag.type, MentionType.Allowed));
			}
	}

	protected static int getTokenIndex(List<Token> tokens, int index)
	{
		int chars = 0;
		for (int i = 0; i < tokens.size(); i++)
		{
			int length = tokens.get(i).getText().length();
			if (index >= chars && index <= chars + length - 1)
				return i;
			chars += length;
		}
		return -1;
	}

	@Override
	public List<Dataset> split(int n)
	{
		List<Dataset> splitDatasets = new ArrayList<Dataset>();
		for (int i = 0; i < n; i++)
		{
			BC2GMDataset dataset = new BC2GMDataset(tokenizer);
			splitDatasets.add(dataset);
		}

		Random r = new Random();
		for (Sentence sentence : sentences)
		{
			int num = r.nextInt(n);
			splitDatasets.get(num).sentences.add(sentence);
		}
		return splitDatasets;
	}
}
