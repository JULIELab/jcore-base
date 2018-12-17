package de.julielab.jcore.consumer.es;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.es.filter.Filter;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.IToken;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.utility.JCoReFeaturePath;

/**
 * <p>
 * The main super class of all classes capable of creating field values (i.e.
 * instances of implementations of {@link IFieldValue}). This class offers a
 * wide range of methods for the creation of field values, mainly required to
 * translate UIMA annotations into the preanalyzed format where all analysis is
 * ported from the CAS right into the ElasticSearch index (please not that this
 * requires the elasticsearch-preanalyzed-mapper plugin on the ElasticSearch
 * server side).
 * </p>
 * <p>
 * The most powerful method is
 * {@link #getTokensForAnnotationIndexes(FeaturePathSets, Filter, boolean, Class, AnnotationFS, DistributedField, JCas)}.
 * This method takes sets of feature paths which define the UIMA annotation
 * types as well as the actual values to be derived from each annotation from a
 * JCas instance. It then creates a list of preanalyzed tokens corresponding the
 * given annotation indexes. If the parameter <tt>AnnotationFS</tt> is given,
 * the returned tokens will be restricted to those that are covered by the
 * passed cover annotation. For more details refer to the JavaDoc of the method.
 * </p>
 * 
 * @author faessler
 *
 */
public abstract class AbstractFieldGenerator {

	protected final Logger log = LoggerFactory.getLogger(AbstractFieldGenerator.class);
	protected final Logger logNullValueFp = LoggerFactory
			.getLogger(AbstractFieldGenerator.class.getCanonicalName() + "NullValueFp");

	long tokenMapCreationTime = 0;

	protected long filterApplicationTime;

	protected long valueDeterminationTime;

	protected long valueToTokenConversionTime;

	protected long featureSetsCreationTime;

	protected long featurePathCreationTime;

	protected long jsonSerializationTime;

	private Map<String, JCoReFeaturePath> fpCache;

	protected FilterRegistry filterRegistry;

	protected Map<Class<? extends FieldGenerator>, FieldGenerator> subFieldGenerators;
	protected Map<Class<? extends FieldValueGenerator>, FieldValueGenerator> innerDocumentGenerators;

	@Deprecated
	private Map<Class<?>, List<TOP>> subgeneratorFS;

	@Deprecated
	private String fieldname;

	@Deprecated
	public AbstractFieldGenerator(String fieldname) {
		this(fieldname, null);
	}

	@Deprecated
	public AbstractFieldGenerator(String fieldname, FilterRegistry filterRegistry) {
		this.fieldname = fieldname;
		this.filterRegistry = filterRegistry;
		fpCache = new HashMap<>();
		subFieldGenerators = new HashMap<>();
		innerDocumentGenerators = new HashMap<>();
	}

	public AbstractFieldGenerator() {
		this((FilterRegistry) null);
	}

	public AbstractFieldGenerator(FilterRegistry filterRegistry) {
		this.filterRegistry = filterRegistry;
		fpCache = new HashMap<>();
		subFieldGenerators = new HashMap<>();
		innerDocumentGenerators = new HashMap<>();
	}

	public void setFilterRegistry(FilterRegistry filterRegistry) {
		this.filterRegistry = filterRegistry;
	}

	@Deprecated
	private void clearSubgeneratorFS() {
		if (null != subgeneratorFS)
			subgeneratorFS.clear();
	}

	/**
	 * 
	 * @param key
	 * @param fieldValue
	 *            Either a complete preanalyzed field (with version and - optional -
	 *            the original field string value) or a list of string tokens.
	 * @param fieldMap
	 */
	@Deprecated
	protected void putFieldIntoMap(String key, Object fieldValue, Map<String, List<Object>> fieldMap) {
		if (null == fieldValue)
			return;
		List<Object> list = fieldMap.get(key);
		if (null == list) {
			list = new ArrayList<>();
			fieldMap.put(key, list);
		}
		// Is fieldValue a List? Then add all elements from from it into the
		// single list for this field
		if (List.class.isAssignableFrom(fieldValue.getClass())) {
			List<?> listValue = (List<?>) fieldValue;
			list.addAll(listValue);
		} else {
			// Otherwise, add the single element
			list.add(fieldValue);
		}
	}

