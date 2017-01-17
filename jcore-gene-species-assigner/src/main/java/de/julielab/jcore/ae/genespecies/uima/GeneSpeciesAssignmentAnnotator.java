package de.julielab.jcore.ae.genespecies.uima;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;

/**
 * This analysis engine assigns each Gene in the CAS index a taxonomy
 * identifier.
 * 
 * @author faessler
 *
 */
public class GeneSpeciesAssignmentAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> organismsIt = aJCas.getAnnotationIndex(Organism.type).iterator();
		FSIterator<Annotation> geneIt = aJCas.getAnnotationIndex(Gene.type).iterator();

		while (organismsIt.hasNext()) {
			// We store species information in the Organism type.
			Organism organism = (Organism) organismsIt.next();
			// However, an organism does not directly know its database entry.
			// The reason is that we could possibly want to assign identifiers
			// from different databases. Thus, every database identifier for a
			// species is stored in a single instance of a ResourceEntry - the
			// resource here being the database we take the identifier from.
			FSArray resourceEntryList = organism.getResourceEntryList();
			if (null == resourceEntryList || resourceEntryList.size() == 0)
				continue;
			// For now we assume a single resource entry, viz. the NCBI Taxonomy
			// ID.
			ResourceEntry resourceEntry = (ResourceEntry) resourceEntryList.get(0);
			String taxId = resourceEntry.getEntryId();

			// TODO continue doing something with the taxonomy ID
		}

		while (geneIt.hasNext()) {
			Gene gene = (Gene) geneIt.next();
			String geneText = gene.getCoveredText();

			String geneTaxId = null;
			// TODO determine the species for this gene

			StringArray geneSpecies = new StringArray(aJCas, 1);
			geneSpecies.set(0, geneTaxId);
			gene.setSpecies(geneSpecies);
		}
	}

}
