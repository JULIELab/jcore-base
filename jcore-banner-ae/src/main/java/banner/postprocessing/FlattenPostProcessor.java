package banner.postprocessing;

import java.util.HashSet;
import java.util.Set;

import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.Mention.MentionType;

public class FlattenPostProcessor implements PostProcessor
{

	private FlattenType flattenType;

	public FlattenPostProcessor(FlattenType flattenType)
	{
		this.flattenType = flattenType;
	}

	public void postProcess(Sentence sentence)
	{
		Set<Mention> handledMentions = new HashSet<Mention>();
		Set<Mention> unhandledMentions = new HashSet<Mention>(sentence.getMentions());
		while (unhandledMentions.size() > 0)
		{
			// Get an unhandled mention
			Mention mention = unhandledMentions.iterator().next();
			handledMentions.add(mention);
			// Get all overlapping mentions
			Set<Mention> overlapping = getOverlappingMentions(sentence, mention);
			// Handle overlaps
			if (overlapping.size() > 1)
				flattenType.handle(sentence, overlapping);
			// Get list of unhandled mentions
			unhandledMentions = new HashSet<Mention>(sentence.getMentions());
			unhandledMentions.removeAll(handledMentions);
		}
	}

	// TODO Fix so only finds overlapping mentions of same entity type & mention type
	private static Set<Mention> getOverlappingMentions(Sentence sentence, Mention mention)
	{
		Set<Mention> overlapping = new HashSet<Mention>();
		for (Mention mention2 : sentence.getMentions())
			if (mention.overlaps(mention2))
				overlapping.add(mention2);
		return overlapping;
	}

	// Handles overlapping mentions by saving the one with the highest mention
	// probability
	// TODO Try union, intersection, longest, shortest
	// TODO Try either adding the probability of mentions combined or ???

	public enum FlattenType
	{
		Union
		{
			@Override
			public void handle(Sentence sentence, Set<Mention> overlapping)
			{
				// TODO Assumes identical entity types, mention types & does not handle probability
				int start = Integer.MAX_VALUE;
				int end = Integer.MIN_VALUE;
				EntityType entityType = null;
				MentionType mentionType = null;
				for (Mention mention : overlapping)
				{
					start = Math.min(start, mention.getStart());
					end = Math.max(end, mention.getEnd());
					entityType = mention.getEntityType();
					mentionType = mention.getMentionType();
					sentence.removeMention(mention);
				}
				sentence.addMention(new Mention(sentence, start, end, entityType, mentionType));
			}
		},
		HighestProbability
		{
			@Override
			public void handle(Sentence sentence, Set<Mention> overlapping)
			{
				Mention bestMention = null;
				for (Mention mention : overlapping)
					if (bestMention == null || mention.getProbability() > bestMention.getProbability())
						bestMention = mention;
				for (Mention mention : overlapping)
					if (mention != bestMention)
						sentence.removeMention(mention);
			}
		};

		public abstract void handle(Sentence sentence, Set<Mention> overlapping);
	}
}
