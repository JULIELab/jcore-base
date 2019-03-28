/** 
 * AceReaderTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: Lichtenwald, Buyko
 * 
 * Current version: 2.0.1	
 * Since version:  1.0
 *
 * Creation date: 01.02.2008 
 * 
 * 
 **/

package de.julielab.jcore.reader.ace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.ace.GPE;
import de.julielab.jcore.types.ace.LOC;
import de.julielab.jcore.types.ace.PART_WHOLE;
import de.julielab.jcore.types.ace.PART_WHOLE_Inverse;
import de.julielab.jcore.types.ace.Transaction;
import de.julielab.jcore.types.ace.Document;
import de.julielab.jcore.types.ace.Entity;
import de.julielab.jcore.types.ace.EntityAttribute;
import de.julielab.jcore.types.ace.Event;
import de.julielab.jcore.types.ace.EventMention;
import de.julielab.jcore.types.ace.Relation;
import de.julielab.jcore.types.ace.RelationMention;
import de.julielab.jcore.types.ace.SourceFile;
import de.julielab.jcore.types.ace.Timex2;
import de.julielab.jcore.types.ace.Value;
import de.julielab.jcore.utility.JCoReTools;

public class AceReaderTest extends TestCase {
	/**
	 * Path to the MedlineReader descriptor
	 */
	private static final String ACE_READER_DESCRIPTOR = "src/main/resources/de/julielab/jcore/reader/ace/desc/jcore-ace-reader.xml";

	/**
	 * Path to SGM file
	 */
	private static final String SGM_FILE = "src/test/resources/de/julielab/jcore/reader/ace/data/XIN_ENG_20030624.0085_test_no_events.sgm";
	
	private static final String OUT_FOLDER = "src/test/resources/de/julielab/jcore/reader/ace/data/out/";
	/**
	 * Object to be tested
	 */
	private CollectionReader aceReader;

	/**
	 * Auxiliary collection reader
	 */
	private CollectionReader testReader;

	/**
	 * CAS array list with CAS objects that where processed by the aceReader
	 */
	private ArrayList<CAS> casArrayList = new ArrayList<CAS>();

	/**
	 * Auxiliary CAS objects
	 */
	private CAS aceReaderCas;

	private CAS testReaderCas;

	private JCas aceReaderJCas;

	private JCas testReaderJCas;

	LOC entity1_1;

	LOC entity1_2;

	GPE entity2_1;

	GPE entity2_2;

	GPE entity2_3;

	GPE entity2_4;

	/*----------------------------------------------------------------------------------------------*/
	@Override
	protected void setUp() throws Exception {
		aceReader = getCollectionReader(ACE_READER_DESCRIPTOR);
		processAllCases();
		super.setUp();

		System.out.println("ALL CASes were processed");
	} // of setUp

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Processes all CASes by the aceReader
	 * 
	 * @throws CASException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void processAllCases() throws CASException, SAXException, ParserConfigurationException {

		try {
			while (aceReader.hasNext()) {

				aceReaderCas = CasCreationUtils.createCas((AnalysisEngineMetaData) aceReader.getMetaData());
				aceReader.getNext(aceReaderCas);
				casArrayList.add(aceReaderCas);
			} // of while

			aceReaderCas = casArrayList.get(0);
			aceReaderJCas = aceReaderCas.getJCas();

			testReader = getCollectionReader(ACE_READER_DESCRIPTOR);
			testReaderCas = CasCreationUtils.createCas((AnalysisEngineMetaData) testReader.getMetaData());

			testReaderJCas = testReaderCas.getJCas();

			System.out.println("Initializing test CAS");
			buildSourceFile(testReaderJCas);

			writeCasToXMI(aceReaderCas, 1);
			// writeCasToXMI(testReaderCas, 1);

			compareCASes();

		} // of try
		catch (CollectionException e) {
			e.printStackTrace();
		} // of catch
		catch (IOException e) {
			e.printStackTrace();
		} // of catch
		catch (ResourceInitializationException e) {
			e.printStackTrace();
		} // of catch
	} // of processAllCases

	/*----------------------------------------------------------------------------------------------*/
	private void compareCASes() {
		assertTrue("Invalid source file attributes!", checkSourceFile());
		assertTrue("Invalid generated Jules Components!", checkGeneratedJulesComponents());
	} // compareCASes

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkGeneratedJulesComponents() {
		System.out.println("CALL checkGeneratedJulesComponents()");
		boolean julesComponentsEqual = true;

		if (!(checkJulesEntities())) {
			System.out.println("ATTENTION! JULES ENTITIES COULD NOT BE COMPARED!");
			return false;
		} // of if

		if (!(checkJulesRelations())) {
			System.out.println("ATTENTION! JULES RELATIONS COULD NOT BE COMPARED!");
			return false;
		} // of if

		/**
		 * if (!(checkJulesEvents())) { System.out.println("ATTENTION JULES EVENTS COULD NOT BE COMPARED!"); return
		 * false; } // of if
		 */
		return julesComponentsEqual;
	} // checkGeneratedJulesComponents

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkJulesEntities() {
		System.out.println("CALL checkJulesEntities()");
		boolean julesEntityEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.EntityMention.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.EntityMention.type);

