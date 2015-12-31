package de.julielab.jcore.utility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeClass;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;

/**
 * <<<<<<< HEAD A simple implementation of a feature path, originally adapted to
 * the needs of the EntityEvaluatorConverter. It is currently not supposed to
 * serve as a full UIMA FeaturePath replacement. However, it is thinkable that
 * the implementation will be extended in the future by whomever requires a
 * feature path more capable of what the original UIMA implementation can do.
 * 
 * UIMA discloses the {@link FeatureValuePath} which seems to be able to handle
 * arrays. However, it is marked deprecated and shouldn't be used any more. The
 * new interface to use is the {@link FeaturePath} interface. Problem is, it
 * seemingly can't handle arrays very well.
 * 
 * The current implementation is quite preliminary. It can only be used via
 * {@link #getValueAsString(FeatureStructure)}, i.e. even numeric types are
 * returned as strings. The workflow is as follows: ======= A simple
 * implementation of a feature path, originally adapted to the needs of the
 * EntityEvaluatorConverter. It is currently not supposed to serve as a full
 * UIMA FeaturePath replacement. However, it is thinkable that the
 * implementation will be extended in the future by whomever requires a feature
 * path more capable of what the original UIMA implementation can do.
 * 
 * UIMA discloses the {@link FeatureValuePath} which seems to be able to handle
 * arrays. However, it is marked deprecated and shouldn't be used any more. The
 * new interface to use is the {@link FeaturePath} interface. Problem is, it
 * seemingly can't handle arrays very well.
 * 
 * The current implementation is quite preliminary. It can only be used via
 * {@link #getValueAsString(FeatureStructure)}, i.e. even numeric types are
 * returned as strings. The workflow is as follows: >>>>>>>
 * 7ad7536ab9d5d42fc932c6811c7ffbe15d509c29
 * 
 * <pre>
 * JulesFeaturePath fp = new JulesFeaturePath();
 * fp.initialize(&quot;/resourceEntryList[0]/entryId&quot;);
 * String entryId = fp.getValueAsString(gene);
 * </pre>
 * 
 * <<<<<<< HEAD This example retrieves the entry ID of the first
 * {@link ResourceEntry} of a {@link Gene} annotation.
 * 
 * <p>
 * Feature paths can be any sequence of feature base names in the form
 * <code>/feature1/feature2/feature3</code>. The last feature must be
 * primitive-valued. If any of the features is an array, the index must be
 * given, e.g. <code>/feature1/feature2[2]/feature3</code>. Otherwise, an array
 * index out of bounds (-1) exception will be thrown. ======= This example
 * retrieves the entry ID of the first {@link ResourceEntry} of a {@link Gene}
 * annotation.
 * 
 * <p>
 * Feature paths can be any sequence of feature base names in the form
 * <code>/feature1/feature2/feature3</code>. The last feature must be
 * primitive-valued. If any of the features is an array, the index must be
 * given, e.g. <code>/feature1/feature2[2]/feature3</code>. Otherwise, an array
 * index out of bounds (-1) exception will be thrown. >>>>>>>
 * 7ad7536ab9d5d42fc932c6811c7ffbe15d509c29
 * </p>
 * 
 * @author faessler
 * 
 */
@SuppressWarnings("deprecation")
public class JCoReFeaturePath implements FeaturePath {

