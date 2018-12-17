package de.julielab.jcore.consumer.es;

import java.util.List;

import de.julielab.jcore.consumer.es.filter.Filter;

/**
 * This class is a declaration of the all the values that should be derived from
 * the UIMA annotations of a specific type for document field token creation.
 * For example, one could generate values (tokens) for the covered text and for
 * the ID of all organization entities, or the part of speech and the lemma of
 * each UIMA token.
 * 
 * @author faessler
 *
 */
public class FeaturePathSet {

	private Filter filter;
	private List<String> featurePaths;
	private int uimaType;
	private String concatenationString;

	public FeaturePathSet(int type, List<String> featurePaths, String concatenationString, Filter filter) {
		this.uimaType = type;
		this.featurePaths = featurePaths;
		this.concatenationString = concatenationString;
		this.filter = filter;
	}

	public FeaturePathSet(int type, List<String> featurePaths) {
		this(type, featurePaths, null, null);
	}

	/**
	 * If set, all values extracted from annotations by feature paths of this
	 * feature path set will concatenated using the given string to create Tokens.
	 * 
	 * @return The feature value concatenation string.
	 */
	public String getConcatenationString() {
		return concatenationString;
	}

	@Override
	public String toString() {
		return "FeaturePathSet [filter=" + filter + ", featurePaths=" + featurePaths + ", uimaType=" + uimaType
				+ ", concatenationString=" + concatenationString + "]";
	}

	/**
	 * Identifies the UIMA type for which this feature set should be used.
	 * 
	 * @return The UIMA type index constant this feature path set refers to.
	 */
	public int getUimaType() {
		return uimaType;
	}

	/**
	 * 
	 * @return The filter or filter chain that should be applied to all values
	 *         derived from an annotation for all the feature paths in this set.
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * 
	 * @return The feature paths to apply to the annotations of the referred types.
	 */
	public List<String> getFeaturePaths() {
		return featurePaths;
	}
}
