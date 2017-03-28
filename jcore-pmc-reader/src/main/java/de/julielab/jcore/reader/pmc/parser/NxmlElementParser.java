package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathParseException;

public abstract class NxmlElementParser extends NxmlParser {
	/**
	 *  the name of the XML element to parse
	 */
	protected String elementName;
	private AutoPilot reusableAutoPilot;
	private boolean autoPilotInUse = false;
	protected NxmlDocumentParser nxmlDocumentParser;

	public NxmlElementParser(NxmlDocumentParser nxmlDocumentParser, String elementName) {
		this.nxmlDocumentParser = nxmlDocumentParser;
		this.elementName = elementName;
		this.vn = nxmlDocumentParser.getVn();
	}

	public abstract ElementParsingResult parse() throws ElementParsingException, DocumentParsingException;
	
	/**
	 * <p>
	 * This abstract class has one convenience AutoPilot object that can be
	 * obtained using this method. The returned AutoPilot will be reset to the
	 * given xPath relative to the given VTDNav.
	 * </p>
	 * <p>
	 * To make sure the AutoPilot is always at most used once, this method is
	 * only allowed to be called again after a call to
	 * {@link #releaseAutoPilot()}.
	 * </p>
	 * 
	 * @param xpath
	 *            The XPath that should be navigated to.
	 * @param vn
	 *            The VTDNav object the AutoPilot should be bound to.
	 * @return The reusable AutoPilot of this class.
	 * @throws XPathParseException
	 */
	protected AutoPilot getAutoPilot(String xpath, VTDNav vn) throws XPathParseException {
		assert !autoPilotInUse : "The reusable AutoPilot is in use and must be released before being used again.";
		if (reusableAutoPilot == null)
			reusableAutoPilot = new AutoPilot();
		reusableAutoPilot.resetXPath();
		reusableAutoPilot.bind(vn);
		reusableAutoPilot.selectXPath(xpath);
		return reusableAutoPilot;
	}

	/**
	 * Signals the end of use of the reusable AutoPilot in this class.
	 */
	protected void releaseAutoPilot() {
		autoPilotInUse = false;
	}
	
	/**
	 * Must be called when vn is positioned on the start tag of the element for which a result should be created.
	 * @return The parsing result for the current element.
	 * @throws NavException 
	 */
	protected ElementParsingResult createParsingResult() throws NavException {
		int begin = getElementStart();
		int end = getElementEnd();
		return new ElementParsingResult(elementName, begin, end);
	}
	
	protected int getElementStart() throws NavException {
		long elementFragment = vn.getElementFragment();
		int offset = (int) elementFragment;
		return offset;
	}
	
	protected int getElementEnd() throws NavException {
		long elementFragment = vn.getElementFragment();
		int offset = (int) elementFragment;
		int length = (int) (elementFragment >> 32);
		return offset + length;
	}
}
