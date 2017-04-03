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
		documentParser.loadElementPropertyFile("/de/julielab/jcore/reader/pmc/resources/elementproperties.yml");
		ElementParsingResult result = documentParser.parse();
		System.out.println(result);
	}

	@Test
	public void testVtd() throws Exception {
		VTDGen vg = new VTDGen();
		vg.parseGZIPFile("src/test/resources/documents/PMC2847692.nxml.gz", false);
		VTDNav vn = vg.getNav();
		AutoPilot ap = new AutoPilot(vn);
		ap.selectXPath("//break");
		ap.evalXPath();
		System.out.println((int)vn.getOffsetAfterHead());
		System.out.println(vn.getTokenLength(vn.getCurrentIndex()));
		
	}
	
	@Test
	public void voidTestVtd2() throws Exception {
		String contrib = " <contrib contrib-type=\"author\" corresp=\"yes\">\n" + 
				"                    <name>\n" + 
				"                        <surname>Elofsson</surname>\n" + 
				"                        <given-names>Katarina</given-names>\n" + 
				"                    </name>\n" + 
				"                    <address>\n" + 
				"                        <email>katarina.elofsson@ekon.slu.se</email>\n" + 
				"                    </address>\n" + 
				"                    <xref ref-type=\"aff\" rid=\"Aff1\"/>\n" + 
				"                    <bio>\n" + 
				"                        <sec id=\"d29e148\">\n" + 
				"                            <title>Katarina Elofsson</title>\n" + 
				"                            <p>is assistant professor at the Swedish University of Agricultural Sciences. Her research is focused on management of water quality and of invasive species.</p>\n" + 
				"                        </sec>\n" + 
				"                    </bio>\n" + 
				"                </contrib>";
		VTDGen vg = new VTDGen();
		vg.setDoc(contrib.getBytes());
		vg.parse(false);
		VTDNav vn = vg.getNav();
		AutoPilot ap = new AutoPilot(vn);
		ap.selectXPath("/contrib[@contrib-type='author']/xref[@ref-type='aff']/@rid");
		System.out.println(ap.evalXPathToString());
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
