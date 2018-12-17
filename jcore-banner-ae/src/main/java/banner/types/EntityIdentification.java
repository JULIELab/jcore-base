package banner.types;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class EntityIdentification
{
	private Mention mention;
	private Map<EntityName, Double> nameValue;

	public EntityIdentification(Mention mention)
	{
		this.mention = mention;
		nameValue = new HashMap<EntityName, Double>();
	}

	public Double set(EntityName name, double value)
	{
		return nameValue.put(name, value);
	}

	public Mention getMention()
	{
		return mention;
	}

	public Set<EntityName> getNames()
	{
		Set<EntityName> sortedNames = new TreeSet<EntityName>(new Comparator<EntityName>()
		{
			public int compare(EntityName name1, EntityName name2)
			{
				if (name1.equals(name2))
					return 0;
				Double value1 = nameValue.get(name1);
				Double value2 = nameValue.get(name2);
				// Descending order of value
				int comparison = value2.compareTo(value1);
				if (comparison != 0)
					return comparison;
				// Ascending order of name
				comparison = name1.getName().compareTo(name2.getName());
				if (comparison != 0)
					return comparison;
				// Break ties arbitrarily but consistently
				return name1.hashCode() - name2.hashCode();
			}
		});
		sortedNames.addAll(nameValue.keySet());
		return Collections.unmodifiableSet(sortedNames);
	}

	public Double getValue(EntityName name)
	{
		return nameValue.get(name);
	}

	public Double getBestValue()
	{
		Double bestValue = null;
		for (EntityName name : nameValue.keySet())
		{
			Double value = nameValue.get(name);
			if (bestValue == null || bestValue.doubleValue() < value)
				bestValue = value;
		}
		return bestValue;
	}

	public Set<Entity> getBestEntities()
	{
		Double bestValue = getBestValue();
		Set<Entity> bestEntities = new HashSet<Entity>();
		for (EntityName name : nameValue.keySet())
			if (bestValue.equals(nameValue.get(name)))
				bestEntities.add(name.getEntity());
		return bestEntities;
	}

	public EntityIdentification copy(Mention mention2)
	{
		EntityIdentification identification2 = new EntityIdentification(mention2);
		for (EntityName name : nameValue.keySet())
			identification2.nameValue.put(name, nameValue.get(name));
		return identification2;
	}

}
