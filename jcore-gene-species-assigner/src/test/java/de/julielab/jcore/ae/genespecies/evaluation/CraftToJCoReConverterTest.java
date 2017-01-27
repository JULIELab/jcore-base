package de.julielab.jcore.ae.genespecies.evaluation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;

public class CraftToJCoReConverterTest {
	@Test
	public void testConversion() throws UIMAException, IOException {
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
		AnalysisEngine craftConverter = AnalysisEngineFactory.createEngine(CraftToJCoReConverter.class, CraftToJCoReConverter.PARAM_CRAFT_TAX_GENE_MAP, "src/test/resources/craft.taxid2gene.map");
			craftReader.getNext(jCas.getCas());
			craftConverter.process(jCas);
		
		 FSIterator<Annotation> orgIt =
		 jCas.getAnnotationIndex(Organism.type).iterator();
		 FSIterator<Annotation> geneIt =
		 jCas.getAnnotationIndex(Gene.type).iterator();
		 assertTrue(jCas.getDocumentText().length() > 1000);
		 assertTrue(orgIt.hasNext());
		 assertTrue(geneIt.hasNext());
		
		 Organism organism = (Organism) orgIt.next();
		 assertNotNull(organism.getResourceEntryList());
		 assertTrue(organism.getResourceEntryList().size() > 0);
		 ResourceEntry orgEntry = organism.getResourceEntryList(0);
		 assertNotNull(orgEntry.getEntryId());
		 assertNotNull(orgEntry.getSource());
		
		 Gene gene = (Gene) geneIt.next();
		 assertNotNull(gene.getResourceEntryList());
		 assertTrue(gene.getResourceEntryList().size() > 0);
		 GeneResourceEntry geneEntry = (GeneResourceEntry)
		 gene.getResourceEntryList(0);
		 assertNotNull(geneEntry.getEntryId());
		 assertNotNull(geneEntry.getSource());
		 assertNotNull("Gene resource entry for gene " + geneEntry.getEntryId() + " hasn't set its taxonomy ID",
		 geneEntry.getTaxonomyId());
		 assertNotNull("Gene has not set any species", gene.getSpecies());
	}
}
