
package de.julielab.jcore.ae;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.DocumentTopics;
import de.julielab.jcore.types.AutoDescriptor;
import de.julielab.topicmodeling.businessobjects.Model;
import de.julielab.topicmodeling.businessobjects.Topic;
import de.julielab.topicmodeling.services.MalletTopicModeling;
import de.julielab.jcore.utility.JCoReTools;

public class TopicIndexer extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(TopicIndexer.class);

	/**
	 * XML configuration file for model training, labeling, and search
	 */
	public static final String PARAM_TOPIC_MODEL_CONFIG = "TopicModelConfig";

	/**
	 * Serialized file containing a de.julielab.topicmodeling.Model object
	 * that includes fields for name, version, ID map for Pubmed-IDs and Mallet-IDs,
	 * index, and the Mallet model object
	 */
	public static final String PARAM_TOPIC_MODEL_FILE_NAME = "TopicModelFile";

	/**
	 * Number of the top topic words that can be used e.g. for displaying as 
	 * label for documents 
	 */
	public static final String PARAM_NUM_DISPLAYED_TOPIC_WORDS = "DisplayedTopicWords";

	/**
	 * Whether or not to store the processed labels in the index of the model object 
	 */
	public static final String PARAM_STORE_IN_MODEL_INDEX = "StoreInModelIndex";

	@ConfigurationParameter(name = PARAM_TOPIC_MODEL_CONFIG, mandatory = true)
	private String model_config;
	@ConfigurationParameter(name = PARAM_TOPIC_MODEL_FILE_NAME, mandatory = true)
	private String model_file;
	@ConfigurationParameter(name = PARAM_NUM_DISPLAYED_TOPIC_WORDS, mandatory = true)
	private int displayedTopicWords;
	@ConfigurationParameter(name = PARAM_STORE_IN_MODEL_INDEX, mandatory = true)
	private boolean toModelIndex;

	MalletTopicModeling tm;
	Model savedModel;
	XMLConfiguration xmlConfig;

	/**
	 * Loads model configuration and serialized model and checks whether to populate the 
	 * model's index
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {
		try {
			model_config = (String) aContext.getConfigParameterValue(PARAM_TOPIC_MODEL_CONFIG);
			model_file = (String) aContext.getConfigParameterValue(PARAM_TOPIC_MODEL_FILE_NAME);
			toModelIndex = (boolean) aContext.getConfigParameterValue(PARAM_STORE_IN_MODEL_INDEX);
			displayedTopicWords = (Integer) aContext.getConfigParameterValue(PARAM_NUM_DISPLAYED_TOPIC_WORDS);
			tm = new MalletTopicModeling();
			xmlConfig = tm.loadConfig(model_config);
			savedModel = tm.readModel(model_file);
			savedModel.index = new HashMap<>();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Infers labels and stores them in the CAS; if toModelIndex is set true, information is to stored
	 * into the model's index 
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
		try {
			String modelID = savedModel.modelId;
			String modelVersion = savedModel.modelVersion;
			String docId = JCoReTools.getDocId(aJCas);
			if (!savedModel.ModelIdpubmedId.containsValue(docId)) {
				Map<String, List<Topic>> result = tm.inferLabel(aJCas, savedModel, xmlConfig);
				DoubleArray topicWeights = new DoubleArray(aJCas, result.size());
				IntegerArray topicIds = new IntegerArray(aJCas, result.size());
				StringArray topicWords = new StringArray(aJCas, displayedTopicWords);
				for (int i = 0; i < result.size(); i++) {
					double topicWeight = result.get(docId).get(i).probability;
					int topicId = result.get(docId).get(i).id;
					for (int k = 0; k < displayedTopicWords; k++){
						String topicWord = (String) result.get(docId).get(i).topicWords[k];
						topicWords.set(k, topicWord);
					}
					topicWeights.set(i, topicWeight);
					topicIds.set(i, topicId);
				}
				DocumentTopics documentTopics = new DocumentTopics(aJCas);
				documentTopics.setIDs(topicIds);
				documentTopics.setWeights(topicWeights);
				documentTopics.setModelID(modelID);
				if (modelVersion != "") {
					documentTopics.setModelVersion(modelVersion);
				}
				documentTopics.setTopicWords(topicWords);
				aJCas.addFsToIndexes(documentTopics);
				log.info("Labeled document " + docId);
				if (toModelIndex) {
					List<Topic> topics = new ArrayList<>();
					for (int i = 0; i < topicWeights.size(); i++) {
						Topic topic = new Topic();
						topic.topicWords = new Object[topicWords.size()];
						topic.probability = topicWeights.get(i);
							topic.id = topicIds.get(i);
							for (int k = 0; k < topicWords.size(); k++) {
								String topicWord = topicWords.get(k);
								topic.topicWords[i] = topicWord; 
							}
							topic.modelId = modelID;
							topic.modelVersion = modelVersion;
							topics.add(topic);
						}	
						savedModel.index.put(docId, topics);
						log.info("Indexed document: " + docId);
						Collection<AutoDescriptor> autoDescs = JCasUtil.select(aJCas, AutoDescriptor.class);
						AutoDescriptor autoDesc;
						if (!autoDescs.isEmpty())
							autoDesc = autoDescs.iterator().next();
						else{ autoDesc = new AutoDescriptor(aJCas); autoDesc.addToIndexes();}
					
						FSArray dt = autoDesc.getDocumentTopics();
						dt = JCoReTools.addToFSArray(dt, documentTopics);
						autoDesc.setDocumentTopics(dt);
					}
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		try {
			tm.saveModel(savedModel, model_file + "-" + InetAddress.getLocalHost().getHostName() + "-"
					+ ManagementFactory.getRuntimeMXBean().getName());
			log.info("Model with index is written in:" + model_file + "-" + InetAddress.getLocalHost().getHostName()
					+ "-" + ManagementFactory.getRuntimeMXBean().getName());
			super.collectionProcessComplete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
