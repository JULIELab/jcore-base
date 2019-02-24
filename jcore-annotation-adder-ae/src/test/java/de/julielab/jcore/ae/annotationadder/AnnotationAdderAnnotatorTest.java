
package de.julielab.jcore.ae.annotationadder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileEntityProvider;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.Resource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
/**
 * Unit tests for jcore-annotation-adder-ae.
 *
 */
public class AnnotationAdderAnnotatorTest{
    @Test
    public void testCharacterOffsets() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-types");
        final ExternalResourceDescription externalResourceDescription = ExternalResourceFactory.createExternalResourceDescription(InMemoryFileEntityProvider.class, new File("src/test/resources/geneannotations_character_offsets.tsv"));
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(AnnotationAdderAnnotator.class, AnnotationAdderAnnotator.KEY_ANNOTATION_SOURCE, externalResourceDescription);
        // Test doc1 (two gene annotations)
        jCas.setDocumentText("BRCA PRKII are the genes of this sentence.");
        final Header h = new Header(jCas);
        h.setDocId("doc1");
        h.addToIndexes();

        engine.process(jCas);

        final List<Gene> genes = new ArrayList<>(JCasUtil.select(jCas, Gene.class));
        assertThat(genes).hasSize(2);

        assertThat(genes.get(0).getBegin()).isEqualTo(0);
        assertThat(genes.get(0).getEnd()).isEqualTo(4);

        assertThat(genes.get(1).getBegin()).isEqualTo(5);
        assertThat(genes.get(1).getEnd()).isEqualTo(10);

        // Test doc2 (no gene annotations)
        jCas.reset();
        jCas.setDocumentText("There are no gene mentions in here");
        Header h2 = new Header(jCas);
        h2.setDocId("doc2");
        h2.addToIndexes();
        engine.process(jCas);
        assertThat(JCasUtil.exists(jCas, Gene.class)).isFalse();

        // Test doc3 (one gene annotation)
        jCas.reset();
        jCas.setDocumentText("PRKAVI does not exist, I think. But this is just a test so it doesn't matter.");
        Header h3 = new Header(jCas);
        h3.setDocId("doc3");
        h3.addToIndexes();
        engine.process(jCas);
        final Gene gene = JCasUtil.selectSingle(jCas, Gene.class);
        assertThat(gene.getBegin()).isEqualTo(0);
        assertThat(gene.getEnd()).isEqualTo(6);
    }

    @Test
    public void testTokenOffsets() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-types");
        final ExternalResourceDescription externalResourceDescription = ExternalResourceFactory.createExternalResourceDescription(InMemoryFileEntityProvider.class, new File("src/test/resources/geneannotations_token_offsets.tsv"));
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(AnnotationAdderAnnotator.class, AnnotationAdderAnnotator.PARAM_OFFSET_MODE, AnnotationAdderAnnotator.OffsetMode.TOKEN, AnnotationAdderAnnotator.KEY_ANNOTATION_SOURCE, externalResourceDescription);
        // Test doc1 (two gene annotations)
        jCas.setDocumentText("BRCA PRKII are the genes of this sentence.");
        new Token(jCas, 0, 4).addToIndexes();
        new Token(jCas, 5, 10).addToIndexes();
        final Header h = new Header(jCas);
        h.setDocId("doc1");
        h.addToIndexes();

        engine.process(jCas);

        final List<Gene> genes = new ArrayList<>(JCasUtil.select(jCas, Gene.class));
        assertThat(genes).hasSize(2);

        assertThat(genes.get(0).getBegin()).isEqualTo(0);
        assertThat(genes.get(0).getEnd()).isEqualTo(4);

        assertThat(genes.get(1).getBegin()).isEqualTo(5);
        assertThat(genes.get(1).getEnd()).isEqualTo(10);

        // Test doc2 (no gene annotations)
        jCas.reset();
        jCas.setDocumentText("There are no gene mentions in here");
        Header h2 = new Header(jCas);
        h2.setDocId("doc2");
        h2.addToIndexes();
        engine.process(jCas);
        assertThat(JCasUtil.exists(jCas, Gene.class)).isFalse();

        // Test doc3 (one gene annotation)
        jCas.reset();
        jCas.setDocumentText("PRKAVI does not exist, I think. But this is just a test so it doesn't matter.");
        new Token(jCas, 0, 6).addToIndexes();
        Header h3 = new Header(jCas);
        h3.setDocId("doc3");
        h3.addToIndexes();
        engine.process(jCas);
        final Gene gene = JCasUtil.selectSingle(jCas, Gene.class);
        assertThat(gene.getBegin()).isEqualTo(0);
        assertThat(gene.getEnd()).isEqualTo(6);

        // Test doc4 (one gene annotation spanning multiple tokens)
        jCas.reset();
        jCas.setDocumentText("PRKAVI BRCA IL-2 come to mind.");
        new Token(jCas, 0, 6).addToIndexes();
        new Token(jCas, 7, 11).addToIndexes();
        new Token(jCas, 12, 16).addToIndexes();
        Header h4 = new Header(jCas);
        h4.setDocId("doc4");
        h4.addToIndexes();
        engine.process(jCas);
        final Gene gene2 = JCasUtil.selectSingle(jCas, Gene.class);
        assertThat(gene2.getBegin()).isEqualTo(0);
        assertThat(gene2.getEnd()).isEqualTo(16);
    }

    @Test
    public void testDescriptorCharacterOffsets() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-types");
        final AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescriptionFromPath("src/main/resources/de/julielab/jcore/ae/annotationadder/desc/jcore-annotation-adder-ae.xml");
        ExternalResourceFactory.bindResource(desc, AnnotationAdderAnnotator.KEY_ANNOTATION_SOURCE, InMemoryFileEntityProvider.class, new File("src/test/resources/geneannotations_character_offsets.tsv").toURI().toString());
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(desc);
        // Test doc1 (two gene annotations)
        jCas.setDocumentText("BRCA PRKII are the genes of this sentence.");
        final Header h = new Header(jCas);
        h.setDocId("doc1");
        h.addToIndexes();

        engine.process(jCas);

        final List<Gene> genes = new ArrayList<>(JCasUtil.select(jCas, Gene.class));
        assertThat(genes).hasSize(2);

        assertThat(genes.get(0).getBegin()).isEqualTo(0);
        assertThat(genes.get(0).getEnd()).isEqualTo(4);

        assertThat(genes.get(1).getBegin()).isEqualTo(5);
        assertThat(genes.get(1).getEnd()).isEqualTo(10);

        // Test doc2 (no gene annotations)
        jCas.reset();
        jCas.setDocumentText("There are no gene mentions in here");
        Header h2 = new Header(jCas);
        h2.setDocId("doc2");
        h2.addToIndexes();
        engine.process(jCas);
        assertThat(JCasUtil.exists(jCas, Gene.class)).isFalse();

        // Test doc3 (one gene annotation)
        jCas.reset();
        jCas.setDocumentText("PRKAVI does not exist, I think. But this is just a test so it doesn't matter.");
        Header h3 = new Header(jCas);
        h3.setDocId("doc3");
        h3.addToIndexes();
        engine.process(jCas);
        final Gene gene = JCasUtil.selectSingle(jCas, Gene.class);
        assertThat(gene.getBegin()).isEqualTo(0);
        assertThat(gene.getEnd()).isEqualTo(6);
    }
}
