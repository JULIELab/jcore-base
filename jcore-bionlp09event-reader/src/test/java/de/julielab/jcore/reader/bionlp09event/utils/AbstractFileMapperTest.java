/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.reader.bionlp09event.utils;

import static org.junit.Assert.*;

import java.io.BufferedReader;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

import de.julielab.jcore.reader.bionlp09event.utils.AbstractFileMapper;
import de.julielab.jules.types.AbstractText;
import de.julielab.jules.types.Title;
import de.julielab.jules.types.pubmed.Header;
import static org.easymock.classextension.EasyMock.*;
public class AbstractFileMapperTest {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
	private JCas cas;
	private AbstractFileMapper abstractFileMapper;
	
	@Before
	public void setUp() throws Exception {
		CollectionReaderDescription readerDescription = (CollectionReaderDescription) UIMAFramework.getXMLParser().parseCollectionReaderDescription(new XMLInputSource(DESCRIPTOR_FILE));
		CollectionReader collectionReader = UIMAFramework.produceCollectionReader(readerDescription);
		cas = CasCreationUtils.createCas(collectionReader.getProcessingResourceMetaData()).getJCas();
		abstractFileMapper = new AbstractFileMapper();
	}
	
	@Test
	public void testMapAbstractFile() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn("title");
		expect(bufferedReader.readLine()).andReturn("text");
		replay(bufferedReader);
		
		abstractFileMapper.mapAbstractFile("123456", "123456.txt", bufferedReader, cas);
		verify(bufferedReader);
		
		assertEquals("title\ntext", cas.getDocumentText());
		Type titleType = cas.getTypeSystem().getType("de.julielab.jules.types.Title");
		assertNotNull(titleType);
		FSIterator titleIterator = cas.getAnnotationIndex(titleType).iterator();
		assertNotNull(titleIterator);
		Title title = (Title) titleIterator.next();
		assertNotNull(title);
		assertEquals(0, title.getBegin());
		assertEquals(5, title.getEnd());
		
		Type abstractType = cas.getTypeSystem().getType("de.julielab.jules.types.AbstractText");
		FSIterator abstractIterator = cas.getAnnotationIndex(abstractType).iterator();
		AbstractText abstractText = (AbstractText) abstractIterator.next();
		assertNotNull(abstractText);
		assertEquals(6, abstractText.getBegin());
		assertEquals(10, abstractText.getEnd());
		
		Type headerType = cas.getTypeSystem().getType("de.julielab.jules.types.pubmed.Header");
		Header header = (Header) cas.getAnnotationIndex(headerType).iterator().next();
		assertNotNull(header);
		assertEquals("123456", header.getDocId());
		assertEquals("123456.txt", header.getSource());
		
	}
}
