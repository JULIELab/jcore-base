package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public abstract class NxmlParser {
	protected VTDNav vn;
	private AutoPilot reusableAutoPilot;
	private boolean autoPilotInUse = false;

	protected String moveToNextStartingTag() throws DocumentParsingException {
		try {
			int i = vn.getCurrentIndex();
			int tokenType = vn.getTokenType(i);
			while (tokenType != VTDNav.TOKEN_STARTING_TAG && i < vn.getTokenCount())
				++i;
			vn.recoverNode(i);
			return vn.toString(vn.getCurrentIndex());
		} catch (NavException e) {
			throw new DocumentParsingException(e);
		}
	}

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
		reusableAutoPilot.bind(vn);
		reusableAutoPilot.selectXPath(xpath);
		return reusableAutoPilot;
	}

	/**
	 * Signals the end of use of the reusable AutoPilot in this class.
	 */
	protected void releaseAutoPilot() {
		reusableAutoPilot.resetXPath();
		autoPilotInUse = false;
	}
	
	protected boolean xPathExists(String xpath) throws XPathParseException, XPathEvalException, NavException {
		try {
			AutoPilot ap = getAutoPilot(xpath, vn);
			return ap.evalXPath() != -1;
		} finally {
			releaseAutoPilot();
		}
	}
}
