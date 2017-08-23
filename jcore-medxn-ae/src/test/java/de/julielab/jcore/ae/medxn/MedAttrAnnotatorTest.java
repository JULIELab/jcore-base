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
package de.julielab.jcore.ae.medxn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

import de.julielab.jcore.types.medical.GeneralAttributeMention;

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
		Assert.assertTrue("Expression count differs; should be '" + 
				Integer.toString(goldlines.length) + "' but is '" + menCount.toString() +"'.",
				lengthEqual);
		Boolean arrayEqual = (goldlines.equals(actLines.toArray(new String[actLines.size()])));
		Assert.assertTrue("Expressions differ", arrayEqual);
	}
	
	private void reset() {
		ajcas.reset();
	}
	
	
	@Before
	public void initializeComponents() throws IOException, UIMAException {
		if (setUpIsDone) {
	        return;
	    }
	    // do the setup
		attrExtractor = AnalysisEngineFactory.createEngine(AE_DESCRIPTOR);
		ajcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		
		setUpIsDone = true;
	}
	
	@Ignore
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
	
	@Ignore
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
	
	@Ignore
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
	
	@Ignore
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
