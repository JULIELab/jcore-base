package banner.postprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Sentence;

public class OneSensePerDocument
{

	private Tokenizer tokenizer;
	private boolean createNew;

	public OneSensePerDocument(Tokenizer tokenizer, boolean createNew)
	{
		this.tokenizer = tokenizer;
		this.createNew = createNew;
	}

	public void process(List<Sentence> sentences)
	{
		// The probability of any mentions with the same text receive the higher
		// of the two probabilities
		Map<String, Double> mentionTextProbability = new HashMap<String, Double>();
		Map<String, Set<EntityType>> mentionTextTypes = new HashMap<String, Set<EntityType>>();
		for (Sentence sentence : sentences)
		{
			if (sentence != null)
			{
				for (Mention mention : sentence.getMentions())
				{
					String mentionText = mention.getText();
					Double current = mentionTextProbability.get(mentionText);
					if (current == null || current.doubleValue() < mention.getProbability())
						mentionTextProbability.put(mentionText, mention.getProbability());
					Set<EntityType> entityTypes = mentionTextTypes.get(mentionText);
					if (entityTypes == null)
					{
						entityTypes = new HashSet<EntityType>(1);
						mentionTextTypes.put(mentionText, entityTypes);
					}
					entityTypes.add(mention.getEntityType());
				}
			}
		}
		DictionaryTagger dictionary = null;
		if (createNew)
		{
			dictionary = new DictionaryTagger();
			dictionary.setTokenizer(tokenizer);
			for (String mentionText : mentionTextTypes.keySet())
				dictionary.add(mentionText, mentionTextTypes.get(mentionText));
		}
		for (Sentence sentence : sentences)
		{
			if (sentence != null)
			{
				if (createNew)
					dictionary.tag(sentence);
				for (Mention mention : new ArrayList<Mention>(sentence.getMentions()))
				{
					Double probability = mentionTextProbability.get(mention.getText());
					// Probability might be null if the dictionary returned a
					// sequence not previously seen
					if (probability == null)
						sentence.removeMention(mention);
					else
						mention.setProbability(probability);
				}
			}
		}
	}
}
