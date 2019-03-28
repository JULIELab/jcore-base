/** 
 * AceReader.java
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
 * Converts the ACE corpus to the CAS representation
 **/

package de.julielab.jcore.reader.ace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.ace.EntityMention;
import de.julielab.jcore.types.ace.EventMention;
import de.julielab.jcore.types.ace.EventMentionArgument;
import de.julielab.jcore.types.ace.RelationMention;
import de.julielab.jcore.types.ace.RelationMentionArgument;
import de.julielab.jcore.types.ace.Timex2;
import de.julielab.jcore.types.ace.Timex2Mention;
import de.julielab.jcore.types.ace.Value;
import de.julielab.jcore.types.ace.ValueMention;
import de.julielab.jcore.utility.JCoReAnnotationTools;

// TODO add query if the data elements (e.g. ArrayLists) are null. Only if it is
// not so, read those elements to prevent exceptions

public class AceReader extends CollectionReader_ImplBase {
	public static final String GENERATE_JCORE_TYPES = "generateJcoreTypes";

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Integer value which is used to count the occurrences of wrong start-end-values
	 */
	private int startEndFailureCounter = 0;

	/**
	 * Integer values which are used to count the valid and the invalid documents
	 */
	private int validDocumentCounter = 0;

	private int invalidDocumentCounter = 0;


	/**
	 * Jules Types (see ACESemantics.xml) have to be generated if true
	 */
	@ConfigurationParameter(name=GENERATE_JCORE_TYPES, description = "Specifies if JULIE Lab Types (jcore-semantics-ace-types.xml) should be generated in addition to types from jcore-ace-types.xml. Defaults to true.", defaultValue = "true")
	private boolean generateJcoreTypes = true;

	/**
	 * mappings between ACE relations and Jules Types Relations in ACESemantics.xml
	 */
	Hashtable<String, String> mappings;

	/**
	 * IDs of ENtityMentions
	 */
	Hashtable<String, de.julielab.jcore.types.Annotation> ids = new Hashtable<String, de.julielab.jcore.types.Annotation>();

	/**
	 * String which will contain the document text
	 */
	private String documentText = "";

	/**
	 * List of files which will be processed by the reader
	 */
	private List<File> files;

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(AceReader.class);

	/**
	 * Name of configuration parameter that must be set to the path of a directory containing input files.
	 */
	public static final String PARAM_INPUTDIR = "inputDirectory";

	/**
	 * Current file number
	 */
	private int currentIndex;

	/**
	 * DocumentBuilder for creating Document objects (XML)
	 */
	private DocumentBuilder builder;

	/**
	 * XML element of content entity
	 */
	public static final String ELEMENT_ENTITY = "entity";

	/**
	 * XML element of content document
	 */
	public static final String ELEMENT_DOCUMENT = "document";

	/**
	 * XML element of content entity_mention
	 */
	public static final String ELEMENT_ENTITY_MENTION = "entity_mention";

	/**
	 * XML element of content source_file
	 */
	public static final String ELEMENT_SOURCE_FILE = "source_file";

	/**
	 * XML element of content extent
	 */
	public static final String ELEMENT_EXTENT = "extent";

	/**
	 * XML element of content head
	 */
	public static final String ELEMENT_HEAD = "head";

	/**
	 * XML element of content charseq
	 */
	public static final String ELEMENT_CHARSEQ = "charseq";

	/**
	 * XML element of content entity_attributes
	 */
	public static final String ELEMENT_ENTITY_ATTRIBUTES = "entity_attributes";

	/**
	 * XML element of content name
	 */
	public static final String ELEMENT_NAME = "name";

	/**
	 * XML element of content value
	 */
	public static final String ELEMENT_VALUE = "value";

	/**
	 * XML element of content timex2
	 */
	public static final String ELEMENT_TIMEX2 = "timex2";

	/**
	 * XML element of content value_mention
	 */
	public static final String ELEMENT_VALUE_MENTION = "value_mention";

	/**
	 * XML element of content anchor
	 */
	public static final String ELEMENT_ANCHOR = "anchor";

	/**
	 * XML element of content ldc_scope
	 */
	public static final String ELEMENT_LDC_SCOPE = "ldc_scope";

	/**
	 * XML element of content timex2_mention
	 */
	public static final String ELEMENT_TIMEX2_MENTION = "timex2_mention";

	/**
	 * XML element of content relation
	 */
	public static final String ELEMENT_RELATION = "relation";

	/**
	 * XML element of content event
	 */
	public static final String ELEMENT_EVENT = "event";

	/**
	 * XML element of content relation_argument
	 */
	public static final String ELEMENT_RELATION_ARGUMENT = "relation_argument";

	/**
	 * XML element of content event_argument
	 */
	public static final String ELEMENT_EVENT_ARGUMENT = "event_argument";

	/**
	 * XML element of content event_mention
	 */
	public static final String ELEMENT_EVENT_MENTION = "event_mention";

	/**
	 * XML element of content relation_mention
	 */
	public static final String ELEMENT_RELATION_MENTION = "relation_mention";

	/**
	 * XML element of content relation_mention_argument
	 */
	public static final String ELEMENT_RELATION_MENTION_ARGUMENT = "relation_mention_argument";

	/**
	 * XML element of content event_mention_argument
	 */
	public static final String ELEMENT_EVENT_MENTION_ARGUMENT = "event_mention_argument";

	/**
	 * XML element of content BODY
	 */
	public static final String ELEMENT_BODY = "BODY";

	/**
	 * XML element of content TEXT
	 */
	public static final String ELEMENT_TEXT = "TEXT";

	/**
	 * XML item of content START
	 */
	public static final String ITEM_START = "START";

	/**
	 * XML item of content END
	 */
	public static final String ITEM_END = "END";

	/**
	 * XML item of content ID
	 */
	public static final String ITEM_ID = "ID";

	/**
	 * XML item of content MOD
	 */
	public static final String ITEM_MOD = "MOD";

	/**
	 * XML item of content COMMENT
	 */
	public static final String ITEM_COMMENT = "COMMENT";

	/**
	 * XML item of content ANCHOR_VAL
	 */
	public static final String ITEM_ANCHOR_VAL = "ANCHOR_VAL";

	/**
	 * XML item of content VAL
	 */
	public static final String ITEM_VAL = "VAL";

	/**
	 * XML item of content SET
	 */
	public static final String ITEM_SET = "SET";

	/**
	 * XML item of content NON_SPECIFIC
	 */
	public static final String ITEM_NON_SPECIFIC = "NON_SPECIFIC";

	/**
	 * XML item of content ANCHOR_DIR
	 */
	public static final String ITEM_ANCHOR_DIR = "ANCHOR_DIR";

	/**
	 * XML item of content LEVEL
	 */
	public static final String ITEM_LEVEL = "LEVEL";

	/**
	 * XML item of content TYPE
	 */
	public static final String ITEM_TYPE = "TYPE";

	/**
	 * XML item of content SOURCE
	 */
	public static final String ITEM_SOURCE = "SOURCE";

	/**
	 * XML item of content AUTHOR
	 */
	public static final String ITEM_AUTHOR = "AUTHOR";

	/**
	 * XML item of content ENCODING
	 */
	public static final String ITEM_ENCODING = "ENCODING";

	/**
	 * XML item of content SUBTYPE
	 */
	public static final String ITEM_SUBTYPE = "SUBTYPE";

	/**
	 * XML item of content LDCTYPE
	 */
	public static final String ITEM_LDCTYPE = "LDCTYPE";

	/**
	 * XML item of content LDCATR
	 */
	public static final String ITEM_LDCATR = "LDCATR";

	/**
	 * XML item of content METONYMY_MENTION
	 */
	public static final String ITEM_METONYMY_MENTION = "METONYMY_MENTION";

	/**
	 * XML item of content TENSE
	 */
	public static final String ITEM_TENSE = "TENSE";

	/**
	 * XML item of content MODALITY
	 */
	public static final String ITEM_MODALITY = "MODALITY";

	/**
	 * XML item of content POLARITY
	 */
	public static final String ITEM_POLARITY = "POLARITY";

	/**
	 * XML item of content GENERICITY
	 */
	public static final String ITEM_GENERICITY = "GENERICITY";

	/**
	 * XML item of content CLASS
	 */
	public static final String ITEM_CLASS = "CLASS";

	/**
	 * XML item of content REFID
	 */
	public static final String ITEM_REFID = "REFID";

	/**
	 * XML item of content ROLE
	 */
	public static final String ITEM_ROLE = "ROLE";

	/**
	 * XML item of content URI
	 */
	public static final String ITEM_URI = "URI";

	/**
	 * XML item of content DOCID
	 */
	public static final String ITEM_DOCID = "DOCID";

	/**
	 * XML item of content LEXICALCONDITION
	 */
	public static final String ITEM_LEXICALCONDITION = "LEXICALCONDITION";
	@ConfigurationParameter(name=PARAM_INPUTDIR, description = "The input directory.")
    private File directory;

