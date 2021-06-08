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

import de.julielab.jcore.types.AuthorInfo;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ContribParserTest {
	@Test
	public void testParse() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents-recursive/PMC2847692.nxml.gz"), cas);

		ContribParser contribParser = new ContribParser(documentParser);
		contribParser.moveToXPath("/article/front/article-meta/contrib-group/contrib");
		ElementParsingResult contribResult = contribParser.parse();
		assertNotNull(contribResult);
		assertNotNull(contribResult.getAnnotation());
		assertTrue(contribResult.getAnnotation() instanceof AuthorInfo);
		AuthorInfo ai = (AuthorInfo) contribResult.getAnnotation();
		assertEquals("Elofsson", ai.getLastName());
		assertEquals("Katarina", ai.getForeName());
		assertEquals("katarina.elofsson@ekon.slu.se", ai.getContact());
		assertEquals("Aff1", ai.getAffiliation());
	}
}
