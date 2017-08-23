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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

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

import com.google.common.collect.Sets;

import de.julielab.jcore.reader.bionlpformat.main.BioEventReader;
import de.julielab.jcore.types.pubmed.Header;

// This test's EventReaderTest.xml descriptor points to local directories of Ekaterina Buyko and as such, the test doesn't work this way. However it might, if the data is made available as proper test data.
@Ignore
public class EventReaderTest {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
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
