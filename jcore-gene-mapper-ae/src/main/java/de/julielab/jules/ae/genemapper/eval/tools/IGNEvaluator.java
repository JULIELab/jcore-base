package de.julielab.jules.ae.genemapper.eval.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import de.julielab.jules.ae.genemapper.DocumentMappingResult;
import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.SynHit;
import de.julielab.jules.ae.genemapper.genemodel.Acronym;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.scoring.TFIDFScorer;
import de.julielab.jules.ae.genemapper.svm.MentionFeatureGenerator;
import de.julielab.jules.ae.genemapper.utils.ContextUtils;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;

public class IGNEvaluator {

	private static final Logger log = LoggerFactory.getLogger(IGNEvaluator.class);

	private static final boolean writePredictedMentionStats = false;

	private static DecimalFormat df = new DecimalFormat("#.00");

	public static void main(String[] args) throws IOException, GeneMapperException {
		String predictionType = "jnet";
//		String predictionType = "gazetteer";
//		String predictionType = "multi";
		String predictionModifier = "";
		String dataType = "train";

		long time = System.currentTimeMillis();
		String propertiesFile = "src/main/resources/ign" + StringUtils.capitalize(predictionType)
				+ "GeneMapper.properties";
		GeneMapper mapper = new GeneMapper(new File(propertiesFile));

		evaluate(mapper, "data/eval_data/ign_data/ign-" + dataType + ".genelist.humanids",
				"data/eval_data/ign_data/ign-" + dataType + "-predicted-" + predictionType + predictionModifier
						+ ".genelist",
				"data/eval_data/ign_data/ign-" + dataType + "-acronyms.ann",
				"data/eval_data/biocreative2_data/" + dataType + "PubmedFiles", true);

		time = System.currentTimeMillis() - time;
		System.out.println(String.format("Eval took %s seconds (%s minutes)", time / 1000d, (time / 1000d) / 60d));
	}

