package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.PmcReaderException;

public abstract class NxmlElementParser {
	// the root element path to look for elements of name 'elementName' in
	protected String xPath;
	// the name of the XML element to parse
	protected String elementName;
	protected boolean recursive;
	protected VTDNav vn;
	private AutoPilot reusableAutoPilot;
	private boolean autoPilotInUse = false;

	public NxmlElementParser(VTDNav vn, String xPath, String elementName, boolean recursive) {
		this.vn = vn;
		this.xPath = xPath;
		this.elementName = elementName;
		this.recursive = recursive;
	}

	public abstract ParsingResult parse() throws PmcReaderException;

	protected AutoPilot getAutoPilot(String xpath, VTDNav vn) throws XPathParseException {
		assert !autoPilotInUse : "The reusable AutoPilot is in use and must be released before being used again.";
		if (reusableAutoPilot == null)
			reusableAutoPilot = new AutoPilot();
		reusableAutoPilot.resetXPath();
		reusableAutoPilot.bind(vn);
		reusableAutoPilot.selectXPath(xpath);
		return reusableAutoPilot;
	}
	
	protected void releaseAutoPilot() {
		autoPilotInUse = false;
	}
}
