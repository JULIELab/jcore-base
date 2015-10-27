/** 
 * DocumentTextParser.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: bernd
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 14.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.mapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

/**
 * Handels to parse the DocumentText
 * 
 * @author weigel
 */

public class DocumentTextHandler {
	Logger LOGGER = LoggerFactory.getLogger(DocumentTextHandler.class);
	private DocumentTextData docTextData;

	public DocumentTextHandler() {
		docTextData = new DocumentTextData();
	}

	public void addPartOfDocumentTextXPath(int id, String xpath) {
		this.docTextData.put(id, new PartOfDocument(id, xpath));

	}

	public PartOfDocument getPartOfDocumentText(int id) {
		return this.docTextData.get(id);
	}

	public DocumentTextData parseAndAddToCas(VTDNav vn, JCas jcas,
			byte[] identifier) throws VTDException {
		List<String> textPartList = new ArrayList<String>(
				this.docTextData.size());
		int offset = 0;
		for (int i = 0; i < docTextData.size(); i++) {
			PartOfDocument docTextPart = this.docTextData.get(i);
			if (docTextPart == null) {
				LOGGER.error("corrupted DocumentText Data in MappingFile! Not all Ids are set.");
				continue;
			}

			String textPartStr = getTextPart(vn, docTextPart, identifier);
			docTextPart.setText(textPartStr);

			if (textPartStr.length() > 0) {
				// Important: First compute offset, then add the new text part.
				// Otherwise, the new text part will be treated as a former text part
				// and there will be an offset increment although it is incorrect.
				if (i - 1 >= 0 && i - 1 < textPartList.size() && textPartList.get(i - 1).length() > 0)
					offset += 1;
				textPartList.add(textPartStr);
			}
			docTextPart.setBegin(offset);
			offset += textPartStr.length();
			docTextPart.setEnd(offset);
			
		}
		String docTextStr = StringUtils.join(textPartList, "\n");
		docTextData.setText(docTextStr);
		jcas.setDocumentText(docTextStr);
		return this.docTextData;
	}

	private String getTextPart(VTDNav vn, PartOfDocument part, byte[] identifier)
			throws XPathParseException, XPathEvalException, NavException {
		vn.cloneNav();
		AutoPilot ap = new AutoPilot(vn);
		String textPart = "";
		ap.selectXPath(part.getXPath());

		int i = ap.evalXPath();
//		if (i < 0)
//			LOGGER.warn("no match for xPath " + part.getXPath()
//					+ " in document with identifier " + new String(identifier));
		while (i != -1) {
			textPart = MapperUtils.getElementText(vn);
			i = ap.evalXPath();
		}
		return textPart;
	}

}
