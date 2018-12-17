package banner.tagging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import banner.types.Sentence;
import cc.mallet.fst.CRF;
import cc.mallet.fst.MaxLatticeDefault;
import cc.mallet.fst.SumLatticeDefault;
import cc.mallet.types.Instance;
import cc.mallet.types.Sequence;
import cc.mallet.types.SequencePairAlignment;
import dragon.nlp.tool.Lemmatiser;

public class NBestCRFTagger extends CRFTagger
{
	private int n;
	private boolean useMaxConfidence;

	protected NBestCRFTagger(CRF model, FeatureSet featureSet, int order, int n, boolean useMaxConfidence)
	{
		super(model, featureSet, order);
		this.n = n;
		this.useMaxConfidence = useMaxConfidence;
	}

	public static NBestCRFTagger load(File f, Lemmatiser lemmatiser, dragon.nlp.tool.Tagger posTagger, Tagger preTagger, int n, boolean useMaxConfidence) throws IOException
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(f)));
			CRF model = (CRF) ois.readObject();
			FeatureSet featureSet = (FeatureSet) ois.readObject();
			featureSet.setLemmatiser(lemmatiser);
			featureSet.setPosTagger(posTagger);
			featureSet.setPreTagger(preTagger);
			int order = ois.readInt();
			ois.close();
			return new NBestCRFTagger(model, featureSet, order, n, useMaxConfidence);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void tag(Sentence sentence)
	{
		// System.out.println(sentence.getTokens());
		Instance instance = getInstance(sentence);
		double totalWeight = new SumLatticeDefault(model, (Sequence) instance.getData()).getTotalWeight();
		MaxLatticeDefault maxLattice = new MaxLatticeDefault(model, (Sequence) instance.getData(), null, n);
		// NOTE This is a workaround for the number of sequences being returned being less than what was requested
		int n2 = maxLattice.bestViterbiNodeSequences(n).size();
		List<SequencePairAlignment<Object, Object>> bestOutputAlignments = maxLattice.bestOutputAlignments(n2);
		Map<List<String>, Double> tagConfidenceMap = new HashMap<List<String>, Double>();
		for (SequencePairAlignment<Object, Object> spa : bestOutputAlignments)
		{
			List<String> tagList = getTagList(spa.output());
			double weight = spa.getWeight();
			double confidence = Math.exp(weight - totalWeight);
			// System.out.println(confidence + "\t" + weight + "\t" + tagList);
			if (useMaxConfidence)
			{
				// max(confidence)
				if (!tagConfidenceMap.containsKey(tagList) || tagConfidenceMap.get(tagList).doubleValue() < confidence)
					tagConfidenceMap.put(tagList, new Double(confidence));
			}
			else
			{
				// sum(confidence)
				if (tagConfidenceMap.containsKey(tagList))
				{
					double newConfidence = tagConfidenceMap.get(tagList) + confidence;
					tagConfidenceMap.put(tagList, new Double(newConfidence));
				}
				else
				{
					tagConfidenceMap.put(tagList, new Double(confidence));
				}
			}
		}
		// Find total confidence
		double total = 0.0;
		for (List<String> tagList : tagConfidenceMap.keySet())
		{
			double confidence = tagConfidenceMap.get(tagList).doubleValue();
			// System.out.println(confidence + "\t" + tagList);
			total += confidence;
		}
		// System.out.println("Total: " + total);
		// Normalize confidences by total
		for (List<String> tagList : new HashSet<List<String>>(tagConfidenceMap.keySet()))
		{
			double normConfidence = tagConfidenceMap.get(tagList).doubleValue() / total;
			// System.out.println(normConfidence + "\t" + tagList);
			sentence.addMentions(tagList, normConfidence);
		}
	}
}