	/**
	 * <p>
	 * Generates tokens obeying the declarations given by the feature path sets. The
	 * {@link FeaturePathSets) object is a list of {@link FeaturePathSet} objects.
	 * Each of the feature path set instances refers to a single UIMA annotation
	 * type. For each annotation of the given UIMA type, a feature path set defines
	 * a collection of feature paths that define which values should be extracted
	 * from the annotation for document field token creation.
	 * </p>
	 * <p>
	 * To obtain tokens from all the FeaturePathSet objects in the FeaturePathSets
	 * parameter, this method creates an {@link AnnotationIndexMerger} iterator over
	 * the types given by <tt>featurePathSets</tt>. The iterator outputs UIMA
	 * annotation after UIMA annotation. Token generation is done on annotation
	 * level and all feature paths and filters specified for the respective
	 * annotation type are applied annotation-wise. That specifically means that
	 * feature paths specific filters ({@links FeaturePathSet#getFilter()) are reset
	 * before each single annotation: UniqueFilters remove duplicates on annotation
	 * level, not on type level. If multiple <tt>featurePathSets</tt> refer to the
	 * same type, this type is <em>not</em> iterated over multiple times. Each type
	 * is only iterated over once. However, all the different feature paths and
	 * filters are applied to the respective annotations</p>
	 * 
	 * @param featurePathSets
	 *            The definition of which annotation types to iterate over and what
	 *            values to derive from each annotation of a specific type.
	 * @param filterForField
	 *            A filter used for all tokens, independent of the annotation type
	 *            they came from.
	 * @param sort
	 *            Whether or not the returned tokens should be aligned by start
	 *            offsets. This is required to create a proper "stacking" of terms
	 *            in the ElasticSearch index so that, for example, a gene ID is
	 *            searched for but the actual gene name in the text is highlighted.
	 * @param tokenClass
	 *            {@link RawToken} or {@link PreanalyzedToken}.
	 * @param coveringAnnotation
	 *            An annotation that restricts the returned token list to tokens
	 *            from annotations covered by the given covering annotation.
	 * @param distributedFields
	 *            A definition of a distribution of tokens to multiple fields. This
	 *            can be used to sort a predefined list of token values into
	 *            "buckets" where each bucket corresponds to a document field. After
	 *            this method has finished, if the distributedFields parameter was
	 *            not null, the distributedFields object itself will contain the
	 *            token buckets. They must then be set as fields to a document.
	 * @param aJCas
	 *            The UIMA JCas object to derive annotations and, thus, tokens from.
	 * @return A list of tokens created from the JCas corresponding to the
	 *         <tt>featurePathSets</tt>.
	 * @throws CASException
	 *             If reading the JCas fails.
	 */
	@SuppressWarnings("unchecked")
	public <T extends IToken> List<T> getTokensForAnnotationIndexes(FeaturePathSets featurePathSets,
			Filter filterForField, boolean sort, Class<T> tokenClass, AnnotationFS coveringAnnotation,
			DistributedField<? extends IToken> distributedFields, JCas aJCas) throws CASException {
		// If we do distribute the annotations according to a DistributedField
		// definition, we won't create our own token
		// list here.
		List<T> l = null;
		if (null == distributedFields)
			l = new ArrayList<>();

		if (null != filterForField)
			filterForField.reset();

		LinkedHashSet<Integer> annotationIndexes = new LinkedHashSet<>(featurePathSets.size());
		for (Integer type : featurePathSets.getUimaTypes())
			annotationIndexes.add(type);
		AnnotationIndexMerger indexMerger;
		try {
			indexMerger = new AnnotationIndexMerger(annotationIndexes, sort, coveringAnnotation, aJCas);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Type could be found in the type system.", e);
		}

		int positionIncrement = 1;
		boolean valueForCurrentOffsetFound = false;
		while (indexMerger.incrementAnnotation()) {
			TOP a = indexMerger.getAnnotation();
			int start = indexMerger.getCurrentBegin();
			int end = indexMerger.getCurrentEnd();
			// adapt offsets for the covering annotation
			if (null != coveringAnnotation) {
				start -= coveringAnnotation.getBegin();
				end -= coveringAnnotation.getBegin();
			}
			// If we don't sort, the position increment doesn't make sense
			// anyway. Sorting is only needed for phrase
			// queries and highlighting.
			if (sort) {
				if (indexMerger.hasBeginOffsetChanged()) {
					if (!valueForCurrentOffsetFound && !indexMerger.firstToken())
						positionIncrement++;
					else
						positionIncrement = 1;
					valueForCurrentOffsetFound = false;
				} else if (!indexMerger.firstToken) {
					positionIncrement = 0;
				}
			}
			int type = a.getTypeIndexID();

			Collection<FeaturePathSet> featurePaths = featurePathSets.getFeaturePathSetsForType(type);
			// the feature paths can be empty when 'a' is not exactly the type
			// that was given in the original declaration of the feature path
			// set but a subtype. We search the feature paths for the supertype
			// of the type 'a' belongs to
			if (featurePaths.isEmpty())
				featurePaths = featurePathSets.findFeaturePathSetsForSupertypes(a.getType(), aJCas);
			for (FeaturePathSet featureSet : featurePaths) {
				Filter filter = featureSet.getFilter();
				if (null != filter)
					filter.reset();
				String featureConcatenationString = featureSet.getConcatenationString();
				List<List<String>> featureValues = new ArrayList<>(featurePaths.size());

				// In the following if-else we determine the final feature
				// values, including filtering.
				// So, this determines the 'token source'.
				long time = System.currentTimeMillis();
				if (null != featureSet.getFeaturePaths() && featureSet.getFeaturePaths().size() > 0) {
					for (String featurePath : featureSet.getFeaturePaths()) {
						JCoReFeaturePath fp = getCachedFeaturePath(featurePath);
						List<String> valueList = fp.getValueAsStringList(a);
						valueList = applyFilter(valueList, filter);
						if (!valueList.isEmpty())
							featureValues.add(valueList);
					}
				} else if (a instanceof Annotation) {
					try {
						// There are no feature paths given, use the covered text
						List<String> value = new ArrayList<>();
						value.add(((Annotation) a).getCoveredText());
						value = applyFilter(value, filter);
						featureValues.add(value);
					} catch (StringIndexOutOfBoundsException e) {
						log.error(
								"An annotation occurred with offsets that exceed the document text. The annotation: \n{}\nthe document text length: {}",
								a, a.getCAS().getDocumentText().length());
						throw e;
					}
				} else {
					throw new IllegalArgumentException("No feature path has been given for feature structure " + a
							+ " but since it does not cover any text no field value can be retrieved.");
				}
				time = System.currentTimeMillis() - time;
				valueDeterminationTime += time;

				time = System.currentTimeMillis();
				// Concatenate received values, if desired.
				// Concatenation for multiple feature paths happens by expecting
				// that each feature path results in the
				// same number of annotation results. Then, we concatenate the
				// parallel values. Example:
				// Author type, we have the features "firstName" and "lastName".
				// We want to create a field value of the
				// form "lastName, firstName". However, a document may have an
				// arbitrary number of authors. But each of
				// them has a first and a last name.
				if (!featureValues.isEmpty() && !StringUtils.isEmpty(featureConcatenationString)) {
					for (int i = 0; i < featureValues.get(0).size(); ++i) {
						List<String> parallelFeatureValues = new ArrayList<>(featureValues.size());
						for (int j = 0; j < featureValues.size(); ++j) {
							String v = featureValues.get(j).get(i);
							// NOTE: here we filter out null values (e.g. there
							// is the case that some authors don't have a
							// foreName in medline). If we sometimes need the
							// null values, that should be settable by
							// parameter.
							if (null != v)
								parallelFeatureValues.add(v);
						}
						String singleConcatValue = StringUtils.join(parallelFeatureValues, featureConcatenationString);
						List<String> filteredValues = applyFilter(singleConcatValue, filterForField);
						for (String concatValue : filteredValues) {
							if (concatValue != null && concatValue.length() > 0) {
								if (null == distributedFields) {
									if (tokenClass.equals(RawToken.class))
										l.add((T) new RawToken(concatValue));
									else if (tokenClass.equals(PreanalyzedToken.class))
										l.add((T) createPreanalyzedTokenInTokenSequence(l, concatValue, start, end,
												positionIncrement, null, null, 0));
									else
										throw new IllegalArgumentException(
												"Token class " + tokenClass + " is not supported.");
								} else {
									if (tokenClass.equals(RawToken.class))
										distributedFields.addTokenForTerm(new RawToken(concatValue));
									else if (tokenClass.equals(PreanalyzedToken.class))
										distributedFields.addTokenForTerm(createPreanalyzedToken(concatValue, -1, -1,
												positionIncrement, null, null, 0));
									else
										throw new IllegalArgumentException(
												"Token class " + tokenClass + " is not supported.");
								}
								valueForCurrentOffsetFound = true;
								// If we have multiple features for the same
								// annotation, we want to position all of them
								// at
								// the same position
								positionIncrement = 0;
							}
						}
					}
				} else if (!featureValues.isEmpty()) {
					// no concatenation
					for (int i = 0; i < featureValues.size(); i++) {
						List<String> valueArray = featureValues.get(i);
						List<String> filteredList = applyFilter(valueArray, filterForField);
						for (int j = 0; j < filteredList.size(); j++) {
							String value = filteredList.get(j);
							if (value != null && value.length() > 0) {
								if (null == distributedFields) {
									if (tokenClass.equals(RawToken.class))
										l.add((T) new RawToken(value));
									else if (tokenClass.equals(PreanalyzedToken.class))
										l.add((T) createPreanalyzedTokenInTokenSequence(l, value, start, end,
												positionIncrement, null, null, 0));
									else
										throw new IllegalArgumentException(
												"Token class " + tokenClass + " is not supported.");
								} else {
									if (tokenClass.equals(RawToken.class))
										distributedFields.addTokenForTerm(new RawToken(value));
									else if (tokenClass.equals(PreanalyzedToken.class))
										distributedFields.addTokenForTerm(createPreanalyzedToken(value, -1, -1,
												positionIncrement, null, null, 0));
									else
										throw new IllegalArgumentException(
												"Token class " + tokenClass + " is not supported.");
								}
								valueForCurrentOffsetFound = true;
								// If we have multiple features for the same
								// annotation, we want to position all of them
								// at the same position
								positionIncrement = 0;
							}
						}
					}
				}
				time = System.currentTimeMillis() - time;
				valueToTokenConversionTime += time;
			}
		}
		return l;
	}

