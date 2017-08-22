package de.julielab.jules.ae.genemapper.genemodel;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.Range;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fulmicoton.multiregexp.MultiPatternSearcher;
import com.fulmicoton.multiregexp.MultiPattern;

import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention;
import de.julielab.jules.ae.genemapper.genemodel.GeneMention.GeneTagger;
import junit.framework.TestCase;

public class GeneDocumentTest extends TestCase {
	
	NavigableSet<GeneMention> sortedGenes;
	
	@Before
	public void setUp() {
		this.sortedGenes = new TreeSet<GeneMention>(new Comparator<GeneMention>() {

			@Override
			public int compare(GeneMention gm1, GeneMention gm2) {
				if (gm1.getBegin() == gm2.getBegin()) {
					if (gm1.getEnd() == gm2.getEnd()) {
						return 0;
					} else if (gm1.getEnd() < gm2.getEnd()) {
						return -1;
					} else {
						return 1;
					}
				} else if (gm1.getBegin() < gm2.getBegin()) {
					return -1;
				} else {
					return 1;
				}
			}
			
		});
	}
	
	/**
	 * Logger for this class
	 */
	private static final Logger log = LoggerFactory.getLogger(GeneDocumentTest.class);

	@Test
	public void testUnifyGenesPrioritizeJnet() {	
		GeneDocument geneDoc = new GeneDocument();
		//A set with a defined order of elements for testing purposes
		Set<GeneMention> genes = new LinkedHashSet<>();
		
		GeneMention gm1 = new GeneMention();
		gm1.setOffsets(Range.between(5,11));
		gm1.setTagger(GeneTagger.GAZETTEER);
		gm1.setText("IL3001");
		genes.add(gm1);
		log.debug("Offsets: {} and {}", gm1.getBegin(), gm1.getEnd());

		GeneMention gm2 = new GeneMention();
		gm2.setOffsets(Range.between(5,20));
		gm2.setTagger(GeneTagger.JNET);
		gm2.setText("IL3001 receptor");
		genes.add(gm2);
		log.debug("Offsets: {} and {}", gm2.getBegin(), gm2.getEnd());
		
		GeneMention gm3 = new GeneMention();
		gm3.setOffsets(Range.between(5,11));
		gm3.setTagger(GeneTagger.JNET);
		gm3.setText("IL3001");
		genes.add(gm3);
		log.debug("Offsets: {} and {}", gm3.getBegin(), gm3.getEnd());
		
		geneDoc.setGenes(genes);
		geneDoc.unifyGenesPrioritizeTagger(sortedGenes, GeneTagger.JNET);
		for (GeneMention gm : geneDoc.getGenes()) {
			log.debug("Offsets: {} and {}", gm.getBegin(), gm.getEnd());
			log.debug("Tagger: {}", gm.getTagger());
		}
		assertEquals(1, geneDoc.getGenes().size());
		assertEquals(GeneMention.GeneTagger.JNET, geneDoc.getGenes().iterator().next().getTagger());
	}

	@Test
	public void testUnifyGenesPrioritizeGazetteer() {		
		GeneDocument geneDoc = new GeneDocument();
		//A set with a defined order of elements for testing purposes
		Set<GeneMention> genes = new LinkedHashSet<>();
		
		GeneMention gm1 = new GeneMention();
		gm1.setOffsets(Range.between(5,11));
		gm1.setTagger(GeneTagger.JNET);
		gm1.setText("IL3001");
		genes.add(gm1);
		log.debug("Offsets: {} and {}", gm1.getBegin(), gm1.getEnd());

		GeneMention gm2 = new GeneMention();
		gm2.setOffsets(Range.between(5,20));
		gm2.setTagger(GeneTagger.JNET);
		gm2.setText("IL3001 receptor");
		genes.add(gm2);
		log.debug("Offsets: {} and {}", gm2.getBegin(), gm2.getEnd());
		
		GeneMention gm3 = new GeneMention();
		gm3.setOffsets(Range.between(5,11));
		gm3.setTagger(GeneTagger.GAZETTEER);
		gm3.setText("IL3001");
		genes.add(gm3);
		log.debug("Offsets: {} and {}", gm3.getBegin(), gm3.getEnd());
		
		geneDoc.setGenes(genes);
		geneDoc.unifyGenesPrioritizeTagger(sortedGenes, GeneTagger.GAZETTEER);
		for (GeneMention gm : geneDoc.getGenes()) {
			log.debug("Offsets: {} and {}", gm.getBegin(), gm.getEnd());
			log.debug("Tagger: {}", gm.getTagger());
		}
		assertEquals(1, geneDoc.getGenes().size());
		assertEquals(GeneMention.GeneTagger.GAZETTEER, geneDoc.getGenes().iterator().next().getTagger());
	}

