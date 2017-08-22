package de.julielab.jules.ae.genemapper.eval.tools;

import static de.julielab.java.utilities.FileUtilities.getInputStreamFromFile;
import static de.julielab.java.utilities.FileUtilities.getReaderFromFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.LuceneCandidateRetrieval;
import de.julielab.jules.ae.genemapper.genemodel.Acronym;
import de.julielab.jules.ae.genemapper.genemodel.AcronymLongform;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention.GeneTagger;
import de.julielab.jules.ae.genemapper.genemodel.SpeciesMention;
import de.julielab.jules.ae.genemapper.scoring.MaxEntScorer;
import de.julielab.jules.ae.genemapper.utils.OffsetMap;

/**
 * This class just offers a few methods required commonly by other classes in
 * this packages.
 * 
 * @author faessler
 *
 */
public class EvalToolUtilities {
	/**
	 * The expected format for the evaluation data is the BioCreative II gene
	 * list data. Additional columns (besides document ID and gene ID) are
	 * ignored.
	 * 
	 * @return A map from document ID to the set of IDs occurring in this
	 *         document
	 */
	public static Multimap<String, GeneMention> readGoldIds(String geneList) throws IOException {
		Multimap<String, GeneMention> ids = HashMultimap.create();
		LineIterator lineIterator = IOUtils.lineIterator(getInputStreamFromFile(new File(geneList)), "UTF-8");
		while (lineIterator.hasNext()) {
			String line = (String) lineIterator.next();
			String[] split = line.split("\t");
			GeneMention mappedGeneMention = new GeneMention();
			mappedGeneMention.setId(split[1]);
			mappedGeneMention.setText(split[2]);
			mappedGeneMention.setMappedSynonym(mappedGeneMention.getText());
			ids.put(split[0], mappedGeneMention);
		}
		return ids;
	}

