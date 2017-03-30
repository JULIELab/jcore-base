package de.julielab.jcore.reader.pmc.parser;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class SectionParserTest {
	@Test
	public void testParse() throws Exception {
		File file = new File("src/test/resources/documents/PMC2847692.nxml.gz");
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		NxmlDocumentParser documentParser = new NxmlDocumentParser();
		documentParser.reset(file, cas);
		ElementParsingResult result = documentParser.parse();
		System.out.println(result);
	}

	@Test
	public void testVtd() throws Exception {
		VTDGen vg = new VTDGen();
		vg.parseGZIPFile("src/test/resources/documents/PMC2847692.nxml.gz", false);
		VTDNav vn = vg.getNav();
		System.out.println(vn.getCurrentIndex());
//		vn.push();
		AutoPilot ap = new AutoPilot(vn);
		ap.selectXPath("/article/front/article-meta/abstract");
		assertTrue(ap.evalXPath() != -1);
		System.out.println(vn.getCurrentIndex());
//		ap.resetXPath();
//		vn.pop();
		ap.selectXPath("/article/front/journal-meta/journal-title-group/journal-title");
		assertTrue(ap.evalXPath() != -1);
		System.out.println(vn.getCurrentIndex());
	}

	private void printText(VTDNav vn) throws NavException {
		System.out.println(vn.toString(vn.getText()));
	}

	private void printCurrentToken(VTDNav vn) throws NavException {
		System.out.println(vn.toString(vn.getCurrentIndex()));
	}

	@Test
	public void testGetDtd() throws Exception {
		int i = 1;
		for (; i < 10; ++i) {
			if (i == 2)
				break;
		}
		System.out.println(i);
	}

}
