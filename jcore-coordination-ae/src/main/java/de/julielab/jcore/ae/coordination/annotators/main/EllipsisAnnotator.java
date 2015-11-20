/** 
 * EllipsisAnnotator.java
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
 * Creation date: 11.04.2008 
 * 
 * Ellipsis annotator that invokes the baseline prediction of resolved ellipses within 
 * all EEE of a JCas. Moreover, this annotator creates new entity mentions (one for each 
 * conjunct of an EEE), adds them to the JCas and removes the old entity mentions. This 
 * annotator assumes that EntityMentoions, EEEs and CoordinationElements have been 
 * annotated in the CAS.  
 **/
package de.julielab.jcore.ae.coordination.annotators.main;

import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.ANTECEDENT;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.BASELINE;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.CONJUNCT;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.CONJUNCTION;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.COORDINATION;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.EMPTY_STRING;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.ENTITY;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.SPACE_CHAR;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.value_mode;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.value_object_of_analysis;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.value_result_of_analysis;

import java.util.ArrayList;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.types.CoordinationElement;
import de.julielab.jules.types.EEE;
import de.julielab.jules.types.EntityMention;
import de.julielab.jcore.utility.JCoReAnnotationTools;

public class EllipsisAnnotator {

	private final static Logger LOGGER = LoggerFactory.getLogger(EllipsisAnnotator.class);
	private static final String COMMA = ",";

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to process the ellipsis
	 * 
	 * @param jcas
	 *            JCas which will be used to process the ellipsis
	 */
	public void predictEllipsis(JCas jcas) {
		LOGGER.info("EllipsisAnnotator was called ... ");
		if (value_result_of_analysis.equals(COORDINATION)) {
			resolveEllipsis(jcas);
		} // of if
		if (value_result_of_analysis.equals(ENTITY)) {
			processEntityMentions(jcas);
			EEEAnnotator eeeAnnotator = new EEEAnnotator();
			eeeAnnotator.removeEEEFromCas(jcas);
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to resolve the ellipsis, in other words to obtain the String which will hold the
	 * resolve ellipsis
	 * 
	 * @param jcas
	 *            JCas which will be used to resolve the ellipsis
	 */
	// TODO another, more descriptive method name?
	public void resolveEllipsis(JCas jcas) {
		LOGGER.info("Resolve ellipsis ... ");
		AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		AnnotationIndex coordinationElementIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						CoordinationElement.type);
		FSIterator eeeIterator = eeeIndex.iterator();
		String resolvedEllipsisString = EMPTY_STRING;
		while (eeeIterator.hasNext()) {
			EEE eee = (EEE) eeeIterator.next();
			FSIterator coordElIterator = coordinationElementIndex.subiterator(eee);
			ArrayList<CoordinationElement> coordElArrayList = new ArrayList<CoordinationElement>();
			while (coordElIterator.hasNext()) {
				CoordinationElement coordEl = (CoordinationElement) coordElIterator.next();
				coordElArrayList.add(coordEl);
			} // of while
			resolvedEllipsisString = computeResolvedEllipsisString(coordElArrayList);
			// System.out.println("RESOLVED: _" + resolvedEllipsis + "_");
			eee.setElliptical(true);
			eee.setResolved(resolvedEllipsisString);
		} // of while
	} // of resolveEllipsis

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to compute the resolved ellipsis string using an Array List of CoordinationElements
	 * 
	 * @param coordElArrayList
	 *            ArrayList which contains CoordinationElements; these elements will be used to
	 *            compute the resolved ellipsis string.
	 * @return resolvedEllipsis String which will contain the resolved ellipsis
	 */
	private String computeResolvedEllipsisString(ArrayList<CoordinationElement> coordElArrayList) {
		String resolvedEllipsis = EMPTY_STRING;
		if (checkCatValidity(coordElArrayList)) {
			String leftAntecedent = getLeftAntecedent(coordElArrayList);
			String rightAntecedent = getRightAntecedent(coordElArrayList);
			ArrayList<String> conjunctArrayList = getConjunctArrayList(coordElArrayList);
			ArrayList<String> conjunctionArrayList = getConjunctionArrayList(coordElArrayList);
			for (int i = 0; i < conjunctArrayList.size() - 1; i++) {
				String conjunct = conjunctArrayList.get(i);
				String conjunction = conjunctionArrayList.get(i);
				if (!leftAntecedent.equals(EMPTY_STRING)) {
					resolvedEllipsis = resolvedEllipsis + leftAntecedent + SPACE_CHAR;
				} // of if
				resolvedEllipsis = resolvedEllipsis + conjunct;
				if (!rightAntecedent.equals(EMPTY_STRING)) {
					resolvedEllipsis = resolvedEllipsis + SPACE_CHAR + rightAntecedent;
				} // of if
				if (!conjunction.startsWith(COMMA)) {
					resolvedEllipsis = resolvedEllipsis + SPACE_CHAR;
				} // of if
				resolvedEllipsis = resolvedEllipsis + conjunction + SPACE_CHAR;
			} // of for
			// "add" the last conjunct manually to the resolved ellipsis
			String conjunct = conjunctArrayList.get(conjunctArrayList.size() - 1);
			if (!leftAntecedent.equals(EMPTY_STRING)) {
				resolvedEllipsis = resolvedEllipsis + leftAntecedent + SPACE_CHAR;
			} // of if
			resolvedEllipsis = resolvedEllipsis + conjunct;
			if (!rightAntecedent.equals(EMPTY_STRING)) {
				resolvedEllipsis = resolvedEllipsis + SPACE_CHAR + rightAntecedent;
			} // of if
			resolvedEllipsis = resolvedEllipsis.trim();
		} else {
			LOGGER.info("No valid categories of the coordination elemens were found; cannot resolve ellipsis!");
		} // of else
		return resolvedEllipsis;
	} // of computeResolvedEllipsisString

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve the conjunctions from a list of coordination elements. In other words,
	 * the conjunctions among the coordinationElements will be identified, put into a list and then
	 * this list will be returned.
	 * 
	 * @param coordElArrayList
	 *            ArrayList of CordinationElements which will be used to retrieve the conjunctions
	 * @return conjunctionArrayList ArrayList of String which will contain the found conjunctions
	 */
	private ArrayList<String> getConjunctionArrayList(ArrayList<CoordinationElement> coordElArrayList) {
		ArrayList<String> conjunctionArrayList = new ArrayList<String>();
		String conjunction = EMPTY_STRING;
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			String cat = coordEl.getCat();
			if (cat.equals(CONJUNCTION)) {
				conjunction = conjunction + SPACE_CHAR + coordEl.getCoveredText();
			} // of if
			if ((cat.equals(CONJUNCT) || i == coordElArrayList.size() - 1) && !(conjunction.equals(EMPTY_STRING))) {
				conjunction = conjunction.trim();
				conjunctionArrayList.add(conjunction);
				conjunction = EMPTY_STRING;
			} // of if
		} // of for
		// System.out.println("\nCONJUNCTIONS: " + conjunctionArrayList + "\n");
		return conjunctionArrayList;
	} // of getConjunctionArrayList

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to check if the categories of coordination elements are valid. Please note that these
	 * categories are considered to be valid, if they are not null.
	 * 
	 * @param coordElArrayList
	 *            ArrayList which contains CoordinationElements; categories of these
	 *            CoordinationElements will be checked for validity
	 * @return catValid Boolean which indicates the validity of the coordination element categories
	 */
	private boolean checkCatValidity(ArrayList<CoordinationElement> coordElArrayList) {
		boolean catValid = true;
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			if (coordEl.getCat() == null) {
				return false;
			} // of if
		} // of for
		return catValid;
	}// of checkCatValidity

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve a list of conjuncts from a list of coordination elements. In other words,
	 * the conjuncts among the coordinationElements will be identified, put into a list and then
	 * this list will be returned.
	 * 
	 * @param coordElArrayList
	 *            ArrayList of CordinationElements which will be used to retrieve the conjuncts
	 * @return conjunctArrayList ArrayList of String which will contain the found conjuncts
	 */
	private ArrayList<String> getConjunctArrayList(ArrayList<CoordinationElement> coordElArrayList) {
		ArrayList<String> conjunctArrayList = new ArrayList<String>();
		String conjunct = EMPTY_STRING;
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			String cat = coordEl.getCat();
			if (cat.equals(CONJUNCT)) {
				conjunct = conjunct + SPACE_CHAR + coordEl.getCoveredText();
			} // of if
			if ((cat.equals(CONJUNCTION) || i == coordElArrayList.size() - 1) && !(conjunct.equals(EMPTY_STRING))) {
				conjunct = conjunct.trim();
				conjunctArrayList.add(conjunct);
				conjunct = EMPTY_STRING;
			} // of if
		} // of for
		// System.out.println("\nCONJUNCTS: " + conjunctArrayList + "\n");
		return conjunctArrayList;
	} // of getConjunctArrayList

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve the right antecedent using an Array List of CoordinationElements
	 * 
	 * @param coordElArrayList
	 *            ArrayList which will be used to find the right antecedent
	 * @return rightAntecedent String which will contain the right antecedent;
	 */
	private String getRightAntecedent(ArrayList<CoordinationElement> coordElArrayList) {
		String rightAntecedent = EMPTY_STRING;
		for (int i = coordElArrayList.size() - 1; i >= 0; i--) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			if (coordEl.getCat().equals(ANTECEDENT)) {
				rightAntecedent = coordEl.getCoveredText() + SPACE_CHAR + rightAntecedent;
			} else {
				break;
			} // of if
		} // of for
		rightAntecedent = rightAntecedent.trim();
		return rightAntecedent;
	} // of getRightAntecedent

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve the left antecedent using an Array List of CoordinationElements
	 * 
	 * @param coordElArrayList
	 *            ArrayList which will be used to find the left antecedent
	 * @return leftAntecedent String which will contain the left antecedent;
	 */
	private String getLeftAntecedent(ArrayList<CoordinationElement> coordElArrayList) {
		String leftAntecedent = EMPTY_STRING;
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			if (coordEl.getCat().equals(ANTECEDENT)) {
				leftAntecedent = leftAntecedent + SPACE_CHAR + coordEl.getCoveredText();
			} else {
				break;
			} // of if
		} // of for
		leftAntecedent = leftAntecedent.trim();
		return leftAntecedent;
	} // of getLeftAntecedent

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Used if the result of analysis is entity. For every EEE in the JCas, there will be at least
	 * two such new EntityMentions - dependent on the number of conjuncts in the EEE.
	 * 
	 * @param jcas
	 *            JCas which new entityMentions will be written to
	 */
	public void processEntityMentions(JCas jcas) {
		LOGGER.info("Processing entity mentions ... ");
		AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		AnnotationIndex coordinationElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						CoordinationElement.type);
		FSIterator eeeIterator = eeeIndex.iterator();
		while (eeeIterator.hasNext()) {
			EEE eee = (EEE) eeeIterator.next();
			FSIterator coordinationElIterator = coordinationElIndex.subiterator(eee);
			ArrayList<CoordinationElement> coordElArrayList = new ArrayList<CoordinationElement>();
			while (coordinationElIterator.hasNext()) {
				CoordinationElement coordinationElement = (CoordinationElement) coordinationElIterator.next();
				coordElArrayList.add(coordinationElement);
			} // of while
			EntityMention deletedEntityMention = getDeletedEntityMention(jcas, eee);
			ArrayList<EntityMention> newEntityMentionArrayList = getNewEntityMentions(jcas, eee, coordElArrayList,
							deletedEntityMention);
			if (newEntityMentionArrayList.size() > 0) {
				writeEntityMentionsToCas(newEntityMentionArrayList);
				jcas.removeFsFromIndexes(deletedEntityMention);
			} else {
				LOGGER
								.info("No new entity mentions were added to the JCas. Also, already existing entity mentions will not be removed from JCas");
			} // of if
		} // of while
	} // of processEntityMentions

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve the EntityMention (belonging to the given EEE) which has to be deleted
	 * 
	 * @param jcas
	 *            JCas which will be used to obtain the appropriate AnnotationIndex
	 * @param eee
	 *            EEE which will be used to retrieve the EntityMention
	 * @return entityMention EntityMention which corresponds to the EEE
	 */
	private EntityMention getDeletedEntityMention(JCas jcas, EEE eee) {
		AnnotationIndex entityMentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						EntityMention.type);
		FSIterator entityMentionIterator = entityMentionIndex.iterator();
		while (entityMentionIterator.hasNext()) {
			EntityMention entityMention = (EntityMention) entityMentionIterator.next();
			if ((entityMention.getBegin() == eee.getBegin()) && (entityMention.getEnd() == eee.getEnd())) {
				return entityMention;
			} // of if
		} // of while
		LOGGER.info("No entity mention was found, which belongs to the EEE " + eee.getCoveredText());
		return null;
	} // of getDeletedEntityMentions

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to write EntityMentions to the Cas
	 * 
	 * @param entityMentionArrayList
	 *            ArrayList of EntityMentions which will be added to the Cas
	 */
	private void writeEntityMentionsToCas(ArrayList<EntityMention> entityMentionArrayList) {
		LOGGER.info("Writing new entity mentions to JCas ... ");
		for (int i = 0; i < entityMentionArrayList.size(); i++) {
			EntityMention entityMention = entityMentionArrayList.get(i);
			LOGGER.info("New entity mention to JCas ... " + entityMention.getTextualRepresentation());
			entityMention.addToIndexes();
		} // of for
	} // of writeEntityMentionsToCas

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to construct new EntityMentions using jcas to which these new mentions will be
	 * written to, an eee (responsible for begin and end), an ArrayList of CoordinationElements
	 * (responsible for the number of new EntityMentions and their textual representation),
	 * 
	 * @param JCas
	 *            jcas which will be used to construct EntityMentions
	 * @param eee
	 *            EEE which will be used to construct new EntityMention
	 * @param coordElArrayList
	 *            which will be used to construct new EntityMention
	 * @param deletedEntityMention
	 *            EntityMention which attributes will be used for the new EntityMentions
	 * @return entityMentionArrayList ArrayList of newly constructed EntityMentions
	 */
	private ArrayList<EntityMention> getNewEntityMentions(JCas jcas, EEE eee,
					ArrayList<CoordinationElement> coordElArrayList, EntityMention deletedEntityMention) {
		ArrayList<EntityMention> entityMentionArrayList = new ArrayList<EntityMention>();
		ArrayList<String> conjunctArrayList = new ArrayList<String>();
		if (checkCatValidity(coordElArrayList)) {
			String leftAntecedent = getLeftAntecedent(coordElArrayList);
			String rightAntecedent = getRightAntecedent(coordElArrayList);
			conjunctArrayList = getConjunctArrayList(coordElArrayList);
			for (int i = 0; i < conjunctArrayList.size(); i++) {
				String conjunct = conjunctArrayList.get(i);
				String textualRepresentation = getTextualRepresentation(leftAntecedent, conjunct, rightAntecedent);
				String entityType = deletedEntityMention.getClass().getName();
				EntityMention entityMention = null;
				try {
					entityMention = (EntityMention) JCoReAnnotationTools.getAnnotationByClassName(jcas, entityType);
					setEntityMentionAttributes(eee, deletedEntityMention, textualRepresentation, entityMention);
					entityMentionArrayList.add(entityMention);
				} catch (Exception e) {
					LOGGER
									.error("Could not create new entity mention with the following entity type: "
													+ entityType, e);
				} // of try catch
			} // of for
		} else {
			LOGGER
							.info("No valid categories of the coordination elemens were found; cannot create new EntityMentions!");
		} // of else
		return entityMentionArrayList;
	} // of getNewEntityMentions

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * This is just an auxiliary method which is used to "transfer" the entity mention attributes
	 * from the old entity mention which is ought to be deleted to the new entity mention
	 * 
	 * @param eee
	 *            EEE from which the begin and the end attribute will be used
	 * @param deletedEntityMention
	 *            EntityMention which will be deleted and which attributes will be "transferred" to
	 *            the new one
	 * @param textualRepresentation
	 *            String which determines the textual representation of a discontinuous entity
	 * @param entityMention
	 *            EntityMention to which the attributes will be transferred
	 */
	private void setEntityMentionAttributes(EEE eee, EntityMention deletedEntityMention, String textualRepresentation,
					EntityMention entityMention) {
		entityMention.setBegin(eee.getBegin());
		entityMention.setEnd(eee.getEnd());
		entityMention.setTextualRepresentation(textualRepresentation);
		entityMention.setConfidence(deletedEntityMention.getConfidence());
		entityMention.setComponentId(deletedEntityMention.getComponentId());
		entityMention.setId(deletedEntityMention.getId());
		entityMention.setSpecificType(deletedEntityMention.getSpecificType());
		entityMention.setRef(deletedEntityMention.getRef());
		entityMention.setResourceEntryList(deletedEntityMention.getResourceEntryList());
		entityMention.setHead(deletedEntityMention.getHead());
		entityMention.setMentionLevel(deletedEntityMention.getMentionLevel());
	} // of setEntityMentionAttributes

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to construct textual representation of an EntityMention using left antecedents, the
	 * conjunct and the right antecedents
	 * 
	 * @param leftAntecedent
	 *            String which contains the left antecedent. Please note that here, the left
	 *            antecedent can consist of multiple tokens.
	 * @param conjunct
	 *            String which contains the conjunct
	 * @param rightAntecedent
	 *            String which contains the right antecedent. Please note that here, the right
	 *            antecedent can consist of multiple tokens.
	 * @return textualRepresentation String which contains the textual representation of the new
	 *         entity
	 */
	private String getTextualRepresentation(String leftAntecedent, String conjunct, String rightAntecedent) {
		String textualRepresentation = "";
		if (!leftAntecedent.equals(EMPTY_STRING)) {
			textualRepresentation = leftAntecedent + SPACE_CHAR;
		} // of if
		textualRepresentation = textualRepresentation + conjunct + SPACE_CHAR;
		if (!rightAntecedent.equals(EMPTY_STRING)) {
			textualRepresentation = textualRepresentation + rightAntecedent;
		} // of if
		textualRepresentation = textualRepresentation.trim();
		return textualRepresentation;
	} // of getTextualRepresentation
	/*--------------------------------------------------------------------------------------------*/
} // of EllipsisAnnotator