	private String[] featurePath;
	private String[] featureBaseNames;
	/**
	 * The sequence of features given in the feature path. For example, the
	 * feature path <tt>/argument[0]/resourceEntryList[1]/entryId</tt> would
	 * contain three features <tt>argument</tt>, <tt>resourceEntryList</tt>
	 * and </tt>entryId</tt>.
	 */
	private Feature[] features;
	/**
	 * Feature candidates for features not actually present on the base type of
	 * a feature but on one or more subtypes of the feature type. This list is
	 * currently not really used since it seems to be faster and easier to just
	 * get the correct feature from the concrete feature structure at hand at
	 * evaluation time than to iterate through all candidates and check which
	 * one is the right. We still need to create the list though because we need
	 * to know whether there are none, exactly one or multiple possibilities for
	 * a feature name.
	 */
	private List<Set<Feature>> featureCandidates;
	/**
	 * For array-valued features, this array contains which element of such an
	 * array feature should be used for the evaluation of the feature path
	 */
	private int[] arrayIndexes;
	private Type currentType;
	private Matcher arrayIndexMatcher = Pattern.compile("\\[-?[0-9]+\\]").matcher("");
	private String featurePathString;
	private Map<Class<?>, Method> getterMap;
	private Map<Class<?>, Method> setterMap;
	private Map<?, ?> replacements;
	/**
	 * A set containing those FeatureStructures that already had their feature
	 * values replaced to which this FeaturePath is pointing to. This set has to
	 * be cleared by calling {@link #clearReplacementCache()} after finishing
	 * the processing of a CAS.
	 */
	private Set<FeatureStructure> alreadyReplaced;
	private boolean replaceUnmappedValues;
	private Object defaultReplacementValue;
	/**
	 * Can be set to true in {@link #initialize(String)} and is set to false
	 * after a successful call to {@link #typeInit(Type)}. Required in
	 * {@link #typeInit(Type)} to determine whether the feature path should be
	 * (re-)determined on the current type.
	 */
	private boolean featurePathChanged;
	private String builtInFunction;

	private final static Logger log = LoggerFactory.getLogger(JCoReFeaturePath.class);

	@Override
	public int size() {
		if (null != features)
			return features.length;
		return 0;
	}

	@Override
	public Feature getFeature(int i) {
		if (null != features)
			return features[i];
		return null;
	}

	@Override
	public void addFeature(Feature feat) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initialize(String featurePath) throws CASException {
		log.debug("Initializing with feature path \"{}\".", featurePath);
		this.featurePathChanged = null == featurePathString || !featurePathString.equals(featurePath);
		this.featurePathString = featurePath;
		if (!featurePathString.startsWith("/"))
			featurePathString = "/" + featurePathString;
		this.featurePath = featurePath.trim().substring(1).split("/");
		log.debug("Initializing feature path with these path elements: {}", Arrays.toString(this.featurePath));
		String[] builtInFunctionSplit = this.featurePath[this.featurePath.length - 1].split(":");
		if (builtInFunctionSplit.length > 1) {
			this.featurePath[this.featurePath.length - 1] = builtInFunctionSplit[0];
			this.builtInFunction = builtInFunctionSplit[1];
			log.debug("Found in-built function {} to apply on the feature structure pointed to by the feature path.",
					builtInFunction);
		}
		this.getterMap = new HashMap<>();
		this.setterMap = new HashMap<>();
		this.alreadyReplaced = new HashSet<>();
		this.replaceUnmappedValues = false;

	}

	public void initialize(String featurePath, Map<?, ?> replacements) throws CASException {
		initialize(featurePath);
		this.replacements = replacements;
	}

