/** 
 * GeneMapperAnnotator.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 2.3.1
 * Since version:   1.0
 *
 * Creation date: Dec 11, 2006 
 * 
 * This is a wrapper to the JULIE named entity mapper (which maps found named entities to 
 * their database identifiers).
 * 
 * NEW since 1.5: disjunctive and conjunctive queries, lucene hits not accessed wit iterator any more, generic types for mapping considered
 * 
 * TODO:
 *  - clean up descriptor (remove old ones, add new ones)
 * 
 **/
package de.julielab.jules.ae.genemapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Range;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.jules.ae.genemapper.disambig.org.MeshtermOrganismFinder;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.utils.ContextUtils;
import de.julielab.jules.ae.genemapper.utils.UIMAUtils;

public class GeneMapperAnnotator extends JCasAnnotator_ImplBase {

	private static final String ORGANISM_MAPPING_FILE = "OrganismMappingFile";
	private static final String CONTEXT_WINDOW_SIZE = "ContextWindowSize";
	private static final String ABSTRACT_CONTEXT_SCOPE = "AbstractContextScope";
	private static final String TOKEN_CONTEXT = "TokenContext";
	private static final String ABBREVIATION_EXPANSION = "AbbreviationExpansion";

	private static final String COMPONENT_ID = "de.julielab.jules.ae.genemapper.GeneMapperAnnotator";
	private static final String ORG_INDEPENDENT_SOURCE = "UniProt org-independent";
	private static final String ORG_DEPENDENT_SOURCE = "UniProt org-dependent";
	public static final String TOKEN_LEVEL = "de.julielab.jules.types.Token";
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(GeneMapperAnnotator.class);
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");;
	private GeneMapper mapper = null;
	private HashMap<String, Pattern> entityMappingTypes = null;
	private MeshtermOrganismFinder organismFinder = null;
	private boolean useAbbreviationExpansion = false;

	/*
	 * the scope to use for the semantic mention index (if not specified,
	 * complete document text is used
	 */
	private String abstractContextScope = null;

	/**
	 * context window size for the disambiguation fro mapping in full texts
	 */

	private int contextWindowSize = 50;

	/*
	 * the Boolean query to be used for the semantic scorer for this document
	 */
	private BooleanQuery contextQuery = null;

	/*
	 * functions for initialization
	 */
	private Boolean tokenContext = false;
	private String orgMappingFile;

