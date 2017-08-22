package de.julielab.jcore.reader.pmc.parser;

import java.util.List;
import java.util.Optional;

import com.ximpleware.NavException;

import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.types.AbstractSection;
import de.julielab.jcore.types.AbstractSectionHeading;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.SectionTitle;

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
	protected void parseElement(ElementParsingResult parsingResult) throws ElementParsingException {
		try {
			String sectionId = getElementAttributes().get("id");
			String elementPath = getElementPath();

			super.parseElement(parsingResult);

			if (elementPath.contains("abstract")) {
				AbstractSectionHeading sectionHeading = null;
				List<AbstractSectionHeading> abstractSectionHeadings = parsingResult
						.getSubResultAnnotations(AbstractSectionHeading.class);
				if (!abstractSectionHeadings.isEmpty())
					sectionHeading = abstractSectionHeadings.get(0);
				// Do not create annotations for sections that do not include any text themselves
				if (parsingResult.getDirectResultText().trim().length() == 0 && abstractSectionHeadings.isEmpty()) {
					parsingResult.setAnnotation(null);
					return;
				}
				AbstractSection section = (AbstractSection) parsingResult.getAnnotation();
				section.setComponentId(PMCReader.class.getName());
				section.setAbstractSectionHeading(sectionHeading);
			} else {
				SectionTitle sectionHeading = null;
				List<SectionTitle> secTitleAnnotations = parsingResult.getSubResultAnnotations(SectionTitle.class);
				if (!secTitleAnnotations.isEmpty())
					sectionHeading = secTitleAnnotations.get(0);
				Section section = (Section) parsingResult.getAnnotation();
				section.setComponentId(PMCReader.class.getName());
				section.setSectionHeading(sectionHeading);
				section.setDepth(depth);
				section.setSectionId(sectionId);
				List<ParsingResult> label = parsingResult.getSubResults("label");
				if (!label.isEmpty()) {
					// there is only one label element
					ElementParsingResult labelParsingResult = (ElementParsingResult) label.get(0);
					section.setLabel(labelParsingResult.getResultText());
				}
			}
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

	@Override
	protected void editResult(ElementParsingResult result) {
		result.setBlockElement(true);
	}

}