	@Override
	public void typeInit(Type featurePathType) throws CASException {
		if (featurePathChanged || null == currentType || !currentType.equals(featurePathType)) {
			log.debug("Initializing internal structure for feature path {} on type {}.", featurePathString,
					featurePathType.getName());
			Type currentFeatureType = featurePathType;
			features = new Feature[this.featurePath.length];
			featureBaseNames = new String[this.featurePath.length];
			arrayIndexes = new int[this.featurePath.length];
			featureCandidates = new ArrayList<>(this.featurePath.length);
			for (int i = 0; i < this.featurePath.length; i++) {
				String featureName = this.featurePath[i];
				arrayIndexes[i] = Integer.MIN_VALUE;
				featureCandidates.add(null);
				arrayIndexMatcher.reset(featureName);
				try {
					if (arrayIndexMatcher.find()) {
						String indexSpecification = arrayIndexMatcher.group();
						String indexString = indexSpecification.substring(1, indexSpecification.length() - 1);
						arrayIndexes[i] = Integer.parseInt(indexString);
						if (arrayIndexes[i] == Integer.MIN_VALUE)
							throw new IllegalArgumentException("The negative array index " + arrayIndexes[i]
									+ " is not allowed because Integer.MIN_VALUE is used to identify non-specified array indexes.");
						featureName = featureName.substring(0, featureName.length() - indexSpecification.length());
						log.debug("Identified array index {} for feature {}.", indexString, featureName);
					}
					featureBaseNames[i] = featureName;
					Feature feature = currentFeatureType.getFeatureByBaseName(featureName);
					if (null == feature) {
						log.debug(
								"Feature \"{}\" is not defined for type \"{}\". It is checked whether the feature is defined on one or more subtypes.",
								featureName, currentFeatureType);
						TypeSystem typeSystem = ((TypeImpl) featurePathType).getTypeSystem();
						// We get all possible features only to check whether
						// there is a single - and thus canonical -
						// feature, none or even multiple candidates.
						Set<Feature> featuresOfSubtypes = searchFeatureInSubtypes(featureName, currentFeatureType,
								typeSystem);
						if (featuresOfSubtypes.size() == 1)
							feature = featuresOfSubtypes.iterator().next();
						else
							featureCandidates.set(i, featuresOfSubtypes);
						if (null == feature && featuresOfSubtypes.isEmpty())
							throw new CASException(CASException.UNDEFINED_FEATURE,
									new Object[] { featureName, currentFeatureType });
					}
					if (null != feature) {
						features[i] = feature;
						if (feature.getRange().isPrimitive()) {
							log.trace("Feature {} identified as primitive-valued.", featureName);
							currentFeatureType = feature.getRange();
						} else if (feature.getRange().isArray()) {
							log.trace("Feature {} identified as array-valued.", featureName);
							currentFeatureType = feature.getRange().getComponentType();
						} else {
							log.trace("Feature {} identified as FeatureStructure-valued.", featureName);
							currentFeatureType = feature.getRange();
						}
						log.debug("Determined type \"{}\" for feature \"{}\".", currentFeatureType, featureName);
					}
				} catch (Exception e) {
					log.error(
							"Error happened while initializing feature path \"{}\" on type \"{}\". Path element index: {} (\"{}\").",
							new Object[] { this.featurePathString, featurePathType, i, featureName });
					throw e;
				}
			}
			currentType = featurePathType;
			featurePathChanged = false;
		}
	}

	private Set<Feature> searchFeatureInSubtypes(String featureName, Type type, TypeSystem typeSystem) {
		Set<Feature> returnFeatures = new HashSet<>();
		List<Type> subtypes = typeSystem.getDirectSubtypes(type);
		if (0 == subtypes.size())
			return Collections.emptySet();
		for (Type subtype : subtypes) {
			Feature foundFeature = subtype.getFeatureByBaseName(featureName);
			if (null != foundFeature) {
				log.debug("Determined feature \"{}\" to actually belong to subtype \"{}\" (and possible others).",
						featureName, type);
				returnFeatures.add(foundFeature);
			}
			returnFeatures.addAll(searchFeatureInSubtypes(featureName, subtype, typeSystem));
		}
		return returnFeatures;
	}

	/**
	 * When using this method, please note the comment at
	 * {@link #clearReplacementCache()}.
	 * 
	 * @param fs
	 * @return
	 */
	public Object replaceValue(FeatureStructure fs) {
		return getValue(fs, 0, replacements);
	}

	/**
	 * Begins to traverse the feature path from position
	 * <tt>startFeatureIndex</tt> and returns whatever values lie at the end of
	 * the feature path. This includes <tt>FeatureStructures</tt>, primitive
	 * values or even array-typed values.
	 * 
	 * @param fs
	 * @param startFeatureIndex
	 * @return
	 */
	public Object getValue(FeatureStructure fs, int startFeatureIndex) {
		return getValue(fs, startFeatureIndex, null);
	}

