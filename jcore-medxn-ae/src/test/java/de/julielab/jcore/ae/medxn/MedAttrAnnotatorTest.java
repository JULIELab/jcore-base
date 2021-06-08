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
package de.julielab.jcore.ae.medxn;

import de.julielab.jcore.types.medical.GeneralAttributeMention;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MedAttrAnnotatorTest {

	private static final String AE_DESCRIPTOR = "de.julielab.jcore.ae.medxn.desc.jcore-medxn-ae-attributes-german";
	private static final String TEST_FILES_ROOT = "src/test/resources/de/julielab/jcore/ae/medxn/test_strings/";
	private static boolean setUpIsDone = false;
	
	AnalysisEngine attrExtractor;
	JCas ajcas;

	private String loadTestExpression(String tpath) throws IOException {
		File fin = new File((String) tpath.trim());
		String text = null;
		
		if (fin.exists() && fin.isFile()) {
			text = FileUtils.readFileToString(fin, "UTF-8");
		}
		
		return text;
	}
	
	private void check(String[] goldlines, JCas tcas) {
		final JFSIndexRepository indexes = tcas.getJFSIndexRepository();
		FSIterator<Annotation> tokIter = indexes.getAnnotationIndex(GeneralAttributeMention.type).iterator();
		List<String> actLines = new ArrayList<String>();
		
		Integer menCount = 0;
		while (tokIter.hasNext()) {
			GeneralAttributeMention tok = (GeneralAttributeMention) tokIter.next();
			actLines.add(tok.getCoveredText());
			System.out.println(tok.getCoveredText() + " -- " + tok.getTag());
			menCount++;
		}
		
		
		Boolean lengthEqual = (goldlines.length == menCount);
		assertTrue(lengthEqual,
				"Expression count differs; should be '" +
				goldlines.length + "' but is '" + menCount.toString() +"'.");
		Boolean arrayEqual = (goldlines.equals(actLines.toArray(new String[actLines.size()])));
		assertTrue(arrayEqual, "Expressions differ");
	}
	
	private void reset() {
		ajcas.reset();
	}
	
	
	@BeforeEach
	public void initializeComponents() throws IOException, UIMAException {
		if (setUpIsDone) {
	        return;
	    }
	    // do the setup
		attrExtractor = AnalysisEngineFactory.createEngine(AE_DESCRIPTOR);
		ajcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		
		setUpIsDone = true;
	}
	
	@Disabled
	@Test
	public void testDuration() {
			String text;
			try {
				text = loadTestExpression(TEST_FILES_ROOT + "duration");
				String[] lines = text.split("\\r?\\n");
				ajcas.setDocumentText(text);
				
				attrExtractor.process(ajcas);
				
				check(lines, ajcas);
				reset();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AnalysisEngineProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@Disabled
	@Test
	public void testDose() {
			String text;
			try {
				text = loadTestExpression(TEST_FILES_ROOT + "dose");
				String[] lines = text.split("\\r?\\n");
				ajcas.setDocumentText(text);
				
				attrExtractor.process(ajcas);
				
				check(lines, ajcas);
				reset();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AnalysisEngineProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@Disabled
	@Test
	public void testFrequency() {
			String text;
			try {
				text = loadTestExpression(TEST_FILES_ROOT + "frequency");
				String[] lines = text.split("\\r?\\n");
				ajcas.setDocumentText(text);
				
				attrExtractor.process(ajcas);
				
				check(lines, ajcas);
				reset();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AnalysisEngineProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@Disabled
	@Test
	public void testModus() {
			String text;
			try {
				text = loadTestExpression(TEST_FILES_ROOT + "modus");
				String[] lines = text.split("\\r?\\n");
				ajcas.setDocumentText(text);
				
				attrExtractor.process(ajcas);
				
				check(lines, ajcas);
				reset();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AnalysisEngineProcessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
