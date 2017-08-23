/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
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
