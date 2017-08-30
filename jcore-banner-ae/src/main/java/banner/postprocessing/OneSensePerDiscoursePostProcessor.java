package banner.postprocessing;

import java.util.HashMap;
import java.util.Map;

import banner.types.Mention;
import banner.types.Sentence;

public class OneSensePerDiscoursePostProcessor implements PostProcessor
{

	public OneSensePerDiscoursePostProcessor()
	{
		// Empty
	}

	public void postProcess(Sentence sentence)
	{
		// The probability of any mentions with the same text receive the higher
		// of the two probabilities
		// TODO Try creating a mention if there is a token sequence which
		// matches an existing mention
		// TODO Figure out how to do this at a range beyond a single sentence
		Map<String, Double> mentionTextProbability = new HashMap<String, Double>();
		for (Mention mention : sentence.getMentions())
		{
			String mentionText = mention.getText();
			Double current = mentionTextProbability.get(mentionText);
			if (current == null || current.doubleValue() < mention.getProbability())
				mentionTextProbability.put(mentionText, mention.getProbability());
		}
		for (Mention mention : sentence.getMentions())
		{
			double probability = mentionTextProbability.get(mention.getText());
			mention.setProbability(probability);
		}
	}
}
