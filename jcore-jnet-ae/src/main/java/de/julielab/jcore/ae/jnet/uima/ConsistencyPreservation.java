/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
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
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReHashMapAnnotationIndex;
import de.julielab.jcore.utility.index.JCoReMapAnnotationIndex;
import de.julielab.jcore.utility.index.JCoReSetAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;

public class ConsistencyPreservation {

	private static final String COMPONENT_ID = "JNET ConsistencyPreservation";

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyPreservation.class);

	public static final String MODE_ACRO2FULL = "acro2full";
	public static final String MODE_FULL2ACRO = "full2acro";
	/**
	 * String matches will be expanded to token boundaries
	 */
	public static final String MODE_STRING = "string";
	/**
	 * If set, only create new annotations if the matched string begins and ends
	 * exactly with token borders. This avoids partial token matches which are
	 * then expanded to the whole token. Should be used for full texts.
	 */
	public static final String MODE_STRING_TOKEN_BOUNDARIES = "stringTokenBoundaries";

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
					&& (!mode.equals(MODE_STRING) && (!mode.equals(MODE_STRING_TOKEN_BOUNDARIES)))) {
				LOGGER.error("ConsistencyPreservation() - unknown mode found!");
				throw new ResourceInitializationException();
			}
			activeModes.add(mode);
		}

		LOGGER.info("ConsistencyPreservation() - modes used in consistency engine: " + activeModes.toString());
	}


	public void acroMatch(final JCas aJCas, final Set<String> entityMentionClassnames)
			throws AnalysisEngineProcessException {

		// check whether any mode enabled
		if ((activeModes == null) || (activeModes.size() == 0)
				|| !(activeModes.contains(ConsistencyPreservation.MODE_FULL2ACRO)
						|| activeModes.contains(ConsistencyPreservation.MODE_ACRO2FULL)))
			return;

		Comparator<Annotation> comparator = new Comparator<Annotation>() {

			@Override
			public int compare(Annotation o1, Annotation o2) {
				if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
					return 0;
				else if (o1.getBegin() - o2.getBegin() == 0)
					return o1.getEnd() - o2.getEnd();
				return o1.getBegin() - o2.getBegin();
			}

		};
		TreeSet<Annotation> acronyms = new TreeSet<>(comparator);
		TreeSet<Annotation> fullforms = new TreeSet<>(comparator);
		TreeSet<Annotation> entities = new TreeSet<>(comparator);

		for (Iterator<Annotation> it = aJCas.getAnnotationIndex(Abbreviation.type).iterator(); it.hasNext();) {
			Abbreviation abbreviation = (Abbreviation) it.next();
			acronyms.add(abbreviation);
			fullforms.add(abbreviation.getTextReference());
		}

		for (String entityMentionClassName : entityMentionClassnames) {
			Type entityType = aJCas.getTypeSystem().getType(entityMentionClassName);
			for (Iterator<Annotation> it = aJCas.getAnnotationIndex(entityType).iterator(); it.hasNext();)
				entities.add(it.next());
		}

		for (Iterator<Annotation> it = aJCas.getAnnotationIndex(Abbreviation.type).iterator(); it.hasNext();) {
			Abbreviation abbreviation = (Abbreviation) it.next();
			de.julielab.jcore.types.Annotation fullform = abbreviation.getTextReference();
			EntityMention abbreviationEntityMention = (EntityMention) entities.floor(abbreviation);
			EntityMention fullFormEntityMention = (EntityMention) entities.floor(fullform);

			// restrict to exact matches
			abbreviationEntityMention = abbreviationEntityMention != null
					&& comparator.compare(abbreviationEntityMention, abbreviation) == 0 ? abbreviationEntityMention
							: null;
			fullFormEntityMention = fullFormEntityMention != null
					&& comparator.compare(fullFormEntityMention, fullform) == 0 ? fullFormEntityMention : null;

			// check whether full2acro mode is enabled
			if (activeModes.contains(ConsistencyPreservation.MODE_FULL2ACRO)) {

				// If:
				// The abbreviation has no exact entity match
				// and
				// The longform HAS an exact entity match
				if (abbreviationEntityMention == null && fullFormEntityMention != null) {
					// if we found an entity mention on the full form (exact
					// match!), add first entity mention
					// to abbreviation
					final EntityMention refEntityMention = fullFormEntityMention;

					try {
						final EntityMention newEntityMention = (EntityMention) JCoReAnnotationTools
								.getAnnotationByClassName(aJCas, refEntityMention.getClass().getName());
						newEntityMention.setBegin(abbreviation.getBegin());
						newEntityMention.setEnd(abbreviation.getEnd());
						newEntityMention.setSpecificType(refEntityMention.getSpecificType());
						newEntityMention.setResourceEntryList(refEntityMention.getResourceEntryList());
						newEntityMention.setConfidence(refEntityMention.getConfidence());
						newEntityMention.setComponentId(COMPONENT_ID + " Abbrev");
						newEntityMention.addToIndexes();
					} catch (ClassNotFoundException | SecurityException | NoSuchMethodException
							| IllegalArgumentException | InstantiationException | IllegalAccessException
							| InvocationTargetException e) {
						throw new AnalysisEngineProcessException(e);
					}
				}
			}
			if (activeModes.contains(ConsistencyPreservation.MODE_ACRO2FULL)) {
				// If:
				// The long has no exact entity match
				// and
				// The abbreviation HAS an exact entity match
				if (fullFormEntityMention == null && abbreviationEntityMention != null) {
					// if we found an entity mention on the full form (exact
					// match!), add first entity mention
					// to abbreviation
					final EntityMention refEntityMention = abbreviationEntityMention;

					try {
						final EntityMention newEntityMention = (EntityMention) JCoReAnnotationTools
								.getAnnotationByClassName(aJCas, refEntityMention.getClass().getName());
						newEntityMention.setBegin(fullform.getBegin());
						newEntityMention.setEnd(fullform.getEnd());
						newEntityMention.setSpecificType(refEntityMention.getSpecificType());
						newEntityMention.setResourceEntryList(refEntityMention.getResourceEntryList());
						newEntityMention.setConfidence(refEntityMention.getConfidence());
						newEntityMention.setComponentId(COMPONENT_ID + " Abbrev");
						newEntityMention.addToIndexes();
					} catch (ClassNotFoundException | SecurityException | NoSuchMethodException
							| IllegalArgumentException | InstantiationException | IllegalAccessException
							| InvocationTargetException e) {
						throw new AnalysisEngineProcessException(e);
					}
				}
			}
		}
	}

	/**
	 * consistency presevation based on (exact) string matching. If string was
	 * annotated once as entity, all other occurrences of this string get the
	 * same label. For mode: _string_ TODO: more intelligent (voting) mechanism
	 * needed to avoid false positives
	 * 
	 * @param aJCas
	 * @param entityMentionClassnames
	 * @param confidenceThresholdForConsistencyPreservation
	 * @throws AnalysisEngineProcessException
	 */
	public void stringMatch(final JCas aJCas, final TreeSet<String> entityMentionClassnames,
			double confidenceThresholdForConsistencyPreservation) throws AnalysisEngineProcessException {

		// check whether this mode is enabled
		if ((activeModes == null) || (activeModes.size() == 0)
				|| (!activeModes.contains(ConsistencyPreservation.MODE_STRING)
						&& (!activeModes.contains(ConsistencyPreservation.MODE_STRING_TOKEN_BOUNDARIES))))
			return;

		if (activeModes.contains(ConsistencyPreservation.MODE_STRING_TOKEN_BOUNDARIES)) {
			stringMatchTokenBoundaries(aJCas, entityMentionClassnames);
			return;
		}

		final String text = aJCas.getDocumentText();

		final TypeSystem ts = aJCas.getTypeSystem();
		// This map stores the EntityMentions that share the same specificType.
		// We want to use the TreeSet to check - for a given specificType - if
		// there is already an annotation overlapping a specific text offset.
		// See the comparator below.
		final Map<String, JCoReSetAnnotationIndex<EntityMention>> overlapIndex = new HashMap<>();
		JCoReSetAnnotationIndex<Annotation> tokenIndex = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator(),
				aJCas, Token.type);

		for (final String entityMentionClassname : entityMentionClassnames) {
			// we use the index entity class wise; we don't want one class to
			// interfer with another
			overlapIndex.clear();
			try {
				// loop over all entity types to be considered
				EntityMention mentionForOffsetComparison = (EntityMention) JCoReAnnotationTools
						.getAnnotationByClassName(aJCas, entityMentionClassname);

				LOGGER.debug("doStringBased() - checking consistency for type: " + entityMentionClassname);
				final Multimap<String, EntityMention> entityMap = HashMultimap.create();

				// final EntityMention myEntity = (EntityMention)
				// JCoReAnnotationTools
				// .getAnnotationByClassName(aJCas, entityMentionClassname);
				final Type entityType = ts.getType(entityMentionClassname);
				if (null == entityType)
					throw new IllegalArgumentException(
							"Entity type \"" + entityMentionClassname + "\" was not found in the type system.");

				// loop over all entity annotations in document and put them in
				// hashmap
				LOGGER.debug("doStringBased() - building entity map");
				final Iterator<Annotation> entityIter = aJCas.getAnnotationIndex(entityType).iterator();
				while (entityIter.hasNext()) {
					final EntityMention entity = (EntityMention) entityIter.next();
					entityMap.put(entity.getCoveredText(), entity);
					// additionally, add the entities into the overlap index so
					// we can later quickly lookup whether there is already an
					// entity with the same specific type at a certain location
					String specificType = "<null>";
					if (!StringUtils.isBlank(entity.getSpecificType()))
						specificType = entity.getSpecificType();
					JCoReSetAnnotationIndex<EntityMention> set = overlapIndex.get(specificType);
					if (null == set) {
						set = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator());
						overlapIndex.put(specificType, set);
					}
					set.add(entity);

				}

				// now search for strings not detected as this kind of entity
				LOGGER.debug("doStringBased() - searching for missed entities...");
				for (final String entityString : entityMap.keySet()) {
					final EntityMention entity = entityMap.get(entityString).iterator().next();
					String specificType = "<null>";
					if (!StringUtils.isBlank(entity.getSpecificType()))
						specificType = entity.getSpecificType();
					JCoReSetAnnotationIndex<EntityMention> overlapSet = overlapIndex.get(specificType);

					LOGGER.debug("doStringBased() - checking entity string: " + entityString);

					int pos = 0;
					// start with 0 because indexOf won't find entities at
					// document beginnings otherwise
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

						length = entityString.length();
						mentionForOffsetComparison.setBegin(pos);
						mentionForOffsetComparison.setEnd(pos + length);
						boolean overlappingExists = overlapSet.contains(mentionForOffsetComparison);

						if (!overlappingExists) {
							// if there is no annotation of same type on this
							// text span yet...
							LOGGER.debug("doStringBased() - adding annotation to unlabeled entity mention");
							EntityMention refEntity = (EntityMention) JCoReAnnotationTools
									.getAnnotationByClassName(aJCas, entityMentionClassname);
							// We will not directly just annotate the found
							// string but extend it to offsets of
							// overlapped tokens.
							NavigableSet<Annotation> overlappingTokens = tokenIndex
									.searchSubset(mentionForOffsetComparison);

							Annotation firstToken = overlappingTokens.isEmpty() ? null : overlappingTokens.first();
							Annotation lastToken = overlappingTokens.isEmpty() ? null : overlappingTokens.last();
							if (activeModes.contains(MODE_STRING_TOKEN_BOUNDARIES)) {
								if (firstToken == null)
									continue;
								if (pos != firstToken.getBegin())
									continue;
								if (pos + length != lastToken.getEnd())
									continue;
							}
							int begin = overlappingTokens.size() > 0 ? overlappingTokens.first().getBegin() : pos;
							int end = overlappingTokens.size() > 0 ? overlappingTokens.last().getEnd()
									: pos + entityString.length();
							// If we would have to adjust the offsets too much,
							// we have most like just hit some
							// substring of a larger token by coincidence.
							refEntity.setBegin(begin);
							refEntity.setEnd(end);
							refEntity.setSpecificType(entity.getSpecificType());
							refEntity.setResourceEntryList(entity.getResourceEntryList());
							refEntity.setConfidence(entity.getConfidence());
							refEntity.setTextualRepresentation(entity.getTextualRepresentation());
							refEntity.setComponentId(COMPONENT_ID + " String (" + entity.getCoveredText() + ", " + begin
									+ "-" + end + ")");
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
									meanConfidence += Double.parseDouble(recognizedEntity.getConfidence());
								}
							}
							meanConfidence /= entityMap.get(entityString).size();

							int allMatches = stringMatchedEntities.size() + entityMap.get(entityString).size();
							if (entityMap.get(entityString).size() >= allMatches / 3d) {
								if (meanConfidence > confidenceThresholdForConsistencyPreservation) {
									for (EntityMention refEntity : stringMatchedEntities) {
										// we have to add the new entities to
										// the overlap-index to avoid duplicates
										// by other entities that are a
										// substring of the current entity
										overlapSet.add(refEntity);
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
							// we have to add the new entities to the
							// overlap-index to avoid duplicates by other
							// entities that are a substring of the current
							// entity
							overlapSet.add(refEntity);
							refEntity.addToIndexes();
						}
					}
				}

			} catch (final Exception e) {
				LOGGER.error("doStringBased() - exception occured: " + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			}

		}
	}

	private void stringMatchTokenBoundaries(JCas aJCas, TreeSet<String> entityMentionClassnames) {
		Set<Type> entityTypes = entityMentionClassnames.stream().map(name -> aJCas.getTypeSystem().getType(name))
				.collect(Collectors.toSet());
		JCoReMapAnnotationIndex<String, Token> tokenPrefixIndex = new JCoReHashMapAnnotationIndex<>(
				TermGenerators.edgeNGramTermGenerator(3), TermGenerators.prefixTermGenerator(3), aJCas, Token.type);

		JCoReHashMapAnnotationIndex<Integer, Token> tokenEndIndex = new JCoReHashMapAnnotationIndex<>(a -> a.getEnd(),
				a -> a.getEnd(), aJCas, Token.type);

		Map<String, JCoReSetAnnotationIndex<EntityMention>> indexMap = new HashMap<>();
		for (Type t : entityTypes) {
			indexMap.clear();
			for (Iterator<Annotation> it = aJCas.getAnnotationIndex(t).iterator(); it.hasNext();) {
				EntityMention em = (EntityMention) it.next();
				String specificType = em.getSpecificType();
				JCoReSetAnnotationIndex<EntityMention> specificIndex = indexMap.get(specificType);
				if (null == specificIndex) {
					specificIndex = new JCoReSetAnnotationIndex<>(Comparators.overlapComparator());
					indexMap.put(specificType, specificIndex);
				}
				specificIndex.add(em);
			}

			for (String specificType : indexMap.keySet()) {
				Set<String> processedEntityNames = new HashSet<>();
				JCoReSetAnnotationIndex<EntityMention> specificIndex = indexMap.get(specificType);
				new ArrayList<>(specificIndex.getIndex()).stream().forEach(entity -> {
					String entityName = entity.getCoveredText();
					if (!processedEntityNames.add(entityName))
						return;
					tokenPrefixIndex.search(entity).filter(token -> {
						return token.getEnd() - token.getBegin() <= entityName.length()
								&& entityName.startsWith(token.getCoveredText()) && !specificIndex.contains(token);
					}).map(token -> {
						// At this point we only have tokens at most as long as
						// the
						// entity name where the complete token is a prefix of
						// or
						// even the whole entity name and no entity of the same
						// specific type is overlapping the token.
						int begin = token.getBegin();
						int end = -1;
						if (token.getEnd() == begin + entityName.length()) {
							// the token is an exact match, we're finished
							end = token.getEnd();
						} else {
							Token lastToken = tokenEndIndex.get(begin + entityName.length());
							if (lastToken != null)
								end = lastToken.getEnd();
						}
						EntityMention refEntity;
						if (end >= 0 && aJCas.getDocumentText().substring(begin, end).equals(entityName)) {
							refEntity = (EntityMention) aJCas.getCas().createAnnotation(t, begin, end);
							refEntity.setBegin(begin);
							refEntity.setEnd(end);
							refEntity.setSpecificType(entity.getSpecificType());
							refEntity.setResourceEntryList(entity.getResourceEntryList());
							refEntity.setConfidence(entity.getConfidence());
							refEntity.setTextualRepresentation(entity.getTextualRepresentation());
							refEntity.setComponentId(COMPONENT_ID + " String (" + entity.getCoveredText() + ", " + begin
									+ "-" + end + ")");
							return refEntity;
						}
						return null;
					}).filter(e -> e != null && !specificIndex.contains(e)).map(e -> {
						specificIndex.add(e);
						return e;
					}).collect(Collectors.toList()).stream().forEach(e -> {
						e.addToIndexes();
					});
				});
			}
		}
	}

	@Override
	public String toString() {
		return "activeModes: " + activeModes;
	}
}
