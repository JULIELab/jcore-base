/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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