    /*----------------------------------------------------------------------------------------------*/
	/**
	 * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	 * 
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			builder = factory.newDocumentBuilder();
		} // of try
		catch (ParserConfigurationException e) {
			logger.error("getNext(CAS): " + e.getMessage() + "\nERROR! No builder available\n"
					+ e.getStackTrace());
		} // of catch

		if (files == null) {
			files = getFilesFromInputDirectory();
		} // of if

		JCas jcas;

		try {
			jcas = cas.getJCas();
		} // of try
		catch (CASException e) {
			throw new CollectionException(e);
		} // of catch

		File apfXmlFile = (File) files.get(currentIndex++);
		logger.info("getNext(CAS) - Reading file " + apfXmlFile.getName());
		FileInputStream apfXmlFis = new FileInputStream(apfXmlFile);
		FileInputStream sgmFis = null;
		String sgmFileName = getSgmFileName(apfXmlFile.getName());

		try {
			File sgmFile = (File) getSgmFileFromInputDirectory(sgmFileName);
			logger.info("getNext(CAS) - Reading source file " + sgmFile.getName());
			Document sgmDomDocument = builder.parse(sgmFile);
			setDocumentText(jcas, sgmDomDocument);
			Document apfXmlDomDocument = builder.parse(apfXmlFile);
			addSourceFileInformation(apfXmlDomDocument, jcas);

			if (generateJcoreTypes) {
				generateJulesTypes(jcas);
			} // of if
		} // of try
		catch (SAXException e1) {
			logger.error("getNext(CAS): " + e1.getMessage() + "\n" + e1.getStackTrace());
		} // of catch
		catch (IndexOutOfBoundsException e2) {
			logger.error("getNext(CAS): " + e2.getMessage() + "\n" + e2.getStackTrace());
		} // of catch
		catch (SecurityException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		} catch (NoSuchMethodException e) {

			e.printStackTrace();
		} catch (InstantiationException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}

		apfXmlFis.close();

		if (!(sgmFis == null))
			sgmFis.close();
		if (startEndFailureCounter > 0) {
			System.out.println("ATTENTION! There have been " + startEndFailureCounter
					+ " start-end-failures. Check the logger for more information!\n\n");
			invalidDocumentCounter++;

			// This is for debugging purposes only
			// writeFailureFile(apfXmlFile.getName());
		} // of if
		else {
			validDocumentCounter++;
		} // of else

		System.out.println("\n\nValid Documents: " + validDocumentCounter);
		System.out.println("Invalid Documents: " + invalidDocumentCounter);

		startEndFailureCounter = 0;
	} // of getNext

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void generateJulesTypes(JCas jcas) throws SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		logger.info("Generating Jules Entities");
		generateEntities(jcas);
		logger.info("Generating Jules Values");
		generateValues(jcas);
		logger.info("Generating Jules Timex2");
		generateTimex2(jcas);
		logger.info("Generating Jules Relations");
		generateRelation(jcas);
		logger.info("Generating Jules Events");
		generateEvents(jcas);
	} // of generateJcoreTypes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void generateEvents(JCas jcas) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
			NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

		String type;
		EventMentionArgument orignial_arg;
		de.julielab.jcore.types.ArgumentMention event_arg;

		AnnotationIndex eventmentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
				EventMention.type);
		FSIterator evIterator = eventmentionIndex.iterator();

		while (evIterator.hasNext()) {

			EventMention em = (EventMention) evIterator.next();
			type = "de.julielab.jcore.types.ace." + em.getEvent_ref().getAce_type();

			de.julielab.jcore.types.EventMention event = (de.julielab.jcore.types.EventMention) JCoReAnnotationTools
					.getAnnotationByClassName(jcas, type);

			event.setBegin(em.getBegin());
			event.setEnd(em.getEnd());
			event.setSpecificType(em.getEvent_ref().getAce_subtype());
			event.setGenericity(em.getEvent_ref().getGenericity());
			event.setModality(em.getEvent_ref().getModality());
			event.setTense(em.getEvent_ref().getTense());
			event.setPolarity(em.getEvent_ref().getPolarity());

			// Event Arguments
			FSArray eventmention_args = em.getArguments();
			FSArray event_args = new FSArray(jcas, eventmention_args.size());
			for (int i = 0; i < eventmention_args.size(); i++) {
				orignial_arg = (EventMentionArgument) eventmention_args.get(i);
				event_arg = new de.julielab.jcore.types.ArgumentMention(jcas);
				event_arg.setRef((de.julielab.jcore.types.Annotation) ids.get(orignial_arg.getRefid()));
				event_arg.setRole(orignial_arg.getAce_role());
				event_arg.setBegin(orignial_arg.getBegin());
				event_arg.setEnd(orignial_arg.getEnd());
				event_arg.addToIndexes();
				event_args.set(i, event_arg);
			} // of for

			event_args.addToIndexes();
			event.setArguments(event_args);
			event.setBegin(em.getAnchor().getBegin());
			event.setEnd(em.getAnchor().getEnd());
			event.setId(em.getId());
			event.addToIndexes();
		} // of while
	} // of generateEvents

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void generateRelation(JCas jcas) throws SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		String type, subtype;
		boolean inverse;
		AnnotationIndex relationmentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
				RelationMention.type);
		FSIterator relationmentionIterator = relationmentionIndex.iterator();
		RelationMentionArgument arg1 = null, arg2 = null;

		while (relationmentionIterator.hasNext()) {

			RelationMention rl = (RelationMention) relationmentionIterator.next();
			RelationMentionArgument node1 = rl.getArguments(0);
			if (node1.getAce_role().equals("Arg-1")) {
				arg1 = node1;
			} else
				arg2 = node1;

			RelationMentionArgument node2 = rl.getArguments(1);
			if (node2.getAce_role().equals("Arg-1")) {
				arg1 = node2;
			} else
				arg2 = node2;

			/**
			 * Is inverse or not? Is arg1 after arg2? If the same begin(is arg2 longer as arg1?)
			 */

			if (arg1.getBegin() < arg2.getBegin())
				inverse = false;
			else if (arg1.getBegin() == arg2.getBegin() && arg1.getEnd() > arg2.getEnd())
				inverse = false;
			else
				inverse = true;

			type = "de.julielab.jcore.types.ace." + mappings.get(rl.getRelation_ref().getAce_type());

			if (inverse)
				type = type + "_Inverse";
			de.julielab.jcore.types.RelationMention relation = (de.julielab.jcore.types.RelationMention) JCoReAnnotationTools
					.getAnnotationByClassName(jcas, type);

			subtype = rl.getRelation_ref().getAce_subtype();
			relation.setSpecificType(subtype);
			relation.setBegin(rl.getBegin());
			relation.setEnd(rl.getEnd());
			relation.setModality(rl.getRelation_ref().getModality());
			relation.setTense(rl.getRelation_ref().getTense());
			relation.setId(rl.getId());
			FSArray arguments = new FSArray(jcas, 2);
			ArgumentMention mentionarg1 = new ArgumentMention(jcas);
			ArgumentMention mentionarg2 = new ArgumentMention(jcas);