	/**
	 * Takes a <em>folder</em> of text files. Each file must be named according
	 * to the ID of an evaluation document, up to the first dot (i.e.
	 * 23742287.txt.genes.uniq is valid, where the first part is the document
	 * ID). The contents of the file are the text forms of the predicted gene
	 * mentions in the respective document, one mention per line. The file may
	 * have multiple tab-separated columns, all but the first are ignored and
	 * the first column is required to hold the actual mention text.
	 * 
	 * @param mentionListDocumentsDirectory
	 * @throws Exception
	 * @throws @return
	 */
	public static Multimap<String, String> readPredictedMentions(String mentionListDocumentsDirectory)
			throws IOException {
		Multimap<String, String> predictedMentions = HashMultimap.create();
		File directory = new File(mentionListDocumentsDirectory);
		if (!directory.isDirectory())
			throw new IllegalArgumentException("The path \"" + mentionListDocumentsDirectory
					+ "\" does not point to a directory. A directory holding separate files for each gene mention evaluation document is expected.");
		File[] predictedMentionFiles = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.equals(".DS_Store");
			}
		});
		for (File mentionFile : predictedMentionFiles) {
			String docId = mentionFile.getName().substring(0, mentionFile.getName().indexOf('.'));
			LineIterator lineIterator = IOUtils.lineIterator(getInputStreamFromFile(mentionFile), "UTF-8");
			while (lineIterator.hasNext()) {
				String mentionString = (String) lineIterator.next();
				predictedMentions.put(docId, mentionString.split("\\t")[0]);
			}
		}
		return predictedMentions;
	}

	/**
	 * Reads gene mentions with ID, offsets, entity text taxonomy ID and Tagger type (JNET or Gazetteer). The last three items are optional.
	 * @param geneListFile The file listing all predicted genes
	 * @return The read gene mentions grouped by document Id
	 * @throws IOException 
	 * @throws  
	 * @throws Exception
	 */
	public static Multimap<String, GeneMention> readMentionsWithOffsets(String geneListFile) throws IOException {
		Multimap<String, GeneMention> mentionsWithOffsets = LinkedHashMultimap.create();
		File directory = new File(geneListFile);
		if (!directory.isFile())
			throw new IllegalArgumentException("The path \"" + geneListFile
					+ "\" does not point to a file. A file holding one documentId, gene id, begin, end and gene mention record per line is required.");
		LineIterator lineIterator = IOUtils.lineIterator(getInputStreamFromFile(new File(geneListFile)), "UTF-8");
		while (lineIterator.hasNext()) {
			String mentionString = (String) lineIterator.next();
			String[] split = mentionString.split("\\t");
			String docId = split[0];
			String geneId = split[1];
			int begin = Integer.parseInt(split[2]);
			int end = Integer.parseInt(split[3]);
			String text = null;
			GeneTagger tagger = null;
			if (split.length > 4)
				text = split[4];
			if (split.length > 5) {
				String systemId = split[5];
				if (systemId.endsWith("GazetteerAnnotator"))
					tagger = GeneTagger.GAZETTEER;
				if (systemId.endsWith("EntityAnnotator"))
					tagger = GeneTagger.JNET;
			}
			
			GeneMention geneMention = new GeneMention();
			geneMention.setDocId(docId);
			if (!StringUtils.isBlank(geneId))
				geneMention.setId(geneId);
			geneMention.setOffsets(Range.between(begin, end));
			geneMention.setText(text);
			geneMention.setTagger(tagger);
			mentionsWithOffsets.put(docId, geneMention);
		}
		return mentionsWithOffsets;
	}
	
	/**
	 * Reads gene mentions with ID, offsets, entity text, taxonomy ID, species for the gene and Tagger type (JNET or Gazetteer). The last four items are optional.
	 * @param geneListFile The file listing all predicted genes
	 * @return The read gene mentions grouped by document Id
	 * @throws Exception
	 */
	public static Multimap<String, GeneMention> readMentionsWithOffsetsAndSpecies(String geneListFile) throws IOException {
		Multimap<String, GeneMention> mentionsWithOffsets = LinkedHashMultimap.create();
		File directory = new File(geneListFile);
		if (!directory.isFile())
			throw new IllegalArgumentException("The path \"" + geneListFile
					+ "\" does not point to a file. A file holding one documentId, gene id, begin, end and gene mention record per line is required.");
		LineIterator lineIterator = IOUtils.lineIterator(getInputStreamFromFile(new File(geneListFile)), "UTF-8");
		while (lineIterator.hasNext()) {
			String mentionString = (String) lineIterator.next();
			String[] split = mentionString.split("\\t");
			String docId = split[0];
			String geneId = split[1];
			int begin = Integer.parseInt(split[2]);
			int end = Integer.parseInt(split[3]);
			String text = null;
			String taxId = null;
			GeneTagger tagger = null;
			if (split.length > 4)
				text = split[4];
			if (split.length > 5)
				taxId = split[5];
			if (split.length > 6) {
				String systemId = split[6];
				if (systemId.endsWith("GazetteerAnnotator"))
					tagger = GeneTagger.GAZETTEER;
				if (systemId.endsWith("EntityAnnotator"))
					tagger = GeneTagger.JNET;
			}
			
			GeneMention geneMention = new GeneMention();
			geneMention.setDocId(docId);
			if (!StringUtils.isBlank(geneId))
				geneMention.setId(geneId);
			geneMention.setOffsets(Range.between(begin, end));
			geneMention.setText(text);
			geneMention.setTaxonomyId(taxId);
			geneMention.setTagger(tagger);
			mentionsWithOffsets.put(docId, geneMention);
		}
		return mentionsWithOffsets;
	}

	/**
	 * With 'context' we mean the words in the neighborhood of a gene mention.
	 * For the gene mapper, this traditionally is either the whole abstract text
	 * - the idea being that an abstract is focused enough to mainly contain
	 * relevant information about a gene mention in this abstract - or a 'token
	 * window' of words around each gene mention. The latter approach is mostly
	 * used for full texts. Each file must be named according to the ID of an
	 * evaluation document, up to the first dot (i.e. 23742287.txt.context is
	 * valid, where the first part is the document ID).
	 * 
	 * @param contextDocumentsDirectory
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> readGeneContexts(String contextDocumentsDirectory) throws IOException {
		Map<String, String> documentContexts = new HashMap<>();
		File directory = new File(contextDocumentsDirectory);
		if (!directory.isDirectory())
			throw new IllegalArgumentException("The path \"" + contextDocumentsDirectory
					+ "\" does not point to a directory. A directory holding separate files for each gene mention evaluation document is expected.");
		File[] contextFiles = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.equals(".DS_Store");
			}
		});
		for (File mentionFile : contextFiles) {
			String docId = mentionFile.getName().substring(0, mentionFile.getName().indexOf('.'));
			try (InputStream is = getInputStreamFromFile(mentionFile)) {
				String context = IOUtils.toString(is, "UTF-8");
				documentContexts.put(docId, context);
			}
		}
		return documentContexts;
	}

	public static Multimap<String, String> convertGoldMentionsToIdsPerDocument(
			Multimap<String, GeneMention> bc2TrainGold) {
		Multimap<String, String> idsPerDoc = HashMultimap.create();
		for (String docId : bc2TrainGold.keySet()) {
			Collection<GeneMention> goldMentions = bc2TrainGold.get(docId);
			for (GeneMention goldMention : goldMentions) {
				idsPerDoc.put(docId, goldMention.getId());
			}
		}
		return idsPerDoc;
	}

	public static Multimap<String, String> convertGoldMentionsToMentionTextPerDocument(
			Multimap<String, GeneMention> bc2TrainGold) {
		Multimap<String, String> idsPerDoc = HashMultimap.create();
		for (String docId : bc2TrainGold.keySet()) {
			Collection<GeneMention> goldMentions = bc2TrainGold.get(docId);
			for (GeneMention goldMention : goldMentions) {
				idsPerDoc.put(docId, goldMention.getText());
			}
		}
		return idsPerDoc;
	}

	public static Set<String> getIdsOfMentions(Collection<GeneMention> goldMentionsForDoc) {
		Set<String> ids = new HashSet<>(goldMentionsForDoc.size());
		for (GeneMention mention : goldMentionsForDoc)
			ids.add(mention.getId());
		return ids;
	}

	/**
	 * Given a gold gene mention and a collection of predicted gene mentions
	 * (e.g. by some named entity recognition algorithm like JNET), this method
	 * uses the Levensthein algorithm and the candidate scorer of the gene
	 * mapper (which is supposed to score how well two different strings are
	 * actually the same gene after all) to find a predicted gene mention to the
	 * given gold mention. The method uses some thresholding to reject gold
	 * mentions for which we don't find a good prediction at all. For those, the
	 * NER algorithm has most probably just not returned an appropriate gene
	 * mention.
	 * 
	 * @param mapper
	 * @param notMatched
	 * @param predictedDocMentions
	 * @param goldMention
	 * @return
	 * @throws Exception
	 */
	public static String getClosestPredictedMention(GeneMapper mapper, Multimap<GeneMention, String> notMatched,
			Collection<String> predictedDocMentions, GeneMention goldMention) throws RuntimeException {
		String bestPredictedMention = "";

		int minDist = Integer.MAX_VALUE;
		// 'me' like Maximum Entropy because I used the ME scorer for
		// development of this method
		double meScore = Double.MIN_VALUE;
		String closestLSPrediction = "";
		// note that all development for this method has been done using the
		// original maximum entropy scorer from Katrin Tomanek (Wermter et al.)
		String closestScoredPrediction = "";
		for (String predictedMention : predictedDocMentions) {
			int levenshteinDistance = StringUtils.getLevenshteinDistance(goldMention.getText(), predictedMention);
			if (levenshteinDistance < minDist) {
				minDist = levenshteinDistance;
				closestLSPrediction = predictedMention;
			}
			// All those ME score comparisons only make sense when we actually
			// use the ME scorer
			if (((LuceneCandidateRetrieval) mapper.getMappingCore().getCandidateRetrieval())
					.getScorer() instanceof MaxEntScorer) {
				MaxEntScorer maxEntScorer = (MaxEntScorer) ((LuceneCandidateRetrieval) mapper.getMappingCore()
						.getCandidateRetrieval()).getScorer();
				double score = maxEntScorer.getScore(
						mapper.getMappingCore().getTermNormalizer().normalize(goldMention.getText()),
						mapper.getMappingCore().getTermNormalizer().normalize(predictedMention));
				if (score > meScore) {
					meScore = score;
					closestScoredPrediction = predictedMention;
				}
			}
		}
		double lengthNormMinDist = minDist
				/ Math.max((double) goldMention.getText().length(), (double) closestLSPrediction.length());
		boolean containsLS = goldMention.getText().contains(closestLSPrediction)
				|| closestLSPrediction.contains(goldMention.getText());
		boolean containsME = goldMention.getText().contains(closestScoredPrediction)
				|| closestScoredPrediction.contains(goldMention.getText());
		// these are thresholds obtained just by looking on the best candidates,
		// their respective score and deciding whether the candidates would be
		// right or wrong; this whole method isn't supposed to be perfect, we
		// just want to know how well we find candidates for a (more or less
		// correct) mention
		boolean lsAccept = lengthNormMinDist <= 0.5;
		boolean meAccept = meScore >= 0.8;
		boolean accepted = false;
		if (lsAccept || meAccept) {
			accepted = true;
			double lsScore = 1d - lengthNormMinDist;
			if (lsScore >= meScore)
				// System.out.println("\tLS " + goldMention.text + " --> " +
				// closestLSPrediction + " (" + minDist
				// + ", length normalized: " + lengthNormMinDist + "), contains:
				// " + containsLS + ", doc: "
				// + docId);
				bestPredictedMention = closestLSPrediction;
			if (meScore > lsScore)
				// System.out.println("\tME " + goldMention.text + " --> " +
				// closestScoredPrediction + " (" + meScore
				// + "), contains: " + containsME);
				bestPredictedMention = closestScoredPrediction;
		} else {
			// The Levensthein-algorithm works quite well, however it treats
			// number as any other character. With genes, this may be an issue
			// since IL-2 and IL-10 are very different things. So, if the
			// Levenshtein distance is not so small anyway and there are number
			// in the expressions, better drop it.
			if (containsLS && !(containsDigit(goldMention.getText()) && containsDigit(closestLSPrediction))) {
				accepted = true;
				// System.out.println("LS " + goldMention.text + " --> " +
				// closestLSPrediction + " (" + minDist
				// + ", length normalized: " + lengthNormMinDist + "), contains:
				// " + containsLS + ", doc: "
				// + docId);
				bestPredictedMention = closestLSPrediction;
			}
			if (containsME) {
				accepted = true;
				// System.out.println("ME " + goldMention.text + " --> " +
				// closestScoredPrediction + " (" + meScore
				// + "), contains: " + containsME);
				bestPredictedMention = closestScoredPrediction;
			}
		}
		if (!accepted) {
			notMatched.put(goldMention, closestLSPrediction + " (LS; " + lengthNormMinDist + ")");
			notMatched.put(goldMention, closestScoredPrediction + " (ME; " + meScore + ")");
		}
		return bestPredictedMention;
	}

	private static boolean containsDigit(String closestPrediction) {
		for (int i = 0; i < closestPrediction.length(); ++i) {
			if (Character.isDigit(closestPrediction.charAt(i)))
				return true;
		}
		return false;
	}

	public static List<GeneMention> getGeneMentionsInRange(Collection<GeneMention> mentions, int begin, int end) {
		List<GeneMention> ret = new ArrayList<>();
		Range<Integer> soughtRange = Range.between(begin, end);
		for (GeneMention mention : mentions) {
			Range<Integer> mentionRange = Range.between(mention.getBegin(), mention.getEnd());
			if (mentionRange.isOverlappedBy(soughtRange))
				ret.add(mention);
		}
		return ret;
	}

	public static Set<GeneMention> getGeneMentionsAtPosition(GeneMention referenceMention,
			Collection<GeneMention> candidateMentions) {
		Set<GeneMention> ret = new HashSet<>();
		Range<Integer> predictedSpan = Range.between(referenceMention.getBegin(), referenceMention.getEnd());
		for (GeneMention candidate : candidateMentions) {
			Range<Integer> candidateSpan = Range.between(candidate.getBegin(), candidate.getEnd());
			if (candidateSpan.isOverlappedBy(predictedSpan))
				ret.add(candidate);
		}
		return ret;
	}

	public static Multimap<String, Acronym> readAcronymAnnotations(String acronymAnnotationPath) throws IOException {
		File acronymAnnotationFile = new File(acronymAnnotationPath);
		Multimap<String, Acronym> acronyms = HashMultimap.create();
		try (Stream<String> lines = getReaderFromFile(acronymAnnotationFile).lines()) {
			Iterator<String> iterator = lines.iterator();
			Map<String, AcronymLongform> longforms = new HashMap<>();
			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				String docId = split[0];
				String id = split[1];
				int begin = Integer.parseInt(split[2]);
				int end = Integer.parseInt(split[3]);


				if (id.startsWith("A")) {
					String longformid = split[4];
					// This longform always already exists in the map. This is
					// because the input format is required to first define the
					// longform and then the acronyms referencing it.
					AcronymLongform acronymLongform = longforms.get(longformid);
					Acronym acronym = new Acronym();
					acronym.setOffsets(Range.between(begin, end));
					acronym.setLongform(acronymLongform);
					
					acronyms.put(docId, acronym);
				}
				if (id.startsWith("F")) {
					AcronymLongform longform = new AcronymLongform();
					longform.setOffsets(Range.between(begin, end));
					longforms.put(id, longform);
				}
			}
		}

		return acronyms;
	}

	public static Map<String, String> readTitles(String titlesPath) throws IOException {
		File titlesFile = new File(titlesPath);
		Map<String, String> titles = new HashMap<>();
		try (Stream<String> lines = getReaderFromFile(titlesFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				if (split.length != 2) {
					throw new IllegalArgumentException(titlesPath + " should have exactly two columns.");
				}
				String docId = split[0];
				String title = split[1];
				titles.put(docId, title);
			}
		}
		return titles;
	}

	public static Multimap<String, String> readMeshterms(String meshPath) throws IOException {
		File meshFile = new File(meshPath);
		Multimap<String, String> meshterms = HashMultimap.create();
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				if (split.length < 2) {
					throw new IllegalArgumentException(meshPath + " should have at least two columns in each line.");
				}
				String docId = split[0];
				for (int i = 1; i < split.length; ++i) {
					String mesh = split[1];
					meshterms.put(docId, mesh);
				}
			}
		}
		return meshterms;
	}

	public static Multimap<String, String> readTitleSpecies(String titlesPath) throws IOException {
		File meshFile = new File(titlesPath);
		Multimap<String, String> titleSpecies = HashMultimap.create();
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				if (split.length < 2) {
					throw new IllegalArgumentException(titlesPath + " should have exactly two columns.");
				}
				String docId = split[0];
				String[] species = split[1].split(";");
				for (String s : species) {
					titleSpecies.put(docId, s);
				}
			}	
		}
		return titleSpecies;

	}

	public static Multimap<String, String> readMeshSpecies(String meshPath) throws IOException {
		File meshFile = new File(meshPath);
		Multimap<String, String> meshSpecies = HashMultimap.create();
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				if (split.length < 2) {
					throw new IllegalArgumentException(meshPath + " should have at least two columns in each line.");
				}
				String docId = split[0];
				for (int i = 1; i < split.length; ++i) {
					if (!split[i].equals("")) {
						String[] species = split[i].split(";");
						for (String s : species) {
							meshSpecies.put(docId, s);
						}
					}
				}
			}
		}
		return meshSpecies;
	}
