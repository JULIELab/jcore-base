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

import com.ximpleware.NavException;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.types.AuthorInfo;

import java.util.Optional;

public class ContribParser extends NxmlElementParser {

	public ContribParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
		elementName = "contrib";
	}

	@Override
	protected void parseElement(ElementParsingResult result) throws ElementParsingException {
		try {

			Optional<String> givenNames = getXPathValue("name/given-names");
			Optional<String> surname = getXPathValue("name/surname");
			Optional<String> affRef = getXPathValue("xref[@ref-type='aff']/@rid");
			Optional<String> email = getXPathValue("address/email");
			Optional<String> initials = getXPathValue("name/given-names/@initials");

			AuthorInfo ai = new AuthorInfo(nxmlDocumentParser.cas);
			ai.setComponentId(PMCReader.class.getName());
			givenNames.ifPresent(ai::setForeName);
			surname.ifPresent(ai::setLastName);
			initials.ifPresent(ai::setInitials);
			affRef.ifPresent(ai::setAffiliation);
			email.ifPresent(ai::setContact);

			result.setAnnotation(ai);
		} catch (NavException | XPathParseException | XPathEvalException e) {
			throw new ElementParsingException(e);
		}
	}

}