	/**
	 * initiaziation of GeneMapper: load the index, get some parameters
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		// invoke default initialization
		super.initialize(aContext);
		// instantiate mapper (given its properties file from descriptor)
		instantiateMapper(aContext);
		// get entity mapping types (given definition in descriptor)
		getEntityMappingTypes(aContext);
		// check whether abbreviations should be extended (optional parameter)
		Boolean exp = (Boolean) aContext.getConfigParameterValue(ABBREVIATION_EXPANSION);
		if (exp != null) {
			this.useAbbreviationExpansion = exp;
		}

		// get parameter for token-wise processing
		Boolean tokenContextParameter = (Boolean) aContext.getConfigParameterValue(TOKEN_CONTEXT);
		if (tokenContextParameter != null) {
			this.tokenContext = tokenContextParameter;

		}

		// set context scope for semantic mention index
		String tmp = (String) aContext.getConfigParameterValue(ABSTRACT_CONTEXT_SCOPE);

		if (tmp != null) {
			this.abstractContextScope = tmp;

		}

		// set window size
		Integer windowSize = (Integer) aContext.getConfigParameterValue(CONTEXT_WINDOW_SIZE);
		if (windowSize != null) {
			this.contextWindowSize = windowSize.intValue();
		}

		// load organism finder
		String orgMappingFile = (String) aContext.getConfigParameterValue(ORGANISM_MAPPING_FILE);
		if (orgMappingFile == null) {
			this.orgMappingFile = orgMappingFile;
			LOGGER.error("initialize() - You need to specify organism mapping file!");
			throw new ResourceInitializationException();
		}
		try {
			organismFinder = new MeshtermOrganismFinder(orgMappingFile);
		} catch (IOException e) {
			LOGGER.error("initialize() - could not load organism definition.");
			e.printStackTrace();
			throw new ResourceInitializationException();
		}
		exp = null;
		orgMappingFile = null;

		// get infos about GeNo
		if (tokenContext) {
			LOGGER.info("Token Context is on! GeNo uses tokens from window of size " + this.contextWindowSize
					+ " for mapping.");
		} else {
			LOGGER.info("AbstractContext scope " + abstractContextScope);
			if (abstractContextScope == null) {
				LOGGER.info("GeNo uses the complete jcas text for mapping.");

			} else {
				LOGGER.info("GeNo uses scope of " + abstractContextScope + "for mapping.");

			}

		}

		logConfigurationParameters();
	}

	private void logConfigurationParameters() {
		LOGGER.info("{}: {}", ABBREVIATION_EXPANSION, useAbbreviationExpansion);
		LOGGER.info("{}: {}", TOKEN_CONTEXT, tokenContext);
		LOGGER.info("{}: {}", CONTEXT_WINDOW_SIZE, contextWindowSize);
		LOGGER.info("{}: {}", ABSTRACT_CONTEXT_SCOPE, abstractContextScope);
		LOGGER.info("{}: {}", ORGANISM_MAPPING_FILE, orgMappingFile);

	}

	/**
	 * get the types (uima annotatins) to be mapped from the descriptor. Fills a
	 * hashmap with entity types and regexp patterns which will be applied to
	 * the specificType attribute. each line in the String array entTypes is
	 * assumed to have the following format: <class-name-of-entity>= <specTypes>
	 * where the specTypes should be delimited with a "|"
	 * 
	 * @param aContext
	 * @throws ResourceInitializationException
	 */
	private void getEntityMappingTypes(UimaContext aContext) throws ResourceInitializationException {
		String[] entTypes = (String[]) aContext.getConfigParameterValue("EntityMappingTypes");
		if (entTypes != null) {
			entityMappingTypes = new HashMap<String, Pattern>();
			for (int i = 0; i < entTypes.length; i++) {
				String[] entDefinition = entTypes[i].split("=");
				if (entDefinition.length != 2) {
					LOGGER.error("initialize() - EntityMappingTypes in wrong format: " + entTypes[i]);
				}
				String entName = entDefinition[0];
				Pattern entSpecificPattern = Pattern.compile(entDefinition[1]);
				entityMappingTypes.put(entName, entSpecificPattern);
			}
			LOGGER.info("initialize() - entity types to be considered for mapping: " + entityMappingTypes);
		} else {
			LOGGER.error("initialize() - no entity mapping types defined.");
			throw new ResourceInitializationException();
		}
		entTypes = null;
	}

	/**
	 * Gets parameters for mapper and instantiates one. Make an instance of the
	 * gene mapper to be used first read mapping configuration from aContext
	 * then read type of mapper from aContext
	 */
	private void instantiateMapper(UimaContext aContext) throws ResourceInitializationException {
		// PARAM: get mapper configuration properties
		String mapperConfigFile = (String) aContext.getConfigParameterValue("MapperConfigFile");
		if (mapperConfigFile != null) {
			try {
				mapper = new GeneMapper(new File(mapperConfigFile));
			} catch (CorruptIndexException e) {
				LOGGER.error("initialize() - Error initializing gene mapper: index corrup: " + e.getMessage());
				throw new ResourceInitializationException(e);
			} catch (Exception e) {
				LOGGER.error("initialize() - Error initializing gene mapper: " + e.getMessage());
				throw new ResourceInitializationException(e);
			}
		} else {
			LOGGER.error("initialize() - Error initializing gene mapper: no config file for mapper specified.");
			throw new ResourceInitializationException();
		}
		mapperConfigFile = null;
	}

