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
package de.julielab.jcore.ae.biosem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Test;

import de.julielab.jcore.ae.biosem.BioSemEventAnnotator;
import de.julielab.jcore.ae.biosem.DBUtilsProviderImpl;
import de.julielab.jcore.consumer.bionlp09event.main.EventConsumer;
import de.julielab.jcore.reader.bionlpformat.main.BioEventReader;
import de.julielab.jcore.types.EventMention;

public class BioSemEventAnnotatorTest {
	@Test
	public void testProcess() throws Exception {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader bioNlpSTReader = CollectionReaderFactory.createReader(BioEventReader.class,
				BioEventReader.DIRECTORY_PARAM, "src/test/resources/st09-traindoc/",
				BioEventReader.BIOEVENT_SERVICE_MODE_PARAM, false);
		ExternalResourceDescription dbResourceDescription = ExternalResourceFactory.createExternalResourceDescription(
				DBUtilsProviderImpl.class,
				"file:src/test/resources/de/julielab/jcore/ae/biosemannotator.test.properties");
		AnalysisEngine engine = AnalysisEngineFactory.createEngine(BioSemEventAnnotator.class,
				BioSemEventAnnotator.RESOURCE_TRAINED_DB, dbResourceDescription);
		AnalysisEngine bioNlpSTWriter = AnalysisEngineFactory.createEngine(EventConsumer.class,
				EventConsumer.DIRECTORY_PARAM, "src/test/resources/test-predict-out",
				EventConsumer.BIOEVENT_SERVICE_MODE_PARAM, false);

		// first, delete the possibly already existing old test output file
		File testOutputFile = new File("src/test/resources/test-predict-out/1313226.a2");
		if (testOutputFile.exists())
			testOutputFile.delete();

		assertTrue("Test document was not found by the BioNLP ST reader.", bioNlpSTReader.hasNext());
		bioNlpSTReader.getNext(jCas.getCas());
		engine.process(jCas);
		bioNlpSTWriter.process(jCas);

		// now load the predicted file we created by hand (seen as 'correct' in
		// this context, although there actually might be tagging errors in it)
		// and the file we just created and compare them
		List<String> expectedLines = IOUtils
				.readLines(new FileInputStream("src/test/resources/st09-predicted/1313226.a2"));
		List<String> actualLines = IOUtils
				.readLines(new FileInputStream("src/test/resources/test-predict-out/1313226.a2"));
		// sort the lines because the order varies from run to run since the
		// results are internally stored in a set
		Collections.sort(expectedLines);
		Collections.sort(actualLines);

		assertEquals(expectedLines, actualLines);
	}

	@Test
	public void testStackoverflow() throws Exception {
		// The document in this test used to cause a StackOverflowError
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		XmiCasDeserializer.deserialize(new FileInputStream("src/test/resources/test-xmi/4401723.xmi"), jCas.getCas());
		ExternalResourceDescription dbResourceDescription = ExternalResourceFactory.createExternalResourceDescription(
				DBUtilsProviderImpl.class,
				"file:src/test/resources/de/julielab/jcore/ae/biosemannotator.test.properties");
		AnalysisEngine engine = AnalysisEngineFactory.createEngine(BioSemEventAnnotator.class,
				BioSemEventAnnotator.RESOURCE_TRAINED_DB, dbResourceDescription);
		engine.process(jCas);
		// there are no events to find in this paper (graph matching algorithm)
		// we just wanted to process it without an exception
	}
}
