 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the BSD-2-Clause License
  */

package de.julielab.jcore.reader.bionlpformat.utils;

 import com.google.common.collect.Lists;
 import de.julielab.jcore.reader.bionlpformat.main.FormatClashException;
 import de.julielab.jcore.types.*;
 import de.julielab.jcore.utility.JCoReTools;
 import org.apache.uima.jcas.JCas;
 import org.apache.uima.jcas.cas.FSArray;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.io.BufferedReader;
 import java.io.IOException;
 import java.util.List;
 import java.util.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

/**
 * //TODO describe purpose of class
 * 
 * @author buyko
 */
public class AnnotationFileMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationFileMapper.class);
	private static final String PROTEIN = "Protein";
	private static final String PROTEIN_SPECIFIC_TYPE = "protein";
	private static final String ENTITY_MENTION_SPECIFIC_TYPE = "entity";
	private static final String COREF_EXPRESSION = "Exp";
	private static final String ENTITY = "Entity";
	private static final String THEME = "Theme";
	private static final String THEME2 = "Theme2";
	private static final String CAUSE = "Cause";

	private int numCorefRelations = 0;
	private int numCorefExpressions = 0;

	public void mapEventFile(Map<String, Annotation> mappedProteins, BufferedReader bufferedReader, JCas cas)
			throws IOException {
		Map<String, Annotation> mappedAnnotations = new HashMap<String, Annotation>();
		if (mappedProteins != null)
			mappedAnnotations.putAll(mappedProteins);
		mapFile(mappedAnnotations, bufferedReader, cas);
	}

	public Map<String, Annotation> mapProteinFile(BufferedReader bufferedReader, JCas cas) throws IOException {
		Map<String, Annotation> mappedProteins = new HashMap<String, Annotation>();
		mapFile(mappedProteins, bufferedReader, cas);
		return mappedProteins;
	}

	private void mapFile(Map<String, Annotation> mappedAnnotations, BufferedReader bufferedReader, JCas cas)
			throws IOException {
		// List<String> entries = new ArrayList<String>();
		Map<String, String> eventEntries = new HashMap<String, String>();
		List<String> corefEntries = new ArrayList<>();
		Collection<Collection<String>> equivalents = new ArrayList<Collection<String>>();
		String line = null;

		while ((line = bufferedReader.readLine()) != null) {
			if (line.startsWith("T"))
				mapEntity(mappedAnnotations, line, cas);
			else if (line.startsWith("E"))
				eventEntries.put(line.substring(0, line.indexOf("\t")), line);
			else if (line.startsWith("*	Equiv")) {
				String equivIDs = line.substring(line.indexOf("Equiv") + 5).trim();
				equivalents.add(Lists.newArrayList(equivIDs.split(" ")));
			} else if (line.startsWith("R")) {
				corefEntries.add(line);
			}
			// entries.add(line);
		}
		for (String eventID : eventEntries.keySet()) {
			if (!mappedAnnotations.keySet().contains(eventID))
				mapEventEntry(mappedAnnotations, eventEntries, eventID, cas);
		}
		for (Collection<String> equiv : equivalents)
			mapEquivalents(mappedAnnotations, equiv, cas);
		for (String corefEntry : corefEntries)
			mapCorefRelation(mappedAnnotations, corefEntry, cas);

		LOGGER.trace("Number of coreference expressions: {}", getNumCorefExpressions());
		LOGGER.trace("Number of coreference relations: {}", getNumCorefRelations());
	}

	private void mapCorefRelation(Map<String, Annotation> mappedAnnotations, String corefEntry, JCas cas) {
		// example:
		// R1 Coref Anaphora:T28 Antecedent:T26 Antecedent2:T27 [T5, T4]
		// with a variable number of antecedents and a variable number of entity IDs within the antecedents in the
		// brackets
		String corefId = corefEntry.split("\\t", 2)[0];
		Pattern relationPattern = Pattern.compile("Coref(?:erence)? (?:Anaphora|Subject):(T[0-9]+)( (?:Antecedent|Object)[0-9]*:T[0-9]+)+");
		Matcher relationMatcher = relationPattern.matcher(corefEntry);

		Matcher idMatcher = Pattern.compile("T[0-9]+").matcher("");

		try {
			if (relationMatcher.find()) {
				// here we get this part: Coref Anaphora:T28 Antecedent:T26 Antecedent2:T27
				String relationString = relationMatcher.group();
				CorefExpression anaphora = null;
				idMatcher.reset(relationString);
				if (idMatcher.find()) {
					String anaphoraId = idMatcher.group();
					anaphora = createCorefExpression(anaphora, mappedAnnotations.get(anaphoraId), cas, false);
				} else {
					throw new FormatClashException(relationString);
				}
				// there might be multiple antecedents
				List<CorefExpression> antecedents = new ArrayList<>();
				while (idMatcher.find()) {
					String antecedentId = idMatcher.group();
					CorefExpression antecedent = null;
					CorefExpression antecedentExpression = createCorefExpression(antecedent, mappedAnnotations.get(antecedentId), cas, true);
					// We do only create a new Antecedent annotation if it has not been created yet. Remember that the
					// coreference expressions - anaphoras and antecedents - are first created as plain Annotation
					// objects because the .a2 file does not specify directly which function a specific expression has
					// (anaphora or antecedent), especially because could be actually be both. We however break
					// everything up into anaphoras and antecedants with the respective CorefRelation between them. That
					// also means that a single coreference expression can result in two annotations at the same place,
					// one anaphor and one antecedent annotation, if the expression serves both functions, e.g. in an
					// anaphoric chain.
					// if (antecedentExpression.getClass().equals(Annotation.class)) {
					// antecedentExpression =
					// new Antecedent(cas, antecedentExpression.getBegin(), antecedentExpression.getEnd());
					// antecedentExpression.setId(antecedentId);
					// antecedentExpression.addToIndexes();
					// mappedAnnotations.put(antecedentId, antecedentExpression);
					// }
					antecedents.add(antecedentExpression);
				}
				// add all the antecedents to the list for the anaphora
				FSArray antecedentArray = new FSArray(cas, antecedents.size());
				for (int i = 0; i < antecedents.size(); i++) {
					CorefExpression antecedent = antecedents.get(i);
					antecedentArray.set(i, antecedent);
				}
				CorefRelation corefRel = new CorefRelation(cas);
				++numCorefRelations;
				corefRel.setId(corefId);
				// due to the BioNLP Shared Task 2011 co-reference data we model coreferences as n-ary relations instead
				// of multiple binary or coreference chains.
				corefRel.setAntecedents(antecedentArray);
				corefRel.setAnaphora(anaphora);
				// avoid multiple references to the same FSArray
				anaphora.setAntecedentRelation(corefRel);
				for (int i = 0; i < antecedents.size(); i++) {
					CorefExpression antecedent = antecedents.get(i);
					antecedent
							.setAnaphoraRelations(JCoReTools.addToFSArray(antecedent.getAnaphoraRelations(), corefRel));
				}
			} else {
				throw new FormatClashException(corefEntry);

			}
		} catch (FormatClashException e) {
			LOGGER.error(
					"Expression {} could not be parsed correctly as coreference relation, check the format and correct the error.",
					e.getMessage());
			throw new IllegalStateException("Line " + corefEntry
					+ " could not be parsed correctly as coreference relation, check the format and correct the error.");
		}
	}

	private CorefExpression createCorefExpression(CorefExpression coref, Annotation trigger, JCas cas, boolean isAntecedent) {
		coref = new CorefExpression(cas);
		coref.setBegin(trigger.getBegin());
		coref.setEnd(trigger.getEnd());
		coref.setId(trigger.getId());
		if (!isAntecedent) {
			coref.setIsAnaphor(true);
		} else {
			coref.setIsAntecedent(true);
		}
		coref.addToIndexes();
		return coref;
	}

	private void mapEquivalents(Map<String, Annotation> mappedAnnotations, Collection<String> equiv, JCas cas) {
		Entity entity = new Entity(cas);
		entity.setBegin(0);
		entity.setEnd(0);

		FSArray mentions = new FSArray(cas, equiv.size());

		int i = 0;
		for (String id : equiv) {
			EntityMention entityMention = (EntityMention) mappedAnnotations.get(id);
			entityMention.setRef(entity);
			mentions.set(i++, entityMention);
		}

		entity.setMentions(mentions);
		entity.addToIndexes(cas);
	}

	private void mapEventEntry(Map<String, Annotation> mappedAnnotations, Map<String, String> eventEntries,
			String eventID, JCas cas) {
		String[] headAndTail = eventEntries.get(eventID).split("\t");
		String id = headAndTail[0];
		String tail = headAndTail[1];
		String[] tokens = tail.split("\\p{Blank}+");
		String triggerID = tokens[0].split(":")[1];
		EventTrigger trigger = (EventTrigger) mappedAnnotations.get(triggerID);
		EventMention event = new EventMention(cas);
		event.setId(id);
		event.setBegin(trigger.getBegin());
		event.setEnd(trigger.getEnd());
		event.setSpecificType(trigger.getSpecificType());
		LOGGER.trace(trigger.getSpecificType());
		FSArray arguments = new FSArray(cas, tokens.length - 1);
		for (int i = 1; i < tokens.length; i++) {
			LOGGER.trace(tokens[i]);
			String[] argumentStrings = tokens[i].split(":");
			String argumentID = argumentStrings[1];
			Annotation argumentAnnotation = mappedAnnotations.get(argumentID);
			if (argumentAnnotation == null) {
				mapEventEntry(mappedAnnotations, eventEntries, argumentID, cas);
				argumentAnnotation = mappedAnnotations.get(argumentID);
			}
			ArgumentMention argument = new ArgumentMention(cas);
			argument.setRef(argumentAnnotation);
			argument.setBegin(argumentAnnotation.getBegin());
			argument.setEnd(argumentAnnotation.getEnd());
			String role = argumentStrings[0];
			role = changeRole(role);
			argument.setRole(role);
			arguments.set(i - 1, argument);
		}
		event.setTrigger(trigger);
		event.setArguments(arguments);
		event.addToIndexes();
		mappedAnnotations.put(id, event);
	}

	private String changeRole(String role) {
		if (role.equals(THEME2))
			role = new String(THEME);
		return role;
	}

	private void mapEntity(Map<String, Annotation> mappedEntities, String entry, JCas cas) {
		String[] headAndTail = entry.split("\t");
		String id = headAndTail[0];
		String tail = headAndTail[1];
		String[] tokens = tail.split(" ");
		Annotation annotation = null;
		if (tokens[0].equals(PROTEIN)) {
			Gene protein = new Gene(cas);
			protein.setSpecificType(PROTEIN_SPECIFIC_TYPE);
			annotation = protein;
		} else if (tokens[0].equals(ENTITY)) {
			EntityMention entityMention = new EntityMention(cas);
			entityMention.setSpecificType(ENTITY_MENTION_SPECIFIC_TYPE);
			annotation = entityMention;
		} else if (tokens[0].equals(COREF_EXPRESSION)) {
			annotation = new CorefExpression(cas);
			++numCorefExpressions;
		} else {
			EventTrigger eventTrigger = new EventTrigger(cas);
			eventTrigger.setSpecificType(tokens[0]);
			annotation = eventTrigger;
		}
		annotation.setId(id);
		annotation.setBegin(new Integer(tokens[1]));
		annotation.setEnd(new Integer(tokens[tokens.length-1]));
		annotation.addToIndexes();
		mappedEntities.put(id, annotation);
	}

	/**
	 * Returns the number of coreference relations mapped by this mapper at the time of the call.
	 * 
	 * @return
	 */
	public int getNumCorefRelations() {
		return numCorefRelations;
	}

	/**
	 * Returns the number coreference expressions mapped by this mapper at the time of the call.
	 * 
	 * @return
	 */
	public int getNumCorefExpressions() {
		return numCorefExpressions;
	}

}
