package banner.normalization;

import java.util.ArrayList;
import java.util.List;

import banner.types.EntityIdentification;
import banner.types.EntityName;
import banner.types.Mention;
import banner.types.Sentence;

public class BestMatchDisambiguator implements MentionDisambiguator
{

	private double threshold;

	public BestMatchDisambiguator(double threshold)
	{
		this.threshold = threshold;
	}

	public void disambiguateMentions(Sentence sentence)
	{
		List<Mention> mentions = new ArrayList<Mention>(sentence.getMentions());
		for (Mention mention : mentions)
		{
			if (!disambiguate(mention))
				sentence.removeMention(mention);
		}
	}

	public boolean disambiguate(Mention mention)
	{
		Sentence sentence = mention.getSentence();
		EntityIdentification identification = sentence.getIdentification(mention);
		if (identification == null)
			return false;
		Double bestValue = identification.getBestValue();
		if (bestValue == null || bestValue <= threshold)
			return false;
		EntityIdentification disambiguated = new EntityIdentification(mention);
		for (EntityName name : identification.getNames())
		{
			Double value = identification.getValue(name);
			if (value >= bestValue)
				disambiguated.set(name, value);
		}
		sentence.setIdentification(mention, disambiguated);
		return true;
	}
}
