package de.julielab.jules.ae.genemapper.eval.tools;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.Range;
import org.apache.lucene.search.BooleanQuery;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import de.julielab.evaluation.entities.EntityEvaluationResult;
import de.julielab.evaluation.entities.EntityEvaluator;
import de.julielab.evaluation.entities.EvaluationData;
import de.julielab.evaluation.entities.EvaluationDataEntry;
import de.julielab.jules.ae.genemapper.DocumentMappingResult;
import de.julielab.jules.ae.genemapper.GeneMapper;
import de.julielab.jules.ae.genemapper.MentionMappingResult;
import de.julielab.jules.ae.genemapper.genemodel.Acronym;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.genemodel.SpeciesCandidates;
import de.julielab.jules.ae.genemapper.genemodel.SpeciesMention;
import de.julielab.jules.ae.genemapper.utils.ContextUtils;
import de.julielab.jules.ae.genemapper.utils.GeneMapperException;
import de.julielab.jules.ae.genemapper.utils.OffsetMap;
import de.julielab.jules.ae.genemapper.utils.OffsetSet;

public class ISGNEvaluator {
	private static final Logger log = LoggerFactory.getLogger(ISGNEvaluator.class);

	private static DecimalFormat df = new DecimalFormat("#.00");

	public static void main(String[] args) throws IOException, GeneMapperException, UIMAException {

		long time = System.currentTimeMillis();
		String propertiesFile = "src/main/resources/isgnGeneMapper.properties";
		GeneMapper mapper = new GeneMapper(new File(propertiesFile));
		
		String titleSpecies = "data/eval_data/isgn_data/bc100-titles-species.tsv";
		String meshSpecies = "data/eval_data/isgn_data/bc100-mesh-species.tsv";
		String textSpecies = "data/eval_data/isgn_data/bc100-species.tsv.gz";
		
		String sentences = "data/eval_data/isgn_data/bc100-sentences.tsv.gz";
		String chunks = "data/eval_data/isgn_data/bc100-chunks.tsv.gz";

		evaluate(mapper, "data/eval_data/isgn_data/bc_100.genelist",
				"data/eval_data/isgn_data/bc100-genes.tsv.gz", "data/eval_data/isgn_data/bc100-acronyms.ann.gz",
				"data/eval_data/isgn_data/bc100-text", titleSpecies, meshSpecies, textSpecies, sentences, chunks, true);

		time = System.currentTimeMillis() - time;
		System.out.println(String.format("Eval took %s seconds (%s minutes)", time / 1000d, (time / 1000d) / 60d));
	}

	public static EntityEvaluationResult evaluate(GeneMapper mapper, String goldGeneListPath, String predictedMentionsPath,
			String acronymsPath, String documentsPath, String titlesPath, String meshPath, String textPath,
			String sentencePath, String chunkPath, boolean printResult) throws IOException, GeneMapperException, UIMAException {		
		Multimap<String, GeneMention> goldIds = EvalToolUtilities.readGoldIds(goldGeneListPath);
		Multimap<String, GeneMention> predictedMentions = EvalToolUtilities
				.readMentionsWithOffsetsAndSpecies(predictedMentionsPath);
		Map<String, String> documentContexts = EvalToolUtilities.readGeneContexts(documentsPath);
		Multimap<String, Acronym> acronyms = EvalToolUtilities.readAcronymAnnotations(acronymsPath);
		Multimap<String, String> titleSpecies = EvalToolUtilities.readTitleSpecies(titlesPath);
		Multimap<String, String> meshSpecies = EvalToolUtilities.readMeshSpecies(meshPath);
		Map<String, OffsetMap<SpeciesMention>> textSpecies = EvalToolUtilities.readTextSpecies(textPath);
		Multimap<String, Range<Integer>> sentences = EvalToolUtilities.readSentenceOffsets(sentencePath);
		Map<String, OffsetMap<String>> chunks = EvalToolUtilities.readChunkOffsets(chunkPath);
		
		EvaluationData predictedData = new EvaluationData();
		for (String docId : Sets.union(goldIds.keySet(), predictedMentions.keySet())) {
			GeneDocument geneDocument = new GeneDocument();
			geneDocument.setGenes(new HashSet<>(predictedMentions.get(docId)));
			geneDocument.setAcronyms(new HashSet<>(acronyms.get(docId)));
			geneDocument.setSpecies(new SpeciesCandidates(
					new HashSet<>(titleSpecies.get(docId)),
					new HashSet<>(meshSpecies.get(docId)),
									textSpecies.get(docId)
					));
			
			geneDocument.setSentences(new OffsetSet(sentences.get(docId)));
			geneDocument.setChunks(chunks.get(docId));
			// set document context and context query to the genes in this
			// document
			String documentContext = documentContexts.get(docId);
			geneDocument.setDocumentText(documentContext);
			BooleanQuery contextQuery = ContextUtils.makeContextQuery(documentContext);
			for (GeneMention gm : geneDocument.getGenes()) {
				gm.setDocumentContext(documentContext);
				gm.setContextQuery(contextQuery);
			}

			DocumentMappingResult documentResult = mapper.map(geneDocument);
			for (MentionMappingResult mentionResult : documentResult.mentionResults) {
				String id = mentionResult.resultEntry == MentionMappingResult.REJECTION ? null
						: mentionResult.resultEntry.getId();
				predictedData.add(new EvaluationDataEntry(docId, id, mentionResult.mappedMention.getBegin(),
						mentionResult.mappedMention.getEnd()));
			}

		}

		EvaluationData goldData = EvaluationData.readDataFile(new File(goldGeneListPath));
		
		EntityEvaluator evaluator = new EntityEvaluator();
		EntityEvaluationResult evaluationResult = evaluator.evaluate(goldData, predictedData);

		if (printResult) {
			System.out.println(evaluationResult.getEvaluationReportLong());
		}
		
		return evaluationResult;
	}

}