	private List<String> applyFilter(List<String> values, Filter f) {
		long time = System.currentTimeMillis();
		if (null == values)
			return Collections.emptyList();
		if (null == f)
			return values;
		List<String> ret = new ArrayList<>(values.size());
		for (String value : values) {
			if (null == value) {
				ret.add(null);
				continue;
			}
			for (String filteredValue : f.filter(value))
				if (null != filteredValue)
					ret.add(filteredValue);
		}
		time = System.currentTimeMillis() - time;
		filterApplicationTime += time;
		return ret;
	}

	private List<String> applyFilter(String value, Filter f) {
		long time = System.currentTimeMillis();
		if (null == value) {
			List<String> ret = new ArrayList<>(1);
			ret.add(value);
			return ret;
		}
		if (null == f) {
			ArrayList<String> ret = new ArrayList<>(1);
			ret.add(value);
			return ret;
		}
		List<String> ret = f.filter(value);
		time = System.currentTimeMillis() - time;
		filterApplicationTime += time;
		return ret;
	}

	protected IFieldValue createPreanalyzedFieldValueForAnnotation(Annotation a, String featurePath)
			throws CASException {
		return createPreanalyzedFieldValueForAnnotation(a, featurePath, null);
	}

	/**
	 * Creates a {@link PreanalyzedToken} or a {@link ArrayFieldValue} of
	 * PreanalyzedToken by extracting (a) value(s) according to the given feature
	 * path on the given annotation and applying the given filter. The filter is
	 * only used if a featurePath is given. It is always at the beginning of the
	 * method.
	 * 
	 * @param a
	 *            The annotation to derive tokens from.
	 * @param featurePath
	 *            The declaration of which feature of a should be extracted.
	 * @param f
	 *            A filter to apply for the feature path values.
	 * @return A PreanalyzedToken or ArrayFieldValue if the feature path application
	 *         resulted in multiple values.
	 * @throws CASException
	 */
	protected IFieldValue createPreanalyzedFieldValueForAnnotation(Annotation a, String featurePath, Filter f)
			throws CASException {

		if (null != f)
			f.reset();

		int start = a.getBegin();
		int end = a.getEnd();
		if (!StringUtils.isBlank(featurePath)) {
			JCoReFeaturePath fp = getCachedFeaturePath(featurePath);
			// TODO shouldn't be necessary
			fp.initialize(featurePath);
			List<String> valueArray = fp.getValueAsStringList(a);
			if (null == valueArray) {
				logNullValueFp.debug("Feature path {} returned null value for annotation {}", featurePath, a);
				return null;
			}
			List<PreanalyzedFieldValue> fieldValues = new ArrayList<>();
			for (String fpValue : valueArray) {
				List<PreanalyzedToken> tokens = new ArrayList<>();
				List<String> filteredValues = applyFilter(fpValue, f);
				for (String filteredValue : filteredValues) {
					if (!StringUtils.isBlank(filteredValue)) {
						PreanalyzedToken token = createPreanalyzedTokenInTokenSequence(tokens, filteredValue, start,
								end, 1, null, null, 0);
						tokens.add(token);
					}
				}
				PreanalyzedFieldValue preanalyzedFieldValue = createPreanalyzedFieldValue(fpValue, tokens);
				if (null != preanalyzedFieldValue)
					fieldValues.add(preanalyzedFieldValue);
			}
			if (fieldValues.isEmpty())
				return null;
			else if (fieldValues.size() == 1)
				return fieldValues.get(0);
			else
				return new ArrayFieldValue(fieldValues);
		} else {
			List<PreanalyzedToken> tokens = new ArrayList<>();
			String coveredText = a.getCoveredText();
			PreanalyzedToken token = createPreanalyzedTokenInTokenSequence(tokens, coveredText, start, end, 1, null,
					null, 0);
			tokens.add(token);
			return createPreanalyzedFieldValue(coveredText, tokens);
		}
	}

