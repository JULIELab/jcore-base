
package de.julielab.jcore.ae;

import de.julielab.jcore.reader.xmi.XmiCollectionReader;
import de.julielab.jcore.types.DocumentTopics;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.topicmodeling.businessobjects.Model;
import de.julielab.topicmodeling.businessobjects.Topic;
import de.julielab.topicmodeling.services.MalletTopicModeling;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for jcore-topic-labeling-ae.
 * @author 
 *
 */public class TopicIndexingTest {
	
	
	@Test
	public void testReading() throws IOException, UIMAException {
		CollectionReader xmiReader = CollectionReaderFactory.createReader(
				"de.julielab.jcore.reader.xmi.desc.jcore-xmi-reader", 
				XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/xmi_data"
//				XmiCollectionReader.PARAM_INPUTDIR, "D:/testprocessed_lemma/"
				);
		// TODO remove xmi-splitter-types and document-meta-extension-types
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", 
				"de.julielab.jcore.types.jcore-xmi-splitter-types",
				"de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
				"de.julielab.jcore.types.jcore-document-structure-pubmed-types",
				"de.julielab.jcore.types.jcore-morpho-syntax-types");
		CAS aCAS = jCas.getCas();
		
		while (xmiReader.hasNext()) {
			xmiReader.getNext(aCAS);
			JCas filledJCas = aCAS.getJCas();
			System.out.println(filledJCas.getDocumentText());
			String docId = JCoReTools.getDocId(filledJCas);
			assertNotNull(docId);
		}
	}
	
	@Test
	public void testLabeling() throws IOException, UIMAException {
		CollectionReader xmiReader = CollectionReaderFactory.createReader(
				"de.julielab.jcore.reader.xmi.desc.jcore-xmi-reader", 
				XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/xmi_data"
				);
		// TODO remove xmi-splitter-types and document-meta-extension-types
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", 
				"de.julielab.jcore.types.jcore-xmi-splitter-types",
				"de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
				"de.julielab.jcore.types.jcore-document-structure-pubmed-types",
				"de.julielab.jcore.types.jcore-morpho-syntax-types");
		CAS aCAS = jCas.getCas();
		
		while (xmiReader.hasNext()) {
			xmiReader.getNext(aCAS);
			JCas filledJCas = aCAS.getJCas();
			
			AnalysisEngine topicIndexer = AnalysisEngineFactory.createEngine(
					"de.julielab.jcore.ae.topiclabeling.desc.jcore-topic-indexing-ae", 
					TopicIndexer.PARAM_TOPIC_MODEL_CONFIG, "src/test/resources/config_template.xml", 
					TopicIndexer.PARAM_TOPIC_MODEL_FILE_NAME, "src/test/resources/test_topic_model.ser",
					TopicIndexer.PARAM_NUM_DISPLAYED_TOPIC_WORDS, 5, 
					TopicIndexer.PARAM_STORE_IN_MODEL_INDEX, false
					);
			topicIndexer.process(filledJCas);
			FSIterator<Annotation> iterator = filledJCas.getAnnotationIndex(DocumentTopics.type).iterator();
			while(iterator.hasNext()){
				DocumentTopics topics = (DocumentTopics) iterator.next();
				for (int i = 0; i < topics.getTopicWords().size(); i++) {
					assertNotNull(topics.getTopicWords(i));
				}
				assertTrue(topics.getTopicWords().size() == 5);
			}
		}
	}
	
	@Test
	public void testIndexingAndWriteToFile() throws IOException, UIMAException {
			CollectionReader xmiReader = CollectionReaderFactory.createReader(
					"de.julielab.jcore.reader.xmi.desc.jcore-xmi-reader", 
					XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/xmi_data"
					);
			// TODO remove xmi-splitter-types and document-meta-extension-types
			JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", 
					"de.julielab.jcore.types.jcore-xmi-splitter-types",
					"de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
					"de.julielab.jcore.types.jcore-document-structure-pubmed-types",
					"de.julielab.jcore.types.jcore-morpho-syntax-types");
			CAS aCAS = jCas.getCas();
			
			AnalysisEngine topicIndexer = AnalysisEngineFactory.createEngine(
					"de.julielab.jcore.ae.topiclabeling.desc.jcore-topic-indexing-ae", 
					TopicIndexer.PARAM_TOPIC_MODEL_CONFIG, "src/test/resources/config_template.xml", 
					TopicIndexer.PARAM_TOPIC_MODEL_FILE_NAME, "src/test/resources/test_topic_model.ser",
					TopicIndexer.PARAM_NUM_DISPLAYED_TOPIC_WORDS, 5, 
					TopicIndexer.PARAM_STORE_IN_MODEL_INDEX, true
					);
			while (xmiReader.hasNext()) {
				xmiReader.getNext(aCAS);
				JCas filledJCas = aCAS.getJCas();
				topicIndexer.process(filledJCas);
			}
			topicIndexer.collectionProcessComplete();
			
			try {
			MalletTopicModeling tm = new MalletTopicModeling();
			Model savedIndexedModel = tm.readModel("src/test/resources/test_topic_model.ser" 
					+ "-" + InetAddress.getLocalHost().getHostName()
					+ "-" + ManagementFactory.getRuntimeMXBean().getName());
			assertTrue(savedIndexedModel.index.containsKey("11442408"));
			assertTrue(savedIndexedModel.index.containsKey("12390745"));
			assertTrue(savedIndexedModel.index.size() == 2);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	@Test
	public void testIndexing() throws IOException, UIMAException {
			CollectionReader xmiReader = CollectionReaderFactory.createReader(
					"de.julielab.jcore.reader.xmi.desc.jcore-xmi-reader", 
					XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/xmi_data"
					);
			// TODO remove xmi-splitter-types and document-meta-extension-types
			JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", 
					"de.julielab.jcore.types.jcore-xmi-splitter-types",
					"de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
					"de.julielab.jcore.types.jcore-document-structure-pubmed-types",
					"de.julielab.jcore.types.jcore-morpho-syntax-types");
			CAS aCAS = jCas.getCas();
			
			AnalysisEngine topicIndexer = AnalysisEngineFactory.createEngine(
					"de.julielab.jcore.ae.topiclabeling.desc.jcore-topic-indexing-ae", 
					TopicIndexer.PARAM_TOPIC_MODEL_CONFIG, "src/test/resources/config_template.xml", 
//					TopicIndexer.PARAM_TOPIC_MODEL_FILE_NAME, "src/test/resources/test_topic_model.ser",
					TopicIndexer.PARAM_TOPIC_MODEL_FILE_NAME, "src/test/resources/model_eval_tm_b",
					TopicIndexer.PARAM_NUM_DISPLAYED_TOPIC_WORDS, 5, 
					TopicIndexer.PARAM_STORE_IN_MODEL_INDEX, true
					);
			while (xmiReader.hasNext()) {
				xmiReader.getNext(aCAS);
				JCas filledJCas = aCAS.getJCas();
				topicIndexer.process(filledJCas);
			}
			topicIndexer.collectionProcessComplete();
			
			try {
			MalletTopicModeling tm = new MalletTopicModeling();
			Model savedIndexedModel = tm.readModel("src/test/resources/test_topic_model.ser" 
					+ "-" + InetAddress.getLocalHost().getHostName()
					+ "-" + ManagementFactory.getRuntimeMXBean().getName());
			List<Topic> topics = savedIndexedModel.index.get("11442408");
			System.out.println(topics.size());
			
			List<Topic> topics2 = savedIndexedModel.index.get("11442408");
			System.out.println(topics2.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}