	private Object getValue(FeatureStructure fs, int startFeatureIndex, Map<?, ?> replacements) {
		if (fs == null)
			throw new IllegalArgumentException("Passed FeatureStructure may not be null but it is.");

		Object featureValue = null;

		try {
			// Only initialize onto a new type if we begin to traverse the
			// feature path from the beginning. If we only
			// traverse a tail of the feature path, it is a recursive call for
			// the current type.
			if (startFeatureIndex == 0)
				typeInit(fs.getType());
			FeatureStructure currentFs = fs;
			for (int i = startFeatureIndex; i < features.length; i++) {
				Feature currentFeature = features[i];
				log.trace("Now traversing feature {} on type {}, beginning at position {} of the feature path.",
						new Object[] { currentFeature, fs.getType(), startFeatureIndex });
				// If the feature is null this means, there are multiple
				// candidates for the feature OR there were
				// multiple candidates before and thus we don't know what comes
				// next. Just get the
				// correct one from the current feature structure.
				if (null == currentFeature)
					currentFeature = currentFs.getType().getFeatureByBaseName(featureBaseNames[i]);
				if (null == currentFeature)
					throw new CASException(CASException.UNDEFINED_FEATURE,
							new Object[] { featureBaseNames[i], currentFs.getType() });
				if (currentFeature.getRange().isPrimitive()) {
					if (i < features.length - 1) {
						log.warn(
								"The value of the feature \"{}\" is primitive. However, the feature path \"{}\" has not yet come to an end. The current feature value is returned, the rest of the feature path is ignored.");
						break;
					}
					featureValue = getFeatureValueFromFeatureStructure(currentFs, currentFeature, replacements);
					log.trace("Feature {} identified as primitive-valued. The value is: \"{}\"", currentFeature,
							featureValue);
				} else if (currentFeature.getRange().isArray()) {
					log.trace("Feature {} identified as array-valued.", currentFeature);
					FeatureStructure array = currentFs.getFeatureValue(currentFeature);
					if (null == array)
						// Nothing to get for this fs
						break;
					Class<? extends FeatureStructure> arrayClass = array.getClass();
					int index = arrayIndexes[i];

					if (arrayClass.equals(FSArray.class)) {
						log.trace("Value of feature  {} is a {}.", currentFeature, arrayClass.getSimpleName());
						FSArray fsArray = (FSArray) array;
						if (index >= fsArray.size()) {
							log.trace(
									"Array index is {} which is greater than the array has entries. Nothing is returned for this feature structure.",
									index);
							// Nothing to get for this fs
							break;
						} else if (index == Integer.MIN_VALUE) {
							log.trace(
									"No particular index to access has been given. Returning values for all elements in the array.");
							// No index given on an array feature; thus we
							// collect all values in the array.
							List<Object> valueList = new ArrayList<>();
							for (int j = 0; j < fsArray.size(); j++) {
								FeatureStructure element = fsArray.get(j);
								if (null == element) {
									log.trace("Element at position {} was null, skipping this element.", j);
									continue;
								}
								if (i == features.length - 1) {
									// End of the feature path
									valueList.add(element);
									log.trace(
											"Retrieved value \"{}\" for element at position {}. This is the end of the feature path.",
											element, j);
								} else {
									// The feature path continues, thus we now
									// have to recursively collect the feature
									// path values for all array elements for
									// the remaining features in the feature
									// path.
									Object elementValue = getValue(element, i + 1, replacements);
									log.trace("Retrieved value \"{}\" for element no {}.", elementValue, j);
									// The elementValue could be of any data
									// type. For now we just want to know
									// whether
									// it is a List - i.e. the remainder of the
									// feature path also contained array-valued
									// features - or a single value.
									if (null != elementValue && elementValue.getClass() == ArrayList.class) {
										log.trace("Adding values to result list.");
										List<?> remainderPathValues = (ArrayList<?>) elementValue;
										valueList.addAll(remainderPathValues);
									} else {
										log.trace("Adding value to result list.");
										valueList.add(elementValue);
									}
								}
							}
							featureValue = valueList;
							// We must not continue here because then the loop
							// would continue to the next feature - but
							// we already have got all values through the
							// recursion.
							break;
						} else { // A valid index is given.
							int effectiveIndex = index;
							if (effectiveIndex < 0)
								effectiveIndex = fsArray.size() + index;
							if (effectiveIndex < 0 || effectiveIndex >= fsArray.size()) {
								log.trace("Array index {} is out of bounds for array found for feature, returning null",
										effectiveIndex, currentFeature);
								return null;
							}
							FeatureStructure arrayElement = fsArray.get(effectiveIndex);
							if (i < features.length - 1) {
								currentFs = arrayElement;
							} else {
								featureValue = arrayElement;
								if (null != replacements)
									throw new IllegalArgumentException(
											"Replacements of feature values is only supported for primitive feature types. However, the feature path "
													+ featurePathString + " points to the feature value "
													+ featureValue);
							}
						}
					} else {
						if (arrayClass.equals(StringArray.class)) {
							log.trace("Value of feature  {} is a {}.", currentFeature, arrayClass.getSimpleName());
							CommonArrayFS sa = (CommonArrayFS) array;
							try {
								featureValue = getArrayValue(sa, index, replacements);
								alreadyReplaced.add(currentFs);
								// are the next two lines necessary?
								// if (null == featureValue)
								// break;
							} catch (NoSuchMethodException | SecurityException | IllegalAccessException
									| IllegalArgumentException | InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				} else { // no primitive, no array, thus: FeatureStructure
					if (i < features.length - 1)
						currentFs = currentFs.getFeatureValue(currentFeature);
					else
						featureValue = currentFs.getFeatureValue(currentFeature);
				}
			}
		} catch (CASException e) {
			throw new RuntimeException(e);
		}
		if (builtInFunction != null && featureValue != null) {
			if (List.class.isAssignableFrom(featureValue.getClass())) {
				@SuppressWarnings("unchecked")
				List<Object> valueList = (List<Object>) featureValue;
				for (int i = 0; i < valueList.size(); i++) {
					Object value = valueList.get(i);
					Object functionValue = null;
					functionValue = applyBuiltInFunction(value);
					if (functionValue != null)
						valueList.set(i, functionValue);
				}
			} else if (featureValue instanceof FeatureStructure) {
				featureValue = applyBuiltInFunction(featureValue);
			}
		}
		return featureValue;
	}

	private Object applyBuiltInFunction(Object value) {
		Object functionValue = null;
		if (value instanceof Annotation) {
			if (builtInFunction.equals("coveredText()"))
				functionValue = ((Annotation) value).getCoveredText();
			else
				throw new NotImplementedException(
						"Built-in function " + builtInFunction + " is currently not supported by the JCoReFeaturePath");
		}
		return functionValue;
	}

	private Object getArrayValue(CommonArrayFS sa, int index, Map<?, ?> replacements) throws NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object featureValue;

		Method getter = getterMap.get(sa.getClass());
		if (null == getter) {
			getter = sa.getClass().getDeclaredMethod("get", int.class);
			getterMap.put(sa.getClass(), getter);
		}

		Method setter = null;
		if (null != replacements) {
			setter = setterMap.get(sa.getClass());
			if (null == setter) {
				setter = sa.getClass().getDeclaredMethod("set", int.class, getter.getReturnType());
				setterMap.put(sa.getClass(), setter);
			}
		}

		if (index >= sa.size()) {
			log.trace(
					"Array index is {} which is greater than the array has entries. Nothing is returned for this feature structure.",
					index);
			// Nothing to get for this fs
			return null;
		} else if (index == Integer.MIN_VALUE) {
			log.trace("No particular index to access has been given. Returning values for all elements in the array.");
			// No index given on an array feature; thus we collect all values in
			// the array.
			List<Object> valueList = new ArrayList<>();
			for (int j = 0; j < sa.size(); j++) {
				Object value = getArrayElement(sa, replacements, getter, setter, j);
				valueList.add(value);
			}
			featureValue = valueList;
		} else {
			featureValue = getArrayElement(sa, replacements, getter, setter, index);
		}
		return featureValue;
	}

	protected Object getArrayElement(CommonArrayFS sa, Map<?, ?> replacements, Method getter, Method setter, int index)
			throws IllegalAccessException, InvocationTargetException {
		int effectiveIndex = index;
		if (effectiveIndex < 0)
			effectiveIndex = sa.size() + effectiveIndex;
		if (effectiveIndex < 0 || effectiveIndex > sa.size())
			return null;
		Object value = getter.invoke(sa, effectiveIndex);
		// TODO is the alreadyReplaced check really necessary here?
		if (null != replacements && !alreadyReplaced.contains(sa)) {
			Object replacement = replacements.get(value);
			if (null == replacement && !replaceUnmappedValues) {
				log.trace(
						"Value {} for array position {} is not replaced because there was no replacement entry (i.e. the replacement is null) and null value replacement is switched off.",
						new Object[] { value, effectiveIndex });
			} else {
				if (null == replacement) {
					replacement = defaultReplacementValue;
					log.trace("No mapped value found for feature value {}, using default value {} instead.", value,
							defaultReplacementValue);
				}
				// replace the original value with the mapped value
				log.trace("Replacing array value at position {}: {} --> {}",
						new Object[] { effectiveIndex, value, replacement });
				value = replacement;
				setter.invoke(sa, effectiveIndex, value);
			}
		}
		return value;
	}

	/**
	 * Returns the value of the feature <tt>feature</tt> of the feature
	 * structure <tt>fs</tt>.
	 * 
	 * @param fs
	 * @param feature
	 * @param replacements
	 * @return
	 */
	protected Object getFeatureValueFromFeatureStructure(FeatureStructure fs, Feature feature, Map<?, ?> replacements) {
		if (null == fs)
			throw new IllegalArgumentException("Passed FeatureStucture was null");
		Object featureValue;
		Type rangeType = feature.getRange();
		switch (rangeType.getName()) {
		case "uima.cas.String":
			featureValue = fs.getFeatureValueAsString(feature);
			break;
		case "uima.cas.Integer":
			featureValue = fs.getIntValue(feature);
			break;
		case "uima.cas.Boolean":
			featureValue = fs.getBooleanValue(feature);
			break;
		case "uima.cas.Float":
			featureValue = fs.getFloatValue(feature);
			break;
		case "uima.cas.Double":
			featureValue = fs.getDoubleValue(feature);
			break;
		case "uima.cas.Byte":
			featureValue = fs.getByteValue(feature);
			break;
		case "uima.cas.Long":
			featureValue = fs.getLongValue(feature);
			break;
		case "uima.cas.Short":
			featureValue = fs.getShortValue(feature);
			break;
		default:
			throw new IllegalArgumentException(
					"The type " + rangeType + " is currently not supported as feature value type.");
		}
		if (null != replacements && !alreadyReplaced.contains(fs)) {
			Object replacement = replacements.get(featureValue);
			if (null == replacement && !replaceUnmappedValues) {
				log.trace(
						"Value {} for feature {} is not replaced because there was no replacement entry (i.e. the replacement is null) and null value replacement is switched off.",
						new Object[] { featureValue, feature.getName() });
			} else {
				if (null == replacement) {
					replacement = defaultReplacementValue;
					log.trace("No mapped value found for feature value {}, using default value {} instead.",
							featureValue, defaultReplacementValue);
				}
				log.trace("Replacing value for feature {}: {} --> {}",
						new Object[] { feature.getName(), featureValue, replacement });
				featureValue = replacement;
				// This is certainly not optimal concerning runtime, but it's
				// quite concisely written...
				fs.setFeatureValueFromString(feature, null != featureValue ? String.valueOf(featureValue) : null);
				alreadyReplaced.add(fs);
			}
		} else if (null != replacements && alreadyReplaced.contains(fs)) {
			log.trace(
					"Value {} for feature {} is not replaced because the respective feature structure {} was already subject to a replacement.",
					new Object[] { featureValue, feature.getName(), fs.toString() });
			// for (Object o : alreadyReplaced) {
			// FeatureStructure f = (FeatureStructure) o;
			// if (fs.equals(f))
			// log.trace("The existing structure: {}; has keys: {}, {}", new
			// Object[] {f, f.hashCode(), fs.hashCode()});
			// }
		} else {
			log.trace("No replacement because replacements are null: {}", replacements);
		}
		return featureValue;
	}

	public String[] getValueAsStringArray(FeatureStructure fs, boolean doReplacements) {
		Object value = getValue(fs, 0, doReplacements ? replacements : null);
		return getValueAsStringArray(value);
	}

	/**
	 * Returns the feature path values without performing value replacements.
	 * 
	 * @param fs
	 * @return
	 */
	public String[] getValueAsStringArray(FeatureStructure fs) {
		Object value = getValue(fs, 0);
		return getValueAsStringArray(value);
	}

	private String[] getValueAsStringArray(Object value) {
		if (null == value)
			return null;
		if (value.getClass() == ArrayList.class) {
			List<?> objectValues = (List<?>) value;
			String[] stringValues = new String[objectValues.size()];
			for (int i = 0; i < objectValues.size(); i++) {
				String stringValue = getObjectValueAsString(objectValues.get(i));
				stringValues[i] = stringValue;
			}
			return stringValues;
		} else {
			String[] stringValue = new String[1];
			stringValue[0] = getObjectValueAsString(value);
			return stringValue;
		}
	}

	public List<String> getValueAsStringList(FeatureStructure fs, boolean doReplacements) {
		Object value = getValue(fs, 0, doReplacements ? replacements : null);
		return getValueAsStringList(value);
	}

	/**
	 * Returns the feature path values without performing value replacements.
	 * The returned list is not held by this object and can be modified in the
	 * application without side effects.
	 * 
	 * @param fs
	 * @return
	 */
	public List<String> getValueAsStringList(FeatureStructure fs) {
		Object value = getValue(fs, 0);
		return getValueAsStringList(value);
	}

	private List<String> getValueAsStringList(Object value) {
		if (null == value)
			return null;
		if (value.getClass() == ArrayList.class) {
			List<?> objectValues = (List<?>) value;
			List<String> stringValues = new ArrayList<>(objectValues.size());
			for (int i = 0; i < objectValues.size(); i++) {
				String stringValue = getObjectValueAsString(objectValues.get(i));
				stringValues.add(stringValue);
			}
			return stringValues;
		} else {
			List<String> stringValue = new ArrayList<>();
			stringValue.add(getObjectValueAsString(value));
			return stringValue;
		}
	}

	/**
	 * Get a String representation for a single primitive value.
	 * 
	 * @param objectValue
	 * @return
	 */
	protected String getObjectValueAsString(Object objectValue) {
		if (null == objectValue)
			return null;
		String stringValue = null;
		if (objectValue.getClass() == String.class) {
			stringValue = (String) objectValue;
		} else if (objectValue instanceof Number) {
			stringValue = String.valueOf(objectValue);
		}
		return stringValue;
	}

	@Override
	public String getValueAsString(FeatureStructure fs) {
		return getValueAsString(fs, false);
	}

	/**
	 * If using replacements, please note the comment at
	 * {@link #clearReplacementCache()}.
	 * 
	 * @param fs
	 * @param doReplacements
	 * @return
	 */
	public String getValueAsString(FeatureStructure fs, boolean doReplacements) {
		Object value = getValue(fs, 0, doReplacements ? replacements : null);
		if (null == value)
			return null;
		if (List.class.isAssignableFrom(value.getClass())) {
			List<?> objectValues = (List<?>) value;
			List<String> stringValues = new ArrayList<>();
			for (int i = 0; i < objectValues.size(); i++) {
				Object objectValue = objectValues.get(i);
				String stringValue = null;
				if (objectValue.getClass() == String.class) {
					stringValue = (String) objectValue;
				} else if (objectValue instanceof Number) {
					stringValue = String.valueOf(objectValue);
				}
				stringValues.add(stringValue);
			}
			return StringUtils.join(stringValues, ", ");
		}
		if (value instanceof Number) {
			return String.valueOf(value);
		} else {
			return (String) value;
		}

	}

	/**
	 * For the work with replacements, please note the comment of
	 * {@link #clearReplacementCache()}.
	 * 
	 * @param replaceWithNullValues
	 */
	public void setReplaceUnmappedValues(boolean replaceWithNullValues) {
		this.replaceUnmappedValues = replaceWithNullValues;
	}

	/**
	 * Returns <tt>true</tt> if feature values not contained in the replacement
	 * map are to be replaced by a default value.
	 * 
	 * @return
	 */
	public boolean getReplaceUnmappedValues() {
		return replaceUnmappedValues;
	}

	@Override
	public String ll_getValueAsString(int fsRef, LowLevelCAS llCas) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getType(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeClass getTypClass(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFeaturePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStringValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getIntValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getBooleanValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Byte getByteValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getDoubleValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getFloatValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getLongValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Short getShortValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeatureStructure getFSValue(FeatureStructure fs) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Reads a replacement file with lines of the form
	 * <code>originalValue=replacementValue</code> and immediately populates the
	 * internal replacement table with those entries.
	 * 
	 * @param replacementsFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void loadReplacementsFromFile(String replacementsFile) throws FileNotFoundException, IOException {
		this.replacements = readReplacementsFromFile(replacementsFile);
	}

	/**
	 * Reads a replacement file with lines of the form
	 * <code>originalValue=replacementValue</code> and returns the respective
	 * map.
	 * 
	 * @param replacementsFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Map<String, String> readReplacementsFromFile(String replacementsFile)
			throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(replacementsFile)) {
			return readReplacementsFromInputStream(fis);
		}
	}

	/**
	 * Reads a replacement file with lines of the form
	 * <code>originalValue=replacementValue</code> and returns the respective
	 * map.
	 * 
	 * @param replacementsFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Map<String, String> readReplacementsFromInputStream(InputStream is)
			throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			Map<String, String> replacements = new HashMap<>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] split = line.split("=");
				if (split.length != 2)
					throw new IllegalArgumentException(
							"Format error in replacements file: Expected format is 'originalValue=replacementValue' but the input line '"
									+ line + "' has " + split.length + " columns.");
				replacements.put(split[0].trim(), split[1].trim());
			}
			return replacements;
		} finally {
			br.close();
		}
	}

	public Object getDefaultReplacementValue() {
		return defaultReplacementValue;
	}

	/**
	 * Sets a default replacement value to be used when there is no replacement
	 * for a feature value and {@link #replaceUnmappedValues} is set to
	 * <tt>true</tt>. Defaults to <tt>null</tt>.
	 * 
	 * @param defaultReplacementValue
	 */
	public void setDefaultReplacementValue(Object defaultReplacementValue) {
		this.defaultReplacementValue = defaultReplacementValue;
	}

	/**
	 * Important only if using value replacements. Clears the cache of already
	 * replaced feature structures. This has to be reset after each CAS in order
	 * to avoid collisions between different annotations from different CASes,
	 * which may happen due to the fact that the hash codes may collide and that
	 * the internal annotation addresses within a case are only unique within a
	 * CAS. Thus, the next CAS could have an annotation that doesn't get its
	 * value replaced because this annotation allegedly was already replaced due
	 * to an identity collision.
	 */
	public void clearReplacementCache() {
		alreadyReplaced.clear();
	}
}
