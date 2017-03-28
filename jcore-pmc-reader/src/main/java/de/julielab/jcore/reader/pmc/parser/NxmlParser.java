package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

public abstract class NxmlParser {
	protected VTDNav vn;
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
}
