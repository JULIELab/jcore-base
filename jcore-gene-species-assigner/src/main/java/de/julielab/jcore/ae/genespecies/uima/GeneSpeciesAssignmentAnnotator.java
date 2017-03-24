package de.julielab.jcore.ae.genespecies.uima;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.pubmed.Header;
import edu.ucdenver.ccp.nlp.core.uima.annotation.CCPDocumentInformation;

/**
 * This analysis engine assigns each Gene in the CAS index a taxonomy
 * identifier.
 * 
 * @author faessler
 *
 */
public class GeneSpeciesAssignmentAnnotator extends JCasAnnotator_ImplBase {
	private static final Logger log = LoggerFactory.getLogger(GeneSpeciesAssignmentAnnotator.class);

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> geneIt = aJCas.getAnnotationIndex(Gene.type).iterator();
		FSIterator<Annotation> sentenceIt = aJCas.getAnnotationIndex(Sentence.type).iterator();
		FSIterator<Annotation> organismsIt = aJCas.getAnnotationIndex(Organism.type).iterator();
		
		Map<String, HashSet<String>> organismsMap = new HashMap<String, HashSet<String>>(10);
		//Used for the majority indicator
		Map<String, Integer> organismCounter = new HashMap<String, Integer>(10);
		
		// We iterate over the whole text twice. Once over all organisms to
		// build up statistics for the majority indicator and to look for
		// patterns to be used as prefixes. The second time, we evaluate the
		// text sentence-wise to make use of the forward and backward indicators.
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
		
			Integer counter = organismCounter.get(taxId);
			counter = (null == counter)? 1: ++counter;
			organismCounter.put(taxId, counter);

