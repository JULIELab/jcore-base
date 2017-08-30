package banner.util;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IndexedMetricSetSimilarity<E, V>
{
	private SetSimilarityMetric metric;
	private int resultsSize;
	private Index<E, Integer> elementToIndex;
	private List<V> indexToValue;
	private TIntArrayList indexToSize;

	public IndexedMetricSetSimilarity(SetSimilarityMetric metric, int resultsSize)
	{
		this.metric = metric;
		this.resultsSize = resultsSize;
		elementToIndex = new Index<E, Integer>();
		indexToValue = new ArrayList<V>();
		indexToSize = new TIntArrayList();
	}

	public void addValue(Collection<E> elements, V value)
	{
		assert indexToValue.size() == indexToSize.size();
		Integer index = new Integer(indexToValue.size());
		indexToValue.add(value);
		int size = 0;
		for (E element : elements)
		{
			E transform = transform(element);
			if (transform != null)
			{
				elementToIndex.add(transform, index);
				size++;
			}
		}
		indexToSize.add(size);
	}

	public RankedList<V> indexMatch(Collection<E> lookupElements)
	{
		int[] counts = new int[indexToValue.size()];
		Set<E> transformedLookupElementSet = new HashSet<E>();
		for (E element : lookupElements)
		{
			E transform = transform(element);
			if (transform != null)
				transformedLookupElementSet.add(transform);
		}
		for (E element : transformedLookupElementSet)
		{
			Set<Integer> indexSet = elementToIndex.lookup(element);
			if (indexSet != null)
				for (Integer index : indexSet)
					counts[index.intValue()]++;
		}
		int lookupSize = transformedLookupElementSet.size();
		RankedList<V> results = new RankedList<V>(resultsSize);
		for (int i = 0; i < counts.length; i++)
		{
			if (counts[i] > 0)
			{
				V value = indexToValue.get(i);
				int size = indexToSize.get(i);
				double similarity = metric.similarity(counts[i], lookupSize, size);
				if (similarity > 0.0)
					results.add(similarity, value);
			}
		}
		return results;
	}

	/**
	 * All elements are transformed prior to use for storage or lookup. Default implementation does nothing
	 * 
	 * @param element
	 * @return
	 */
	protected E transform(E element)
	{
		return element;
	}

}
