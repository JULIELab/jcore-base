package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import com.aliasi.chunk.Chunker;
import com.aliasi.dict.*;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.ibm.icu.text.Transliterator;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking;
import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * An alternative implementation because the original seemed a bit too complicated and inefficient.
 * 
 * @author faessler
 * 
 */
public class ChunkerProviderImplAlt implements ChunkerProvider, SharedResourceObject {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChunkerProviderImplAlt.class);
	public final static String PARAM_USE_APPROXIMATE_MATCHING = "UseApproximateMatching";
	public final static String PARAM_CASE_SENSITIVE = "CaseSensitive";
	public final static String PARAM_MAKE_VARIANTS = "MakeVariants";
	public final static String PARAM_STOPWORD_FILE = "StopWordFile";
	public final static String PARAM_DICTIONARY_FILE = "DictionaryFile";
	/**
	 * Parameter to indicate whether text - dictionary entries for this class - should be normalized by completely
	 * removing dashes, parenthesis, genitive 's and perhaps more. This is meant to replace the generation of term
	 * variants and cannot be used together with variation generation. If this is switched on here, it must also be
	 * switched on in the descriptor for the annotator itself!
	 */
	public final static String PARAM_NORMALIZE_TEXT = "NormalizeText";
	/**
	 * Only in effect when {@link #PARAM_NORMALIZE_TEXT} is set to <tt>true</tt>. If so, will normalize plurals
	 * found in the text by removing the training 's'. Requires annotations of the type {@link de.julielab.jcore.types.PennBioIEPOSTag}
	 * to be present in the CAS.
	 */
	public static final String PARAM_NORMALIZE_PLURAL = "NormalizePlural";
	/**
	 * Parameter to indicate whether text - dictionary entries for this class - should be transliterated, i.e. whether
	 * accents and other character variations should be stripped. If this is switched on here, it must also be switched
	 * on in the descriptor of the annotator itself!
	 */
	public final static String PARAM_TRANSLITERATE_TEXT = "TransliterateText";

	private boolean generateVariants;
	private boolean caseSensitive;
	private boolean useApproximateMatching;
	private boolean transliterate;
	private boolean normalize;
	private boolean normalizePlural;
	private InputStream dictFile;
	private InputStream stopFile;

	private AbstractDictionary<String> dict;
	private Chunker dictChunker = null;
	private final double CHUNK_SCORE = 1.0;

	private final int MIN_TERM_LENGTH = 3;
	private final double APPROX_MATCH_THRESHOLD_SCORE = 100;
	private Set<String> stopWords = new HashSet<String>();
	private String dictionaryFilePath;
	private String stopwordFilePath;

	public Chunker getChunker() {
		return dictChunker;
	}

	public boolean getNormalizePlural() {
		return normalizePlural;
	}

	public void load(DataResource resource) throws ResourceInitializationException {
		LOGGER.info("Loading configuration file from URI \"{}\" (URL: \"{}\").", resource.getUri(), resource.getUrl());
		Properties properties = new Properties();
		try {
			InputStream is;
			try {
				is = resource.getInputStream();
			} catch (NullPointerException e) {
				LOGGER.info("Couldn't get InputStream from UIMA. Trying to load the resource by classpath lookup.");
				String cpAddress = resource.getUri().toString();
				is = getClass().getResourceAsStream(cpAddress.startsWith("/") ? cpAddress : "/" + cpAddress);
				if (null == is) {
					String err =
							"Couldn't find the resource at \"" + resource.getUri()
									+ "\" neither as an UIMA external resource file nor as a classpath resource.";
					LOGGER.error(err);
					throw new ResourceInitializationException(new IllegalArgumentException(err));
				}
			}
			properties.load(is);
		} catch (IOException e) {
			LOGGER.error("Error while loading properties file", e);
			throw new ResourceInitializationException(e);
		}

		LOGGER.info("Creating dictionary chunker with " + resource.getUrl() + " properties file.");

		dictionaryFilePath = properties.getProperty(PARAM_DICTIONARY_FILE);
		if (dictionaryFilePath == null)
			throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT,
					new Object[] { PARAM_DICTIONARY_FILE });

		stopwordFilePath = properties.getProperty(PARAM_STOPWORD_FILE);
		if (stopwordFilePath == null)
			throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT,
					new Object[] { PARAM_STOPWORD_FILE });

		String generateVariantsString = properties.getProperty(PARAM_MAKE_VARIANTS);
		generateVariants = true;
		if (generateVariantsString != null)
			generateVariants = new Boolean(generateVariantsString);
		LOGGER.info("Generate variants: {}", generateVariants);

		String normalizeString = properties.getProperty(PARAM_NORMALIZE_TEXT);
		normalize = false;
		if (normalizeString != null)
			normalize = new Boolean(normalizeString);
		LOGGER.info("Normalize dictionary entries and text (i.e. completely strip dashes, parenthesis etc): {}", normalize);

		normalizePlural = Boolean.parseBoolean(properties.getProperty(PARAM_NORMALIZE_PLURAL, "false")) && normalize;
		if (normalize)
		LOGGER.info("Also normalize plural forms to singular: {}", normalizePlural);

		String transliterateString = properties.getProperty(PARAM_TRANSLITERATE_TEXT);
		transliterate = false;
		if (transliterateString != null)
			transliterate = new Boolean(transliterateString);
		LOGGER.info("Transliterate dictionary entries (i.e. transform accented characters to their base forms): {}",
				transliterate);

		String caseSensitiveString = properties.getProperty(PARAM_CASE_SENSITIVE);
		caseSensitive = false;
		if (caseSensitiveString != null)
			caseSensitive = new Boolean(caseSensitiveString);
		LOGGER.info("Case sensitive: {}", caseSensitive);

		String useApproximateMatchingString = properties.getProperty(PARAM_USE_APPROXIMATE_MATCHING);
		useApproximateMatching = false;
		if (useApproximateMatchingString != null)
			useApproximateMatching = new Boolean(useApproximateMatchingString);
		LOGGER.info("Use approximate matching: {}", useApproximateMatching);

		if (normalize && generateVariants)
			throw new ResourceInitializationException(
					new IllegalStateException(
							"MakeVariants and NormalizeText are both activated which is invalid. The two options work towards the same goal in two different ways, i.e. to recognize dictionary entry variants not given explicitly. However, the approaches are not compatible and you have to choose a single one."));

		dictFile = readStreamFromFileSystemOrClassPath(dictionaryFilePath);
		stopFile = readStreamFromFileSystemOrClassPath(stopwordFilePath);

		try {
			initStopWords(stopFile);
			readDictionary(dictFile);

			LOGGER.info("Now creating chunker.");
			long time = System.currentTimeMillis();
			if (useApproximateMatching) {
				final Set<Character> charsToDelete = new HashSet<>();
				charsToDelete.add('-');
				// charsToDelete.add('+');
				// charsToDelete.add(',');
				// charsToDelete.add('.');
				// charsToDelete.add(':');
				// charsToDelete.add(';');
				// charsToDelete.add('?');
				// charsToDelete.add('!');
				// charsToDelete.add('*');
				// charsToDelete.add('§');
				// charsToDelete.add('$');
				// charsToDelete.add('%');
				// charsToDelete.add('&');
				// charsToDelete.add('/');
				// charsToDelete.add('\\');
				// charsToDelete.add('(');
				// charsToDelete.add(')');
				// charsToDelete.add('<');
				// charsToDelete.add('>');
				// charsToDelete.add('[');
				// charsToDelete.add(']');
				// charsToDelete.add('=');
				// charsToDelete.add('\'');
				// charsToDelete.add('`');
				// charsToDelete.add('´');
				// charsToDelete.add('"');
				// charsToDelete.add('#');

				WeightedEditDistance editDistance = ApproxDictionaryChunker.TT_DISTANCE;
				editDistance = new WeightedEditDistance() {

					@Override
					public double deleteWeight(char cDeleted) {
						double ret;
						if (cDeleted == '-')
							ret = -5.0;
						else if (cDeleted == ' ' || charsToDelete.contains(cDeleted))
							ret = -10.0;
						else
							ret = -110.0;
						return ret;
					}

					@Override
					public double insertWeight(char cInserted) {
						return deleteWeight(cInserted);
					}

					@Override
					public double matchWeight(char cMatched) {
						return 0.0;
					}

					@Override
					public double substituteWeight(char cDeleted, char cInserted) {
						if (cDeleted == ' ' && cInserted == '-')
							return -2.0;
						if (cDeleted == '-' && cInserted == ' ')
							return -2.0;
						if (cDeleted == ' ' && charsToDelete.contains(cInserted))
							return -10.0;
						if (charsToDelete.contains(cDeleted) && cInserted == ' ')
							return -10.0;
						return -110.0;
					}

					@Override
					public double transposeWeight(char c1, char c2) {
						return Double.NEGATIVE_INFINITY;
					}
				};

				dictChunker =
						new ApproxDictionaryChunker((TrieDictionary<String>) dict,
								IndoEuropeanTokenizerFactory.INSTANCE, editDistance, APPROX_MATCH_THRESHOLD_SCORE);
			} else {
				dictChunker =
						new ExactDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, false, caseSensitive);
			}
			time = System.currentTimeMillis() - time;
			LOGGER.info("Building the actual chunker from the dictionary took {}ms ({}s).", time, time / 1000);

		} catch (Exception e) {
			LOGGER.error("Exception while creating chunker instance", e);
		}
	}

	private void readDictionary(InputStream dictFileStream) throws IOException, AnalysisEngineProcessException {
		long time = System.currentTimeMillis();
		if (useApproximateMatching) {
			dict = new TrieDictionary<String>();
		} else {
			dict = new MapDictionary<String>();
		}
		// now read from file and add entries
		LOGGER.info("readDictionary() - adding entries from " + dictionaryFilePath + " to dictionary...");
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(dictFileStream));
			String line = "";

			Transliterator transliterator  = Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC");

			TokenizerFactory tokenizerFactory = null;
			if (normalize)
				tokenizerFactory = new IndoEuropeanTokenizerFactory();
			while ((line = bf.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				String[] values = line.split("\t");
				if (values.length != 2) {
					LOGGER.error("readDictionary() - wrong format of line: " + line);
					throw new AnalysisEngineProcessException(AnalysisEngineProcessException.ANNOTATOR_EXCEPTION, null);
				}

				String term = values[0].trim();

				if (stopWords.contains(term.toLowerCase()))
					continue;

				if (normalize) {
					term = StringNormalizerForChunking.normalizeString(term, tokenizerFactory, transliterator).string;
				}
				if (transliterate)
					term = transliterator.transform(term);
				if (useApproximateMatching && !caseSensitive)
					term = term.toLowerCase();

				String label = values[1].trim();
				if (term.length() < MIN_TERM_LENGTH)
					continue;

				if (generateVariants) {
					if (true)
						throw new NotImplementedException(
								"In this alternative ChunkerProvider, generating variants will currently fail to adequately filter out stop words due to the transliteration and/or normalization algorithms. If you don't need those algorithms, just stick to the original ChunkerProviderImpl. Otherwise, this issue must be fixed (shouldnt be too difficult). Variants are also currently not treated with normalization/transliteration (but this is deemed to be two alternative ways to achieve a similar thing anyway)");
				} else {
					// This is a second stop-word-check but here the term has been transliterated and/or normalized. If
					// somehow the result of this was a stop word, ignore it.
					if (!stopWords.contains(term.toLowerCase()))
						dict.addEntry(new DictionaryEntry<String>(term, label, CHUNK_SCORE));
				}
			}

			time = System.currentTimeMillis() - time;
			LOGGER.info("Reading dictionary took {}ms ({}s)", time, time / 1000);
		} finally {
			if (null != bf)
				bf.close();
		}
	}

	private void initStopWords(InputStream stopFileStream) throws IOException {
		stopWords = new HashSet<String>();

		LOGGER.info("readDictionary() - adding entries from " + stopwordFilePath + " to dictionary...");
		BufferedReader bf = new BufferedReader(new InputStreamReader(stopFileStream));
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
		return normalize;
	}

	@Override
	public boolean getTransliterate() {
		return transliterate;
	}

	@Override
	public boolean getCaseSensitive() {
		return caseSensitive;

	}

	private InputStream readStreamFromFileSystemOrClassPath(String filePath) {
		InputStream is = null;
		File file = new File(filePath);
		if (file.exists()) {
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			is = getClass().getResourceAsStream(filePath.startsWith("/") ? filePath : "/" + filePath);
		}
		if (filePath.endsWith(".gz") || filePath.endsWith(".gzip"))
			try {
				is = new GZIPInputStream(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		return is;
	}
}
