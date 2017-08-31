package de.julielab.jcore.ae.banner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.utility.JCoReTools;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;

/**
 * @author siddhartha, faessler
 *
 */
@TypeCapability(inputs = "de.julielab.jcore.types.Sentence", outputs = "de.julielab.jcore.types.Gene")
public class BANNERAnnotator extends JCasAnnotator_ImplBase {

	private final static Logger log = LoggerFactory.getLogger(BANNERAnnotator.class);

	public static final String PARAM_CONFIG_FILE = "ConfigFile";

	private Tokenizer tokenizer;
	private DictionaryTagger dictionary;
	private HierarchicalConfiguration config;
	// Dataset dataset;
	private EngLemmatiser lemmatiser;
	private Tagger posTagger;
	private CRFTagger tagger;
	private PostProcessor postProcessor;

	@ConfigurationParameter(name = PARAM_CONFIG_FILE, mandatory = true, description = "The XML configuration file for BANNER.")
	private String configFilePath;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		try {
			configFilePath = (String) aContext.getConfigParameterValue(PARAM_CONFIG_FILE);
			File configFile = new File(configFilePath);
			if (configFile.exists()) {
				log.debug("Found configuration file {}", configFile);
				config = new XMLConfiguration(configFile);
			} else {
				String classpathAddress = configFilePath.startsWith("/") ? configFilePath : "/" + configFilePath;
				log.debug(
						"Did not find configuration file as regular file at {}. Trying as classpath resource with address {}",
						configFile, classpathAddress);
				InputStream is = getClass().getResourceAsStream(classpathAddress);
				if (is != null) {
					log.debug("Found configuration file as classpath resource {}. Loading configuration.",
							classpathAddress);
					config = new XMLConfiguration();
					((XMLConfiguration) config).load(is);
				} else {
					throw new ResourceInitializationException(ResourceInitializationException.COULD_NOT_ACCESS_DATA,
							new Object[] { configFilePath });
				}
			}

			tokenizer = BANNER.getTokenizer(config);
			dictionary = BANNER.getDictionary(config);
			lemmatiser = BANNER.getLemmatiser(config);
			posTagger = BANNER.getPosTagger(config);
			postProcessor = BANNER.getPostProcessor(config);

			SubnodeConfiguration subConfig = config.configurationAt("banner.eval");
			String modelFilename = subConfig.getString("modelFilename");

			InputStream modelIs;
			if (new File(modelFilename).exists()) {
				modelIs = new FileInputStream(modelFilename);
			} else {
				modelIs = getClass().getResourceAsStream(modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename);
			}
			if (null == modelIs)
				throw new ResourceInitializationException(ResourceInitializationException.COULD_NOT_ACCESS_DATA,
						new Object[] { modelFilename });
			tagger = CRFTagger.load(modelIs, lemmatiser, posTagger, dictionary);
			log.info("{}: {}", PARAM_CONFIG_FILE, configFilePath);
			log.info("Model: {}", modelFilename);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String docId = JCoReTools.getDocId(jcas);
		FSIterator<Annotation> sentIt = jcas.getAnnotationIndex(de.julielab.jcore.types.Sentence.type).iterator();
		int geneCount = 0;
		int sentCount = 0;
		while (sentIt.hasNext()) {
			de.julielab.jcore.types.Sentence jcoreSentence = (de.julielab.jcore.types.Sentence) sentIt.next();
			int sentenceBegin = jcoreSentence.getBegin();
			String sentenceId = jcoreSentence.getId() != null ? jcoreSentence.getId() : docId + ": " + sentCount++;
			Sentence sentence = new Sentence(sentenceId, docId, jcoreSentence.getCoveredText());
			sentence = BANNER.process(tagger, tokenizer, postProcessor, sentence);
			for (Mention mention : sentence.getMentions()) {
				Gene g = new Gene(jcas, sentenceBegin + mention.getStartChar(), sentenceBegin + mention.getEndChar());
				g.setId("BANNER, " + docId + ": " + geneCount++);
				g.setComponentId(BANNERAnnotator.class.getCanonicalName());
				g.setConfidence(String.valueOf(mention.getProbability()));
				g.addToIndexes();
				/*
				 * StringBuilder output = new StringBuilder();
				 * output.append(line); // sentence identifier
				 * output.append("\t"); output.append(mention.getEntityType());
				 * output.append("\t"); output.append(mention.getStartChar());
				 * output.append("\t"); output.append(mention.getEndChar());
				 * output.append("\t"); output.append(mention.getText());
				 * System.out.println(output.toString());
				 */
			}
		}
	}
}
