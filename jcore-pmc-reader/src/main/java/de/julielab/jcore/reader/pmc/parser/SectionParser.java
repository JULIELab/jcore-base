package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.PmcReaderException;

public class SectionParser extends NxmlElementParser {

	public SectionParser(VTDNav vn, String xPath, boolean recursive) {
		super(vn, xPath, "sec", recursive);
	}
	
	@Override
	public ParsingResult parse() throws PmcReaderException  {
		try {
			AutoPilot ap = getAutoPilot(xPath, vn);
			while (ap.evalXPath() != -1) {
				String sectionTitle = getSectionTitle(vn);
			}
		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new PmcReaderException(e);
		}
		
		// TODO Auto-generated method stub
		return null;
	}

	private String getSectionTitle(VTDNav vn) throws XPathParseException {
		AutoPilot ap = new AutoPilot(vn);
		ap.selectXPath("title");
		return ap.evalXPathToString();
	}

}
