package de.julielab.jcore.reader.pmc.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public abstract class NxmlElementParser extends NxmlParser {

	private static final Logger log = LoggerFactory.getLogger(NxmlElementParser.class);

	/**
	 * the name of the XML element to parse
	 */
	protected String elementName;
	protected NxmlDocumentParser nxmlDocumentParser;

	public NxmlElementParser(NxmlDocumentParser nxmlDocumentParser) {
		this.nxmlDocumentParser = nxmlDocumentParser;
		this.vn = nxmlDocumentParser.getVn();
	}

	public final ElementParsingResult parse() throws ElementParsingException {
		try {
			beforeParseElement();
			checkCursorPosition();
			int elementDepth = vn.getTokenDepth(vn.getCurrentIndex());
			int startElementIndex = vn.getCurrentIndex();

			ElementParsingResult elementParsingResult = createParsingResult();
			parseElement(elementParsingResult);

			if (vn.getCurrentIndex() < startElementIndex)
				vn.recoverNode(startElementIndex);

			if (vn.getTokenType(vn.getCurrentIndex()) != VTDNav.TOKEN_STARTING_TAG) {
				vn.toElement(VTDNav.PARENT);
				if (getElementEnd() > elementParsingResult.getEnd())
					throw new IllegalStateException("Parsed element \"" + elementName + "\" ends at byte "
							+ elementParsingResult.getEnd()
							+ " but VTDNav was positioned after parsing within an element which ends at "
							+ getElementEnd()
							+ ". Each element parser must finish within its element or at a starting tag immediately following the parser's element closing tag.");
			}

			int index = findIndexAfterElement(elementDepth, vn.getCurrentIndex());

			elementParsingResult.setLastTokenIndex(index);

			afterParseElement();
			return elementParsingResult;
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

	private int findIndexAfterElement(int elementDepth, int startIndex) {
		int index = vn.getCurrentIndex();
		int tokenType = vn.getTokenType(index);

		if (tokenType == VTDNav.TOKEN_STARTING_TAG)
			index = findIndexAfterStartingTag(index);
		while (index < vn.getTokenCount() && tokenIndexBelongsToElement(index, elementDepth))
			++index;
		return index;

	}

	private int findIndexAfterStartingTag(int startIndex) {
		int index = startIndex;
		int tokenType = vn.getTokenType(index);
		if (tokenType == VTDNav.TOKEN_STARTING_TAG)
			++index;
		// look for the first token that does not belong to the starting
		// tag
		while (tokenType == VTDNav.TOKEN_ATTR_NAME || tokenType == VTDNav.TOKEN_ATTR_VAL
				|| tokenType == VTDNav.TOKEN_ATTR_NS || tokenType == VTDNav.TOKEN_PI_NAME
				|| tokenType == VTDNav.TOKEN_PI_VAL)
			++index;
		return index;
	}

	protected void beforeParseElement() throws ElementParsingException {
	}

	protected void afterParseElement() throws ElementParsingException {
	}

	protected abstract void parseElement(ElementParsingResult elementParsingResult) throws ElementParsingException;

	

	/**
	 * Must be called when vn is positioned on the start tag of the element for
	 * which a result should be created.
	 * 
	 * @return The parsing result for the current element.
	 * @throws NavException
	 */
	private ElementParsingResult createParsingResult() throws NavException {
		int begin = getElementStart();
		int end = getElementEnd();
		ElementParsingResult result = new ElementParsingResult(elementName, begin, end);
		return result;
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
	 * Throws an exception if the VTDNav cursor is not set to an element
	 * starting tag.
	 * 
	 * @throws NavException
	 */
	private void checkCursorPosition() throws NavException {
		if (vn.getTokenType(vn.getCurrentIndex()) != VTDNav.TOKEN_STARTING_TAG)
			throw new IllegalStateException("VTDNav is positioned incorrectly. It must point to a starting tag.");
		if (!vn.toString(vn.getCurrentIndex()).equals(elementName))
			throw new IllegalStateException(
					"VTDNav is positioned incorrectly. It is expected to be positioned at the starting tag \""
							+ elementName + "\" but it is set to \"" + vn.toString(vn.getCurrentIndex()) + "\".");
	}

	/**
	 * Sets the VTDNav cursor the first position after the current element.
	 * 
	 * @return The new token index.
	 */
	protected int skipElement() {
		int elementDepth = vn.getCurrentDepth();
		int i = vn.getCurrentIndex() + 1;
		while (i < vn.getTokenCount() && tokenIndexBelongsToElement(i, elementDepth)) {
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

	/**
	 * Moves the VTDNav cursor to the given XPath.
	 * 
	 * @param xpath
	 *            The XPath to move to.
	 * @return True if the XPath was found, false otherwise.
	 * @throws XPathParseException
	 * @throws XPathEvalException
	 * @throws NavException
	 */
	protected boolean moveToXPath(String xpath) throws XPathParseException, XPathEvalException, NavException {
		AutoPilot ap = getAutoPilot(xpath, vn);
		int index = ap.evalXPath();
		releaseAutoPilot();

		return index != -1;
	}

	/**
	 * Moves the VTDNav to the given XPath, which has to point to an element
	 * (and not, for example, an attribute) and calls the registered parser for
	 * the target element.
	 * 
	 * @param xpath
	 *            The XPath to move to.
	 * @return The element parsing result of the element found at
	 *         <tt>xpath</tt>.
	 * @throws XPathParseException
	 * @throws XPathEvalException
	 * @throws NavException
	 * @throws ElementParsingException
	 */
	protected Optional<ParsingResult> parseXPath(String xpath)
			throws XPathParseException, XPathEvalException, NavException, ElementParsingException {
		try {
			vn.push();
			if (moveToXPath(xpath)) {
				return Optional.of(nxmlDocumentParser.getParser(vn.toString(vn.getCurrentIndex())).parse());
			}
			log.trace("XPath not found: {}", xpath);
			return Optional.empty();
		} finally {
			vn.pop();
		}
	}

	/**
	 * Returns the value of <tt>xpath</tt> as retrieved when calling
	 * {@link AutoPilot#evalXPathToString()}.
	 * 
	 * @param xpath
	 *            The XPath for which a value should be returned.
	 * @return The XPath value.
	 * @throws XPathParseException
	 * @throws XPathEvalException
	 * @throws NavException
	 */
	protected Optional<String> getXPathValue(String xpath)
			throws XPathParseException, XPathEvalException, NavException {
		try {
			vn.push();
			AutoPilot ap = getAutoPilot(xpath, vn);
			String xpathValue = ap.evalXPathToString().trim();
			if (xpathValue != null && xpathValue.length() > 0)
				return Optional.of(xpathValue);
			return Optional.empty();
		} finally {
			releaseAutoPilot();
			vn.pop();
		}
	}

	protected Optional<List<String>> getXPathValues(String xpath)
			throws XPathParseException, XPathEvalException, NavException {
		try {
			vn.push();
			List<String> values = new ArrayList<>();
			AutoPilot ap = getAutoPilot(xpath, vn);
			while (ap.evalXPath() != -1) {
				int text = vn.getText();
				if (text != -1)
					values.add(vn.toString(text));
			}
			if (!values.isEmpty())
				return Optional.of(values);
			return Optional.empty();
		} finally {
			releaseAutoPilot();
			vn.pop();
		}
	}

	/**
	 * This method returns an absolute XPath-like representation of the position
	 * of the current element. Only simple paths like
	 * /article/front/article-meta will be returned and it is required that the
	 * VTDNav is located at an element starting tag when calling this method.
	 * 
	 * @return The path of the current element.
	 * @throws NavException
	 */
	protected String getElementPath() throws NavException {
		try {
			assert vn.getTokenType(vn
					.getCurrentIndex()) == VTDNav.TOKEN_STARTING_TAG : "This method only works if the VTDNav is positioned at the beginning of an element.";
			List<String> pathItems = new ArrayList<>(vn.getCurrentDepth() + 1);
			vn.push();
			do {
				pathItems.add(vn.toString(vn.getCurrentIndex()));

				// in the test we append the restriction to starting tags
				// because in the current version of VTD, the parent of the root
				// element seems to the doctype, if present. To make this
				// algorithm more robust and not to rely and seemingly random
				// behavior of VTD, we stick to actual elements
			} while (vn.toElement(VTDNav.PARENT) && vn.getTokenType(vn.getCurrentIndex()) == VTDNav.TOKEN_STARTING_TAG);
			int pathLength = pathItems.size() - 1;
			return "/" + IntStream.rangeClosed(0, pathLength).mapToObj(i -> pathItems.get(pathLength - i))
					.collect(Collectors.joining("/"));
		} finally {
			vn.pop();
		}
	}

	/**
	 * Requires the VTDNav to be positioned at the beginning of the element
	 * opening tag. Extracts all attribute name-value pairs and returns them as
	 * a map.
	 * 
	 * @return A map of attribute name-value pairs for the current element.
	 * @throws NavException
	 */
	protected Map<String, String> getElementAttributes() throws NavException {
		Map<String, String> attributesOfElement = new HashMap<>();
		int i = vn.getCurrentIndex();
		// check if the current token position is the starting tag with the
		// correct name or, if we are positioned at an attribute name, the previous token; if none of these, we can't get
		// the attributes here
		if ((vn.getTokenType(i) == VTDNav.TOKEN_STARTING_TAG && !elementName.equals(vn.toString(i)))
				|| (vn.getTokenType(i) == VTDNav.TOKEN_ATTR_NAME && vn.getTokenType(i - 1) == VTDNav.TOKEN_STARTING_TAG && !elementName.equals(vn.toString(i - 1)))) {
			throw new IllegalStateException(
					"To create the element attribute map, the VTDNav cursor must be set to the starting tag or the first attribute name.");
		}
		long tagEndOffset = vn.getOffsetAfterHead();
		String attrName = null;
		String attrValue = null;
		while (vn.getTokenOffset(i) < tagEndOffset) {
			switch (vn.getTokenType(i)) {
			case VTDNav.TOKEN_ATTR_NAME:
				attrName = vn.toString(i);
				break;
			case VTDNav.TOKEN_ATTR_VAL:
				attrValue = vn.toString(i);
				attributesOfElement.put(attrName, attrValue);
				break;
			}
			++i;
		}
		return attributesOfElement;
	}
}
