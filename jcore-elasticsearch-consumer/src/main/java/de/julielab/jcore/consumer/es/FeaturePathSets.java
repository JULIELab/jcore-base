package de.julielab.jcore.consumer.es;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is just a list of {@link FeaturePathSet} instances with a few helper
 * methods. Each single FeaturePathSet refers to a single UIMA type and defines
 * which values to derive from its annotations. This class bundles multiple of
 * such UIMA type references, thus being the top-level declaration of which
 * values to extract from which annotations.
 * 
 * @author faessler
 *
 */
public class FeaturePathSets extends ArrayList<FeaturePathSet> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6466780556071050906L;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((featurePathSetsByUimaType == null) ? 0 : featurePathSetsByUimaType.hashCode());
		result = prime * result
				+ ((featurePathSetsByUimaTypeIndexId == null) ? 0 : featurePathSetsByUimaTypeIndexId.hashCode());
		result = prime * result + ((uimaTypes == null) ? 0 : uimaTypes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeaturePathSets other = (FeaturePathSets) obj;
		if (featurePathSetsByUimaType == null) {
			if (other.featurePathSetsByUimaType != null)
				return false;
		} else if (!featurePathSetsByUimaType.equals(other.featurePathSetsByUimaType))
			return false;
		if (featurePathSetsByUimaTypeIndexId == null) {
			if (other.featurePathSetsByUimaTypeIndexId != null)
				return false;
		} else if (!featurePathSetsByUimaTypeIndexId.equals(other.featurePathSetsByUimaTypeIndexId))
			return false;
		if (uimaTypes == null) {
			if (other.uimaTypes != null)
				return false;
		} else if (!uimaTypes.equals(other.uimaTypes))
			return false;
		return true;
	}

	private Multimap<Integer, FeaturePathSet> featurePathSetsByUimaTypeIndexId;
	/**
	 * Only filled if necessary on-the-fly (requires the type system and, thus, a
	 * JCas)
	 */
	private Multimap<Type, FeaturePathSet> featurePathSetsByUimaType;
	private List<Integer> uimaTypes;

	public FeaturePathSets(FeaturePathSet... sets) {
		this();
		for (int i = 0; i < sets.length; i++) {
			FeaturePathSet featurePathSet = sets[i];
			add(featurePathSet);
		}
	}

	public FeaturePathSets() {
		this.featurePathSetsByUimaTypeIndexId = LinkedHashMultimap.create();
		// We use LinkedHashMaps instead of plain hash maps to keep the order in
		// which the annotation indexes are converted to tokens intact. This is
		// probably not really important but it keeps the results stable and
		// predictable.
		this.featurePathSetsByUimaType = LinkedHashMultimap.create();
		this.uimaTypes = new ArrayList<>();
	}

	public Multimap<Integer, FeaturePathSet> getFeaturePathSetsByUimaType() {
		return featurePathSetsByUimaTypeIndexId;
	}

	public List<Integer> getUimaTypes() {
		return uimaTypes;
	}

	@Override
	public boolean add(FeaturePathSet e) {
		featurePathSetsByUimaTypeIndexId.put(e.getUimaType(), e);
		uimaTypes.add(e.getUimaType());
		return super.add(e);
	}

	/**
	 * Returns those feature path sets that refer to the given UIMA type.
	 * 
	 * @param type
	 *            A UIMA type constant. Those are accessible via the
	 *            <code>type</code> field of each UIMA annotation type class.
	 * @return The FeaturePathSets that extract values from annotations of the given
	 *         type.
	 */
	public Collection<FeaturePathSet> getFeaturePathSetsForType(int type) {
		return featurePathSetsByUimaTypeIndexId.get(type);
	}

	/**
	 * <p>
	 * Returns all FeaturePathSets referring to direct or indirect super types of
	 * <code>type</code>. This allows to define field values for a UIMA type higher
	 * in the type system hierarchy and have them applied to all their children.
	 * </p>
	 * <p>
	 * Note that no feature sets referring directly to <code>type</code> will be
	 * returned. For this purpose, use {@link #getFeaturePathSetsForType(int)};
	 * </p>
	 * 
	 * 
	 * @param type
	 *            The base type of which all feature sets will be tested whether
	 *            they refer to a super type of it.
	 * @param aJCas
	 *            The JCas to derive values from.
	 * @return All FeaturePathSets referring to a super type of <code>type</code>.
	 */
	public Collection<FeaturePathSet> findFeaturePathSetsForSupertypes(Type type, JCas aJCas) {
		Collection<FeaturePathSet> sets = featurePathSetsByUimaType.get(type);
		if (null == sets || sets.isEmpty()) {
			// we will now add the FeatureSet for the supertype of
			// 'type'. Hence, we iterate through the index IDs, get their CAS
			// types, check if we found the supertype, and if so, add its
			// associated FeaturePath to 'type'
			TypeSystem ts = aJCas.getTypeSystem();
			for (Integer indexId : uimaTypes) {
				Type featureType = aJCas.getCasType(indexId);
				if (ts.subsumes(featureType, type)) {
					featurePathSetsByUimaType.putAll(type, featurePathSetsByUimaTypeIndexId.get(indexId));
				}
			}
			sets = featurePathSetsByUimaType.get(type);
		}
		return sets;
	}

}
