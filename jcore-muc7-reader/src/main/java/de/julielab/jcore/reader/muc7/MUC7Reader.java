/** 
 * MUC7Reader.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 * 
 * Author: poprat
 * 
 * Current version: 1.1.1	
 * Since version:   1.0
 *
 * Creation date: Oct 11, 2007 
 * 
 * CollectionReader for MUC7 texts (in particular the coreferences and entities)
 **/
package de.julielab.jcore.reader.muc7;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Progress;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.Paragraph;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.muc7.Coref;
import de.julielab.jcore.types.muc7.ENAMEX;
import de.julielab.jcore.types.muc7.MUC7Header;
import de.julielab.jcore.types.muc7.NUMEX;
import de.julielab.jcore.types.muc7.TIMEX;

public class MUC7Reader extends CollectionReader_ImplBase {

	/**
	 * Logger for this class
	 */
	private static Logger logger = null;
	// XML documents to be defined
	/**
	 * XML element of the root of the MUC7 articles
	 */
	private static final String ELEMENT_DOCS = "DOC";
	/**
	 * XML element of the id of the MUC7 articles
	 */
	private static final String ELEMENT_DOCID = "DOCID";
	/**
	 * XML element of the storyid of the MUC7 articles
	 */
	private static final String ELEMENT_STORYID = "STORYID";
	/**
	 * XML element of the slug of the MUC7 articles
	 */
	private static final String ELEMENT_SLUG = "SLUG";
	/**
	 * XML element of the date of the MUC7 articles
	 */
	private static final String ELEMENT_DATE = "DATE";
	/**
	 * XML element of the numbe of words of the MUC7 articles
	 */
	private static final String ELEMENT_NWORDS = "NWORDS";
	/**
	 * XML element of the preamble of the MUC7 articles
	 */
	private static final String ELEMENT_PREAMBLE = "PREAMBLE";
	/**
	 * XML element of the text of the MUC7 articles
	 */
	private static final String ELEMENT_TEXT = "TEXT";
	/**
	 * XML element of the paragraphs of the MUC7 articles
	 */
	private static final String ELEMENT_PARAGRAPH = "p";
	/**
	 * XML element of the corefences in the MUC7 articles
	 */
	private static final String ELEMENT_COREF = "COREF";
	/**
	 * XML element of the TIMEX-NEs in the MUC7 articles
	 */
	private static final String ELEMENT_TIMEX = "TIMEX";
	/**
	 * XML element of the ENAMEX-NEs in the MUC7 articles
	 */
	private static final String ELEMENT_ENAMEX = "ENAMEX";
	/**
	 * XML element of the NUMEX-NEs in the MUC7 articles
	 */
	private static final String ELEMENT_NUMEX = "NUMEX";
	/**
	 * the attribute name of the ENAMEX and the TIMEX NEs for the head (MINIMUM)
	 */
	private static final String ELEMENT_NE_MIN = "MIN";
	/**
	 * the attribute name of the ENAMEX and the TIMEX NEs for the head (TYPE)
	 */
	private static final String ELEMENT_NE_TYPE = "TYPE";
	/**
	 * XML element of the corefences in the MUC7 articles
	 */
	private static final String ELEMENT_TRAILER = "TRAILER";
	/**
	 * XML elements comprised in an object list
	 */
	// public static final String[] ELEMENT_TEXT_TO_BE_PROCESSED = { ELEMENT_SLUG, ELEMENT_DATE,
	// ELEMENT_NWORDS,
	// ELEMENT_PREAMBLE, ELEMENT_TEXT, ELEMENT_TRAILER };
	public static final String[] ELEMENT_TEXT_TO_BE_PROCESSED = { ELEMENT_TEXT };
	/**
	 * the position in the text to always get the correct offset
	 */
	private static int startPosition;
	/**
	 * the HashMap that has the coreference ID as key and the MUC7Coreference as value; used to
	 * build the CAS object (in particular useful when building the referring CAS objects)
	 */
	private static HashMap<Integer, MUC7Coreference> corefHashMap;
	/**
	 * List of all files with abstracts XML
	 */
	private List<File> files;
	/**
	 * 
	 */
	private HashMap<String, ArrayList<Node>> docIDDocNodeHash;
	/**
	 * 
	 */
	private Iterator<String> keyIter;
	/**
	 * JCAS
	 */
	private JCas jcas;
	/**
	 * DocumentBuilder for creating Document objects (XML)
	 */
	private DocumentBuilder builder;
	/**
	 * Name of configuration parameter that must be set to the path of a directory containing input
	 * files.
	 */
	public static final String PARAM_INPUTDIR = "InputDirectory";

