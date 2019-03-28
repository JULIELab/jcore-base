package banner.eval;

import banner.eval.dataset.Dataset;
import banner.eval.uima.Util;
import banner.postprocessing.*;
import banner.postprocessing.FlattenPostProcessor.FlattenType;
import banner.tagging.CRFTagger;
import banner.tagging.FeatureSet;
import banner.tagging.TagFormat;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Mention.MentionType;
import banner.types.Sentence;
import banner.types.Sentence.OverlapOption;
import banner.types.Token;
import banner.util.CollectionsRand;
import banner.util.RankedList;
import dragon.nlp.tool.HeppleTagger;
import dragon.nlp.tool.MedPostTagger;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.*;
import java.util.*;

public class BANNER {

	private enum Function {
		help, tag, test, train, eval5by2, eval10Fold, describeDataset, testDict, coordEllipsis;
	}

	public static void main(String[] args) throws ConfigurationException,
			IOException {
		if (args.length == 0) {
			System.out
					.println("Usage: banner.sh <command> <configuration> <parameters>");
			System.out.println("Exceute \"banner.sh help\" for details");
		} else {
			// TODO check input & provide --help output
			switch (Function.valueOf(args[0])) {
			case help:
				System.out.println("Commands:");
				System.out.println("help: Prints this help text");
				System.out
						.println("tag: Uses a trained model to tag sentences from an input file");
				System.out
						.println("\tUsage: banner.sh tag config.xml sentences.txt");
				System.out
						.println("\t   Or: banner.sh tag config.xml sentences.txt 0.2");
				System.out
						.println("test: Evaluates a previously trained model against the test data set up in the config file");
				System.out.println("\tUsage: banner.sh test config.xml");
				System.out.println("\t   Or: banner.sh test config.xml 0.2");
				System.out
						.println("train: Uses the training data and configuration from the config file to create a new model");
				System.out.println("\tUsage: banner.sh train config.xml");
				System.out.println("\t   Or: banner.sh train config.xml 0.2");
				System.out
						.println("eval5by2: Performs a 5 by 2 cross-validation on the data set up in the config file");
				System.out.println("\tUsage: banner.sh eval5by2 config.xml");
				System.out
						.println("\t   Or: banner.sh eval5by2 config.xml 0.2");
				System.out
						.println("eval10Fold: Performs a 10 fold cross-validation on the data set up in the config file");
				System.out.println("\tUsage: banner.sh eval10Fold config.xml");
				System.out
						.println("\t   Or: banner.sh eval10Fold config.xml 0.2");
				break;
			case tag:
				tag(new XMLConfiguration(args[1]), args[2],
						args.length > 3 ? Double.valueOf(args[3]) : null);
				break;
			case test:
				test(new XMLConfiguration(args[1]));
				break;
			case train:
				train(new XMLConfiguration(args[1]),
						args.length > 2 ? Double.valueOf(args[2]) : null);
				break;
			case eval5by2:
				eval5by2(new XMLConfiguration(args[1]),
						args.length > 2 ? Double.valueOf(args[2]) : null);
				break;
			case eval10Fold:
				eval10Fold(new XMLConfiguration(args[1]),
						args.length > 2 ? Double.valueOf(args[2]) : null);
				break;
			case describeDataset:
				describeDataset(new XMLConfiguration(args[1]));
				break;
			case testDict:
				testDict(new XMLConfiguration(args[1]));
				break;
			default:
				System.out.println("Unrecognized command \"" + args[0]
						+ "\"; use \"help\" for a list of valid commands");
				break;
			}
		}
	}