	@Test
	public void testUnifyGenesDontUnify() {		
		GeneDocument geneDoc = new GeneDocument();
		//A set with a defined order of elements for testing purposes
		Set<GeneMention> genes = new LinkedHashSet<>();
		
		GeneMention gm1 = new GeneMention();
		gm1.setOffsets(Range.between(5,10));
		gm1.setTagger(GeneTagger.JNET);
		gm1.setText("IL3001");
		genes.add(gm1);
		log.debug("Offsets: {} and {}", gm1.getBegin(), gm1.getEnd());

		GeneMention gm2 = new GeneMention();
		gm2.setOffsets(Range.between(15,29));
		gm2.setTagger(GeneTagger.JNET);
		gm2.setText("IL3001 receptor");
		genes.add(gm2);
		log.debug("Offsets: {} and {}", gm2.getBegin(), gm2.getEnd());
		
		GeneMention gm3 = new GeneMention();
		gm3.setOffsets(Range.between(35,40));
		gm3.setTagger(GeneTagger.GAZETTEER);
		gm3.setText("IL3001");
		genes.add(gm3);
		log.debug("Offsets: {} and {}", gm3.getBegin(), gm3.getEnd());
		
		geneDoc.setGenes(genes);
		geneDoc.unifyGenesPrioritizeTagger(sortedGenes, GeneTagger.GAZETTEER);
		for (GeneMention gm : geneDoc.getGenes()) {
			log.debug("Offsets: {} and {}", gm.getBegin(), gm.getEnd());
			log.debug("Tagger: {}", gm.getTagger());
		}
		assertEquals(3, geneDoc.getGenes().size());
	}

	@Test
	public void testRemoveSpeciesMention() {
		GeneDocument geneDoc = new GeneDocument();
		
		MultiPatternSearcher searcher = MultiPattern.of(
	            "yeast ",
	            "mammalian ",
	            "Drosophila (melanogaster )?",
	            "mouse ",
	            "murine ", 
	            "human ",
	            "Saccharomyces (cerevisiae )?",
	            "E. coli ",
	            "Escherichia coli",
	            "Arabidopsis (thaliana )?",
	            "Tetrahymena ",
	            "eukariotic ",
	            "rabbit ",
	            "goat ",
	            "rat ",
	            "Caenorhabditis (elegans )?"
	    ).searcher();
		
		Set<GeneMention> genes = new LinkedHashSet<>();

		GeneMention gm1 = new GeneMention();
		gm1.setOffsets(Range.between(100,126));
		gm1.setText("E. coli Hsp70 protein DnaK");
		genes.add(gm1);
		geneDoc.setGenes(genes);
		
		GeneMention gm2 = new GeneMention();
		gm2.setOffsets(Range.between(200,244));
		gm2.setText("Saccharomyces cerevisiae SCH9 protein kinase");
		genes.add(gm2);
		geneDoc.setGenes(genes);
		
		geneDoc.removeSpeciesMention(searcher);
		int[] expectedBegin = {108, 225};
		String[] expectedText = {"Hsp70 protein DnaK", "SCH9 protein kinase"};

		Iterator<GeneMention> gmIt = genes.iterator();
		for (int i = 0; i < expectedBegin.length; ++i) {
			GeneMention gm = gmIt.next();
			assertEquals(expectedBegin[i], (int) gm.getOffsets().getMinimum());
			assertEquals(expectedText[i], gm.getText());			
		}
	}
	
}
