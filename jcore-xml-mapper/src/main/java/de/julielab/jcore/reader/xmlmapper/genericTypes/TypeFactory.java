/** 
 * TypeFactory.java
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
 * Creation date: 02.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.genericTypes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.apache.uima.collection.CollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextHandler;

/**
 * Parses the Mapping File to a List of Feature- and Type Templates
 * 
 * @author Weigel
 */
public class TypeFactory {

	private static final String DEFAULT_VALUE_MAPPING = "default";
	private static final String ROOT = "mappings";
	private static final String DOCUMENT_TEXT = "documentText";
	private static final String PART_OF_DOCUMENT_TEXT = "partOfDocumentText";
	private static final String IS_TYPE = "isType";
	private static final String TS_FEATURE_NAME = "tsFeatureName";
	private static final String XML_ELEMENT = "xmlElement";
	private static final String FULL_CLASS_NAME = "tsFullClassName";
	private static final String TS_TYPE = "tsType";
	private static final String TS_FEATURE = "tsFeature";
	private static final String VALUE_X_PATH = "xPath";
	private static final String VALUE_MAPPING = "valueMapping";
	private static final String VALUE = "value";
	private static final String EXTERNAL_PARSER = "externalParser";
	private static final String ADDITIONAL_DATA = "additionalData";
	private static final String OFFSET = "offset";
	private static final String MULTI_INSTANCE = "multipleInstances";
	private static final String INLINE = "inline";
	private static final String ID = "id";
	Logger LOGGER = LoggerFactory.getLogger(TypeFactory.class);
	private List<TypeTemplate> types;
	private byte[] mappingFileData;
	private DocumentTextHandler documentTextParser;

	/**
	 * creates a new instance of the TypeFactory
	 * 
	 * @param mappingFile
	 */
	public TypeFactory(byte[] mappingFileData) {
		this.mappingFileData = mappingFileData;
		types = new ArrayList<TypeTemplate>();
		this.documentTextParser = new DocumentTextHandler();
	}

	/**
	 * *
	 * 
	 * @return a List of created TypeTemplates from the mapping File
	 * 
	 * @throws CollectionException
	 */
	// TODO throwing a CollectionException is no longer appropriate since the
	// component is no longer a CollcetionReader
	public List<TypeTemplate> createTemplates() throws CollectionException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		InputStream in = null;
		in = new ByteArrayInputStream(mappingFileData);

