package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.multiplier.pmc.PMCMultiplier;
import de.julielab.jcore.types.Header;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PMCMultiplierTest {

    @Test
    public void testMultiplier() throws UIMAException, IOException {
        CollectionReader reader = CollectionReaderFactory.createReader(PMCMultiplierReader.class,
                PMCMultiplierReader.PARAM_INPUT, "src/test/resources/documents-zip",
                PMCMultiplierReader.PARAM_RECURSIVELY, true,
                PMCMultiplierReader.PARAM_SEARCH_ZIP, true,
                PMCMultiplierReader.PARAM_BATCH_SIZE, 2);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(PMCMultiplier.class);
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");
        assertThat(reader.hasNext());
        Set<String> receivedDocIds = new HashSet<>();
        int numBatches = 0;
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            assertThat(jCasIterator.hasNext());
            while (jCasIterator.hasNext()) {
                JCas next = jCasIterator.next();
                String docId = JCasUtil.selectSingle(next, Header.class).getDocId();
                receivedDocIds.add(docId);
                next.release();
            }
            ++numBatches;
        }
        assertThat(receivedDocIds).containsExactlyInAnyOrder("2847692", "2758189",
                "2970367", "3201365", "4257438");
        assertThat(numBatches).isEqualTo(3);
    }

    @Test
    public void testMultiplierFromDescriptors() throws UIMAException, IOException {
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.pmc.desc.jcore-pmc-multiplier-reader",
                PMCMultiplierReader.PARAM_INPUT, "src/test/resources/documents-zip",
                PMCMultiplierReader.PARAM_RECURSIVELY, true,
                PMCMultiplierReader.PARAM_SEARCH_ZIP, true,
                PMCMultiplierReader.PARAM_BATCH_SIZE, 2);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine("de.julielab.jcore.multiplier.pmc.desc.jcore-pmc-multiplier");
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");
        assertThat(reader.hasNext());
        Set<String> receivedDocIds = new HashSet<>();
        int numBatches = 0;
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            assertThat(jCasIterator.hasNext());
            while (jCasIterator.hasNext()) {
                JCas next = jCasIterator.next();
                String docId = JCasUtil.selectSingle(next, Header.class).getDocId();
                receivedDocIds.add(docId);
                next.release();
            }
            ++numBatches;
        }
        assertThat(receivedDocIds).containsExactlyInAnyOrder("2847692", "2758189",
                "2970367", "3201365", "4257438");
        assertThat(numBatches).isEqualTo(3);
    }
}
