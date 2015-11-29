/** 
 * ConsistencyPreservation.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.3.5
 * Since version:   2.2
 *
 * Helper functions for consistency preservation 
 **/

package de.julielab.jcore.ae.jnet.uima;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;

public class ConsistencyPreservation {

	private static final String COMPONENT_ID = "JNET ConsistencyPreservation";

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyPreservation.class);

	public static final String MODE_ACRO2FULL = "acro2full";
	public static final String MODE_FULL2ACRO = "full2acro";
	public static final String MODE_STRING = "string";

	private TreeSet<String> activeModes = null;

	/**
	 * builds the modes used during consistency preservation from a string which
	 * is a coma-separated list of modes.
	 * 
	 * @param tring
	 *            coma-separated list of modes to be used
	 * @throws AnalysisEngineProcessException
	 */
	public ConsistencyPreservation(final String modesString) throws ResourceInitializationException {
		activeModes = new TreeSet<String>();
		final String[] modes = modesString.split(",");
		for (final String mode2 : modes) {
			final String mode = mode2.trim();
			if (!mode.equals(MODE_ACRO2FULL) && (!mode.equals(MODE_FULL2ACRO))
					&& (!mode.equals(MODE_STRING))) {
				LOGGER.error("ConsistencyPreservation() - unknown mode found!");
				throw new ResourceInitializationException();
			}
			activeModes.add(mode);
		}

		LOGGER.info("ConsistencyPreservation() - modes used in consistency engine: "
				+ activeModes.toString());
	}

	/**
	 * this method checks whether the full form (at the position where an
	 * abbreviation was introduced) of an abbreviation is labeled as an entity.
	 * If so, and the abbreviation was not labeled as an entity, the entity
	 * label is copied to the abbreviation. As only the full form where the
	 * abbreviation was introduced is considered, this method should be run
	 * AFTER e.g. doStringBased() which makes sure that all Strings get the same
	 * entity annotation. For modes: _full2acro_ and _acro2full_
	 * 
	 * @param aJCas
	 * @param entityMentionClassnames
	 *            the entity mention class names to be considered
	 * @throws AnalysisEngineProcessException
	 */
	public void acroMatch(final JCas aJCas, final TreeSet<String> entityMentionClassnames)
			throws AnalysisEngineProcessException {

		// check whether any mode enabled
		if ((activeModes == null)
				|| (activeModes.size() == 0)
				|| !(activeModes.contains(ConsistencyPreservation.MODE_FULL2ACRO) || activeModes
						.contains(ConsistencyPreservation.MODE_ACRO2FULL)))
			return;

		// TODO needs to be checked for performance

		// make a set of Annotation objects for entity class names to be
		// considered
		// EF, 28.5.2013: Changed from TreeSet to HashSet because our UIMA
		// annotation types do not implement Comparable which is a prerequisite
		// for the usage of TreeSet. However, before Java7, there was a bug
		// allowing the first inserted element not to be Comparable. I hope it
		// wasn't important in any way that this was a TreeMap.
		// When using a TreeMap here and running on a Java7 JVM, a
		// ClassCastException (cannot cast to Comparable) would be risen.
		Set<EntityMention> entityMentionTypes = null;
		try {
			entityMentionTypes = new HashSet<EntityMention>();
			for (final String className : entityMentionClassnames)
				entityMentionTypes.add((EntityMention) JCoReAnnotationTools
						.getAnnotationByClassName(aJCas, className));
		} catch (final SecurityException e1) {
			e1.printStackTrace();
		} catch (final IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (final ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (final NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (final InstantiationException e1) {
			e1.printStackTrace();
		} catch (final IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (final InvocationTargetException e1) {
			e1.printStackTrace();
		}

		// loop over these full forms
		final JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		final Iterator<org.apache.uima.jcas.tcas.Annotation> abbrevIter = indexes
				.getAnnotationIndex(Abbreviation.type).iterator();

		while (abbrevIter.hasNext()) {
			final Abbreviation abbrev = (Abbreviation) abbrevIter.next();
			final Annotation fullFormAnnotation = abbrev.getTextReference();
			LOGGER.debug("doAbbreviationBased() - checking abbreviation: "
					+ abbrev.getCoveredText());

			final ArrayList<EntityMention> mentionList = new ArrayList<EntityMention>();

			// check whether abbreviation was identified as an entity mention of
			// interest
			for (final EntityMention mention : entityMentionTypes)
				mentionList.addAll(UIMAUtils.getAnnotations(aJCas, abbrev, mention.getClass()));
			if ((mentionList == null) || (mentionList.size() == 0)) {

				// check whether full2acro mode is enabled
				if (activeModes.contains(ConsistencyPreservation.MODE_FULL2ACRO)) {

					// if the abbreviation has no entity annotation of the types
					// of interest
					LOGGER.debug("doAbbreviationBased() -  no entity mentions of interest found on this abbreviation");

					final ArrayList<EntityMention> fullFormMentionList = new ArrayList<EntityMention>();
					for (final EntityMention mention : entityMentionTypes)
						// check whether respective full form does have an
						// entity annotation of
						// interest. Important: exact match ! Theses below...
						fullFormMentionList.addAll(UIMAUtils.getExactAnnotations(aJCas,
								fullFormAnnotation, mention.getClass()));

					if ((fullFormMentionList != null) && (fullFormMentionList.size() > 0)) {
						// if we found an entity mention on the full form (exact
						// match!), add first entity mention
						// to abbreviation
						final EntityMention refEntityMention = fullFormMentionList.get(0);
						LOGGER.debug("doAbbreviationBased() -  but found entity mention on full form");
						LOGGER.debug("doAbbreviationBased() -  adding annotation to unlabeled entity mention");
						try {
							final EntityMention newEntityMention = (EntityMention) JCoReAnnotationTools
									.getAnnotationByClassName(aJCas, refEntityMention.getClass()
											.getName());
							newEntityMention.setBegin(abbrev.getBegin());
							newEntityMention.setEnd(abbrev.getEnd());
							newEntityMention.setSpecificType(refEntityMention.getSpecificType());
							newEntityMention.setResourceEntryList(refEntityMention
									.getResourceEntryList());
							newEntityMention.setConfidence(refEntityMention.getConfidence());
							newEntityMention.setTextualRepresentation(abbrev.getCoveredText());
							newEntityMention.setComponentId(COMPONENT_ID + " Abbrev");
							newEntityMention.addToIndexes();
						} catch (final Exception e) {
							LOGGER.error("doAbbreviationBased() - could not get create new entity mention annotation: "
									+ refEntityMention.getClass().getName());
							throw new AnalysisEngineProcessException();
						}
					}
				}
			} else // check whether acro2full mode is enabled
			if (activeModes.contains(ConsistencyPreservation.MODE_ACRO2FULL))
				if (mentionList.size() > 0) {
					LOGGER.debug("doAbbreviationBased() -  abbreviation has entity mentions of interest");
					final ArrayList<EntityMention> fullFormMentionList = new ArrayList<EntityMention>();
					for (final EntityMention mention : entityMentionTypes)
						// check whether respective full form does have an
						// entity annotation of
						// interest
						fullFormMentionList.addAll(UIMAUtils.getAnnotations(aJCas,
								fullFormAnnotation, mention.getClass()));

					if ((fullFormMentionList == null) || (fullFormMentionList.size() == 0)) {
						// if full form has none, add one
						final EntityMention refEntityMention = mentionList.get(0);
						LOGGER.debug("doAbbreviationBased() -  but reference full form has no entity mentions of interest");
						LOGGER.debug("doAbbreviationBased() -  adding annotation to unlabeled entity mention");
						try {
							final EntityMention newEntityMention = (EntityMention) JCoReAnnotationTools
									.getAnnotationByClassName(aJCas, refEntityMention.getClass()
											.getName());
							newEntityMention.setBegin(fullFormAnnotation.getBegin());
							newEntityMention.setEnd(fullFormAnnotation.getEnd());
							newEntityMention.setSpecificType(refEntityMention.getSpecificType());
							newEntityMention.setResourceEntryList(refEntityMention
									.getResourceEntryList());
							newEntityMention.setConfidence(refEntityMention.getConfidence());
							newEntityMention.setTextualRepresentation(abbrev.getCoveredText());
							newEntityMention.setComponentId(COMPONENT_ID + " Abbrev");
							newEntityMention.addToIndexes();
						} catch (final Exception e) {
							LOGGER.error("doAbbreviationBased() - could not get create new entity mention annotation: "
									+ refEntityMention.getClass().getName());
							throw new AnalysisEngineProcessException();
						}
					}

				}

		}
	}

	/**
	 * consistency presevation based on (exact) string matching. If string was
	 * annotated once as entity, all other occurrences of this string get the
	 * same label. For mode: _string_ TODO: more intelligent (voting) mechanism
	 * needed to avoid false positives TODO: needs to be checked for performance
	 * 
	 * @param aJCas
	 * @param entityMentionClassnames
	 * @param confidenceThresholdForConsistencyPreservation
	 * @throws AnalysisEngineProcessException
	 */
	public void stringMatch(final JCas aJCas, final TreeSet<String> entityMentionClassnames,
			double confidenceThresholdForConsistencyPreservation)
			throws AnalysisEngineProcessException {

		// check whether this mode is enabled
		if ((activeModes == null) || (activeModes.size() == 0)
				|| !activeModes.contains(ConsistencyPreservation.MODE_STRING))
			return;

		final String text = aJCas.getDocumentText();

		final Map<String, TreeSet<EntityMention>> overlapIndex = new HashMap<>();
		final TypeSystem ts = aJCas.getTypeSystem();
		Comparator<EntityMention> overlapComparator = new Comparator<EntityMention>() {

			@Override
			public int compare(EntityMention o1, EntityMention o2) {
				int b1 = o1.getBegin();
				int e1 = o1.getEnd();
				int b2 = o2.getBegin();
				int e2 = o2.getEnd();

				if ((b1 <= b2) && (e1 >= e2)) {
					return 0;
				} else if ((b1 >= b2) && (e1 <= e2)) {
					return 0;
				}
				//
				else if ((b1 < e2) && (e1 > e2)) {
					return 0;
				} else if ((b1 < b2) && (e1 > b2)) {
					return 0;
				}
				return b1 - b2;
			}
		};

		for (final String entityMentionClassname : entityMentionClassnames) {
			// we use the index entity class wise; we don't want one class to
			// interfer with another
			overlapIndex.clear();
			try {
				// loop over all entity types to be considered
				EntityMention mentionForOffsetComparison = (EntityMention) JCoReAnnotationTools
						.getAnnotationByClassName(aJCas, entityMentionClassname);

				LOGGER.debug("doStringBased() - checking consistency for type: "
						+ entityMentionClassname);
				final Multimap<String, EntityMention> entityMap = HashMultimap.create();

				// final EntityMention myEntity = (EntityMention)
				// JCoReAnnotationTools
				// .getAnnotationByClassName(aJCas, entityMentionClassname);
				final Type entityType = ts.getType(entityMentionClassname);
				if (null == entityType)
					throw new IllegalArgumentException("Entity type \"" + entityMentionClassname
							+ "\" was not found in the type system.");

				// loop over all entity annotations in document and put them in
				// hashmap
				LOGGER.debug("doStringBased() - building entity map");
				final Iterator<Annotation> entityIter = aJCas.getAnnotationIndex(entityType)
						.iterator();
				while (entityIter.hasNext()) {
					final EntityMention entity = (EntityMention) entityIter.next();
					entityMap.put(entity.getCoveredText(), entity);
					// additionally, add the entities into the overlap index so
					// we can later quickly lookup whether there is already an
					// entity with the same specific type at a certain location
					String specificType = "<null>";
					if (!StringUtils.isBlank(entity.getSpecificType()))
						specificType = entity.getSpecificType();
					TreeSet<EntityMention> set = overlapIndex.get(specificType);
					if (null == set) {
						set = new TreeSet<>(overlapComparator);
						overlapIndex.put(specificType, set);
					}
					set.add(entity);

				}

				// now search for strings not detected as this kind of entity
				LOGGER.debug("doStringBased() - searching for missed entities...");
				for (final String entityString : entityMap.keySet()) {
					final EntityMention entity = entityMap.get(entityString).iterator().next();

					LOGGER.debug("doStringBased() - checking entity string: " + entityString);

					int pos = 0;
					int length = 0;
					List<EntityMention> stringMatchedEntities = new ArrayList<>();
					while ((pos = text.indexOf(entityString, (pos + length))) > -1) {
						// for each position where we have found this entity
						// string
						LOGGER.debug("doStringBased() - found string at pos: " + pos);

						// check whether there is already an annotation of this
						// type
						// this older approach had the issue that only one
						// overlapping annotation of entityMentionClassname was
						// returned; but this type could be the wrong one in
						// that the returned had a different specific type but
						// another existed with the same specificType as the
						// sought entity
						// EntityMention refEntity = (EntityMention)
						// JCoReAnnotationTools
						// .getOverlappingAnnotation(aJCas,
						// entityMentionClassname, pos, pos
						// + entityString.length());

						mentionForOffsetComparison.setBegin(pos);
						mentionForOffsetComparison.setEnd(pos + length);
						String specificType = "<null>";
						if (!StringUtils.isBlank(entity.getSpecificType()))
							specificType = entity.getSpecificType();
						TreeSet<EntityMention> overlapSet = overlapIndex.get(specificType);
						boolean overlappingExists = overlapSet.contains(mentionForOffsetComparison);

						// if (refEntity == null
						// || (refEntity.getSpecificType() == null ^
						// entity.getSpecificType() == null)
						// || (refEntity.getSpecificType() != null
						// && entity.getSpecificType() != null && !refEntity
						// .getSpecificType().equals(entity.getSpecificType())))
						// {
						if (!overlappingExists) {
							// if there is no annotation of same type on this
							// text span yet...
							LOGGER.debug("doStringBased() - adding annotation to unlabeled entity mention");
							EntityMention refEntity = (EntityMention) JCoReAnnotationTools
									.getAnnotationByClassName(aJCas, entityMentionClassname);
							// We will not directly just annotate the found
							// string but extend it to offsets of
							// overlapped tokens.
							List<Token> overlappingTokens = JCoReAnnotationTools
									.getNearestOverlappingAnnotations(aJCas, new Annotation(entity
											.getCAS().getJCas(), pos, pos + entityString.length()),
											Token.class);
							int begin = overlappingTokens.size() > 0 ? overlappingTokens.get(0)
									.getBegin() : pos;
							int end = overlappingTokens.size() > 0 ? overlappingTokens.get(
									overlappingTokens.size() - 1).getEnd() : pos
									+ entityString.length();
							// If we would have to adjust the offsets too much,
							// we have most like just hit some
							// substring of a larger token by coincidence.
							refEntity.setBegin(begin);
							refEntity.setEnd(end);
							refEntity.setSpecificType(entity.getSpecificType());
							refEntity.setResourceEntryList(entity.getResourceEntryList());
							refEntity.setConfidence(entity.getConfidence());
							refEntity.setTextualRepresentation(entity.getTextualRepresentation());
							refEntity.setComponentId(COMPONENT_ID + " String (" + entity.getCoveredText() + ", " + begin + "-" + end + ")");
							stringMatchedEntities.add(refEntity);

						} else
							LOGGER.debug("doStringBased() - there is already an entity!");

						length = entityString.length();
					}

					// A.R. 30.06.15: this option can now be turned on, just by
					// setting the config parameter
					// confidenceThresholdForConsistencyPreservation to a value
					// greater than 0
					// earlier it has been switched by commenting or
					// un-commenting the following code

					// If confidenceThresholdForConsistencyPreservation is given
					// (value != -1)
					// only add the new entities if there is enough evidence by
					// originally found entities with the same string that
					// this is indeed an entity we would like to find.
					if (confidenceThresholdForConsistencyPreservation > 0) {
						if (!stringMatchedEntities.isEmpty()) {

							double meanConfidence = 0;
							for (EntityMention recognizedEntity : entityMap.get(entityString)) {
								if (null != entity.getConfidence()) {
									meanConfidence += Double.parseDouble(recognizedEntity
											.getConfidence());
								}
							}
							meanConfidence /= entityMap.get(entityString).size();

							int allMatches = stringMatchedEntities.size()
									+ entityMap.get(entityString).size();
							if (entityMap.get(entityString).size() >= allMatches / 3d) {
								if (meanConfidence > confidenceThresholdForConsistencyPreservation) {
									for (EntityMention refEntity : stringMatchedEntities) {
										refEntity.addToIndexes();
									}
								}
							}
						}
					}
					// if confidence score doesn't need to be checked, just add
					// all occurrences
					else {
						for (EntityMention refEntity : stringMatchedEntities) {
							refEntity.addToIndexes();
						}
					}
				}

			} catch (final Exception e) {
				LOGGER.error("doStringBased() - exception occured: " + e.getMessage());
				throw new AnalysisEngineProcessException();
			}

		}
	}

	@Override
	public String toString() {
		return "activeModes: " + activeModes;
	}
}