			if (inverse) {
				mentionarg1.setRef((de.julielab.jcore.types.Annotation) ids.get(arg2.getRefid()));
				mentionarg1.setRole("arg1");

				mentionarg2.setRef((de.julielab.jcore.types.Annotation) ids.get(arg1.getRefid()));
				mentionarg2.setRole("arg2");

			} else {
				mentionarg1.setRef((de.julielab.jcore.types.Annotation) ids.get(arg1.getRefid()));
				mentionarg1.setRole("arg1");

				mentionarg2.setRef((de.julielab.jcore.types.Annotation) ids.get(arg2.getRefid()));
				mentionarg2.setRole("arg2");

			} // of if/else
			mentionarg1.addToIndexes();
			mentionarg2.addToIndexes();
			arguments.set(0, mentionarg1);
			arguments.set(1, mentionarg2);
			arguments.addToIndexes();
			relation.setArguments(arguments);
			relation.addToIndexes();
		} // of while
	} // of generateRelation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void generateEntities(JCas jcas) throws SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {
		String type, subtype;
		AnnotationIndex entitymentionIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(
				EntityMention.type);
		FSIterator entitymentionIterator = entitymentionIndex.iterator();

		while (entitymentionIterator.hasNext()) {
			EntityMention em = (EntityMention) entitymentionIterator.next();

			type = "de.julielab.jcore.types.ace." + em.getEntity_ref().getAce_type();
			subtype = em.getEntity_ref().getAce_subtype();
			de.julielab.jcore.types.EntityMention entity = (de.julielab.jcore.types.EntityMention) JCoReAnnotationTools
					.getAnnotationByClassName(jcas, type);

			entity.setSpecificType(subtype);
			entity
					.setHead(new de.julielab.jcore.types.Annotation(jcas, em.getHead().getBegin(), em.getHead()
							.getEnd()));
			entity.setMentionLevel(em.getMention_type());
			entity.setBegin(em.getBegin());
			entity.setEnd(em.getEnd());
			entity.setId(em.getId());
			entity.addToIndexes();

			/**
			 * In Hastable mit entity_mention id, needed for Relations and Events as tehy refer to ID with refid
			 */
			ids.put(em.getId(), entity);

		} // of while
	} // of generateEntities

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void generateTimex2(JCas jcas) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
			NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

		AnnotationIndex timex2Index = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Timex2.type);
		FSIterator timex2Iterator = timex2Index.iterator();
		while (timex2Iterator.hasNext()) {
			Timex2 timex2 = (Timex2) timex2Iterator.next();
			FSArray timex2Mentions = timex2.getMentions();
			for (int i = 0; i < timex2Mentions.size(); i++) {

				Timex2Mention timex2mention = (Timex2Mention) timex2Mentions.get(i);

				de.julielab.jcore.types.Timex2Mention current = (de.julielab.jcore.types.Timex2Mention) JCoReAnnotationTools
						.getAnnotationByClassName(jcas, "de.julielab.jcore.types.Timex2Mention");

				// current.setSpecificType(subtype);
				current.setBegin(timex2mention.getBegin());
				current.setEnd(timex2mention.getEnd());
				current.setId(timex2mention.getId());
				current.addToIndexes();
				ids.put(timex2mention.getId(), current);

			} // of for
		} // of while
	} // of generateTimex2

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param jcas
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void generateValues(JCas jcas) throws SecurityException, IllegalArgumentException, ClassNotFoundException,
			NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

		AnnotationIndex valueIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Value.type);
		FSIterator valueIterator = valueIndex.iterator();

		while (valueIterator.hasNext()) {
			Value value = (Value) valueIterator.next();
			FSArray valuementions = value.getMentions();
			for (int i = 0; i < valuementions.size(); i++) {
				ValueMention valueMention = (ValueMention) valuementions.get(i);
				String type = "de.julielab.jcore.types.ace." + mappings.get(value.getAce_type());
				String subtype = value.getAce_subtype();

				de.julielab.jcore.types.ValueMention current = (de.julielab.jcore.types.ValueMention) JCoReAnnotationTools
						.getAnnotationByClassName(jcas, type);

				current.setSpecificType(subtype);
				current.setBegin(valueMention.getBegin());
				current.setEnd(valueMention.getEnd());
				current.setId(valueMention.getId());
				current.addToIndexes();
				ids.put(valueMention.getId(), current);
			} // of for
		} // of while
	} // of generateValues

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used for debugging purposes only
	 * 
	 * @param fileName
	 *            String which specifies the destination file for the failure information
	 */
	public void writeFailureFile(String fileName) {
		String failureFilePath = "src/test/resources/failureFile";
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(failureFilePath, true)));
			out.write(fileName);
			out.write("\n");
			out.close();
		} // of try
		catch (Exception e) {
			System.err.println("ERROR! ");
			e.printStackTrace();
		} // of catch
	} // of writeFailureFile

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the document text to the cas
	 * 
	 * @param jcas
	 *            JCas which will be updated by adding the document text
	 * @param sgmDomDocument
	 *            Document which will be parsed in order to retrieve the document text
	 */
	public void setDocumentText(JCas jcas, Document sgmDomDocument) {
		Node documentNode = sgmDomDocument.getDocumentElement();
		documentText = documentNode.getTextContent();
		documentText = replaceWhiteChar(documentText);
		jcas.setDocumentText(documentText);
	} // of setDocumentText

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * 
	 * @param documentText
	 * @return
	 */
	private String replaceWhiteChar(String inputDocumentText) {
		String documentText = inputDocumentText;
		documentText = documentText.replaceAll("\n", " ");
		documentText = documentText.replaceAll("\t", " ");
		documentText = documentText.replaceAll("\r", " ");
		documentText = documentText.replaceAll("\f", " ");
		return documentText;
	} // of replaceWhiteChar

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method turns a file name with the apf.xml extension into a file name with the sgm extension
	 * 
	 * @param apfXmlFileName
	 *            String which specifies a file with the apf.xml extension
	 * @return sgmFileName String which contains the file name with the smg extension
	 */
	public String getSgmFileName(String apfXmlFileName) {

		// Cut the last seven characters ("apf.xml") from the apfXmlFileName.
		// The result is the plain file name without the type extension.
		String sgmFileName = apfXmlFileName.substring(0, apfXmlFileName.length() - 7);
		sgmFileName = sgmFileName + "sgm";
		return sgmFileName;
	} // getSgmFileName

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to put the timex2 information from the XML file into proper objects of the AceTypeSystem
	 * 
	 * @param timex2Node
	 *            Node which will be parsed in order to retrieve timex2 information
	 * @param aceTimex2
	 *            ace.Timex2 which will be updated by adding its attribute values
	 * @param jcas
	 *            Jcas which will be filled with retrieved information
	 */
	public void addTimex2Information(Node timex2Node, de.julielab.jcore.types.ace.Timex2 aceTimex2, JCas jcas) {
		setTimex2Attributes(aceTimex2, timex2Node);
		FSArray timex2MentionFSArray = getTimex2MentionFSArray(timex2Node, jcas);
		aceTimex2.setMentions(timex2MentionFSArray);
		aceTimex2.addToIndexes();
	} // of addTimex2Information

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to put the relation information from the XML file into proper objects of the AceTypeSystem
	 * 
	 * @param relationNode
	 *            Node which will be parsed in order to retrieve relation information
	 * @param aceRelation
	 *            ace.Relation which will be updated by adding its attribute values
	 * @param jcas
	 *            Jcas which will be filled with retrieved information
	 */
	public void addRelationInformation(Node relationNode, de.julielab.jcore.types.ace.Relation aceRelation, JCas jcas) {
		setRelationAttributes(aceRelation, relationNode);
		FSArray relationArgumentFSArray = getRelationArgumentFSArray(relationNode, jcas);
		FSArray relationMentionFSArray = getRelationMentionFSArray(relationNode, aceRelation, jcas);
		aceRelation.setArguments(relationArgumentFSArray);
		aceRelation.setMentions(relationMentionFSArray);
		aceRelation.addToIndexes();
	} // of addRelationInformation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to put the event information from the XML file into proper objects of the AceTypeSystem
	 * 
	 * @param eventNode
	 *            Node which will be parsed in order to retrieve relation information
	 * @param aceEvent
	 *            ace.Event which will be updated by adding its attribute values
	 * @param jcas
	 *            Jcas which will be filled with retrieved information
	 */
	public void addEventInformation(Node eventNode, de.julielab.jcore.types.ace.Event aceEvent, JCas jcas) {
		setEventAttributes(aceEvent, eventNode);
		FSArray eventArgumentFSArray = getEventArgumentFSArray(eventNode, jcas);
		FSArray eventMentionFSArray = getEventMentionFSArray(eventNode, aceEvent, jcas);
		aceEvent.setArguments(eventArgumentFSArray);
		aceEvent.setMentions(eventMentionFSArray);
	} // of addEventInformation

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to retrieve a certain attribute value from an XML node
	 * 
	 * @param node
	 *            Node which specified attribute will be parsed in order to retrieve its value
	 * @param attributeName
	 *            String which specifies the name of the attribute which value is to be retrieved
	 * 
	 * @return attributeValue String which contains the retrieved attribute value
	 */
	public String retrieveAttribute(Node node, String attributeName) {
		String attributeValue = "";

		try {
			attributeValue = node.getAttributes().getNamedItem(attributeName).getNodeValue();
		} // of try
		catch (Exception e) {
			logger.info("retrieveAttribute(Node, String): " + e.getMessage() + "\nATTENTION! Node "
					+ node.getNodeName() + " has no " + attributeName + " attribute!");
		} // of catch

		return attributeValue;
	} // of setAceID

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to put the value information from the XML file into proper objects of the AceTypeSystem
	 * 
	 * @param valueNode
	 *            Node which will be parsed in order to retrieve value information
	 * @param aceValue
	 *            ace.Value which will be updated by adding its attribute values
	 * @param jcas
	 *            Jcas which will be filled with retrieved information
	 */
	public void addValueInformation(Node valueNode, de.julielab.jcore.types.ace.Value aceValue, JCas jcas) {
		setValueAttributes(aceValue, valueNode);
		FSArray valueMentionFSArray = getValueMentionFSArray(valueNode, jcas);
		aceValue.setMentions(valueMentionFSArray);
		aceValue.addToIndexes();
	} // of addValueInformation

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to put the entities from the XML file into proper objects of the AceTypeSystem
	 * 
	 * @param entityNode
	 *            Node which will be parsed in order to retrieve valuable information
	 * @param aceEntity
	 *            ace.Entity which will be updated by adding its attribute values
	 * @param jcas
	 *            Jcas which will be filled with retrieved information
	 */
	public void addEntityInformation(Node entityNode, de.julielab.jcore.types.ace.Entity aceEntity, JCas jcas) {
		setEntityAttributes(aceEntity, entityNode);
		FSArray entityMentionFSArray = getEntityMentionFSArray(entityNode, aceEntity, jcas);
		FSArray entityAttributesFSArray = getEntityAttributesFSArray(entityNode, jcas);
		aceEntity.setEntity_mentions(entityMentionFSArray);
		aceEntity.setEntity_attributes(entityAttributesFSArray);
		aceEntity.addToIndexes();
	} // of addEntityInformation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build an FSArray which will contain entity attributes
	 * 
	 * @param entityNode
	 *            Node which will be parsed in order to retrieve the entity attributes information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getEntityAttributesFSArray(Node entityNode, JCas jcas) {
		NodeList children = entityNode.getChildNodes();
		ArrayList<Node> attributesNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_ENTITY_ATTRIBUTES);
		FSArray entityAttributesFSArray = new FSArray(jcas, attributesNodeArrayList.size());

		for (int i = 0; i < attributesNodeArrayList.size(); i++) {
			Node entityAttributesNode = attributesNodeArrayList.get(i);
			de.julielab.jcore.types.ace.EntityAttribute entityAttribute = new de.julielab.jcore.types.ace.EntityAttribute(
					jcas);
			setEntityAttributesInformation(entityAttribute, entityAttributesNode, jcas);
			entityAttribute.addToIndexes();
			entityAttributesFSArray.set(i, entityAttribute);
		} // of for

		entityAttributesFSArray.addToIndexes();
		return entityAttributesFSArray;
	} // of getEntityAttributesFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes information of a particular instance of entityAttributes
	 * 
	 * @param entityAttributes
	 *            ace.EntityAttributes which will be updated by adding of certain entity attributes information
	 * @param entityAttributesNode
	 *            Node which will be parsed in order to retrieve entity attributes information
	 * @param jcas
	 *            JCAs which will be filled with retrieved information
	 */
	public void setEntityAttributesInformation(de.julielab.jcore.types.ace.EntityAttribute entityAttributes,
			Node entityAttributesNode, JCas jcas) {
		ArrayList<Node> nameNodeArrayList = new ArrayList<Node>();
		NodeList children = entityAttributesNode.getChildNodes();
		nameNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_NAME);
		FSArray nameFSArray = new FSArray(jcas, nameNodeArrayList.size());

		for (int i = 0; i < nameNodeArrayList.size(); i++) {
			Node nameNode = nameNodeArrayList.get(i);
			de.julielab.jcore.types.ace.Name julesName = new de.julielab.jcore.types.ace.Name(jcas);
			setJulesNameName(julesName, nameNode);
			setJulesNameStartEnd(julesName, nameNode);
			julesName.addToIndexes();
			nameFSArray.set(i, julesName);
		} // of for

		nameFSArray.addToIndexes();
		entityAttributes.setNames(nameFSArray);
	} // of setEntityAttributesInformation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param julesName
	 *            jules.types.Name which will be updated by adding the start and end information
	 * @param nameNode
	 *            Node which contains the name start and end
	 * 
	 */
	public void setJulesNameStartEnd(de.julielab.jcore.types.ace.Name julesName, Node nameNode) {
		NodeList children = nameNode.getChildNodes();
		ArrayList<Node> charseqNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = charseqNodeArrayList.get(0);
		int nameStart = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_START).getNodeValue());
		int nameEnd = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_END).getNodeValue());
		validateStartEnd(nameStart, nameEnd, charseqNode);
		julesName.setBegin(nameStart);
		julesName.setEnd(nameEnd + 1);
	} // of setJulesNameStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param valueMention
	 *            ValueMention which will be updated by adding the start and end information
	 * @param valueMentionNode
	 *            Node which contains start and end of the value
	 */
	public void setValueMentionStartEnd(de.julielab.jcore.types.ace.ValueMention valueMention, Node valueMentionNode) {
		int valueMentionStart = retrieveStartEndValue(valueMentionNode, ITEM_START);
		int valueMentionEnd = retrieveStartEndValue(valueMentionNode, ITEM_END);
		validateStartEnd(valueMentionStart, valueMentionEnd, getMentionCharseqNode(valueMentionNode));
		valueMention.setBegin(valueMentionStart);
		valueMention.setEnd(valueMentionEnd + 1);
	} // of setValueMentionStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param relationMention
	 *            ace.RelationMention which will be updated by adding the start and end information
	 * @param relationMentionNode
	 *            Node which contains start and end of the value
	 */
	public void setRelationMentionStartEnd(de.julielab.jcore.types.ace.RelationMention relationMention,
			Node relationMentionNode) {
		int relationMentionStart = retrieveStartEndValue(relationMentionNode, ITEM_START);
		int relationMentionEnd = retrieveStartEndValue(relationMentionNode, ITEM_END);
		validateStartEnd(relationMentionStart, relationMentionEnd, getMentionCharseqNode(relationMentionNode));
		relationMention.setBegin(relationMentionStart);
		relationMention.setEnd(relationMentionEnd + 1);
	} // of setRelationMentionStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param relationMentionArgument
	 *            ace.RelationMentionArgument which will be updated by adding the start and end information
	 * @param relationMentionArgumentNode
	 *            Node which contains start and end of the value
	 */
	public void setRelationMentionArgumentStartEnd(
			de.julielab.jcore.types.ace.RelationMentionArgument relationMentionArgument,
			Node relationMentionArgumentNode) {
		int relationMentionArgumentStart = retrieveStartEndValue(relationMentionArgumentNode, ITEM_START);
		int relationMentionArgumentEnd = retrieveStartEndValue(relationMentionArgumentNode, ITEM_END);
		validateStartEnd(relationMentionArgumentStart, relationMentionArgumentEnd,
				getMentionCharseqNode(relationMentionArgumentNode));
		relationMentionArgument.setBegin(relationMentionArgumentStart);
		relationMentionArgument.setEnd(relationMentionArgumentEnd + 1);
	} // setRelationMentionStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param eventMention
	 *            ace.EventMention which will be updated by adding the start and end information
	 * @param eventMentionNode
	 *            Node which contains start and end of the value
	 */
	public void setEventMentiuonStartEnd(de.julielab.jcore.types.ace.EventMention eventMention, Node eventMentionNode) {
		int eventMentionStart = retrieveStartEndValue(eventMentionNode, ITEM_START);
		int eventMentionEnd = retrieveStartEndValue(eventMentionNode, ITEM_END);
		validateStartEnd(eventMentionStart, eventMentionEnd, getMentionCharseqNode(eventMentionNode));
		eventMention.setBegin(eventMentionStart);
		eventMention.setEnd(eventMentionEnd + 1);
	} // setEventMentiuonStartEnd

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param eventMentionArgument
	 *            ace.EventMentionArgument which will be updated by adding the start and end information
	 * @param eventMentionArgumentNode
	 *            Node which contains start and end of the value
	 */
	public void setEventMentionArgumentStartEnd(de.julielab.jcore.types.ace.EventMentionArgument eventMentionArgument,
			Node eventMentionArgumentNode) {
		int eventMentionArgumentStart = retrieveStartEndValue(eventMentionArgumentNode, ITEM_START);
		int eventMentionArgumentEnd = retrieveStartEndValue(eventMentionArgumentNode, ITEM_END);
		validateStartEnd(eventMentionArgumentStart, eventMentionArgumentEnd,
				getMentionCharseqNode(eventMentionArgumentNode));
		eventMentionArgument.setBegin(eventMentionArgumentStart);
		eventMentionArgument.setEnd(eventMentionArgumentEnd + 1);
	} // of setEventMentionArgumentStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the value of either the start attribute or the end attribute of an XML node.
	 * 
	 * @param node
	 *            Node which will be parsed in order to retrieve the proper values
	 * @param itemName
	 *            String which specifies, which item of an XML node has to be retrieved
	 */
	public int retrieveStartEndValue(Node node, String itemName) {
		int value = 0;
		NodeList children = node.getChildNodes();
		ArrayList<Node> extentNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_EXTENT);
		Node extentNode = extentNodeArrayList.get(0);
		children = extentNode.getChildNodes();
		ArrayList<Node> charseqNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = charseqNodeArrayList.get(0);

		try {
			value = Integer.parseInt(charseqNode.getAttributes().getNamedItem(itemName).getNodeValue());
		} // of try
		catch (Exception e) {
			logger.info("retrieveStartEndValue(Node, String): " + e.getMessage() + "\nATTENTION! The node "
					+ node.getNodeName() + "has insufficient start/end information!");
		} // of catch

		return value;
	} // of retrieveStartEndValue

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param timex2Mention
	 *            Timex2Mention which will be updated by adding the start and end information
	 * @param timex2MentionNode
	 *            Node which contains start and end of the value
	 */
	public void setTimex2MentionStartEnd(de.julielab.jcore.types.ace.Timex2Mention timex2Mention, Node timex2MentionNode) {
		int timex2MentionStart = retrieveStartEndValue(timex2MentionNode, ITEM_START);
		int timex2MentionEnd = retrieveStartEndValue(timex2MentionNode, ITEM_END);
		validateStartEnd(timex2MentionStart, timex2MentionEnd, getMentionCharseqNode(timex2MentionNode));
		timex2Mention.setBegin(timex2MentionStart);
		timex2Mention.setEnd(timex2MentionEnd + 1);
	} // of setTimex2MentionStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param eventMentionAnchor
	 *            ace.Anchor which will be updated by adding the start and end information
	 * @param anchorNode
	 *            Node which contains start and end of the value
	 */
	public void setEventMentionAnchorStartEnd(de.julielab.jcore.types.ace.Anchor eventMentionAnchor, Node anchorNode) {
		NodeList children = anchorNode.getChildNodes();
		ArrayList<Node> charseqNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = charseqNodeArrayList.get(0);
		int anchorStart = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_START).getNodeValue());
		int anchorEnd = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_END).getNodeValue());
		validateStartEnd(anchorStart, anchorEnd, charseqNode);
		eventMentionAnchor.setBegin(anchorStart);
		eventMentionAnchor.setEnd(anchorEnd + 1);
	} // setEventMentionAnchorStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the end of a value from an XML node and to set it to proper
	 * attributes of an ace type
	 * 
	 * @param eventMentionLDCScope
	 *            ace.LDC_Scope which will be updated by adding the start and end information
	 * @param LDCScopeNode
	 *            Node which contains start and end of the value
	 */
	public void setEventMentionLDCScopeStartEnd(de.julielab.jcore.types.ace.LDC_Scope eventMentionLDCScope,
			Node LDCScopeNode) {
		NodeList children = LDCScopeNode.getChildNodes();
		ArrayList<Node> charseqNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = charseqNodeArrayList.get(0);
		int LDCScopeStart = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_START).getNodeValue());
		int LDCScopeEnd = Integer.parseInt(charseqNode.getAttributes().getNamedItem(ITEM_END).getNodeValue());
		validateStartEnd(LDCScopeStart, LDCScopeEnd, charseqNode);
		eventMentionLDCScope.setBegin(LDCScopeStart);
		eventMentionLDCScope.setEnd(LDCScopeEnd + 1);
	} // of setEventMentionLDCScopeStartEnd

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to safely set the name attribute for the jules.types.Name. In the unlikely case that some
	 * name node does not have a name, a try-catch-instruction will be used to provide proper exception handling.
	 * 
	 * @param julesName
	 *            jules.types.Name which attribute "name" will be updated
	 * @param nameNode
	 *            Node which attribute "NAME" will be parsed in order to retrieve its value
	 */
	public void setJulesNameName(de.julielab.jcore.types.ace.Name julesName, Node nameNode) {
		String name = "";

		try {
			name = nameNode.getAttributes().getNamedItem("NAME").getNodeValue();
			julesName.setName(name);
		} // of try
		catch (Exception e) {
			logger.info("setJulesNameName(ace.Name, Node): " + e.getMessage()
					+ "\nATTENTION! Entity attribute has no name.");
		} // of catch
	} // of setJulesName

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of entityMentions of a specific XML node
	 * 
	 * @param entityNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getEntityMentionFSArray(Node entityNode, de.julielab.jcore.types.ace.Entity aceEntity, JCas jcas) {
		NodeList children = entityNode.getChildNodes();
		ArrayList<Node> entityMentionNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_ENTITY_MENTION);
		FSArray entityMentionFSArray = new FSArray(jcas, entityMentionNodeArrayList.size());

		for (int i = 0; i < entityMentionNodeArrayList.size(); i++) {
			Node entityMentionNode = entityMentionNodeArrayList.get(i);
			de.julielab.jcore.types.ace.EntityMention entityMention = new de.julielab.jcore.types.ace.EntityMention(
					jcas);
			setEntityMentionAttributes(entityMention, entityMentionNode);
			setEntityMentionStartEnd(entityMention, entityMentionNode);
			entityMention.setEntity_ref(aceEntity);

			setEntityMentionHead(entityMention, entityMentionNode, jcas);
			entityMention.addToIndexes();
			entityMentionFSArray.set(i, entityMention);
		} // of for

		entityMentionFSArray.addToIndexes();
		return entityMentionFSArray;
	} // of getEntityMentionFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of timex2Mentions of a specific XML node
	 * 
	 * @param timex2Node
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getTimex2MentionFSArray(Node timex2Node, JCas jcas) {
		NodeList children = timex2Node.getChildNodes();
		ArrayList<Node> timex2MentionNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_TIMEX2_MENTION);
		FSArray timex2MentionFSArray = new FSArray(jcas, timex2MentionNodeArrayList.size());

		for (int i = 0; i < timex2MentionNodeArrayList.size(); i++) {
			Node timex2MentionNode = timex2MentionNodeArrayList.get(i);
			de.julielab.jcore.types.ace.Timex2Mention timex2Mention = new de.julielab.jcore.types.ace.Timex2Mention(
					jcas);
			setTimex2MentionAttributes(timex2Mention, timex2MentionNode);
			setTimex2MentionStartEnd(timex2Mention, timex2MentionNode);
			timex2Mention.addToIndexes();
			timex2MentionFSArray.set(i, timex2Mention);
		} // of for

		timex2MentionFSArray.addToIndexes();
		return timex2MentionFSArray;
	} // of getTimex2MentionFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of RelationArguments of a specific XML node
	 * 
	 * @param relationNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getRelationArgumentFSArray(Node relationNode, JCas jcas) {
		NodeList children = relationNode.getChildNodes();
		ArrayList<Node> relationArgumentNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_RELATION_ARGUMENT);
		FSArray relationArgumentFSArray = new FSArray(jcas, relationArgumentNodeArrayList.size());

		for (int i = 0; i < relationArgumentNodeArrayList.size(); i++) {
			Node relationArgumentNode = relationArgumentNodeArrayList.get(i);
			de.julielab.jcore.types.ace.RelationArgument relationArgument = new de.julielab.jcore.types.ace.RelationArgument(
					jcas);
			setRelationArgumentAttributes(relationArgument, relationArgumentNode);
			relationArgument.addToIndexes();
			relationArgumentFSArray.set(i, relationArgument);
		} // of for

		relationArgumentFSArray.addToIndexes();
		return relationArgumentFSArray;
	} // of getRelationArgumentFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of eventArguments of a specific XML node
	 * 
	 * @param eventNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getEventArgumentFSArray(Node eventNode, JCas jcas) {
		NodeList children = eventNode.getChildNodes();
		ArrayList<Node> eventArgumentNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_EVENT_ARGUMENT);
		FSArray eventArgumentFSArray = new FSArray(jcas, eventArgumentNodeArrayList.size());

		for (int i = 0; i < eventArgumentNodeArrayList.size(); i++) {
			Node eventArgumentNode = eventArgumentNodeArrayList.get(i);
			de.julielab.jcore.types.ace.EventArgument eventArgument = new de.julielab.jcore.types.ace.EventArgument(
					jcas);
			setEventArgumentAttributes(eventArgument, eventArgumentNode);
			eventArgument.addToIndexes();
			eventArgumentFSArray.set(i, eventArgument);
		} // of for

		eventArgumentFSArray.addToIndexes();
		return eventArgumentFSArray;
	} // of getEventArgumentFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of eventMentions of a specific XML node
	 * 
	 * @param eventNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getEventMentionFSArray(Node eventNode, de.julielab.jcore.types.ace.Event aceEvent, JCas jcas) {
		NodeList children = eventNode.getChildNodes();
		ArrayList<Node> eventMentionNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_EVENT_MENTION);
		FSArray eventMentionFSArray = new FSArray(jcas, eventMentionNodeArrayList.size());

		for (int i = 0; i < eventMentionNodeArrayList.size(); i++) {
			Node eventMentionNode = eventMentionNodeArrayList.get(i);
			de.julielab.jcore.types.ace.EventMention eventMention = new de.julielab.jcore.types.ace.EventMention(jcas);
			setEventMentionAttributes(eventMention, eventMentionNode);
			setEventMentiuonStartEnd(eventMention, eventMentionNode);
			setEventMentionLDCScope(eventMention, eventMentionNode, jcas);
			eventMention.setEvent_ref(aceEvent);
			setEventMentionAnchor(eventMention, eventMentionNode, jcas);
			FSArray eventMentionArgumentFSArray = getEventMentionArgumentFSArray(eventMentionNode, jcas);
			eventMention.setArguments(eventMentionArgumentFSArray);
			eventMention.addToIndexes();
			eventMentionFSArray.set(i, eventMention);
		} // of for

		eventMentionFSArray.addToIndexes();
		return eventMentionFSArray;
	} // of getEventMentionFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of RelationMentions of a specific XML node
	 * 
	 * @param relationNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 * 
	 * @return relationMentionFSArray FSarray which contains the relationMentions
	 */
	public FSArray getRelationMentionFSArray(Node relationNode, de.julielab.jcore.types.ace.Relation aceRelation,
			JCas jcas) {
		NodeList children = relationNode.getChildNodes();
		ArrayList<Node> relationMentionNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_RELATION_MENTION);
		FSArray relationMentionFSArray = new FSArray(jcas, relationMentionNodeArrayList.size());

		for (int i = 0; i < relationMentionNodeArrayList.size(); i++) {
			Node relationMentionNode = relationMentionNodeArrayList.get(i);
			de.julielab.jcore.types.ace.RelationMention relationMention = new de.julielab.jcore.types.ace.RelationMention(
					jcas);
			setRelationMentionAttributes(relationMention, relationMentionNode);
			setRelationMentionStartEnd(relationMention, relationMentionNode);
			relationMention.setRelation_ref(aceRelation);
			FSArray relationMentionArgumentFSArray = getRelationMentionArgumentFSArray(relationMentionNode, jcas);
			relationMention.setArguments(relationMentionArgumentFSArray);
			relationMention.addToIndexes();
			relationMentionFSArray.set(i, relationMention);
		} // of for

		relationMentionFSArray.addToIndexes();
		return relationMentionFSArray;
	} // of getRelationMentionFSArray

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to get an FSArray of RelationMentionArguments of a specific XML node
	 * 
	 * @param relationMentionNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 * 
	 * @param relationMentionArgumentFSArray
	 *            FSArray which contains relationMentionArguments
	 */
	public FSArray getRelationMentionArgumentFSArray(Node relationMentionNode, JCas jcas) {
		NodeList children = relationMentionNode.getChildNodes();
		ArrayList<Node> relationMentionArgumentNodeArrayList = getSpecificNodeArrayList(children,
				ELEMENT_RELATION_MENTION_ARGUMENT);
		FSArray relationMentionArgumentFSArray = new FSArray(jcas, relationMentionArgumentNodeArrayList.size());

		for (int i = 0; i < relationMentionArgumentNodeArrayList.size(); i++) {
			Node relationMentionArgumentNode = relationMentionArgumentNodeArrayList.get(i);
			de.julielab.jcore.types.ace.RelationMentionArgument relationMentionArgument = new de.julielab.jcore.types.ace.RelationMentionArgument(
					jcas);
			setRelationMentionArgumentAttributes(relationMentionArgument, relationMentionArgumentNode);
			setRelationMentionArgumentStartEnd(relationMentionArgument, relationMentionArgumentNode);
			relationMentionArgument.addToIndexes();
			relationMentionArgumentFSArray.set(i, relationMentionArgument);
		} // of for

		relationMentionArgumentFSArray.addToIndexes();
		return relationMentionArgumentFSArray;
	} // of getRelationMentionArgumentFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get an FSArray of eventMentionArguments of a specific XML node
	 * 
	 * @param eventMentionNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 * 
	 * @param eventMentionArgumentFSArray
	 *            FSArray which contains eventMentionArguments
	 */
	public FSArray getEventMentionArgumentFSArray(Node eventMentionNode, JCas jcas) {
		NodeList children = eventMentionNode.getChildNodes();
		ArrayList<Node> eventMentionArgumentNodeArrayList = getSpecificNodeArrayList(children,
				ELEMENT_EVENT_MENTION_ARGUMENT);
		FSArray eventMentionArgumentFSArray = new FSArray(jcas, eventMentionArgumentNodeArrayList.size());

		for (int i = 0; i < eventMentionArgumentNodeArrayList.size(); i++) {
			Node eventMentionArgumentNode = eventMentionArgumentNodeArrayList.get(i);
			de.julielab.jcore.types.ace.EventMentionArgument eventMentionArgument = new de.julielab.jcore.types.ace.EventMentionArgument(
					jcas);
			setEventMentionArgumentAttributes(eventMentionArgument, eventMentionArgumentNode);
			setEventMentionArgumentStartEnd(eventMentionArgument, eventMentionArgumentNode);
			eventMentionArgument.addToIndexes();
			eventMentionArgumentFSArray.set(i, eventMentionArgument);
		} // of for

		eventMentionArgumentFSArray.addToIndexes();
		return eventMentionArgumentFSArray;
	} // of getEventMentionArgumentFSArray

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to set the attributes for an aceEntity
	 * 
	 * @param aceEntity
	 *            ace.Entity which will be updated by adding the attributes information
	 * @param entityNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEntityAttributes(de.julielab.jcore.types.ace.Entity aceEntity, Node entityNode) {
		String entityID = retrieveAttribute(entityNode, ITEM_ID);
		String entityAceType = retrieveAttribute(entityNode, ITEM_TYPE);
		String entityAceSubtype = retrieveAttribute(entityNode, ITEM_SUBTYPE);
		String entityAceClass = retrieveAttribute(entityNode, ITEM_CLASS);
		aceEntity.setId(entityID);
		aceEntity.setAce_type(entityAceType);
		aceEntity.setAce_subtype(entityAceSubtype);
		aceEntity.setAce_class(entityAceClass);
	} // of setEntityAttributes

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to set the attributes for an aceValue
	 * 
	 * @param aceValue
	 *            ace.Value which will be updated by adding the attributes information
	 * @param valueNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setValueAttributes(de.julielab.jcore.types.ace.Value aceValue, Node valueNode) {
		String valueID = retrieveAttribute(valueNode, ITEM_ID);
		String valueAceType = retrieveAttribute(valueNode, ITEM_TYPE);
		String valueAceSubtype = retrieveAttribute(valueNode, ITEM_SUBTYPE);
		aceValue.setId(valueID);
		aceValue.setAce_type(valueAceType);
		aceValue.setAce_subtype(valueAceSubtype);
	} // setValueAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an aceRelation
	 * 
	 * @param aceRelation
	 *            ace.Relation which will be updated by adding the attributes information
	 * @param relationNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setRelationAttributes(de.julielab.jcore.types.ace.Relation aceRelation, Node relationNode) {
		String relationID = retrieveAttribute(relationNode, ITEM_ID);
		String relationType = retrieveAttribute(relationNode, ITEM_TYPE);
		String relationSubtype = retrieveAttribute(relationNode, ITEM_SUBTYPE);
		String relationTense = retrieveAttribute(relationNode, ITEM_TENSE);
		String relationModality = retrieveAttribute(relationNode, ITEM_MODALITY);
		aceRelation.setId(relationID);
		aceRelation.setAce_type(relationType);
		aceRelation.setAce_subtype(relationSubtype);
		aceRelation.setTense(relationTense);
		aceRelation.setModality(relationModality);
	} // of setRelationAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an aceRelation
	 * 
	 * @param aceEvent
	 *            ace.Event which will be updated by adding the attributes information
	 * @param eventNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEventAttributes(de.julielab.jcore.types.ace.Event aceEvent, Node eventNode) {
		String eventID = retrieveAttribute(eventNode, ITEM_ID);
		String eventType = retrieveAttribute(eventNode, ITEM_TYPE);
		String eventSubtype = retrieveAttribute(eventNode, ITEM_SUBTYPE);
		String eventModality = retrieveAttribute(eventNode, ITEM_MODALITY);
		String eventPolarity = retrieveAttribute(eventNode, ITEM_POLARITY);
		String eventGenericity = retrieveAttribute(eventNode, ITEM_GENERICITY);
		String eventTense = retrieveAttribute(eventNode, ITEM_TENSE);
		aceEvent.setId(eventID);
		aceEvent.setAce_type(eventType);
		aceEvent.setAce_subtype(eventSubtype);
		aceEvent.setModality(eventModality);
		aceEvent.setPolarity(eventPolarity);
		aceEvent.setGenericity(eventGenericity);
		aceEvent.setTense(eventTense);
	} // of setEventAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an aceTimex2
	 * 
	 * @param aceTimex2
	 *            ace.Timex2 which will be updated by adding the attributes information
	 * @param timex2Node
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setTimex2Attributes(de.julielab.jcore.types.ace.Timex2 aceTimex2, Node timex2Node) {
		String timex2ID = retrieveAttribute(timex2Node, ITEM_ID);
		String timex2Mod = retrieveAttribute(timex2Node, ITEM_MOD);
		String timex2Comment = retrieveAttribute(timex2Node, ITEM_COMMENT);
		String timex2AnchorVal = retrieveAttribute(timex2Node, ITEM_ANCHOR_VAL);
		String timex2Val = retrieveAttribute(timex2Node, ITEM_VAL);
		String timex2Set = retrieveAttribute(timex2Node, ITEM_SET);
		String timex2NonSpecific = retrieveAttribute(timex2Node, ITEM_NON_SPECIFIC);
		String timex2AnchorDir = retrieveAttribute(timex2Node, ITEM_ANCHOR_DIR);
		aceTimex2.setId(timex2ID);
		aceTimex2.setMod(timex2Mod);
		aceTimex2.setComment(timex2Comment);
		aceTimex2.setAnchor_val(timex2AnchorVal);
		aceTimex2.setVal(timex2Val);
		aceTimex2.setSet(timex2Set);
		aceTimex2.setNon_specific(timex2NonSpecific);
		aceTimex2.setAnchor_dir(timex2AnchorDir);
	} // of setTimex2Attributes

	/*----------------------------------------------------------------------------------------------*/

	public void setTimex2MentionAttributes(de.julielab.jcore.types.ace.Timex2Mention timex2Mention,
			Node timex2MentionNode) {
		String timex2MentionID = retrieveAttribute(timex2MentionNode, ITEM_ID);
		timex2Mention.setId(timex2MentionID);
	} // of setTimex2MentionAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for a relationMentionArgument
	 * 
	 * @param relationMentionArgument
	 *            ace.RelationMentionArgument which will be updated by adding the attributes information
	 * @param relationMentionArgumentNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setRelationMentionArgumentAttributes(
			de.julielab.jcore.types.ace.RelationMentionArgument relationMentionArgument,
			Node relationMentionArgumentNode) {
		String relationMentionArgumentRefID = retrieveAttribute(relationMentionArgumentNode, ITEM_REFID);
		String relationMentionArgumentRole = retrieveAttribute(relationMentionArgumentNode, ITEM_ROLE);
		relationMentionArgument.setRefid(relationMentionArgumentRefID);
		relationMentionArgument.setAce_role(relationMentionArgumentRole);
	} // setRelationMentionArgumentAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an eventMentionArgument
	 * 
	 * @param eventMentionArgument
	 *            ace.EventMentionArgument which will be updated by adding the attributes information
	 * @param eventMentionArgumentNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEventMentionArgumentAttributes(
			de.julielab.jcore.types.ace.EventMentionArgument eventMentionArgument, Node eventMentionArgumentNode) {
		String eventMentionArgumentRefID = retrieveAttribute(eventMentionArgumentNode, ITEM_REFID);
		String eventMentionArgumentRole = retrieveAttribute(eventMentionArgumentNode, ITEM_ROLE);
		eventMentionArgument.setRefid(eventMentionArgumentRefID);
		eventMentionArgument.setAce_role(eventMentionArgumentRole);
	} // of setEventMentionArgumentAttributes

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to set the attributes for a sourceFile
	 * 
	 * @param sourceFile
	 *            ace.SourceFile which will be updated by adding the attributes information
	 * @param sourceFileNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setSourceFileAttributes(de.julielab.jcore.types.ace.SourceFile aceSourceFile, Node sourceFileNode) {
		String sourceFileUri = retrieveAttribute(sourceFileNode, ITEM_URI);
		String sourceFileType = retrieveAttribute(sourceFileNode, ITEM_TYPE);
		String sourceFileSource = retrieveAttribute(sourceFileNode, ITEM_SOURCE);
		String sourceFileAuthor = retrieveAttribute(sourceFileNode, ITEM_AUTHOR);
		String sourceFileEncoding = retrieveAttribute(sourceFileNode, ITEM_ENCODING);
		aceSourceFile.setUri(sourceFileUri);
		aceSourceFile.setAce_type(sourceFileType);
		aceSourceFile.setSource(sourceFileSource);
		aceSourceFile.setAuthor(sourceFileAuthor);
		aceSourceFile.setEncoding(sourceFileEncoding);
	} // of setSourceFileAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an entityMention
	 * 
	 * @param entityMention
	 *            ace.EntityMention which will be updated by adding the attributes information
	 * @param entityMentionNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEntityMentionAttributes(de.julielab.jcore.types.ace.EntityMention entityMention,
			Node entityMentionNode) {
		String entityMentionID = retrieveAttribute(entityMentionNode, ITEM_ID);
		String entityMentionType = retrieveAttribute(entityMentionNode, ITEM_TYPE);
		String entityMentionLDCType = retrieveAttribute(entityMentionNode, ITEM_LDCTYPE);
		String entityMentionRole = retrieveAttribute(entityMentionNode, ITEM_ROLE);
		String entityMentionMetonymyMention = retrieveAttribute(entityMentionNode, ITEM_METONYMY_MENTION);
		String entityMentionLDCAtr = retrieveAttribute(entityMentionNode, ITEM_LDCATR);
		entityMention.setId(entityMentionID);
		entityMention.setMention_type(entityMentionType);
		entityMention.setMention_ldctype(entityMentionLDCType);
		entityMention.setAce_role(entityMentionRole);
		entityMention.setMetonymy_mention(entityMentionMetonymyMention);
		entityMention.setLdcatr(entityMentionLDCAtr);
	} // of setEntityMentionAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for a relationArgument
	 * 
	 * @param relationArgument
	 *            ace.RelationArgument which will be updated by adding the attributes information
	 * @param relationArgumentNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setRelationArgumentAttributes(de.julielab.jcore.types.ace.RelationArgument relationArgument,
			Node relationArgumentNode) {
		String relationArgumentREFID = retrieveAttribute(relationArgumentNode, ITEM_REFID);
		String relationArgumentRole = retrieveAttribute(relationArgumentNode, ITEM_ROLE);
		relationArgument.setRefid(relationArgumentREFID);
		relationArgument.setAce_role(relationArgumentRole);
	} // of setRelatoinArgumentAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for an eventArgument
	 * 
	 * @param eventArgument
	 *            ace.EventArgument which will be updated by adding the attributes information
	 * @param eventArgumentNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEventArgumentAttributes(de.julielab.jcore.types.ace.EventArgument eventArgument,
			Node eventArgumentNode) {
		String eventArgumentRefID = retrieveAttribute(eventArgumentNode, ITEM_REFID);
		String eventArgumentRole = retrieveAttribute(eventArgumentNode, ITEM_ROLE);
		String eventArgumentType = retrieveAttribute(eventArgumentNode, ITEM_TYPE);
		eventArgument.setRefid(eventArgumentRefID);
		eventArgument.setAce_role(eventArgumentRole);
		eventArgument.setAce_type(eventArgumentType);
	} // of setEventArgumentAttributes

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to set the attributes for an eventMention
	 * 
	 * @param eventMention
	 *            ace.EventMention which will be updated by adding the attributes information
	 * @param eventMentionNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setEventMentionAttributes(de.julielab.jcore.types.ace.EventMention eventMention, Node eventMentionNode) {
		String eventMentionID = retrieveAttribute(eventMentionNode, ITEM_ID);
		String eventMentionLevel = retrieveAttribute(eventMentionNode, ITEM_LEVEL);
		eventMention.setId(eventMentionID);
		eventMention.setLevel(eventMentionLevel);
	} // of setEventMentionAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the anchor for an eventMention
	 * 
	 * @param eventMention
	 *            ace.EventMention which will be updated by adding the anchor information
	 * @param eventMentionNode
	 *            Node which will be parsed in order to retrieve the anchor information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public void setEventMentionAnchor(de.julielab.jcore.types.ace.EventMention eventMention, Node eventMentionNode,
			JCas jcas) {
		NodeList children = eventMentionNode.getChildNodes();
		ArrayList<Node> anchorNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_ANCHOR);

		// Since there should be one and only one anchor, it will be accessed directly.
		// ATTENTION! This could be critical as some event mentions in a given XML
		// file might not have an anchor node.
		Node anchorNode = anchorNodeArrayList.get(0);
		de.julielab.jcore.types.ace.Anchor eventMentionAnchor = new de.julielab.jcore.types.ace.Anchor(jcas);

		try {
			setEventMentionAnchorStartEnd(eventMentionAnchor, anchorNode);
		} // of try
		catch (Exception e) {
			logger.info("setEventMentionAnchor(ace.EventMention, Node, JCas): " + e.getMessage()
					+ "\nATTENTION! Couldn't retrieve the start-end-information of " + anchorNode.getNodeName() + "!");
		} // of catch

		eventMentionAnchor.addToIndexes();
		eventMention.setAnchor(eventMentionAnchor);
	} // of setEventMentionAnchor

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the LDC scope for an eventMention
	 * 
	 * @param eventMention
	 *            ace.EventMention which will be updated by adding the LDC scope information
	 * @param eventMentionNode
	 *            Node which will be parsed in order to retrieve the LDC scope information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public void setEventMentionLDCScope(de.julielab.jcore.types.ace.EventMention eventMention, Node eventMentionNode,
			JCas jcas) {
		NodeList children = eventMentionNode.getChildNodes();
		ArrayList<Node> LDCScopeNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_LDC_SCOPE);

		// Since there should be one and only one ldc scope per event mention, it will be accessed directly.
		// ATTENTION! This could be critical since some event mentions might not have an ldc scope.
		Node LDCScopeNode = LDCScopeNodeArrayList.get(0);
		de.julielab.jcore.types.ace.LDC_Scope eventMentionLDCScope = new de.julielab.jcore.types.ace.LDC_Scope(jcas);

		try {
			setEventMentionLDCScopeStartEnd(eventMentionLDCScope, LDCScopeNode);
		} // of try
		catch (Exception e) {
			logger
					.info("setEventMentionAnchor(ace.EventMention, Node, JCas): " + e.getMessage()
							+ "\nATTENTION! Couldn't retrieve the start-end-information of "
							+ LDCScopeNode.getNodeName() + "!");
		} // of catch

		eventMentionLDCScope.addToIndexes();
		eventMention.setLdc_scope(eventMentionLDCScope);
	} // of setEventMentionLDCScope

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for a relationMention
	 * 
	 * @param relationMention
	 *            ace.RelationMention which will be updated by adding the attributes information
	 * @param relationMentionNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setRelationMentionAttributes(de.julielab.jcore.types.ace.RelationMention relationMention,
			Node relationMentionNode) {
		String relationMentionID = retrieveAttribute(relationMentionNode, ITEM_ID);
		String relationMentionLexCond = retrieveAttribute(relationMentionNode, ITEM_LEXICALCONDITION);
		relationMention.setId(relationMentionID);
		relationMention.setLexical_condition(relationMentionLexCond);
	} // of setRelationMentionAttributes

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to set the attributes for a valueMention
	 * 
	 * @param valueMention
	 *            ace.ValueMention which will be updated by adding the attributes information
	 * @param valueMentionNode
	 *            Node which will be parsed in order to retrieve the attributes
	 */
	public void setValueMentionAttributes(de.julielab.jcore.types.ace.ValueMention valueMention, Node valueMentionNode) {
		String valueMentionID = retrieveAttribute(valueMentionNode, ITEM_ID);
		valueMention.setId(valueMentionID);
	} // of setValueMentionAttributes

	/*----------------------------------------------------------------------------------------------*/

	/**
	 * This method is used to get an FSArray of valueMentions of a specific XML node
	 * 
	 * @param valueNode
	 *            Node which will be processed in order to extract the proper information to fill the FSArray
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getValueMentionFSArray(Node valueNode, JCas jcas) {
		NodeList children = valueNode.getChildNodes();
		ArrayList<Node> valueMentionNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_VALUE_MENTION);
		FSArray valueMentionFSArray = new FSArray(jcas, valueMentionNodeArrayList.size());

		for (int i = 0; i < valueMentionNodeArrayList.size(); i++) {
			Node valueMentionNode = valueMentionNodeArrayList.get(i);
			de.julielab.jcore.types.ace.ValueMention valueMention = new de.julielab.jcore.types.ace.ValueMention(jcas);
			setValueMentionAttributes(valueMention, valueMentionNode);
			setValueMentionStartEnd(valueMention, valueMentionNode);
			valueMention.addToIndexes();
			valueMentionFSArray.set(i, valueMention);
		} // of for

		valueMentionFSArray.addToIndexes();
		return valueMentionFSArray;
	} // of getValueMentionFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to update the entity mention information by adding the entity mention head
	 * 
	 * @param entityMention
	 *            ace.EntityMention which head will be updated
	 * @param entityMentionNode
	 *            Node which will be parsed in order to retrieve the head information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public void setEntityMentionHead(de.julielab.jcore.types.ace.EntityMention entityMention, Node entityMentionNode,
			JCas jcas) {
		try {
			de.julielab.jcore.types.ace.Head entityMentionHead = new de.julielab.jcore.types.ace.Head(jcas);
			getHeadInformation(entityMentionHead, entityMentionNode);
			entityMention.setHead(entityMentionHead);
		} // of try
		catch (Exception e) {
			logger.info("setEntityMentionHead(ace.EntityMention, Node, JCas): " + e.getMessage()
					+ "\nAttention! Entity mention head information couldn't be retrieved!");
		} // of catch
	} // of setEntityMentionHead

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the start and the of an entity mention from an XML node
	 * 
	 * @param entityMention
	 *            jules.types.ace.EntityMention which will be updated by adding the start and end information
	 * @param entityMentionNode
	 *            Node which contains the entity mention start and end
	 */
	public void setEntityMentionStartEnd(de.julielab.jcore.types.ace.EntityMention entityMention, Node entityMentionNode) {
		int entityMentionStart = retrieveStartEndValue(entityMentionNode, ITEM_START);
		int entityMentionEnd = retrieveStartEndValue(entityMentionNode, ITEM_END);
		validateStartEnd(entityMentionStart, entityMentionEnd, getMentionCharseqNode(entityMentionNode));
		entityMention.setBegin(entityMentionStart);
		entityMention.setEnd(entityMentionEnd + 1);
	} // of getEntityMentionStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to get the information about the head of an entity mention
	 * 
	 * @param entityMentionHead
	 *            the jules.types.ace.Head which will be returned after retrieving information from the XML file
	 * @param entityMentionNode
	 *            a Node which contain the head information
	 */
	public void getHeadInformation(de.julielab.jcore.types.ace.Head entityMentionHead, Node entityMentionNode) {
		int headStart = 0;
		int headEnd = 0;
		NodeList children = entityMentionNode.getChildNodes();
		ArrayList<Node> auxiliaryArrayList = getSpecificNodeArrayList(children, ELEMENT_HEAD);

		// As there should be only one head per entity mention, the auxiliaryHeadArrayList
		// should have only one element which will be accessed straightaway.
		Node headNode = auxiliaryArrayList.get(0);

		// Now, the charseq node will be accessed using the same procedure as accessing the
		// extent node.
		children = headNode.getChildNodes();
		auxiliaryArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = auxiliaryArrayList.get(0);

		try {
			headStart = Integer.parseInt(retrieveAttribute(charseqNode, ITEM_START));
			headEnd = Integer.parseInt(retrieveAttribute(charseqNode, ITEM_END));
			validateStartEnd(headStart, headEnd, charseqNode);
		} // of try
		catch (Exception e) {
			logger.info("getHeadInformation(ace.Head, Node): " + e.getMessage() + "\nATTENTION! The node "
					+ entityMentionNode.getNodeName() + " has no valid head information.");
		} // of catch

		entityMentionHead.setBegin(headStart);
		entityMentionHead.setEnd(headEnd + 1);
		entityMentionHead.addToIndexes();
	} // of getHead

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve from a NodeList the nodes with a specific name. Note that an ArrayList is
	 * returned, not a NodeList!
	 * 
	 * @param children
	 *            NodeList from which specific nodes will be retrieved
	 * @param elementName
	 *            String which specifies the name of the nodes which will be retrieved from the NodeList
	 * @return specificNodeArrayList ArrayList which contains retrieved nodes from the NodeList
	 */
	public ArrayList<Node> getSpecificNodeArrayList(NodeList children, String elementName) {
		Node child = null;
		ArrayList<Node> specificNodeArrayList = new ArrayList<Node>();

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			if (child.getNodeName().equals(elementName)) {
				specificNodeArrayList.add(child);
			} // of if
		} // of for

		return specificNodeArrayList;
	} // of getSpecificNodeList

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to put the document information of the XML file into the proper object of the AceTypeSystem.
	 * 
	 * @param documentNode
	 *            Node which will be parsed in order to retrieve the document information
	 * @param aceDocument
	 *            ace.Document which will be updated by adding retrieved document information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public void addDocumentInformation(Node documentNode, de.julielab.jcore.types.ace.Document aceDocument, JCas jcas) {
		try {
			String docID = retrieveAttribute(documentNode, ITEM_DOCID);
			FSArray entityFSArray = getEntityFSArray(documentNode, jcas);
			FSArray valueFSArray = getValueFSArray(documentNode, jcas);
			FSArray timex2FSArray = getTimex2FSArray(documentNode, jcas);
			FSArray relationFSArray = getRelationFSArray(documentNode, jcas);
			FSArray eventFSArray = getEventFSArray(documentNode, jcas);
			aceDocument.setDocid(docID);
			aceDocument.setEntities(entityFSArray);
			aceDocument.setValues(valueFSArray);
			aceDocument.setTimex2(timex2FSArray);
			aceDocument.setRelations(relationFSArray);
			aceDocument.setEvents(eventFSArray);
			aceDocument.addToIndexes();
		} // of try
		catch (Exception e) {
			logger.error("addDocumentInformation(Node, ace.Document, JCas): " + e.getMessage()
					+ "\nATTENTION! Document information couldn't be retrieved!\n" + e.getStackTrace());
		} // of catch
	} // of addDocumentInformation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build the FSArray which will contain aceEntities
	 * 
	 * @param documentNode
	 *            Node that will be parsed in order to retrieve entity information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */

	public FSArray getEntityFSArray(Node documentNode, JCas jcas) {
		NodeList children = documentNode.getChildNodes();
		ArrayList<Node> entityNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_ENTITY);
		FSArray entityFSArray = new FSArray(jcas, entityNodeArrayList.size());

		for (int i = 0; i < entityNodeArrayList.size(); i++) {
			de.julielab.jcore.types.ace.Entity aceEntity = new de.julielab.jcore.types.ace.Entity(jcas);
			Node entityNode = entityNodeArrayList.get(i);
			addEntityInformation(entityNode, aceEntity, jcas);
			entityFSArray.set(i, aceEntity);
		} // of for

		entityFSArray.addToIndexes();
		return entityFSArray;
	} // of getEntityFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build the FSArray which will contain aceValues
	 * 
	 * @param documentNode
	 *            Node that will be parsed in order to retrieve value information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getValueFSArray(Node documentNode, JCas jcas) {
		NodeList children = documentNode.getChildNodes();
		ArrayList<Node> valueNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_VALUE);
		FSArray valueFSArray = new FSArray(jcas, valueNodeArrayList.size());

		for (int i = 0; i < valueNodeArrayList.size(); i++) {
			de.julielab.jcore.types.ace.Value aceValue = new de.julielab.jcore.types.ace.Value(jcas);
			Node valueNode = valueNodeArrayList.get(i);
			addValueInformation(valueNode, aceValue, jcas);
			valueFSArray.set(i, aceValue);
		} // of for

		valueFSArray.addToIndexes();
		return valueFSArray;
	} // of getValueFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build the FSArray which will contain aceValues
	 * 
	 * @param documentNode
	 *            Node that will be parsed in order to retrieve value information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getTimex2FSArray(Node documentNode, JCas jcas) {
		NodeList children = documentNode.getChildNodes();
		ArrayList<Node> timex2NodeArrayList = getSpecificNodeArrayList(children, ELEMENT_TIMEX2);
		FSArray timex2FSArray = new FSArray(jcas, timex2NodeArrayList.size());

		for (int i = 0; i < timex2NodeArrayList.size(); i++) {
			de.julielab.jcore.types.ace.Timex2 aceTimex2 = new de.julielab.jcore.types.ace.Timex2(jcas);
			Node timex2Node = timex2NodeArrayList.get(i);
			addTimex2Information(timex2Node, aceTimex2, jcas);
			timex2FSArray.set(i, aceTimex2);
		} // of for

		timex2FSArray.addToIndexes();
		return timex2FSArray;
	} // of getTimex2FSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build the FSArray which will contain aceValues
	 * 
	 * @param documentNode
	 *            Node that will be parsed in order to retrieve value information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getRelationFSArray(Node documentNode, JCas jcas) {
		NodeList children = documentNode.getChildNodes();
		ArrayList<Node> relationNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_RELATION);
		FSArray relationFSArray = new FSArray(jcas, relationNodeArrayList.size());

		for (int i = 0; i < relationNodeArrayList.size(); i++) {
			de.julielab.jcore.types.ace.Relation aceRelation = new de.julielab.jcore.types.ace.Relation(jcas);
			Node relationNode = relationNodeArrayList.get(i);
			addRelationInformation(relationNode, aceRelation, jcas);
			relationFSArray.set(i, aceRelation);
		} // of for

		relationFSArray.addToIndexes();
		return relationFSArray;
	} // of getRelationFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build the FSArray which will contain aceEvents
	 * 
	 * @param documentNode
	 *            Node that will be parsed in order to retrieve value information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getEventFSArray(Node documentNode, JCas jcas) {
		NodeList children = documentNode.getChildNodes();
		ArrayList<Node> eventNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_EVENT);
		FSArray eventFSArray = new FSArray(jcas, eventNodeArrayList.size());

		for (int i = 0; i < eventNodeArrayList.size(); i++) {
			de.julielab.jcore.types.ace.Event aceEvent = new de.julielab.jcore.types.ace.Event(jcas);
			Node eventNode = eventNodeArrayList.get(i);
			addEventInformation(eventNode, aceEvent, jcas);
			aceEvent.addToIndexes();
			eventFSArray.set(i, aceEvent);
		} // of for

		eventFSArray.addToIndexes();
		return eventFSArray;
	} // of getEventFSArray

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to put the source file information of the XML file into the proper object of the
	 * AceTypeSystem.
	 * 
	 * @param domDocument
	 *            the Document which contains the parsed information from the XML file
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public void addSourceFileInformation(Document domDocument, JCas jcas) {
		NodeList sourceFileNodeList = domDocument.getElementsByTagName(ELEMENT_SOURCE_FILE);

		for (int i = 0; i < sourceFileNodeList.getLength(); i++) {
			Node sourceFileNode = sourceFileNodeList.item(i);
			de.julielab.jcore.types.ace.SourceFile aceSourceFile = new de.julielab.jcore.types.ace.SourceFile(jcas);
			setSourceFileAttributes(aceSourceFile, sourceFileNode);
			FSArray documentsFSArray = getDocumentFSArray(sourceFileNode, jcas);
			aceSourceFile.setDocuments(documentsFSArray);
			aceSourceFile.addToIndexes();
		} // of for
	} // of addSourceFileInformation

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to build an FSArray which will contain instances of ace.Documents
	 * 
	 * @param domDocument
	 *            Document which will be parsed in order to retrieve aceDocument information
	 * @param jcas
	 *            JCas which will be filled with retrieved information
	 */
	public FSArray getDocumentFSArray(Node sourceFileNode, JCas jcas) {
		NodeList children = sourceFileNode.getChildNodes();
		ArrayList<Node> documentNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_DOCUMENT);
		FSArray documentFSArray = new FSArray(jcas, documentNodeArrayList.size());

		for (int i = 0; i < documentNodeArrayList.size(); i++) {
			try {
				de.julielab.jcore.types.ace.Document aceDocument = new de.julielab.jcore.types.ace.Document(jcas);
				Node documentNode = documentNodeArrayList.get(i);
				addDocumentInformation(documentNode, aceDocument, jcas);
				documentFSArray.set(i, aceDocument);
			} // of try
			catch (Exception e) {
				logger.info("getDocumentFSArray(Node, JCas): " + e.getMessage()
						+ "\nATTENTION! No document information could be retrieved.");
			} // of catch
		} // of for

		documentFSArray.addToIndexes();
		return documentFSArray;
	} // of getDocumentFSArray




	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to initialize attributes which are needed for the processing of the XML files
	 */
	public void initialize() {

		UimaContext aContext = this.getUimaContext();

		if ((Boolean) aContext.getConfigParameterValue(GENERATE_JCORE_TYPES) != null) {
			generateJcoreTypes = (Boolean) aContext.getConfigParameterValue(GENERATE_JCORE_TYPES);
		} // of if
		generateMappings();

//		logger = getUimaContext().getLogger();
		logger.info("initialize() - Initializing Ace Reader...");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			builder = factory.newDocumentBuilder();
		} // of try
		catch (ParserConfigurationException e) {
			logger.error("initialize(): " + e.getMessage() + "\n" + e.getStackTrace());
		} // of catch

		files = getFilesFromInputDirectory();
	} // of initialize

	/*----------------------------------------------------------------------------------------------*/
	private void generateMappings() {
		mappings = new Hashtable<String, String>();
		mappings.put("PART-WHOLE", "PART_WHOLE");
		mappings.put("PER-SOC", "PER_SOC");
		mappings.put("ORG-AFF", "ORG_AFF");
		mappings.put("GEN-AFF", "GEN_AFF");
		mappings.put("ART", "ART");
		mappings.put("PHYS", "PHYS");
		mappings.put("Numeric", "Numeric");
		mappings.put("Sentence", "SentenceACE");
		mappings.put("Contact-Info", "Contact_Info");
		mappings.put("Crime", "Crime");
		mappings.put("Job-Title", "Job_Title");
	} // of generateMappings

	/*----------------------------------------------------------------------------------------------*/
	public void close() throws IOException {

	} // of close

	/*----------------------------------------------------------------------------------------------*/
	public Progress[] getProgress() {
		return null;
	} // of getProgress

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * @see org.apache.uima.collection.CollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {

		return currentIndex < files.size();
	} // of hasNext

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Get files from directory that is specified in the configuration parameter PARAM_INPUTDIR of the collection reader
	 * descriptor.
	 * 
	 * @return documentFiles List which contain the files from the input directory
	 */
	private List<File> getFilesFromInputDirectory() {

		List<File> documentFiles = new ArrayList<File>();
        directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());

		if (!directory.exists() || !directory.isDirectory()) {
			logger.info("getFilesFromInputDirectory(): "
					+ "ERROR! Input directory doesn't exist or is not a directory.");
			return null;
		} // of if

		// get list of files (not subdirectories) in the specified directory.
		File[] dirFiles = directory.listFiles();
		for (int i = 0; i < dirFiles.length; i++) {
			if (!dirFiles[i].isDirectory() && dirFiles[i].getAbsolutePath().endsWith("apf.xml")) {
				logger.info("getFilesFromInputDirectory():  FILE NAME: " + dirFiles[i].toString());
				documentFiles.add(dirFiles[i]);
				System.out.println(dirFiles[i]);
			} // of if
		} // of for

		return documentFiles;
	} // of getFilesFromInoutDirectory

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve a specific sgm file from the input directory
	 * 
	 * @param sgmFileName
	 *            String which specifies the sgm file which has to be returned
	 * @return sgmFile File which is specified by the sgmFileName
	 */
	private File getSgmFileFromInputDirectory(String sgmFileName) {
		File sgmFile = null;
		File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
		if (!directory.exists() || !directory.isDirectory()) {
			logger.error("getSgmFileFromInputDirectory(): ERROR! Input directory doesn't exist or is not a directory.");
			return null;
		} // of if
		File[] dirFiles = directory.listFiles();
		for (int i = 0; i < dirFiles.length; i++) {
			if (!dirFiles[i].isDirectory() && dirFiles[i].getAbsolutePath().endsWith(sgmFileName)) {
				sgmFile = dirFiles[i];
				break;
			} // of if
		} // of for
		return sgmFile;
	} // getSgmFileFromInputDirectory

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to check if the retrieved start-end-information complies with the document text
	 * 
	 * @param start
	 *            Integer which contains the start infromation
	 * @param end
	 *            Integer which contains the end infromation
	 * @param node
	 *            Node which contains the text span which is delimited by the start and end
	 */
	private void validateStartEnd(int start, int end, Node charseqNode) {
		String charseqTextSpan = charseqNode.getTextContent();
		charseqTextSpan = replaceWhiteChar(charseqTextSpan);

		String retrievedTextSpan = documentText.substring(start, end + 1);
		boolean spansEqual = charseqTextSpan.equals(retrievedTextSpan);

		if (!spansEqual) {
			logger.error("validateStartEnd(int, int, Node): wrong start-end-information!" + "\nSTART: "
					+ start + "\nEND: " + end + "\ncharseq text span: " + charseqTextSpan + "\nretrieved text span: "
					+ retrievedTextSpan);
			startEndFailureCounter++;
		} // of if
	} // validateStartEnd

	/*----------------------------------------------------------------------------------------------*/
	/**
	 * This method is used to retrieve the charseqNode of a given mention node which for example can be a value mention
	 * node or a timex2 mention node. Due to the same structure of this type of nodes (a mention node contains an extent
	 * node, which in turn contains the charseq node) only one method is provided instead of having a separate method
	 * for every kind of mention nodes. Please note that mention argument nodes can and will be processed by this very
	 * method as the mention argument nodes have the same structure (extent node, charseq node).
	 * 
	 * @param mentionNode
	 *            Node which will be parsed in order to retrieve the charseq node
	 * @return charseqNode Node which was retrieved after parsing the mention node
	 */
	private Node getMentionCharseqNode(Node mentionNode) {
		NodeList children = mentionNode.getChildNodes();
		ArrayList<Node> extentNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_EXTENT);

		// As always, there should be only one extent node per entity mention node.
		Node extentNode = extentNodeArrayList.get(0);
		children = extentNode.getChildNodes();

		// There should also be only one charseq node in every extent node.
		ArrayList<Node> charseqNodeArrayList = getSpecificNodeArrayList(children, ELEMENT_CHARSEQ);
		Node charseqNode = charseqNodeArrayList.get(0);
		return charseqNode;
	} // of getEntityMentionCharseqNode

} // of AceReader
