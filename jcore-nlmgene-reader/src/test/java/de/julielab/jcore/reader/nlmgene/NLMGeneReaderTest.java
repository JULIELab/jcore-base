
package de.julielab.jcore.reader.nlmgene;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.ResourceEntry;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.AbstractText;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Unit tests for jcore-nlmgene-reader.
 * @author 
 *
 */
public class NLMGeneReaderTest{

    private final static Logger log = LoggerFactory.getLogger(NLMGeneReaderTest.class);

    @Test
    public void testReader() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
        final CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.nlmgene.desc.jcore-nlmgene-reader", NLMGeneReader.PARAM_INPUT_DIR, Path.of("src", "test", "resources", "input").toString());
        assertThat(reader.hasNext()).isTrue();
        reader.getNext(jCas.getCas());
        assertThat(reader.hasNext()).isFalse();
        final Header header = JCasUtil.selectSingle(jCas, Header.class);
        assertThat(header.getDocId()).isEqualTo("12461077");
        final Title title = JCasUtil.selectSingle(jCas, Title.class);
        assertThat(title).extracting(Title::getBegin, Title::getEnd).isEqualTo(List.of(0, 151));
        final AbstractText abstractText = JCasUtil.selectSingle(jCas, AbstractText.class);
        assertThat(abstractText).extracting(AbstractText::getBegin, AbstractText::getEnd).isEqualTo(List.of(152, 2168));
        final List<Gene> genes = new ArrayList<>(JCasUtil.select(jCas, Gene.class));
        assertThat(genes).hasSize(39);
        final Gene firstGene = genes.get(0);
        assertThat(firstGene).extracting(Gene::getCoveredText).isEqualTo("ICSBP");
        assertThat(firstGene.getResourceEntryList()).isNotNull().isNotEmpty();
        assertThat(firstGene.getResourceEntryList(0)).extracting(ResourceEntry::getEntryId).isEqualTo("15900");

        final Gene secondGene = genes.get(9);
        assertThat(secondGene).extracting(Gene::getCoveredText).isEqualTo("CD11c");
        assertThat(secondGene.getResourceEntryList(0)).extracting(ResourceEntry::getEntryId).isEqualTo("16411");
    }
}
