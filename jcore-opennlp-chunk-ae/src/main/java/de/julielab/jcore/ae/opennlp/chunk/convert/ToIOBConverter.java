package de.julielab.jcore.ae.opennlp.chunk.convert;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

/**
 * This should be the better conversion algorithm. At least no single "and" will be an NP here :-)
 * @author faessler
 *
 */
public class ToIOBConverter {

	/**
	 * JTBD is our default tokenizer for biomedical english. Thus, we will try
	 * to adapt the tokenization to the tokenization schema. Of course this is
	 * an automatic procedure and will probably produce some errors, but it
	 * shouldn't be too much.
	 */
	private AnalysisEngine jtbd;
	private JCas jCas;
	private AnalysisEngine pennbioIEPosTagger;

	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage: " + ToIOBConverter.class.getCanonicalName()
					+ " <from file or dir> <to file or dir> [true: create single output file]");
			System.exit(1);
		}

		File from = new File(args[0]);
		File to = new File(args[1]);
		boolean singleFile = false;
		if (args.length == 3)
			singleFile = Boolean.parseBoolean(args[2]);
		if (!singleFile && (from.isFile() && to.exists() && to.isDirectory()
				|| from.isDirectory() && to.exists() && to.isFile()))
			throw new IllegalArgumentException("Both paths must be directories or both must be files.");

		System.out.println("Input: " + from.getAbsolutePath());
		System.out.println("Output: " + to.getAbsolutePath());
		System.out.println("Output is written as a single file: " + singleFile);

		File[] inputFiles;
		if (from.isDirectory()) {
			inputFiles = from.listFiles((f, n) -> n.endsWith(".xml") || n.endsWith(".xml.gz"));
			if (!to.exists() && !singleFile) {
				System.out.println("Creating target directory " + to.getAbsolutePath());
				to.mkdirs();
			}
		} else {
			inputFiles = new File[] { from };
		}
		System.out.println("Converting");
		ToIOBConverter converter = new ToIOBConverter();
		if (singleFile) {
			try (BufferedWriter writerToFile = FileUtilities.getWriterToFile(to)) {
				for (int i = 0; i < inputFiles.length; i++) {
					File file = inputFiles[i];
					converter.convert(FileUtilities.getInputStreamFromFile(file), writerToFile);
				}
			}
		} else {
			for (int i = 0; i < inputFiles.length; i++) {
				File file = inputFiles[i];
				File targetFile = new File(
						to.getAbsolutePath() + File.separator + file.getName().replace(".xml", ".iob"));
				try (BufferedInputStream is = FileUtilities.getInputStreamFromFile(file);
						BufferedWriter bw = FileUtilities.getWriterToFile(targetFile)) {
					converter.convert(is, bw);
				}
			}
		}
		System.out.println("Done.");
	}

	public void convert(File from, File to, boolean singleFile) throws Exception {
		try (InputStream is = FileUtilities.getInputStreamFromFile(from);
				BufferedWriter bw = FileUtilities.getWriterToFile(to)) {
			convert(is, bw);
		}
	}

	public void convert(InputStream input, BufferedWriter output) throws Exception {
		XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(input);
		String consTag = "O";
		String iobState = "";
		String lastWrittenConsTag = "";
		List<ChunkRecord> sentenceRecords = new ArrayList<>();
		ChunkRecord previousRecord = null;
		Stack<String> consTags = new Stack<>();
		while (reader.hasNext()) {
			int eventType = reader.next();
			if (eventType == XMLStreamReader.START_ELEMENT) {
				String tag = reader.getLocalName();
				switch (tag) {
				case "sentence":
					sentenceRecords.clear();
					break;
				case "cons":
					String newTag = reader.getAttributeValue("", "cat");
					iobState = "";
					consTag = newTag;
					consTags.add(consTag);
					break;
				case "tok":
					String tokTag = reader.getAttributeValue("", "cat");
					String tokText = reader.getElementText();
					List<String[]> tokens = new ArrayList<>();
					tokens.add(new String[] { tokText, tokTag });
					if (tokText.contains(" "))
						tokens = tokenize(tokText);
					if (tokText.contains("("))
						tokens = balanceParenthesis(tokens);
					for (String[] tokPos : tokens) {
						tokText = tokPos[0];
						tokTag = tokPos[1];
						if (iobState.isEmpty() && !consTag.equals("O")) {
							iobState = "B-";
						} else if (iobState.equals("B-")) {
							iobState = "I-";
						} else {
							// Exceptions and rules. Required in case of a
							// phrase
							// that is fully embedded into an outer phrase.
							// Then
							// we
							// need to determine for the second part of the
							// outer
							// phrase the chunk type. Otherwise the second
							// part
							// would be O.
							if (consTag.equals("O")) {
								String outerConsTag = consTags.peek();
								if (!tokTag.equals("CC")) {
									consTag = outerConsTag;
									iobState = "B-";
								}
							}
						}
						switch (tokTag) {
						case "LRB":
						case "RRB":
							consTag = "O";
							iobState = "";
							break;
						case "COMMA":
							if (iobState.equals("B-")) {
								consTag = "O";
								iobState = "";
							}
							break;
						}
						if (lastWrittenConsTag.equals(consTag) && !consTag.equalsIgnoreCase("O"))
							iobState = "I-";
						if (previousRecord != null && !iobState.equals("I-") && isPunctuation(previousRecord.tokTag)) {
							previousRecord.consTag = "O";
							previousRecord.iobState = "";
						}

						tokTag = mapPennTreebankToPennBioIETag(tokTag);
						tokTag = mapTokenToPennBioIETag(tokText, tokTag);
						if (null != previousRecord)
							sentenceRecords.add(previousRecord);
						previousRecord = new ChunkRecord(tokText, tokTag, iobState, consTag);
					}
					lastWrittenConsTag = consTag;
					break;
				}
			}

			if (eventType == XMLStreamReader.END_ELEMENT) {
				String tag = reader.getLocalName();
				switch (tag) {
				case "cons":
					consTag = "O";
					iobState = "";
					consTags.pop();
					break;
				case "sentence":
					// Remove sentences where the S-category has been recognized
					// as a Chunk category. In this case, the sentence is just
					// not (fully) annotated and we omit it.
					boolean omitSentence = false;
					for (ChunkRecord cr : sentenceRecords) {
						if (cr.consTag.equals("S")) {
							// If we have a word that should be in a regular
							// chunk, then we omit the whole sentence because it
							// seems to lack annotations. Otherwise its alright
							// because we wouldn't want to include the word into
							// a chunk anyway.
							if (!mapPosTagToPhraseType(cr.tokTag).isEmpty()) {
								omitSentence = true;
							} else {
								cr.consTag = "O";
								cr.iobState = "";
							}
						}
					}
					if (omitSentence) {
						previousRecord = null;
						break;
					}
//					for (int i = 0; i < sentenceRecords.size(); i++) {
//						ChunkRecord cr = sentenceRecords.get(i);
//						String supposedChunkType = mapPosTagToPhraseType(cr.tokTag);
//						// When empty we have a word the is just a secondary element to a phrase. Check if it is connected to the chunk is is supposed to belong to.
//						if (supposedChunkType.isEmpty() && !cr.consTag.equals("O")) {
//							if (cr.iobState.equals("B-")) {
//								int j = i+1;
//								boolean primaryElementFound = false;
//								while(j < sentenceRecords.size() && !sentenceRecords.get(j).equals("B-")) {
//									if (mapPosTagToPhraseType(sentenceRecords.get(j).tokTag).equals(supposedChunkType))
//										primaryElementFound = true;
//									++j;
//								}
//								if (!primaryElementFound)
//								{
//									cr.consTag = "O";
//									cr.iobState= "";
//								}
//							}
//						}
//					}
					// repair parenthesis where the opening parenthesis is
					// within a chunk and the closing one is outside
					// repairParenthesisTags(sentenceRecords);
					// for (int i = 0; i < sentenceRecords.size(); ++i) {
					// ChunkRecord cr = sentenceRecords.get(i);
					// if (cr.tokTag.equals("-LRB-") && cr.tokenPart.length() >
					// 1) {
					// ChunkRecord previousCr = sentenceRecords.get(i - 1);
					// cr.iobState = "I-";
					// cr.consTag = previousCr.consTag;
					// }
					// }
					for (ChunkRecord cr : sentenceRecords) {
						output.write(cr.getRecordLine());
					}
					if (isPunctuation(previousRecord.tokTag)) {
						previousRecord.consTag = "O";
						previousRecord.iobState = "";
					}
					output.write(previousRecord.getRecordLine());
					previousRecord = null;
					output.write("\n");
					break;
				}
			}
		}
	}


	public void repairEnvironment(List<ChunkRecord> sentenceRecords, int i, ChunkRecord cr) {
		ChunkRecord previousCr = sentenceRecords.get(i - 1);
		if (previousCr.consTag.equals(cr.consTag))
			cr.iobState = "I-";
		else if (!cr.consTag.equals("O"))
			cr.iobState = "B-";
		if (i < sentenceRecords.size() - 1) {
			ChunkRecord nextCr = sentenceRecords.get(i + 1);
			if (nextCr.consTag.equals(cr.consTag) && !nextCr.consTag.equals("O"))
				nextCr.iobState = "I-";
		}
	}

	private boolean isPunctuation(String tokTag) {
		switch (tokTag) {
		case ".":
		case ",":
		case ":":
		case "``":
		case "''":
		case "-LRB-":
		case "-RRB-":
			return true;
		default:
			return false;
		}
	}

	private List<String[]> balanceParenthesis(List<String[]> tokens) {
		// we dont want to close all parenthesis pairs
		if (tokens.get(0)[1].equals("-LRB-"))
			return tokens;
		List<String[]> ret = new ArrayList<>();
		int numOpen = 0;
		int numClose = 0;
		Matcher mo = Pattern.compile("\\(").matcher("");
		Matcher mc = Pattern.compile("\\)").matcher("");
		for (int i = 0; i < tokens.size(); ++i) {
			String[] tokPos = tokens.get(i);
			String token = tokPos[0];
			mo.reset(token);
			mc.reset(token);
			while (mo.find()) {
				++numOpen;
			}
			while (mc.find()) {
				++numClose;
			}
			while (numOpen > numClose && i < tokens.size() - 1) {
				++i;
				String[] nextTokPos = tokens.get(i);
				String nextTok = nextTokPos[0];
				mo.reset(nextTok);
				mc.reset(nextTok);
				while (mo.find()) {
					++numOpen;
				}
				while (mc.find()) {
					++numClose;
				}
				token += nextTok;
			}
			ret.add(new String[] { token, tokPos[1] });
		}
		return ret;
	}

	public String mapPosTagToPhraseType(String tag) {
		switch (tag) {
		case "MD":
		case "VB":
		case "VBC":
		case "VBD":
		case "VBF":
		case "VBG":
		case "VBN":
		case "VBP":
		case "VBZ":
			return "VP";
		case "NN":
		case "NNS":
		case "NNP":
		case "NNPS":
		case "PRP$":
		case "PRP":
		case "WP":
		case "WP$":
			return "NP";
		case "IN":
			return "PP";
		default:
			// no particular mapping
			return "";
		}
	}

	public String mapPennTreebankToPennBioIETag(String tag) {
		switch (tag) {
		case "COLON":
			return ":";
		case "COMMA":
			return ",";
		case "LQT":
			return "``";
		case "LRB":
			return "-LRB-";
		case "PERIOD":
			return ".";
		case "PRPP":
			return "PRP$";
		case "RQT":
			return "''";
		case "RRB":
			return "-RRB-";
		case "WPP":
			return "WP$";
		default:
			return tag;
		}
	}

	public String mapTokenToPennBioIETag(String token, String originalTag) {
		switch (token) {
		case "-":
			return "HYPH";
		default:
			return originalTag;
		}
	}

	public ToIOBConverter() throws Exception {
		jtbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jtbd.desc.jcore-jtbd-ae-biomedical-english");
		pennbioIEPosTagger = AnalysisEngineFactory
				.createEngine("de.julielab.jcore.ae.opennlp.postag.desc.jcore-opennlp-postag-ae-biomedical-english");
		jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types");
	}

	/**
	 * Returns the text tokenized and pos-tagged, in that order.
	 * 
	 * @param text
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	private List<String[]> tokenize(String text) throws AnalysisEngineProcessException {
		jCas.reset();
		jCas.setDocumentText(text);
		new Sentence(jCas, 0, text.length()).addToIndexes();
		jtbd.process(jCas.getCas());
		pennbioIEPosTagger.process(jCas.getCas());
		return JCasUtil.select(jCas, Token.class).stream()
				.map(t -> new String[] { t.getCoveredText(), t.getPosTag(0).getValue() }).collect(Collectors.toList());
	}

	private class ChunkRecord {

		private String tokenPart;
		private String tokTag;
		private String iobState;
		private String consTag;

		public ChunkRecord(String tokenPart, String tokTag, String iobState, String consTag) {
			super();
			this.tokenPart = tokenPart;
			this.tokTag = tokTag;
			this.iobState = iobState;
			this.consTag = consTag;
		}

		public String getRecordLine() {
			return tokenPart + " " + tokTag + " " + iobState + consTag + "\n";
		}
	}
}
