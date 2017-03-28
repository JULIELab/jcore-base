package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.NavException;

public class ParagraphParser extends NxmlElementParser {

	public ParagraphParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser, "p");
	}

	@Override
	public ElementParsingResult parse() throws ElementParsingException {
		try {
			int textNodeIndex = vn.getText();
			if (textNodeIndex != -1) {
				String text = vn.toString(textNodeIndex);
			}
			return null;
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

}
