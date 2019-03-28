/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.lingpipe.porterstemmer;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import de.julielab.jcore.types.StemmedForm;
import de.julielab.jcore.types.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

public class LingpipePorterstemmerAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> tokenIt = aJCas.getAnnotationIndex(Token.type).iterator();
		while (tokenIt.hasNext()) {
			Token token = (Token) tokenIt.next();
			String stem = PorterStemmerTokenizerFactory.stem(token.getCoveredText());
			StemmedForm stemmedForm = new StemmedForm(aJCas);
			// Stemming might change characters (bushy -> bushi) and thus the
			// stem has to be set to the value feature. We use the offsets to
			// indicate the original token span from which the stem was derived.
			stemmedForm.setBegin(token.getBegin());
			stemmedForm.setEnd(token.getEnd());
			stemmedForm.setValue(stem);
			token.setStemmedForm(stemmedForm);
		}
	}

}
