package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.AuthorInfo;

public class ContribParserTest {
	@Test
	public void testParse() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents/PMC2847692.nxml.gz"), cas);

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