	private HashMap<String, ArrayList<Node>> buildDocIDDocNodeHash(List<File> files) {
		HashMap<String, ArrayList<Node>> docIDDocNodeHash = new HashMap<String, ArrayList<Node>>();
		for (File file : files) {
			Document doc;
			logger.log(Level.INFO, "buildDocIDDocNodeHash() -- Reading file " + file.getName());
			try {
				doc = builder.parse(file);
				NodeList documentNL = doc.getElementsByTagName(ELEMENT_DOCS);
				for (int i = 0; i < documentNL.getLength(); i++) {
					Node docNode = documentNL.item(i);
					Node docIDNode = getChildrenNodes(docNode, ELEMENT_DOCID, new ArrayList<Node>()).get(0);
					String docID = docIDNode.getTextContent();
					ArrayList<Node> docNodes;
					if (docIDDocNodeHash.containsKey(docID)) {
						docNodes = docIDDocNodeHash.get(docID);
					} else {
						docNodes = new ArrayList<Node>();
					}
					docNodes.add(docNode);
					docIDDocNodeHash.put(docID, docNodes);
				}
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return docIDDocNodeHash;
	}

	/**
	 * the text to be processed (to be stored as document text in the CAS)
	 * 
	 * @param docNode
	 *            (the root node, actually)
	 * @return the text to be processed
	 */
	private String annotateTextToBeProcessed(Node docNode) {
		String textToBeProcessed = "";
		ArrayList<Node> al = new ArrayList<Node>();
		al = getChildrenNodes(docNode, ELEMENT_TEXT_TO_BE_PROCESSED, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			textToBeProcessed = textToBeProcessed + al.get(i).getTextContent();
		}
		textToBeProcessed = normalizeString(textToBeProcessed);
		// System.out.println(textToBeProcessed);
		jcas.setDocumentText(textToBeProcessed);
		return textToBeProcessed;
	}

	/**
	 * the header information (docID and storyID) that is annotated begin=0; end=length of the
	 * document text
	 * 
	 * @param docNode
	 *            (the root node)
	 */
	private void annotateHeader(Node docNode) {
		String docID = normalizeString(getChildrenNodes(docNode, ELEMENT_DOCID, new ArrayList<Node>()).get(0)
						.getTextContent());
		String storyID = normalizeString(getChildrenNodes(docNode, ELEMENT_STORYID, new ArrayList<Node>()).get(0)
						.getTextContent());
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(MUC7Header.type).iterator();
		while (iter.hasNext()) {
			MUC7Header muc7header = (MUC7Header) iter.next();
			if (muc7header.getDocId().equals(docID) && muc7header.getStoryID().equals(storyID)) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			MUC7Header muc7Header = new MUC7Header(jcas);
			muc7Header.setDocId(docID);
			muc7Header.setStoryID(storyID);
			muc7Header.addToIndexes(jcas);
		}
	}

	/**
	 * the slug to be annotated with offest information; we see this as a section of a particular
	 * type
	 * 
	 * @param docNode
	 *            (the root node)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotateSlug(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_SLUG, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		// HACK --- without a leading non-whitspace character it doesn't work ...
		// text = "+" + text;
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[1];
		// annotating other things that occur in this section
		buildCorefHashMap(al.get(0), beginEnd);
		annotateENAMEX(al.get(0), beginEnd);
		annotateTIMEX(al.get(0), beginEnd);
		annotateNUMEX(al.get(0), beginEnd);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == (beginEnd[0] - 1) && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Slug")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Slug");
			section.setBegin(beginEnd[0] - 1);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the date to be annotated with offset information; we see this as a section of a particular
	 * type; this is actually necessary because also the date might be coreferential
	 * 
	 * @param docNode
	 *            (the root node, actually)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotateDate(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_DATE, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[1];
		// annotating other things that occur in this section
		buildCorefHashMap(al.get(0), beginEnd);
		annotateENAMEX(al.get(0), beginEnd);
		annotateTIMEX(al.get(0), beginEnd);
		annotateNUMEX(al.get(0), beginEnd);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == beginEnd[0] && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Date")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Date");
			section.setBegin(beginEnd[0]);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the number of words to be annotated with offset information; we see this as a section of a
	 * particular type; this is actually necessary because sometimes this is wrongly annotated as
	 * date which might be coreferential
	 * 
	 * @param docNode
	 *            (the root node)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotateNumOfWords(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_NWORDS, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[1];
		// annotating other things that occur in this section
		buildCorefHashMap(al.get(0), beginEnd);
		annotateENAMEX(al.get(0), beginEnd);
		annotateTIMEX(al.get(0), beginEnd);
		annotateNUMEX(al.get(0), beginEnd);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == beginEnd[0] && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Number of Words")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Number of Words");
			section.setBegin(beginEnd[0]);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the preamble to be annotated with offset information;
	 * 
	 * @param docNode
	 *            (the root node)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotatePreamble(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_PREAMBLE, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[1];
		// annotating other things that occur in this section
		buildCorefHashMap(al.get(0), beginEnd);
		annotateENAMEX(al.get(0), beginEnd);
		annotateTIMEX(al.get(0), beginEnd);
		annotateNUMEX(al.get(0), beginEnd);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == beginEnd[0] && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Preamble")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Preamble");
			section.setBegin(beginEnd[0]);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the main text to be annotated
	 * 
	 * @param docNode
	 *            (the root node)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotateText(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_TEXT, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[0];
		// annotating other things that occur in this section
		annotateParagraphs(al.get(0), textToBeProcessed);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == beginEnd[0] && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Text")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Text");
			section.setBegin(beginEnd[0]);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the trailer to be annotated
	 * 
	 * @param docNode
	 *            (the root node)
	 * @param textToBeProcessed
	 *            (the document text)
	 */
	private void annotateTrailer(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_TRAILER, new ArrayList<Node>());
		String text = normalizeString(al.get(0).getTextContent());
		beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
		startPosition = beginEnd[1];
		// annotating other things that occur in this section
		buildCorefHashMap(al.get(0), beginEnd);
		annotateENAMEX(al.get(0), beginEnd);
		annotateTIMEX(al.get(0), beginEnd);
		annotateNUMEX(al.get(0), beginEnd);
		boolean exist = false;
		FSIterator iter = jcas.getAnnotationIndex(Section.type).iterator();
		while (iter.hasNext()) {
			Section section = (Section) iter.next();
			if (section.getBegin() == beginEnd[0] && section.getEnd() == beginEnd[1]
							&& section.getSectionType().equals("Trailer")) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			Section section = new Section(jcas);
			section.setSectionType("Trailer");
			section.setBegin(beginEnd[0]);
			section.setEnd(beginEnd[1]);
			section.addToIndexes(jcas);
		}
	}

	/**
	 * the paragraphs to be annotated
	 * 
	 * @param docNode
	 *            (the text node)
	 * @param textToBeProcessed
	 *            (the text that is covered by the text node)
	 */
	private void annotateParagraphs(Node docNode, String textToBeProcessed) {
		ArrayList<Node> al = new ArrayList<Node>();
		int[] beginEnd = { 0, 0 };
		al = getChildrenNodes(docNode, ELEMENT_PARAGRAPH, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			String text = normalizeString(al.get(i).getTextContent());
			beginEnd = getBeginEndOfSequence(text, textToBeProcessed, startPosition);
			startPosition = beginEnd[1];
			// annotating other things that occur in this section
			// buildCorefHashMap(al.get(i), beginEnd);
			annotateENAMEX(al.get(i), beginEnd);
			annotateTIMEX(al.get(i), beginEnd);
			annotateNUMEX(al.get(i), beginEnd);
			boolean exist = false;
			FSIterator iter = jcas.getAnnotationIndex(Paragraph.type).iterator();
			while (iter.hasNext()) {
				Paragraph para = (Paragraph) iter.next();
				if (para.getBegin() == beginEnd[0] && para.getEnd() == beginEnd[1]) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				Paragraph para = new Paragraph(jcas);
				para.setBegin(beginEnd[0]);
				para.setEnd(beginEnd[1]);
				para.addToIndexes(jcas);
			}
		}
	}

	/**
	 * in this method we annotate the TIMEX entities
	 * 
	 * @param docNode
	 *            (the node where to search for TIMEX)
	 * @param beginEnd
	 *            (begin and end of the docNode text)
	 */
	private void annotateTIMEX(Node docNode, int[] beginEnd) {
		ArrayList<Node> al = new ArrayList<Node>();
		al = getChildrenNodes(docNode, ELEMENT_TIMEX, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			Node timexNode = al.get(i);
			String min = "";
			if (timexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN) != null) {
				min = normalizeString(timexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN).getNodeValue());
			}
			String type = normalizeString(timexNode.getAttributes().getNamedItem(ELEMENT_NE_TYPE).getNodeValue());
			String leftContext = normalizeString(getLeftTextContext(timexNode, docNode));
			int[] beginEndLeftContext = { 0, 0 };
			beginEndLeftContext = getBeginEndOfSequence(leftContext, normalizeString(docNode.getTextContent()), 0);
			// System.out.println("???-"+jcas.getDocumentText().substring(beginEndLeftContext[0]+beginEnd[0],
			// beginEndLeftContext[1]+beginEnd[0])+"--");
			int[] beginEndTimex = { 0, 0 };
			beginEndTimex = getBeginEndOfSequence(normalizeString(al.get(i).getTextContent()), normalizeString(docNode
							.getTextContent()), beginEndLeftContext[1]);
			beginEndTimex[0] = beginEndTimex[0] + beginEnd[0];
			beginEndTimex[1] = beginEndTimex[1] + beginEnd[0];
			boolean exist = false;
			FSIterator iter = jcas.getAnnotationIndex(TIMEX.type).iterator();
			while (iter.hasNext()) {
				TIMEX timex = (TIMEX) iter.next();
				if (timex.getBegin() == beginEndTimex[0] && timex.getEnd() == beginEndTimex[1]
								&& timex.getSpecificType().equals(type) && timex.getMin().equals(min)) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				TIMEX timex = new TIMEX(jcas);
				timex.setBegin(beginEndTimex[0]);
				timex.setEnd(beginEndTimex[1]);
				timex.setSpecificType(type);
				timex.setMin(min);
				timex.addToIndexes(jcas);
			}
		}
	}

	/**
	 * in this method we annotate the ENAMEX entities
	 * 
	 * @param docNode
	 *            (the node where to search for ENAMEX)
	 * @param beginEnystem.out.println("SIZE:"+corefHashMap.size());d
	 *            (begin and end of the docNode text)
	 */
	private void annotateENAMEX(Node docNode, int[] beginEnd) {
		ArrayList<Node> al = new ArrayList<Node>();
		al = getChildrenNodes(docNode, ELEMENT_ENAMEX, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			Node enamexNode = al.get(i);
			String min = "";
			if (enamexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN) != null) {
				min = enamexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN).getNodeValue();
			}
			String type = normalizeString(enamexNode.getAttributes().getNamedItem(ELEMENT_NE_TYPE).getNodeValue());
			String leftContext = normalizeString(getLeftTextContext(enamexNode, docNode));
			int[] beginEndLeftContext = { 0, 0 };
			beginEndLeftContext = getBeginEndOfSequence(leftContext, normalizeString(docNode.getTextContent()), 0);
