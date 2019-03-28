/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.reader.bionlp09event.main;

import com.google.common.collect.Sets;
import de.julielab.jcore.reader.bionlpformat.main.BioEventReader;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// This test's EventReaderTest.xml descriptor points to local directories of Ekaterina Buyko and as such, the test doesn't work this way. However it might, if the data is made available as proper test data.
@Ignore
public class EventReaderTest {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlpformat/desc/EventReaderTest.xml";
	private CollectionReader collectionReader;

	@Before
	public void setUp() throws Exception {
		CollectionReaderDescription readerDescription = (CollectionReaderDescription) UIMAFramework
				.getXMLParser().parseCollectionReaderDescription(
						new XMLInputSource(DESCRIPTOR_FILE));
		collectionReader = UIMAFramework
				.produceCollectionReader(readerDescription);

	}

	@Test
	public void testGetNext() throws Exception {
		CAS cas = CasCreationUtils.createCas(collectionReader
				.getProcessingResourceMetaData());
		JCas jcas = cas.getJCas();

		Type headerType = jcas.getTypeSystem().getType(
				"de.julielab.jcore.types.pubmed.Header");
		Type proteinType = jcas.getTypeSystem().getType(
				"de.julielab.jcore.types.Gene");
		Type eventType = jcas.getTypeSystem().getType(
				"de.julielab.jcore.types.EventMention");

		boolean bioEventServiceMode = (Boolean) collectionReader
				.getConfigParameterValue(BioEventReader.BIOEVENT_SERVICE_MODE_PARAM);

		if (bioEventServiceMode) {
			Set<String> fileNames = Sets.newHashSet("1493333.txt");
			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			Header header = (Header) jcas.getAnnotationIndex(headerType)
					.iterator().next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());

			cas.reset();
			jcas.reset();

		} else {

			Set<String> fileNames = Sets.newHashSet("10485906.txt",
					"1493333.txt", "7929104.txt", "8790376.txt", "9115366.txt",
					"9893043.txt");

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			Header header = (Header) jcas.getAnnotationIndex(headerType)
					.iterator().next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			header = (Header) jcas.getAnnotationIndex(headerType).iterator()
					.next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			header = (Header) jcas.getAnnotationIndex(headerType).iterator()
					.next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			header = (Header) jcas.getAnnotationIndex(headerType).iterator()
					.next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			header = (Header) jcas.getAnnotationIndex(headerType).iterator()
					.next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

			assertTrue(collectionReader.hasNext());
			collectionReader.getNext(cas);
			header = (Header) jcas.getAnnotationIndex(headerType).iterator()
					.next();
			assertTrue(fileNames.contains(header.getSource()));
			assertTrue(jcas.getAnnotationIndex(proteinType).iterator()
					.hasNext());
			assertTrue(jcas.getAnnotationIndex(eventType).iterator().hasNext());
			cas.reset();
			jcas.reset();

		}
		assertFalse(collectionReader.hasNext());
	}
}