	/**
	 * Returns a single list for all tokens that are derived from all feature path
	 * values pointed to <tt>featurePath</tt>. The filters are parallel to the
	 * featurePaths and reset once per featurePath.
	 * 
	 * @param a
	 * @param featurePath
	 * @param f
	 * @return
	 * @throws CASException
	 */
	protected List<PreanalyzedToken> createPreanalyzedTokensForAnnotation(AnnotationFS a, String[] featurePaths,
			Filter[] filters) throws CASException {

		int start = a.getBegin();
		int end = a.getEnd();
		if (null != featurePaths && featurePaths.length > 0) {
			List<PreanalyzedToken> tokens = new ArrayList<>();
			for (int i = 0; i < featurePaths.length; i++) {
				String featurePath = featurePaths[i];
				Filter f = filters[i];

				if (null != f)
					f.reset();

				if (null != featurePath) {
					JCoReFeaturePath fp = getCachedFeaturePath(featurePath);
					// TODO shouldn't be necessary
					fp.initialize(featurePath);
					List<String> valueArray = fp.getValueAsStringList(a);
					if (null == valueArray) {
						logNullValueFp.debug("Feature path {} returned null value for annotation {}", featurePath, a);
						return null;
					}
					for (String fpValue : valueArray) {
						List<String> filteredValues = applyFilter(fpValue, f);
						for (String filteredValue : filteredValues) {
							if (!StringUtils.isBlank(filteredValue)) {
								PreanalyzedToken token = createPreanalyzedTokenInTokenSequence(tokens, filteredValue,
										start, end, 1, null, null, 0);
								tokens.add(token);
							}
						}
					}
				} else {
					String coveredText = a.getCoveredText();
					List<String> filteredValues = applyFilter(coveredText, f);
					for (String filteredValue : filteredValues) {
						PreanalyzedToken token = createPreanalyzedTokenInTokenSequence(tokens, filteredValue, start,
								end, 1, null, null, 0);
						tokens.add(token);
					}
				}
			}
			return tokens;
		}
		throw new IllegalArgumentException(
				"You must deliver a non-empty featurePaths array. It may contain null values in which case the coveredText of the annotation will be used as feature value.");
	}