//			System.out.println(jcas.getDocumentText().substring(beginEndLeftContext[0] + beginEnd[0],
//							beginEndLeftContext[1] + beginEnd[0]));
			int[] beginEndEnamex = { 0, 0 };
			beginEndEnamex = getBeginEndOfSequence(normalizeString(al.get(i).getTextContent()), normalizeString(docNode
							.getTextContent()), beginEndLeftContext[1]);
			beginEndEnamex[0] = beginEndEnamex[0] + beginEnd[0];
			beginEndEnamex[1] = beginEndEnamex[1] + beginEnd[0];
			boolean exist = false;
			FSIterator iter = jcas.getAnnotationIndex(ENAMEX.type).iterator();
			while (iter.hasNext()) {
				ENAMEX enamex = (ENAMEX) iter.next();
				if (enamex.getBegin() == beginEndEnamex[0] && enamex.getEnd() == beginEndEnamex[1]
								&& enamex.getSpecificType().equals(type) && enamex.getMin().equals(min)) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				ENAMEX enamex = new ENAMEX(jcas);
				enamex.setBegin(beginEndEnamex[0]);
				enamex.setEnd(beginEndEnamex[1]);
				enamex.setSpecificType(type);
				enamex.setMin(min);
				enamex.addToIndexes(jcas);
			}
		}
	}

	/**
	 * in this method we annotate the NUMEX entities
	 * 
	 * @param docNode
	 *            (the node where to search for NUMEX)
	 * @param beginEnd
	 *            (begin and end of the docNode text)
	 */
	private void annotateNUMEX(Node docNode, int[] beginEnd) {
		ArrayList<Node> al = new ArrayList<Node>();
		al = getChildrenNodes(docNode, ELEMENT_NUMEX, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			Node numexNode = al.get(i);
			String min = "";
			if (numexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN) != null) {
				min = normalizeString(numexNode.getAttributes().getNamedItem(ELEMENT_NE_MIN).getNodeValue())
								.replaceAll("^ +", "");
			}
			String type = normalizeString(numexNode.getAttributes().getNamedItem(ELEMENT_NE_TYPE).getNodeValue());
			String leftContext = normalizeString(getLeftTextContext(numexNode, docNode));
			int[] beginEndLeftContext = { 0, 0 };
			beginEndLeftContext = getBeginEndOfSequence(leftContext, normalizeString(docNode.getTextContent()), 0);
			// System.out.println(jcas.getDocumentText().substring(beginEndLeftContext[0]+beginEnd[0],
			// beginEndLeftContext[1]+beginEnd[0]));
			int[] beginEndNumex = { 0, 0 };
			beginEndNumex = getBeginEndOfSequence(normalizeString(al.get(i).getTextContent()), normalizeString(docNode
							.getTextContent()), beginEndLeftContext[1]);
			beginEndNumex[0] = beginEndNumex[0] + beginEnd[0];
			beginEndNumex[1] = beginEndNumex[1] + beginEnd[0];
			boolean exist = false;
			FSIterator iter = jcas.getAnnotationIndex(NUMEX.type).iterator();
			while (iter.hasNext()) {
				NUMEX numex = (NUMEX) iter.next();
				if (numex.getBegin() == beginEndNumex[0] && numex.getEnd() == beginEndNumex[1]
								&& numex.getSpecificType().equals(type) && numex.getMin().equals(min)) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				NUMEX numex = new NUMEX(jcas);
				numex.setBegin(beginEndNumex[0]);
				numex.setEnd(beginEndNumex[1]);
				numex.setSpecificType(type);
				numex.setMin(min);
				numex.addToIndexes(jcas);
			}
		}
	}

	/**
	 * this methods fills the corefHashMap with the id of the coreference as a key and the
	 * coreference object muc7coref as value; note that the position of the coreference is
	 * determined by its left textual context (the text from the beginnnig of the paragraph to the
	 * coreference) in order not to get ambiguities
	 * 
	 * @param docNode
	 *            (the paragraph node)
	 * @param beginEnd
	 *            (the begin and end of the paragraph)
	 */
	private void buildCorefHashMap(Node docNode, int[] beginEnd) {
		ArrayList<Node> al = new ArrayList<Node>();
		al = getChildrenNodes(docNode, ELEMENT_COREF, new ArrayList<Node>());
		for (int i = 0; i < al.size(); i++) {
			String leftContext = normalizeString(getLeftTextContext(al.get(i), docNode));
			int[] beginEndLeftContext = { 0, 0 };
			beginEndLeftContext = getBeginEndOfSequence(leftContext, normalizeString(docNode.getTextContent()), 0);
			int[] beginEndCoref = { 0, 0 };
			beginEndCoref = getBeginEndOfSequence(normalizeString(al.get(i).getTextContent()), normalizeString(docNode
							.getTextContent()), beginEndLeftContext[1]);
			beginEndCoref[0] = beginEndCoref[0] + beginEnd[0];
			beginEndCoref[1] = beginEndCoref[1] + beginEnd[0];
			MUC7Coreference muc7Coref = new MUC7Coreference();
			muc7Coref.setBegin(beginEndCoref[0]);
			muc7Coref.setEnd(beginEndCoref[1]);
			int id = (new Integer(al.get(i).getAttributes().getNamedItem("ID").getNodeValue())).intValue();
			muc7Coref.setId(id);
			if (al.get(i).getAttributes().getNamedItem("REF") != null) {
				int refID = (new Integer(al.get(i).getAttributes().getNamedItem("REF").getNodeValue())).intValue();
				muc7Coref.setRefID(refID);
			} else {
				muc7Coref.setRefID(-1);
			}
			if (al.get(i).getAttributes().getNamedItem("TYPE") != null) {
				String typeOfCoref = normalizeString(al.get(i).getAttributes().getNamedItem("TYPE").getNodeValue());
				muc7Coref.setTypeOfCoref(typeOfCoref);
			}
			if (al.get(i).getAttributes().getNamedItem("MIN") != null) {
				String minHead = normalizeString(al.get(i).getAttributes().getNamedItem("MIN").getNodeValue());
				muc7Coref.setMinHead(minHead);
			}
			if (!corefHashMap.containsKey(id)) {
				corefHashMap.put(id, muc7Coref);
			}
		}
	}

	/**
	 * takes the information for the corefHashMap and builds the CAS-Coref
	 */
	private void annotateCorefs() {
		Set<Integer> keys = corefHashMap.keySet();
		Iterator<Integer> keysIter = keys.iterator();
		while (keysIter.hasNext()) {
			Integer id = keysIter.next();
			buildCorefFromCorefHashMap(id);
		}
		// only if coreferences have been found in the text (that is: the size of corefHashMap
		// is large enough ...
		if (corefHashMap.size() > 0) {
			buildCorefReferences();
		}
	}

	/**
	 * takes the value of the corefHashMap for a given id and stores these information to the CAS
	 * object; note that this methods is called recursively
	 * 
	 * @param id
	 *            (the id of the coreference as stored in the corefHashMap)
	 * @return (the CAS coreference object)
	 */
	private Coref buildCorefFromCorefHashMap(int id) {
		MUC7Coreference muc7Coref = corefHashMap.get(id);
		if (getCorefFromCAS(muc7Coref.getBegin(), muc7Coref.getEnd()) == null) {
			boolean exist = false;
			FSIterator iter = jcas.getAnnotationIndex(Coref.type).iterator();
			while (iter.hasNext()) {
				Coref coref = (Coref) iter.next();
				if (coref.getBegin() == muc7Coref.getBegin() && coref.getEnd() == muc7Coref.getEnd()
								&& coref.getCorefType().equals(muc7Coref.getTypeOfCoref())
								&& coref.getMin().equals(muc7Coref.getMinHead()) && coref.getId() == muc7Coref.getId()) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				Coref coref = new Coref(jcas);
				coref.setBegin(muc7Coref.getBegin());
				coref.setEnd(muc7Coref.getEnd());
				coref.setCorefType(muc7Coref.getTypeOfCoref());
				coref.setMin(muc7Coref.getMinHead());
				coref.setId(muc7Coref.getId());
				coref.addToIndexes(jcas);
				return coref;
			}
		}
		return new Coref(jcas);
	}

	/**
	 * this methis goes through all Corefs and adds the coreferenced object
	 */
	private void buildCorefReferences() {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator corefIter = indexes.getAnnotationIndex(Coref.type).iterator();
		while (corefIter.hasNext()) {
			Coref c = (Coref) corefIter.next();
			int corefID = c.getId();
			int refID = corefHashMap.get(corefID).getRefID();
			if (refID > -1) {
				Coref refCoref = getCorefFromCAS(corefHashMap.get(refID).getBegin(), corefHashMap.get(refID).getEnd());
				c.setRef(refCoref);
			}
		}
	}

	/**
	 * this method returns the CAS coreference object with a certain offset; null else
	 * 
	 * @param begin
	 *            (the begin of the int[] beginEndCoref = {0,0}; beginEndCoref =
	 *            getBeginEndOfSequence(al.get(i).getTextContent(), docNode.getTextContent(),
	 *            beginEndLeftContext[1]);coreference offset)
	 * @param end
	 *            (the end of the coreference offset)
	 * @return returns the CAS coreference object or null
	 */
	private Coref getCorefFromCAS(int begin, int end) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator corefIter = indexes.getAnnotationIndex(Coref.type).iterator();
		while (corefIter.hasNext()) {
			Coref c = (Coref) corefIter.next();
			if (c.getBegin() == begin && c.getEnd() == end) {
				return c;
			}
		}
		return null;
	}

	/**
	 * this method is used to get the left textual context of a certain paragraph, for example;
	 * 
	 * @param centerNode
	 *            (the node from which the left context starts)
	 * @param sectionNode
	 *            (the node to determined the left textual context)
	 * @return the left textual context as a string
	 */
	private String getLeftTextContext(Node centerNode, Node sectionNode) {
		String rightContext = "";
		ArrayList<Node> textNodes = getChildrenNodes(sectionNode, "#text", new ArrayList<Node>());
		ArrayList<Node> textCenterNode = getChildrenNodes(centerNode, "#text", new ArrayList<Node>());
		Node rightMostTextCenterNode = textCenterNode.get(0);
		for (int i = 0; i < textNodes.size() && !textNodes.get(i).equals(rightMostTextCenterNode); i++) {
			rightContext = rightContext + textNodes.get(i).getTextContent();
		}
		return rightContext;
	}

	/**
	 * Get files from directory that is specified in the configuration parameter PARAM_INPUTDIR of
	 * the collection reader descriptor.
	 */
	private List<File> getFilesFromInputDirectory() {
		List<File> documentFiles = new ArrayList<File>();
		File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
		// if input directory does not exist or is not a directory, throw exception
		if (!directory.exists() || !directory.isDirectory()) {
			logger.log(Level.WARNING, "getFilesFromInputDirectory() " + directory
							+ " does not exist. Client has to set configuration parameter '" + PARAM_INPUTDIR + "'.");
			return null;
		}
		// get list of files (not subdirectories) in the specified directory
		File[] dirFiles = directory.listFiles();
		for (int i = 0; i < dirFiles.length; i++) {
			if (!dirFiles[i].isDirectory()) {
				documentFiles.add(dirFiles[i]);
			}
		}
		logger.log(Level.INFO, "MUC7 Reader found " + documentFiles.size() + " files in folder " + directory + ".");
		return documentFiles;
	}

	/**
	 * given a node in a tree and a node name, this methods returns all children nodes that match
	 * the node name definied in nodeName
	 * 
	 * @param node
	 *            (the start node to search)
	 * @param nodeName
	 *            (the node's name to be searched)
	 * @param al
	 *            (the array list of found nodes)
	 * @return an array list of nodes that match the nodeName criteria
	 */
	private ArrayList<Node> getChildrenNodes(Node node, String nodeName, ArrayList<Node> al) {
		if (node.getNodeName().equals(nodeName) && !node.getTextContent().equals("")) {
			al.add(node);
		}
		if (node.hasChildNodes()) {
			NodeList nl = node.getChildNodes();
			for (int j = 0; j < nl.getLength(); j++) {
				getChildrenNodes(nl.item(j), nodeName, al);
			}
		}
		return al;
	}

	/**
	 * given a set of child nodes to be search in the sting array nodeNames, a root node to be
	 * started, this method returns nodes that match the criteria
	 * 
	 * @param node
	 *            (the root node where the search should be started)
	 * @param nodeNames
	 *            (the name of the child nodes that will be searched)
	 * @param al
	 *            (the return array list of nodes)
	 * @return the array list of found nodes
	 */
	private ArrayList<Node> getChildrenNodes(Node node, String[] nodeNames, ArrayList<Node> al) {
		for (int i = 0; i < nodeNames.length; i++) {
			String nodeName = nodeNames[i];
			al = getChildrenNodes(node, nodeName, al);
		}
		return al;
	}

	/**
	 * Given a token, a string in which the token occurs and a stating point, this methods retrieves
	 * begin and end position of this token.
	 * 
	 * @param tokenString
	 *            (the token to be searched)
	 * @param inputString
	 *            (the string in which we search the token)
	 * @param startOfToken
	 *            (the begin where the token should be searched in the inputString)
	 * @return the begin and the end position of the token in an int array {begin, end}
	 */
	public int[] getBeginEndOfToken(String tokenString, String inputString, int startOfToken) {
		int[] beginEnd = { startOfToken, 0 };
		String subString = inputString.substring(startOfToken);
		beginEnd[0] = subString.indexOf(tokenString) + startOfToken;
		beginEnd[1] = tokenString.length() + beginEnd[0];
		return beginEnd;
	}

	/**
	 * Given a sequence, a string in which the token occurs and a stating point, this methods
	 * retrieves begin and end position of this sequence.
	 * 
	 * @param sequenceString
	 *            (the sequence to be searched)
	 * @param inputString
	 *            (the string in which we search de sequence)
	 * @param startOfSequence
	 *            (the begin were the sequence shpuld be searched in the inputString)
	 * @return the begin and the end position of the sequence in an int array {begin, end}
	 */
	public int[] getBeginEndOfSequence(String sequenceString, String inputString, int startOfSequence) {
		int[] beginEnd = { startOfSequence, 0 };
		int[] beginEndTemp = { 0, 0 };
		String[] inputStringArr = sequenceString.split(" ");
		if (inputStringArr.length > 0) {
			beginEndTemp = getBeginEndOfToken(inputStringArr[0], inputString, startOfSequence);
		} else {
			beginEndTemp = getBeginEndOfToken(sequenceString, inputString, startOfSequence);
		}
		beginEnd[0] = beginEndTemp[0];
		beginEnd[1] = beginEndTemp[1];
		for (int i = 1; i < inputStringArr.length; i++) {
			beginEndTemp = getBeginEndOfToken(inputStringArr[i], inputString, beginEndTemp[1]);
			beginEnd[1] = beginEndTemp[1];
		}
		return beginEnd;
	}

	/**
	 * normalizes a string by replacing newlines by whitspaces, by removing sequences of more that
	 * one whitespace and by removing the newlines at the beginning of a line; also removes stuff
	 * like "A;N;D;R;LR;" etc.
	 * 
	 * @param stringToBeNormalized
	 * @return the normalized string
	 */
	public String normalizeString(String stringToBeNormalized) {
		stringToBeNormalized = stringToBeNormalized.replaceAll("[A-Z]+;", "");
		stringToBeNormalized = stringToBeNormalized.replaceAll("\n", " ");
		stringToBeNormalized = stringToBeNormalized.replaceAll("\\s+", " ");
		stringToBeNormalized = stringToBeNormalized.replaceFirst("^[\\s]+", "");
		return stringToBeNormalized;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {
		logger = getUimaContext().getLogger();
		logger.log(Level.INFO, "initialize() - Initializing MUC7 Reader...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.log(Level.SEVERE, "initialize() " + e.getMessage());
		}
		files = getFilesFromInputDirectory();
		if (files != null && files.size() > 0) {
			docIDDocNodeHash = buildDocIDDocNodeHash(files);
			keyIter = docIDDocNodeHash.keySet().iterator();
		}
	}

	public void getNext(CAS cas) throws IOException, CollectionException {
		String key = keyIter.next();
		ArrayList<Node> docNodes;
		docNodes = docIDDocNodeHash.get(key);
		try {
			jcas = cas.getJCas();
			jcas.reset();
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		String textToBeProcessed = annotateTextToBeProcessed(docNodes.get(0));
		for (int i = 0; i < docNodes.size(); i++) {
			Node docNode = docNodes.get(i);
			corefHashMap = new HashMap<Integer, MUC7Coreference>();
			startPosition = 0;
			// important: keep this sequence!
			annotateHeader(docNode);
//			annotateSlug(docNode, textToBeProcessed);
//			annotateDate(docNode, textToBeProcessed);
//			annotateNumOfWords(docNode, textToBeProcessed);
//			annotatePreamble(docNode, textToBeProcessed);
			annotateText(docNode, textToBeProcessed);
//			annotateTrailer(docNode, textToBeProcessed);
			annotateCorefs();
		}
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
	}

	public Progress[] getProgress() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasNext() throws IOException, CollectionException {
		return keyIter.hasNext();
	}
}
