package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class NxmlElementParserTest {
	@Test
	public void testGetElementPath() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(new File("src/test/resources/documents/PMC2847692.nxml.gz"), cas);
		
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
