package de.julielab.jcore.reader.ct;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Keyword;
import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.ct.*;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for jcore-ct-reader.
 *
 * @author
 */
public class ClinicalTrialsReaderTest {
    @Test
    public void testReader() throws UIMAException, IOException {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-clinicaltrial-types", "de.julielab.jcore.types.jcore-document-structure-clinicaltrial-types");
        final CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.ct.desc.jcore-clinicaltrials-reader", ClinicalTrialsReader.PARAM_INPUT_DIR, "src/test/resources/testdocs", ClinicalTrialsReader.PARAM_FILES, new String[]{"NCT02206334.xml"});
        assertTrue(reader.hasNext());
        reader.getNext(jCas.getCas());
        Header header = JCasUtil.selectSingle(jCas, Header.class);
        final String idForThisTest = "NCT02206334";
        boolean found = header.getDocId().equals(idForThisTest);
        while (!header.getDocId().equals(idForThisTest) && reader.hasNext()) {
            jCas.reset();
            reader.getNext(jCas.getCas());
            header = JCasUtil.selectSingle(jCas, Header.class);
            found = header.getDocId().equals(idForThisTest);
        }
        assertThat(found).withFailMessage("The document used for this test was not found: %s", idForThisTest).isTrue();


        assertEquals("NCT02206334", header.getDocId());
        assertThat(header.getGender()).containsExactlyInAnyOrder("female", "male");
        assertEquals("Interventional", header.getStudyType());
        assertEquals(18, header.getMinimumAge());
        assertEquals(100, header.getMaximumAge());
        assertNotNull(header.getStudyDesignInfo());
        final StudyDesignInfo sdi = header.getStudyDesignInfo();
        assertEquals("Single Group Assignment", sdi.getInterventionModel());
        assertEquals("Treatment", sdi.getPrimaryPurpose());

        final BriefTitle briefTitle = JCasUtil.selectSingle(jCas, BriefTitle.class);
        assertThat(briefTitle.getCoveredText()).isEqualTo("Stereotactic Body Radiation Therapy in Treating Patients With Metastatic Breast Cancer, Non-small Cell Lung Cancer, or Prostate Cancer");

        final OfficialTitle officialTitle = JCasUtil.selectSingle(jCas, OfficialTitle.class);
        assertThat(officialTitle.getCoveredText()).isEqualTo("A Phase 1 Study of Stereotactic Body Radiotherapy (SBRT) for the Treatment of Multiple Metastases");

        final Summary summary = JCasUtil.selectSingle(jCas, Summary.class);
        assertThat(summary.getCoveredText()).startsWith("This phase I trial studies the side effects and the best dose of stereotactic body radiation").endsWith("surrounding normal tissue.");

        final Description description = JCasUtil.selectSingle(jCas, Description.class);
        assertThat(description.getCoveredText()).startsWith("PRIMARY OBJECTIVES: I. To determine the recommended stereotactic").endsWith("months for 2 years.");
        assertThat(description.getCoveredText()).contains("rates of >= grade 3");

        final Collection<OutcomeMeasure> outcomeMeasures = JCasUtil.select(jCas, OutcomeMeasure.class);
        assertEquals(3, outcomeMeasures.size());

        assertThat(outcomeMeasures).extracting(Annotation::getCoveredText).containsExactly(
                "Dose-limiting toxicity (DLT) scored according to the National Cancer Institute (NCI) CTCAE version 4.0 for each of 7 metastatic locations when multiple metastases are treated with SBRT",
                "Rate of long-term adverse events, scored according to the NCI CTCAE v. 4.0",
                "Rates of >= grade 3 adverse events, scored according to NCI CTCAE v. 4.0");

        final Collection<OutcomeDescription> outcomeDescriptions = JCasUtil.select(jCas, OutcomeDescription.class);
        assertEquals(3, outcomeDescriptions.size());
        assertThat(outcomeDescriptions).extracting(Annotation::getCoveredText).containsExactly(
                "Adverse events outlined by metastatic location (full detail in protocol) reported as being probably or definitely related to protocol treatment.",
                "Adverse events reported as being possibly, probably, or definitely related to protocol treatment.",
                "Adverse events (other than DLTs) reported as being possibly, probably, or definitely related to protocol treatment."
        );

        final Collection<InterventionType> interventionTypes = JCasUtil.select(jCas, InterventionType.class);
        assertEquals(1, interventionTypes.size());
        assertThat(interventionTypes).extracting(Annotation::getCoveredText).containsExactly("Radiation");

        final Collection<InterventionName> interventionNames = JCasUtil.select(jCas, InterventionName.class);
        assertEquals(1, interventionNames.size());
        assertThat(interventionNames).extracting(Annotation::getCoveredText).containsExactly("Stereotactic Radiosurgery");

        final Collection<ArmGroupDescription> armGroupDescriptions = JCasUtil.select(jCas, ArmGroupDescription.class);
        assertEquals(1, armGroupDescriptions.size());
        assertThat(armGroupDescriptions).extracting(Annotation::getCoveredText).containsExactly("Patients undergo 3-5 fractions of image-guided stereotactic body radiation therapy to all existing metastases over 1-3 weeks with at least 40 hours between treatments for an individual metastasis.");

        final ManualDescriptor md = JCasUtil.selectSingle(jCas, ManualDescriptor.class);
        assertThat(md).isNotNull();
        assertThat(md.getMeSHList()).hasSize(7);
        assertThat(md.getMeSHList()).extracting(mh -> ((MeshHeading) mh).getDescriptorName()).containsExactly("Breast Neoplasms",
                "Carcinoma",
                "Lung Neoplasms",
                "Prostatic Neoplasms",
                "Carcinoma, Non-Small-Cell Lung",
                "Adenocarcinoma",
                "Breast Neoplasms, Male");

    }

    @Test
    public void testKeywordReading() throws UIMAException, IOException {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-clinicaltrial-types", "de.julielab.jcore.types.jcore-document-structure-clinicaltrial-types");
        final CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.ct.desc.jcore-clinicaltrials-reader", ClinicalTrialsReader.PARAM_INPUT_DIR, "src/test/resources/testdocs");
        assertTrue(reader.hasNext());
        reader.getNext(jCas.getCas());
        Header header = JCasUtil.selectSingle(jCas, Header.class);
        final String idForThisTest = "NCT01855776";
        boolean found = header.getDocId().equals(idForThisTest);
        while (!header.getDocId().equals(idForThisTest) && reader.hasNext()) {
            jCas.reset();
            reader.getNext(jCas.getCas());
            header = JCasUtil.selectSingle(jCas, Header.class);
            found = header.getDocId().equals(idForThisTest);
        }
        assertThat(found).withFailMessage("The document used for this test was not found: %s", idForThisTest).isTrue();



        final ManualDescriptor md = JCasUtil.selectSingle(jCas, ManualDescriptor.class);
        assertThat(md).isNotNull();
        assertThat(md.getKeywordList()).hasSize(5);
        assertThat(md.getKeywordList()).extracting(mh -> ((Keyword) mh).getName()).containsExactly("Physical Activity/Walking",
                "Full-time employees",
                "RCT",
                "Incentives",
                "Wireless pedometer");

    }
}
