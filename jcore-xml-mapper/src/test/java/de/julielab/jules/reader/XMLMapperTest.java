/**
 * XMLMapperTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: faessler
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 09.05.2011
 **/

package de.julielab.jcore.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Test;

import de.julielab.jcore.types.Sentence;

/**
 * TODO insert description
 * 
 * @author faessler
 */
public class XMLMapperTest {

	public static final String TEST_DESC_PATH = "src/test/resources/XMLReaderDescriptor.xml";
	
	private static final String EXT_RES_KEY = "key";
	private static final String EXT_RES_NAME = "name";
	private static final String EXT_RES_URL = "url";

	@Test
	public void mapperTest() throws Exception {
		Map<String, String> readerConfig = new HashMap<String, String>();
		readerConfig.put(XMLReader.PARAM_INPUT_FILE, "src/test/resources/doc_medline_test_structured_abstract.xml");
		Map<String, String> readerExtRes = new HashMap<String, String>();
		readerExtRes.put(EXT_RES_KEY, XMLReader.RESOURCE_MAPPING_FILE);
		readerExtRes.put(EXT_RES_NAME, "newMappingFile");
		readerExtRes.put(EXT_RES_URL, "file:newMappingFile.xml");
		
		
		CollectionReader xmlReader = createCollectionReaderWithDescriptor(TEST_DESC_PATH, readerConfig, readerExtRes);
		CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData)xmlReader.getMetaData());
		assertTrue(xmlReader.hasNext());
		xmlReader.getNext(cas);
		
		System.out.println(cas.getDocumentText());
	}
	
	@Test
	public void inlineSentenceTest() throws Exception {
		Map<String, String> readerConfig = new HashMap<String, String>();
		readerConfig.put(XMLReader.PARAM_INPUT_FILE, "src/test/resources/doc_inline_sentences_test.xml");
		Map<String, String> readerExtRes = new HashMap<String, String>();
		readerExtRes.put(EXT_RES_KEY, XMLReader.RESOURCE_MAPPING_FILE);
		readerExtRes.put(EXT_RES_NAME, "InlineSentenceMappingFile");
		readerExtRes.put(EXT_RES_URL, "file:inlineSentenceMappingFile.xml");
		
		CollectionReader xmlReader = createCollectionReaderWithDescriptor(TEST_DESC_PATH, readerConfig, readerExtRes);
		CAS cas = CasCreationUtils.createCas((AnalysisEngineMetaData)xmlReader.getMetaData());
		assertTrue(xmlReader.hasNext());
		xmlReader.getNext(cas);
		FSIterator<Annotation> it = cas.getJCas().getAnnotationIndex(Sentence.type).iterator();

		assertTrue(it.hasNext());
		int sentencecount = 0;
		while (it.hasNext()) {
			it.next();
			++sentencecount;
		}
		assertEquals(12, sentencecount);
	}
	
	/**
	 * Creates a new CollectionReader with the given descriptor file and
	 * configuration parameters.
	 * 
	 * @param descriptorFile
	 *            the descriptor file
	 * @param configurationParameters
	 *            the configuration parameters
	 * @return the CollectionReader
	 * @throws UIMAException
	 * @throws IOException
	 */
	private CollectionReader createCollectionReaderWithDescriptor(
			String descriptorFile, Map<String, String> configurationParameters, Map<String, String> externalResources)
			throws UIMAException, IOException {
		CollectionReaderDescription readerDescription = (CollectionReaderDescription) UIMAFramework
				.getXMLParser().parseCollectionReaderDescription(
						new XMLInputSource(descriptorFile));
		ConfigurationParameterSettings settings = readerDescription
				.getCollectionReaderMetaData()
				.getConfigurationParameterSettings();
		if (configurationParameters != null) {
			for (String parameterName : configurationParameters.keySet())
				settings.setParameterValue(parameterName,
						configurationParameters.get(parameterName));
		}
		
		ResourceSpecifierFactory f = UIMAFramework.getResourceSpecifierFactory();
		ExternalResourceDescription extResDesc = f.createExternalResourceDescription();
		extResDesc.setName(externalResources.get(EXT_RES_NAME));
		FileResourceSpecifier fspec = f.createFileResourceSpecifier();
		fspec.setFileUrl(externalResources.get(EXT_RES_URL));
		extResDesc.setResourceSpecifier(fspec);
		
		ExternalResourceBinding extResBind = f.createExternalResourceBinding();
		extResBind.setKey(externalResources.get(EXT_RES_KEY));
		extResBind.setResourceName(externalResources.get(EXT_RES_NAME));
		readerDescription.getResourceManagerConfiguration().addExternalResource(extResDesc);
		readerDescription.getResourceManagerConfiguration().addExternalResourceBinding(extResBind);
		
		
		return UIMAFramework.produceCollectionReader(readerDescription);
	}

}