	public static MentionAndDocResult evaluate(GeneMapper mapper, String goldMappedMentionsPath,
			String predictedMentionsPath, String acronymsPath, String documentsPath, boolean printResult)
			throws IOException, GeneMapperException, CorruptIndexException {


		File confusionFile = new File("confusion.txt");
		if (confusionFile.exists())
			confusionFile.delete();

		TFIDFScorer contextScorer = null;

		Multimap<String, GeneMention> goldData = EvalToolUtilities.readMentionsWithOffsets(goldMappedMentionsPath);
		// Multimap<String, GeneMention> ignTrainGold = EvalToolUtilities
		// .readMentionsWithOffsets("data/eval_data/ign_data/ign-train.genelist.humanids");
		// by using the gold data here, instead of JNET or Gazetteer mentions,
		// for example, we can focus on mapping quality because the mentions are
		// perfect
		Multimap<String, GeneMention> predictedGeneMentions = EvalToolUtilities
				.readMentionsWithOffsets(predictedMentionsPath);
		// Multimap<String, GeneMention> ignTrainJnet = EvalToolUtilities
		// .readMentionsWithOffsets("data/eval_data/ign_data/ign-train-predicted-jnet.genelist");
		// Multimap<String, GeneMention> ignTrainJnet = EvalToolUtilities
		// .readMentionsWithOffsets("data/eval_data/ign_data/ign-train.genelist.humanids");

		Multimap<String, Acronym> acronyms = EvalToolUtilities.readAcronymAnnotations(acronymsPath);

		// IGN is BioCreative on instance level, that means the documents are
		// the same
		Map<String, String> bc2TrainContexts = EvalToolUtilities.readGeneContexts(documentsPath);
		// Map<String, String> bc2TrainContexts = EvalToolUtilities
		// .readGeneContexts("data/eval_data/biocreative2_data/trainPubmedFiles");

		if (writePredictedMentionStats) {
			List<String> mentionStatsHeader = Arrays.asList("is_correct", "min_score", "max_score", "mean_score",
					"score_std_deviation", "num_exact_hits", "num_pred_tokens", "pred_length", "min_token_overlap",
					"max_token_overlap", "mean_token_overlap", "mean_number_compatibility", "mean_token_ratio",
					"mean_length_ratio", "contains_synonyms", "synonyms_contain", "mention", "normalized_mention",
					"gold_ids");
			FileUtils.write(new File("genePredictedMentionStatsIgnTrain.tsv"),
					StringUtils.join(mentionStatsHeader, "\t") + "\n", "UTF-8", false);
		}

		int candidateCutoff = 10;

		CollectionMappingResult collectionResult = new CollectionMappingResult();

		List<String> allDocs = new ArrayList<>(Sets.union(goldData.keySet(), predictedGeneMentions.keySet()));
		int docNum = 0;
		for (String docId : allDocs) {
			docNum++;
			DocumentMappingResult documentMappingResult = mapDocument(mapper, confusionFile, contextScorer, goldData,
					predictedGeneMentions, acronyms, bc2TrainContexts, candidateCutoff, collectionResult, allDocs,
					docNum, docId);
			documentMappingResult.docId = docId;

		}

		if (collectionResult.goldHasNoIdMentions)
			log.warn(
					"The gold data contains gene mentions without an ID. This will produce additional false negatives.");

		writeResultsForEntityEvaluator(collectionResult);

		DocResult mentionWise = new DocResult(collectionResult.tp, collectionResult.fp, collectionResult.fn);
		DocResult docWise = new DocResult(collectionResult.overallDocLevelTps.size(),
				collectionResult.overallDocLevelFps.size(), collectionResult.overallDocLevelFns.size());
		DocResult mentions = new DocResult(collectionResult.numTPPredMentions, collectionResult.numFPPredMentions,
				collectionResult.numFNPredMentions);

		if (printResult) {
			System.out.println("mention-wise\n   " + mentionWise.toDetailedString());
			System.out.println("document-wise\n   " + docWise.toDetailedString());
			System.out.println("Number of unique predicted IDs: " + collectionResult.allPredictedIDs.size());
			System.out.println("Performance gene mention recognition:\n" + mentions.toDetailedString());
			System.out.println("Number of exact hits: " + collectionResult.numExactHits);
			System.out.println("Number of correct exact hits: " + collectionResult.numCorrectExactHits);
			System.out.println("Percentage of correct exact hits: "
					+ df.format(((double) collectionResult.numCorrectExactHits / collectionResult.numExactHits) * 100)
					+ " %");
			System.out.println("-");
			System.out.println("Number of ambiguous exact hits: " + collectionResult.numAmbigExactHits);
			System.out.println("Number of ambiguous correct exact hits: " + collectionResult.numCorrectAmbigExactHits);
			System.out.println("Percentage of ambiguous correct exact hits: " + df.format(
					((double) collectionResult.numCorrectAmbigExactHits / collectionResult.numAmbigExactHits) * 100)
					+ " %");
			System.out.println("-");
			System.out.println("Number of approximate hits: " + collectionResult.numApproxHits);
			System.out.println("Number of correct approximate hits: " + collectionResult.numCorrectApproxHits);
			System.out.println("Percentage of correct approximate hits: "
					+ df.format(((double) collectionResult.numCorrectApproxHits / collectionResult.numApproxHits) * 100)
					+ " %");
			System.out.println();
			System.out.println(
					"Overall candidate retrieval time: " + (collectionResult.candidateRetrievalTime / 1000d) + "sec");
			System.out.println("Overall disambiguation time: " + (collectionResult.disambiguationTime / 1000d) + "sec");
		}

		return new MentionAndDocResult(mentionWise, docWise);
	}

