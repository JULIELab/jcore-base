/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.bionlpformat.utils;

import com.google.common.collect.Lists;
import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.medical.*;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class AnnotationFileMapper_Med {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationFileMapper.class);
	private static final String MEDICATION = "Medication";
	private static final String ANAPHORA = "Anaphora";
	private static final String DOSE = "Dose";
	private static final String FREQUENCY = "Frequency";
	private static final String DURATION = "Duration";
	private static final String MODUS = "Modus";
	private static final String REASON = "Reason";
	
	private static final Set<String> ENTITIES = new HashSet<String>(Arrays.asList(MEDICATION, ANAPHORA));
	private static final Set<String> EVENTS = new HashSet<String>(Arrays.asList(DOSE, FREQUENCY, DURATION, MODUS, REASON));
	
	public void mapEventFile(BufferedReader bufferedReader, JCas jcas)
			throws IOException {
		Map<String, Annotation> mappedAnnotations = new HashMap<String, Annotation>();
		mapFile(mappedAnnotations, bufferedReader, jcas);
	}
	
	private void mapFile(Map<String, Annotation> mappedAnnotations, BufferedReader bufferedReader, JCas jcas)
			throws IOException {
		Map<String, String> eventEntries = new HashMap<String, String>();
		List<String> corefEntries = new ArrayList<>();
		Collection<Collection<String>> equivalents = new ArrayList<Collection<String>>();
		List<String> attrEntries = new ArrayList<>();
		String line = null;

		while ((line = bufferedReader.readLine()) != null) {
			// Entities
			if (line.startsWith("T")) {
				mapEntity(mappedAnnotations, line, jcas);
			}
			// Events
			else if (line.startsWith("E")) {
				eventEntries.put(line.substring(0, line.indexOf("\t")), line);
			}
			// Relations
			else if (line.startsWith("R")) {
				corefEntries.add(line);
			}
			// Attributes
			else if (line.startsWith("A")) {
				attrEntries.add(line);
			}
			// symmetric-transitive Relations
			else if (line.startsWith("*	Equiv")) {
				String equivIDs = line.substring(line.indexOf("Equiv") + 5).trim();
				equivalents.add(Lists.newArrayList(equivIDs.split(" ")));
			}
		}
		for (String eventID : eventEntries.keySet()) {
			if (!mappedAnnotations.keySet().contains(eventID))
				mapEventEntry(mappedAnnotations, eventEntries, eventID, jcas);
		}
//		for (Collection<String> equiv : equivalents)
//			mapEquivalents(mappedAnnotations, equiv, jcas);
//		for (String corefEntry : corefEntries)
//			mapCorefRelation(mappedAnnotations, corefEntry, jcas);
//		for (String attrEntry : attrEntries)
//			mapAttributes(mappedAnnotations, attrEntry, jcas);
	}
	
	private void mapEntity(Map<String, Annotation> mappedEntities, String entry, JCas jcas) {
		String[] headAndTail = entry.split("\t");
		String id = headAndTail[0];
		String tail = headAndTail[1];
		String[] tokens = tail.split(" ");
		Annotation annotation = null;
		Boolean isEntity = false;
		Boolean isEvent = false;
		
		if ( ENTITIES.contains(tokens[0]) ) {
			isEntity = true;
			if ( (tokens[0]).equals(MEDICATION) ) {
				Medication medication = new Medication(jcas);
				annotation = medication;
			}
			else if ( (tokens[0]).equals(ANAPHORA) ) {
				
			}
		}
		else if ( EVENTS.contains(tokens[0]) ) {
			isEvent = true;
			if ( (tokens[0]).equals(DOSE) ) {
				Dose dose = new Dose(jcas);
				annotation = dose;
			}
			else if ( (tokens[0]).equals(DURATION) ) {
				Duration duration = new Duration(jcas);
				annotation = duration;
			}
			else if ( (tokens[0]).equals(FREQUENCY) ) {
				Frequency frequency = new Frequency(jcas);
				annotation = frequency;
			}
			else if ( (tokens[0]).equals(MODUS) ) {
				Modus modus = new Modus(jcas);
				annotation = modus;
			}
			else if ( (tokens[0]).equals(REASON) ) {
				Reason reason = new Reason(jcas);
				annotation = reason;
			}
		}
		
		annotation.setId(id);
		annotation.setBegin(new Integer(tokens[1]));
		annotation.setEnd(new Integer(tokens[tokens.length-1]));
		annotation.addToIndexes();
		mappedEntities.put(id, annotation);
	}
	
	private void mapEventEntry(Map<String, Annotation> mappedAnnotations, Map<String,
			String> eventEntries, String eventID, JCas cas) {
		// mappedAnnotations:	{"T1": ANNOTATION_entity, "T2": ANNOTATION_event}
		// eventID:				some string of the form EXX; e.g. E1, E3, etc. 
		// eventEntries:		{"E2": "E2	Dose:T4 Dose-Arg:T1", "E3": "E3	Frequency:T5 Frequency-Arg:T1"}
		String[] headAndTail = eventEntries.get(eventID).split("\t");
		String id = headAndTail[0]; // e.g. "E2"
		String tail = headAndTail[1]; // e.g. "Dose:T4 Dose-Arg:T1"
		String[] tokens = tail.split("\\p{Blank}+"); // split on one or more "Space" or "Tabs"; e.g. ["Dose:T4", "Dose-Arg:T1"]
		String triggerID = tokens[0].split(":")[1]; // e.g. ["Dose", "T4"][1] -> "T4"
		String triggerName = tokens[0].split(":")[0]; // e.g. ... -> "Dose"
		
		EventMention event = (EventMention) mappedAnnotations.get(triggerID);

		FSArray arguments = new FSArray(cas, tokens.length - 1);
		for (int i = 1; i < tokens.length; i++) {
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
			argument.setRole(role);
			arguments.set(i - 1, argument);
		}
		event.setArguments(arguments);
		event.setSpecificType(triggerName);
		event.addToIndexes();
		mappedAnnotations.put(id, event);
	}
}
