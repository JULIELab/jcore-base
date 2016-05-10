/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * 
 * @author hellrich
 *
 */

package de.julielab.jcore.reader.dta;

import de.julielab.jcore.types.Date;
import de.julielab.xml.FileTooBigException;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.JulieXMLTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.ListUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.PilotException;
import com.ximpleware.VTDNav;

public class DTAFileReader extends CollectionReader_ImplBase {

	public static final String DESCRIPTOR_PARAMTER_INPUTFILE = "inputFile";

	private String filename;

	private File inputFile;

	private int fileCount = 0;

	private int counter = 0;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DTAFileReader.class);

	public DTAFileReader() {

	}

	public void initialize() throws ResourceInitializationException {

		filename = (String) getConfigParameterValue(DESCRIPTOR_PARAMTER_INPUTFILE);

		Integer maxRecordsCount = (Integer) getConfigParameterValue(DESCRIPTOR_PARAMTER_MAXRECORDSCOUNT);

		inputFile = new File(filename);

		if (!inputFile.exists()) {
			new Exception("DIRECTORY_NOT_FOUND!");
		}

		xmlCorpus = JAXB.unmarshal(inputFile, generated.Corpus.class);

		if (xmlCorpus == null) {
			try {
				throw new Exception("Reading configuration from " + inputFile
						+ " failed.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		xmlDocs = xmlCorpus.getDocument();
		fileCount = xmlDocs.size();

		LOGGER.info("Input file contains " + fileCount + " documents.");

	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {

		JCas jcas = null;
		try {
			jcas = aCAS.getJCas();
		} catch (CASException e) {
			try {
				throw new Exception("could not get jcas", e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		LOGGER.info("Document-Number:" + counter);

		generated.Document doc = xmlDocs.get(counter);

		Document docInfo = new Document(jcas);
		docInfo.setId(doc.getId());
		docInfo.addToIndexes();

		Corpus corpus = new Corpus(jcas);

		corpus.setDocType(xmlCorpus.getDocType());
		corpus.setId(xmlCorpus.getId());
		corpus.setLanguage(xmlCorpus.getLang().name().toLowerCase());

		XMLGregorianCalendar xmlDate = xmlCorpus.getCreationDate();
		Date date = new Date(jcas);
		date.setDay(xmlDate.getDay());
		date.setMonth(xmlDate.getMonth());
		date.setYear(xmlDate.getYear());
		corpus.setCreationDate(date);

		corpus.addToIndexes();

		jcas.setDocumentLanguage(xmlCorpus.getLang().name().toLowerCase());

		StringBuilder textB = new StringBuilder();
		int offset = 0;

		for (generated.Unit u : doc.getUnit()) {
			Text t = u.getText();

			for (Object o : t.getContent()) {
				String text = (String) o;
				// inserted \n is not part of the unit itself!
				textB.append(text).append("\n");
				int end = offset + text.length() + 1;
				Unit unit = new Unit(jcas, offset, end - 1);
				unit.setId(u.getId());
				unit.addToIndexes();

				for (generated.E xmlE : u.getE()) {
					Entity entity = new Entity(jcas);
					int begin = xmlE.getOffset().intValue() + offset;
					entity.setBegin(begin);
					entity.setEnd(begin + xmlE.getLen().intValue());

					entity.setId(xmlE.getId());
					entity.setSource(xmlE.getSrc());
					entity.setCui(xmlE.getCui());
					entity.setSemanticType(xmlE.getType());
					entity.setSemanticGroup(xmlE.getGrp().value());

					String entityText = null;
					List<NER> ners = new ArrayList<NER>();
					for (Serializable x : xmlE.getContent()) {
						if (x instanceof generated.NER) {
							NER ner = new NER(jcas);
							ner.setSemanticGroup(((generated.NER) x).getGroup()
									.value());
							ner.setProbability(((generated.NER) x)
									.getProbability());
							ner.addToIndexes();
							ners.add(ner);
						} else if (x instanceof String)
							entityText = (String) x;
					}
					if (!ners.isEmpty()) {
						FSArray nerArray = new FSArray(jcas, ners.size());
						for (int i = 0; i < ners.size(); ++i) {
							nerArray.set(i, ners.get(i));
						}
						nerArray.addToIndexes();
						entity.setNer(nerArray);
					}

					entity.addToIndexes();

					String originalText = text.substring(xmlE.getOffset()
							.intValue(), xmlE.getOffset().intValue()
							+ xmlE.getLen().intValue());
					if (!originalText.equals(entityText))
						LOGGER.error("Error in input file, Entity "
								+ entity.getId()
								+ " has wrong offset/len or text!");
				}

				// TODO handle W annotations here!

				offset = end;
			}

		}
		jcas.setDocumentText(textB.toString());

		counter++;

	}

	public static String getDocumentText(String xmlFile, boolean normalize) throws ParseException, FileTooBigException, FileNotFoundException{
		List<Map<String,String>> fields = new ArrayList<>();

		//<token ID="w1">Des</token>
		Map<String,String> id2token = new HashMap<>();
		String tokenName = "token";
		String tokenIdName ="ID";
		fields.clear();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, tokenName, JulieXMLConstants.XPATH, "."));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, tokenIdName, JulieXMLConstants.XPATH, "@ID"));
		Iterator<Map<String, Object>> tokenIterator = JulieXMLTools.constructRowIterator(xmlFile, 1024, "/D-Spin/TextCorpus/tokens/token", fields, false);
		while (tokenIterator.hasNext()) {
			Map<String, Object> token = tokenIterator
					.next();
			id2token.put((String) token.get(tokenIdName), (String)token.get(tokenName));
		}
		
		//<tag tokenIDs="w1">ART</tag>  
		//TODO: check if STTS <POStags tagset="stts">
		Map<String,String> id2pos = new HashMap<>();
		String tagName = "tag";
		String tagIdName ="ID";
		fields.clear();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, tagName, JulieXMLConstants.XPATH, "."));
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, tagIdName, JulieXMLConstants.XPATH, "@tokenIDs"));
		Iterator<Map<String, Object>> tagIterator = JulieXMLTools.constructRowIterator(xmlFile, 1024, "/D-Spin/TextCorpus/POStags/tag", fields, false);
		while (tagIterator.hasNext()) {
			Map<String, Object> tag = tagIterator
					.next();
			id2pos.put((String) tag.get(tagIdName), (String)tag.get(tagName));
		}
		System.out.println(id2pos);
		
		StringBuilder text = new StringBuilder();
		fields.clear();
		fields.add(ImmutableMap.of(JulieXMLConstants.NAME, "tokenIDs", JulieXMLConstants.XPATH, "@tokenIDs"));
		Iterator<Map<String, Object>> sentenceIterator = JulieXMLTools.constructRowIterator(xmlFile, 1024, "/D-Spin/TextCorpus/sentences/sentence", fields, false);
		while (sentenceIterator.hasNext()) {
			boolean first = true;
			for(Object tokenIDs : sentenceIterator
					.next().values())
				for(String tokenID : ((String)tokenIDs).split(" ")){
					if(! id2token.containsKey(tokenID))
						throw new IllegalArgumentException("Token ID \""+tokenID+"\" has no associated token!");
					if(! id2pos.containsKey(tokenID))
						throw new IllegalArgumentException("Token \""+id2token.get(tokenID)+ "\" with ID \""+tokenID+"\" has no POS information!");
					if(!first || ! (id2pos.get(tokenID).equals("$,") || id2pos.get(tokenID).equals("$.")) ){
						text.append(" ");
						first = false;
					}
		
					text.append(id2token.get(tokenID));
				}
			text.append("\n");
		}
		return text.toString();
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		return counter < fileCount;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(counter, fileCount,
				Progress.ENTITIES) };
	}

	@Override
	public void close() throws IOException {
	}

}
