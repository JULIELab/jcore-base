package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.PmcReaderException;

public class ParagraphParser extends NxmlElementParser {

	public ParagraphParser(VTDNav vn, String xPath, String elementName, boolean recursive) {
		super(vn, xPath, "p", recursive);
	}

	@Override
	public ParsingResult parse() throws PmcReaderException {
		try {
			AutoPilot ap = getAutoPilot(xPath, vn);
			while (ap.evalXPath() != -1) {
				
			}
		} catch (XPathParseException | XPathEvalException | NavException e) {
			throw new PmcReaderException(e);
		}
		return null;
	}

}
