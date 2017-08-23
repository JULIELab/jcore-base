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

import de.julielab.jcore.reader.bionlpformat.main.BioEventReader;
import de.julielab.jcore.types.pubmed.Header;

/**
 * TODO insert description
 * 
 * @author buyko
 */
public class EventReaderApp {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
	private static BioEventReader collectionReader;
	private static String outputDir = "tmp";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			CollectionReaderDescription readerDescription = UIMAFramework.getXMLParser()
							.parseCollectionReaderDescription(new XMLInputSource(DESCRIPTOR_FILE));
			collectionReader = (BioEventReader) UIMAFramework.produceCollectionReader(readerDescription);
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