/*	
	public static Multimap<String, SpeciesMention> readTextSpecies(String textPath) throws IOException {
		File meshFile = new File(textPath);
		Multimap<String, SpeciesMention> textSpecies = HashMultimap.create();
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				//docId, taxId, start, end, text
				if (split.length != 5) {
					throw new IllegalArgumentException(textPath + " should have exactly five columns in each line.");
				}
				String docId = split[0];
				String taxId = split[1];
				Range<Integer> offsets = Range.between(Integer.parseInt(split[2]), Integer.parseInt(split[3]));
				SpeciesMention sm = new SpeciesMention(offsets, taxId, split[4]);
				textSpecies.put(docId, sm);
			}
		}
		return textSpecies;
	}
*/	
	public static Map<String, OffsetMap<SpeciesMention>> readTextSpecies(String textPath) throws IOException {
		File meshFile = new File(textPath);
		Map<String, OffsetMap<SpeciesMention>> species = new HashMap<String, OffsetMap<SpeciesMention>>();
		int lineCounter = 1;
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				//docId, chunk type, start, end, chunk text
				if (split.length != 5) {
					throw new IllegalArgumentException("Line " + lineCounter + ": " + 
							textPath + " should have exactly five columns in each line.");
				}
				String docId = split[0];				
				String taxId = split[1];
				
				int start = Integer.parseInt(split[2]);
				int end   = Integer.parseInt(split[3]);
				Range<Integer> offsets = Range.between(start, end);
				
				if (species.containsKey(docId)) {
					OffsetMap<SpeciesMention> docChunks = species.get(docId);
					docChunks.put(offsets, new SpeciesMention(taxId, split[4]));
				} else {
					OffsetMap<SpeciesMention> docSpecies = new OffsetMap<SpeciesMention>();
					docSpecies.put(offsets, new SpeciesMention(taxId, split[4]));
					species.put(docId, docSpecies);
				}
				++lineCounter;
			}
		}
		return species;
	}
	
	public static Multimap<String, Range<Integer>> readSentenceOffsets(String sentencePath) throws IOException {
		File meshFile = new File(sentencePath);
		Multimap<String, Range<Integer>> sentences = HashMultimap.create();
		int lineCounter = 1;
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				//docId, (empty), start, end, sentence
				if (split.length != 5) {
					throw new IllegalArgumentException("Line " + lineCounter + ": " + 
								sentencePath + " should have exactly five columns in each line.");
				}
				String docId = split[0];
				int start = Integer.parseInt(split[2]);
				int end   = Integer.parseInt(split[3]);
				Range<Integer> offsets = Range.between(start, end);
				sentences.put(docId, offsets);
				++lineCounter;
			}
		}
		return sentences;
	}
	
	public static Map<String, OffsetMap<String>> readChunkOffsets(String chunkPath) throws IOException {
		File meshFile = new File(chunkPath);
		Map<String, OffsetMap<String>> chunks = new HashMap<String, OffsetMap<String>>();
		int lineCounter = 1;
		try (Stream<String> lines = getReaderFromFile(meshFile).lines()) {
			Iterator<String> iterator = lines.iterator();

			while (iterator.hasNext()) {
				String line = iterator.next();
				String[] split = line.split("\\t");
				//docId, chunk type, start, end, chunk text
				if (split.length != 5) {
					throw new IllegalArgumentException("Line " + lineCounter + ": " + 
								chunkPath + " should have exactly five columns in each line.");
				}
				String docId = split[0];				
				String type = split[1];
				String[] tempType = type.split("\\.");		
				type = tempType[tempType.length - 1];
				
				int start = Integer.parseInt(split[2]);
				int end   = Integer.parseInt(split[3]);
				Range<Integer> offsets = Range.between(start, end);
				
				if (chunks.containsKey(docId)) {
					OffsetMap<String> docChunks = chunks.get(docId);
					docChunks.put(offsets, type);
				} else {
					OffsetMap<String> docChunks = new OffsetMap<String>();
					docChunks.put(offsets, type);
					chunks.put(docId, docChunks);
				}
				++lineCounter;
			}
		}
		return chunks;
	}
}
