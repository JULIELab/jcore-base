package de.julielab.jcore.reader.pmc.parser;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import de.julielab.jcore.types.pubmed.InternalReference;

import java.util.Optional;



public class XRefParser extends DefaultElementParser {

	public XRefParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "xref";
	}

	@Override
	protected void parseElement(ElementParsingResult result) throws ElementParsingException {
		try {
			Optional<String> refType = getXPathValue("@ref-type");
			Optional<String> elementId = getXPathValue("@id");
			Optional<String> referencedId = getXPathValue("@rid");

			super.parseElement(result);
			
			InternalReference reference = new InternalReference(nxmlDocumentParser.cas);
			refType.ifPresent(t -> reference.setReftype(getJCoReReferenceType(t)));
			elementId.ifPresent(reference::setId);
			referencedId.ifPresent(reference::setRefid);

			result.setAnnotation(reference);
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

	private String getJCoReReferenceType(String pmcType) {
		switch (pmcType) {
		case "aff":
			return "affiliation";
		case "app":
			return "appendix";
		case "author-notes":
			return "authornotes";
		case "bibr":
			return "bibliography";
		case "boxed-text":
			return "textbox";
		case "chem":
			return "chemical";
		case "contrib":
			return "contributor";
		case "corresp":
			return "correspondingauthor";
		case "disp-formula":
			return "displayformula";
		case "fig":
			return "figure";
		case "fn":
			return "footnote";
		case "kwd":
			return "keyword";
		case "list":
			return "list";
		case "other":
			return "other";
		case "plate":
			return "plate";
		case "scheme":
			return "scheme";
		case "sec":
			return "section";
		case "statement":
			return "statement";
		case "supplementary-material":
			return "supplementary";
		case "table":
			return "table";
		case "table-fn":
			return "tablefootnote";
		default:
			return "other";
		}
	}

}
