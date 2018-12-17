/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.pmc.parser;

import java.util.Optional;

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.types.Caption;
import de.julielab.jcore.types.Figure;
import de.julielab.jcore.types.Title;

public class FigParser extends NxmlElementParser {

	public FigParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		this.elementName = "fig";
	}

	@Override
	protected void parseElement(ElementParsingResult figResult) throws ElementParsingException {
		try {
			Optional<String> tableWrapId = getXPathValue("@id");
			Optional<ParsingResult> labelResult = parseXPath("label");
			Optional<String> labelString = getXPathValue("label");
			Optional<ParsingResult> captionResult = parseXPath("caption");

			captionResult.ifPresent(r -> {
				ElementParsingResult result = (ElementParsingResult) r;
				Caption caption = (Caption) result.getAnnotation();
				caption.setCaptionType("table");
				figResult.addSubResult(r);
			});
			labelResult.ifPresent(figResult::addSubResult);

			Figure figure = new Figure(nxmlDocumentParser.cas);
			figure.setComponentId(PMCReader.class.getName());
			labelResult.map(l -> ((ElementParsingResult) l).getAnnotation()).map(Title.class::cast).map(t -> {
				t.setTitleType("figure");
				return t;
			}).ifPresent(figure::setObjectTitle);
			labelString.ifPresent(figure::setObjectLabel);
			captionResult.map(r -> (Caption) ((ElementParsingResult) r).getAnnotation())
					.ifPresent(figure::setObjectCaption);
			tableWrapId.ifPresent(figure::setObjectId);

			figResult.setAnnotation(figure);
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

}
