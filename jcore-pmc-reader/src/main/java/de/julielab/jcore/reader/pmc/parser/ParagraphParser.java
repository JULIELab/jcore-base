package de.julielab.jcore.reader.pmc.parser;

import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.Paragraph;

public class ParagraphParser extends DefaultElementParser {

	public ParagraphParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
	}

	@Override
	protected Annotation getParsingResultAnnotation() {
		return new Paragraph(nxmlDocumentParser.cas);
	}

	

}
