package banner.eval.dataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;

import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.Mention.MentionType;

public class AZDCDataset extends Dataset
{

	public AZDCDataset(Tokenizer tokenizer)
	{
		super();
		this.tokenizer = tokenizer;
	}

	public AZDCDataset()
	{
		super();
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		// TODO Refactor this to work in one pass
		HierarchicalConfiguration localConfig = config.configurationAt(this.getClass().getPackage().getName());
		String sentenceFilename = localConfig.getString("sentenceFilename");
		String mentionsFilename = localConfig.getString("mentionTestFilename");
		try
		{
			BufferedReader mentionTestFile = new BufferedReader(new FileReader(mentionsFilename));
			HashMap<String, LinkedList<Tag>> tags = getTags(mentionTestFile);
			mentionTestFile.close();

			// Determine ambiguity and polysemy
			Map<String, Set<String>> nameToId = new HashMap<String, Set<String>>();
			Map<String, Set<String>> idToName = new HashMap<String, Set<String>>();

			BufferedReader sentenceFile = new BufferedReader(new FileReader(sentenceFilename));
			String line = sentenceFile.readLine();
			// Get past header
			line = sentenceFile.readLine();
			while (line != null)
			{
				String[] split = line.split("\\t");
				String id = split[0] + "-" + split[1] + "-" + split[2];
				String sentenceText = split[3];
				Sentence sentence = getSentence(id, split[1], sentenceText, tokenizer, tags, nameToId, idToName);
				sentences.add(sentence);
				line = sentenceFile.readLine();
			}
			sentenceFile.close();

			int[] idCountForName = new int[20];
			for (String name : nameToId.keySet())
			{
				idCountForName[nameToId.get(name).size()]++;
				if (nameToId.get(name).size() > 2)
				{
					System.out.println("Name " + name + " has " + nameToId.get(name).size() + " IDs: " + nameToId.get(name));
				}
			}
			System.out.print("idCountForName: [");
			for (int i = 0; i < idCountForName.length; i++)
				System.out.print(idCountForName[i] + ", ");
			System.out.println("]");
			int[] nameCountForId = new int[25];
			for (String id : idToName.keySet())
			{
				if (!id.equals("[]"))
				{
					nameCountForId[idToName.get(id).size()]++;
					if (idToName.get(id).size() > 5)
						System.out.println("ID " + id + " has " + idToName.get(id).size() + " names: " + idToName.get(id));
				}
			}
			System.out.print("nameCountForId: [");
			for (int i = 0; i < nameCountForId.length; i++)
				System.out.print(nameCountForId[i] + ", ");
			System.out.println("]");
			System.out.println("Number of names without annotations: " + idToName.get("[]").size());
			System.out.println("Number of ids represented: " + idToName.size());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected HashMap<String, LinkedList<Tag>> getTags(BufferedReader tagFile) throws IOException
	{
		HashMap<String, LinkedList<Tag>> tags = new HashMap<String, LinkedList<Tag>>();

		String line = tagFile.readLine();
		// Get past header
		line = tagFile.readLine();
		int i = 0;
		while (line != null)
		{
			// String[] split = line.split("\\s|\\|");
			String[] split = line.split("\\t");

			// System.out.println(Arrays.asList(split));
			boolean valid = split.length >= 6;
			valid = valid && split[4].length() > 0;
			valid = valid && split[5].length() > 0;
			valid = valid && split[4].matches("\\d+");
			valid = valid && split[5].matches("\\d+");
			valid = valid && Integer.parseInt(split[5]) > Integer.parseInt(split[4]);

			if (valid)
			{
				String uniqueId = split[0] + "-" + split[1] + "-" + split[2];
				LinkedList<Tag> tagList = tags.get(uniqueId);

				if (tagList == null)
					tagList = new LinkedList<Tag>();

				Tag tag = new Tag(EntityType.getType("DISE"), Integer.parseInt(split[4]), Integer.parseInt(split[5]));
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
					if (split.length >= 10)
					{
						String[] split2 = split[9].split("[\\s-,\\[\\]]+");
						// System.out.println(Arrays.asList(split2));
						for (String id : split2)
							if (id.length() > 0)
								tag.addId(id);
					}
					tagList.add(tag);
					tags.put(uniqueId, tagList);
				}
			}
			/*
			else
			{
				// Print out invalid mentions not listed as "no annotation"
				if (split.length < 9 || !split[8].toLowerCase().startsWith("no annotation"))
				{
					System.out.println("Invalid mention: " + Arrays.asList(split));
				}
			}
			*/
			line = tagFile.readLine();
			i++;
		}
		return tags;
	}

	protected Sentence getSentence(String sentenceId, String documentId, String sentenceText, Tokenizer tokenizer, HashMap<String, LinkedList<Tag>> tags,
			Map<String, Set<String>> nameToId, Map<String, Set<String>> idToName)
	{

		Sentence sentence = new Sentence(sentenceId, documentId, sentenceText);
		tokenizer.tokenize(sentence);
		LinkedList<Tag> tagList = tags.get(sentenceId);
		if (tagList != null)
			for (Tag tag : tagList)
			{
				// System.out.println(sentenceId + " : " + sentenceText + " (" + sentenceText.length() + ")");
				int tagstart = tag.start - 1;

				int tagend = tag.end;
				// System.out.println(tagstart + " - " + tagend + " |" + sentenceText.substring(tagstart, tagend) +
				// "|");
				// System.out.println(sentence.getTokens().size());

				int start = sentence.getTokenIndex(tagstart, true);
				assert start >= 0;
				int end = sentence.getTokenIndex(tagend, false);
				// List<Token> tokens = sentence.getTokens();
				// System.out.println(start + " - " + end);
				// System.out.println(tokens.get(start).getText() + " - " + tokens.get(end).getText());

				assert end > start;
				Mention mention = new Mention(sentence, start, end, tag.type, MentionType.Required);

				// System.out.println(start + " - " + end + " |" + mention.getText() + "|");
				sentence.addMention(mention);

				String name = mention.getText().toLowerCase();
				String tagId = tag.getIds().toString();
				// System.out.println("*** ADDING: " + name + " --> " + tagId + " ***");
				Set<String> idsForName = nameToId.get(name);
				if (idsForName == null)
				{
					idsForName = new HashSet<String>();
					nameToId.put(name, idsForName);
				}
				idsForName.add(tagId);
				Set<String> namesForId = idToName.get(tagId);
				if (namesForId == null)
				{
					namesForId = new HashSet<String>();
					idToName.put(tagId, namesForId);
				}
				namesForId.add(name);
			}
		return sentence;
	}

	public List<Dataset> split(int n)
	{
		List<Dataset> splitDatasets = new ArrayList<Dataset>();
		for (int i = 0; i < n; i++)
			splitDatasets.add(new AZDCDataset(tokenizer));

		List<Set<String>> splitAbstractIds = new ArrayList<Set<String>>();
		for (int i = 0; i < n; i++)
			splitAbstractIds.add(new HashSet<String>());

		Random r = new Random();
		for (Sentence sentence : sentences)
		{
			String abstractId = sentence.getDocumentId();
			int num = -1;
			for (int i = 0; i < n && num == -1; i++)
				if (splitAbstractIds.get(i).contains(abstractId))
					num = i;
			if (num == -1)
				num = r.nextInt(n);
			splitDatasets.get(num).sentences.add(sentence);
			splitAbstractIds.get(num).add(abstractId);
		}
		return splitDatasets;
	}
}
