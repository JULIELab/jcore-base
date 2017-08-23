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
package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.reader.pmc.parser.ParsingResult.ResultType;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.pubmed.Header;

public class FrontParserTest {
	@Test
	public void testParser() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		documentParser.reset(new File("src/test/resources/documents-recursive/PMC2847692.nxml.gz"), cas);

		FrontParser frontMatterParser = new FrontParser(documentParser);
		frontMatterParser.moveToXPath("/article/front");
		ElementParsingResult frontResult = frontMatterParser.parse();
		assertNotNull(frontResult);

		Annotation annotation = frontResult.getAnnotation();
		assertTrue(annotation instanceof Header);
		Header header = (Header) annotation;
		assertEquals("2847692", header.getDocId());
		assertEquals("10.1007/s13280-009-0005-8", header.getDoi());
		assertNotNull(header.getOtherIDs());
		assertTrue(header.getOtherIDs().size() > 0);
		assertNotNull(header.getOtherIDs().get(0));

		assertNotNull(header.getPubTypeList());
		assertTrue(header.getPubTypeList().size() > 0);
		Journal journal = (Journal) header.getPubTypeList(0);
		assertEquals(journal.getPubDate().getDay(), 24);
		assertEquals(journal.getPubDate().getMonth(), 2);
		assertEquals(journal.getPubDate().getYear(), 2010);
		assertEquals("Ambio", journal.getTitle());

		ElementParsingResult titleResult = getElementResult("article-title", frontResult);
		assertNotNull(titleResult);
		ElementParsingResult abstractResult = getElementResult("abstract", frontResult);
		assertNotNull(abstractResult);

		assertNotNull(header.getAuthors());
	}

	@Test
	public void testParser2() throws Exception {
		// this publication does not define pages but an electronic location
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		documentParser.reset(new File("src/test/resources/documents-misc/PMC4393605.nxml.gz"), cas);

		FrontParser frontMatterParser = new FrontParser(documentParser);
		frontMatterParser.moveToXPath("/article/front");
		ElementParsingResult frontResult = frontMatterParser.parse();
		assertNotNull(frontResult);
		Header header = (Header) frontResult.getAnnotation();
		assertEquals("80", ((Journal) header.getPubTypeList(0)).getPages());
	}

	@Test
	public void testParser3() throws Exception {
		// this publication does not define the last page
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		documentParser.reset(new File("src/test/resources/documents-misc/PMC3997261.nxml.gz"), cas);

		FrontParser frontMatterParser = new FrontParser(documentParser);
		frontMatterParser.moveToXPath("/article/front");
		ElementParsingResult frontResult = frontMatterParser.parse();
		assertNotNull(frontResult);
	}

	@Test
	public void testParser4() throws Exception {
		// this publication's "day" element contains a line break
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		documentParser.reset(new File("src/test/resources/documents-misc/PMC3821097.nxml.gz"), cas);

		FrontParser frontMatterParser = new FrontParser(documentParser);
		frontMatterParser.moveToXPath("/article/front");
		ElementParsingResult frontResult = frontMatterParser.parse();
		assertNotNull(frontResult);
	}

	@Test
	public void testParser5() throws Exception {
		// this publication does not define any pages or an elocation-id nor an
		// epub date; thus we check that the ppub date is used instead
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		documentParser.reset(new File("src/test/resources/documents-misc/PMC4154068.nxml.gz"), cas);

		FrontParser frontMatterParser = new FrontParser(documentParser);
		frontMatterParser.moveToXPath("/article/front");
		ElementParsingResult frontResult = frontMatterParser.parse();
		assertNotNull(frontResult);
		Header header = (Header) frontResult.getAnnotation();
		assertEquals(7, ((Journal) header.getPubTypeList(0)).getPubDate().getMonth());
		assertEquals(2014, ((Journal) header.getPubTypeList(0)).getPubDate().getYear());
	}

	private ElementParsingResult getElementResult(String elementName, ElementParsingResult parse) {
		for (ParsingResult parsingResult : parse.getSubResults()) {
			if (parsingResult.getResultType() == ResultType.ELEMENT) {
				ElementParsingResult elementResult = (ElementParsingResult) parsingResult;
				if (elementResult.getElementName().equals(elementName)) {
					return elementResult;
				}
			}
		}
		return null;
	}
}
