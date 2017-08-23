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
package de.julielab.jcore.ae.stanford.lemma;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Token;
import edu.stanford.nlp.process.Morphology;

public class StanfordLemmatizer extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(StanfordLemmatizer.class);

	/**
	 * The Stanford CoreNLP lemmatizer component. Note that the actual class
	 * MorphaAnnotator requires the whole CoreNLP pipeline (sentence splitting,
	 * tokenization, POS tagging) to run beforehand. We therefore use this class
	 * instead to integrate lemmatization in our own pipeline.
	 */
	Morphology lemmatizer = new Morphology();

	@SuppressWarnings("rawtypes")
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		LOGGER.debug("[JCoRe Stanford Lemmatizer] processing document ...");
		FSIterator tokenIterator = aJCas.getAnnotationIndex(Token.type)
				.iterator();

		while (tokenIterator.hasNext()) {
			Token token = (Token) tokenIterator.next();
			int start = token.getBegin();
			int end = token.getEnd();
			String tokenStr = token.getCoveredText();
			
			String posTagStr = null;
			FSArray posTagArray = token.getPosTag();
			if (null != posTagArray && posTagArray.size() > 0) {
				// At the moment there is only one POS tag assigned, so only take the first one.
				for (int i = 0; i < posTagArray.size(); i++) {
					POSTag posTag = (POSTag) posTagArray.get(i);
					if (posTag != null) {
						posTagStr = posTag.getValue();
					}
				}
				String lemmaStr = lemmatizer.lemma(tokenStr, posTagStr);
				Lemma lemma = new Lemma(aJCas);
				lemma.setBegin(start);
				lemma.setEnd(end);
				lemma.setValue(lemmaStr);
				lemma.setComponentId(this.getClass().getName());
				token.setLemma(lemma);
			}
		}
	}
}