		while (aceReaderIterator.hasNext()) {
			de.julielab.jcore.types.EntityMention aceReaderJulesEntity = (de.julielab.jcore.types.EntityMention) aceReaderIterator
					.next();
			de.julielab.jcore.types.EntityMention testReaderJulesEntity = (de.julielab.jcore.types.EntityMention) testReaderIterator
					.next();

			System.out.println("Entities ACE reader " + aceReaderJulesEntity.getCoveredText() + " test reader "
					+ testReaderJulesEntity.getCoveredText());

			if (!(aceReaderJulesEntity.getSpecificType().equals(testReaderJulesEntity.getSpecificType()))) {
				System.out.println("ATTENTION! JULES ENTITY SPECIFIC TYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEntity.getMentionLevel().equals(testReaderJulesEntity.getMentionLevel()))) {
				System.out.println("ATTENTION! JULES ENTITY MENTION LEVELS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEntity.getBegin() == testReaderJulesEntity.getBegin())) {
				System.out.println("ATTENTION! JULES ENTITY BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEntity.getEnd() == testReaderJulesEntity.getEnd() + 1)) {
				System.out.println("ATTENTION! JULES ENTITY ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEntity.getHead().getBegin() == testReaderJulesEntity.getHead().getBegin())) {
				System.out.println("ATTENTION JULES ENTITY MENTION HEAD BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEntity.getHead().getEnd() == testReaderJulesEntity.getHead().getEnd() + 1)) {
				System.out.println("ATTENTION! JULES ENTITY MENTION HEAD ENDS UNEQUAL!");
				return false;
			} // of if

		} // OF WHILE

		return julesEntityEqual;
	} // of checkJulesEntities

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkJulesRelations() {
		System.out.println("CALL checkJulesRelations()");
		boolean juleRelationEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.RelationMention.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.RelationMention.type);

		while (aceReaderIterator.hasNext()) {
			de.julielab.jcore.types.RelationMention aceReaderRelation = (de.julielab.jcore.types.RelationMention) aceReaderIterator
					.next();
			de.julielab.jcore.types.RelationMention testReaderRelation = (de.julielab.jcore.types.RelationMention) testReaderIterator
					.next();

			if (!(aceReaderRelation.getSpecificType()).equals(testReaderRelation.getSpecificType())) {
				System.out.println("ATTENTION! JULES READER RELATION SPECIFIC TYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getModality().equals(testReaderRelation.getModality()))) {
				System.out.println("ATTENTION! JULES READER RELATION MODALITY UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getTense().equals(testReaderRelation.getTense()))) {
				System.out.println("ATTENTION JULES READER RELATION TESES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getBegin() == testReaderRelation.getBegin())) {
				System.out.print("ATTENTION! JULES READER RELATION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getEnd() == testReaderRelation.getEnd() + 1)) {
				System.out.println("ATTENTION! JULES READER RELATION ENDS UNEQUAL!");
				return false;
			} // of if

			if (!checkJulesRelationArguments(aceReaderRelation, testReaderRelation)) {
				System.out.println("ATTENTION! JULES RELATION ARGUMENTS UNEQUAL!");
				return false;
			} // of if

		} // of while

		return juleRelationEqual;
	} // of checkJulesRelations

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkJulesRelationArguments(de.julielab.jcore.types.RelationMention aceReaderRelation,
			de.julielab.jcore.types.RelationMention testReaderRelation) {
		System.out.println("CALL checkJulesRelationArguments()");
		boolean julesRelationArgumentEqual = true;

		de.julielab.jcore.types.EntityMention aceReaderArgument1 = (EntityMention) ((ArgumentMention) aceReaderRelation
				.getArguments().get(0)).getRef();
		de.julielab.jcore.types.EntityMention testReaderArgument1 = (EntityMention) ((ArgumentMention) testReaderRelation
				.getArguments().get(0)).getRef();

		de.julielab.jcore.types.EntityMention aceReaderArgument2 = (EntityMention) ((ArgumentMention) aceReaderRelation
				.getArguments().get(1)).getRef();
		de.julielab.jcore.types.EntityMention testReaderArgument2 = (EntityMention) ((ArgumentMention) aceReaderRelation
				.getArguments().get(1)).getRef();

		System.out.println("ace reader relation:" + aceReaderRelation.getClass().getName() + "test reader relation"
				+ testReaderRelation.getClass().getName() + "  arguments " + "ACE reader 1 "
				+ aceReaderArgument1.getCoveredText() + " 2  " + aceReaderArgument2.getCoveredText() + "test reader 1 "
				+ testReaderArgument1.getCoveredText() + " 2 " + testReaderArgument2.getCoveredText());

		if (!(aceReaderArgument1.getBegin() == testReaderArgument1.getBegin())) {
			System.out.println("ATTENTION! JULES RELATION ARGUMENT ARG1 BEGINS UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderArgument1.getEnd() == (testReaderArgument1.getEnd() + 1))) {
			System.out.println("ATTENTION! JULES RELATION ARGUMENT ARG1 ENDS UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderArgument2.getBegin() == testReaderArgument2.getBegin())) {
			System.out.println("ATTENTION! JULES RELATION ARGUMENT ARG2 BEGINS UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderArgument2.getEnd() == (testReaderArgument2.getEnd()))) {
			System.out.println("ATTENTION! JULES RELATION ARGUMENT ARG2 ENDS UNEQUAL!");
			return false;
		} // of if

		return julesRelationArgumentEqual;
	} // of checkJulesRelationArguments

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkJulesEvents() {
		System.out.println("CALL checkJulesEvents()");
		boolean julesEventEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.Event.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.Event.type);

		while (aceReaderIterator.hasNext()) {
			de.julielab.jcore.types.EventMention aceReaderJulesEvent = (de.julielab.jcore.types.EventMention) aceReaderIterator
					.next();
			de.julielab.jcore.types.EventMention testReaderJulesEvent = (de.julielab.jcore.types.EventMention) testReaderIterator
					.next();

			if (!(aceReaderJulesEvent.getSpecificType().equals(testReaderJulesEvent.getSpecificType()))) {
				System.out.println("ATTENTION! JULES EVENT SPECIFIC TYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getGenericity().equals(testReaderJulesEvent.getGenericity()))) {
				System.out.println("ATTENTION! JULES EVENT GENERICITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getModality().equals(testReaderJulesEvent.getModality()))) {
				System.out.println("ATTENTION! JULES EVENT MODALITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getTense().equals(testReaderJulesEvent.getTense()))) {
				System.out.println("ATTENTION! JULES EVENT TENSES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getPolarity().equals(testReaderJulesEvent.getPolarity()))) {
				System.out.println("ATTENTION! JULES EVENT POLARITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getBegin() == testReaderJulesEvent.getBegin())) {
				System.out.println("ATTENTION! JULES EVENT BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getEnd() == testReaderJulesEvent.getEnd() + 1)) {
				System.out.println("ATTENTION! JULES EVENT ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getArguments().size() == testReaderJulesEvent.getArguments().size())) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!(checkJulesEventArguments(aceReaderJulesEvent, testReaderJulesEvent))) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENTS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getBegin() == testReaderJulesEvent.getBegin())) {
				System.out.print("ATTENTION! JULES EVENT ANCHOR BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEvent.getEnd() == testReaderJulesEvent.getEnd() + 1)) {
				System.out.print("ATTENTION! JULES EVENT ANCHOR ENDS UNEQUAL!");
				return false;
			} // of if

		} // of while

		return julesEventEqual;
	} // checkJulesEvents

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkJulesEventArguments(de.julielab.jcore.types.EventMention aceReaderJulesEvent,
			de.julielab.jcore.types.EventMention testReaderJulesEvent) {
		System.out.println("CALL checkJulesEventArgument()");
		boolean julesEventArgumentEqual = true;

		FSArray aceReaderJulesEventArgumentFSArray = aceReaderJulesEvent.getArguments();
		FSArray testReaderJulesEventArgumentFSArray = testReaderJulesEvent.getArguments();

		for (int i = 0; i < aceReaderJulesEventArgumentFSArray.size(); i++) {
			de.julielab.jcore.types.ArgumentMention aceReaderJulesEventArgument = (de.julielab.jcore.types.ArgumentMention) aceReaderJulesEventArgumentFSArray
					.get(i);
			de.julielab.jcore.types.ArgumentMention testReaderJulesEventArgument = (de.julielab.jcore.types.ArgumentMention) testReaderJulesEventArgumentFSArray
					.get(i);

			if (!(aceReaderJulesEventArgument.getBegin() == testReaderJulesEventArgument.getBegin())) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEventArgument.getEnd() == testReaderJulesEventArgument.getEnd() + 1)) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEventArgument.getRole().equals(testReaderJulesEventArgument.getRole()))) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEventArgument.getRef().getBegin() == testReaderJulesEventArgument.getRef().getBegin())) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderJulesEventArgument.getRef().getEnd() == testReaderJulesEventArgument.getRef().getEnd() + 1)) {
				System.out.println("ATTENTION! JULES EVENT ARGUMENT MENTION ENDS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return julesEventArgumentEqual;
	} // of checkJulesEventArguments

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkSourceFile() {
		boolean sourceFileEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.ace.SourceFile.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.ace.SourceFile.type);

		de.julielab.jcore.types.ace.SourceFile aceReaderSourceFile = (de.julielab.jcore.types.ace.SourceFile) aceReaderIterator
				.next();
		de.julielab.jcore.types.ace.SourceFile testReaderSourceFile = (de.julielab.jcore.types.ace.SourceFile) testReaderIterator
				.next();

		if (!(aceReaderSourceFile.getUri().equals(testReaderSourceFile.getUri()))) {
			System.out.println("ERROR! SOURCE FILE: URI UNEQUAL");
			return false;
		} // of if

		if (!(aceReaderSourceFile.getAuthor().equals(testReaderSourceFile.getAuthor()))) {
			System.out.println("ERROR! SOURCE FILE: AUTHOR UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderSourceFile.getAce_type().equals(testReaderSourceFile.getAce_type()))) {
			System.out.println("ERROR! SOURCE FILE: ACE TYPE UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderSourceFile.getSource().equals(testReaderSourceFile.getSource()))) {
			System.out.println("ERROR! SOURCE FILE: SOURCE UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderSourceFile.getEncoding().equals(testReaderSourceFile.getEncoding()))) {
			System.out.println("ERROR! SOURCE FILE: ENCODING UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderSourceFile.getDocuments().size() == testReaderSourceFile.getDocuments().size())) {
			System.out.println("ERROR! Documents FSArrays of different sizes");
			return false;
		} // of if

		if (!checkDocument()) {
			System.out.println("ERROR! DOCUMENTS UNEQUAL!");
			return false;
		} // of if

		return sourceFileEqual;
	} // checkSourceFile

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkDocument() {
		boolean documentEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.ace.Document.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.ace.Document.type);

		de.julielab.jcore.types.ace.Document aceReaderDocument = (de.julielab.jcore.types.ace.Document) aceReaderIterator
				.next();
		de.julielab.jcore.types.ace.Document testReaderDocument = (de.julielab.jcore.types.ace.Document) testReaderIterator
				.next();

		if (!(aceReaderDocument.getDocid().equals(testReaderDocument.getDocid()))) {
			System.out.println("ERROR! DOCUMENT ID UNEQUAL");
			return false;
		} // of if

		if (!(aceReaderDocument.getEntities().size() == testReaderDocument.getEntities().size())) {
			System.out.println("ERROR! ENTITY FSARRAY SIZES UNEQUAL!");
			return false;
		} // of if

		if (!checkEntities()) {
			System.out.println("ERROR! ENTITIES UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderDocument.getValues().size() == testReaderDocument.getValues().size())) {
			System.out.println("ERROR! VALUE FSARRAY SIZES UNEQUAL!");
			return false;
		} // of if

		if (!checkValues(aceReaderDocument, testReaderDocument)) {
			System.out.println("ERROR! VALUES UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderDocument.getTimex2().size() == testReaderDocument.getTimex2().size())) {
			System.out.println("ERROR! TIMEX2 FSARRAY SIZES UNEQUAL!");
			return false;
		} // of if

		if (!checkTimex2(aceReaderDocument, testReaderDocument)) {
			System.out.println("ERROR! TIMEX2 UNEQUAL!");
			return false;
		} // of if

		if (!(aceReaderDocument.getRelations().size() == testReaderDocument.getRelations().size())) {
			System.out.println("ERROR! RELATION FSARRAY SIZES UNEQUAL!");
			return false;
		} // of if

		if (!checkRelations(aceReaderDocument, testReaderDocument)) {
			System.out.println("ERROR! RELATIONS UNEQUAL!");
			return false;
		} // of if

		/*
		 * if (!(aceReaderDocument.getEvents().size() == testReaderDocument .getEvents().size())) {
		 * System.out.println("ERROR! EVENT FSARRAY SIZES UNEQUAL!"); return false; } // of if
		 */
		if (!checkEvents(aceReaderDocument, testReaderDocument)) {
			System.out.println("ERROR! EVENTS UNEQUAL!");
			return false;
		} // of if

		return documentEqual;
	} // of checkDocument

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEvents(Document aceReaderDocument, Document testReaderDocument) {
		System.out.println("CALL checkEvents()");
		boolean eventEqual = true;

		FSArray aceReaderEventFSArray = aceReaderDocument.getEvents();
		FSArray testReaderEventFSArray = testReaderDocument.getEvents();

		for (int i = 0; i < aceReaderEventFSArray.size(); i++) {
			de.julielab.jcore.types.ace.Event aceReaderEvent = (de.julielab.jcore.types.ace.Event) aceReaderEventFSArray
					.get(i);
			de.julielab.jcore.types.ace.Event testReaderEvent = (de.julielab.jcore.types.ace.Event) testReaderEventFSArray
					.get(i);

			if (!(aceReaderEvent.getId().equals(testReaderEvent.getId()))) {
				System.out.println("ERROR! EVENT IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getAce_subtype().equals(testReaderEvent.getAce_subtype()))) {
				System.out.println("ERROR! EVENT SUBTYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getAce_type()).equals(testReaderEvent.getAce_type())) {
				System.out.println("ERROR! EVENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getPolarity().equals(testReaderEvent.getPolarity()))) {
				System.out.println("ERROR! EVENT POLARITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getTense().equals(testReaderEvent.getTense()))) {
				System.out.println("ERROR! EVENT TENSES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getModality().equals(testReaderEvent.getModality()))) {
				System.out.println("ERROR! EVENT MODALITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getGenericity().equals(testReaderEvent.getGenericity()))) {
				System.out.println("ERROR! EVENT GENERICITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getArguments().size() == testReaderEvent.getArguments().size())) {
				System.out.println("ERROR! EVENT ARGUMENT FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkEventArguments(aceReaderEvent, testReaderEvent)) {
				System.out.println("ERROR! EVENT ARGUMENTS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEvent.getMentions().size() == testReaderEvent.getMentions().size())) {
				System.out.println("ERROR! EVENT MENTION FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkEventMentions(aceReaderEvent, testReaderEvent)) {
				System.out.println("ERROR! EVENT MENTIONS UNEQUAL!");
				return false;
			} // of if
		} // of for

		return eventEqual;
	} // of checkEvents

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEventMentions(Event aceReaderEvent, Event testReaderEvent) {
		boolean eventMentionEqual = true;

		FSArray aceReaderEventMentionFSArray = aceReaderEvent.getMentions();
		FSArray testReaderEventMentionFSArray = testReaderEvent.getMentions();

		for (int i = 0; i < aceReaderEventMentionFSArray.size(); i++) {
			de.julielab.jcore.types.ace.EventMention aceReaderEventMention = (de.julielab.jcore.types.ace.EventMention) aceReaderEventMentionFSArray
					.get(i);
			de.julielab.jcore.types.ace.EventMention testReaderEventMention = (de.julielab.jcore.types.ace.EventMention) testReaderEventMentionFSArray
					.get(i);

			if (!(aceReaderEventMention.getId().equals(testReaderEventMention.getId()))) {
				System.out.println("ERROR! EVENT MENTION IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getBegin() == testReaderEventMention.getBegin())) {
				System.out.println("ERROR! EVENT MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getEnd() == testReaderEventMention.getEnd() + 1)) {
				System.out.println("ERROR! EVENT MENTION ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getLdc_scope().getBegin() == testReaderEventMention.getLdc_scope().getBegin())) {
				System.out.println("ERROR! EVENT MENTION LDCSCOPE BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getLdc_scope().getEnd() == testReaderEventMention.getLdc_scope().getEnd() + 1)) {
				System.out.println("ERROR! EVENT MENTION LDCSTOPE ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getAnchor().getBegin() == testReaderEventMention.getAnchor().getBegin())) {
				System.out.println("ERROR! EVENT MENTION ANCHOR BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getAnchor().getEnd() == testReaderEventMention.getAnchor().getEnd() + 1)) {
				System.out.println("ERROR! EVENT MENTION ANCHOR ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMention.getArguments().size() == testReaderEventMention.getArguments().size())) {
				System.out.println("ERROR! EVENT MENTION ARGUMENT FSARRAY SIZES UNEQUAL!");
				return false;
			}

			if (!checkEventMentionArguments(aceReaderEventMention, testReaderEventMention)) {
				System.out.println("ERROR! EVENT MENTION ARGUMENTS UNEQUAL!");
				return false;
			} // of if
		} // of for

		return eventMentionEqual;
	} // checkEventMentions

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEventMentionArguments(EventMention aceReaderEventMention, EventMention testReaderEventMention) {
		boolean eventMentionArgumentEqual = true;

		FSArray aceReaderEventMentionArgumentFSArray = aceReaderEventMention.getArguments();
		FSArray testReaderEventMentionArgumentFSArray = testReaderEventMention.getArguments();

		for (int i = 0; i < aceReaderEventMentionArgumentFSArray.size(); i++) {
			de.julielab.jcore.types.ace.EventMentionArgument aceReaderEventMentionArgument = (de.julielab.jcore.types.ace.EventMentionArgument) aceReaderEventMentionArgumentFSArray
					.get(i);
			de.julielab.jcore.types.ace.EventMentionArgument testReaderEventMentionArgument = (de.julielab.jcore.types.ace.EventMentionArgument) testReaderEventMentionArgumentFSArray
					.get(i);

			if (!(aceReaderEventMentionArgument.getAce_role().equals(testReaderEventMentionArgument.getAce_role()))) {
				System.out.println("ERROR! EVENT MENTION ARGUMENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMentionArgument.getRefid().equals(testReaderEventMentionArgument.getRefid()))) {
				System.out.println("ERROR! EVENT MENTION ARGUMENT REFIDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMentionArgument.getBegin() == testReaderEventMentionArgument.getBegin())) {
				System.out.println("ERROR! EVENT MENTION ARGUMENT BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventMentionArgument.getEnd() == testReaderEventMentionArgument.getEnd() + 1)) {
				System.out.println("ERROR! EVENT MENTION ARGUMENT ENDS UNEQUAL!");
				return false;
			} // of if
		} // of for

		return eventMentionArgumentEqual;
	} // of checkEventMentionArguments

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEventArguments(Event aceReaderEvent, Event testReaderEvent) {
		boolean eventArgumentEqual = true;

		FSArray aceReaderEventArgumentFSArray = aceReaderEvent.getArguments();
		FSArray testReaderEventArgumentFSArray = testReaderEvent.getArguments();

		for (int i = 0; i < aceReaderEventArgumentFSArray.size(); i++) {
			de.julielab.jcore.types.ace.EventArgument aceReaderEventArgument = (de.julielab.jcore.types.ace.EventArgument) aceReaderEventArgumentFSArray
					.get(i);
			de.julielab.jcore.types.ace.EventArgument testReaderEventArgument = (de.julielab.jcore.types.ace.EventArgument) testReaderEventArgumentFSArray
					.get(i);

			if (!(aceReaderEventArgument.getAce_role().equals(testReaderEventArgument.getAce_role()))) {
				System.out.println("ERROR! EVENT ARGUMENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEventArgument.getRefid().equals(testReaderEventArgument.getRefid()))) {
				System.out.println("ERROR! EVENT ARGUMENT REFIDs UNEQUAL!");
				return false;
			} // of if
		} // of for

		return eventArgumentEqual;
	} // of checkEventArguments

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkRelations(Document aceReaderDocument, Document testReaderDocument) {
		boolean relationEqual = true;

		FSArray aceReaderRelationFSArray = aceReaderDocument.getRelations();
		FSArray testReaderRelationFSArray = testReaderDocument.getRelations();

		for (int i = 0; i < aceReaderRelationFSArray.size(); i++) {
			de.julielab.jcore.types.ace.Relation aceReaderRelation = (de.julielab.jcore.types.ace.Relation) aceReaderRelationFSArray
					.get(i);
			de.julielab.jcore.types.ace.Relation testReaderRelation = (de.julielab.jcore.types.ace.Relation) testReaderRelationFSArray
					.get(i);

			if (!(aceReaderRelation.getId().equals(testReaderRelation.getId()))) {
				System.out.println("ERROR! RELATION IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getModality().equals(testReaderRelation.getModality()))) {
				System.out.println("ERROR! RELATION MODALITIES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getAce_type().equals(testReaderRelation.getAce_type()))) {
				System.out.println("ERROR! RELATION TYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getTense().equals(testReaderRelation.getTense()))) {
				System.out.println("ERROR! RELATION TENSES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getAce_subtype().equals(testReaderRelation.getAce_subtype()))) {
				System.out.println("ERROR! RELATION SUBTYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getArguments().size() == testReaderRelation.getArguments().size())) {
				System.out.println("ERROR! RELATION ARGUMENT FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkRelationArguments(aceReaderRelation, testReaderRelation)) {
				System.out.println("ERROR! RELATION ARGUMENTS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelation.getMentions().size() == testReaderRelation.getMentions().size())) {
				System.out.println("ERROR! RELATION MENTION FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkRelationMentions(aceReaderRelation, testReaderRelation)) {
				System.out.println("ERROR! RELATION MENTIONS UNEQUAL!");
				return false;
			} // of if

		} // of if

		return relationEqual;
	} // of checkRelations

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkRelationMentions(Relation aceReaderRelation, Relation testReaderRelation) {
		boolean relationMentionEqual = true;

		FSArray aceReaderRelationMentionFSArray = aceReaderRelation.getMentions();
		FSArray testReaderRelationMentionFSArray = testReaderRelation.getMentions();

		for (int i = 0; i < aceReaderRelationMentionFSArray.size(); i++) {
			de.julielab.jcore.types.ace.RelationMention aceReaderRelationMention = (de.julielab.jcore.types.ace.RelationMention) aceReaderRelationMentionFSArray
					.get(i);
			de.julielab.jcore.types.ace.RelationMention testReaderRelationMention = (de.julielab.jcore.types.ace.RelationMention) testReaderRelationMentionFSArray
					.get(i);

			if (!(aceReaderRelationMention.getId().equals(testReaderRelationMention.getId()))) {
				System.out.println("ERROR! RELATION MENTION IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMention.getLexical_condition().equals(testReaderRelationMention
					.getLexical_condition()))) {
				System.out.println("ERROR! RELATION MENTION LEXICAL CONDITIONS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMention.getBegin() == testReaderRelationMention.getBegin())) {
				System.out.println("ERROR! RELATION MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMention.getEnd() == testReaderRelationMention.getEnd() + 1)) {
				System.out.println("ERROR! RELATION MENTION ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMention.getRelation_ref().getId().equals(testReaderRelationMention.getRelation_ref()
					.getId()))) {
				System.out.println("ERROR! RELATION MENTION RELATION REFERENCES UNEQUAL!");
				return false;
			}

			if (!(aceReaderRelationMention.getArguments().size() == testReaderRelationMention.getArguments().size())) {
				System.out.println("ERROR! RELATION MENTION ARGUMENT FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkRelationMentionArguments(aceReaderRelationMention, testReaderRelationMention)) {
				System.out.println("ERROR! RELATION MENTION ARGUMENTS UNEQUAL!");
				return false;
			} // of if

		} // of if

		return relationMentionEqual;
	} // checkRelationMentions

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkRelationMentionArguments(RelationMention aceReaderRelationMention,
			RelationMention testReaderRelationMention) {
		boolean relationMentionArgumentEqual = true;

		FSArray aceReaderRelationMentionArgumentFSArray = aceReaderRelationMention.getArguments();
		FSArray testReaderRelationMentionArgumentFSArray = testReaderRelationMention.getArguments();

		for (int i = 0; i < aceReaderRelationMentionArgumentFSArray.size(); i++) {
			de.julielab.jcore.types.ace.RelationMentionArgument aceReaderRelationMentionArgument = (de.julielab.jcore.types.ace.RelationMentionArgument) aceReaderRelationMentionArgumentFSArray
					.get(i);
			de.julielab.jcore.types.ace.RelationMentionArgument testReaderRelationMentionArgument = (de.julielab.jcore.types.ace.RelationMentionArgument) testReaderRelationMentionArgumentFSArray
					.get(i);

			if (!(aceReaderRelationMentionArgument.getRefid().equals(testReaderRelationMentionArgument.getRefid()))) {
				System.out.println("ERROR! RELATION MENTION ARGUMENT REFIDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMentionArgument.getAce_role()
					.equals(testReaderRelationMentionArgument.getAce_role()))) {
				System.out.println("ERROR! RELATION MENTION ARGUMENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMentionArgument.getBegin() == testReaderRelationMentionArgument.getBegin())) {
				System.out.println("ERROR! RELATION MENTION ARGUMENT BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationMentionArgument.getEnd() == testReaderRelationMentionArgument.getEnd() + 1)) {
				System.out.println("ERROR! RELATION MENTION ARGUMENT ENDS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return relationMentionArgumentEqual;
	}

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkRelationArguments(Relation aceReaderRelation, Relation testReaderRelation) {
		boolean relationArgumentEqual = true;

		FSArray aceReaderRelationArgumentFSArray = aceReaderRelation.getArguments();
		FSArray testReaderRelationArgumentFSArray = testReaderRelation.getArguments();

		for (int i = 0; i < aceReaderRelationArgumentFSArray.size(); i++) {
			de.julielab.jcore.types.ace.RelationArgument aceReaderRelationArgument = (de.julielab.jcore.types.ace.RelationArgument) aceReaderRelationArgumentFSArray
					.get(i);
			de.julielab.jcore.types.ace.RelationArgument testReaderRelationArgument = (de.julielab.jcore.types.ace.RelationArgument) testReaderRelationArgumentFSArray
					.get(i);

			if (!(aceReaderRelationArgument.getAce_role().equals(testReaderRelationArgument.getAce_role()))) {
				System.out.println("ERROR! RELATION ARGUMENT ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderRelationArgument.getRefid().equals(testReaderRelationArgument.getRefid()))) {
				System.out.println("ERROR! RELATION ARGUMENT REFIDs UNEQUAL!");
				return false;
			} // of if
		} // of for

		return relationArgumentEqual;
	} // checkRelationArguments

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkTimex2(Document aceReaderDocument, Document testReaderDocument) {
		boolean timex2Equal = true;

		FSArray aceReaderTimex2FSArray = aceReaderDocument.getTimex2();
		FSArray testReaderTimex2FSArray = testReaderDocument.getTimex2();

		for (int i = 0; i < aceReaderTimex2FSArray.size(); i++) {
			de.julielab.jcore.types.ace.Timex2 aceReaderTimex2 = (de.julielab.jcore.types.ace.Timex2) aceReaderTimex2FSArray
					.get(i);
			de.julielab.jcore.types.ace.Timex2 testReaderTimex2 = (de.julielab.jcore.types.ace.Timex2) testReaderTimex2FSArray
					.get(i);

			if (!(aceReaderTimex2.getId().equals(testReaderTimex2.getId()))) {
				System.out.println("ERROR! TIMEX2 IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderTimex2.getMentions().size() == testReaderTimex2.getMentions().size())) {
				System.out.println("ERROR! TIMEX2 MENTION FSARRAY SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkTimex2Mentions(aceReaderTimex2, testReaderTimex2)) {
				System.out.println("ERROR! TIMEX2 MENTIONS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return timex2Equal;
	} // checkTimex2

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkTimex2Mentions(Timex2 aceReaderTimex2, Timex2 testReaderTimex2) {
		boolean timex2MentionEqual = true;

		FSArray aceReaderTimex2MentionFSArray = aceReaderTimex2.getMentions();
		FSArray testReaderTimex2MentionFSArray = testReaderTimex2.getMentions();

		for (int i = 0; i < aceReaderTimex2MentionFSArray.size(); i++) {
			de.julielab.jcore.types.ace.Timex2Mention aceReaderTimex2Mention = (de.julielab.jcore.types.ace.Timex2Mention) aceReaderTimex2MentionFSArray
					.get(i);
			de.julielab.jcore.types.ace.Timex2Mention testReaderTimex2Mention = (de.julielab.jcore.types.ace.Timex2Mention) testReaderTimex2MentionFSArray
					.get(i);

			if (!(aceReaderTimex2Mention.getId().equals(testReaderTimex2Mention.getId()))) {
				System.out.println("ERROR! TIMEX2 MENTION IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderTimex2Mention.getBegin() == testReaderTimex2Mention.getBegin())) {
				System.out.println("ERROR! TIMEX2 MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderTimex2Mention.getEnd() == testReaderTimex2Mention.getEnd() + 1)) {
				System.out.println("ERROR! TIMEX2 MENTION ENDS UNEQUAL!");
				return false;
			} // of if
		} // of for

		return timex2MentionEqual;
	} // of checkTimex2Mentions

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkValues(Document aceReaderDocument, Document testReaderDocument) {
		boolean valueEqual = true;

		FSArray aceReaderValueFSArray = aceReaderDocument.getValues();
		FSArray testReaderValueFSArray = testReaderDocument.getValues();

		for (int i = 0; i < aceReaderValueFSArray.size(); i++) {
			de.julielab.jcore.types.ace.Value aceReaderValue = (de.julielab.jcore.types.ace.Value) aceReaderValueFSArray
					.get(i);
			de.julielab.jcore.types.ace.Value testReaderValue = (de.julielab.jcore.types.ace.Value) testReaderValueFSArray
					.get(i);

			if (!(aceReaderValue.getAce_type().equals(testReaderValue.getAce_type()))) {
				System.out.println("ERROR! VALUE TYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderValue.getId().equals(testReaderValue.getId()))) {
				System.out.println("ERROR! VALUE IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderValue.getAce_subtype().equals(testReaderValue.getAce_subtype()))) {
				System.out.println("ERROR! VALUE SUBTYPES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderValue.getMentions().size() == testReaderValue.getMentions().size())) {
				System.out.println("ERROR! VALUE MENTION SIZES UNEQUAL!");
				return false;
			} // of if

			if (!checkValueMentions(aceReaderValue, testReaderValue)) {
				System.out.println("ERROR! VALUE MENTIONS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return valueEqual;
	} // of checkValues

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkValueMentions(Value aceReaderValue, Value testReaderValue) {
		boolean valueMentionEqual = true;

		FSArray aceReaderValueMentionFSArray = aceReaderValue.getMentions();
		FSArray testReaderValueMentionFSArray = testReaderValue.getMentions();

		for (int i = 0; i < aceReaderValueMentionFSArray.size(); i++) {
			de.julielab.jcore.types.ace.ValueMention aceReaderValueMention = (de.julielab.jcore.types.ace.ValueMention) aceReaderValueMentionFSArray
					.get(i);
			de.julielab.jcore.types.ace.ValueMention testReaderValueMention = (de.julielab.jcore.types.ace.ValueMention) testReaderValueMentionFSArray
					.get(i);

			if (!(aceReaderValueMention.getId().equals(testReaderValueMention.getId()))) {
				System.out.println("ERROR! VALUE MENTION IDs NOT EQUAL!");
				return false;
			} // of if

			if (!(aceReaderValueMention.getBegin() == testReaderValueMention.getBegin())) {
				System.out.println("ERROR! VALUE MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderValueMention.getEnd() == testReaderValueMention.getEnd() + 1)) {
				System.out.println("ERROR! VALUE MENTION ENDS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return valueMentionEqual;
	} // of checkValueMentions

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEntities() {
		boolean entityEqual = true;

		Iterator aceReaderIterator = getTypeIterator(aceReaderCas, de.julielab.jcore.types.ace.Entity.type);
		Iterator testReaderIterator = getTypeIterator(testReaderCas, de.julielab.jcore.types.ace.Entity.type);

		try {
			while (aceReaderIterator.hasNext()) {
				de.julielab.jcore.types.ace.Entity aceReaderEntity = (de.julielab.jcore.types.ace.Entity) aceReaderIterator
						.next();
				de.julielab.jcore.types.ace.Entity testReaderEntity = (de.julielab.jcore.types.ace.Entity) testReaderIterator
						.next();

				// It is assumed, that both of the iterators contain the enti-
				// ties in the same order. In other words: the very first entity
				// in the aceReaderIterator is corresponding to the very first
				// entity in the testReaderIterator.
				if (!(aceReaderEntity.getId().equals(testReaderEntity.getId()))) {
					System.out.println("ERROR! ENTITY IDs UNEQUAL!");
					return false;
				} // of if

				if (!(aceReaderEntity.getAce_class().equals(testReaderEntity.getAce_class()))) {
					System.out.println("ERROR! ENTITY CLASSES UNEQUAL!");
					return false;
				} // of if

				if (!(aceReaderEntity.getAce_type().equals(testReaderEntity.getAce_type()))) {
					System.out.println("ERROR! ENTITY TYPES UNEQUAL!");
					return false;
				} // of if

				if (!(aceReaderEntity.getAce_subtype().equals(testReaderEntity.getAce_subtype()))) {
					System.out.println("ERROR! ENTITY SUBTYPE UNEQUAL!");
					return false;
				} // of if

				if (!(aceReaderEntity.getEntity_mentions() == null) && !(testReaderEntity.getEntity_mentions() == null)) {
					if (!(aceReaderEntity.getEntity_mentions().size() == testReaderEntity.getEntity_mentions().size())) {
						System.out.println("ERROR! ENTITY MENTION FSARRAY SIZES UNEQUAL!");
						return false;
					} // of if

					if (!(checkEntityMentions(aceReaderEntity, testReaderEntity))) {
						System.out.println("ERROR! ENTITY MENTIONS UNEQUAL");
						return false;
					} // of if

				} // of if
				else {
					System.out.println("ATTENTION! CHECK IF THE ENTITY MENTION FSARRAYS ARE INITIALIZED!");
				} // of else

				if (!(aceReaderEntity.getEntity_attributes() == null)
						&& !(testReaderEntity.getEntity_attributes() == null)) {
					if (!(aceReaderEntity.getEntity_attributes().size() == testReaderEntity.getEntity_attributes()
							.size())) {
						System.out.println("ERROR! ENTITY ATTRIBUTES FSARRAY SIZES UNEQUAL!");
						return false;
					} // of if

					if (!(checkEntityAttributes(aceReaderEntity, testReaderEntity))) {
						System.out.println("ERROR! ENTITY ATTRIBUTES UNEQUAL!");
						return false;
					} // of if
				} // of if
				else {
					System.out.println("ATTENTION! CHECK IF THE ENTITY ATTRIBUTES FSARRAYS ARE INITIALIZED!");
				} // of else

			} // of while

		} // of try
		catch (Exception e) {
			System.out.println("ERROR! UNABLE TO COMPARE ENTITIES.");
			e.printStackTrace();
			return false;
		} // of catch

		return entityEqual;
	} // checkEntities

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEntityAttributes(Entity aceReaderEntity, Entity testReaderEntity) {
		boolean entityAttributeEqual = true;
		FSArray aceReaderEntityAttributeFSArray = aceReaderEntity.getEntity_attributes();
		FSArray testReaderEntityAttributeFSArray = testReaderEntity.getEntity_attributes();

		for (int i = 0; i < aceReaderEntityAttributeFSArray.size(); i++) {
			de.julielab.jcore.types.ace.EntityAttribute aceReaderEntityAttribute = (de.julielab.jcore.types.ace.EntityAttribute) aceReaderEntityAttributeFSArray
					.get(i);
			de.julielab.jcore.types.ace.EntityAttribute testReaderEntityAttribute = (de.julielab.jcore.types.ace.EntityAttribute) testReaderEntityAttributeFSArray
					.get(i);

			if (!(aceReaderEntityAttribute.getNames() == null) && !(testReaderEntityAttribute.getNames() == null)) {
				if (!(aceReaderEntityAttribute.getNames().size() == testReaderEntityAttribute.getNames().size())) {
					System.out.println("ERROR! ENTITY ATTRIBUTE NAMES FSARRAY SIZES UNEQUAL!");
					return false;
				} // of if

				if (!checkEntityAttributesNames(aceReaderEntityAttribute, testReaderEntityAttribute)) {
					System.out.println("ERROR! ENTITY ATTRIBUTE NAMES UNEQUAL!");
					return false;
				} // of if
			} // of if
			else {
				System.out.println("ATTENTION! CHECK IF THE ENTITY ATTRIBUTE NAMES FSARRAYS ARE INITIALIZED!");
			} // of else

		} // of for

		return entityAttributeEqual;
	} // of checkEntityAttributes

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEntityAttributesNames(EntityAttribute aceReaderEntityAttribute,
			EntityAttribute testReaderEntityAttribute) {
		boolean entityAttributesNamesEqual = true;
		FSArray aceReaderEntityAttributesNamesFSArray = aceReaderEntityAttribute.getNames();
		FSArray testReaderEntityAttributesNamesFSArray = testReaderEntityAttribute.getNames();

		for (int i = 0; i < aceReaderEntityAttributesNamesFSArray.size(); i++) {
			de.julielab.jcore.types.ace.Name aceReaderEntityAttributeName = (de.julielab.jcore.types.ace.Name) aceReaderEntityAttributesNamesFSArray
					.get(i);
			de.julielab.jcore.types.ace.Name testReaderEntityAttributeName = (de.julielab.jcore.types.ace.Name) testReaderEntityAttributesNamesFSArray
					.get(i);

			if (!(aceReaderEntityAttributeName.getName().equals(testReaderEntityAttributeName.getName()))) {
				System.out.println("ERROR! ENTITY ATTRIBUTE NAMES UNEQUAL!");
				return false;
			}

			if (!(aceReaderEntityAttributeName.getBegin() == testReaderEntityAttributeName.getBegin())) {
				System.out.println("ERROR! ENTITY ATTRIBUTE NAME BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityAttributeName.getEnd() == testReaderEntityAttributeName.getEnd() + 1)) {
				System.out.println("ERROR! ENTITY ATTRIBUTE ENDS UNEQUAL!");
				return false;
			} // of if

		} // of for

		return entityAttributesNamesEqual;
	} // checkEntityAttributesNames

	/*----------------------------------------------------------------------------------------------*/
	private boolean checkEntityMentions(Entity aceReaderEntity, Entity testReaderEntity) {
		boolean entityMentionEqual = true;
		FSArray aceReaderEntityMentionFSArray = aceReaderEntity.getEntity_mentions();
		FSArray testReaderEntityMentionFSArray = testReaderEntity.getEntity_mentions();

		for (int i = 0; i < aceReaderEntityMentionFSArray.size(); i++) {
			de.julielab.jcore.types.ace.EntityMention aceReaderEntityMention = (de.julielab.jcore.types.ace.EntityMention) aceReaderEntityMentionFSArray
					.get(i);
			de.julielab.jcore.types.ace.EntityMention testReaderEntityMention = (de.julielab.jcore.types.ace.EntityMention) testReaderEntityMentionFSArray
					.get(i);

			if (!(aceReaderEntityMention.getId().equals(testReaderEntityMention.getId()))) {
				System.out.println("ERROR! ENTITY MENTION IDs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getMention_ldctype().equals(testReaderEntityMention.getMention_ldctype()))) {
				System.out.println("ERROR! ENTITY MENTION LDCTYPEs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getMention_type().equals(testReaderEntityMention.getMention_type()))) {
				System.out.println("ERROR! ENTITY MENTION TYPEs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getLdcatr().equals(testReaderEntityMention.getLdcatr()))) {
				System.out.println("ERROR! ENTITY MENTION LDCATRs UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getAce_role().equals(testReaderEntityMention.getAce_role()))) {
				System.out.println("ERROR! ENTITY MENTION ROLES UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getBegin() == testReaderEntityMention.getBegin())) {
				System.out.println("ERROR! ENTITY MENTION BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getEnd() == testReaderEntityMention.getEnd() + 1)) {
				System.out.println("ERROR! ENTITY MENTION ENDS UNEQUAL");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getHead().getBegin() == testReaderEntityMention.getHead().getBegin())) {
				System.out.println("ERROR! ENTITY MENTION HEAD BEGINS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getHead().getEnd() == testReaderEntityMention.getHead().getEnd() + 1)) {
				System.out.println("ERROR! ENTITY MENTION HEAD ENDS UNEQUAL!");
				return false;
			} // of if

			if (!(aceReaderEntityMention.getEntity_ref().getId()
					.equals(testReaderEntityMention.getEntity_ref().getId()))) {
				System.out.println("ERROR! ENTITY MENTION ENTITY REFERENCE IDs UNEQUAL!");
				return false;
			} // of if

		} // of for

		return entityMentionEqual;
	} // of checkEntityMentions

	/*----------------------------------------------------------------------------------------------*/
	private void buildSourceFile(JCas jcas) throws SAXException, IOException, ParserConfigurationException {
		de.julielab.jcore.types.ace.SourceFile sourceFile = new de.julielab.jcore.types.ace.SourceFile(jcas);

		sourceFile.setUri("XIN_ENG_20030624.0085.sgm");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		File sgmFile = new File(SGM_FILE);
		org.w3c.dom.Document sgmDomDocument = builder.parse(sgmFile);
		setDocumentText(testReaderCas, sgmDomDocument);

		sourceFile.setAuthor("LDC");
		sourceFile.setAce_type("text");
		sourceFile.setSource("newswire");
		sourceFile.setEncoding("UTF-8");
		buildDocument(jcas, sourceFile);
		sourceFile.addToIndexes();
	} // buildSourceFile

	/*----------------------------------------------------------------------------------------------*/
	private void setDocumentText(CAS testReaderCas2, org.w3c.dom.Document sgmDomDocument) {
		Node documentNode = sgmDomDocument.getDocumentElement();
		String documentText = documentNode.getTextContent();
		testReaderCas2.setDocumentText(documentText);
	} // of setDocumentText

	/*----------------------------------------------------------------------------------------------*/
	private void buildDocument(JCas jcas, SourceFile sourceFile) {
		de.julielab.jcore.types.ace.Document document = new de.julielab.jcore.types.ace.Document(jcas);
		document.setDocid("XIN_ENG_20030624.0085");
		buildEntities(jcas, document);
		buildValues(jcas, document);
		buildTimex2(jcas, document);
		buildRelations(jcas, document);
		buildEvents(jcas, document);

		buildJulesEntities(jcas, document);
		buildJulesRelations(jcas, document);
		// buildJulesEvents(jcas, document);

		document.addToIndexes();
		FSArray documentFSArray = new FSArray(jcas, 1);
		documentFSArray.set(0, document);
		documentFSArray.addToIndexes();
		sourceFile.setDocuments(documentFSArray);
	} // of buildDocument

	/*----------------------------------------------------------------------------------------------*/
	private void buildJulesEvents(JCas jcas, Document document) {
		Transaction event1 = new Transaction(jcas);
		event1.setSpecificType("Transfer-Money");
		event1.setGenericity("Specific");
		event1.setModality("Asserted");
		event1.setTense("Future");
		event1.setPolarity("Positive");
		event1.setBegin(663);
		event1.setEnd(667);
		buildJulesEventArgs(jcas, event1);

		event1.addToIndexes();
	} // of buildJulesEvents

	/*----------------------------------------------------------------------------------------------*/
	private void buildJulesEventArgs(JCas jcas, Transaction event1) {
		System.out.println("CALL buildJulesEventArgs()");

		de.julielab.jcore.types.ArgumentMention eventArg1 = new de.julielab.jcore.types.ArgumentMention(jcas);
		eventArg1.setRef(entity1_1);

		// System.out.println("MENTION: " + eventArg1.getMention().toString());

		eventArg1.setRole("Recipient");
		eventArg1.setBegin(763);
		eventArg1.setEnd(767);
		eventArg1.addToIndexes();

		de.julielab.jcore.types.ArgumentMention eventArg2 = new de.julielab.jcore.types.ArgumentMention(jcas);
		eventArg2.setRef(entity2_1);
		eventArg2.setRole("Recipient");
		eventArg2.setBegin(773);
		eventArg2.setEnd(778);
		eventArg2.addToIndexes();

		FSArray eventArgFSArray = new FSArray(jcas, 2);
		eventArgFSArray.set(0, eventArg1);
		eventArgFSArray.set(1, eventArg2);
		eventArgFSArray.addToIndexes();

		event1.setArguments(eventArgFSArray);
	} // buildJulesEventArgs

	/*----------------------------------------------------------------------------------------------*/
	private void buildJulesRelations(JCas jcas, Document document) {
		System.out.println("CALL buildJulesRelations()");
		PART_WHOLE relation1_1 = new PART_WHOLE(jcas);
		relation1_1.setBegin(543);
		relation1_1.setEnd(579);
		relation1_1.setSpecificType("Geographical");
		relation1_1.setModality("Asserted");
		relation1_1.setTense("Unspecified");
		FSArray arguments = new FSArray(jcas, 2);
		ArgumentMention arg1 = new ArgumentMention(jcas);
		arg1.setRef(entity1_2);
		arg1.setRole("arg1");
		ArgumentMention arg2 = new ArgumentMention(jcas);
		arg2.setRef(entity2_1);
		arg2.setRole("arg2");
		arguments.set(0, arg1);
		arguments.set(1, arg2);
		relation1_1.setArguments(arguments);
		relation1_1.addToIndexes();

		PART_WHOLE relation1_2 = new PART_WHOLE(jcas);
		relation1_2.setBegin(594);
		relation1_2.setEnd(616);
		relation1_2.setSpecificType("Geographical");
		relation1_2.setModality("Asserted");
		relation1_2.setTense("Unspecified");
		FSArray arguments2 = new FSArray(jcas, 2);
		ArgumentMention arg1_2 = new ArgumentMention(jcas);
		arg1_2.setRef(entity1_1);
		arg1_2.setRole("arg1");
		ArgumentMention arg2_2 = new ArgumentMention(jcas);
		arg2_2.setRef(entity2_2);
		arg2_2.setRole("arg2");
		arguments2.set(0, arg1_2);
		arguments2.set(1, arg2_2);
		relation1_2.setArguments(arguments2);
		// relation1_2.setArg1(entity1_1);
		// relation1_2.setArg2(entity2_2);
		relation1_2.addToIndexes();

		PART_WHOLE_Inverse relation2_1 = new PART_WHOLE_Inverse(jcas);
		relation2_1.setBegin(543);
		relation2_1.setEnd(579);
		relation2_1.setSpecificType("Geographical");
		relation2_1.setModality("Asserted");
		relation2_1.setTense("Unspecified");
		// relation2_1.setArg1(entity2_1); //old
		// relation2_1.setArg2(entity1_2); //old
		FSArray arguments3 = new FSArray(jcas, 2);
		ArgumentMention arg1_3 = new ArgumentMention(jcas);
		arg1_3.setRef(entity1_2);
		arg1_3.setRole("arg1");
		ArgumentMention arg2_3 = new ArgumentMention(jcas);
		arg2_3.setRef(entity2_1);
		arg2_3.setRole("arg2");
		arguments3.set(0, arg1_3);
		arguments3.set(1, arg2_3);
		relation2_1.setArguments(arguments3);
		// relation2_1.setArg1(entity1_2);
		// relation2_1.setArg2(entity2_1);
		relation2_1.addToIndexes();

		PART_WHOLE_Inverse relation2_2 = new PART_WHOLE_Inverse(jcas);
		relation2_2.setBegin(594);
		relation2_2.setEnd(616);
		relation2_2.setSpecificType("Geographical");
		relation2_2.setModality("Asserted");
		relation2_2.setTense("Unspecified");
		// relation2_2.setArg1(entity2_2); //old
		// relation2_2.setArg2(entity1_1); //old

		FSArray arguments4 = new FSArray(jcas, 2);
		ArgumentMention arg1_4 = new ArgumentMention(jcas);
		arg1_4.setRef(entity1_1);
		arg1_4.setRole("arg1");
		ArgumentMention arg2_4 = new ArgumentMention(jcas);
		arg2_4.setRef(entity2_2);
		arg2_4.setRole("arg2");
		arguments4.set(0, arg1_4);
		arguments4.set(1, arg2_4);
		relation2_2.setArguments(arguments4);

		// relation2_2.setArg1(entity1_1);
		// relation2_2.setArg2(entity2_2);
		relation2_2.addToIndexes();

	} // of buildJulesRelations

	/*----------------------------------------------------------------------------------------------*/
	private void buildJulesEntities(JCas jcas, Document document) {
		System.out.println("CALL buildJulesEntities()");

		entity1_1 = new LOC(jcas);
		entity1_1.setBegin(594);
		entity1_1.setEnd(616);
		entity1_1.setSpecificType("Region-General");
		entity1_1.setMentionLevel("PRO");
		Annotation head1_1 = new Annotation(jcas);
		head1_1.setBegin(600);
		head1_1.setEnd(604);
		entity1_1.setHead(head1_1);
		entity1_1.addToIndexes();

		entity1_2 = new LOC(jcas);
		entity1_2.setBegin(543);
		entity1_2.setEnd(579);
		entity1_2.setSpecificType("Region-General");
		entity1_2.setMentionLevel("PRO");
		Annotation head1_2 = new Annotation(jcas);
		head1_2.setBegin(549);
		head1_2.setEnd(553);
		entity1_2.setHead(head1_2);
		entity1_2.addToIndexes();

		entity2_1 = new GPE(jcas);
		entity2_1.setBegin(558);
		entity2_1.setEnd(579);
		entity2_1.setSpecificType("State-or-Province");
		entity2_1.setMentionLevel("NAM");
		Annotation head2_1 = new Annotation(jcas);
		head2_1.setBegin(562);
		head2_1.setEnd(579);
		entity2_1.setHead(head2_1);
		entity2_1.addToIndexes();

		entity2_2 = new GPE(jcas);
		entity2_2.setBegin(609);
		entity2_2.setEnd(616);
		entity2_2.setSpecificType("State-or-Province");
		entity2_2.setMentionLevel("NAM");
		Annotation head2_2 = new Annotation(jcas);
		head2_2.setBegin(609);
		head2_2.setEnd(616);
		entity2_2.setHead(head2_2);
		entity2_2.addToIndexes();

		entity2_3 = new GPE(jcas);
		entity2_3.setBegin(226);
		entity2_3.setEnd(255);
		entity2_3.setSpecificType("State-or-Province");
		entity2_3.setMentionLevel("NAM");
		Annotation head2_3 = new Annotation(jcas);
		head2_3.setBegin(239);
		head2_3.setEnd(255);
		entity2_3.setHead(head2_3);
		entity2_3.addToIndexes();

		entity2_4 = new GPE(jcas);
		entity2_4.setBegin(394);
		entity2_4.setEnd(401);
		entity2_4.setSpecificType("State-or-Province");
		entity2_4.setMentionLevel("NAM");
		Annotation head2_4 = new Annotation(jcas);
		head2_4.setBegin(394);
		head2_4.setEnd(401);
		entity2_4.setHead(head2_4);
		entity2_4.addToIndexes();

	} // of buildJulesEntities

	/*----------------------------------------------------------------------------------------------*/
	private void buildEvents(JCas jcas, Document document) {
		de.julielab.jcore.types.ace.Event event = new de.julielab.jcore.types.ace.Event(jcas);

		event.setGenericity("Specific");
		event.setModality("Asserted");
		event.setTense("Future");
		event.setAce_type("Transaction");
		event.setAce_subtype("Transfer-Money");
		event.setPolarity("Positive");
		event.setId("XIN_ENG_20030405.0080-EV2");
		buildEventArguments(jcas, event);
		buildEventMentions(jcas, event);
		event.addToIndexes();

		FSArray eventFSArray = new FSArray(jcas, 1);
		eventFSArray.set(0, event);
		eventFSArray.addToIndexes();
		document.setEvents(eventFSArray);
	} // of buildEvents

	/*----------------------------------------------------------------------------------------------*/
	private void buildEventMentions(JCas jcas, Event event) {
		de.julielab.jcore.types.ace.EventMention eventMention = new de.julielab.jcore.types.ace.EventMention(jcas);
		eventMention.setId("XIN_ENG_20030405.0080-EV2-1");
		eventMention.setBegin(625);
		eventMention.setEnd(854);
		eventMention.setEvent_ref(event);

		de.julielab.jcore.types.ace.LDC_Scope ldcScope = new de.julielab.jcore.types.ace.LDC_Scope(jcas);
		ldcScope.setBegin(625);
		ldcScope.setEnd(854);
		ldcScope.addToIndexes();
		eventMention.setLdc_scope(ldcScope);

		de.julielab.jcore.types.ace.Anchor anchor = new de.julielab.jcore.types.ace.Anchor(jcas);
		anchor.setBegin(663);
		anchor.setEnd(667);
		anchor.addToIndexes();
		eventMention.setAnchor(anchor);

		buildEventMentionArguments(jcas, eventMention);
		eventMention.addToIndexes();

		FSArray eventMentionFSArray = new FSArray(jcas, 1);
		eventMentionFSArray.set(0, eventMention);
		eventMentionFSArray.addToIndexes();
		event.setMentions(eventMentionFSArray);
	} // of buildEventMentions

	/*----------------------------------------------------------------------------------------------*/
	private void buildEventMentionArguments(JCas jcas, EventMention eventMention) {
		de.julielab.jcore.types.ace.EventMentionArgument eventMentionArgument1 = new de.julielab.jcore.types.ace.EventMentionArgument(
				jcas);
		eventMentionArgument1.setAce_role("Recipient");
		eventMentionArgument1.setRefid("XIN_ENG_20030624.0085-E1-4");
		eventMentionArgument1.setBegin(763);
		eventMentionArgument1.setEnd(767);
		eventMentionArgument1.addToIndexes();

		de.julielab.jcore.types.ace.EventMentionArgument eventMentionArgument2 = new de.julielab.jcore.types.ace.EventMentionArgument(
				jcas);
		eventMentionArgument2.setAce_role("Recipient");
		eventMentionArgument2.setRefid("XIN_ENG_20030624.0085-E11-2");
		eventMentionArgument2.setBegin(773);
		eventMentionArgument2.setEnd(778);
		eventMentionArgument2.addToIndexes();

		FSArray eventMentionArgumentFSArray = new FSArray(jcas, 2);
		eventMentionArgumentFSArray.set(0, eventMentionArgument1);
		eventMentionArgumentFSArray.set(1, eventMentionArgument2);
		eventMentionArgumentFSArray.addToIndexes();
		eventMention.setArguments(eventMentionArgumentFSArray);
	} // of buildEventMentionArguments

	/*----------------------------------------------------------------------------------------------*/
	private void buildEventArguments(JCas jcas, Event event) {
		de.julielab.jcore.types.ace.EventArgument eventArgument1 = new de.julielab.jcore.types.ace.EventArgument(jcas);
		eventArgument1.setAce_role("Recipient");
		eventArgument1.setRefid("XIN_ENG_20030405.0080-E1");
		eventArgument1.addToIndexes();

		de.julielab.jcore.types.ace.EventArgument eventArgument2 = new de.julielab.jcore.types.ace.EventArgument(jcas);
		eventArgument2.setAce_role("Recipient");
		eventArgument2.setRefid("XIN_ENG_20030405.0080-E11");
		eventArgument2.addToIndexes();

		FSArray eventArgumentFSArray = new FSArray(jcas, 2);
		eventArgumentFSArray.set(0, eventArgument1);
		eventArgumentFSArray.set(1, eventArgument2);
		eventArgumentFSArray.addToIndexes();
		event.setArguments(eventArgumentFSArray);
	} // of buildEventArguments

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelations(JCas jcas, Document document) {
		de.julielab.jcore.types.ace.Relation relation1 = new de.julielab.jcore.types.ace.Relation(jcas);
		relation1.setModality("Asserted");
		relation1.setTense("Unspecified");
		relation1.setAce_type("PART-WHOLE");
		relation1.setAce_subtype("Geographical");
		relation1.setId("XIN_ENG_20030624.0085-R7");
		buildRelationAgruments1(jcas, relation1);
		buildRelationMentions1(jcas, relation1);
		relation1.addToIndexes();

		de.julielab.jcore.types.ace.Relation relation2 = new de.julielab.jcore.types.ace.Relation(jcas);
		relation2.setModality("Asserted");
		relation2.setTense("Unspecified");
		relation2.setAce_type("PART-WHOLE");
		relation2.setAce_subtype("Geographical");
		relation2.setId("XIN_ENG_20030624.0085-R8");
		buildRelationArguments2(jcas, relation2);
		buildRelationMentions2(jcas, relation2);
		relation2.addToIndexes();

		FSArray relationFSArray = new FSArray(jcas, 2);
		relationFSArray.set(0, relation1);
		relationFSArray.set(1, relation2);
		relationFSArray.addToIndexes();
		document.setRelations(relationFSArray);
	} // of buildRelations

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentions2(JCas jcas, Relation relation2) {
		de.julielab.jcore.types.ace.RelationMention relationMention2_1 = new de.julielab.jcore.types.ace.RelationMention(
				jcas);
		relationMention2_1.setLexical_condition("Preposition");
		relationMention2_1.setId("XIN_ENG_20030624.0085-R8-1");
		relationMention2_1.setBegin(543);
		relationMention2_1.setEnd(579);
		relationMention2_1.setRelation_ref(relation2);
		buildRelationMentionArguments2_1(jcas, relationMention2_1);
		relationMention2_1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMention relationMention2_2 = new de.julielab.jcore.types.ace.RelationMention(
				jcas);
		relationMention2_2.setLexical_condition("Preposition");
		relationMention2_2.setId("XIN_ENG_20030624.0085-R8-2");
		relationMention2_2.setBegin(594);
		relationMention2_2.setEnd(616);
		relationMention2_2.setRelation_ref(relation2);
		buildRelationMentionArgument2_2(jcas, relationMention2_2);
		relationMention2_2.addToIndexes();

		FSArray relationMentionFSArray = new FSArray(jcas, 2);
		relationMentionFSArray.set(0, relationMention2_1);
		relationMentionFSArray.set(1, relationMention2_2);
		relationMentionFSArray.addToIndexes();
		relation2.setMentions(relationMentionFSArray);
	} // of buildRelationMentions2

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentionArgument2_2(JCas jcas, RelationMention relationMention2_2) {
		de.julielab.jcore.types.ace.RelationMentionArgument argument1 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument1.setAce_role("Arg-2");
		argument1.setRefid("XIN_ENG_20030624.0085-E1-4");
		argument1.setBegin(594);
		argument1.setEnd(616);
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMentionArgument argument2 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument2.setAce_role("Arg-1");
		argument2.setRefid("XIN_ENG_20030624.0085-E11-5");
		argument2.setBegin(609);
		argument2.setEnd(616);
		argument2.addToIndexes();

		FSArray relationMentionArgumentFSArray = new FSArray(jcas, 2);
		relationMentionArgumentFSArray.set(0, argument1);
		relationMentionArgumentFSArray.set(1, argument2);
		relationMentionArgumentFSArray.addToIndexes();
		relationMention2_2.setArguments(relationMentionArgumentFSArray);
	} // of buildRelationMentionArgument2_2

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentionArguments2_1(JCas jcas, RelationMention relationMention1) {
		de.julielab.jcore.types.ace.RelationMentionArgument argument1 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument1.setAce_role("Arg-2");
		argument1.setRefid("XIN_ENG_20030624.0085-E1-34");
		argument1.setBegin(543);
		argument1.setEnd(579);
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMentionArgument argument2 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument2.setAce_role("Arg-1");
		argument2.setRefid("XIN_ENG_20030624.0085-E11-2");
		argument2.setBegin(558);
		argument2.setEnd(579);
		argument2.addToIndexes();

		FSArray relationMentionArgumentFSArray = new FSArray(jcas, 2);
		relationMentionArgumentFSArray.set(0, argument1);
		relationMentionArgumentFSArray.set(1, argument2);
		relationMentionArgumentFSArray.addToIndexes();
		relationMention1.setArguments(relationMentionArgumentFSArray);
	} // of buildRelationMentionArguments2_1

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationArguments2(JCas jcas, Relation relation2) {
		de.julielab.jcore.types.ace.RelationArgument argument1 = new de.julielab.jcore.types.ace.RelationArgument(jcas);
		argument1.setAce_role("Arg-2");
		argument1.setRefid("XIN_ENG_20030624.0085-E1");
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationArgument argument2 = new de.julielab.jcore.types.ace.RelationArgument(jcas);
		argument2.setAce_role("Arg-1");
		argument2.setRefid("XIN_ENG_20030624.0085-E11");

		FSArray argumentFSArray = new FSArray(jcas, 2);
		argumentFSArray.set(0, argument1);
		argumentFSArray.set(1, argument2);
		argumentFSArray.addToIndexes();
		relation2.setArguments(argumentFSArray);
	} // of buildRelationArguments2

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentions1(JCas jcas, Relation relation) {
		de.julielab.jcore.types.ace.RelationMention relationMention1 = new de.julielab.jcore.types.ace.RelationMention(
				jcas);
		relationMention1.setLexical_condition("Preposition");
		relationMention1.setId("XIN_ENG_20030624.0085-R7-1");
		relationMention1.setBegin(543);
		relationMention1.setEnd(579);
		relationMention1.setRelation_ref(relation);
		buildRelationMentionArguments1_1(jcas, relationMention1);
		relationMention1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMention relationMention2 = new de.julielab.jcore.types.ace.RelationMention(
				jcas);
		relationMention2.setLexical_condition("Preposition");
		relationMention2.setId("XIN_ENG_20030624.0085-R7-2");
		relationMention2.setBegin(594);
		relationMention2.setEnd(616);
		relationMention2.setRelation_ref(relation);
		buildRelationMentionArguments1_2(jcas, relationMention2);
		relationMention2.addToIndexes();

		FSArray mentionFSArray = new FSArray(jcas, 2);
		mentionFSArray.set(0, relationMention1);
		mentionFSArray.set(1, relationMention2);
		mentionFSArray.addToIndexes();
		relation.setMentions(mentionFSArray);
	} // buildRelationMentions

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentionArguments1_2(JCas jcas, RelationMention relationMention2) {
		de.julielab.jcore.types.ace.RelationMentionArgument argument1 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument1.setAce_role("Arg-1");
		argument1.setRefid("XIN_ENG_20030624.0085-E1-4");
		argument1.setBegin(594);
		argument1.setEnd(616);
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMentionArgument argument2 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument2.setAce_role("Arg-2");
		argument2.setRefid("XIN_ENG_20030624.0085-E11-5");
		argument2.setBegin(609);
		argument2.setEnd(616);
		argument2.addToIndexes();

		FSArray argumentFSArray = new FSArray(jcas, 2);
		argumentFSArray.set(0, argument1);
		argumentFSArray.set(1, argument2);
		argumentFSArray.addToIndexes();
		relationMention2.setArguments(argumentFSArray);
	} // buildRelationMentionArguments2

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationMentionArguments1_1(JCas jcas, RelationMention relationMention1) {
		de.julielab.jcore.types.ace.RelationMentionArgument argument1 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument1.setAce_role("Arg-1");
		argument1.setRefid("XIN_ENG_20030624.0085-E1-34");
		argument1.setBegin(543);
		argument1.setEnd(579);
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationMentionArgument argument2 = new de.julielab.jcore.types.ace.RelationMentionArgument(
				jcas);
		argument2.setAce_role("Arg-2");
		argument2.setRefid("XIN_ENG_20030624.0085-E11-2");
		argument2.setBegin(558);
		argument2.setEnd(579);
		argument2.addToIndexes();

		FSArray argumentFSArray = new FSArray(jcas, 2);
		argumentFSArray.set(0, argument1);
		argumentFSArray.set(1, argument2);
		argumentFSArray.addToIndexes();
		relationMention1.setArguments(argumentFSArray);
	} // buildRelationMentionArguments1

	/*----------------------------------------------------------------------------------------------*/
	private void buildRelationAgruments1(JCas jcas, Relation relation) {
		de.julielab.jcore.types.ace.RelationArgument argument1 = new de.julielab.jcore.types.ace.RelationArgument(jcas);
		argument1.setAce_role("Arg-1");
		argument1.setRefid("XIN_ENG_20030624.0085-E1");
		argument1.addToIndexes();

		de.julielab.jcore.types.ace.RelationArgument argument2 = new de.julielab.jcore.types.ace.RelationArgument(jcas);
		argument2.setAce_role("Arg-2");
		argument2.setRefid("XIN_ENG_20030624.0085-E11");
		argument2.addToIndexes();

		FSArray argumentFSArray = new FSArray(jcas, 2);
		argumentFSArray.set(0, argument1);
		argumentFSArray.set(1, argument2);
		argumentFSArray.addToIndexes();
		relation.setArguments(argumentFSArray);
	} // buildRelationAgruments

	/*----------------------------------------------------------------------------------------------*/
	private void buildTimex2(JCas jcas, Document document) {
		de.julielab.jcore.types.ace.Timex2 timex2_1 = new de.julielab.jcore.types.ace.Timex2(jcas);
		timex2_1.setId("XIN_ENG_20030624.0085-T4");
		buildTimex2Mentions1(jcas, timex2_1);

		de.julielab.jcore.types.ace.Timex2 timex2_2 = new de.julielab.jcore.types.ace.Timex2(jcas);
		timex2_2.setId("XIN_ENG_20030624.0085-T8");
		buildTimex2Mentions2(jcas, timex2_2);

		FSArray timex2FSArray = new FSArray(jcas, 2);
		timex2FSArray.set(0, timex2_1);
		timex2FSArray.set(1, timex2_2);
		timex2FSArray.addToIndexes();
		document.setTimex2(timex2FSArray);
	} // buildTimex2

	/*----------------------------------------------------------------------------------------------*/
	private void buildTimex2Mentions2(JCas jcas, Timex2 timex2_2) {
		de.julielab.jcore.types.ace.Timex2Mention timex2Mention = new de.julielab.jcore.types.ace.Timex2Mention(jcas);
		timex2Mention.setId("XIN_ENG_20030624.0085-T8-1");
		timex2Mention.setBegin(1327);
		timex2Mention.setEnd(1330);
		timex2Mention.addToIndexes();

		FSArray timex2MentionFSArray = new FSArray(jcas, 1);
		timex2MentionFSArray.set(0, timex2Mention);
		timex2MentionFSArray.addToIndexes();
		timex2_2.setMentions(timex2MentionFSArray);
	} // buildTimex2Mentions2

	/*----------------------------------------------------------------------------------------------*/
	private void buildTimex2Mentions1(JCas jcas, Timex2 timex2_1) {
		de.julielab.jcore.types.ace.Timex2Mention timex2Mention = new de.julielab.jcore.types.ace.Timex2Mention(jcas);
		timex2Mention.setId("XIN_ENG_20030624.0085-T4-1");
		timex2Mention.setBegin(327);
		timex2Mention.setEnd(332);
		timex2Mention.addToIndexes();

		FSArray timex2MentionFSArray = new FSArray(jcas, 1);
		timex2MentionFSArray.set(0, timex2Mention);
		timex2MentionFSArray.addToIndexes();
		timex2_1.setMentions(timex2MentionFSArray);
	} // buildTimex2Mentions1

	/*----------------------------------------------------------------------------------------------*/
	private void buildValues(JCas jcas, Document document) {
		de.julielab.jcore.types.ace.Value value1 = new de.julielab.jcore.types.ace.Value(jcas);
		value1.setAce_type("Numeric");
		value1.setAce_subtype("Money");
		value1.setId("XIN_ENG_20030624.0085-V2");
		buildValueMentions1(jcas, value1);
		value1.addToIndexes();

		de.julielab.jcore.types.ace.Value value2 = new de.julielab.jcore.types.ace.Value(jcas);
		value2.setAce_type("Numeric");
		value2.setAce_subtype("Money");
		value2.setId("XIN_ENG_20030624.0085-V3");
		buildValueMentuions2(jcas, value2);
		value2.addToIndexes();

		FSArray valueFSArray = new FSArray(jcas, 2);
		valueFSArray.set(0, value1);
		valueFSArray.set(1, value2);
		valueFSArray.addToIndexes();
		document.setValues(valueFSArray);
	} // buildValues

	/*----------------------------------------------------------------------------------------------*/
	private void buildValueMentuions2(JCas jcas, Value value2) {
		de.julielab.jcore.types.ace.ValueMention valueMention = new de.julielab.jcore.types.ace.ValueMention(jcas);
		valueMention.setId("XIN_ENG_20030624.0085-V3-1");
		valueMention.setBegin(1079);
		valueMention.setEnd(1087);
		valueMention.addToIndexes();

		FSArray valueMentionFSArray = new FSArray(jcas, 1);
		valueMentionFSArray.set(0, valueMention);
		valueMentionFSArray.addToIndexes();
		value2.setMentions(valueMentionFSArray);
	} // buildValueMentuions2

	/*----------------------------------------------------------------------------------------------*/
	private void buildValueMentions1(JCas jcas, Value value1) {
		de.julielab.jcore.types.ace.ValueMention valueMention = new de.julielab.jcore.types.ace.ValueMention(jcas);
		valueMention.setId("XIN_ENG_20030624.0085-V2-1");
		valueMention.setBegin(826);
		valueMention.setEnd(854);
		valueMention.addToIndexes();

		FSArray valueMentionFSArray = new FSArray(jcas, 1);
		valueMentionFSArray.set(0, valueMention);
		valueMentionFSArray.addToIndexes();
		value1.setMentions(valueMentionFSArray);
	} // buildValueMentions1

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntities(JCas jcas, de.julielab.jcore.types.ace.Document document) {
		Entity entity1 = new Entity(jcas);
		entity1.setAce_class("USP");
		entity1.setAce_type("LOC");
		entity1.setId("XIN_ENG_20030624.0085-E1");
		entity1.setAce_subtype("Region-General");
		buildEntityMentions1(jcas, entity1);
		buildEntityAttributes1(jcas, entity1);
		entity1.addToIndexes();

		Entity entity2 = new Entity(jcas);
		entity2.setAce_class("SPC");
		entity2.setAce_type("GPE");
		entity2.setId("XIN_ENG_20030624.0085-E11");
		entity2.setAce_subtype("State-or-Province");
		buildEntityMentions2(jcas, entity2);
		buildEntityAttributes2(jcas, entity2);
		entity2.addToIndexes();

		FSArray entityFSArray = new FSArray(jcas, 2);
		entityFSArray.set(0, entity1);
		entityFSArray.set(1, entity2);
		entityFSArray.addToIndexes();
		document.setEntities(entityFSArray);
	} // of buildEntities

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntityAttributes1(JCas jcas, Entity entity1) {
		FSArray entityAttributeFSArray = new FSArray(jcas, 0);
		entityAttributeFSArray.addToIndexes();
		entity1.setEntity_attributes(entityAttributeFSArray);
	} // buildEntityAttributes1

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntityAttributes2(JCas jcas, Entity entity2) {
		de.julielab.jcore.types.ace.EntityAttribute entityAttribute = new de.julielab.jcore.types.ace.EntityAttribute(
				jcas);

		buildEntityAttributeNames(jcas, entityAttribute);
		entityAttribute.addToIndexes();

		FSArray entityAttributeFSArray = new FSArray(jcas, 1);
		entityAttributeFSArray.set(0, entityAttribute);
		entityAttributeFSArray.addToIndexes();
		entity2.setEntity_attributes(entityAttributeFSArray);
	} // ofbuildEntityAttributes2

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntityAttributeNames(JCas jcas, de.julielab.jcore.types.ace.EntityAttribute entityAttribute) {
		FSArray nameFSArray = new FSArray(jcas, 4);

		de.julielab.jcore.types.ace.Name entityAttributeName1 = new de.julielab.jcore.types.ace.Name(jcas);
		entityAttributeName1.setName("Shandong Province");
		entityAttributeName1.setBegin(239);
		entityAttributeName1.setEnd(255);
		entityAttributeName1.addToIndexes();

		de.julielab.jcore.types.ace.Name entityAttributeName2 = new de.julielab.jcore.types.ace.Name(jcas);
		entityAttributeName2.setName("Shandong");
		entityAttributeName2.setBegin(394);
		entityAttributeName2.setEnd(401);
		entityAttributeName2.addToIndexes();

		de.julielab.jcore.types.ace.Name entityAttributeName3 = new de.julielab.jcore.types.ace.Name(jcas);
		entityAttributeName3.setName("Shandong Peninsula");
		entityAttributeName3.setBegin(562);
		entityAttributeName3.setEnd(579);
		entityAttributeName3.addToIndexes();

		de.julielab.jcore.types.ace.Name entityAttributeName4 = new de.julielab.jcore.types.ace.Name(jcas);
		entityAttributeName4.setName("Shandong");
		entityAttributeName4.setBegin(609);
		entityAttributeName4.setEnd(616);
		entityAttributeName4.addToIndexes();

		nameFSArray.set(0, entityAttributeName1);
		nameFSArray.set(1, entityAttributeName2);
		nameFSArray.set(2, entityAttributeName3);
		nameFSArray.set(3, entityAttributeName4);
		nameFSArray.addToIndexes();
		entityAttribute.setNames(nameFSArray);
	} // buildEntityAttributeNames

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntityMentions1(JCas jcas, Entity entity) {
		de.julielab.jcore.types.ace.EntityMention entityMention1 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention1.setMention_ldctype("PTV");
		entityMention1.setMention_type("PRO");
		entityMention1.setId("XIN_ENG_20030624.0085-E1-4");
		entityMention1.setEntity_ref(entity);
		entityMention1.setLdcatr("");
		entityMention1.setAce_role("");
		entityMention1.setBegin(594);
		entityMention1.setEnd(616);
		de.julielab.jcore.types.ace.Head entityMentionHead1 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead1.setBegin(600);
		entityMentionHead1.setEnd(604);
		entityMentionHead1.addToIndexes();
		entityMention1.setHead(entityMentionHead1);
		entityMention1.addToIndexes();

		de.julielab.jcore.types.ace.EntityMention entityMention2 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention2.setMention_ldctype("PTV");
		entityMention2.setMention_type("PRO");
		entityMention2.setId("XIN_ENG_20030624.0085-E1-34");
		entityMention2.setEntity_ref(entity);
		entityMention2.setLdcatr("");
		entityMention2.setAce_role("");
		entityMention2.setBegin(543);
		entityMention2.setEnd(579);
		de.julielab.jcore.types.ace.Head entityMentionHead2 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead2.setBegin(549);
		entityMentionHead2.setEnd(553);
		entityMentionHead2.addToIndexes();
		entityMention2.setHead(entityMentionHead2);
		entityMention2.addToIndexes();

		FSArray entityMentionFSArray = new FSArray(jcas, 2);
		entityMentionFSArray.set(0, entityMention1);
		entityMentionFSArray.set(1, entityMention2);
		entityMentionFSArray.addToIndexes();
		entity.setEntity_mentions(entityMentionFSArray);
	} // of buildEntityMentions

	/*----------------------------------------------------------------------------------------------*/
	private void buildEntityMentions2(JCas jcas, Entity entity2) {
		de.julielab.jcore.types.ace.EntityMention entityMention1 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention1.setLdcatr("FALSE");
		entityMention1.setAce_role("LOC");
		entityMention1.setMention_ldctype("NAM");
		entityMention1.setMention_type("NAM");
		entityMention1.setId("XIN_ENG_20030624.0085-E11-2");
		entityMention1.setEntity_ref(entity2);
		entityMention1.setBegin(558);
		entityMention1.setEnd(579);
		de.julielab.jcore.types.ace.Head entityMentionHead1 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead1.setBegin(562);
		entityMentionHead1.setEnd(579);
		entityMentionHead1.addToIndexes();
		entityMention1.setHead(entityMentionHead1);
		entityMention1.addToIndexes();

		de.julielab.jcore.types.ace.EntityMention entityMention2 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention2.setLdcatr("FALSE");
		entityMention2.setAce_role("LOC");
		entityMention2.setMention_ldctype("NAM");
		entityMention2.setMention_type("NAM");
		entityMention2.setId("XIN_ENG_20030624.0085-E11-5");
		entityMention2.setEntity_ref(entity2);
		entityMention2.setBegin(609);
		entityMention2.setEnd(616);
		de.julielab.jcore.types.ace.Head entityMentionHead2 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead2.setBegin(609);
		entityMentionHead2.setEnd(616);
		entityMentionHead2.addToIndexes();
		entityMention2.setHead(entityMentionHead2);
		entityMention2.addToIndexes();

		de.julielab.jcore.types.ace.EntityMention entityMention3 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention3.setLdcatr("FALSE");
		entityMention3.setAce_role("LOC");
		entityMention3.setMention_ldctype("NAM");
		entityMention3.setMention_type("NAM");
		entityMention3.setId("XIN_ENG_20030624.0085-E11-22");
		entityMention3.setEntity_ref(entity2);
		entityMention3.setBegin(226);
		entityMention3.setEnd(255);
		de.julielab.jcore.types.ace.Head entityMentionHead3 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead3.setBegin(239);
		entityMentionHead3.setEnd(255);
		entityMentionHead3.addToIndexes();
		entityMention3.setHead(entityMentionHead3);
		entityMention3.addToIndexes();

		de.julielab.jcore.types.ace.EntityMention entityMention4 = new de.julielab.jcore.types.ace.EntityMention(jcas);
		entityMention4.setLdcatr("TRUE");
		entityMention4.setAce_role("GPE");
		entityMention4.setMention_ldctype("NAMPRE");
		entityMention4.setMention_type("NAM");
		entityMention4.setId("XIN_ENG_20030624.0085-E11-30");
		entityMention4.setEntity_ref(entity2);
		entityMention4.setBegin(394);
		entityMention4.setEnd(401);
		de.julielab.jcore.types.ace.Head entityMentionHead4 = new de.julielab.jcore.types.ace.Head(jcas);
		entityMentionHead4.setBegin(394);
		entityMentionHead4.setEnd(401);
		entityMentionHead4.addToIndexes();
		entityMention4.setHead(entityMentionHead4);
		entityMention4.addToIndexes();

		FSArray entityMentionFSArray = new FSArray(jcas, 4);
		entityMentionFSArray.set(0, entityMention1);
		entityMentionFSArray.set(1, entityMention2);
		entityMentionFSArray.set(2, entityMention3);
		entityMentionFSArray.set(3, entityMention4);
		entityMentionFSArray.addToIndexes();
		entity2.setEntity_mentions(entityMentionFSArray);
	} // buildEntityMentions2

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * Test if method getNextCas() has done its job
	 */
	public void testGetNextCas() {
		System.out.println("CALL testGetNextCas");
		checkDocumentText();

	} // of testGetNextCas

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Test if the CAS returned by the collectionReader has a non-empty document text returns
	 */
	public void checkDocumentText() {
		System.out.println("CALL checkDocumentText()");

		for (int i = 0; i < casArrayList.size(); i++) {
			String text = casArrayList.get(i).getDocumentText();
			assertTrue(((text == null) ? "null" : text), (text != null) && (!text.equals("")));
		} // of for
	} // of checkDocumentText

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Gets an Iterator over the the CAS for the specific type
	 * 
	 * @param cas
	 *            the CAS
	 * @param type
	 *            the type
	 * @return the iterator
	 */
	private Iterator getTypeIterator(CAS cas, int type) {

		Iterator iterator = null;
		try {
			iterator = cas.getJCas().getJFSIndexRepository().getAnnotationIndex(type).iterator();
		} catch (CASException e) {
			e.printStackTrace();
		}
		return iterator;
	} // getTypeIterator

	/*----------------------------------------------------------------------------------------------*/
	private void writeCasToXMI(CAS cas, int docs) throws CASException, IOException, SAXException {

		JFSIndexRepository indexes = cas.getJCas().getJFSIndexRepository();
		Iterator documentIter = indexes.getAnnotationIndex(Document.type).iterator();
		String filename = "";
		while (documentIter.hasNext()) {
			Document doc = (Document) documentIter.next();
			filename = doc.getDocid();
		}
		if (filename.length() == 0) {
			filename = "doc" + docs;
		}
		filename += ".xmi";

		System.out.println("Serialiazing CAS: " + filename);
		// now write CAS
		FileOutputStream fos = new FileOutputStream(OUT_FOLDER+filename);
		XmiCasSerializer ser = new XmiCasSerializer(cas.getTypeSystem());
		XMLSerializer xmlSer = new XMLSerializer(fos, false);
		ser.serialize(cas, xmlSer.getContentHandler());
	} // writeCasToXMI

	private static CollectionReader getCollectionReader(String readerDescriptor) {
		CollectionReaderDescription readerDescription;
		CollectionReader collectionReader = null;
		try {
			readerDescription = (CollectionReaderDescription) UIMAFramework.getXMLParser()
					.parseCollectionReaderDescription(new XMLInputSource(readerDescriptor));
			readerDescription.getMetaData().getConfigurationParameterSettings().setParameterValue(AceReader.PARAM_INPUTDIR, "src/test/resources/de/julielab/jcore/reader/ace/data");
			collectionReader = UIMAFramework.produceCollectionReader(readerDescription);
		} catch (InvalidXMLException | IOException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
		return collectionReader;
	}

} // of public class AceReaderTest