			//Map all instances to their IDs
			if (organismsMap.containsKey(taxId)) {
				HashSet<String> idSet = organismsMap.get(taxId);
				idSet.add(resourceEntry.getCoveredText());
			} else {
				HashSet<String> idSet = new HashSet<String>();
				idSet.add(resourceEntry.getCoveredText());
				organismsMap.put(taxId, idSet);
			}		
		}
	
		//Map initial letter of species instance to tax_ID
		Set<String> taxIds = organismsMap.keySet();
		if (taxIds.isEmpty()) {
			FSIterator<Annotation> headerIt = aJCas.getAnnotationIndex(Header.type).iterator();
			if (headerIt.hasNext()) {
				Header header = (Header) headerIt.next();
				log.debug("Doc ID " + header.getDocId() + ": No organisms");
			} else {
				log.debug("No organisms");
			}
			return;
		}
		//TODO: Can the set be empty?
		Map<Character, String> prefixMap = new HashMap<Character, String>(10);
		for (String taxId : taxIds) {
			Set<String> species = organismsMap.get(taxId);
			if (species.size() == 1) {
				String instance = species.iterator().next();
				char prefixChar = instance.toLowerCase().charAt(0);
				prefixMap.put(prefixChar, taxId);
			} else {
				int maxLength = 0;
				String longest = "";
				for (String s : species) {
					if (s.length() > maxLength) {
						maxLength = s.length();
						longest = s;
					}
				}
				char prefixChar = longest.toLowerCase().charAt(0);
				prefixMap.put(prefixChar, taxId);
				//TODO: Case of multiple organisms with same initial
			}
		}
		
		// Setup prefix indicator
		Pattern prefix = Pattern.compile("^[a-z][A-Z].*");
		while (sentenceIt.hasNext()) {
			Sentence sentence = (Sentence) sentenceIt.next();
			organismsIt = aJCas.getAnnotationIndex(Organism.type).subiterator(sentence);
			TreeMap<Integer, String> organismPosition = new TreeMap<Integer, String>();

			while (organismsIt.hasNext()) {
				organismPosition.clear();
				Organism organism = (Organism) organismsIt.next();

				FSArray resourceEntryList = organism.getResourceEntryList();
				if (null == resourceEntryList || resourceEntryList.size() == 0)
					continue;

				ResourceEntry resourceEntry = (ResourceEntry) resourceEntryList.get(0);
				String taxId = resourceEntry.getEntryId();
				organismPosition.put(organism.getBegin(), taxId);	
			}

			HashMap<String, TreeMap<String, Integer>> spreadMap = new HashMap<>(20);
			while (geneIt.hasNext()) {
				Gene gene = (Gene) geneIt.next();
				String geneText = gene.getCoveredText();
				String geneTaxId = null;
				
				// Apply prefix rule
				Matcher m = prefix.matcher(geneText);
				if (m.matches()) {
					String taxId = prefixMap.get(geneText.charAt(0));
					if (null != taxId) {
						geneTaxId = taxId;
					}
				}
				
				if (null == geneTaxId && !organismPosition.isEmpty()) {
					// Forward indicator
					Integer key = organismPosition.lowerKey(gene.getBegin());
					if (null != key) {
						geneTaxId = organismPosition.get(key);
						if (spreadMap.containsKey(geneText)) {
							TreeMap<String, Integer> frequency = spreadMap.get(geneText);
							Integer freq = frequency.get(geneTaxId);
							if (null != freq) {
								++freq;
								frequency.put(geneTaxId, freq);
							} else {
								frequency.put(geneTaxId, 1);
							}
						} else {
							TreeMap<String, Integer> frequency = new TreeMap<>();
							frequency.put(geneTaxId, 1);
							spreadMap.put(geneText, frequency);
						}
					} else {
					// Backward indicator
						key = organismPosition.higherKey(gene.getBegin());
						if (null != key) {
							geneTaxId = organismPosition.get(key);
							if (spreadMap.containsKey(geneText)) {
								TreeMap<String, Integer> frequency = spreadMap.get(geneText);
								Integer freq = frequency.get(geneTaxId);
								if (null != freq) {
									++freq;
									frequency.put(geneTaxId, freq);
								} else {
									frequency.put(geneTaxId, 1);
								}
							} else {
								TreeMap<String, Integer> frequency = new TreeMap<>();
								frequency.put(geneTaxId, 1);
								spreadMap.put(geneText, frequency);
							}
						}
					}
					if (null != geneTaxId && geneTaxId.equals("10088")) {
						geneTaxId = "10090";
					}
					
					StringArray geneSpecies = new StringArray(aJCas, 1);
					geneSpecies.set(0, geneTaxId);
					gene.setSpecies(geneSpecies);
				}
			}
			
			geneIt.moveToFirst();
//			geneIt = aJCas.getAnnotationIndex(Gene.type).iterator();
			while (geneIt.hasNext()) {
				Gene gene = (Gene) geneIt.next();
				
				StringArray sa = gene.getSpecies();
				if (null != sa) {
					continue;
				}
				
				String geneText = gene.getCoveredText();
				String geneTaxId = null;

				
				// Spreading the assignments
				if (spreadMap.containsKey(geneText)) {
					TreeMap<String, Integer> frequency = spreadMap.get(geneText);
					if (frequency.size() == 1) {
						geneTaxId = frequency.firstKey();
					} else {
						int max = 0;
						String species = null;
						for (Entry<String, Integer> s : frequency.entrySet()) {
							if (s.getValue() > max) {
								max = s.getValue();
								species = s.getKey();
							}
						}
						geneTaxId = species;
					}
				}
				
				// Use majority indicator
				if (null == geneTaxId && organismCounter.size() > 0) {
					int max = 0;
					//Used for tie-breaks
					TreeSet<String> ids = new TreeSet<String>();
	
					for (String k : organismCounter.keySet()) {
						int occurrences = organismCounter.get(k);
						if (occurrences >= max) {
							if (occurrences > max) {
								ids.clear();
								max = occurrences;
							}
							ids.add(k);
						}
					}
	
//					if (ids.contains("9606")){ // "human"
//						geneTaxId = "9606";
//					} else {
//	//					System.out.println(ids.size());
						geneTaxId = ids.iterator().next();
//					}
	//				System.out.println("Id:" + geneTaxId);
				}
				
				if (null != geneTaxId && geneTaxId.equals("10088")) {
					geneTaxId = "10090";
				}
				
				StringArray geneSpecies = new StringArray(aJCas, 1);
				geneSpecies.set(0, geneTaxId);
				gene.setSpecies(geneSpecies);
			}
		}
	}

}
