package de.julielab.jcore.reader.cord19;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Paragraph;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.AbstractText;
import de.julielab.jcore.types.pubmed.InternalReference;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-cord19-reader.
 *
 * @author faessler
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
        Set<String> ids = new HashSet<>();
        while (jCasIterator.hasNext()) {
            JCas nextCas = jCasIterator.next();
            ids.add(JCoReTools.getDocId(nextCas));
            if (JCoReTools.getDocId(nextCas).equals("99408604499bba576bd955c922a371c5d35bc969"))
                checkSecondDocument(nextCas);
            ++docCounter;
            nextCas.release();
        }
        assertThat(docCounter).isEqualTo(3);
        assertThat(ids).containsExactlyInAnyOrder("9692bb55e1e2eec083333ee2139137e6ddf3a4d8", "PMC6687912", "99408604499bba576bd955c922a371c5d35bc969");
    }

    private void checkSecondDocument(JCas cas) {
        // This should be 99408604499bba576bd955c922a371c5d35bc969.json
        assertThat(cas.getDocumentText().length()).isGreaterThan(0);
        List<Title> documentTitles = JCasUtil.select(cas, Title.class).stream().filter(t -> t.getTitleType().equals("document")).collect(Collectors.toList());
        assertThat(documentTitles).hasSize(1);
        assertThat(documentTitles.get(0)).extracting(Annotation::getCoveredText).isEqualTo("Recombinant M protein-based ELISA test for detection of antibodies to canine coronavirus");

        AbstractText abstractText = JCasUtil.selectSingle(cas, AbstractText.class);
        assertThat(abstractText.getCoveredText()).startsWith("Abstract The membrane (M) protein of canine");
        assertThat(abstractText.getCoveredText()).endsWith("antibodies to CCoV in dog sera.");

        Collection<Paragraph> paragraphs = JCasUtil.select(cas, Paragraph.class);
        assertThat(paragraphs).hasSize(19);

        Collection<Section> sections = JCasUtil.select(cas, Section.class);
        assertThat(sections).hasSize(8);
        Section firstSection = sections.iterator().next();
        assertThat(firstSection.getSectionHeading().getCoveredText()).isEqualTo("Introduction");
        assertThat(firstSection.getCoveredText()).startsWith("Canine coronavirus (CCoV)").endsWith("antibodies are described.");

        Collection<InternalReference> references = JCasUtil.select(cas, InternalReference.class);
        assertThat(references).hasSize(6 + 7 + 1 + 2 + 3);
    }
}