	/**
	 * 
	 * @param input
	 * @param f
	 * @return
	 * @throws ClassCastException
	 *             If the requested return type is not assignment compatible with
	 *             the actually created type. This happens for example if the filter
	 *             creates multiple values - resulting in an ArrayFieldValue - but a
	 *             single RawToken value is expected to be returned. In cases where
	 *             the return type - array or single value - is not known, just use
	 *             <tt>IFieldValue</tt> as the return type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends IFieldValue> T createRawFieldValueForString(String input, Filter f) {
		if (null != f)
			f.reset();
		List<String> filteredValues = f.filter((String) input);
		if (filteredValues.isEmpty())
			return (T) new ArrayFieldValue();
		else if (filteredValues.size() == 1 && null != filteredValues.get(0))
			return (T) new RawToken(filteredValues.get(0));
		else
			return (T) createRawArrayFieldValue(filteredValues);

	}

	/**
	 * Convenience method to create an array of raw - non pre-analyzed - field
	 * values. Note that for non-complex raw field values (strings, numbers, ...)
	 * you can just create a <tt>RowToken</tt> if no filter is employed. Otherwise,
	 * use {@link #createRawFieldValueForString(String, Filter)}.
	 * 
	 * @param fieldValues
	 * @return
	 */
	public <T> ArrayFieldValue createRawArrayFieldValue(Collection<T> fieldValues) {
		ArrayFieldValue array = new ArrayFieldValue();
		for (T value : fieldValues) {
			if (null != value)
				array.add(new RawToken(value));
		}
		return array;
	}

