/** 
 * CoordinationElementAnnotator.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: lichtenwald, buyko
 * 
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: 11.04.2008 
 * 
 * Coordination element annotator that invokes the baseline prediction of coordination 
 * elements within a given EEE (elliptical entity expression). This annotator assumes 
 * that tokens, POS tags and EEEs have been annotated in the CAS. 
 **/
package de.julielab.jcore.ae.coordination.annotators.baseline;

import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.ANTECEDENT;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.CONJUNCT;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.CONJUNCTION;
import static de.julielab.jcore.ae.coordination.main.CoordinationAnnotator.EMPTY_STRING;

import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.coordination.annotators.main.CoordinationElementAnnotator;
import de.julielab.jules.types.CoordinationElement;
import de.julielab.jules.types.EEE;
import de.julielab.jules.types.Token;

public class BaselineCoordinationElementAnnotator extends CoordinationElementAnnotator {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaselineCoordinationElementAnnotator.class);
	private static final String POS_COMMA = ",";
	private static final String POS_CONJUNCTION = "CC";

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to predict coordination elements of every EEE within the JCas
	 * 
	 * @param jcas
	 *            JCas which will be used to retrieve EEE from it and to predict the coordination
	 *            elements
	 */
	public void predictCoordinationElements(JCas jcas) {
		LOGGER.info("CoordinationElementAnnotator is processing ... ");
		AnnotationIndex eeeIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		AnnotationIndex eeeTokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		FSIterator eeeIterator = eeeIndex.iterator();
		ArrayList<Token> tokenArrayList = null;
		while (eeeIterator.hasNext()) {
			EEE eee = (EEE) eeeIterator.next();
			FSIterator eeeTokenIterator = eeeTokenIndex.subiterator(eee);
			tokenArrayList = new ArrayList<Token>();
			while (eeeTokenIterator.hasNext()) {
				Token eeeToken = (Token) eeeTokenIterator.next();
				tokenArrayList.add(eeeToken);
			} // of while
			// premark the coordination elements as antecedents
			markAntecedents(jcas, tokenArrayList);
			markConjunctions(jcas, tokenArrayList);
			markConjuncts(jcas, tokenArrayList, eee);
			postprocessCoordinationElements(jcas, eee);
		}
	}

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to post-process the coordination elements and to remove the categories, if there were
	 * no antecedents found. Please note that removing of the categories of coordination elements is
	 * actually done by removing the coordination elements from the JCas and writing new
	 * coordination elements without categories (category is null) to the JCas instead
	 * 
	 * @param jcas
	 *            JCas which will be used to post-process the coordination elements
	 * @param eee
	 *            EEE which will be used to post-process the coordination elements
	 */
	private void postprocessCoordinationElements(JCas jcas, EEE eee) {
		AnnotationIndex coordElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						CoordinationElement.type);
		FSIterator coordElIterator = coordElIndex.subiterator(eee);
		ArrayList<CoordinationElement> coordElArrayList = new ArrayList<CoordinationElement>();
		boolean antecedentFound = false;
		while (coordElIterator.hasNext()) {
			CoordinationElement coordEl = (CoordinationElement) coordElIterator.next();
			coordElArrayList.add(coordEl);
		} // of while
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			if (coordEl.getCat().equals(ANTECEDENT)) {
				antecedentFound = true;
			} // of if
		} // of for
		if (!antecedentFound) {
			for (int i = 0; i < coordElArrayList.size(); i++) {
				CoordinationElement oldCoordEl = coordElArrayList.get(i);
				CoordinationElement newCoordEl = new CoordinationElement(jcas);
				newCoordEl.setBegin(oldCoordEl.getBegin());
				newCoordEl.setEnd(oldCoordEl.getEnd());
				oldCoordEl.removeFromIndexes();
				newCoordEl.addToIndexes();
			} // of for
			LOGGER
							.info("Could not predict coordination element categories; setting them to null. Ellipsis resolution will fail!");
		} // of if
	} // of postprocessCoordinationElements

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to mark the conjunct tokens as conjuncts and write them as coordination elements to
	 * the JCas.
	 * 
	 * @param jcas
	 *            JCas to which the new coordination elements will be written
	 * @param eeeTokenArrayList
	 *            Array List of Tokens which will be used to create new coordination elements
	 * @param eee
	 *            EEE which is used to find conjuncts
	 */
	private void markConjuncts(JCas jcas, ArrayList<Token> eeeTokenArrayList, EEE eee) {
		AnnotationIndex coordElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
						CoordinationElement.type);
		FSIterator coordElIterator = coordElIndex.subiterator(eee);
		ArrayList<CoordinationElement> coordElArrayList = new ArrayList<CoordinationElement>();
		while (coordElIterator.hasNext()) {
			CoordinationElement coordEl = (CoordinationElement) coordElIterator.next();
			coordElArrayList.add(coordEl);
		} // of while
		for (int i = 0; i < coordElArrayList.size(); i++) {
			CoordinationElement coordEl = coordElArrayList.get(i);
			if (coordEl.getCat().equals(CONJUNCTION)) {
				computeConjuncts(jcas, coordElArrayList, i, eee);
			} // of if
		} // of for
	} // markConjuncts

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to compute conjuncts using the Array List of coordination elements and a certain
	 * position within this ArrayList. Please note that this method is the root of the matter of the
	 * whole baseline algorithm for conjunct prediction.
	 * 
	 * @param jcas
	 *            JCas which will be used to to compute conjuncts
	 * @param coordElArrayList
	 *            ArrayList which contains coordination elements
	 * @param initialIndex
	 *            Integer which marks the position of the current conjunction
	 * @param eee
	 *            EEE which will be used to compute conjuncts
	 */
	// TODO detailed description of the approach?
	private void computeConjuncts(JCas jcas, ArrayList<CoordinationElement> coordElArrayList, int initialIndex, EEE eee) {
		String leftPosSoFar = EMPTY_STRING;
		String rightPosSoFar = EMPTY_STRING;
		int leftIndex = initialIndex - 1;
		int rightIndex = initialIndex + 1;
		boolean stop = false;
		boolean equals = false;
		// This is done to skip the consecutive conjunction (, or / , and) as in "X, Y, and Z cells"
		if (coordElArrayList.get(leftIndex).getCat().equals(CONJUNCTION)) {
			leftIndex--;
		} // of if
		if (coordElArrayList.get(rightIndex).getCat().equals(CONJUNCTION)) {
			rightIndex++;
		} // of if
		while (!stop) {
			leftPosSoFar = getPosTag(jcas, coordElArrayList.get(leftIndex), eee) + leftPosSoFar;
			rightPosSoFar = rightPosSoFar + getPosTag(jcas, coordElArrayList.get(rightIndex), eee);
			if (leftPosSoFar.equals(rightPosSoFar)) {
				equals = true;
			} // of if
			if (!(leftPosSoFar.equals(rightPosSoFar)) && (equals)) {
				stop = true;
				leftIndex++;
				rightIndex--;
			} // of if
			leftIndex--;
			rightIndex++;
			if ((leftIndex < 0) || (rightIndex > coordElArrayList.size() - 1)
							|| (coordElArrayList.get(leftIndex).getCat().equals(CONJUNCTION))
							|| (coordElArrayList.get(rightIndex).getCat().equals(CONJUNCTION))) {
				stop = true;
			} // of if
		} // of while
		leftIndex++;
		rightIndex--;
		for (int j = leftIndex; j < initialIndex; j++) {
			if (!(coordElArrayList.get(j).getCat().equals(CONJUNCTION))) {
				coordElArrayList.get(j).setCat(CONJUNCT);
			} // of if
		} // of for
		for (int j = initialIndex + 1; j <= rightIndex; j++) {
			if (!(coordElArrayList.get(j).getCat().equals(CONJUNCTION))) {
				coordElArrayList.get(j).setCat(CONJUNCT);
			} // of if
		} // of for
	} // of computeConjuncts

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to retrieve the POS tag which belongs to the coordination element.
	 * 
	 * @param jcas
	 *            JCas which will be used to search the POS tag
	 * @param coordEl
	 *            CoordinationElement for which the POS tag will be searched
	 * @param eee
	 *            EEE which will be used to search the POS tag within
	 */
	private String getPosTag(JCas jcas, CoordinationElement coordEl, EEE eee) {
		int begin = coordEl.getBegin();
		int end = coordEl.getEnd();
		String posTag = EMPTY_STRING;
		AnnotationIndex eeeTokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		FSIterator eeeTokenIterator = eeeTokenIndex.subiterator(eee);
		while (eeeTokenIterator.hasNext()) {
			Token eeeToken = (Token) eeeTokenIterator.next();
			if ((eeeToken.getBegin() == begin) && (eeeToken.getEnd() == end)) {
				posTag = eeeToken.getPosTag(0).getValue();
			} // of if
		} // of while
		return posTag;
	} // of getPosTag

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to mark the conjunction tokens as conjunctions and write them as coordination
	 * elements to the JCas.
	 * 
	 * @param jcas
	 *            JCas to which the new coordination elements will be written
	 * @param eeeTokenArrayList
	 *            Array List of Tokens which will be used to create new coordination elements
	 */
	private void markConjunctions(JCas jcas, ArrayList<Token> eeeTokenArrayList) {
		for (int i = 0; i < eeeTokenArrayList.size(); i++) {
			Token eeeToken = eeeTokenArrayList.get(i);
			String eeeTokenPosTag = eeeToken.getPosTag(0).getValue();
			if (eeeTokenPosTag.equals(POS_COMMA) || eeeTokenPosTag.equals(POS_CONJUNCTION)) {
				CoordinationElement coordEl = new CoordinationElement(jcas);
				coordEl.setBegin(eeeToken.getBegin());
				coordEl.setEnd(eeeToken.getEnd());
				coordEl.setCat(CONJUNCTION);
				coordEl.addToIndexes();
			} // of if
		} // of for
	} // of markConjunctions

	/*--------------------------------------------------------------------------------------------*/
	/**
	 * Is used to mark the certain tokens of an EEE as antecedents and write them as coordination
	 * elements to the JCas. Please note that this is just the pre-machining since some of the
	 * coordination elements will later be identified as conjuncts. For the conjunction tokens no
	 * coordination elements will be created.
	 * 
	 * @param jcas
	 *            JCas to which the new coordination elements will be written
	 * @param eeeTokenArrayList
	 *            Array List of Tokens which will be used to create new coordination elements
	 */
	private void markAntecedents(JCas jcas, ArrayList<Token> eeeTokenArrayList) {
		for (int i = 0; i < eeeTokenArrayList.size(); i++) {
			Token eeeToken = eeeTokenArrayList.get(i);
			String eeeTokenPosTag = eeeToken.getPosTag(0).getValue();
			if (!(eeeTokenPosTag.equals(POS_COMMA)) && !(eeeTokenPosTag.equals(POS_CONJUNCTION))) {
				CoordinationElement coordEl = new CoordinationElement(jcas);
				coordEl.setBegin(eeeToken.getBegin());
				coordEl.setEnd(eeeToken.getEnd());
				coordEl.setCat(ANTECEDENT);
				coordEl.addToIndexes();
			} // of if
		} // of for
	}

	@Override
	public void setModel(UimaContext context) throws AnnotatorConfigurationException, AnnotatorContextException,
					AnnotatorInitializationException {
		// TODO implement
	}
} // CoordinationElementAnnotator