	private static void writeResultsForEntityEvaluator(CollectionMappingResult collectionResult) {
		try (OutputStream os = new FileOutputStream("mappingresult.genelist")) {
			for (DocumentMappingResult result : collectionResult.documentMappingResults) {
				for (MentionMappingResult mentionResult : result.mentionResults) {
					if (mentionResult.resultEntry == MentionMappingResult.REJECTION)
						continue;
					List<String> columns = new ArrayList<>();
					columns.add(result.docId);
					columns.add(mentionResult.resultEntry.getId());
					columns.add(String.valueOf(mentionResult.mappedMention.getBegin()));
					columns.add(String.valueOf(mentionResult.mappedMention.getEnd()));
					columns.add(mentionResult.mappedMention.getText());
					IOUtils.write(StringUtils.join(columns, "\t") + "\n", os, "UTF-8");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static DocumentMappingResult mapDocument(GeneMapper mapper, File confusionFile, TFIDFScorer contextScorer,
			Multimap<String, GeneMention> ignTrainGold, Multimap<String, GeneMention> ignTrainJnet,
			Multimap<String, Acronym> acronyms, Map<String, String> bc2TrainContexts, int candidateCutoff,
			CollectionMappingResult collectionResult, List<String> allDocs, int docNum, String docId)
			throws IOException, GeneMapperException, CorruptIndexException {

		// mentions
		Collection<GeneMention> predictedMentions = ignTrainJnet.get(docId);
		Collection<Acronym> acronymsInDoc = acronyms.get(docId);

		// document-wide unique IDs for doc-level performance
		Set<String> goldIdsInDoc = new HashSet<>();
		Set<String> tpIdsInDoc = new HashSet<>();
		Set<String> fpIdsInDoc = new HashSet<>();
		Set<String> fnIdsInDoc = new HashSet<>();

		// document context
		String documentContext = bc2TrainContexts.get(docId);
		BooleanQuery contextQuery = ContextUtils.makeContextQuery(documentContext);

		// set the context information to the genes themselves
		for (GeneMention gm : predictedMentions) {
			gm.setDocumentContext(documentContext);
			gm.setContextQuery(contextQuery);
		}

		// get the best predicted candidate for the predicted mentions
		List<String> tpStrings = new ArrayList<>();
		List<String> fpStrings = new ArrayList<>();
		List<String> fnStrings = new ArrayList<>();
		Set<GeneMention> hitGoldMentionsInDoc = new HashSet<>();
		int numPred = 1;
		DocumentMappingResult documentResult = new DocumentMappingResult();

		// ============ The Mapping ================
		// here we can switch between mapping approaches

		// numPred = mapAllMentionsInDocumentExperimental(mapper, contextScorer,
		// candidateCutoff, allDocs, docNum, docId,
		// predictedMentions, documentContext, contextQuery, numPred,
		// documentResult);
		// numPred = mapAllMentionsInDocumentOriginalGeno(mapper,
		// documentResult, predictedMentions, numPred, docNum,
		// allDocs, contextQuery, documentContext);
		documentResult = mapAllMentionsInDocumentGeneMapper(mapper, predictedMentions, acronymsInDoc, documentContext);
		collectionResult.documentMappingResults.add(documentResult);
		collectionResult.candidateRetrievalTime += documentResult.candidateRetrievalTime;
		collectionResult.disambiguationTime += documentResult.disambiguationTime;
		numPred += predictedMentions.size();

		// Mapping is done, results have been collected, now count the
		// evaluation stats
		Set<GeneMention> goldGeneMentions = new HashSet<>(ignTrainGold.get(docId));
		for (GeneMention gold : goldGeneMentions)
			goldIdsInDoc.add(gold.getId());
		if (goldIdsInDoc.contains(GeneMention.NOID)) {
			collectionResult.goldHasNoIdMentions = true;
		}
		for (MentionMappingResult mentionMappingResult : documentResult.mentionResults) {
			SynHit hit = mentionMappingResult.resultEntry;

			// if the hit is null it means that the GeneMention was rejected as
			// being a gene mention in the first place
			if (hit == MentionMappingResult.REJECTION)
				continue;
			
			GeneMention predictedMention = mentionMappingResult.mappedMention;

			Set<GeneMention> goldMentionsForPrediction = EvalToolUtilities.getGeneMentionsAtPosition(predictedMention,
					goldGeneMentions);
			if (goldMentionsForPrediction.isEmpty())
				++collectionResult.numFPPredMentions;
			else
				++collectionResult.numTPPredMentions;


			if (goldIdsInDoc.contains(hit.getId()))
				tpIdsInDoc.add(hit.getId());
			collectionResult.allPredictedIDs.add(hit.getId());
			// we exclude false positive gene mentions from this
			// statistic because I want to see the quality of the ID
			// mapping
			if (!goldMentionsForPrediction.isEmpty()) {
				if (mentionMappingResult.matchType == MentionMappingResult.MatchType.EXACT)
					++collectionResult.numExactHits;
				else
					++collectionResult.numApproxHits;
				if (mentionMappingResult.matchType == MentionMappingResult.MatchType.EXACT
						&& mentionMappingResult.ambiguityDegree > 1)
					++collectionResult.numAmbigExactHits;
			}
			GeneMention hitGoldMentions = filterGoldMentionsForCorrectId(hit, goldMentionsForPrediction);
			hitGoldMentionsInDoc.add(hitGoldMentions);
			
			
			// to check the theoretical maximum doc-level recall
			// if (!goldMentionsForPrediction.isEmpty())
			// tpIdsInDoc.add(goldMentionsForPrediction.iterator().next().id);

			// if (!goldMentionsForPrediction.isEmpty()) {
			if (null != hitGoldMentions) {
				tpStrings.add(" [TP] " + mentionMappingResult.matchType + " id=" + hit.getId() + " mention: '"
						+ hit.getMappedMention() + "'"
						+ (" " + predictedMention.getBegin() + "-" + predictedMention.getEnd()) + " mapped to: '"
						+ hit.getSynonym() + "' (score: " + hit.getMentionScore() + ")");
				
				++collectionResult.tp;
				if (mentionMappingResult.matchType == MentionMappingResult.MatchType.EXACT)
					++collectionResult.numCorrectExactHits;
				else
					++collectionResult.numCorrectApproxHits;
				if (mentionMappingResult.matchType == MentionMappingResult.MatchType.EXACT
						&& mentionMappingResult.ambiguityDegree > 1)
					++collectionResult.numCorrectAmbigExactHits;
			} else {
				String gold = null;
				if (!goldMentionsForPrediction.isEmpty()) {
					gold = "";
					for (GeneMention goldGm : goldMentionsForPrediction) {
						gold += goldGm.getText() + " (" + goldGm.getId() + "); ";
					}
				}
				fpStrings.add(" [FP] " + mentionMappingResult.matchType + " id=" + hit.getId() + " mention: '"
						+ hit.getMappedMention() + "'"
						+ (" " + predictedMention.getBegin() + "-" + predictedMention.getEnd()) + " mapped to: '"
						+ hit.getSynonym() + "' (score: " + hit.getMentionScore() + "), gold: " + gold);
				fpIdsInDoc.add(hit.getId());
				++collectionResult.fp;
			}
			if (writePredictedMentionStats && mentionMappingResult.originalCandidates != null)
				// if doing a filter classifier on basis of these data, one
				// could try the filteredCandidates as well as the
				// originalCandidates
				writeMentionStats(mapper, !goldMentionsForPrediction.isEmpty(), mentionMappingResult.originalCandidates,
						new GeneMention(predictedMention.getText(), mapper.getMappingCore().getTermNormalizer()),
						goldMentionsForPrediction);

		}

		for (GeneMention gold : Sets.difference(goldGeneMentions, hitGoldMentionsInDoc)) {
			fnStrings.add(" [FN] id=" + gold.getId() + " mention: '" + gold.getText() + "'"
					+ (" " + gold.getBegin() + "-" + gold.getEnd()));
			++collectionResult.fn;
		}
		
//		//GeneMention level
//		File fnLog = new File("fnlog.log");
//		if (!fnLog.exists()) {
//			fnLog.createNewFile();
//		}
//
//		Set<GeneMention> fnSet = getFalseNegativeMentions(goldGeneMentions, predictedMentions);
//		if (!fnSet.isEmpty()) {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(fnLog, true));
//			for (GeneMention gm : fnSet) {
//				writer.append(gm.getText() + "\n");
//				writer.flush();
//			}
//			writer.close();
//			System.out.println("False negatives: " + fnSet.size());
//		}
		
		collectionResult.numFNPredMentions += getFalseNegativeMentions(goldGeneMentions, predictedMentions).size();
		// For document level, it does not matter if we had one occasion
		// where we predicted an ID too much that was in the document anyway
		fpIdsInDoc = Sets.difference(fpIdsInDoc, goldIdsInDoc);
		// for the document level, it does not matter, where the correct
		// ID was found.
		// If we found the correct ID once but missed another
		// occurrence, that doesn't count as an FN
		fnIdsInDoc = Sets.difference(goldIdsInDoc, tpIdsInDoc);
		
		
		//Document level
		File fnLog = new File("fndoclog.log");
		if (!fnLog.exists()) {
			fnLog.createNewFile();
		}

		if (!fnIdsInDoc.isEmpty()) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fnLog, true));
			for (String fn : fnIdsInDoc) {
				writer.append(docId + ": " + fn + "\n");
				writer.flush();
			}
			writer.close();
//			System.out.println("False negatives: " + fnSet.size());
		}
		
		// this can be used quite well for error analysis
//		 if (!hasExactHits && !fpIdsInDoc.isEmpty())
//		if (!fpIdsInDoc.isEmpty())
		writeConfusionInfo(confusionFile, docId, tpStrings, fpStrings,
		 fnStrings);

		collectionResult.overallDocLevelTps.addAll(tpIdsInDoc);
		collectionResult.overallDocLevelFps.addAll(fpIdsInDoc);
		collectionResult.overallDocLevelFns.addAll(fnIdsInDoc);

		return documentResult;
	}


	private static DocumentMappingResult mapAllMentionsInDocumentGeneMapper(GeneMapper mapper,
			Collection<GeneMention> predictedMentions, Collection<Acronym> acronymsInDoc,
			String documentContext) throws GeneMapperException {

		GeneDocument genedoc = new GeneDocument();
		genedoc.setAcronyms(new HashSet<>(acronymsInDoc));
		genedoc.setGenes(new HashSet<>(predictedMentions));
		genedoc.setDocumentText(documentContext);

		DocumentMappingResult res = new DocumentMappingResult();
		res = mapper.map(genedoc);

		return res;
	}

	private static void writeConfusionInfo(File file, String docId, List<String> tpStrings, List<String> fpStrings,
			List<String> fnStrings) throws IOException {
		List<String> lines = new ArrayList<>();

		lines.add("\n-------------" + docId + "--------------\n");
		lines.add(StringUtils.join(tpStrings, "\n"));
		lines.add("\n");
		lines.add(StringUtils.join(fpStrings, "\n"));
		lines.add("\n");
		lines.add(StringUtils.join(fnStrings, "\n"));
		
		FileUtils.writeLines(file, lines, true);
	}

	private static void writeMentionStats(GeneMapper mapper, boolean isCorrect, List<SynHit> candidates,
			GeneMention mapperGeneMention, Set<GeneMention> goldMentionsForPrediction) throws IOException {

		List<Object> stats = MentionFeatureGenerator.getFullMentionStats(isCorrect, candidates, mapperGeneMention,
				goldMentionsForPrediction);
		FileUtils.write(new File("genePredictedMentionStatsIgnTrain.tsv"), StringUtils.join(stats, "\t") + "\n",
				"UTF-8", true);
	}

	private static Set<GeneMention> getFalseNegativeMentions(Set<GeneMention> goldGeneMentions,
			Collection<GeneMention> predictedMentions) {
		Set<GeneMention> ret = new HashSet<>();
		for (GeneMention gold : goldGeneMentions) {
			Range<Integer> goldSpan = gold.getOffsets();
			boolean found = false;
			for (GeneMention predictedMention : predictedMentions) {
				Range<Integer> predictedSpan = predictedMention.getOffsets();
				if (goldSpan.isOverlappedBy(predictedSpan))
					found = true;
			}
			if (!found)
				ret.add(gold);
		}
		return ret;
	}

	private static GeneMention filterGoldMentionsForCorrectId(SynHit hit, Set<GeneMention> goldMentionsForPrediction) {
		for (GeneMention gold : goldMentionsForPrediction) {
			if (gold.getId().equals(hit.getId()))
				return gold;
		}
		return null;
	}

	private static class CollectionMappingResult {
		public long disambiguationTime;
		public long candidateRetrievalTime;
		public int tp = 0;
		public int fp = 0;
		public int fn = 0;
		public int numFPPredMentions = 0;
		public int numTPPredMentions = 0;
		public int numFNPredMentions = 0;
		public boolean goldHasNoIdMentions = false;
		public int numExactHits = 0;
		public int numCorrectExactHits = 0;
		public int numAmbigExactHits = 0;
		public int numCorrectAmbigExactHits = 0;
		public int numApproxHits = 0;
		public int numCorrectApproxHits = 0;

		public Multiset<String> overallDocLevelTps = HashMultiset.create();
		public Multiset<String> overallDocLevelFps = HashMultiset.create();
		public Multiset<String> overallDocLevelFns = HashMultiset.create();
		public Set<String> allPredictedIDs = new HashSet<>();
		public List<DocumentMappingResult> documentMappingResults = new ArrayList<>();
	}
}
