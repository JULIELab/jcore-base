package banner.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionsRand
{

	private CollectionsRand()
	{
		// Uninstantiable
	}

	public static <E> boolean isIndependent(Collection<E> collection1, Collection<E> collection2)
	{
		Set<E> set1 = new HashSet<E>(collection1);
		set1.retainAll(collection2);
		return set1.isEmpty();
	}

	public static <E> boolean isSubset(Collection<E> collection1, Collection<E> collection2)
	{
		Set<E> set1 = new HashSet<E>(collection1);
		set1.removeAll(collection2);
		return set1.isEmpty();
	}

	public static <E> Set<E> randomSubset(Collection<E> collection, int subsetSize)
	{
		List<E> list = new ArrayList<E>(collection);
		Collections.shuffle(list);
		return new HashSet<E>(list.subList(0, subsetSize));
	}

	public static <E> Set<E> randomSubset(Collection<E> collection, double subsetPercentage)
	{
		if (subsetPercentage <= 0.0)
			throw new IllegalArgumentException("Percentage must be greater than 0.0");
		if (subsetPercentage > 1.0)
			throw new IllegalArgumentException("Percentage may not be greater than 1.0");
		int subsetSize = (int) Math.floor(collection.size() * subsetPercentage + 0.5);
		return randomSubset(collection, subsetSize);
	}

	public static <E> List<Set<E>> randomSplit(Collection<E> set, int numSplits)
	{
		List<E> list = new ArrayList<E>(set);
		Collections.shuffle(list);
		List<Set<E>> splits = new ArrayList<Set<E>>(numSplits);
		for (int i = 0; i < numSplits; i++)
			splits.add(new HashSet<E>());
		for (int i = 0; i < list.size(); i++)
			splits.get(i % numSplits).add(list.get(i));
		return splits;
	}

}