	/**
	 * Convenience method to create an array of raw - non pre-analyzed - field
	 * values. Note that for non-complex raw field values (strings, numbers, ...)
	 * you can just create a <tt>RowToken</tt> if no filter is employed. Otherwise,
	 * use {@link #createRawFieldValueForString(String, Filter)}.
	 * 
	 * @param fieldValues
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> ArrayFieldValue createRawArrayFieldValue(Collection<T> fieldValues, Filter filter) {
		ArrayFieldValue array = new ArrayFieldValue();
		for (T value : fieldValues) {
			if (null != value) {
				List<String> filteredValue;
				if (List.class.isAssignableFrom(value.getClass()))
					filteredValue = applyFilter((List<String>) value, filter);
				else
					filteredValue = applyFilter((String) value, filter);
				array.addFlattened(createRawArrayFieldValue(filteredValue));
			}
		}
		return array;
	}

	/**
	 * For each feature structure in <tt>fs</tt>, applies the given
	 * <tt>featurePath</tt> and filter (may be null).
	 * 
	 * @param fs
	 *            An array of feature structures to create field values from.
	 * @param featurePath
	 *            A feature path retrieving values from all feature structures.
	 * @param filter
	 *            An optional filter applied to each retrieved value.
	 * @param overallFilter
	 *            An optional filter that is applied after all field values have
	 *            been retrieved and filtered.
	 * @return An array field value consisting of the filtered values retrieved from
	 *         the elements of <tt>fs</tt> by <tt>featurePath</tt>.
	 * @throws CASException
	 *             If reading the CAS of a feature structure fails.
	 */
	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] fs, String featurePath, Filter filter,
			Filter overallFilter) throws CASException {
		ArrayFieldValue arrayFieldValue = new ArrayFieldValue();
		for (int i = 0; i < fs.length; i++) {
			FeatureStructure annotation = fs[i];
			IFieldValue fieldValueForAnnotation = createRawFieldValueForAnnotation(annotation, featurePath, filter);
			arrayFieldValue.addFlattened(fieldValueForAnnotation);
		}
		if (null != overallFilter) {
			overallFilter.reset();
			ArrayFieldValue filteredArrayFieldValue = new ArrayFieldValue();
			for (IFieldValue fieldValue : arrayFieldValue) {
				RawToken token = (RawToken) fieldValue;
				String tokenString = String.valueOf(token.token);
				List<String> filteredTokens = overallFilter.filter(tokenString);
				if (!filteredTokens.isEmpty()) {
					for (String filteredToken : filteredTokens)
						filteredArrayFieldValue.add(new RawToken(filteredToken));
				}
			}
			arrayFieldValue = filteredArrayFieldValue;
		}
		return arrayFieldValue;
	}

	/**
	 * For each feature structure in <tt>fs</tt>, applies the given
	 * <tt>featurePath</tt> and filter (may be null).
	 * 
	 * @param fs
	 *            An array of feature structures to create field values from.
	 * @param featurePath
	 *            A feature path retrieving values from all feature structures.
	 * @param filter
	 *            An optional filter applied to each retrieved value.
	 * @return An array field value consisting of the filtered values retrieved from
	 *         the elements of <tt>fs</tt> by <tt>featurePath</tt>.
	 * @throws CASException
	 *             If reading the CAS of a feature structure fails.
	 */
	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] fs, String featurePath, Filter filter)
			throws CASException {
		return createRawFieldValueForAnnotations(fs, featurePath, filter, null);
	}

	/**
	 * For each feature structure in <tt>fs</tt>, applies the given
	 * <tt>featurePath</tt>.
	 * 
	 * @param fs
	 *            An array of feature structures to create field values from.
	 * @param featurePath
	 *            A feature path retrieving values from all feature structures.
	 * @return An array field value consisting of the filtered values retrieved from
	 *         the elements of <tt>fs</tt> by <tt>featurePath</tt>.
	 * @throws CASException
	 *             If reading the CAS of a feature structure fails.
	 */
	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] fs, String featurePath)
			throws CASException {
		return createRawFieldValueForAnnotations(fs, featurePath, null);
	}

	/**
	 * For each feature structure in <tt>fs</tt>, applies each given featurePath in
	 * <tt>featurePaths<tt>.
	 * 
	 * &#64;param fs
	 *            An array of feature structures to create field values from.
	 * &#64;param featurePaths
	 *            An array of feature paths retrieving values from all feature structures.
	 * &#64;return An array field value consisting of the filtered values retrieved from
	 *         the elements of <tt>fs</tt> by <tt>featurePath</tt>.
	 * 
	 * @throws CASException
	 *             If reading the CAS of a feature structure fails.
	 */
	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] a, String[] featurePaths)
			throws CASException {
		return createRawFieldValueForAnnotations(a, featurePaths, null, null);
	}

	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] a, String[] featurePaths,
			Filter[] filters) throws CASException {
		return createRawFieldValueForAnnotations(a, featurePaths, filters, null);
	}

	/**
	 * Applies the
	 * {@link #createRawFieldValueForAnnotation(FeatureStructure, String[], Filter[])
	 * method to all feature structures in <tt>fss</tt>. Thus, the feature paths and
	 * filters are expected to be <em>parallel</em>: Each feature path has its own
	 * filter. If the filters array is shorter than the feature paths array, the
	 * missing filters will be treated as if they were null. Finally, after all
	 * values have been created in this way, if the <tt>overallFilter</tt> is not
	 * null, it will be applied to all resulting values. It will be reset once
	 * before the complete filtering.
	 * 
	 * @param fss
	 * @param featurePaths
	 * @param filters
	 * @param overallFilter
	 * @return
	 * @throws CASException
	 */
	public ArrayFieldValue createRawFieldValueForAnnotations(FeatureStructure[] fss, String[] featurePaths,
			Filter[] filters, Filter overallFilter) throws CASException {
		ArrayFieldValue arrayFieldValue = new ArrayFieldValue();
		for (int i = 0; i < fss.length; i++) {
			FeatureStructure annotation = fss[i];
			ArrayFieldValue fieldValueForAnnotation = createRawFieldValueForAnnotation(annotation, featurePaths,
					filters);
			arrayFieldValue.addFlattened(fieldValueForAnnotation);
		}
		if (null != overallFilter) {
			overallFilter.reset();
			ArrayFieldValue filteredArrayFieldValue = new ArrayFieldValue();
			for (IFieldValue fieldValue : arrayFieldValue) {
				RawToken token = (RawToken) fieldValue;
				String tokenString = String.valueOf(token.token);
				List<String> filteredTokens = overallFilter.filter(tokenString);
				if (!filteredTokens.isEmpty()) {
					for (String filteredToken : filteredTokens)
						filteredArrayFieldValue.add(new RawToken(filteredToken));
				}
			}
			arrayFieldValue = filteredArrayFieldValue;
		}
		return arrayFieldValue;
	}

	/**
	 * Creates a single array of all field values derived by the given feature paths
	 * and filters. The <tt>filters</tt> array is taken to be parallel to
	 * <tt>featurePaths</tt>. It may be smaller than <tt>featurePaths</tt> in which
	 * case it is equivalent as when the filters corresponding to the missing
	 * feature paths were <tt>null</tt>.
	 * 
	 * @param annotation
	 * @param featurePaths
	 * @param filters
	 * @return
	 * @throws CASException
	 */
	public ArrayFieldValue createRawFieldValueForAnnotation(FeatureStructure annotation, String[] featurePaths,
			Filter[] filters) throws CASException {
		ArrayFieldValue arrayFieldValue = new ArrayFieldValue();
		for (int i = 0; i < featurePaths.length; i++) {
			String featurePath = featurePaths[i];
			Filter filter = null;
			if (filters != null && i < filters.length)
				filter = filters[i];
			arrayFieldValue.addFlattened(createRawFieldValueForAnnotation(annotation, featurePath, filter));
		}
		return arrayFieldValue;
	}

	/**
	 * Extracts values from the feature structure (e.g. an annotation) <tt>fs</tt>
	 * by the given <tt>featurePath</tt>. The retrieved value(s) are then filtered
	 * by <tt>f</tt>.
	 * 
	 * @param fs
	 *            The feature structure to create a field value for.
	 * @param featurePath
	 *            The feature path specifying which value(s) to derive from
	 *            <tt>fs</tt>.
	 * @param f
	 *            An optional filter (might be null) applied to all values received
	 *            from <tt>featurePath</tt>.
	 * @return A {@link RawToken} If a single value is created by the process, an
	 *         {@link ArrayFieldValue} if multiple values are returned.
	 * @throws CASException
	 *             If reading the CAS of <tt>fs</tt> fails.
	 */
	public IFieldValue createRawFieldValueForAnnotation(FeatureStructure fs, String featurePath, Filter f)
			throws CASException {
		if (null != f)
			f.reset();
		if (!StringUtils.isBlank(featurePath)) {
			JCoReFeaturePath fp = getCachedFeaturePath(featurePath);
			// TODO shouldn't be necessary
			fp.initialize(featurePath);
			List<String> valueArray = fp.getValueAsStringList(fs);
			if (null == valueArray) {
				logNullValueFp.debug("Feature path {} returned null value for annotation {}", featurePath, fs);
				return null;
			}
			List<RawToken> fieldValues = new ArrayList<>();
			for (String fpValue : valueArray) {
				List<String> filteredValues = applyFilter(fpValue, f);
				for (String filteredValue : filteredValues) {
					if (!StringUtils.isBlank(filteredValue)) {
						fieldValues.add(new RawToken(filteredValue));
					}
				}
			}
			if (fieldValues.size() == 0)
				return null;
			else if (fieldValues.size() == 1)
				return fieldValues.get(0);
			else
				return new ArrayFieldValue(fieldValues);
		} else if (fs instanceof AnnotationFS) {
			String coveredText = ((AnnotationFS) fs).getCoveredText();
			List<String> filteredText = applyFilter(coveredText, f);
			if (filteredText.size() == 0)
				return null;
			else if (filteredText.size() == 1)
				return new RawToken(filteredText.get(0));
			else
				return createRawArrayFieldValue(filteredText);
		} else
			return null;
	}

	/**
	 * Creates a single value for a field that is pre-analyzed given the string
	 * content of that field (the raw, non-analyzed value) and the list of
	 * pre-analyzed tokens for that field (that is, the analysis of the field).
	 * 
	 * @param stringValue
	 * @param tokens
	 * @return
	 */
	public PreanalyzedFieldValue createPreanalyzedFieldValue(String stringValue, List<PreanalyzedToken> tokens) {
		PreanalyzedFieldValue field = new PreanalyzedFieldValue();

		if (StringUtils.isBlank(stringValue) && (null == tokens || tokens.size() == 0))
			return null;

		if (null != stringValue)
			field.fieldString = stringValue;
		if (null != tokens && tokens.size() > 0)
			field.tokens = tokens;

		return field;
	}

	protected JCoReFeaturePath getCachedFeaturePath(String featurePath) throws CASException {
		long time = System.currentTimeMillis();
		JCoReFeaturePath fp = fpCache.get(featurePath);
		if (null == fp) {
			fp = new JCoReFeaturePath();
			fp.initialize(featurePath);
			fpCache.put(featurePath, fp);
		}
		time = System.currentTimeMillis() - time;
		featurePathCreationTime += time;
		return fp;
	}

	/**
	 * Creates a {@link PreanalyzedToken} with the given attributes and possibly
	 * adjusts the <tt>positionIncrement</tt> attribute.
	 * <p>
	 * An adjustment of the <tt>positionIncrement</tt> attribute happens if the
	 * attribute is passed as 0 but the token would be the first token in the list.
	 * In this case, the <tt>positionIncrement</tt> is set to 1 because Lucene (and
	 * with it, ElasticSearch) demands the first token of a field to have a position
	 * increment value larger than 0.
	 * </p>
	 * 
	 * @param fieldValueList
	 *            The list the token will be appended to
	 * @param term
	 *            The term string of this token
	 * @param start
	 *            The start offset of this token
	 * @param end
	 *            The end offset of this token
	 * @param posIncr
	 *            The position increment of this token, adjusted to 1 if a 0 is
	 *            passed but the token would be the first token in
	 *            <tt>fieldValueList</tt>
	 * @param payload
	 *            The payload of this token
	 * @param type
	 *            The type of this token
	 * @param flags
	 *            The flags of this token
	 * @return An appropriate {@link PreanalyzedToken} with the passed attributes
	 *         and potentially adjusted <tt>positionIncrement</tt>.
	 */
	public PreanalyzedToken createPreanalyzedTokenInTokenSequence(List<?> fieldValueList, String term, int start,
			int end, int posIncr, byte[] payload, String type, int flags) {
		int effectivePosIncr = posIncr;
		if (fieldValueList.isEmpty() && posIncr == 0)
			effectivePosIncr = 1;
		PreanalyzedToken token = createPreanalyzedToken(term, start, end, effectivePosIncr, payload, type, flags);
		return token;
	}

	/**
	 * Creates a {@link PreanalyzedToken} with the given attributes.
	 * 
	 * @param fieldValueList
	 *            The list the token will be appended to
	 * @param term
	 *            The term string of this token
	 * @param start
	 *            The start offset of this token
	 * @param end
	 *            The end offset of this token
	 * @param posIncr
	 *            The position increment of this token
	 * @param payload
	 *            The payload of this token
	 * @param type
	 *            The type of this token
	 * @param flags
	 *            The flags of this token
	 * @return An appropriate {@link PreanalyzedToken} with the passed attributes.
	 */
	protected PreanalyzedToken createPreanalyzedToken(String term, int start, int end, int posIncr, byte[] payload,
			String type, int flags) {
		long time = System.currentTimeMillis();
		PreanalyzedToken token = new PreanalyzedToken();
		token.term = term;
		if (start >= 0)
			token.start = start;
		if (end >= 0)
			token.end = end;
		if (posIncr != 1)
			token.positionIncrement = posIncr;
		if (null != payload)
			// do some external Base64 encoding if payload is necessary at one
			// point
			;
		if (null != type)
			token.type = type;
		if (flags != 0)
			token.flags = Integer.toHexString(flags);

		time = System.currentTimeMillis() - time;
		tokenMapCreationTime += time;
		return token;
	}

	public String getFieldname() {
		return fieldname;
	}

	public void setFieldname(String fieldname) {
		this.fieldname = fieldname;
	}

	public void addFieldSubgenerator(FieldGenerator subgenerator) {
		subFieldGenerators.put(subgenerator.getClass(), subgenerator);
	}

	public FieldGenerator getFieldSubgenerator(Class<? extends FieldGenerator> generatorClass) {
		FieldGenerator generator = subFieldGenerators.get(generatorClass);
		return generator;
	}

	public void addInnerDocumentGenerator(FieldValueGenerator subgenerator) {
		innerDocumentGenerators.put(subgenerator.getClass(), subgenerator);
	}

	public FieldValueGenerator getInnerDocumentGenerator(Class<? extends FieldValueGenerator> generatorClass) {
		FieldValueGenerator generator = innerDocumentGenerators.get(generatorClass);
		return generator;
	}

	/**
	 * Adds <tt>featureStructure</tt> to the list of feature structures stored under
	 * <tt>key</tt>. These feature structures are meant to be used by this fields
	 * generator or by its subgenerators. The key should allow this generator to
	 * distinguish between those feature structures meant for fields generation by
	 * itself and those feature structures that should be passed to its
	 * subgenerators. A simple possibility is to use the subgenerator's class
	 * instances for canonical identification.
	 * 
	 * @param key
	 * @param featureStructure
	 */
	@Deprecated
	public void addSubgeneratorFeatureStructure(Object key, TOP featureStructure) {
		Class<?> clz = null;
		if (key instanceof Class)
			clz = (Class<?>) key;
		else
			clz = key.getClass();

		if (null == subgeneratorFS)
			subgeneratorFS = new HashMap<>();
		List<TOP> list = subgeneratorFS.get(clz);
		if (null == list) {
			list = new ArrayList<>();
			subgeneratorFS.put(clz, list);
		}
		list.add(featureStructure);
	}

	public Map<Class<?>, List<TOP>> getSubgeneratorFS() {
		return subgeneratorFS;
	}

}