		XMLEventReader reader = null;
		try {
			reader = inputFactory.createXMLEventReader(in);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (reader.hasNext()) {
			XMLEvent event = null;
			try {
				event = reader.nextEvent();
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Check the root children: These are only allowed to be 'documentText' nodes or 'tsType' nodes.
			if (event.isStartElement()) {
				String nodeName = event.asStartElement().getName().toString();
				// tsType...
				if (nodeName.equals(TS_TYPE)) {
					TypeTemplate type = null;
					try {
						type = parseType(reader);
						Attribute inlineAttribute = event.asStartElement().getAttributeByName(new QName(INLINE));
						if (inlineAttribute != null) {
							type.setInlineAnnotation(Boolean.parseBoolean(inlineAttribute.getValue()));
						}
						Attribute multiAttribute = event.asStartElement().getAttributeByName(new QName(MULTI_INSTANCE));
						if (multiAttribute != null) {
							type.setMultipleInstances(Boolean.parseBoolean(multiAttribute.getValue()));
						}
						types.add(type);
					} catch (XMLStreamException e) {
						e.printStackTrace();
					}
					// or documentText?
				} else if (nodeName.equals(DOCUMENT_TEXT)) {
					try {
						this.fillDocumentParser(reader);
					} catch (XMLStreamException e) {
						e.printStackTrace();
					}
				} else {
					if (!nodeName.equals(ROOT)) {
						LOGGER.warn("unknown tag in mapping file: " + nodeName + "!!");
					}
				}
			}
		}
		return this.types;
	}

	@SuppressWarnings("unchecked")
	private void fillDocumentParser(XMLEventReader reader) throws XMLStreamException {
		this.documentTextParser = new DocumentTextHandler();
		XMLEvent event = reader.nextEvent();
		while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(DOCUMENT_TEXT))) {
			if (event.isStartElement()) {
				String nodeName = event.asStartElement().getName().toString();
				if (nodeName.equals(PART_OF_DOCUMENT_TEXT)) {
					Iterator<Attribute> attributes = event.asStartElement().getAttributes();
					int id = -1;
					if (attributes.hasNext()) {
						Attribute next = attributes.next();
						if (next.getName().getLocalPart().equals("id")) {
							id = Integer.parseInt(next.getValue());
						}
					} else {
						LOGGER.error("no id for " + PART_OF_DOCUMENT_TEXT);
						throw new RuntimeException();
					}
					event = reader.nextEvent();
					String xpath = "";
					if (event.isCharacters()) {
						xpath = (event.asCharacters()).getData();
					}
					if (xpath.length() > 0 && id >= 0) {
						documentTextParser.addPartOfDocumentTextXPath(id, xpath);
					} else {
						LOGGER.error("Unkownt data in " + DOCUMENT_TEXT + " tag ");
					}
				} else {
					LOGGER.error("Unknown element in mapping file: " + nodeName);
				}
			}
			event = reader.nextEvent();
		}

	}

	@SuppressWarnings("unchecked")
	private TypeTemplate parseType(XMLEventReader reader) throws XMLStreamException, CollectionException {
		TypeTemplate type = new TypeTemplate();
		XMLEvent event = reader.nextEvent();
		while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(TS_TYPE))) {
			if (event.isStartElement()) {
				String nodeName = event.asStartElement().getName().toString();
				if (nodeName.equals(TS_FEATURE)) {
					Attribute inlineAttribute = event.asStartElement().getAttributeByName(new QName(INLINE));
					boolean parseBoolean = false;
					if (inlineAttribute != null) {
						parseBoolean = Boolean.parseBoolean(inlineAttribute.getValue());
					}
					Attribute multiAttribute = event.asStartElement().getAttributeByName(new QName(MULTI_INSTANCE));
					boolean parseBoolean2 = false;
					if (multiAttribute != null) {
						parseBoolean2 = Boolean.parseBoolean(multiAttribute.getValue());
					}
					FeatureTemplate feature = parseFeature(reader);
					feature.setInlineAnnotation(parseBoolean);
					feature.setMultipleInstances(parseBoolean2);
					type.addFeature(feature);
				} else if (nodeName.equals(OFFSET)) {
					this.parseOffset(type, reader);
				} else if (nodeName.equals(FULL_CLASS_NAME)) {
					event = reader.nextEvent();
					type.setFullClassName(event.asCharacters().getData().trim());
				} else if (nodeName.equals(VALUE_X_PATH)) {
					event = reader.nextEvent();
					type.addXPath(event.asCharacters().getData().trim());
				} else if (nodeName.equals(EXTERNAL_PARSER)) {
					event = reader.nextEvent();
					type.setParser(event.asCharacters().getData().trim());
				} else if (nodeName.equals(ADDITIONAL_DATA)) {
					int index = -1;
					Iterator<Attribute> attributes = event.asStartElement().getAttributes();
					if (attributes.hasNext()) {
						Attribute next = attributes.next();
						if (next.getName().getLocalPart().equals("id")) {
							index = Integer.parseInt(next.getValue());
						}
					}
					if (index >= 0) {
						event = reader.nextEvent();
						type.addAdditionalData(event.asCharacters().getData().trim(), index);
					}
				} else {
					LOGGER.warn("unknown tag in mapping file: " + nodeName + "!!");
				}
			}
			event = reader.nextEvent();
		}
		// reflection type anlegen
		// iteration über alle features
		// if(feature.type==null)
		// über getter bestimmen
		return type;
	}

	private void parseOffset(TypeTemplate type, XMLEventReader reader) throws XMLStreamException {
		XMLEvent event = reader.nextEvent();
		while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(OFFSET))) {
			if (event.isStartElement()) {
				if (event.isStartElement()) {
					String nodeName = event.asStartElement().getName().toString();
					if (nodeName.equals(PART_OF_DOCUMENT_TEXT)) {
						while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(PART_OF_DOCUMENT_TEXT))) {
							event = reader.nextEvent();
							if (event.isStartElement() && event.asStartElement().getName().toString().equals(ID)) {
								event = reader.nextEvent();
								if (event.isCharacters()) {
									type.addPartOfDocumentText(Integer.valueOf(event.asCharacters().getData()));
								} else {
									LOGGER.error("corrupt offsetdata in mappingfile");
								}
							}
						}
					} else {
						LOGGER.error("Unknown element in mapping file: " + nodeName);
					}
				}
			}
			event = reader.nextEvent();
		}
	}

	private FeatureTemplate parseFeature(XMLEventReader reader) throws XMLStreamException, CollectionException {
		FeatureTemplate feature = new FeatureTemplate();
		XMLEvent event = reader.nextEvent();
		boolean externalParser = false;
		boolean isType = false;
		while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(TS_FEATURE))) {
			if (event.isStartElement()) {
				String nodeName = event.asStartElement().getName().toString();
				if (nodeName.equals(FULL_CLASS_NAME)) {
					event = reader.nextEvent();
					feature.setFullClassName(event.asCharacters().getData().trim());
				} else if (nodeName.equals(VALUE_X_PATH)) {
					event = reader.nextEvent();
					feature.addXPath(event.asCharacters().getData().trim());
				} else if (nodeName.equals(TS_FEATURE_NAME)) {
					event = reader.nextEvent();
					feature.setTsName(event.asCharacters().getData().trim());
				} else if (nodeName.equals(VALUE_MAPPING)) {
					event = reader.nextEvent();
					parseValueMapping(reader, feature);
				} else if (nodeName.equals(EXTERNAL_PARSER)) {
					externalParser = true;
					event = reader.nextEvent();
					feature.setParser(event.asCharacters().getData().trim());
				} else if (nodeName.equals(IS_TYPE)) {
					event = reader.nextEvent();
					isType = Boolean.parseBoolean(event.asCharacters().getData().trim());
					feature.setType(isType);
				} else if (nodeName.equals(TS_FEATURE)) {
					isType = true;
					feature.setType(isType);
					FeatureTemplate newFeature = parseFeature(reader);
					feature.addFeature(newFeature);
				} else {
					LOGGER.warn("unknown tag in mapping file: " + nodeName + "!!");
				}
			}
			event = reader.nextEvent();
		}
		if (!externalParser && !isType) {
			feature.setParser(null);
		}
		return feature;
	}

	private void parseValueMapping(XMLEventReader reader, FeatureTemplate feature) throws XMLStreamException {
		XMLEvent event = reader.nextEvent();
		String value = null;
		String xmlElement = null;
		while (!(event.isEndElement() && event.asEndElement().getName().toString().equals(VALUE_MAPPING))) {
			if (event.isStartElement()) {
				String nodeName = event.asStartElement().getName().toString();
				if (nodeName.equals(XML_ELEMENT)) {
					event = reader.nextEvent();
					xmlElement = event.asCharacters().getData().trim();
				} else if (nodeName.equals(VALUE)) {
					event = reader.nextEvent();
					value = event.asCharacters().getData().trim();
				} else if (nodeName.equals(DEFAULT_VALUE_MAPPING)) {
					event = reader.nextEvent();
					xmlElement = "defaultValueMapping";
					value = event.asCharacters().getData().trim();
				}
			}
			event = reader.nextEvent();
		}
		if (xmlElement != null) {
			if (value == null) {
				value = xmlElement;
			}
			feature.addVauleMapping(xmlElement, value);
		}
	}

	public DocumentTextHandler getDocumentTextParser() {
		return this.documentTextParser;
	}

}
