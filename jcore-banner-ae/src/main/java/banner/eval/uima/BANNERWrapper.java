package banner.eval.uima;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;


import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;


public class BANNERWrapper {

	Tokenizer tokenizer;
	DictionaryTagger dictionary;
	HierarchicalConfiguration config;
	// Dataset dataset;
	EngLemmatiser lemmatiser;
	Tagger posTagger;
	CRFTagger tagger;
	PostProcessor postProcessor;
	private final String configPrefix = "config/";
	private final String modelPrefix = "output/";

	public Map<String, String> getAnnotations(String docText) {
		Map<String, String> annotSpans = new HashMap<String, String>();
		Scanner sc = new Scanner(docText);
		int count = 0;
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if (line.length() > 0) {
				// String[] split = line.split("\\t");
				Sentence sentence = new Sentence(Integer.toString(count), "",
						line);
				sentence = BANNER.process(tagger, tokenizer, postProcessor,
						sentence);
				for (Mention mention : sentence.getMentions()) {
					annotSpans.put(mention.getText(), mention.getEntityType()
							.getText());
					// Gene g = new Gene(jcas, count+mention.getStartChar(),
					// count+mention.getEndChar());
					// g.setId("");
					// g.addToIndexes();
					/*
					 * StringBuilder output = new StringBuilder();
					 * output.append(line); // sentence identifier
					 * output.append("\t");
					 * output.append(mention.getEntityType());
					 * output.append("\t");
					 * output.append(mention.getStartChar());
					 * output.append("\t"); output.append(mention.getEndChar());
					 * output.append("\t"); output.append(mention.getText());
					 * System.out.println(output.toString());
					 */
				}
			}
			count += line.length() + 1;
		}
		return annotSpans;
	}
	
	
	public InputStream getStream(String path){
	  URL url = getClass().getClassLoader().getResource(path);
    try {
      return url.openStream();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
	  return null;
	}
	
	 public String getFile(String path){
	    URL url = getClass().getClassLoader().getResource(path);
	    System.out.println("URL:" + url.getFile()); 
	    return url.getFile();
	  }
	
	public void initializeModel(String configFile, String modelFile) throws ConfigurationException{
		long start = System.currentTimeMillis();
		System.out.println("modelFile: " + modelFile);
    System.out.println("configFile: " + configFile);
		this.config = new XMLConfiguration(getFile(configPrefix+ configFile));

		// dataset = BANNER.getDataset(config);
		tokenizer = BANNER.getTokenizer(config);
		dictionary = BANNER.getDictionary(config);
		lemmatiser = BANNER.getLemmatiser(config);
		posTagger = BANNER.getPosTagger(config);
		postProcessor = BANNER.getPostProcessor(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		
		try {
			System.out.println(modelFile);
			tagger = CRFTagger.load(getClass().getClassLoader().getResource(modelPrefix +modelFile).openStream(), lemmatiser, posTagger,
					dictionary);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Loaded model: "
				+ (System.currentTimeMillis() - start) + "ms");

	}

	public void initialize(String configFilePath, String modelFilePath)
			throws IOException, ConfigurationException {
		
		/*TEST*/
		//File f = new File(configPrefix + configFilePath);
		
		String fullConfigFilePath = configPrefix + configFilePath;
		String fullModelFilePath = modelPrefix + modelFilePath;
		File configFile = new File(fullConfigFilePath);
		File modelFile = new File(fullModelFilePath);
		System.out.println("test path: " +  configFile.getAbsolutePath());
		System.out.println("test path: " +  modelFile.getAbsolutePath());
		
	//	if (configFile.exists() && modelFile.exists()) {
			System.out.println("Reading model from file: " + fullModelFilePath);
			System.out.println("Reading config from file: "
					+ fullConfigFilePath);
			initializeModel(configFilePath,modelFilePath);
		//} else {
		//	System.err.println("File not found: " + configFile + "\nor: " + modelFile);
		//}
		
	
		/*
		 * BANNER.logInput(dataset.getSentences(), config);
		 * System.out.println("Completed input: " + (System.currentTimeMillis()
		 * - start)); Performance performance = test(dataset, tagger, config);
		 * performance.print();
		 */
	}
	/*
	public void initialize( String configUrl,  String modelUrl){
		String cp = System.getProperty("java.class.path");
		System.out.println("classpath: " + cp);
		//System.out.println(new File("."))
		System.out.println("Reading model from system resource: "
				+ modelUrl);
		System.out.println("Reading config from system resource: "
				+ configUrl);
		try {
		//	File configFile = new File("jar:" + configUrl.getFile());
		//	File modelFile = new File(modelUrl);
		 //  System.out.println(modelFile.getPath());
			initializeModel(configUrl,modelUrl);
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}*/
	
	public static void main(String[] args){
		String question = "Does BRCA1 cause mad cow disease?";
	//	List<String> questions = new ArrayList<String>();
		//questions.add(question);
		BANNERWrapper bw = new BANNERWrapper();
		String configPath = "config/banner_AZDC.xml";
		String modelPath = "output/model_AZDC.bin";
		try {
			bw.initialize(configPath, modelPath);
		} catch (ConfigurationException e) {
			System.err.println("configuration exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("io exception");			
			e.printStackTrace();
		}
		System.out.println("Tag diseases:");
		Map<String,String> diseasesAnnots = bw.getAnnotations(question);
		for(String k : diseasesAnnots.keySet()){
			String mentionText = k, type = diseasesAnnots.get(k);
			System.out.println("keyterm: " + mentionText +  "type: " + type);
		}
		
		
		bw = new BANNERWrapper();
		configPath = "config/banner_BC2GM.xml";
		modelPath = "output/model_BC2GM.bin";
		
		try {
			bw.initialize(configPath, modelPath);
		} catch (ConfigurationException e) {
			System.err.println("configuration exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("io exception");			
			e.printStackTrace();
		}
		
		
		
		System.out.println("Tag genes:");
		
		Map<String,String> geneAnnots = bw.getAnnotations(question);
		for(String k : geneAnnots.keySet()){
			String mentionText = k, type = geneAnnots.get(k);
			System.out.println("keyterm: " + mentionText +  "type: " + type);
		}
		
		
		
		
	}

	/*
	 * private DictionaryTagger loadDictionary(UimaContext aContext) { try {
	 * //String dict = aContext.getResourceFilePath("dict"); String
	 * tokenizerName = aContext.getResourceFilePath("tokenizer"); tokenizer =
	 * (Tokenizer)Class.forName(tokenizerName).newInstance();
	 * 
	 * String dictionaryName = aContext.getResourceFilePath("dictionaryTagger");
	 * dictionary =
	 * (DictionaryTagger)Class.forName(dictionaryName).newInstance();
	 * 
	 * //configure dictionary DictionaryTagger d = new DictionaryTagger();
	 * d.filterContainedMentions = (Boolean)
	 * aContext.getConfigParameterValue("filterContainedMentions");
	 * d.normalizeMixedCase = (Boolean)
	 * aContext.getConfigParameterValue("normalizeMixedCase"); d.normalizeDigits
	 * = (Boolean) aContext.getConfigParameterValue("normalizeDigits");
	 * d.generate2PartVariations = (Boolean)
	 * aContext.getConfigParameterValue("generate2PartVariations");
	 * d.dropEndParentheticals = (Boolean)
	 * aContext.getConfigParameterValue("dropEndParentheticals");
	 * 
	 * 
	 * 
	 * String dictionaryTypeName = (String)
	 * aContext.getConfigParameterValue("dictionaryType");
	 * 
	 * 
	 * 
	 * 
	 * String delimiter = (String)
	 * aContext.getConfigParameterValue("delimiter"); int column = (Integer)
	 * aContext.getConfigParameterValue("column"); EntityType dictionaryType =
	 * EntityType.getType(dictionaryTypeName);
	 * 
	 * // Load data BufferedReader reader = new BufferedReader(new
	 * FileReader(dictionaryName)); String line = reader.readLine(); while (line
	 * != null) { line = line.trim(); if (line.length() > 0) { if (delimiter ==
	 * null) { add(line, dictionaryType); } else { // TODO Performance - don't
	 * use split String[] split = line.split(delimiter); add(split[column],
	 * dictionaryType); } } line = reader.readLine(); } reader.close();
	 * 
	 * } catch (ResourceAccessException e) { e.printStackTrace(); } catch
	 * (InstantiationException e) { e.printStackTrace(); } catch
	 * (IllegalAccessException e) { e.printStackTrace(); } catch
	 * (ClassNotFoundException e) { e.printStackTrace(); } return null; }
	 */

}
