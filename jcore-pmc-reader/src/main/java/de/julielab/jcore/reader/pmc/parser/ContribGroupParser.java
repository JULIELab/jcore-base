package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class ContribGroupParser extends NxmlElementParser {

	public ContribGroupParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "contrib-group";
	}

	@Override
	protected void parseElement(ElementParsingResult result) throws ElementParsingException {
		try {
			AutoPilot ap = getAutoPilot("/article/front/article-meta/contrib-group/contrib[@contrib-type='author']", vn);
			while (ap.evalXPath() != -1) {
				result.addSubResult(nxmlDocumentParser.getParser("contrib").parse());
			}
			releaseAutoPilot();
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

}
