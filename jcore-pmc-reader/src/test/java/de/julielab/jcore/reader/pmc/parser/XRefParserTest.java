package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;

import de.julielab.jcore.types.pubmed.InternalReference;

public class XRefParserTest {
	@Test
	public void testParser() throws Exception {
		// this publication does not define title for all sections
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		File inputFile = new File("src/test/resources/documents-misc/PMC4303521.nxml.gz");
		documentParser.reset(inputFile, cas);


		XRefParser xrefParser = new XRefParser(documentParser);
		VTDNav vn = documentParser.getVn();
		AutoPilot ap = new AutoPilot(vn);
		ap.selectXPath("//xref");
		while (ap.evalXPath() != -1) {
			ElementParsingResult xrefResult = xrefParser.parse();
			assertNotNull(xrefResult);
			Annotation annotation = xrefResult.getAnnotation();
			assertNotNull(annotation);
			assertEquals(InternalReference.class, annotation.getClass());
			InternalReference ref = (InternalReference) annotation;
			assertNotNull(ref.getRefid());
			assertNotNull(ref.getReftype());
		}
	}
}
