package de.julielab.jcore.reader.pmc.parser;

import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.reader.pmc.parser.ParsingResult.ResultType;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Title;

public class SectionParser extends DefaultElementParser {

	public SectionParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "sec";
	}
	
	@Override
	public ElementParsingResult parse() throws ElementParsingException  {
		ElementParsingResult parsingResult = super.parse();
		
		Title sectionHeading = null;
		for (ParsingResult subresult : parsingResult.getSubResults()) {
			if (subresult.getResultType() == ResultType.ELEMENT) {
				ElementParsingResult elementSubresult = (ElementParsingResult) subresult;
				if (elementSubresult.getAnnotation() instanceof Title) {
					sectionHeading = (Title) elementSubresult.getAnnotation();
					break;
				}
			}
		}
		Section section = (Section) parsingResult.getAnnotation();
		section.setSectionHeading(sectionHeading);
		// TODO finish
		return parsingResult;
	}

	@Override
	protected Annotation getParsingResultAnnotation() {
		return new Section(nxmlDocumentParser.cas);
	}

	@Override
	protected void editResult(ElementParsingResult result) {
		result.setBlockElement(true);
	}
	
	


}
