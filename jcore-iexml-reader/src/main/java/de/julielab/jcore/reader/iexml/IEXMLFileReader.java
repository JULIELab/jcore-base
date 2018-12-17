/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * 
 * @author hellrich
 *
 */

package de.julielab.jcore.reader.iexml;

import generated.Unit.Text;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.mantra.Document;
import de.julielab.jcore.types.mantra.Entity;
import de.julielab.jcore.types.mantra.NER;
import de.julielab.jcore.types.mantra.Unit;
import de.julielab.jcore.types.mantra.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IEXMLFileReader extends CollectionReader_ImplBase {

	public static final String DESCRIPTOR_PARAMTER_INPUTFILE = "inputFile";

	public static final String DESCRIPTOR_PARAMTER_MAXRECORDSCOUNT = "maxRecordsCount";

	private String filename;

	private File inputFile;

	private int fileCount = 0;

	private int counter = 0;

	private List<generated.Document> xmlDocs;

	private generated.Corpus xmlCorpus;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IEXMLFileReader.class);

	public IEXMLFileReader() {

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

		if (maxRecordsCount != null) {
			fileCount = Math.min(fileCount, maxRecordsCount);
			LOGGER.info("maxRecordsCount was set: Setting fileCount down to "
					+ fileCount + " documents.");
		}

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
					for(Serializable x : xmlE.getContent()){
						if(x instanceof generated.NER){
							NER ner = new NER(jcas);
							ner.setSemanticGroup(((generated.NER)x).getGroup().value());
							ner.setProbability(((generated.NER)x).getProbability());
							ner.addToIndexes();
							ners.add(ner);
						}
						else if(x instanceof String)
							entityText = (String) x;
					}
					if(!ners.isEmpty()){
						FSArray nerArray = new FSArray(jcas, ners.size());
						for(int i=0; i<ners.size(); ++i){
							nerArray.set(i, ners.get(i));
						}		
						nerArray.addToIndexes();
						entity.setNer(nerArray);
					}
					
					entity.addToIndexes();
					
			
					String originalText = text.substring(xmlE.getOffset().intValue(), xmlE.getOffset().intValue()+xmlE.getLen().intValue());
					if(!originalText.equals(entityText))
						LOGGER.error("Error in input file, Entity "+entity.getId()+" has wrong offset/len or text!");
				}
				
				//TODO handle W annotations here!

				offset = end;
			}

		}
		jcas.setDocumentText(textB.toString());
		
		counter++;

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
