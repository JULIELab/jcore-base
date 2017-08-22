package de.julielab.jcore.reader.pmc.parser;

import java.util.Optional;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.types.Caption;
import de.julielab.jcore.types.Table;
import de.julielab.jcore.types.Title;

public class TableWrapParser extends NxmlElementParser {

	public TableWrapParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		this.elementName = "table-wrap";
	}

	@Override
	protected void parseElement(ElementParsingResult tableWrapResult) throws ElementParsingException {
		try {
			Optional<String> tableWrapId = getXPathValue("@id");
			Optional<ParsingResult> labelResult = parseXPath("label");
			Optional<String> labelString = getXPathValue("label");
			Optional<ParsingResult> captionResult = parseXPath("caption");

			captionResult.ifPresent(r -> {
				ElementParsingResult result = (ElementParsingResult) r;
				Caption caption = (Caption) result.getAnnotation();
				caption.setCaptionType("table");
				tableWrapResult.addSubResult(r);
			});
			labelResult.ifPresent(tableWrapResult::addSubResult);

			Table table = new Table(nxmlDocumentParser.cas);
			table.setComponentId(PMCReader.class.getName());
			labelResult.map(l -> ((ElementParsingResult) l).getAnnotation()).map(Title.class::cast).map(t -> {
				t.setTitleType("table");
				return t;
			}).ifPresent(table::setObjectTitle);
			labelString.ifPresent(table::setObjectLabel);
			captionResult.map(r -> (Caption) ((ElementParsingResult) r).getAnnotation())
					.ifPresent(table::setObjectCaption);
			tableWrapId.ifPresent(table::setObjectId);

			tableWrapResult.setAnnotation(table);
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

}
