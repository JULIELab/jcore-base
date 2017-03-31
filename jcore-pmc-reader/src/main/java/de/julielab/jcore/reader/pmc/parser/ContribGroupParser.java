package de.julielab.jcore.reader.pmc.parser;

public class ContribGroupParser extends NxmlElementParser {

	public ContribGroupParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "contrib-group";
	}

	@Override
	public ElementParsingResult parse() throws ElementParsingException, DocumentParsingException {
		checkCursorPosition();
		return null;
	}

}