	/*
	 * functions that do the mapping
	 */
	/**
	 * The process method. loop over all entity types to be considered and do a
	 * mapping for all entities of this type
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		// get infos about GeNo
		if (tokenContext) {
			LOGGER.debug("Token Context is on! GeNo uses tokens from window of size " + +contextWindowSize
					+ " for mapping.");
		} else {
			LOGGER.debug("AbstractContext scope " + abstractContextScope);
			if (abstractContextScope == null) {
				LOGGER.debug("GeNo uses the complete jcas text for mapping.");

			} else {
				LOGGER.debug("GeNo uses scope of " + abstractContextScope + "for mapping.");

			}
		}

		// reset stuff when going to new document
		this.contextQuery = null;

		// get the abstract text and the context query (used later in the map
		// method)
		try {

			// if abstract text or the complete text is used for mapping
			if (!tokenContext) {
				contextQuery = ContextUtils.makeContextQuery(aJCas, this.abstractContextScope);

				if (contextQuery != null) {
					// get organisms for document
					TreeSet<String> organisms = organismFinder.getOrganismsForDocument(aJCas);
					// loop over entity mapping types and do the mapping
					for (Iterator iter = entityMappingTypes.keySet().iterator(); iter.hasNext();) {
						String mappingEntityTypeName = (String) iter.next();
						LOGGER.debug("doing mapping for entities of type '" + mappingEntityTypeName + "'");
						// get all annotations of this type
						EntityMention mappingEntityType = getEntityByTypeName(aJCas, mappingEntityTypeName);
						Pattern specTypePattern = entityMappingTypes.get(mappingEntityTypeName);
						JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
						Iterator<Annotation> entityIterator = indexes
								.getAnnotationIndex(mappingEntityType.getTypeIndexID()).iterator();
						while (entityIterator.hasNext()) {
							EntityMention entity = (EntityMention) entityIterator.next();
							String specificType = entity.getSpecificType();
							// check whether this specificType is allowed
							if (specTypePattern.matcher(specificType).matches()) {
								LOGGER.debug("mapping next gene mention: " + entity.getCoveredText() + " ("
										+ mappingEntityTypeName + "|" + specificType + ")");
								// now do the mapping itself
								doMapping(aJCas, entity, organisms);
							}
						}

					}

				} else {
					LOGGER.error("no mapping performed on this document as no context found!");
				}

			}
			// if token-wise context generation should be done
			else if (tokenContext) {
				TreeSet<String> organisms = organismFinder.getOrganismsForDocument(aJCas);

				for (Iterator iter = entityMappingTypes.keySet().iterator(); iter.hasNext();) {
					String mappingEntityTypeName = (String) iter.next();

					LOGGER.debug("doing token-wise mapping for entities of type '" + mappingEntityTypeName + "'");
					// get all annotations of this type
					EntityMention mappingEntityType = getEntityByTypeName(aJCas, mappingEntityTypeName);
					Pattern specTypePattern = entityMappingTypes.get(mappingEntityTypeName);
					JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
					Iterator<Annotation> entityIterator = indexes.getAnnotationIndex(mappingEntityType.getTypeIndexID())
							.iterator();
					while (entityIterator.hasNext()) {
						EntityMention entity = (EntityMention) entityIterator.next();
						String specificType = entity.getSpecificType();
						LOGGER.debug("next entity " + entity.getSpecificType());
						// check whether this specificType is allowed
						if (specTypePattern.matcher(specificType).matches()) {
							LOGGER.debug("process() - mapping next gene mention: " + entity.getCoveredText() + " ("
									+ mappingEntityTypeName + "|" + specificType + ")");

							contextQuery = ContextUtils.makeContextQuery(aJCas, this.abstractContextScope,
									contextWindowSize, entity);

							if (contextQuery != null) {
								doMapping(aJCas, entity, organisms);
							} else {
								LOGGER.warn("process() - no mapping performed on this document as no context found!");
							}
						}
					}

					mappingEntityType = null;
					mappingEntityTypeName = null;
					specTypePattern = null;
					indexes = null;
					entityIterator = null;
				}

				organisms = null;

			}
		} catch (IOException e) {
			String info = "Error generating the boolean context query";
			AnalysisEngineProcessException e1 = new AnalysisEngineProcessException(e);
			LOGGER.error(info, e);
			throw e1;
		}
	}

	/**
	 * performs the mapping (search and write to cas) First, organism
	 * independent mapping is done. Then, the found IDs are mapped to
	 * organism-dependent ones (different organism disambiguators can be used).
	 * Finally, both mappings are merged and written to the CAS.
	 * 
	 * @param searchTermVariants
	 *            Several variants of the search term, constructed by resolving
	 *            abbreviations. Currently, only the first search term is mapped
	 *            (this one contains the full form if available). The short form
	 *            is not mapped as this is typically to ambiguous and might
	 *            bring to many false positives
	 */
	private void doMapping(JCas aJCas, EntityMention entity, TreeSet<String> organisms)
			throws AnalysisEngineProcessException {
		LOGGER.debug("do mapping for " + entity.getCoveredText() + ":" + entity.getSpecificType());
		// JFSIndexRepository indexes = aJCas.getJFSIndexRepository();

		String searchTerm = entity.getCoveredText();
		if (useAbbreviationExpansion) {
			searchTerm = getFullform(aJCas, entity);
		}
		ArrayList<SynHit> allHits = new ArrayList<>();

		try {
			// allHits = mapper.map(searchTerm, aJCas.getDocumentText(),
			// organisms);
			// allHits = mapper.map(searchTerm, contextQuery, organisms);
			GeneMention predictedGeneMention = new GeneMention(searchTerm);
			predictedGeneMention.setOffsets(Range.between(entity.getBegin(), entity.getEnd()));
			String documentContext = tokenContext
					? ContextUtils.makeContext(aJCas, this.abstractContextScope, contextWindowSize, entity)
					: aJCas.getDocumentText();
			LOGGER.debug("Trying to map gene mention {} at offsets {}", entity.getCoveredText(),
					entity.getBegin() + "-" + entity.getEnd());
			MentionMappingResult map = mapper.map(predictedGeneMention, contextQuery, documentContext);
					LOGGER.debug("Mapped gene mention {} at offsets {} to ID {}",
							new Object[] { entity.getCoveredText(), entity.getBegin() + "-" + entity.getEnd(),
									map.resultEntry.getId() });
			if (map.resultEntry == MentionMappingResult.REJECTION)
				LOGGER.debug("No fitting database entry was found for gene mention {} at offsets {}. This mention will not be assigned a database ID.",
						entity.getCoveredText(), entity.getBegin() + "-" + entity.getEnd());
			else
				allHits.add(map.resultEntry);
		} catch (BooleanQuery.TooManyClauses e) {
			LOGGER.error(
					"Entity was too long to query for candidates. This gene mention is rejected as it seems unprobably that a single gene mention would be that long: {}",
					entity.getCoveredText());
			return;
		} catch (Exception e) {
			LOGGER.error("mapping failed!");
			e.printStackTrace();
			// throw new
			// AnalysisEngineProcessException(AnalysisEngineProcessException.ANNOTATOR_EXCEPTION,
			// null);
			throw new AnalysisEngineProcessException(e);
		}

		// write hits

		String normalizedSearchTerm = mapper.getMappingCore().getTermNormalizer().normalize(searchTerm);
		// remove (partly/completely) modifiers, domains, non-descriptives
		normalizedSearchTerm = GeneMapper.removeModifiers(normalizedSearchTerm);
		writeMappingsToCAS(aJCas, entity, allHits, normalizedSearchTerm);
		searchTerm = null;
	}

