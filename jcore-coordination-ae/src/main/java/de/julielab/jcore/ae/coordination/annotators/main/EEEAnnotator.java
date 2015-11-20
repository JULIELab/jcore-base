/** 
 * EEEAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: Lichtenwald
 * 
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: 10.4.2008 
 * 
 * EEE annotator that invokes the baseline prediction of EEEs (elliptical entity 
 * expression) within a given sentence. This annotator assumes that entityMentions have 
 * been annotated in the CAS.  
 **/
package de.julielab.jcore.ae.coordination.annotators.main;

import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.BASELINE;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.ENTITY;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.SPACE_CHAR;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.used_conjunctions_array;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.value_mode;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.value_object_of_analysis;

import java.util.ArrayList;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.types.EEE;
import de.julielab.jules.types.EntityMention;

public class EEEAnnotator {

	private final static Logger LOGGER = LoggerFactory.getLogger(EEEAnnotator.class);

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to predict elliptical entity expressions (EEE) within the JCas. The approach is to
	 * iterate over entity mentions and determine if they contain given conjunction. If they do,
	 * they are EEEs. In that case a corresponding EEE will be written to the JCas
	 * 
	 * @param jcas
	 *            JCas which will be used to iterate within
	 * @param usedConjunctionsFileName
	 *            String which contains the path to the text file with conjunctions, which were used
	 *            in th baseline
	 * @return eeePredicted Boolean which indicates, if at least one EEE could be predicted or not
	 */
	public boolean predictEEE(JCas jcas) {
		LOGGER.info("EEEAnnotator was called ... ");
		AnnotationIndex entityMentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						EntityMention.type);
		FSIterator entityMentionIterator = entityMentionIndex.iterator();
		boolean eeePredicted = false;
		if (value_object_of_analysis.equals(ENTITY)) {
			while (entityMentionIterator.hasNext()) {
				EntityMention entityMention = (EntityMention) entityMentionIterator.next();
				String coveredText = entityMention.getCoveredText();
				if (cotainsConjunction(coveredText, used_conjunctions_array)) {
					EEE eee = new EEE(jcas);
					eee.setBegin(entityMention.getBegin());
					eee.setEnd(entityMention.getEnd());
					eee.addToIndexes();
					eeePredicted = true;
					// System.out.println("EEE: " + eee.getCoveredText());
				} // of if
			} // of while
		} else {
			// TODO Katja
		} // of if else
		if (!eeePredicted) {
			LOGGER.info("No EEE could be predicted ... ");
		} // of if
		return eeePredicted;
	} // of predictEEE

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to determine if a certain entity mention contains one of the given coordinations.
	 * 
	 * @param coveredText
	 *            String which represents the covered text by the entity mention
	 * @param coordinationArray
	 *            String Array which contains the given coordinations
	 * @return Boolean which determines if the entity mention contains one of the given
	 *         coordinations
	 */
	private boolean cotainsConjunction(String coveredText, String[] conjunctionArray) {
		for (int i = 0; i < conjunctionArray.length; i++) {
			if (coveredText.contains(SPACE_CHAR + conjunctionArray[i] + SPACE_CHAR)) {
				return true;
			} // of if
		} // of for
		return false;
	} // of cotainsConjunction

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to remove any EEEs from given JCas since EEEs are only auxiliary constructs. The
	 * approach is to iterate over EEEs, collect any detected EEEs in an ArrayList and remove then
	 * each of those EEEs from the jcas.
	 * 
	 * @param jcas
	 *            JCas which the EEEs will be removed from
	 */
	public void removeEEEFromCas(JCas jcas) {
		LOGGER.info("All EEEs will be removed from JCas ... ");
		AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		FSIterator eeeIterator = eeeIndex.iterator();
		ArrayList<EEE> eeeArrayList = new ArrayList<EEE>();
		while (eeeIterator.hasNext()) {
			EEE eee = (EEE) eeeIterator.next();
			eeeArrayList.add(eee);
		} // of while
		for (int i = 0; i < eeeArrayList.size(); i++) {
			EEE eee = eeeArrayList.get(i);
			jcas.removeFsFromIndexes(eee);
		} // of for
	} // removeEEEFromCas
	/*--------------------------------------------------------------------------------------------*/
} // of EEEAnnotator
