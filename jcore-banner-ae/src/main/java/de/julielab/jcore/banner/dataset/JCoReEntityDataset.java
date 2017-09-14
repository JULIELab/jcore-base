package de.julielab.jcore.banner.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import banner.eval.dataset.Dataset;
import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Mention.MentionType;
import banner.types.Sentence;
import banner.types.Token;
import de.julielab.java.utilities.FileUtilities;

public class JCoReEntityDataset extends Dataset {

	private static final Logger log = LoggerFactory.getLogger(JCoReEntityDataset.class);

	public JCoReEntityDataset(Tokenizer tokenizer) {
		super();
		this.tokenizer = tokenizer;
	}

	public JCoReEntityDataset() {
		super();
	}

	@Override
	public void load(HierarchicalConfiguration config) {
		HierarchicalConfiguration localConfig = config.configurationAt(Dataset.class.getPackage().getName());
		String sentenceFilename = localConfig.getString("sentenceFilename");
		String mentionsFilename = localConfig.getString("mentionTestFilename");
		load(new File(sentenceFilename), new File(mentionsFilename));
	}

	public void load(File sentenceFile, File mentionsFile) {
		try (BufferedReader sentReader = FileUtilities.getReaderFromFile(sentenceFile);
				BufferedReader mentReader = FileUtilities.getReaderFromFile(mentionsFile)) {
			Map<String, Sentence> sentences = new HashMap<>();
			sentReader.lines().forEach(s -> {
				String[] split = s.split("\\t");
				Sentence sentence = new Sentence(split[1], split[0], split[2]);
				tokenizer.tokenize(sentence);
				sentences.put(split[1], sentence);
				this.sentences.add(sentence);
			});

			// ml ~ mention line
			mentReader.lines().forEach(ml -> {
				String[] split = ml.split("\\t");
				Sentence sentence = sentences.get(split[0]);
				int begin = Integer.parseInt(split[1]);
				int end = Integer.parseInt(split[2]);
				EntityType label = EntityType.getType(split[3]);
				try {
					Mention mention = new Mention(sentence, getTokenIndex(sentence.getTokens(), begin),
							getTokenIndex(sentence.getTokens(), end), label, MentionType.Required);
					sentence.addMention(mention);
				} catch (IllegalArgumentException e) {
					log.warn("Skipping mention {}", ml);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int getTokenIndex(List<Token> tokens, int characterIndex) {
		for (int i = 0; i < tokens.size(); ++i) {
			Token t = tokens.get(i);
			if (t.getStart() >= characterIndex && t.getEnd() >= characterIndex)
				return i;
		}
		log.warn("Could not get the token index for character index " + characterIndex
				+ " with the following token sequence: "
				+ tokens.stream().map(t -> t.getText()).collect(Collectors.joining(" ")));
		throw new IllegalArgumentException();
	}

	@Override
	public List<Dataset> split(int n) {
		List<Dataset> splitDatasets = new ArrayList<Dataset>();
		for (int i = 0; i < n; i++) {
			JCoReEntityDataset dataset = new JCoReEntityDataset(tokenizer);
			splitDatasets.add(dataset);
		}

		Random r = new Random();
		for (Sentence sentence : sentences) {
			int num = r.nextInt(n);
			((JCoReEntityDataset)splitDatasets.get(num)).sentences.add(sentence);
		}
		return splitDatasets;
	}

}
