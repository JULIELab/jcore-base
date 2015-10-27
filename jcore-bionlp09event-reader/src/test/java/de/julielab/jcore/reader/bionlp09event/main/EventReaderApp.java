/** 
 * EventReaderApp.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: buyko
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 22.01.2009 
 **/
package de.julielab.jcore.reader.bionlp09event.main;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import de.julielab.jcore.reader.bionlp09event.main.EventReader;
import de.julielab.jcore.types.pubmed.Header;

/**
 * TODO insert description
 * 
 * @author buyko
 */
public class EventReaderApp {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
	private static EventReader collectionReader;
	private static String outputDir = "tmp";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			CollectionReaderDescription readerDescription = UIMAFramework.getXMLParser()
							.parseCollectionReaderDescription(new XMLInputSource(DESCRIPTOR_FILE));
			collectionReader = (EventReader) UIMAFramework.produceCollectionReader(readerDescription);
			CAS cas = CasCreationUtils.createCas(collectionReader.getProcessingResourceMetaData());
			
			while (collectionReader.hasNext()) {
				collectionReader.getNext(cas);
				writeXMI(cas.getJCas());
				cas.reset();
			}
		} catch (InvalidXMLException e) {
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CollectionException e) {
			e.printStackTrace();
		} catch (CASException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeXMI(JCas jcas) {
		try {
			String fileName = null;
			FSIterator header = jcas.getAnnotationIndex(Header.type).iterator();
			while (header.hasNext()) {
				fileName = ((Header) header.next()).getDocId();
			}
			FileOutputStream fos = new FileOutputStream(outputDir + "/" + fileName + ".xmi");
			XmiCasSerializer.serialize(jcas.getCas(), fos);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
