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
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ContribGroupParserTest {
	@Test
	public void testParser() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents-recursive/PMC2847692.nxml.gz"), cas);

		ContribGroupParser contribGroupParser = new ContribGroupParser(documentParser);
		contribGroupParser.moveToXPath("/article/front/article-meta/contrib-group");
		ElementParsingResult parse = contribGroupParser.parse();
		assertNotNull(parse);
		assertEquals(1, parse.getSubResults().size());
		assertNotNull(parse.getSubResults().get(0));
	}
}