	/**
	 * write the found hits as DB entries to the CAS. If we are doing an entity
	 * mention mapping, tie these DB entries to their entity annotation.
	 * 
	 * @param aJCas
	 *            where to write it
	 * @param annotation
	 *            the annotation type used during the mapping (e.g. an Entity,
	 *            or NP, ...)
	 * @param allHits
	 *            the list of hits (mappings) found
	 * @param normalizedSearchTerm
	 */
	private void writeMappingsToCAS(JCas aJCas, EntityMention entity, ArrayList<SynHit> allHits,
			String normalizedSearchTerm) {
		if (allHits == null || allHits.size() == 0) {
			LOGGER.debug("writeMappingsToCAS() - no hits available for writing");
			return;
		}
		if (allHits.size() > 1)
			throw new IllegalStateException();
		LOGGER.debug("writeMappingsToCAS() - writing " + allHits.size() + " organism specific hits...");
		ArrayList<ResourceEntry> allDBEntries = new ArrayList<ResourceEntry>();
		// HashMap<String, SynHit> orgIndependentHits = new HashMap<String,
		// SynHit>();
		for (SynHit hit : allHits) {
			// organism dependent hits
			GeneResourceEntry resourceEntry = new GeneResourceEntry(aJCas);
			resourceEntry.setSource("WRITE SOURCE INTO INDEX");
			String myID = hit.getId();

			resourceEntry.setEntryId(myID);
			resourceEntry.setTaxonomyId(hit.getTaxId());
			resourceEntry.setBegin(entity.getBegin());
			resourceEntry.setEnd(entity.getEnd());
			resourceEntry.setComponentId(COMPONENT_ID);
			String confidence = DECIMAL_FORMAT.format(hit.getMentionScore()) + " / "
					+ DECIMAL_FORMAT.format(hit.getSemanticScore());
			resourceEntry.setConfidence(confidence);
			resourceEntry.setId(normalizedSearchTerm);
			resourceEntry.setSynonym(hit.getSynonym());
			resourceEntry.addToIndexes();
			allDBEntries.add(resourceEntry);

			// add to orgIndependentIndex
			// String id = getOrganismIndependentID(hit.getId());
			// if (id != null) {
			// hit.setId(id);
			// orgIndependentHits.put(id, hit);
			// }

		}

		// We don't do that anymore.
		// LOGGER.debug("writeMappingsToCAS() - writing "
		// + orgIndependentHits.size() + " organism UNspecific hits...");
		// for (Iterator iterator = orgIndependentHits.keySet().iterator();
		// iterator
		// .hasNext();) {
		// // organism independent hits
		// String id = (String) iterator.next();
		// SynHit hit = orgIndependentHits.get(id);
		// ResourceEntry resourceEntry = new ResourceEntry(aJCas);
		// resourceEntry.setSource(ORG_INDEPENDENT_SOURCE);
		// resourceEntry.setEntryId(hit.getId());
		// resourceEntry.setBegin(entity.getBegin());
		// resourceEntry.setEnd(entity.getEnd());
		// resourceEntry.setComponentId(COMPONENT_ID);
		// String confidence = DECIMAL_FORMAT.format(hit.getMentionScore());
		// resourceEntry.setConfidence(confidence);
		// resourceEntry.setId(normalizedSearchTerm);
		// resourceEntry.addToIndexes();
		// allDBEntries.add(resourceEntry);
		// }

		// get overall number of entries

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(
					"Adding {} new resource {} (ID{}: {}) to gene {} (offsets: {}). This gene had {} resource {} before.",
					new Object[] { allDBEntries.size(), allDBEntries.size() == 1 ? "entry" : "entries",
							allDBEntries.size() == 1 ? "" : "s", StringUtils
									.join(Collections2.transform(allDBEntries, new Function<ResourceEntry, String>() {
										@Override
										public String apply(ResourceEntry input) {
											return input.getEntryId();
										}
									}), ", "),
							entity.getCoveredText(), entity.getBegin() + "-" + entity.getEnd(),
							entity.getResourceEntryList() == null ? 0 : entity.getResourceEntryList().size(),
							entity.getResourceEntryList() != null && entity.getResourceEntryList().size() == 1 ? "entry"
									: "entries" });

		FSArray resourceEntryList = entity.getResourceEntryList();
		if (null == resourceEntryList && allDBEntries.size() > 0)
			resourceEntryList = new FSArray(aJCas, allDBEntries.size());
		FSArray newEntryList = JCoReTools.addToFSArray(resourceEntryList, allDBEntries);

		// only for debugging
		// System.out.println("-------- new gene -----------");
		// System.out.print("New entry list: ");
		// JulesTools.printFSArray(newEntryList);
		// for (int i = 0; i < newEntryList.size(); i++) {
		// System.out.println("Is the Resource Entry null: " +
		// (newEntryList.get(i) == null));
		// System.out.println("Were there old entries? " +
		// (entity.getResourceEntryList() != null));
		//
		// }
		entity.setResourceEntryList(newEntryList);

		// // int numEntries = allDBEntries.size();
		// // if (oldResourceEntries != null) {
		// // numEntries += oldResourceEntries.size();
		// // }
		// //
		// // // now add all hits to FSArray
		// // FSArray resourceEntryList = new FSArray(aJCas, numEntries);
		// // int startPos = 0;
		// // if (oldResourceEntries != null) {
		// // for (int i = 0; i < oldResourceEntries.size(); i++) {
		// // resourceEntryList.set(i, oldResourceEntries.get(i));
		// // }
		// // startPos = oldResourceEntries.size();
		// // }
		// //
		// // // now also copy old entries
		// // for (int i = startPos; i < allDBEntries.size(); i++) {
		// // resourceEntryList.set(i, allDBEntries.get(i));
		// // }
		//
		// System.out.println("-------- new gene -----------");
		// for (int i = 0; i < resourceEntryList.size(); i++) {
		// System.out.println("Is the Resource Entry null: " +
		// (resourceEntryList.get(i) == null));
		// System.out.println("Were there old entries? " + (oldResourceEntries
		// != null));
		//
		// }
		//
		// allDBEntries = null;
		// // orgIndependentHits = null;
		// entity.setResourceEntryList(resourceEntryList);
	}

