/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.banner;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.pubmed.InternalReference;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BANNERAnnotatorTest {
	private final static Logger log = LoggerFactory.getLogger(BANNERAnnotatorTest.class);
	@Test
	public void testProcess() throws Exception {
		// just tag a single sentence with a test model that actually used that sentence as training data.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-meta-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		// this is sentence P00055040A0000 from the test BC2GM train data
		jcas.setDocumentText(
				"Ten out-patients with pustulosis palmaris et plantaris were examined with direct immunofluorescence (IF) technique for deposition of fibrinogen, fibrin or its degradation products (FR-antigen) in affected and unaffected skin, together with heparin-precipitable fraction (HPF), cryoglobulin and total plasma fibrinogen in the blood.");
		new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();

		AnalysisEngine bannerAe = AnalysisEngineFactory.createEngine(BANNERAnnotator.class,
				BANNERAnnotator.PARAM_CONFIG_FILE, "src/test/resources/banner_ae_test.xml", BANNERAnnotator.PARAM_TYPE_MAPPING, new String[] {"GENE=de.julielab.jcore.types.Gene"});
		bannerAe.process(jcas);

		// expected result from the GENE.eval.small file:
		// P00055040A0000|116 125|fibrinogen
		// P00055040A0000|127 132|fibrin
		// P00055040A0000|158 167|FR-antigen
		// P00055040A0000|243 254|cryoglobulin
		// P00055040A0000|269 278|fibrinogen
		// However, we ignore the offsets because the eval offsets ignore white spaces
		List<Gene> geneList = new ArrayList<Gene>(JCasUtil.select(jcas, Gene.class));
		assertEquals("fibrinogen", geneList.get(0).getCoveredText());
		assertEquals("fibrin", geneList.get(1).getCoveredText());
		assertEquals("FR-antigen", geneList.get(2).getCoveredText());
		assertEquals("cryoglobulin", geneList.get(3).getCoveredText());
		assertEquals("fibrinogen", geneList.get(4).getCoveredText());
	}

	@Test
	public void testInternalReferenceExclusion() throws Exception {
		// Internal references in papers, e.g. for bibliography, often appear as numbers. If such a number is
		// directly appended to a gene name, it is mostly included into the gene name by BANNER.
		// Thus, such reference spans are removed afterwards in the annotator and this test is checking that it works.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
				"de.julielab.jcore.types.jcore-document-meta-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-structure-pubmed-types");
		// this is sentence P00055040A0000 from the test BC2GM train data EXCEPT the '19' following 'fibrinogen' which
		// is our internal reference for this test.
		jcas.setDocumentText(
				"Ten out-patients with pustulosis palmaris et plantaris were examined with direct immunofluorescence (IF) technique for deposition of fibrinogen19, fibrin or its degradation products (FR-antigen) in affected and unaffected skin, together with heparin-precipitable fraction (HPF), cryoglobulin and total plasma fibrinogen in the blood.");
		new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();
		new InternalReference(jcas, 143, 145).addToIndexes();
		AnalysisEngine bannerAe = AnalysisEngineFactory.createEngine(BANNERAnnotator.class,
				BANNERAnnotator.PARAM_CONFIG_FILE, "src/test/resources/banner_ae_test.xml", BANNERAnnotator.PARAM_TYPE_MAPPING, new String[] {"GENE=de.julielab.jcore.types.Gene"});
		bannerAe.process(jcas);

		// expected result from the GENE.eval.small file:
		// P00055040A0000|116 125|fibrinogen
		// P00055040A0000|127 132|fibrin
		// P00055040A0000|158 167|FR-antigen
		// P00055040A0000|243 254|cryoglobulin
		// P00055040A0000|269 278|fibrinogen
		// However, we ignore the offsets because the eval offsets ignore white spaces
		List<Gene> geneList = new ArrayList<Gene>(JCasUtil.select(jcas, Gene.class));
		assertEquals("fibrinogen", geneList.get(0).getCoveredText());
		assertEquals("fibrin", geneList.get(1).getCoveredText());
		assertEquals("FR-antigen", geneList.get(2).getCoveredText());
		assertEquals("cryoglobulin", geneList.get(3).getCoveredText());
		assertEquals("fibrinogen", geneList.get(4).getCoveredText());
	}

	@Test
	public void testMultithreading() throws Exception {
		List<Thread> ts = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final BannerMultithread t = new BannerMultithread();
			t.start();
			ts.add(t);
		}
		log.debug("Joining");
		for (Thread t : ts)
			t.join();
		log.debug("Finished joining");
	}

	private void tagalot() throws UIMAException {
        // just tag a single sentence with a test model that actually used that sentence as training data.
        JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-meta-types",
                "de.julielab.jcore.types.jcore-semantics-biology-types");
        // this is sentence P00055040A0000 from the test BC2GM train data
        jcas.setDocumentText(
                "Maintenance of skeletal muscle mass is regulated by the balance between anabolic and catabolic processes. Mammalian target of rapamycin (mTOR) is an evolutionarily conserved serine/threonine kinase, and is known to play vital roles in protein synthesis. Recent findings have continued to refine our understanding of the function of mTOR in maintaining skeletal muscle mass. mTOR controls the anabolic and catabolic signaling of skeletal muscle mass, resulting in the modulation of muscle hypertrophy and muscle wastage. This review will highlight the fundamental role of mTOR in skeletal muscle growth by summarizing the phenotype of skeletal-specific mTOR deficiency. In addition, the evidence that mTOR is a dual regulator of anabolism and catabolism in skeletal muscle mass will be discussed. A full understanding of mTOR signaling in the maintenance of skeletal muscle mass could help to develop mTOR-targeted therapeutics to prevent muscle wasting.");
        new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();
        AnalysisEngine bannerAe = AnalysisEngineFactory.createEngine(BANNERAnnotator.class,
                BANNERAnnotator.PARAM_CONFIG_FILE, "src/test/resources/banner_ae_test.xml", BANNERAnnotator.PARAM_TYPE_MAPPING, new String[] {"GENE=de.julielab.jcore.types.Gene"});
        bannerAe.process(jcas);


    }

	private class BannerMultithread extends Thread {
		@Override
		public void run() {
			try {
				tagalot();
			} catch (Exception e) {
				throw new RuntimeException();

			}
		}
	}
}
