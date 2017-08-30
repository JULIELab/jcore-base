package banner.eval.dataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.HierarchicalConfiguration;

import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.EntityType;
import banner.types.Sentence;
import banner.types.Token;

public abstract class Dataset
{

	protected Tokenizer tokenizer;
	protected Set<Sentence> sentences;

	protected Dataset()
	{
		sentences = new HashSet<Sentence>();
	}

	// TODO This goes away if mentions are character based
	public void setTokenizer(Tokenizer tokenizer)
	{
		this.tokenizer = tokenizer;
	}

	public abstract void load(HierarchicalConfiguration config);

	public abstract List<Dataset> split(int n);

	public Set<Sentence> getSentences()
	{
		return Collections.unmodifiableSet(sentences);
	}

	public Map<String, Integer> getTokenCountTotal()
	{
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for (Sentence sentence : sentences)
		{
			for (Token token : sentence.getTokens())
			{
				String text = token.getText();
				Integer count = counts.get(text);
				if (count == null)
					counts.put(text, 1);
				else
					counts.put(text, count + 1);
			}
		}
		return Collections.unmodifiableMap(counts);
	}

	public Map<String, Integer> getTokenCountMention()
	{
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for (Sentence sentence : sentences)
		{
			for (Mention mention : sentence.getMentions())
			{
				for (Token token : mention.getTokens())
				{
					String text = token.getText();
					Integer count = counts.get(text);
					if (count == null)
						counts.put(text, 1);
					else
						counts.put(text, count + 1);
				}
			}
		}
		return Collections.unmodifiableMap(counts);
	}

	public Map<EntityType, Integer> getTypeCounts()
	{
		Map<EntityType, Integer> typeCounts = new HashMap<EntityType, Integer>();
		for (Sentence sentence : sentences)
		{
			for (Mention mention : sentence.getMentions())
			{
				Integer typeCount = typeCounts.get(mention.getEntityType());
				if (typeCount == null)
					typeCounts.put(mention.getEntityType(), new Integer(1));
				else
					typeCounts.put(mention.getEntityType(), new Integer(typeCount + 1));
			}
		}
		return typeCounts;
	}

	protected static class Tag
	{
		public EntityType type;
		public int start;
		public int end;
		public Set<String> ids;

		public Tag(EntityType type, int start, int end)
		{
			this.type = type;
			this.start = start;
			this.end = end;
			ids = new TreeSet<String>();
		}

		public void addId(String id)
		{
			ids.add(id);
		}

		public Set<String> getIds()
		{
			return Collections.unmodifiableSet(ids);
		}

		public boolean overlaps(Tag tag)
		{
			return start <= tag.end && tag.start <= end;
		}

		public boolean contains(Tag tag)
		{
			return start <= tag.start && end >= tag.end;
		}
	}
}
