package banner.normalization;

import banner.types.Entity;
import banner.types.EntityIdentification;
import banner.types.EntityName;
import banner.types.Mention;
import banner.types.Sentence;
import banner.util.IndexedMetricSetSimilarity;
import banner.util.RankedList;
import banner.util.SetSimilarityMetric;

public class IndexedMentionIdentifier implements MentionIdentifier
{

	private IndexedMetricSetSimilarity<String, EntityName> identifier;

	public IndexedMentionIdentifier(SetSimilarityMetric metric, int resultsSize)
	{
		identifier = new IndexedMetricSetSimilarity<String, EntityName>(metric, resultsSize)
		{
			@Override
			protected String transform(String element)
			{
				// TODO Figure out how to make this configurable
				return element.toLowerCase();
			}
		};
	}

	@Override
	public void addEntity(Entity entity)
	{
		for (EntityName name : entity.getNames())
		{
			identifier.addValue(name.getElements(), name);
		}
	}

	@Override
	public void identifyMentions(Sentence sentence)
	{
		for (Mention mention : sentence.getMentions())
		{
			EntityIdentification identification = new EntityIdentification(mention);
			EntityName mentionName = EntityName.createFromTokens(mention.getText(), mention.getTokens());
			RankedList<EntityName> matches = identifier.indexMatch(mentionName.getElements());
			for (int i = 0; i < matches.size(); i++)
				identification.set(matches.getObject(i), matches.getValue(i));
			sentence.setIdentification(mention, identification);
		}
	}
}
