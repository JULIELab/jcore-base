package de.julielab.jcore.cr.mmax2;

import de.julielab.jcore.types.Protein;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
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
 * Unit tests for jcore-mmax2-reader.
 *
 * @author
 */
public class MMAX2ReaderTest {

    @Test
    public void testReader() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.cr.mmax2.desc.jcore-mmax2-reader",
                MMAX2Reader.PARAM_INPUT_DIR, Path.of("src", "test", "resources", "input").toString(),
                MMAX2Reader.PARAM_ANNOTATION_LEVELS, new String[]{"proteins", "sentence"},
                MMAX2Reader.PARAM_UIMA_ANNOTATION_TYPES, new String[]{"de.julielab.jcore.types.Protein", "de.julielab.jcore.types.Sentence"});
        assertThat(reader.hasNext()).isTrue();
        reader.getNext(jCas.getCas());
        // the text should be tokenized because we did not provide the original text
        assertThat(jCas.getDocumentText()).startsWith("Characterization of antihuman IFNAR-1 monoclonal antibodies : epitope localization and functional analysis .");
        Collection<Protein> proteins = JCasUtil.select(jCas, Protein.class);
        assertThat(proteins).hasSize(16);
        assertThat(proteins).map(Protein::getCoveredText).contains("IFNAR-1", "type I interferon receptor", "HuIFNAR-1", "Stat");
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
        assertThat(sentences).hasSize(10);
        Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
        // check a small sample of tokens that should have been created
       assertThat(tokens).map(Token::getCoveredText).contains("Characterization", "IFNAR-1", ":", "(", "subunits", "recognition", ".", "HuIFNAR-1");
    }

    @Test
    public void testReaderOriginalText() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.cr.mmax2.desc.jcore-mmax2-reader",
                MMAX2Reader.PARAM_INPUT_DIR, Path.of("src", "test", "resources", "input").toString(),
                MMAX2Reader.PARAM_ORIGINAL_TEXT_FILES, Path.of("src", "test", "resources", "originalText").toString(),
                MMAX2Reader.PARAM_ANNOTATION_LEVELS, new String[]{"proteins"},
                MMAX2Reader.PARAM_UIMA_ANNOTATION_TYPES, new String[]{"de.julielab.jcore.types.Protein"});
        assertThat(reader.hasNext()).isTrue();
        reader.getNext(jCas.getCas());
        // in this test, the text should not appear tokenized but arranged according to the original text
        assertThat(jCas.getDocumentText()).startsWith("Characterization of antihuman IFNAR-1 monoclonal antibodies: epitope localization and functional analysis.");
        Collection<Protein> proteins = JCasUtil.select(jCas, Protein.class);
        assertThat(proteins).hasSize(16);
        assertThat(proteins).map(Protein::getCoveredText).contains("IFNAR-1", "type I interferon receptor", "HuIFNAR-1", "Stat");
        Collection<Token> tokens = JCasUtil.select(jCas, Token.class);
        // check a small sample of tokens that should have been created
        assertThat(tokens).map(Token::getCoveredText).contains("Characterization", "IFNAR-1", ":", "(", "subunits", "recognition", ".", "HuIFNAR-1");
    }
}
