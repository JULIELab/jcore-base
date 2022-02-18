
package de.julielab.jcore.reader;


import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-bnp-bioc-reader.
 * @author 
 *
 */
public class GNormPlusFormatMultiplierReaderTest{

    private JCas getCas() throws Exception {
        return JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");
    }
    @Test
    public void testReader() throws Exception {
        CollectionReader reader = CollectionReaderFactory.createReader(GNormPlusFormatMultiplierReader.class, GNormPlusFormatMultiplierReader.PARAM_INPUT_PATH, Path.of("src", "test", "resources", "test-input-path").toString());
        assertThat(reader.hasNext()).isTrue();
        JCas jCas = getCas();
        reader.getNext(jCas.getCas());
        Collection<JCoReURI> uris = JCasUtil.select(jCas, JCoReURI.class);
        assertThat(uris).extracting(JCoReURI::getUri).map(Path::of).map(Path::getFileName).map(Path::toString).containsExactlyInAnyOrder("bioc_collection_2.xml", "bioc_collection_3.xml", "bioc_collection_0.xml", "bioc_collection_1.xml");
        assertThat(reader.hasNext()).isFalse();
    }

    @Test
    public void testReader2() throws Exception {
        // check that the non-recursive mode also works
        CollectionReader reader = CollectionReaderFactory.createReader(GNormPlusFormatMultiplierReader.class, GNormPlusFormatMultiplierReader.PARAM_INPUT_PATH, Path.of("src", "test", "resources", "test-input-path").toString(), GNormPlusFormatMultiplierReader.PARAM_RECURSIVE, false);
        assertThat(reader.hasNext());
        JCas jCas = getCas();
        reader.getNext(jCas.getCas());
        Collection<JCoReURI> uris = JCasUtil.select(jCas, JCoReURI.class);
        assertThat(uris).extracting(JCoReURI::getUri).map(Path::of).map(Path::getFileName).map(Path::toString).containsExactlyInAnyOrder("bioc_collection_3.xml");
        assertThat(reader.hasNext()).isFalse();
    }

    @Test
    public void testReader3() throws Exception {
        // check that the batch size parameter works as intended
        CollectionReader reader = CollectionReaderFactory.createReader(GNormPlusFormatMultiplierReader.class, GNormPlusFormatMultiplierReader.PARAM_INPUT_PATH, Path.of("src", "test", "resources", "test-input-path").toString(), GNormPlusFormatMultiplierReader.PARAM_BATCH_SIZE, 2);
        assertThat(reader.hasNext()).isTrue();
        JCas jCas = getCas();
        reader.getNext(jCas.getCas());
        Collection<JCoReURI> uris = JCasUtil.select(jCas, JCoReURI.class);
        assertThat(uris).hasSize(2);
        assertThat(reader.hasNext()).isTrue();
        jCas.reset();
        // there should another batch available
        reader.getNext(jCas.getCas());
        Collection<JCoReURI> uris2 = JCasUtil.select(jCas, JCoReURI.class);
        assertThat(uris2).hasSize(2);
        // now the reader should be exhausted
        assertThat(reader.hasNext()).isFalse();
    }
}
