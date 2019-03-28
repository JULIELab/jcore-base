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
package de.julielab.jcore.ae.ecn;

import de.julielab.jcore.types.Enzyme;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ECNumberAnnotator extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(ECNumberAnnotator.class);
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String document = aJCas.getDocumentText();
		
		Pattern p = Pattern.compile("EC \\d\\.(\\d{1,2}|-{1})\\.(\\d{1,2}|-{1})\\.(\\d{1,3}|-{1})");
		Matcher m = p.matcher(document);
		
		while(m.find()) {
			Enzyme annotation = new Enzyme(aJCas);
			annotation.setBegin(m.start());
			annotation.setEnd(m.end());
			annotation.setComponentId(this.getClass().getName());
			String type = document.substring(m.start()+3, m.end());
			annotation.setSpecificType(type);
			annotation.addToIndexes();
		}
	}

}
