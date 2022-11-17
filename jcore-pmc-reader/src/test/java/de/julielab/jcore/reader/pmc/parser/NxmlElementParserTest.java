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
package de.julielab.jcore.reader.pmc.parser;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NxmlElementParserTest {
	@Test
	public void testGetElementPath() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents-recursive/PMC2847692.nxml.gz"), cas);
		
		DefaultElementParser parser = new DefaultElementParser(documentParser);
		
		String xpath = "/article";
		parser.moveToXPath(xpath);
		assertEquals(xpath, parser.getElementPath());
		
		xpath = "/article/front/article-meta/contrib-group";
		parser.moveToXPath(xpath);
		assertEquals(xpath, parser.getElementPath());
		
		xpath = "/article/body/sec/title";
		parser.moveToXPath(xpath);
		assertEquals(xpath, parser.getElementPath());
		
		xpath = "/article/front/journal-meta";
		parser.moveToXPath(xpath);
		assertEquals(xpath, parser.getElementPath());
	}
}