	private static void tag(HierarchicalConfiguration config,
			String sentenceFilename, Double percentage) throws IOException {
		long start = System.currentTimeMillis();
		Tokenizer tokenizer = getTokenizer(config);
		DictionaryTagger dictionary = getDictionary(config);
		EngLemmatiser lemmatiser = getLemmatiser(config);
		Tagger posTagger = getPosTagger(config);
		PostProcessor postProcessor = getPostProcessor(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");
		System.out.println("Model: " + modelFilename);
		CRFTagger tagger = CRFTagger.load(BANNER.class.getClass().getResourceAsStream(modelFilename), lemmatiser,
				posTagger, dictionary);
		System.out.println("Completed input: "
				+ (System.currentTimeMillis() - start) + "ms");

		// TODO NEW Data input format: <identifier>\t<sentence text>
		// TODO Data output format: <identifier>\t<type>\t<start>\t<end>\t<text>
		// TODO Set this to use a Dataset, then use a random subset for the
		// percentage
		BufferedReader reader = new BufferedReader(new FileReader(
				sentenceFilename));
		String line = reader.readLine();
		while (line != null) {
			line = line.trim();
			if (line.length() > 0) {
				// TODO How to output results?
				String[] split = line.split("\\t");
				Sentence sentence = new Sentence(split[0], "", split[1]);
				sentence = process(tagger, tokenizer, postProcessor, sentence);
				for (Mention mention : sentence.getMentions()) {
					StringBuilder output = new StringBuilder();
					output.append(split[0]); // sentence identifier
					output.append("\t");
					output.append(mention.getEntityType());
					output.append("\t");
					output.append(mention.getStartChar());
					output.append("\t");
					output.append(mention.getEndChar());
					output.append("\t");
					output.append(mention.getText());
					System.out.println(output.toString());
				}
			}
			line = reader.readLine();
		}
		reader.close();
	}

	public static void train(HierarchicalConfiguration config,
			Double percentage) throws ConfigurationException, IOException {
		long start = System.currentTimeMillis();
		Dataset dataset = getDataset(config);
		TagFormat tagFormat = getTagFormat(config);
		DictionaryTagger dictionary = getDictionary(config);
		int crfOrder = getCRFOrder(config);
		System.out.println("tagformat=" + tagFormat);
		System.out.println("crfOrder=" + crfOrder);
		EngLemmatiser lemmatiser = getLemmatiser(config);
		Tagger posTagger = getPosTagger(config);
		Set<MentionType> mentionTypes = getMentionTypes(config);
		OverlapOption sameTypeOverlapOption = getSameTypeOverlapOption(config);
		OverlapOption differentTypeOverlapOption = getDifferentTypeOverlapOption(config);
		String simFindFilename = getSimFindFilename(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");
		Set<Sentence> sentences = dataset.getSentences();
		if (percentage != null)
			sentences = CollectionsRand.randomSubset(sentences, percentage);
		BANNER.logInput(sentences, config);
		System.out.println("Completed input: "
				+ (System.currentTimeMillis() - start) + "ms");

		System.out.println("Training data loaded, starting training");
		FeatureSet featureSet = new FeatureSet(tagFormat, lemmatiser,
				posTagger, dictionary, simFindFilename, mentionTypes,
				sameTypeOverlapOption, differentTypeOverlapOption);
		CRFTagger tagger = CRFTagger.train(sentences, crfOrder, tagFormat,
				featureSet);
		System.out.println("Training complete, saving model");
		tagger.write(new File(modelFilename));
	}

	public static void test(HierarchicalConfiguration config)
			throws ConfigurationException, IOException {
		long start = System.currentTimeMillis();
		Dataset dataset = getDataset(config);
		DictionaryTagger dictionary = getDictionary(config);
		EngLemmatiser lemmatiser = getLemmatiser(config);
		Tagger posTagger = getPosTagger(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");
		System.out.println("Model: " + modelFilename);
		BANNER.logInput(dataset.getSentences(), config);
		System.out.println("Completed input: "
				+ (System.currentTimeMillis() - start));

		CRFTagger tagger = CRFTagger.load(new FileInputStream(new File(modelFilename)), lemmatiser,
				posTagger, dictionary);
		Performance performance = test(dataset, tagger, config);
		performance.print();
		System.out.println("Elapsed time: "
				+ (System.currentTimeMillis() - start) + "ms");
	}

	public static void eval5by2(HierarchicalConfiguration config,
			Double percentage) throws ConfigurationException, IOException {
		long start = System.currentTimeMillis();
		Dataset dataset = getDataset(config);

		Map<EntityType, Integer> typeCounts = dataset.getTypeCounts();
		for (EntityType type : typeCounts.keySet())
			System.out.println(type.toString() + ", count="
					+ typeCounts.get(type));

		TagFormat tagFormat = getTagFormat(config);
		DictionaryTagger dictionary = getDictionary(config);
		int crfOrder = getCRFOrder(config);
		EngLemmatiser lemmatiser = getLemmatiser(config);
		Tagger posTagger = getPosTagger(config);
		Set<MentionType> mentionTypes = getMentionTypes(config);
		OverlapOption sameTypeOverlapOption = getSameTypeOverlapOption(config);
		OverlapOption differentTypeOverlapOption = getDifferentTypeOverlapOption(config);
		String simFindFilename = getSimFindFilename(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");

		for (int run = 0; run < 5; run++) {
			start = System.currentTimeMillis();
			List<Dataset> splitDataset = dataset.split(2);
			System.out.println("Created folds for run " + run + ": "
					+ (System.currentTimeMillis() - start));

			for (int cross = 0; cross < 2; cross++) {
				Dataset dataset_A = splitDataset.get(cross);
				Dataset dataset_B = splitDataset.get(cross == 0 ? 1 : 0);
				Set<Sentence> sentences_A = dataset_A.getSentences();
				if (percentage != null)
					sentences_A = CollectionsRand.randomSubset(sentences_A,
							percentage.doubleValue());
				String filenameSuffix = Integer.toString(run)
						+ Integer.toString(cross);
				logInput(sentences_A, config, filenameSuffix);

				// Train on fold A
				start = System.currentTimeMillis();
				System.out.println("\tTraining data loaded, starting training");
				FeatureSet featureSet = new FeatureSet(tagFormat, lemmatiser,
						posTagger, dictionary, simFindFilename, mentionTypes,
						sameTypeOverlapOption, differentTypeOverlapOption);
				CRFTagger tagger = CRFTagger.train(sentences_A, crfOrder,
						tagFormat, featureSet);
				System.out.println("Completed training for run " + run
						+ " cross " + cross + ": "
						+ (System.currentTimeMillis() - start));
				tagger.write(new File(
						getFilename(modelFilename, filenameSuffix)));
				System.gc();

				// Test on fold B & output results
				start = System.currentTimeMillis();
				Performance performance = test(dataset_B, tagger, config,
						filenameSuffix);
				performance.print();
				System.out.println("Completed testing for run " + run
						+ " cross " + cross + ": "
						+ (System.currentTimeMillis() - start) + "ms");

				tagger = null;
				System.gc();
			}
		}
	}

	private static class DatasetCombiner extends Dataset {

		public DatasetCombiner(Collection<Dataset> datasets) {
			super();
			for (Dataset dataset : datasets)
				sentences.addAll(dataset.getSentences());
		}

		@Override
		public List<Dataset> split(int n) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void load(HierarchicalConfiguration config) {
			throw new UnsupportedOperationException();
		}

	}

	public static void eval10Fold(HierarchicalConfiguration config,
			Double percentage) throws ConfigurationException, IOException {
		long start = System.currentTimeMillis();
		Dataset dataset = getDataset(config);

		Map<EntityType, Integer> typeCounts = dataset.getTypeCounts();
		for (EntityType type : typeCounts.keySet())
			System.out.println(type.toString() + ", count="
					+ typeCounts.get(type));

		TagFormat tagFormat = getTagFormat(config);
		DictionaryTagger dictionary = getDictionary(config);
		int crfOrder = getCRFOrder(config);
		EngLemmatiser lemmatiser = getLemmatiser(config);
		Tagger posTagger = getPosTagger(config);
		Set<MentionType> mentionTypes = getMentionTypes(config);
		OverlapOption sameTypeOverlapOption = getSameTypeOverlapOption(config);
		OverlapOption differentTypeOverlapOption = getDifferentTypeOverlapOption(config);
		String simFindFilename = getSimFindFilename(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");

		start = System.currentTimeMillis();
		List<Dataset> splitDataset = dataset.split(10);
		System.out.println("Created folds: "
				+ (System.currentTimeMillis() - start));

		for (int cross = 0; cross < 10; cross++) {
			List<Dataset> datasets = new ArrayList<Dataset>();
			for (int i = 0; i < 10; i++) {
				if (i != cross)
					datasets.add(splitDataset.get(i));
				System.out.println(splitDataset.get(i).getSentences().size());
			}
			Dataset dataset_A = new DatasetCombiner(datasets);
			Dataset dataset_B = splitDataset.get(cross);
			Set<Sentence> sentences_A = dataset_A.getSentences();
			if (percentage != null)
				sentences_A = CollectionsRand.randomSubset(sentences_A,
						percentage.doubleValue());
			logInput(sentences_A, config, Integer.toString(cross));

			// Train on fold A
			start = System.currentTimeMillis();
			System.out.println("\tTraining data loaded, starting training");
			FeatureSet featureSet = new FeatureSet(tagFormat, lemmatiser,
					posTagger, dictionary, simFindFilename, mentionTypes,
					sameTypeOverlapOption, differentTypeOverlapOption);
			CRFTagger tagger = CRFTagger.train(sentences_A, crfOrder,
					tagFormat, featureSet);
			System.out.println("Completed training for cross " + cross + ": "
					+ (System.currentTimeMillis() - start));
			tagger.write(new File(getFilename(modelFilename,
					Integer.toString(cross))));
			System.gc();

			// Test on fold B & output results
			start = System.currentTimeMillis();
			Performance performance = test(dataset_B, tagger, config,
					Integer.toString(cross));
			performance.print();
			System.out.println("Completed testing for cross " + cross + ": "
					+ (System.currentTimeMillis() - start));

			tagger = null;
			System.gc();

		}
	}

	private static void describeDataset(HierarchicalConfiguration config)
			throws ConfigurationException, IOException {
		Dataset dataset = getDataset(config);
		Set<Sentence> sentences = dataset.getSentences();
		BANNER.logInput(sentences, config);

		int tokenCount = 0;
		int mentionCount = 0;
		int[] sentenceLength = new int[100];
		Integer[] mentionFrequency = new Integer[15];
		for (int i = 0; i < mentionFrequency.length; i++)
			mentionFrequency[i] = new Integer(0);
		Integer[] mentionLength = new Integer[50];
		for (int i = 0; i < mentionLength.length; i++)
			mentionLength[i] = new Integer(0);
		Set<String> mentionTexts = new HashSet<String>();
		DictionaryTagger d = new DictionaryTagger();

		Map<String, Count> tokenFrequencies = new HashMap<String, Count>();
		Map<String, Count> tokenFrequenciesInMention = new HashMap<String, Count>();
		for (Sentence sentence : sentences) {
			int numTokens = sentence.getTokens().size();
			if (numTokens < sentenceLength.length)
				sentenceLength[numTokens]++;
			tokenCount += numTokens;
			for (Token token : sentence.getTokens()) {
				Count count = tokenFrequencies.get(token.getText());
				if (count == null) {
					count = new Count();
					tokenFrequencies.put(token.getText(), count);
				}
				count.incr();
			}
			List<Mention> mentions = sentence.getMentions();
			int numMentions = mentions.size();
			mentionCount += numMentions;
			if (numMentions < mentionFrequency.length)
				mentionFrequency[numMentions] = new Integer(
						mentionFrequency[numMentions].intValue() + 1);
			for (Mention mention : mentions) {
				if (mention.length() < mentionLength.length)
					mentionLength[mention.length()] += 1;
				mentionTexts.add(mention.getText());
				List<String> tokens = new ArrayList<String>();
				for (Token token : mention.getTokens())
					tokens.add(token.getText());
				d.add(tokens, Collections.singleton(mention.getEntityType()));

				for (Token token : mention.getTokens()) {
					Count count = tokenFrequenciesInMention
							.get(token.getText());
					if (count == null) {
						count = new Count();
						tokenFrequenciesInMention.put(token.getText(), count);
					}
					count.incr();
				}

			}
		}
		System.out.println();
		System.out.println("Number of sentences: " + sentences.size());
		System.out.println("Number of tokens: " + tokenCount);
		System.out.println("Number of mentions: " + mentionCount);
		System.out.println("Number of sentences per sentence length: "
				+ Arrays.toString(sentenceLength));
		System.out.println("Number of sentences per mention frequency: "
				+ Arrays.asList(mentionFrequency));
		System.out.println("Number of mentions per mention length: "
				+ Arrays.asList(mentionLength));
		System.out.println("Number of unique mention texts: "
				+ mentionTexts.size());
		for (Sentence sentence : sentences) {
			Sentence sentence2 = sentence.copy(true, false);
			d.tag(sentence2);
		}

		System.out.println("Token frequencies:");
		RankedList<String> tokenFrequenciesList = new RankedList<String>(100);
		int[] tokenFreqenciesNonMention = new int[100];
		int[] tokenFreqenciesMention = new int[100];
		for (String token : tokenFrequencies.keySet()) {
			int count = tokenFrequencies.get(token).getCount();
			int countInMention = tokenFrequenciesInMention.get(token) == null ? 0
					: tokenFrequenciesInMention.get(token).getCount();
			int countNonMention = Math.max(0, count - countInMention);
			if (countNonMention < tokenFreqenciesNonMention.length)
				tokenFreqenciesNonMention[countNonMention]++;
			if (countInMention < tokenFreqenciesMention.length)
				tokenFreqenciesMention[countInMention]++;
			if (count > 5)
				tokenFrequenciesList.add(1.0 - (double) countInMention / count,
						token);
		}
		System.out
				.println("Number of tokens which appear in mentions with a specific frequency:"
						+ Arrays.toString(tokenFreqenciesMention));
		System.out
				.println("Number of tokens which appear non mention with a specific frequency:"
						+ Arrays.toString(tokenFreqenciesNonMention));
		// for (int i = 0; i < tokenFrequenciesList.size(); i++)
		// {
		// String token = tokenFrequenciesList.getObject(i);
		// int count = tokenFrequencies.get(token).getCount();
		// int countInMention = tokenFrequenciesInMention.get(token) == null ? 0
		// : tokenFrequenciesInMention
		// .get(token).getCount();
		// System.out.println(token + "\t" + countInMention + "\t" + count);
		// }
	}

	private static class Count {
		private int count;

		public Count() {
			count = 0;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void incr() {
			count++;
		}
	}

	public static void testDict(HierarchicalConfiguration config)
			throws ConfigurationException, IOException {
		// TODO Want to just call test() and let the config sort out which
		// tagger to use
		long start = System.currentTimeMillis();
		Dataset dataset = getDataset(config);
		DictionaryTagger dictionary = getDictionary(config);
		BANNER.logInput(dataset.getSentences(), config);
		System.out.println("Completed input: "
				+ (System.currentTimeMillis() - start) + "ms");

		// Test & output results
		start = System.currentTimeMillis();
		Performance performance = test(dataset, dictionary, config);
		performance.print();
		System.out.println("Completed tagging: "
				+ (System.currentTimeMillis() - start));
	}

	public static void logInput(Set<Sentence> sentences,
			HierarchicalConfiguration config) throws IOException {
		logInput(sentences, config, null);
	}

	private static void logInput(Set<Sentence> sentences,
			HierarchicalConfiguration config, String filenameSuffix)
			throws IOException {
		// TODO Handle nulls
		TagFormat tagFormat = getTagFormat(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String idInputFilename = getFilename(
				localConfig.getString("idInputFilename"), filenameSuffix);
		String rawInputFilename = getFilename(
				localConfig.getString("rawInputFilename"), filenameSuffix);
		String trainingInputFilename = getFilename(
				localConfig.getString("trainingInputFilename"), filenameSuffix);
		PrintWriter idFile = new PrintWriter(new BufferedWriter(new FileWriter(
				idInputFilename)));
		PrintWriter rawFile = new PrintWriter(new BufferedWriter(
				new FileWriter(rawInputFilename)));
		PrintWriter trainingFile = new PrintWriter(new BufferedWriter(
				new FileWriter(trainingInputFilename)));
		for (Sentence sentence : sentences) {
			idFile.println(sentence.getSentenceId());
			rawFile.println(sentence.getText());
			trainingFile.println(getTrainingText(sentence, tagFormat,
					EnumSet.of(MentionType.Required), OverlapOption.Raw,
					OverlapOption.Raw));
		}
		idFile.close();
		rawFile.close();
		trainingFile.close();
	}

	public static String getTrainingText(Sentence sentence, TagFormat format,
			Set<MentionType> mentionTypes, OverlapOption sameType,
			OverlapOption differentType) {
		StringBuilder trainingText = new StringBuilder();
		List<String> labels = sentence.getTokenLabels(format, mentionTypes,
				sameType, differentType);
		List<Token> tokens = sentence.getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			trainingText.append(token.getText());
			trainingText.append("|");
			trainingText.append(labels.get(i));
			trainingText.append(" ");
		}
		return trainingText.toString().trim();
	}

	private static String getFilename(String originalFilename,
			String filenameSuffix) {
		if (originalFilename == null)
			return null;
		if (filenameSuffix == null)
			return originalFilename;
		int period = originalFilename.lastIndexOf(".");
		String name = originalFilename;
		String extension = "";
		if (period != -1) {
			name = originalFilename.substring(0, period);
			extension = originalFilename.substring(period);
		}
		return name + filenameSuffix + extension;
	}

	public static void outputMentions(Sentence sentence,
			PrintWriter mentionOutputFile, boolean onlyNonBlank,
			boolean ignoreWhitespace) {
		// FIXME What is onlyNonBlank for???
		if (onlyNonBlank) {
			List<Token> tokens = sentence.getTokens();
			int charCount = 0;
			for (int i = 0; i < tokens.size(); i++) {
				List<Mention> mentions = sentence.getMentions(tokens.get(i),
						EnumSet.of(MentionType.Required));
				assert mentions.size() == 0 || mentions.size() == 1;
				Mention mention = null;
				if (mentions.size() > 0)
					mention = mentions.get(0);
				if (mention != null && i == mention.getStart()) {
					mentionOutputFile.print(sentence.getSentenceId());
					mentionOutputFile.print("|");
					mentionOutputFile.print(charCount);
					mentionOutputFile.print(" ");
				}
				charCount += tokens.get(i).length();
				if (mention != null && i == mention.getEnd() - 1) {
					mentionOutputFile.print(charCount - 1);
					mentionOutputFile.print("|");
					mentionOutputFile.println(mention.getText());
				}
			}
		} else {
			for (Mention mention : sentence.getMentions(MentionType.Found)) {
				mentionOutputFile.print(sentence.getSentenceId());
				mentionOutputFile.print("|");
				mentionOutputFile.print(mention.getStartChar(ignoreWhitespace));
				mentionOutputFile.print(" ");
				mentionOutputFile.print(mention.getEndChar(ignoreWhitespace));
				mentionOutputFile.print("|");
				mentionOutputFile.println(mention.getText());
			}
		}
	}

	public static Performance test(Dataset dataset,
			banner.tagging.Tagger tagger, HierarchicalConfiguration config)
			throws IOException {
		return test(dataset, tagger, config, null);
	}

	public static Performance test(Dataset dataset,
			banner.tagging.Tagger tagger, HierarchicalConfiguration config,
			String filenameSuffix) throws IOException {
		TagFormat tagFormat = getTagFormat(config);
		Tokenizer tokenizer = getTokenizer(config);
		PostProcessor postProcessor = getPostProcessor(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());

		// TODO Handle null filenames by using a NullWriter
		// instead of the BufferedWriter & FileWriter
		String outputFilename = getFilename(
				localConfig.getString("outputFilename"), filenameSuffix);
		String mentionFilename = getFilename(
				localConfig.getString("mentionFilename"), filenameSuffix);
		String inContextAnalysisFilename = getFilename(
				localConfig.getString("inContextAnalysisFilename"),
				filenameSuffix);
		PrintWriter outputFile = new PrintWriter(new BufferedWriter(
				new FileWriter((outputFilename))));
		PrintWriter mentionFile = new PrintWriter(new BufferedWriter(
				new FileWriter((mentionFilename))));
		PrintWriter inContextAnalysisFile = null;
		if (inContextAnalysisFilename != null)
			inContextAnalysisFile = new PrintWriter(new BufferedWriter(
					new FileWriter(inContextAnalysisFilename)));

		System.out.println("\tTagging sentences");
		if (inContextAnalysisFile != null)
			inContextAnalysisFile.println("<html><body>");
		int count = 0;
		Performance performance = new Performance(MatchCriteria.Strict);
		try {
			for (Sentence sentence : dataset.getSentences()) {
				if (count % 1000 == 0)
					System.out.println(count);
				Sentence sentence2 = process(tagger, tokenizer, postProcessor,
						sentence);
				outputFile.println(getTrainingText(sentence2, tagFormat,
						EnumSet.of(MentionType.Required), OverlapOption.Raw,
						OverlapOption.Raw));
				outputMentions(sentence2, mentionFile, false, true);
				if (inContextAnalysisFile != null)
					outputAnalysis(sentence, sentence2, inContextAnalysisFile,
							false);
				performance.update(sentence, sentence2);
				count++;
			}
		} finally {
			outputFile.close();
			mentionFile.close();
			if (inContextAnalysisFile != null) {
				inContextAnalysisFile.println("</body></html>");
				inContextAnalysisFile.close();
			}
		}
		return performance;
	}

	public static Sentence process(banner.tagging.Tagger tagger,
			Tokenizer tokenizer, PostProcessor postProcessor, Sentence sentence) {
		// TODO Solidify ability to separate found/required/allowed mentions
		// TODO Then use original Sentence for tagging instead of copy
		// TODO Can we make tokenization and post-processing part of the MALLET
		// pipe?
		Sentence sentence2 = sentence.copy(false, false);
		tokenizer.tokenize(sentence2);
		tagger.tag(sentence2);
		postProcessor.postProcess(sentence2);
		return sentence2;
	}

	private enum FontColor {
		Black, Blue, Green, Red, Purple;

		@Override
		public String toString() {
			return name().toLowerCase();
		}

		public String changeColor(FontColor newColor) {
			StringBuffer str = new StringBuffer();
			if (!equals(newColor) && !equals(Black))
				str.append("</font>");
			str.append(" ");
			if (!equals(newColor) && !newColor.equals(Black))
				str.append("<font color=\"" + newColor.toString() + "\">");
			return str.toString();
		}
	}

	public enum MatchCriteria {
		Strict, Left, Right, LeftOrRight, Approximate, Partial;
	}

	public static class Performance {

		private PerformanceData overall;
		private Map<EntityType, PerformanceData> perMention;
		private Map<String, PerformanceData> perText;

		public Performance(MatchCriteria matchCriteria) {
			// TODO Implement
			if (matchCriteria != MatchCriteria.Strict)
				throw new IllegalArgumentException("Not implemented");
			overall = new PerformanceData();
			perMention = new HashMap<EntityType, PerformanceData>();
			perText = new HashMap<String, PerformanceData>();
		}

		private PerformanceData getMentionPerformanceData(EntityType type) {
			PerformanceData performanceData = perMention.get(type);
			if (performanceData == null) {
				performanceData = new PerformanceData();
				perMention.put(type, performanceData);
			}
			return performanceData;
		}

		private PerformanceData getTextPerformanceData(String text) {
			PerformanceData performanceData = perText.get(text);
			if (performanceData == null) {
				performanceData = new PerformanceData();
				perText.put(text, performanceData);
			}
			return performanceData;
		}

		public void update(Sentence sentenceRequired, Sentence sentenceFound) {
			// TODO Write these TP/*TP/FP/FN to file
			Set<Mention> mentionsNotFound = new HashSet<Mention>(
					sentenceRequired.getMentions(MentionType.Required));
			List<Mention> mentionsAllowed = sentenceRequired
					.getMentions(MentionType.Allowed);
			List<Mention> mentionsFound = sentenceFound
					.getMentions(MentionType.Found);
			for (Mention mention : mentionsFound) {
				boolean found = false;
				// String sentenceTag = mention.getSentence().getId().getId();
				if (mentionsNotFound.contains(mention)) {
					// System.out.println("TP|" + sentenceTag + "|" +
					// mention.getText() + "|" + mention.getText());
					mentionsNotFound.remove(mention);
					found = true;
					overall.tp++;
					getMentionPerformanceData(mention.getEntityType()).tp++;
					getTextPerformanceData(mention.getText()).tp++;
				} else if (mentionsAllowed.contains(mention)) {
					found = true;
					for (Mention mentionRequired : new HashSet<Mention>(
							mentionsNotFound)) {
						if (mention.overlaps(mentionRequired)) {
							mentionsNotFound.remove(mentionRequired);
							// System.out.println("*TP|" + sentenceTag + "|" +
							// mentionRequired.getText() + "|"
							// + mention.getText());
							overall.tp++;
							getMentionPerformanceData(mentionRequired
									.getEntityType()).tp++;
							getTextPerformanceData(mentionRequired.getText()).tp++;
						}
					}
				}
				if (!found) {
					// System.out.println("FP|" + sentenceTag + "|" +
					// mention.getText());
					overall.fp++;
					getMentionPerformanceData(mention.getEntityType()).fp++;
					getTextPerformanceData(mention.getText()).fp++;
				}
			}
			for (Mention mentionNotFound : mentionsNotFound) {
				// String sentenceTag =
				// mentionNotFound.getSentence().getId().getId();
				// System.out.println("FN|" + sentenceTag + "|" +
				// mentionNotFound.getText());
				overall.fn++;
				getMentionPerformanceData(mentionNotFound.getEntityType()).fn++;
				getTextPerformanceData(mentionNotFound.getText()).fn++;
			}
		}

		public PerformanceData getOverall() {
			return overall;
		}

		public Map<EntityType, PerformanceData> getPerMention() {
			return Collections.unmodifiableMap(perMention);
		}

		public Map<String, PerformanceData> getPerText() {
			return Collections.unmodifiableMap(perText);
		}

		public void print() {
			System.out.println("OVERALL: ");
			overall.print();
			for (EntityType type : perMention.keySet()) {
				System.out.println();
				System.out.println("TYPE: \"" + type.getText() + "\"");
				perMention.get(type).print();
			}
			// TODO Make per-type configurable
			// for (String text : perText.keySet())
			// {
			// PerformanceData performanceData = perText.get(text);
			// if (performanceData.fn > performanceData.tp || performanceData.fp
			// > performanceData.tp)
			// {
			// System.out.println();
			// System.out.println("TEXT: \"" + text + "\"");
			// performanceData.print();
			// }
			// }
		}

	}

	public static class PerformanceData {
		int tp;
		int fp;
		int fn;

		public PerformanceData() {
			tp = 0;
			fp = 0;
			fn = 0;
		}

		public double getPrecision() {
			return (double) tp / (tp + fp);
		}

		public double getRecall() {
			return (double) tp / (tp + fn);
		}

		public double getFMeasure() {
			double p = getPrecision();
			double r = getRecall();
			return 2.0 * p * r / (p + r);
		}

		public void print() {
			System.out.println("TP: " + tp);
			System.out.println("FP: " + fp);
			System.out.println("FN: " + fn);
			System.out.println("precision: " + getPrecision());
			System.out.println("   recall: " + getRecall());
			System.out.println("f-measure: " + getFMeasure());
		}
	}

	private static void outputAnalysis(Sentence sentenceRequired,
			Sentence sentenceFound, PrintWriter mentionOutputFile,
			boolean outputIfCorrect) {
		Sentence sentenceRequired2 = sentenceRequired.copy(true, true);
		FlattenPostProcessor fpp = new FlattenPostProcessor(FlattenType.Union);
		fpp.postProcess(sentenceRequired2);
		List<Mention> mentionsAllowed = sentenceRequired2
				.getMentions(MentionType.Allowed);
		Set<Mention> mentionsFoundCorrect = new HashSet<Mention>();
		Set<Mention> mentionsFoundIncorrect = new HashSet<Mention>();
		Set<Mention> mentionsNotFound = new HashSet<Mention>();
		mentionsNotFound.addAll(sentenceRequired2
				.getMentions(MentionType.Required));
		for (Mention mention : sentenceFound.getMentions(MentionType.Required)) {
			boolean found = false;
			if (mentionsNotFound.contains(mention)) {
				mentionsNotFound.remove(mention);
				mentionsFoundCorrect.add(mention);
				found = true;
			} else if (mentionsAllowed.contains(mention)) {
				mentionsFoundCorrect.add(mention);
				found = true;
				for (Mention mentionRequired : new HashSet<Mention>(
						mentionsNotFound))
					if (mention.overlaps(mentionRequired))
						mentionsNotFound.remove(mentionRequired);
			}
			if (!found)
				mentionsFoundIncorrect.add(mention);
		}

		// Need to handle five cases:
		// --------------------------
		// Token is not part of any mention
		// Token is part of a mention from mentionsFoundCorrect
		// Token is part of a mention from mentionsFoundIncorrect
		// Token is part of a mention from mentionsNotFound
		// Token is part of a mention from BOTH mentionsFoundIncorrect and
		// mentionsNotFound

		boolean foundError = false;
		StringBuffer analysis = new StringBuffer(sentenceFound.getSentenceId());
		FontColor currentColor = FontColor.Black;
		List<Token> tokens = sentenceFound.getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			boolean inFoundCorrect = false;
			for (Mention mention : mentionsFoundCorrect)
				inFoundCorrect |= mention.contains(i);
			boolean inFoundIncorrect = false;
			for (Mention mention : mentionsFoundIncorrect)
				inFoundIncorrect |= mention.contains(i);
			boolean inNotFound = false;
			for (Mention mention : mentionsNotFound)
				inNotFound |= mention.contains(i);
			foundError |= inNotFound || inFoundIncorrect;
			if (inFoundCorrect) {
				if (inFoundIncorrect || inNotFound) {
					System.out.println("=============");
					System.out.println("inFoundIncorrect: " + inFoundIncorrect);
					System.out.println("inNotFound: " + inNotFound);
					System.out.println(sentenceFound.getSentenceId());
					System.out.println(sentenceFound.getText());
					Mention badMention = sentenceFound.getMentions(
							tokens.get(i), EnumSet.of(MentionType.Required))
							.get(0);
					System.out.println("badMention: " + badMention);
					System.out
							.println("sentenceFound.getMentions().contains(): "
									+ sentenceFound.getMentions(
											MentionType.Required).contains(
											badMention));
					System.out.println("mentionsRequired.contains(): "
							+ sentenceRequired
									.getMentions(MentionType.Required)
									.contains(badMention));
					System.out.println("mentionsAllowed.contains(): "
							+ mentionsAllowed.contains(badMention));
					System.out.println("mentionsFoundCorrect.contains(): "
							+ mentionsFoundCorrect.contains(badMention));
					System.out.println("mentionsFoundIncorrect.contains(): "
							+ mentionsFoundIncorrect.contains(badMention));
					System.out.println("mentionsNotFound.contains(): "
							+ mentionsNotFound.contains(badMention));
					System.out.println("sentenceFound.getMentions(): "
							+ sentenceFound.getMentions(MentionType.Required));
					System.out.println("mentionsFoundCorrect: "
							+ mentionsFoundCorrect);
					System.out.println("mentionsFoundIncorrect: "
							+ mentionsFoundIncorrect);
					System.out.println("mentionsNotFound: " + mentionsNotFound);
					System.out.println("=============");
				}
				assert !inFoundIncorrect;
				assert !inNotFound;
				analysis.append(currentColor.changeColor(FontColor.Green));
				currentColor = FontColor.Green;
			} else if (inFoundIncorrect && inNotFound) {
				analysis.append(currentColor.changeColor(FontColor.Purple));
				currentColor = FontColor.Purple;
			} else if (inFoundIncorrect) {
				analysis.append(currentColor.changeColor(FontColor.Red));
				currentColor = FontColor.Red;
			} else if (inNotFound) {
				analysis.append(currentColor.changeColor(FontColor.Blue));
				currentColor = FontColor.Blue;
			} else {
				analysis.append(currentColor.changeColor(FontColor.Black));
				currentColor = FontColor.Black;
			}
			analysis.append(tokens.get(i).getText());
		}
		analysis.append(currentColor.changeColor(FontColor.Black));
		analysis.append("<br>");
		if (foundError || outputIfCorrect)
			mentionOutputFile.println(analysis);
	}

	public static Dataset getDataset(HierarchicalConfiguration config) {
		Tokenizer tokenizer = getTokenizer(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String datasetName = localConfig.getString("datasetName");
		Dataset dataset = null;
		try {
			dataset = (Dataset) Class.forName(datasetName).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		dataset.setTokenizer(tokenizer);
		dataset.load(config);
		return dataset;
	}

	private static TagFormat getTagFormat(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		return TagFormat.valueOf(localConfig.getString("tagFormat"));
	}

	public static Tokenizer getTokenizer(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		try {
			String tokenizerName = localConfig.getString("tokenizer");
			System.out.println("reading tokenizer: " + tokenizerName);
			Tokenizer tokenizer = (Tokenizer) Class.forName(tokenizerName)
					.newInstance();
			return tokenizer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static DictionaryTagger getDictionary(
			HierarchicalConfiguration config) {
		Tokenizer tokenizer = getTokenizer(config);
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String dictionaryName = localConfig.getString("dictionaryTagger");
		if (dictionaryName == null)
			return null;
		DictionaryTagger dictionary = null;
		try {
			dictionary = (DictionaryTagger) Class.forName(dictionaryName)
					.newInstance();
			dictionary.configure(config, tokenizer);
			dictionary.load(config);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return dictionary;
	}

	public static PostProcessor getPostProcessor(
			HierarchicalConfiguration config) {
		// Guaranteed not to be null
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		SequentialPostProcessor postProcessor = new SequentialPostProcessor();
		if (localConfig.containsKey("useParenthesisPostProcessing"))
			if (localConfig.getBoolean("useParenthesisPostProcessing"))
				postProcessor.addPostProcessor(new ParenthesisPostProcessor());
		if (localConfig.containsKey("useLocalAbbreviationPostProcessing"))
			if (localConfig.getBoolean("useLocalAbbreviationPostProcessing"))
				postProcessor
						.addPostProcessor(new LocalAbbreviationPostProcessor());
		return postProcessor;
	}

	private static int getCRFOrder(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		return localConfig.getInt("crfOrder");
	}

	public static dragon.nlp.tool.Tagger getPosTagger(
			HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());

		String posTagger = localConfig.getString("posTagger");
		if (posTagger == null)
			return null;
		String posTaggerDataDirectory = localConfig
				.getString("posTaggerDataDirectory");
		if (posTaggerDataDirectory == null)
			throw new IllegalArgumentException(
					"Must specify data directory for POS tagger");

		if (posTagger.equals(HeppleTagger.class.getName()))
			return new HeppleTagger(new Util().getFile(posTaggerDataDirectory));
		else if (posTagger.equals(MedPostTagger.class.getName()))
			return new MedPostTagger(posTaggerDataDirectory);
		else
			throw new IllegalArgumentException("Unknown POS tagger type: "
					+ posTagger);
	}

	
	 
	public static EngLemmatiser getLemmatiser(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String lemmatiserDataDirectory = Thread.currentThread().getContextClassLoader().getResource("nlpdata/lemmatiser").getFile();//localConfig.getString("lemmatiserDataDirectory");
		File f = new File (lemmatiserDataDirectory + "/adj.exec");
		System.out.println(lemmatiserDataDirectory + " Exists?: " + f.exists());
		if (lemmatiserDataDirectory == null)
			return null;
		return new EngLemmatiser(lemmatiserDataDirectory, false, true);
	}

	public static String getSimFindFilename(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String simFindFilename = localConfig.getString("simFindFilename");
		return simFindFilename;
	}

	private static Set<MentionType> getMentionTypes(
			HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String mentionTypesStr = localConfig.getString("mentionTypes");
		if (mentionTypesStr == null)
			throw new RuntimeException(
					"Configuration must contain parameter \"mentionTypes\"");
		Set<MentionType> mentionTypes = new HashSet<MentionType>();
		for (String mentionTypeName : mentionTypesStr.split("\\s+"))
			mentionTypes.add(MentionType.valueOf(mentionTypeName));
		return EnumSet.copyOf(mentionTypes);
	}

	private static OverlapOption getSameTypeOverlapOption(
			HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String sameTypeOverlapOption = localConfig
				.getString("sameTypeOverlapOption");
		if (sameTypeOverlapOption == null)
			throw new RuntimeException(
					"Configuration must contain parameter \"sameTypeOverlapOption\"");
		return OverlapOption.valueOf(sameTypeOverlapOption);
	}

	private static OverlapOption getDifferentTypeOverlapOption(
			HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config
				.configurationAt(BANNER.class.getPackage().getName());
		String differentTypeOverlapOption = localConfig
				.getString("differentTypeOverlapOption");
		if (differentTypeOverlapOption == null)
			throw new RuntimeException(
					"Configuration must contain parameter \"differentTypeOverlapOption\"");
		return OverlapOption.valueOf(differentTypeOverlapOption);
	}
}