	// /**
	// * returns the organism independent part of a ID
	// *
	// * @param id
	// * the organism dependent id
	// * @return
	// */
	// private String getOrganismIndependentID(String id) {
	// if (id.contains(MeshtermOrganismFinder.COMBINER)) {
	// return id.substring(0, id.indexOf(MeshtermOrganismFinder.COMBINER));
	// }
	// // unexpected
	// return null;
	// }

	/**
	 * Gets a textual representation for the entity where abbreviations/acronyms
	 * are replaced by their full forms if introduced earlier in text before.
	 * TODO: at the moment this function works but is a bit hacky (only first
	 * abbreviation within entity is considered this is for entity mapping only
	 * (would need some adaption to be used for generic types)).
	 * 
	 * @param indexes
	 *            the CASes annotation index
	 * @param entity
	 *            the entity annotation type to find the full form for
	 * @return the new full form as a string. If no special full form can be
	 *         found, the original covered text is returned.
	 */
	public String getFullform(JCas jcas, EntityMention entity) {

		// get tokens and abbreviations lying in this entity
		ArrayList<Token> entityTokens = (ArrayList<Token>) UIMAUtils.getAnnotations(jcas, entity,
				(new Token(jcas, 0, 0)).getClass());
		ArrayList<Abbreviation> entityAbbreviations = getAbbreviations(entity, jcas);

		if (entityTokens.size() == 0) {
			// This can happen if entity is annotated on subtoken level (e.g.
			// because of consistency
			// engine). Original entity string added as only variant.
			LOGGER.warn("getFullform() - no token for this entity found.");
			return entity.getCoveredText();
		}
		if (entityAbbreviations.size() == 0) {
			// stop if no abbreviation is contained
			LOGGER.debug("getFullform() - no abbreviations contained in this entity.");
			return entity.getCoveredText();
		}
		// TODO use all abbreviations, not just first one
		Abbreviation abbreviation = entityAbbreviations.get(0);
		Range<Integer> entityRange = Range.between(entity.getBegin(), entity.getEnd());
		Range<Integer> abbreviationRange = Range.between(abbreviation.getBegin(), abbreviation.getEnd());
		Range<Integer> expanRange = Range.between(abbreviation.getTextReference().getBegin(),
				abbreviation.getTextReference().getEnd());
		LOGGER.debug("getFullform() - handling abbreviation: " + abbreviation.getCoveredText() + " for: "
				+ abbreviation.getExpan());
		// check cases
		// if (entity.getBegin() == abbreviation.getBegin() && entity.getEnd()
		// == abbreviation.getEnd()) {
		if (entityRange.equals(abbreviationRange)) {
			// 1.) complete entity is an abbreviation
			// Look whether there is an entity within the fullform of the
			// abbreviation.

			// ArrayList<Abbreviation> abbreviationsInEntity =
			// getAbbreviations(entity, jcas);
			// if (abbreviationsInEntity.size() == 0) {
			// LOGGER.debug("No abbreviations contained in this entity.");
			// return entity.getCoveredText();
			// }
			// TODO what if there are multiple abbreviations? Search for
			// examples!
			// Abbreviation abbreviation = abbreviationsInEntity.get(0);
			de.julielab.jcore.types.Annotation longFormAnnotation = abbreviation.getTextReference();
			List<? extends EntityMention> includedAnnotations = JCoReAnnotationTools.getIncludedAnnotations(jcas,
					longFormAnnotation, entity.getClass());
			if (includedAnnotations.size() == 0) {
				LOGGER.debug(
						"No entity mentions found in abbreviation long form, returning the whole fullform itself.");
				return longFormAnnotation.getCoveredText();
			}
			// If there are multiple ConceptMentions found in the full form,
			// take that largest right-most candidate.
			ConceptMention emFullform = null;
			int maxSize = 0;
			for (ConceptMention em : includedAnnotations) {
				if (em.getEnd() - em.getBegin() > maxSize)
					emFullform = em;
			}
			LOGGER.debug("Found an concept mention within the abbreviation long form, returning its text: {}",
					emFullform.getCoveredText());
			return emFullform.getCoveredText();

			// LOGGER.debug("getFullform() - case: complete entity is
			// abbreviation");
			// return abbreviation.getExpan();
		} else {
			// 2.) abbreviation is only part of the entity
			LOGGER.debug("getFullform() - case: abbreviation is only part of entity");
			// check for brackets around abbreviation
			// left and right string corresponding to left and right token below
			String leftString = "";
			String rightString = "";
			// Token abbrevToken = null;
			// left token is the token left to the (one) abbreviation token,
			// right token analogous
			Token leftToken = null;
			Token rightToken = null;
			for (int i = 0; i < entityTokens.size(); i++) {
				// search for the abbreviation token
				Token token = entityTokens.get(i);
				if (token.getCoveredText().equals(abbreviation.getCoveredText())
						&& token.getBegin() == abbreviation.getBegin() && token.getEnd() == abbreviation.getEnd()) {
					// abbrevToken = token;
					// get its left and right token (if available)
					if (i > 0) {
						leftToken = entityTokens.get(i - 1);
						leftString = leftToken.getCoveredText();
					}
					if (i + 1 < entityTokens.size()) {
						rightToken = entityTokens.get(i + 1);
						rightString = rightToken.getCoveredText();
					}
					break;
				}
			}
			// check whether expanded text is found before abbreviation
			boolean fullFormContained = false;
			// int abbrevStart =
			// entity.getCoveredText().indexOf(abbreviation.getCoveredText());
			// int expanStart =
			// entity.getCoveredText().indexOf(abbreviation.getExpan());
			// int expanEnd = -1;
			// if (expanStart > -1) {
			// expanEnd = expanStart + abbreviation.getExpan().length();
			// // System.out.println("start: " + expanStart + "- end: " +
			// // expanEnd);
			// // TODO expanEnd at this point always > -1
			// if (expanEnd > -1 && expanEnd < abbrevStart) {
			// fullFormContained = true;
			// LOGGER.debug("getFullform() - full form contained before
			// abbreviation in brackets");
			// }
			// }

			if (entityRange.containsRange(expanRange) && expanRange.getMaximum() < abbreviationRange.getMinimum()) {
				fullFormContained = true;
				LOGGER.debug("getFullform() - full form contained before abbreviation in brackets");
			}

			if (fullFormContained && leftToken != null && rightToken != null && !leftString.equals("")
					&& !rightString.equals("") && (leftString.equals("(") || leftString.equals("["))
					&& (rightString.equals(")") || rightString.equals("]"))) {
				// 2.1) ABBREVIATION IN BRACKETS AND FULL FORM BEFORE IT
				// --> make 2 variants: full form (replace abbreviation by full
				// form (in else below)),
				// abbreviated form (remove full form)
				LOGGER.debug("getFullform() - case: local abbreviation introduced...");
				// now make two variants: long and short form
				String longForm = entity.getCoveredText().replace(abbreviation.getCoveredText(), " ").trim();
				// String s =
				// entity.getCoveredText().replace(abbreviation.getExpan(),
				// " ").trim();
				return longForm;
			} else {
				// 2.2) NO BRACKETS AROUND ABBREVIATION OR FULL FORM NOT FOUND
				// IN
				// TEXT
				// --> just replace abbreviation by full form
				LOGGER.debug("getQueryVariants() - case: previously introduced abbreviation used...");
				// now make two variants: long and short form
				String longForm = entity.getCoveredText()
						.replace(abbreviation.getCoveredText(), abbreviation.getExpan()).trim();
				// String s = entity.getCoveredText().trim();
				return longForm;
			}
		}

	}

