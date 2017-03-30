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

	public NxmlElementParser(NxmlDocumentParser nxmlDocumentParser) {
		this.nxmlDocumentParser = nxmlDocumentParser;
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
	
	/**
	 * Throws an exception if the VTDNav cursor is not set to an element starting tag.
	 */
	protected void checkCursorPosition() {
		if (vn.getTokenType(vn.getCurrentIndex()) != VTDNav.TOKEN_STARTING_TAG)
			throw new IllegalStateException("VTDNav is positioned incorrectly. It must point to a starting tag.");
	}
	
	protected int skipElement() {
		int elementDepth = vn.getCurrentDepth();
		int i = vn.getCurrentIndex() + 1;
		while(i < vn.getTokenCount() && tokenIndexBelongsToElement(i, elementDepth)) {
			++i;
		}
		return i;
	}
	
	/**
	 * <p>
	 * Helper method to determine whether the current token index still lies
	 * within the element this parser has been called for. Since VTD-XML does
	 * unfortunately not use its VTDNav.TOKEN_ENDING_TAG token type, we have to
	 * infer from token type and token depth whether we have left the element or
	 * not.
	 * </p>
	 * <p>
	 * When an XML element ends, the next token may be another starting tag with
	 * the same depth as the original element (a sibling in the XML tree) or
	 * something else, e.g. text data, with a lower depth belonging to the
	 * parent element. Those two cases are checked in this method.
	 * </p>
	 * 
	 * @param index
	 *            The token index to check.
	 * @param elementDepth
	 *            The depth of the original element this parser is handling.
	 * @return True, if the token with the given index belongs to the element
	 *         for this parser.
	 */
	protected boolean tokenIndexBelongsToElement(int index, int elementDepth) {
		return !((vn.getTokenType(index) == VTDNav.TOKEN_STARTING_TAG && vn.getTokenDepth(index) <= elementDepth)
				|| vn.getTokenDepth(index) < elementDepth);
	}
}
