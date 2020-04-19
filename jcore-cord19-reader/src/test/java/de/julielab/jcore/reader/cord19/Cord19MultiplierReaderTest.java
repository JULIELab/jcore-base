
package de.julielab.jcore.reader.cord19;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Unit tests for jcore-cord19-reader.
 * @author faessler
 *
 */
public class Cord19MultiplierReaderTest {
    @Test
    public void testReader() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.cord19.desc.jcore-cord19-multiplier-reader", Cord19MultiplierReader.PARAM_INPUT_DIR,
                Path.of("src", "test", "resources", "documents").toFile(), Cord19MultiplierReader.PARAM_SEARCH_RECURSIVELY, true);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine("de.julielab.jcore.reader.cord19.desc.jcore-cord19-multiplier");
        assertThat(reader.hasNext()).isTrue();
        reader.getNext(jCas.getCas());
        JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
        int docCounter = 0;
        while (jCasIterator.hasNext()) {
            JCas nextCas = jCasIterator.next();

            ++docCounter;
            nextCas.release();
        }
        assertThat(docCounter).isEqualTo(2);
    }
}
