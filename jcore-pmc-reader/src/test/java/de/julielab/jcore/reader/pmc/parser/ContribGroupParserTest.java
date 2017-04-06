package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class ContribGroupParserTest {
	@Test
	public void testParser() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents-recursive/PMC2847692.nxml.gz"), cas);

		ContribGroupParser contribGroupParser = new ContribGroupParser(documentParser);
		contribGroupParser.moveToXPath("/article/front/article-meta/contrib-group");
		ElementParsingResult parse = contribGroupParser.parse();
		assertNotNull(parse);
		assertEquals(1, parse.getSubResults().size());
	}
}
