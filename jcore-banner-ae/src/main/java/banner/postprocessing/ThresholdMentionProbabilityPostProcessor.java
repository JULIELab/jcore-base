package banner.postprocessing;

import java.util.ArrayList;

import banner.types.Mention;
import banner.types.Sentence;

public class ThresholdMentionProbabilityPostProcessor implements PostProcessor
{
	private double probabilityThreshold;

	public ThresholdMentionProbabilityPostProcessor(double probabilityThreshold)
	{
		this.probabilityThreshold = probabilityThreshold;
	}

	public void postProcess(Sentence sentence)
	{
		for (Mention mention : new ArrayList<Mention>(sentence.getMentions()))
		{
			double probability = mention.getProbability();
			if (probability < probabilityThreshold)
				sentence.removeMention(mention);
		}
	}
}
