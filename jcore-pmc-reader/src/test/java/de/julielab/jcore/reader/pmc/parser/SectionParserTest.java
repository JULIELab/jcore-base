package de.julielab.jcore.reader.pmc.parser;

import org.junit.Test;

import com.ximpleware.AutoPilot;
import com.ximpleware.TextIter;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathParseException;

import de.julielab.xml.JulieXMLTools;

public class SectionParserTest {
	@Test
	public void testParse() throws Exception {
		VTDGen vg = new VTDGen();
		vg.parseGZIPFile("src/test/resources/documents/PMC2847692.nxml.gz", false);
		SectionParser parser = new SectionParser(vg.getNav(), "/article/body/sec", true);
		parser.parse();
	}
}
