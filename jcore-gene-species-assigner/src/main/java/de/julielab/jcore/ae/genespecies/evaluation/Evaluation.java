package de.julielab.jcore.ae.genespecies.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.SAXException;

import de.julielab.jcore.ae.genespecies.uima.GeneSpeciesAssignmentAnnotator;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.ResourceEntry;

public class Evaluation {

	private static AnalysisEngine speciesAssigmentEngine;
	private static AnalysisEngine sentenceSpliter;

	public static void main(String[] args) throws IOException, UIMAException, SAXException {

		speciesAssigmentEngine = AnalysisEngineFactory.createEngine(GeneSpeciesAssignmentAnnotator.class);
		sentenceSpliter = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
		
//		evaluateOnIgnTrain();
		evaluateOnCraft();
	}

	private static void evaluateOnCraft() throws UIMAException, IOException {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types",
				"edu.ucdenver.ccp.nlp.core.uima.TypeSystem");
		// Configure the CraftReader to load entrez gene and ncbi taxonomy
		// annotations; possible values:
		// TEXT_ONLY
		// GOCC
		// GOBP
		// GOMF
		// CL
		// EG
		// NCBITAXON
		// CHEBI
		// PR
		// SO
		// TREEBANK
		// TYPO
		CollectionReader craftReader = CollectionReaderFactory.createReader(
				"edu/ucdenver/ccp/craft/uima/cr/CcpCraftCollectionReader",
				"edu.ucdenver.ccp.craft.uima.cr.CraftCollectionReader.conceptTypesToLoad",
				new String[] { "EG", "NCBITAXON" });
		AnalysisEngine craftConverter = AnalysisEngineFactory.createEngine(CraftToJCoReConverter.class,
				CraftToJCoReConverter.PARAM_CRAFT_TAX_GENE_MAP, "src/test/resources/craft.taxid2gene.map");
		
		EvalStats evalStats = new EvalStats();
		while (craftReader.hasNext()) {
			craftReader.getNext(jCas.getCas());
			craftConverter.process(jCas);
			evaluateForCas(jCas, evalStats);
			jCas.reset();
		}
		System.out
				.println((evalStats.correct / (double) evalStats.all * 100) + "% of species assignments are correct.");
	}

	private static void evaluateOnIgnTrain() throws IOException, UIMAException, SAXException {
		File ignTrainXmiDir = new File("eval/ign-train");
		// get all the XMI files of the IGN train corpus which have been tagged
		// for Organisms by Linnaeus and contain Gene mentions with resource
		// entries holding their taxonomy ID most of the time.
		File[] xmiFiles = ignTrainXmiDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !name.equals(".DS_Store");
			}
		});
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		EvalStats evalStats = new EvalStats();
		for (int i = 0; i < xmiFiles.length; i++) {
			File xmiFile = xmiFiles[i];
			InputStream is = new FileInputStream(xmiFile);
			if (xmiFile.getName().endsWith(".gz"))
				is = new GZIPInputStream(is);

			// load the XMI file into the CAS
			XmiCasDeserializer.deserialize(is, jCas.getCas());
			// The species is currently set correctly on all genes from the gold
			// data. Clear that to avoid nice looking results by accident.
			evaluateForCas(jCas, evalStats);
		}
		System.out
				.println((evalStats.correct / (double) evalStats.all * 100) + "% of species assignments are correct.");
	}

	private static void evaluateForCas(JCas jCas, EvalStats evalStats) throws AnalysisEngineProcessException {
		clearSpeciesFeatureOnGenes(jCas);
		
		// do sentence splitting which is required for the species assignment
		sentenceSpliter.process(jCas);

		// run the assignment engine
		speciesAssigmentEngine.process(jCas);

		// now iterate over the genes and check
		FSIterator<Annotation> geneIt = jCas.getAnnotationIndex(Gene.type).iterator();
		while (geneIt.hasNext()) {
			Gene gene = (Gene) geneIt.next();
			String assignedTaxId = null;
			if (gene.getSpecies() != null && gene.getSpecies().size() > 0)
				assignedTaxId = gene.getSpecies(0);
			GeneResourceEntry resourceEntry = (GeneResourceEntry) gene.getResourceEntryList(0);
			// In some cases, the taxonomy is actually not set in the IGN
			// train gold data
			if (resourceEntry.getTaxonomyId() != null) {
				if (assignedTaxId != null && resourceEntry.getTaxonomyId().equals(assignedTaxId))
					++evalStats.correct;
				++evalStats.all;
			}
		}
	}

	private static void clearSpeciesFeatureOnGenes(JCas jCas) {
		FSIterator<Annotation> geneIt = jCas.getAnnotationIndex(Gene.type).iterator();
		while (geneIt.hasNext()) {
			Gene gene = (Gene) geneIt.next();
			gene.setSpecies(null);
		}
	}

	private static class EvalStats {
		int correct = 0;
		int all = 0;
	}

}
