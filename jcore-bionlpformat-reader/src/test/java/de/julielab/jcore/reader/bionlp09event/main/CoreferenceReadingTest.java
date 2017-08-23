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
package de.julielab.jcore.reader.bionlp09event.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.julielab.jcore.reader.bionlpformat.main.BioEventReader;
import de.julielab.jcore.types.Header;

// Ignore because the data path does generally not exist; a fix should only contain some test data, not the whole dataset
@Ignore
public class CoreferenceReadingTest {
	@Test
	public void testCoreferenceReading() throws UIMAException, IOException,
			SAXException {
		String baseDir = "/Users/faessler/Downloads/coref";
		CollectionReader reader = CollectionReaderFactory.createReader(
				BioEventReader.class, BioEventReader.BIOEVENT_SERVICE_MODE_PARAM,
				false, BioEventReader.DIRECTORY_PARAM, baseDir
						+ "/BioNLP-ST_2011_coreference_training_data");

		JCas jcas = JCasFactory.createJCas("julie-all-types");

		while (reader.hasNext()) {
			reader.getNext(jcas.getCas());
			FSIterator<Annotation> iterator = jcas.getAnnotationIndex(
					Header.type).iterator();
			String docId = ((Header) iterator.next()).getDocId();
			try (OutputStream os = new FileOutputStream(baseDir
					+ "/training-xmi/" + docId)) {
				XmiCasSerializer.serialize(jcas.getCas(), os);
			}
			jcas.reset();
		}
	}
}
