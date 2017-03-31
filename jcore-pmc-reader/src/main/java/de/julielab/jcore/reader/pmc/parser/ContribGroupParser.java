package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class ContribGroupParser extends NxmlElementParser {

	public ContribGroupParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "contrib-group";
	}

	@Override
	public ElementParsingResult parse() throws ElementParsingException {
		try {
			checkCursorPosition();
			vn.push();
			ElementParsingResult result = createParsingResult();

			AutoPilot ap = getAutoPilot("/article/front/article-meta/contrib-group/contrib[@contrib-type='author']", vn);
			while (ap.evalXPath() != -1) {
				result.addSubResult(nxmlDocumentParser.getParser("contrib").parse());
			}
			releaseAutoPilot();
			
			vn.pop();
			vn.toElement(VTDNav.NEXT_SIBLING);
			result.setLastTokenIndex(vn.getCurrentIndex());
			return result;
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

}
