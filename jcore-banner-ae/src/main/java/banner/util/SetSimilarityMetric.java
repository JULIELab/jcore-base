/**
 * 
 */
package banner.util;

public enum SetSimilarityMetric
{
	Dice
	{
		protected double similarityInternal(int intersection, int size1, int size2)
		{
			return 2.0 * intersection / (size1 + size2);
		}
	},
	BooleanXJaccard
	{
		protected double similarityInternal(int intersection, int size1, int size2)
		{
			if (size1 > intersection)
				return 0.0;
			return (double)intersection / (size1 + size2 - intersection);
		}
	},
	Jaccard
	{
		protected double similarityInternal(int intersection, int size1, int size2)
		{
			return (double)intersection / (size1 + size2 - intersection);
		}
	},
	Overlap
	{
		protected double similarityInternal(int intersection, int size1, int size2)
		{
			return (double)intersection / Math.min(size1, size2);
		}
	},
	// TODO Double-check definition of cosine
	CosineTheta
	{
		protected double similarityInternal(int intersection, int size1, int size2)
		{
			return (double)(intersection * intersection) / (size1 * size2);
		}
	};

	public double similarity(int intersection, int size1, int size2)
	{
		if (intersection > Math.min(size1, size2))
		{
			throw new IllegalArgumentException("Illegal arguments - intersection:" + intersection + ", size1:" + size1 + ", size2:" + size2);
		}
		double similarity = similarityInternal(intersection, size1, size2);
		assert similarity > 0.0;
		// Fix rounding error
		if (similarity > 1.0 && similarity < 1.000001)
			similarity = 1.0;
		assert similarity <= 1.0;
		return similarity;
	}

	protected abstract double similarityInternal(int intersection, int size1, int size2);
}