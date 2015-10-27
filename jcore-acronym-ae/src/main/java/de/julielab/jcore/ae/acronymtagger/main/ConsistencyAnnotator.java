/**
 * ConsistencyAnnotator.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: jwermter
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 26.01.2009
 **/
package de.julielab.jcore.ae.acronymtagger.main;

import java.util.Stack;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.Annotation;

/**
 * TODO insert description
 * 
 * @author jwermter
 */
public class ConsistencyAnnotator {

	/**
	 * loops over document and adds the full form (expanded form) to all strings that have
	 * previously been recognized as an acronym.
	 * 
	 * @param aJCas
	 */
	public void consistencyAnnotate(JCas aJCas) {
		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
		FSIterator acronymIter = indexes.getAnnotationIndex(Abbreviation.type).iterator();
		String documenttext = aJCas.getDocumentText();
		String fullformtext;
		String shortformtext;
		Abbreviation tempAbbr;
		Abbreviation abbr;
		Stack<Abbreviation> stack = new Stack<Abbreviation>();
		int pos;
		int abbrPos;
		/*
		 * look for occurences of the shortform - create the Abbreviation objects + put them on the
		 * stack
		 */
		while (acronymIter.hasNext()) {
			abbr = (Abbreviation) acronymIter.next();
			shortformtext = abbr.getCoveredText();
			fullformtext = abbr.getExpan();
			Annotation textRef = abbr.getTextReference();
			abbrPos = abbr.getBegin();
			pos = 0;
			while ((pos = documenttext.indexOf(shortformtext, pos)) >= 0) {
				if (pos != abbrPos) {
					/*
					 * if(pos > 2 && pos-2 < documenttext.length()) { fragment =
					 * documenttext.substring(pos-2, shortformtext.length()+2); int beginWhitespace =
					 * fragment.indexOf(" "); int endWhitespace = fragment.lastIndexOf(" "); if }
					 * else if(pos > 2) fragment = documenttext.substring(pos-2,
					 * shortformtext.length()); else if(pos-2 < documenttext.length()) fragment =
					 * documenttext.substring(pos, shortformtext.length()+2); else fragment =
					 * documenttext.substring(pos, shortformtext.length());
					 */
					tempAbbr = new Abbreviation(aJCas, pos, pos + shortformtext.length());
					tempAbbr.setExpan(fullformtext);
					// abbr is not introduced here:
					tempAbbr.setDefinedHere(false);
					tempAbbr.setTextReference(textRef);
					stack.push(tempAbbr);
				}
				pos++;
			}
			// plural acronyms to match singular ones, e.g. intracranial aneurisms (IAs) --> IA
			if (shortformtext.matches("^[a-z]*[A-Z][A-Z]+s$")) {
				shortformtext = shortformtext.substring(0, shortformtext.length() - 1);
				pos = 0;
				while ((pos = documenttext.indexOf(shortformtext, pos)) >= 0) {
					if (pos != abbrPos) {
						tempAbbr = new Abbreviation(aJCas, pos, pos + shortformtext.length());
						tempAbbr.setExpan(fullformtext);
						// abbr is not introduced here:
						tempAbbr.setDefinedHere(false);
						tempAbbr.setTextReference(textRef);
						stack.push(tempAbbr);
					}
					pos++;
				}
			}
		}
		/*
		 * make an entry for every Abbreviation on the stack
		 */
		int begin = 0;
		int end = 0;
		while (!stack.empty()) {
			tempAbbr = stack.pop();
			begin = tempAbbr.getBegin();
			end = tempAbbr.getEnd();
			// TODO: check whether "parent" token is not too big
			// if(isActualAcronym(documenttext, begin, end)) {
			tempAbbr.addToIndexes();
			// System.err.println(tempAbbr);
			// }
			// tempAbbr.removeFromIndexes();
		}
	}
}