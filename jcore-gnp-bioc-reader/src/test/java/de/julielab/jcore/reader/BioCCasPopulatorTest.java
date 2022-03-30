package de.julielab.jcore.reader;

import de.julielab.jcore.types.*;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class BioCCasPopulatorTest {

    private JCas getJCas() throws Exception {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-types");
    }

    @Test
    public void populateWithNextDocument() throws Exception {
        BioCCasPopulator bioCCasPopulator = new BioCCasPopulator(Path.of("src", "test", "resources", "test-input-path", "bioc_collection_3.xml"), null, null);
        assertThat(bioCCasPopulator.documentsLeftInCollection()).isEqualTo(2);
        JCas jCas = getJCas();
        bioCCasPopulator.populateWithNextDocument(jCas);

        assertThat(jCas.getDocumentText()).startsWith("Langerin").endsWith("antigen-processing pathway.");
        Title title = JCasUtil.selectSingle(jCas, Title.class);
        assertThat(title).extracting(Title::getTitleType).isEqualTo("document");
        assertThat(title).extracting(Title::getCoveredText).isEqualTo("Langerin, a novel C-type lectin specific to Langerhans cells, is an endocytic receptor that induces the formation of Birbeck granules.");
        AbstractText abstractText = JCasUtil.selectSingle(jCas, AbstractText.class);
        assertThat(abstractText).extracting(AbstractText::getCoveredText).is(new Condition<>(s -> s.startsWith("We have identified"), "Abstract has an unexpected beginning"));
        // this document does not have organisms, we check those for the second document in the collection below
        Collection<Gene> genes = JCasUtil.select(jCas, Gene.class);
        assertThat(genes).hasSize(7);
        for (Gene o : genes) {
            assertThat(o.getResourceEntryList()).isNotNull();
            assertThat(o.getResourceEntryList()).hasSize(1);
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getComponentId).isEqualTo(GNormPlusFormatMultiplierReader.class.getCanonicalName());
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getSource).isEqualTo("NCBI Gene");
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getEntryId).isNotNull();
        }
        assertThat(genes).extracting(Gene::getCoveredText).contains("Langerin");

        assertThat(bioCCasPopulator.documentsLeftInCollection()).isEqualTo(1);
        jCas.reset();
        bioCCasPopulator.populateWithNextDocument(jCas);
        assertThat(jCas.getDocumentText()).startsWith("BCAR1, a human homologue");

        Collection<Organism> organisms = JCasUtil.select(jCas, Organism.class);
        assertThat(organisms).isNotEmpty();
        for (Organism o : organisms) {
            assertThat(o.getResourceEntryList()).isNotNull();
            assertThat(o.getResourceEntryList()).hasSize(1);
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getComponentId).isEqualTo(GNormPlusFormatMultiplierReader.class.getCanonicalName());
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getSource).isEqualTo("NCBI Taxonomy");
            assertThat(o.getResourceEntryList(0)).extracting(ResourceEntry::getEntryId).isNotNull();
        }
        assertThat(organisms).extracting(Organism::getCoveredText).contains("human", "patients", "rat", "retrovirus", "ZR-75-1");
    }

    @Test
    public void addFamilyNames() throws Exception {
        BioCCasPopulator bioCCasPopulator = new BioCCasPopulator(Path.of("src", "test", "resources","bioc_collection_0_0.xml"), null, null);
        JCas jCas = getJCas();
        bioCCasPopulator.populateWithNextDocument(jCas);

        Collection<Gene> genes = JCasUtil.select(jCas, Gene.class);
        assertThat(genes).hasSize(23);
        assertThat(genes).filteredOn(Gene::getSpecificType, "FamilyName").hasSize(5);
        for (Gene o : genes) {
            if (o.getSpecificType().equals("FamilyName")) {
                assertThat(o.getSpecies(0)).isEqualTo("9606");
            }
        }
    }
}