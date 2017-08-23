/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.genespecies.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.pubmed.Header;
import edu.ucdenver.ccp.nlp.core.uima.annotation.CCPDocumentInformation;
import edu.ucdenver.ccp.nlp.core.uima.annotation.CCPTextAnnotation;
import edu.ucdenver.ccp.nlp.core.uima.mention.CCPClassMention;
import edu.ucdenver.ccp.nlp.core.uima.mention.CCPIntegerSlotMention;

/**
 * Gets NCBI taxonomy and NCBI gene annotations from CRAFT documents and
 * generates the appropriate JCoRe types.
 * 
 * @author faessler
 *
 */
public class CraftToJCoReConverter extends JCasAnnotator_ImplBase {
	private static final Logger log = LoggerFactory.getLogger(CraftToJCoReConverter.class);

	public static final String PARAM_CRAFT_TAX_GENE_MAP = "CraftTaxGeneMap";
	@ConfigurationParameter(name = PARAM_CRAFT_TAX_GENE_MAP)
	private File taxGeneMapFile;

	private Map<String, String> gene2tax;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		taxGeneMapFile = new File((String) aContext.getConfigParameterValue(PARAM_CRAFT_TAX_GENE_MAP));
		gene2tax = readTaxGeneMapping(taxGeneMapFile);
	}

	private Map<String, String> readTaxGeneMapping(File taxGeneMapFile) throws ResourceInitializationException {
		try {
			Map<String, String> gene2tax = new HashMap<>();
			LineIterator lineIt = FileUtils.lineIterator(taxGeneMapFile);
			int linenum = 1;
			while (lineIt.hasNext()) {
				String line = (String) lineIt.next().trim();
				if (line.startsWith("#"))
					continue;
				String[] split = line.split("\\t");
				if (split.length < 2)
					throw new IllegalArgumentException("TaxId to GeneId mapping file " + taxGeneMapFile
							+ " has less than two columns in line " + linenum);
				gene2tax.put(split[1].trim(), split[0].trim());
				++linenum;
			}
			return gene2tax;
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Type docInfoType = aJCas.getTypeSystem()
				.getType("edu.ucdenver.ccp.nlp.core.uima.annotation.CCPDocumentInformation");
		FSIterator<FeatureStructure> docInfoIt = aJCas.getFSIndexRepository().getAllIndexedFS(docInfoType);
		String docId = null;
		if (docInfoIt.hasNext()) {
			CCPDocumentInformation docInfo = (CCPDocumentInformation) docInfoIt.next();
			docId = docInfo.getDocumentID();
			Header header = new Header(aJCas);
			header.setDocId(docId);
			header.addToIndexes();
		}
		log.debug("DocId: " + docId);
		// For the CRAFT corpus, we can't directly access organism or gene
		// annotations. We just iterate over all "TextAnnotations" and pick
		// those we need.
		FSIterator<Annotation> it = aJCas
				.getAnnotationIndex(edu.ucdenver.ccp.nlp.core.uima.annotation.CCPTextAnnotation.type).iterator();
		while (it.hasNext()) {
			CCPTextAnnotation ann = (CCPTextAnnotation) it.next();
			CCPClassMention classMention = ann.getClassMention();
			String mentionName = classMention.getMentionName();
			// We recognize species annotations via the "NCBITaxon" qualifier /
			// prefix
			if (mentionName.matches("NCBITaxon:[0-9]*")) {
				String taxId = mentionName.substring(mentionName.indexOf(':') + 1);
				Organism organism = new Organism(aJCas);
				ResourceEntry speciesEntry = new ResourceEntry(aJCas, ann.getBegin(), ann.getEnd());
				speciesEntry.setEntryId(taxId);
				speciesEntry.setSource("NCBI Taxonomy");
				FSArray speciesArray = new FSArray(aJCas, 1);
				speciesArray.set(0, speciesEntry);
				organism.setResourceEntryList(speciesArray);
				organism.addToIndexes();
			}
			// We recognize gene annotations via the "Entrez Gene sequence" name
			if (mentionName.equals("Entrez Gene sequence")) {
				FSArray slotMentions = classMention.getSlotMentions();
				if (slotMentions.size() > 1)
					throw new NotImplementedException("Entrez gene sequence " + ann
							+ " has more than one slot mentions, this is currently not handled");
				CCPIntegerSlotMention slotMention = (CCPIntegerSlotMention) slotMentions.get(0);
				FSArray geneResourceMentions = new FSArray(aJCas, slotMention.getSlotValues().size());
				// One textual expression might actually refer to multiple genes
				// (e.g. MTOR in human and mice...) which is
				// why there are multiple "slots".
				Set<String> taxIdsForGene = new HashSet<>();
				for (int i = 0; i < slotMention.getSlotValues().size(); i++) {
					int geneId = slotMention.getSlotValues(i);
					GeneResourceEntry geneResourceEntry = new GeneResourceEntry(aJCas, ann.getBegin(), ann.getEnd());
					geneResourceEntry.setEntryId(String.valueOf(geneId));
					// try {
					// FileUtils.write(new File("craftgenes.lst"), geneId +
					// "\n", "UTF-8", true);
					// } catch (IOException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
					geneResourceEntry.setSource("NCBI Gene");
					geneResourceMentions.set(i, geneResourceEntry);
					if (gene2tax.containsKey(String.valueOf(geneId))) {
						String taxId = gene2tax.get(String.valueOf(geneId));
						taxIdsForGene.add(taxId);
						geneResourceEntry.setTaxonomyId(taxId);
					}
				}
				StringArray geneSpecies = null;
				if (!taxIdsForGene.isEmpty()) {
					geneSpecies = new StringArray(aJCas, taxIdsForGene.size());
					int i = 0;
					for (String taxId : taxIdsForGene) {
						geneSpecies.set(i, taxId);
						++i;
					}
				}

				Gene gene = new Gene(aJCas, ann.getBegin(), ann.getEnd());
				gene.setResourceEntryList(geneResourceMentions);
				if (geneSpecies != null)
					gene.setSpecies(geneSpecies);
				if (gene.getEnd() >= aJCas.getDocumentText().length())
				System.out.println(gene.getEnd() + " " + aJCas.getDocumentText().length());
				gene.addToIndexes();
			}
		}
	}

}
