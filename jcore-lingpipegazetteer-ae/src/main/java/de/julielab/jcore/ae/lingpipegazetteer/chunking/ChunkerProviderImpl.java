package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import com.aliasi.chunk.Chunker;
import com.aliasi.dict.*;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkerProviderImpl implements ChunkerProvider,
		SharedResourceObject {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChunkerProviderImpl.class);
	public final static String PARAM_USE_APPROXIMATE_MATCHING = "UseApproximateMatching";
	public final static String PARAM_CASE_SENSITIVE = "CaseSensitive";
	public final static String PARAM_MAKE_VARIANTS = "MakeVariants";
	public final static String PARAM_STOPWORD_FILE = "StopWordFile";
	public final static String PARAM_DICTIONARY_FILE = "DictionaryFile";
	public final static String PARAM_SERIALIZED_DICTIONARY_FILE = "SerializedDictionaryFile";

	private boolean generateVariants;
	private boolean caseSensitive;
	private boolean useApproximateMatching;

	private AbstractDictionary<String> dict;
	private Chunker dictChunker = null;
	private final double CHUNK_SCORE = 1.0;

	private final int MIN_TERM_LENGTH = 3;
	private final int NUM_HYPHENS4VARIANTS = 7;
	private final String SEPARATOR = "\t";
	private final double APPROX_MATCH_THRESHOLD_SCORE = 100;
	private TreeSet<String> stopWords = new TreeSet<String>();

	public Chunker getChunker() {
		return dictChunker;
	}

	public void load(DataResource resource)
			throws ResourceInitializationException {
		Properties properties = new Properties();
		try {
			properties.load(resource.getInputStream());
		} catch (IOException e) {
			LOGGER.error("Error while loading properties file", e);
			throw new ResourceInitializationException(e);
		}

		LOGGER.info("Creating dictionary chunker with " + resource.getUrl()
				+ " properties file.");

		String dictionaryFilePath = properties
				.getProperty(PARAM_DICTIONARY_FILE);
		if (dictionaryFilePath == null)
			throw new ResourceInitializationException(
					ResourceInitializationException.CONFIG_SETTING_ABSENT,
					new Object[] { PARAM_DICTIONARY_FILE });

		String stopwordFilePath = properties.getProperty(PARAM_STOPWORD_FILE);
		if (stopwordFilePath == null)
			throw new ResourceInitializationException(
					ResourceInitializationException.CONFIG_SETTING_ABSENT,
					new Object[] { PARAM_STOPWORD_FILE });

		String serializedDictionaryPath = properties
				.getProperty(PARAM_SERIALIZED_DICTIONARY_FILE);
		File serializedDictionaryFile = serializedDictionaryPath != null ? new File(
				serializedDictionaryPath) : null;
		LOGGER.debug("Serialized dictionary path: {}", serializedDictionaryPath);

		String generateVariantsString = properties
				.getProperty(PARAM_MAKE_VARIANTS);
		generateVariants = true;
		if (generateVariantsString != null)
			generateVariants = new Boolean(generateVariantsString);
		LOGGER.debug("Generate variants: {}", generateVariants);

		String caseSensitiveString = properties
				.getProperty(PARAM_CASE_SENSITIVE);
		caseSensitive = false;
		if (caseSensitiveString != null)
			caseSensitive = new Boolean(caseSensitiveString);
		LOGGER.debug("Case sensitive: {}", caseSensitive);

		String useApproximateMatchingString = properties
				.getProperty(PARAM_USE_APPROXIMATE_MATCHING);
		useApproximateMatching = false;
		if (useApproximateMatchingString != null)
			useApproximateMatching = new Boolean(useApproximateMatchingString);
		LOGGER.debug("Use approximate matching: {}",
				useApproximateMatchingString);

		try {
			InputStream dictFile;
			if(new File(dictionaryFilePath).exists())
					dictFile = new FileInputStream(dictionaryFilePath);
			else{
				String resourceLocation = dictionaryFilePath.startsWith("/") ? dictionaryFilePath : "/" + dictionaryFilePath;
				dictFile = getClass().getResourceAsStream(resourceLocation);
			}
			
			InputStream stopFile;
			if(new File(stopwordFilePath).exists())
					stopFile = new FileInputStream(stopwordFilePath);
			else{
				String resourceLocation = stopwordFilePath.startsWith("/") ? stopwordFilePath : "/" + stopwordFilePath;
				stopFile = getClass().getResourceAsStream(resourceLocation);
			}
			if (null != serializedDictionaryFile
					&& serializedDictionaryFile.exists()) {
				readSerializedDictionaryFile(serializedDictionaryFile);
			} else {
				initStopWords(stopFile);
				readDictionary(dictFile);
				if (!StringUtils.isBlank(serializedDictionaryPath))
					serializeDictionary(serializedDictionaryFile);
			}

			if (useApproximateMatching) {
				dictChunker = new ApproxDictionaryChunker(
						(TrieDictionary<String>) dict,
						IndoEuropeanTokenizerFactory.INSTANCE,
						ApproxDictionaryChunker.TT_DISTANCE,
						APPROX_MATCH_THRESHOLD_SCORE);
			} else {
				dictChunker = new ExactDictionaryChunker(dict,
						IndoEuropeanTokenizerFactory.INSTANCE, false,
						caseSensitive);
			}

		} catch (Exception e) {
			LOGGER.error("Exception while creating chunker instance", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void readSerializedDictionaryFile(File serializedDictionaryFile)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		long time = System.currentTimeMillis();
		LOGGER.info("Reading serialized dictionary from: {}",
				serializedDictionaryFile.getAbsolutePath());
		LOGGER.info("Warning: Loading a serialized dictionary seems to take longer than just reading the original text entries");
		try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(
				new FileInputStream(serializedDictionaryFile)))) {
			dict = (AbstractDictionary<String>) ois.readObject();
		}
		LOGGER.info("Dictionary has been read.");
		time = System.currentTimeMillis() - time;
		LOGGER.info("Reading serialized dictionary took {}ms ({}s)", time,
				time / 1000);
	}

	private void serializeDictionary(File serializedDictionaryFile)
			throws FileNotFoundException, IOException {
		LOGGER.info("Storing dictionary to: {}",
				serializedDictionaryFile.getAbsolutePath());
		LOGGER.info("Warning: Loading a serialized dictionary seems to take longer than just reading the original text entries");
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(
				new FileOutputStream(serializedDictionaryFile)));
		dict.compileTo(oos);
		oos.close();
		LOGGER.info("{} bytes written.", serializedDictionaryFile.length());
	}

	/*
	 * public static Chunker makeDictChunker() { if (useApproxMatching) {
	 * NGramTokenizerFactory x = new NGramTokenizerFactory(2, 4); dictChunker =
	 * new ApproxDictionaryChunker((TrieDictionary) dict,
	 * IndoEuropeanTokenizerFactory.FACTORY,
	 * ApproxDictionaryChunker.TT_DISTANCE, APPROX_MATCH_THRESHOLD_SCORE); }
	 * else { dictChunker = new ExactDictionaryChunker((MapDictionary) dict,
	 * IndoEuropeanTokenizerFactory.FACTORY, false, caseSensitive); } return
	 * dictChunker; }
	 */

	private void readDictionary(InputStream dictFile) throws IOException,
			AnalysisEngineProcessException {
		long time = System.currentTimeMillis();
		if (useApproximateMatching) {
			dict = new TrieDictionary<String>();
		} else {
			dict = new MapDictionary<String>();
		}
		// now read from file and add entries
		LOGGER.info("readDictionary() - adding entries from " + dictFile
				+ " to dictionary...");
		try (InputStreamReader isr = new InputStreamReader(dictFile)) {
			BufferedReader bf = new BufferedReader(isr);
			String line = "";
			String variant = "";
			TreeSet<String> termVariants;
			TreeSet<String> dictionary = new TreeSet<String>();

			while ((line = bf.readLine()) != null) {
				String[] values = line.split("\t");
				if (values.length != 2) {
					LOGGER.error("readDictionary() - wrong format of line: "
							+ line);
					throw new AnalysisEngineProcessException(
							AnalysisEngineProcessException.ANNOTATOR_EXCEPTION,
							null);
				}

				String term = values[0].trim();
				String label = values[1].trim();
				if (term.length() < MIN_TERM_LENGTH)
					continue;

				if (useApproximateMatching && !caseSensitive)
					term = term.toLowerCase();

				if (generateVariants) {
					LOGGER.debug("readDictionary() - make term variants of ("
							+ term
							+ ", "
							+ label
							+ ") and add them to dictionary (NOTE: this may take a while if dictionary is big!)");
					termVariants = makeTermVariants(term);
					Iterator<String> it = termVariants.iterator();
					while (it.hasNext()) {
						variant = it.next();
						if (!stopWords.contains(variant.toLowerCase())
								&& !variant.equals("")) {
							// System.err.println("ADDING VARIANT: " + variant + "="
							// + label);
							dictionary.add(variant + SEPARATOR + label);
						}
						// dict.addEntry(new DictionaryEntry(it.next(), label,
						// CHUNK_SCORE));
					}
					it = null;
				} else {
					if (!stopWords.contains(term.toLowerCase()))
						dictionary.add(term + SEPARATOR + label);
					// dict.addEntry(new DictionaryEntry(term, label, CHUNK_SCORE));
				}

				if (dictionary.size() >= 10000) {
					LOGGER.debug("readDictionary() - flushing dictionarySet to map dictionary");
					dictionary = flushDictionary(dictionary, dict);
				}

			}

			dictionary = flushDictionary(dictionary, dict);
			dictionary = null;
			time = System.currentTimeMillis() - time;
			LOGGER.info("Reading dictionary took {}ms ({}s)", time, time / 1000);
		}
	}

	private TreeSet<String> flushDictionary(TreeSet<String> dictionarySet,
			AbstractDictionary<String> dict)
			throws AnalysisEngineProcessException {

		Iterator<String> it = dictionarySet.iterator();
		String[] split;
		while (it.hasNext()) {
			split = it.next().split(SEPARATOR);
			if (split.length != 2) {
				LOGGER.error("readDictionary() - wrong split length: "
						+ split.length);
				throw new AnalysisEngineProcessException(
						AnalysisEngineProcessException.ANNOTATOR_EXCEPTION,
						null);
			}
			dict.addEntry(new DictionaryEntry<String>(split[0], split[1],
					CHUNK_SCORE));
		}
		it = null;
		dictionarySet.clear();

		return dictionarySet;
	}

	private TreeSet<String> makeTermVariants(String term) {

		TreeSet<String> termVariants = new TreeSet<String>();
		termVariants.add(term);
		String termVariant = "";

		// replace hyphens with white space unless too many hyphens in term
		String[] splits = term.split("\\-");

		int limit = splits.length + 1;

		if (limit < NUM_HYPHENS4VARIANTS) {

			for (int i = 0; i < limit; i++) {
				splits = term.split("\\-", i);
				String result = "";
				for (String split : splits) {
					result += " " + split;
				}
				// System.err.println(result.trim());
				termVariants.add(result.trim());
				result = result.replaceFirst("\\-", " ");
				termVariants.add(result.trim());
			}

			termVariant = term.replaceAll("\\-", " ");
			termVariants.add(termVariant);
			termVariant = term.replaceFirst("\\-", " ");
			termVariants.add(termVariant);

			// replace hyphens with empty string iff term.length > NUM
			if (term.length() > 8) {

				splits = term.split("\\-");
				limit = splits.length + 1;
				for (int i = 0; i < limit; i++) {
					splits = term.split("\\-", i);
					String result = " ";
					for (String split : splits) {
						result += "" + split;
					}
					// System.err.println(i + " " + result);
					termVariants.add(result.trim());
					result = result.replaceFirst("\\-", "");
					termVariants.add(result.trim());
				}

				termVariant = term.replaceAll("\\-", "");
				termVariants.add(termVariant);
				termVariant = term.replaceFirst("\\-", "");
				termVariants.add(termVariant);
			}

		}
		// replace internal parentheses with ""
		// in addition: add [hyphen to ""] variants
		if (term.contains("(") && term.contains(")")) {

			termVariant = term.replaceFirst("\\(", "");
			termVariant = termVariant.replaceFirst("\\)", "");
			termVariants.add(termVariant);
			termVariant = termVariant.replaceFirst("\\-", "");
			termVariants.add(termVariant);
			termVariant = termVariant.replaceAll("\\-", "");
			termVariants.add(termVariant);

			termVariant = term.replaceAll("\\(", "");
			termVariant = termVariant.replaceAll("\\)", "");
			termVariants.add(termVariant);
			termVariant = termVariant.replaceFirst("\\-", "");
			termVariants.add(termVariant);
			termVariant = termVariant.replaceAll("\\-", "");
			termVariants.add(termVariant);

		}

		// replace white spaces with hyphens
		splits = term.split(" ");
		limit = splits.length + 1;
		for (int i = 0; i < limit; i++) {
			splits = term.split(" ", i);
			String result = "";
			for (String split : splits) {
				result += "-" + split;
			}
			result = result.substring(1).trim();
			// System.err.println(i + " " + result);
			termVariants.add(result.trim());
			result = result.replaceFirst(" ", "-");
			termVariants.add(result.trim());
		}

		termVariant = term.replaceAll(" ", "-");
		termVariants.add(termVariant);
		termVariant = term.replaceFirst(" ", "-");
		termVariants.add(termVariant);

		// genitive 's
		termVariant = term.replaceFirst("'s", "");
		termVariants.add(termVariant);
		termVariant = term.replaceFirst("'s", "s");
		termVariants.add(termVariant);

		return termVariants;
	}

	private void initStopWords(InputStream stopWordFile) throws IOException {
		stopWords = new TreeSet<String>();

		LOGGER.info("readDictionary() - adding entries from " + stopWordFile
				+ " to dictionary...");
		BufferedReader bf = new BufferedReader(new InputStreamReader(stopWordFile));
		String line = "";

		try {
			while ((line = bf.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				stopWords.add(line.trim().toLowerCase());
			}
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<String> getStopWords() {
		return stopWords;
	}

	@Override
	public boolean getUseApproximateMatching() {
		return useApproximateMatching;
	}

	@Override
	public boolean getNormalize() {
		return false;
	}

	@Override
	public boolean getTransliterate() {
		return false;
	}

	@Override
	public boolean getCaseSensitive() {
		return caseSensitive;
	}
}
