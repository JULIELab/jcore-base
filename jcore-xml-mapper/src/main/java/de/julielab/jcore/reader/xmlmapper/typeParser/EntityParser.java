/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.reader.xmlmapper.typeParser;

/**
 * EntityParser.java
 *
 * Copyright (c) 2010, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 *
 * Author: faessler
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 22.03.2010
 **/

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.MedlineTextSentenceBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;
import de.julielab.jcore.types.EntityMention;
import de.julielab.xml.JulieXMLTools;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.julielab.xml.JulieXMLConstants.*;

/**
 * A special XML mapper parser capable of parsing the IeXML in-line XML
 * annotations used in the CalBc and Mantra projects.
 * 
 * @author faessler
 */
public class EntityParser implements TypeParser {

	private Logger LOGGER = LoggerFactory.getLogger(EntityParser.class);

	public TypeBuilder getTypeBuilder() {
		return new MedlineTextSentenceBuilder();
	}

	public void parseType(ConcreteType concreteType, VTDNav nav, JCas jcas,
			byte[] identifier, DocumentTextData docText) throws Exception {
		try {
			String text = docText.getText();
			int kontextPosition = 0;
			int entityPosition = 0;
			int lastPostTextPosition = -1;
			int lastEntityStartIndex = -1;
			for (String xPath : concreteType.getTypeTemplate().getXPaths()) {
				// Build a list of fields. The assembly of these fields can be
				// interpreted as a single record describing one object or like
				// a row in a database.
				// In this case, we traverse all <e> elements (given by xPath
				// which is used for constructRowIterator below).
				// For each of these elements, we want to know:
				// entity mention string | entity mention id | which <w>
				// elements (words) occur in the current entity | which IDs do
				// these words have
				// This information is extracted for each <e> element, pointed
				// to by the XPath "." in the first field definition. The name
				// of this field
				// is "entity". As the entity mention string can contain other
				// elements - namely <w> elements - we set MIXED_CONTENT to
				// true. This way,
				// all the text of the element, including nested element text,
				// is extracted.
				// Likewise, we get for each entity its ID in the second field,
				// just by storing the contents of the XPath "@id" (e.g. <e
				// id="5"> would result in the "5" to be extracted) in the field
				// named "entityId".
				// In the end, we provide the method "constructRowIterator" with
				// these field definition. What we get in return is an
				// Iterator<Map<String, Object>>. Each map represents one row.
				// It maps each field's name to its particular value in the
				// current row. Thus, if we want to know which entity mention
				// string was extracted for the current entity, we can get this
				// information by map.get("entity") because we have given this
				// field name in the definition.
				List<Map<String, String>> fields = new ArrayList<Map<String, String>>();

				Map<String, String> field = new HashMap<String, String>();
				field.put(NAME, "entity");
				field.put(XPATH, ".");
				fields.add(field);

				field = new HashMap<String, String>();
				field.put(NAME, "entityId");
				field.put(XPATH, "@id");
				fields.add(field);

				field = new HashMap<String, String>();
				field.put(NAME, "words");
				field.put(FOR_EACH, "w");
				field.put(XPATH, ".");
				field.put(RETURN_ARRAY, "true");
				fields.add(field);

				field = new HashMap<String, String>();
				field.put(NAME, "wordIds");
				field.put(FOR_EACH, "w");
				field.put(XPATH, "@id");
				field.put(RETURN_ARRAY, "true");
				fields.add(field);

				Iterator<Map<String, Object>> entityIt = JulieXMLTools
						.constructRowIterator(nav.cloneNav(), xPath, fields,
								new String(identifier));
				nav = nav.cloneNav();
				AutoPilot pilot = new AutoPilot(nav);
				pilot.selectXPath(xPath);

				while (entityIt.hasNext()) {
					// System.out.println("----------------");
					Map<String, Object> entityRow = entityIt.next();
					String entityText = (String) entityRow.get("entity");
					String[] words = (String[]) entityRow.get("words");
					String[] wordIds = (String[]) entityRow.get("wordIds");
					String entityIdStr = (String) entityRow.get("entityId");

					// additional xpath evaluation to get the prefix of the
					// entity
					// TODO increase performance by transfering pre and post
					// text to the Utils
					pilot.evalXPath();

					// get text before the entity
					String preText = "";
					int preIndex = nav.getCurrentIndex() - 1;
					if (preIndex < 0)
						preIndex = 0;
					if (nav.getTokenType(preIndex) == VTDNav.TOKEN_CHARACTER_DATA) {
						preText = nav.toString(preIndex);
					}
					// System.out.println("x"+lastEntityStartIndex);

					nav.toElement(VTDNav.FIRST_CHILD);
					// System.out.println("act\t" +
					// nav.toString(nav.getText()));
					int i = nav.getCurrentIndex();
					boolean entityStringSkipped = false;
					boolean tokenTypeIsCharData = nav.getTokenType(i) == VTDNav.TOKEN_CHARACTER_DATA;
					boolean postTextAvailable = true;
					while (!tokenTypeIsCharData || !entityStringSkipped) {
						i++;
						// first char_data is the char data of the entity not
						// the post string
						if (tokenTypeIsCharData)
							entityStringSkipped = true;
						// if there is another entity right after this one, no
						// post text is used
						if (entityStringSkipped
								&& nav.getTokenType(i) == VTDNav.TOKEN_STARTING_TAG
								&& "e".equals(nav.toString(i))) {
							postTextAvailable = false;
							break;
						}

						tokenTypeIsCharData = nav.getTokenType(i) == VTDNav.TOKEN_CHARACTER_DATA;
						// System.out.println(i+"\t"+nav.getTokenType(i)+"\t"+nav.toString(i));
					}

					String postText = "";
					if (postTextAvailable) {
						postText = nav.toString(i);
						// System.out.println("POST\t"+i+"\t" + postText);
					}

					// check where the complete context starts
					String kontext = preText + entityText + postText;

					kontextPosition = text.indexOf(kontext, kontextPosition);
					entityPosition = Math.max(kontextPosition, entityPosition);

					// if there is text right before the first entity or the
					// pretext iffers from the last posttext
					if (preIndex != lastPostTextPosition
							&& preText.length() > 0
							&& preIndex != lastEntityStartIndex) {
						// System.out.println("pre\t" + nav.toString(preIndex));
						entityPosition += preText.length();
					}

					lastEntityStartIndex = nav.getText();
					if (postTextAvailable) {
						lastPostTextPosition = i;
					}
					int entityPosStart = text.indexOf(entityText,
							entityPosition);

					// System.out.println(text + " " + entityText + " ab " +
					// entityPosition);
					int entityPosEnd = entityPosStart + entityText.length();
					// we need the exact word offsets for an entity for cases
					// like this:
					// <e id="MeSH:D004260:T045:0,1|::::ched:0"><w
					// id="0">DNA</w> <w id="1">repair</w></e>
					// Note: For the CalBc project, only our group seems to use
					// the format in which word
					// elements are referenced.
					HashMap<String, String> wordMap = null;
					if (words != null)
						wordMap = buildWordMap(words, wordIds, entityPosition,
								text);

					if (entityPosStart >= 0) {
						if (entityIdStr != null) {
							String[] entityIds = entityIdStr.split("\\|");

							Pattern wordPosP = Pattern.compile(":[0-9,]+$");
							for (String entityId : entityIds) {
								Matcher wordPosM = wordPosP.matcher(entityId);
								if (entityId.split(":").length >= 3
										&& wordPosM.find()) {
									String match = wordPosM.group();
									String[] wordIdsForEntity = match
											.substring(1).split(",");
									int start = Integer.parseInt(wordMap.get(
											wordIdsForEntity[0]).split("-")[0]);
									int end = Integer
											.parseInt(wordMap
													.get(wordIdsForEntity[wordIdsForEntity.length - 1])
													.split("-")[1]);
									// TODO maybe remove last part if there are
									// word elements
									// String specType =
									// entityId.replaceFirst(":[0-9,]+", "");
									String specType = entityId;
									addEntity(jcas, start, end, specType);
									// System.out.println("entityId: " +
									// entityId
									// + ", text: " + entityText
									// + ", start: " + start + ", end: "
									// + end);
								} else {
									addEntity(jcas, entityPosStart,
											entityPosEnd, entityId);
									// System.out.println("Else!" + "entityId: "
									// + entityId + ", text: "
									// + entityText + ", start: "
									// + entityPosStart + ", end: "
									// + entityPosEnd);
								}
							}
						} else {
							LOGGER.warn("entity[begin="
									+ entityPosStart
									+ ",end="
									+ entityPosEnd
									+ "] without id attribute found, setting idText to null");
							addEntity(jcas, entityPosStart, entityPosEnd,
									"null");
						}

						entityPosition = entityPosEnd;
						if (postTextAvailable) {
							entityPosition += postText.length();
						}
					} else {
						throw new IllegalStateException("Entity \""
								+ entityText
								+ "\" is not part of the document "
								+ new String(identifier)
								+ ". Document text is:\n" + docText.getText());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while parsing entity", e);
		}
	}

	private HashMap<String, String> buildWordMap(String[] words,
			String[] wordIds, int entityPosition, String text) {
		HashMap<String, String> wordMap = new HashMap<String, String>();
		int wordEnd = entityPosition;
		for (int i = 0; i < words.length; ++i) {
			String word = words[i];
			String wordId = wordIds[i];
			int wordStart = text.indexOf(word, wordEnd);
			wordEnd = wordStart + word.length();
			// System.out.println("id: " + wordId + ", word: " + word
			// + ", start: " + wordStart + ", end: " + wordEnd);
			wordMap.put(wordId, wordStart + "-" + wordEnd);
		}
		return wordMap;
	}

	// Old Version
	// private HashMap<String, String> buildWordMap(Node entity, int
	// entityPosition, String text) {
	// NodeList wordNodes = entity.getChildNodes();
	// HashMap<String, String> wordMap = new HashMap<String, String>();
	// int wordEnd = entityPosition;
	// for (int j = 0; j < wordNodes.getLength(); ++j) {
	// Node wordNode = wordNodes.item(j);
	// NamedNodeMap attrs = wordNode.getAttributes();
	// if (attrs == null)
	// continue;
	// String wordId = attrs.getNamedItem("id").toString();
	// wordId = wordId.substring(4, wordId.length() - 1);
	// String word = wordNode.getTextContent();
	// int wordStart = text.indexOf(word, wordEnd);
	// wordEnd = wordStart + word.length();
	// wordMap.put(wordId, wordStart + "-" + wordEnd);
	// }
	// return wordMap;
	// }

	private void addEntity(JCas jcas, int entityPosStart, int entityPosEnd,
			String entityId) {
		EntityMention entityAnno = new EntityMention(jcas, entityPosStart,
				entityPosEnd);
		entityAnno.setSpecificType(entityId);

		entityAnno.addToIndexes();
		// EntityMention e = entityAnno;
		// String s = jcas.getDocumentText();
		// System.out.println(s.substring(0,e.getBegin())+"#" +
		// e.getCoveredText() + "#" +s.substring(e.getEnd(),s.length()));
	}

}
