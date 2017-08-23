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
package de.julielab.jcore.reader.bionlp09event.utils;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

import de.julielab.jcore.reader.bionlpformat.utils.TextFileMapper;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;

public class AbstractFileMapperTest {

    private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
    private JCas cas;
    private TextFileMapper abstractFileMapper;

    @Before
    public void setUp() throws Exception {
        CollectionReaderDescription readerDescription = UIMAFramework.getXMLParser()
                .parseCollectionReaderDescription(new XMLInputSource(DESCRIPTOR_FILE));
        CollectionReader collectionReader = UIMAFramework.produceCollectionReader(readerDescription);
        cas = CasCreationUtils.createCas(collectionReader.getProcessingResourceMetaData()).getJCas();
        abstractFileMapper = new TextFileMapper();
    }

    @Test
    public void testMapAbstractFile() throws Exception {
        BufferedReader bufferedReader = createMock(BufferedReader.class);
        expect(bufferedReader.readLine()).andReturn("title");
        expect(bufferedReader.readLine()).andReturn("text");
        expect(bufferedReader.readLine()).andReturn(null);
        replay(bufferedReader);

        abstractFileMapper.mapAbstractFile("123456", "123456.txt", bufferedReader, cas);
        verify(bufferedReader);

        assertEquals("title\ntext", cas.getDocumentText());
        Type titleType = cas.getTypeSystem().getType("de.julielab.jcore.types.Title");
        assertNotNull(titleType);
        FSIterator titleIterator = cas.getAnnotationIndex(titleType).iterator();
        assertNotNull(titleIterator);
        Title title = (Title) titleIterator.next();
        assertNotNull(title);
        assertEquals(0, title.getBegin());
        assertEquals(5, title.getEnd());

        Type abstractType = cas.getTypeSystem().getType("de.julielab.jcore.types.AbstractText");
        FSIterator abstractIterator = cas.getAnnotationIndex(abstractType).iterator();
        AbstractText abstractText = (AbstractText) abstractIterator.next();
        assertNotNull(abstractText);
        assertEquals(6, abstractText.getBegin());
        assertEquals(10, abstractText.getEnd());

        Type headerType = cas.getTypeSystem().getType("de.julielab.jcore.types.pubmed.Header");
        Header header = (Header) cas.getAnnotationIndex(headerType).iterator().next();
        assertNotNull(header);
        assertEquals("123456", header.getDocId());
        assertEquals("123456.txt", header.getSource());

    }
}
