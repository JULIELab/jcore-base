package de.julielab.jcore.reader.pmc.parser;

public class SectionParser extends NxmlElementParser {

	public SectionParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser, "sec");
	}
	
	@Override
	public ElementParsingResult parse() throws ElementParsingException  {
		return null;
	}


}