	/*
	 * helper functions
	 */

	/**
	 * get annotation type for entity class name
	 * 
	 * @param aJCas
	 *            the CAS holding the type system for which the entity type
	 *            should be created (method throws an error if the given entity
	 *            class name is not contained in type system)
	 * @param entityClassName
	 *            the class name of the entity annotation object
	 */
	private EntityMention getEntityByTypeName(JCas aJCas, String entityClassName)
			throws AnalysisEngineProcessException {
		EntityMention myEntity = null;
		try {
			Class[] parameterTypes = new Class[] { JCas.class };
			Class myNewClass;
			myNewClass = Class.forName(entityClassName);
			Constructor myConstructor = myNewClass.getConstructor(parameterTypes);
			myEntity = (EntityMention) myConstructor.newInstance(aJCas);
		} catch (ReflectiveOperationException e) {
			LOGGER.error("getEntityByTypeName() - entity type: " + entityClassName + " not contained in type system!");
			throw new AnalysisEngineProcessException(AnalysisEngineProcessException.ANNOTATOR_EXCEPTION, null);
		}
		return myEntity;
	}

	/**
	 * retrieve a list of abbreviations at a given offsets and for a specific
	 * index. I intentionally don't use the subiterator due to type priority
	 * intricacies. Instead, I use Philip Ogrens functions which do something
	 * very similar and work well. Specialities: ## The abbreviations are sorted
	 * by their length so that longer abbreviations are put higher ##
	 * Abbreviations of length>6 are ignored (to avoid that we use false
	 * positives for mapping)
	 */
	private ArrayList<Abbreviation> getAbbreviations(EntityMention entity, JCas jcas) {

		// TODO: check that this doesn't constantly create a new Abbreviation
		// type
		ArrayList<Abbreviation> abbrevList = (ArrayList<Abbreviation>) UIMAUtils.getAnnotations(jcas, entity,
				(new Abbreviation(jcas, 0, 0)).getClass());

		// sort abbreviations so that longest is first
		Collections.sort(abbrevList, new Comparator<Abbreviation>() {

			public int compare(Abbreviation a1, Abbreviation a2) {
				int c = (new Integer(a2.getEnd() - a2.getBegin()).compareTo(a1.getEnd() - a1.getBegin()));
				return c;
			}
		});

		// remove overlong abbreviations
		ArrayList<Abbreviation> newAbbrev = new ArrayList<Abbreviation>();
		for (Abbreviation a : abbrevList) {
			if (a.getExpan().split("[\\s]+").length > 6) {
				LOGGER.debug("getAbbreviations() - skipping abbreviation with overlong full form : " + a.getExpan());
			} else {
				newAbbrev.add(a);
			}
		}
		abbrevList = null;
		return newAbbrev;
	}
}
