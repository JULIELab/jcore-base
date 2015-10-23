/** 
 * ToIOBConsumer.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: faessler
 * 
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: 05.09.2007 
 * 
 * A CAS Consumer that converts UIMA annotations into IOB format
 **/

/**
 * 
 */
package de.julielab.jcore.consumer.cas2iob.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.cas2iob.utils.UIMAUtils;
import de.julielab.jules.types.Paragraph;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.segmentationEvaluator.IOBToken;
import de.julielab.segmentationEvaluator.IOToken;

/**
 * @author faessler
 * 
 */
public class ToIOBConsumer extends CasConsumer_ImplBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(ToIOBConsumer.class);

	private final String SENTENCE_END_MARK = "SENTENCE_END_MARKER"; // there will be an empty line for each sentence marker
	private final String PARAGRAPH_END_MARK = "PARAGRAPH_END_MARKER"; // there will be 2 empty lines for each sentence marker
	String mode = null;

	String outFileName = null;
	String typePath = null;
	String[] labels = null;

	HashMap<String, String> objNameMethMap = null;

	HashMap<String, String> labelIOBMap = null;

	public void initialize() throws ResourceInitializationException {

		LOGGER.info("Initializing...");

		final String regexp = "[\\s=/\\|]";

		labels = (String[]) getConfigParameterValue("labels");

		outFileName = (String) getConfigParameterValue("outFileName");

		final String[] labelNameMethods = (String[]) getConfigParameterValue("labelNameMethods");

		final String[] iobLabelNames = (String[]) getConfigParameterValue("iobLabelNames");

		typePath = (String) getConfigParameterValue("typePath");
		if (typePath == null) {
			typePath = "";
		}

		mode = (String) getConfigParameterValue("mode");
		if (mode.equals("IOB") || mode.equals("iob")) {
			mode = "IOB";
		} else if (mode.equals("IO") || mode.equals("io")) {
			mode = "IO";
		} else {
			throw new ResourceInitializationException();
		}

		if (labelNameMethods != null) {

			objNameMethMap = new HashMap<String, String>();

			for (int i = 0; i < labelNameMethods.length; i++) {
				String[] parts = labelNameMethods[i].split(regexp);
				if (parts.length == 1) {
					objNameMethMap.put(typePath + parts[0], null);
				} else {
					objNameMethMap.put(typePath + parts[0], parts[1]);
				}
			}

		}

		if (iobLabelNames != null) {

			labelIOBMap = new HashMap<String, String>();

			for (int i = 0; i < iobLabelNames.length; i++) {
				String[] parts = iobLabelNames[i].split(regexp);
				labelIOBMap.put(parts[0], parts[1]);
			}

		}

	}

	public void processCas(CAS cas) throws ResourceProcessException {

		LOGGER.info("Converting CAS to IO(B)Tokens...");

		IOToken[] ioTokens = convertToIOB(cas);

		LOGGER.info("Writing IO(B) file...");

		BufferedWriter bw;

		try {
			bw = new BufferedWriter(new FileWriter(outFileName));
			for (IOToken token : ioTokens) {
				if (token.getText().equals("") || token.getText().equals(SENTENCE_END_MARK)) {
					// empty line for sentence break
					bw.write("\n");
				} else if (token.getText().equals("") || token.getText().equals(PARAGRAPH_END_MARK)) {
					bw.write("\n\n");
				} else {
					bw.write(token + "\n");
				}
			}

			if (bw != null) {
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.info("The IO(B) file was written to " + outFileName);
	}

	/**
	 * @param cas
	 */
	public IOToken[] convertToIOB(CAS cas) {

		Boolean no_paragraphs = true;
		ArrayList<IOToken> ioTokens = new ArrayList<IOToken>();

		JCas jcas = null;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			e.printStackTrace();
		}

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();

		Iterator[] annotationIters = new Iterator[objNameMethMap.size()];

		Iterator it = objNameMethMap.keySet().iterator();
		for (int i = 0; it.hasNext(); i++) {

			String objName = (String) it.next();

			Annotation ann = null;
			try {
				ann = JCoReAnnotationTools.getAnnotationByClassName(jcas, objName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			annotationIters[i] = indexes.getAnnotationIndex(ann.getTypeIndexID()).iterator();

		}

		TreeMap<Integer, IOToken> ioTokenMap = new TreeMap<Integer, IOToken>();

		// label all tokens that are in range of an annotation
		tokenLabeling(ioTokenMap, annotationIters, jcas);

		System.out.println("map: " + ioTokenMap);

		// add the rest of the tokens, i.e. tokens not in range of an annotation type mentioned in
		// the descriptor

		// get a list with all paragraphs
		Iterator<Annotation> paragraphIter = indexes.getAnnotationIndex(Paragraph.type).iterator();
		ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
		while (paragraphIter.hasNext()) {
			paragraphs.add((Paragraph) paragraphIter.next());
		}
		if (paragraphs.isEmpty()) {
			Paragraph dParagraph = null;
			try {
				dParagraph = (Paragraph) JCoReAnnotationTools.getAnnotationByClassName(jcas,Paragraph.class.getName());
			} catch (ClassNotFoundException | SecurityException
					| NoSuchMethodException | IllegalArgumentException
					| InstantiationException | IllegalAccessException
					| InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dParagraph.setBegin(0);
			dParagraph.setEnd(jcas.getDocumentText().length());
			dParagraph.addToIndexes(jcas);
			
			paragraphs.add(dParagraph);
		}
//		Iterator<Sentence> sentIter =
//		
//		while (paragraphIter.hasNext()) {
//			Paragraph paragraph = paragraphIter.next();
//			//System.out.println(paragraph.getCoveredText());
//			ArrayList<Sentence> paragraphSentences = (ArrayList<Sentence>) UIMAUtils.getAnnotations(jcas, paragraph,
//							(new Sentence(jcas, 0, 0).getClass()));
//			//System.out.println("sentences: " + paragraphSentences.size());
//
//			// }
		
		Paragraph lastPara = null;
		
//		int paraCount = 0;
		int overallSentCount = 0;
		
		Iterator<Annotation> sentIter = indexes.getAnnotationIndex(Sentence.type).iterator();

		while (sentIter.hasNext()) {
			Sentence sentence = (Sentence) sentIter.next();
			int sentCount = 0;
			// find paragraph in which this sentence falls
			// might be null if a sentence falls in more than one paragraphs, then we keep the last paragraph
			Paragraph currentParagraph = lastPara;
			for(Paragraph para: paragraphs) {
				if (para.getBegin()<=sentence.getBegin() && para.getEnd() >= sentence.getEnd()) {
					currentParagraph = para;
				}
				
			//for (Sentence sentence : paragraphSentences) {
				
				//System.out.println("para: " + paraCount + ", sent: " + sentCount);
				// get tokens
				ArrayList<Token> tokenList = (ArrayList<Token>) UIMAUtils.getAnnotations(jcas, sentence, (new Token(
								jcas, 0, 0)).getClass());
				for (int i = 0; i < tokenList.size(); i++) {
					Token token = tokenList.get(i);

					// if we are at the first token, we need to add a sentence break mark which is
					// later replaced by an empty line
					if (i == 0 && overallSentCount > 0) {
						IOToken ioToken = null;
						//if (sentCount == 0) {
						if (currentParagraph!=lastPara) {
							// add paragraph end before this sentence
							//System.out.println("para end");
							ioToken = new IOBToken(PARAGRAPH_END_MARK, PARAGRAPH_END_MARK);
						} else {
							//System.out.println("sent end");
							// add sentence end before this sentence
							ioToken = new IOBToken(SENTENCE_END_MARK, SENTENCE_END_MARK);
						}

						ioTokenMap.put(token.getBegin() - 1, ioToken);
					}

					if (!ioTokenMap.containsKey(token.getBegin())) {
						IOToken ioToken = new IOBToken(token.getCoveredText(), "O");
						ioTokenMap.put(token.getBegin(), ioToken);
					}
				}
				overallSentCount++;
				sentCount++;
			}
		}

		Set beginSet = ioTokenMap.keySet();
		for (Iterator beginIt = beginSet.iterator(); beginIt.hasNext();) {
			Integer begin = (Integer) beginIt.next();
			IOToken ioToken = ioTokenMap.get(begin);
			ioTokens.add(ioToken);
		}

		// go over IOBtokens and cast them to IO tokens if necessary
		IOToken[] ret = new IOToken[ioTokens.size()];
		// System.out.println("converting tokens to mode: " + mode);
		if (mode.equals("IOB")) {
			// IOB tokens
			ret = ioTokens.toArray(ret);
		} else {
			// IO tokens
			for (int i = 0; i < ioTokens.size(); i++) {
				IOBToken iobToken = (IOBToken) ioTokens.get(i);
				ret[i] = iobToken.toXIoToken();
			}
		}

		return ret;
	}

	/**
	 * Generates all IOTokens which UIMA tokens are in range of an UIMA annotation given by
	 * annotationIters. Therefore, every annotation is considered that is entered into the
	 * consumer's descriptor.
	 * 
	 * @param tokenMap
	 * @param annotationIters
	 */
	private void tokenLabeling(TreeMap<Integer, IOToken> ioTokenMap, Iterator[] annotationIters, JCas jcas) {

		AnnotationIndex tokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);

		for (int i = 0; i < annotationIters.length; i++) {
			Iterator annoIter = annotationIters[i];

			while (annoIter.hasNext()) {
				// get all annotations of this annotation iterator
				Annotation ann = (Annotation) annoIter.next();
				String label = getAnnotationLabel(ann);
				FSIterator subtokenIterator = tokenIndex.subiterator(ann);

				try {
					Token token = (Token) subtokenIterator.next();
					Integer begin = token.getBegin();

					if (!ioTokenMap.containsKey(begin)) {
						IOToken ioToken = new IOBToken(token.getCoveredText(), "B_" + label);
						ioTokenMap.put(begin, ioToken);
						while (subtokenIterator.hasNext()) {
							token = (Token) subtokenIterator.next();
							begin = token.getBegin();
							ioToken = new IOBToken(token.getCoveredText(), "I_" + label);
							ioTokenMap.put(begin, ioToken);
						}
					} else {
						handleCompetingAnnotations(ioTokenMap, label, subtokenIterator, token, begin);
					}

				} catch (NoSuchElementException e) {
					LOGGER.warn("no token in anno: " + ann.getCoveredText());
					// e.printStackTrace();
				}

			}
		}

	}

	/**
	 * @param ioTokenMap
	 * @param label
	 * @param subtokenIterator
	 * @param token
	 * @param begin
	 */
	private void handleCompetingAnnotations(TreeMap<Integer, IOToken> ioTokenMap, String label,
					FSIterator subtokenIterator, Token token, Integer begin) {
		// computing length of existing annotation
		int oldLength = 0;
		Set keySet = ioTokenMap.keySet();
		for (Iterator keyIt = keySet.iterator(); keyIt.hasNext();) {
			Integer index = (Integer) keyIt.next();
			IOToken actToken = ioTokenMap.get(index);

			if (index >= begin) {
				if (!actToken.getLabel().equals(label) || (!actToken.getIobMark().equals("I") && oldLength > 0)) {
					break;
				}
				++oldLength;
			}
		}

		// getting new annotation and it's length by ArrayList.size()
		HashMap<IOToken, Integer> newTokenSeq = new HashMap<IOToken, Integer>();
		IOToken ioToken = new IOBToken(token.getCoveredText(), "B_" + label);
		newTokenSeq.put(ioToken, begin);

		while (subtokenIterator.hasNext()) {
			token = (Token) subtokenIterator.next();
			begin = token.getBegin();
			ioToken = new IOBToken(token.getCoveredText(), "I_" + label);
			newTokenSeq.put(ioToken, begin);
		}

		// if the new sequence is larger than the existing, override the old one
		if (newTokenSeq.size() > oldLength) {
			Set hashKeys = newTokenSeq.keySet();
			for (Iterator hashIt = hashKeys.iterator(); hashIt.hasNext();) {
				ioToken = (IOBToken) hashIt.next();
				begin = newTokenSeq.get(ioToken);
				ioTokenMap.put(begin, ioToken);
			}
		}
	}

	/**
	 * get the label for a identified annotation. This is done using reflection.
	 */
	private String getAnnotationLabel(Annotation ann) {
		String ret = null;

		Class annClass = ann.getClass();
		Method getLabelMethod = null;
		String methodName = objNameMethMap.get(annClass.getName());

		try {
			if (methodName == null) {
				ret = annClass.getName();
			} else {
				getLabelMethod = annClass.getMethod(methodName);
				ret = (String) getLabelMethod.invoke(ann, (Object[]) null);

				// TODO remove before checkin
				// if labels are contatenated with "|" use only first part
				// this is only needed for MUC export
				// int pos = 0;
				// if ((pos = ret.indexOf("|")) > -1) {
				// ret = ret.substring(0, pos);
				// }
				// end remove

			}
		} catch (NoSuchMethodException e) {
			LOGGER.error("The class \"" + annClass.getName() + "\" does not have a method \"" + methodName + "\".");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// perhaps a label was found but is part of the blacklist?
		if (ret != null) {
			for (String label : labels) {
				if (ret.equals(label)) {
					ret = null;
				}
			}
		}

		// checking if the potentially found label is supposed to get a special name in the output
		// file
		if (ret != null && labelIOBMap.get(ret) != null) {
			ret = labelIOBMap.get(ret);
		}

		return ret;
	}
}
