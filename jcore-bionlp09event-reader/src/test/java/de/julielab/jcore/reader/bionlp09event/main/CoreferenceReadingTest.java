/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

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

import de.julielab.jcore.reader.bionlp09event.main.EventReader;
import de.julielab.jules.types.Header;

// Ignore because the data path does generally not exist; a fix should only contain some test data, not the whole dataset
@Ignore
public class CoreferenceReadingTest {
	@Test
	public void testCoreferenceReading() throws UIMAException, IOException,
			SAXException {
		String baseDir = "/Users/faessler/Downloads/coref";
		CollectionReader reader = CollectionReaderFactory.createReader(
				EventReader.class, EventReader.BIOEVENT_SERVICE_MODE_PARAM,
				false, EventReader.DIRECTORY_PARAM, baseDir
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
