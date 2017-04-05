package de.julielab.jcore.reader.pmc.parser;

import org.apache.uima.jcas.tcas.Annotation;

import com.ximpleware.NavException;

import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.reader.pmc.parser.ParsingResult.ResultType;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.SectionTitle;
import de.julielab.jcore.types.Title;

public class SectionParser extends DefaultElementParser {

	private int depth = -1;

	public SectionParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "sec";
	}

	@Override
	protected void beforeParseElement() throws ElementParsingException {
		++depth;
	}

	@Override
	protected void afterParseElement() throws ElementParsingException {
		--depth;
	}

	@Override
	public void parseElement(ElementParsingResult parsingResult) throws ElementParsingException {
		try {
			String sectionId = getElementAttributes().get("id");
			
			super.parseElement(parsingResult);

			Title sectionHeading = null;
			for (ParsingResult subresult : parsingResult.getSubResults()) {
				if (subresult.getResultType() == ResultType.ELEMENT) {
					ElementParsingResult elementSubresult = (ElementParsingResult) subresult;
					if (elementSubresult.getAnnotation() instanceof SectionTitle) {
						sectionHeading = (Title) elementSubresult.getAnnotation();
						break;
					}
				}
			}
			Section section = (Section) parsingResult.getAnnotation();
			section.setComponentId(PMCReader.class.getName());
			section.setSectionHeading(sectionHeading);
			section.setDepth(depth);
			section.setSectionId(sectionId);
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
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
