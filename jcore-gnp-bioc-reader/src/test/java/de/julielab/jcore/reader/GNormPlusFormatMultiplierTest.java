package de.julielab.jcore.reader;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
class GNormPlusFormatMultiplierTest {
    private JCas getCas() throws Exception {
        return JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");
    }

    @Test
    void process() throws Exception {
        JCas cas = getCas();
        JCoReURI jCoReURI = new JCoReURI(cas);
        jCoReURI.setUri(Path.of("src", "test", "resources", "test-input-path", "subdir1", "bioc_collection_0.xml").toUri().toString());
        jCoReURI.addToIndexes();

        JCoReURI jCoReURI2 = new JCoReURI(cas);
        jCoReURI2.setUri(Path.of("src", "test", "resources", "test-input-path", "subdir2", "bioc_collection_2.xml").toUri().toString());
        jCoReURI2.addToIndexes();

        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(GNormPlusFormatMultiplier.class);
        JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(cas);
        List<String> docIds = new ArrayList<>();
        while (jCasIterator.hasNext()) {
            JCas multiplierCas = jCasIterator.next();
            docIds.add(JCoReTools.getDocId(multiplierCas));
            multiplierCas.release();
        }
        assertThat(docIds).containsExactlyInAnyOrder("1378843", "10896916", "10722742", "1770008");
    }
}