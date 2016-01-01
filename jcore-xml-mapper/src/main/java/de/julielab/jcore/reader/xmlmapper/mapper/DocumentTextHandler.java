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

import static org.fest.reflect.core.Reflection.constructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.xmlmapper.typeParser.TypeParser;

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

	public void addPartOfDocumentTextXPath(int id) {
		this.docTextData.put(id, new PartOfDocument(id));

	}

	public DocumentTextData parseAndAddToCas(VTDNav vn, JCas jcas, byte[] identifier) throws VTDException {
		List<String> textPartList = new ArrayList<String>(this.docTextData.size());
		int offset = 0;
		for (int i = 0; i < docTextData.size(); i++) {
			PartOfDocument docTextPart = this.docTextData.get(i);
			if (docTextPart == null) {
				LOGGER.error("corrupted DocumentText Data in MappingFile! Not all Ids are set.");
				continue;
			}

			int[] beginOffsets;
			int[] endOffsets;
			List<String> textPartStrs;
			if (docTextPart.getParser() == null)
				textPartStrs = getTextPart(vn, docTextPart, identifier);
			else
				textPartStrs = docTextPart.getParser().parseDocumentPart(vn, docTextPart, textPartList.isEmpty() ? offset : offset + 1, jcas, identifier);
			docTextPart.setText(textPartStrs.toArray(new String[textPartStrs.size()]));
			beginOffsets = new int[textPartStrs.size()];
			endOffsets = new int[textPartStrs.size()];
			for (int j = 0; j < textPartStrs.size(); ++j) {
				String textPartStr = textPartStrs.get(j);
				if (textPartStr.length() > 0) {
					// accommodate for the line break after each text part
					// inserted at the end of the method
					if (!textPartList.isEmpty())
						++offset;
					textPartList.add(textPartStr);
					beginOffsets[j] = offset;
					offset += textPartStr.length();
					endOffsets[j] = offset;
				}
			}
			// in case the text part was empty, we need to set the offsets to
			// the current offsets, begin and end equal (part has empty length)
			if (textPartStrs.isEmpty())
				beginOffsets = endOffsets = new int[] { offset };
			docTextPart.setBeginOffsets(beginOffsets);
			// offset += textPartStr.length();
			docTextPart.setEndOffsets(endOffsets);

		}
		String docTextStr = StringUtils.join(textPartList, "\n");
		docTextData.setText(docTextStr);
		jcas.setDocumentText(docTextStr);
		return this.docTextData;
	}

	private List<String> getTextPart(VTDNav vn, PartOfDocument part, byte[] identifier) throws XPathParseException, XPathEvalException, NavException {
		List<String> textParts = new ArrayList<>();
		vn.cloneNav();
		AutoPilot ap = new AutoPilot(vn);
		String textPart;
		if (StringUtils.isBlank(part.getXPath()))
			throw new IllegalStateException("Document text part with ID " + part.getId() + " has no XPath specified.");
		ap.selectXPath(part.getXPath());

		int i = ap.evalXPath();
		// if (i < 0)
		// LOGGER.warn("no match for xPath " + part.getXPath()
		// + " in document with identifier " + new String(identifier));
		while (i != -1) {
			textPart = MapperUtils.getElementText(vn).trim();
			textParts.add(textPart);
			i = ap.evalXPath();
		}
		return textParts;
	}

	public void setXPathForPartOfDocumentText(int id, String xpath) {
		docTextData.get(id).setxPath(xpath);
	}

	public void setExternalParserForPartOfDocument(int id, String externalParserClassName) throws CollectionException {
		if (externalParserClassName != null) {
			Class<?> externalParserClass;
			try {
				externalParserClass = Class.forName(externalParserClassName.trim());
			} catch (ClassNotFoundException e) {
				LOGGER.error("ExternalParser " + externalParserClassName + " for document text part " + id + " returns a ClassNotFoundException", e);
				throw new CollectionException(e);
			}
			DocumentTextPartParser parser = (DocumentTextPartParser) constructor().in(externalParserClass).newInstance();
			this.docTextData.get(id).setParser(parser);
		}
	}

}
