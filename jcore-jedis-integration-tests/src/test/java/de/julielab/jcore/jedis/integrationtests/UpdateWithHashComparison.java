package de.julielab.jcore.jedis.integrationtests;

import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.costosys.dbconnection.SubsetStatus;
import de.julielab.jcore.ae.checkpoint.DBCheckpointAE;
import de.julielab.jcore.consumer.xmi.XMIDBWriter;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.flow.annotationdefined.AnnotationDefinedFlowController;
import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.reader.xml.XMLDBMultiplier;
import de.julielab.jcore.types.Annotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.*;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class UpdateWithHashComparison {
    private static final String SOURCE_XML_TABLE = "_data.source_xml_table";
    private static final String TARGET_XMI_TABLE = "_data_xmi.target_xmi_table";
    private static final String XML_SUBSET_TABLE = "test_subset";
    private static final String XMI_MIRROR_TABLE = "test_xmi_mirror";
    @Container
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:11.12");
    private static String costosysConfigSourceTable;
    private static String costosysConfigTargetTable;
    /**
     * The collection reader that feeds the XMLDBMultiplier the database rows to read.
     */
    private static CollectionReader testCr;
    /**
     * The top-level aggregate containing the XMLDBMultiplier and two "child" aggregates, one for the analysis engines
     * and one for the CAS consumers. In this test, the aggregate delegates are all realized by instances of {@link TestAnnotator}.
     */
    private static AnalysisEngine testAggregate;
    private static JCas cas;
    private static DataBaseConnector dbc;
    private static List<String> namesOfRunComponents = new ArrayList<>();

    @BeforeAll
    public static void setup() throws Exception {
        DBTestUtils.createAndSetHiddenConfig(Path.of("src", "test", "resources", "hiddenConfig").toString(), postgres);

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.setActiveTableSchema("medline_2017");
        costosysConfigSourceTable = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        costosysConfigTargetTable = DBTestUtils.createTestCostosysConfig("xmi_text", 1, postgres);
        new File(costosysConfigSourceTable).deleteOnExit();
        new File(costosysConfigTargetTable).deleteOnExit();
        prepareSourceXMLTable(dbc);
        dbc.defineMirrorSubset(XML_SUBSET_TABLE, SOURCE_XML_TABLE, true, "Test subset");
        assertThat(dbc.getNumRows(SOURCE_XML_TABLE)).isEqualTo(3);
        createTestPipelineComponents();
    }

    @AfterAll
    public static void shutdown() {
        dbc.close();
    }

    private static void prepareSourceXMLTable(DataBaseConnector dbc) throws Exception {
        dbc.createTable(SOURCE_XML_TABLE, "Test XML Table");
        dbc.importFromXMLFile(Path.of("src", "test", "resources", "pubmed21n1016_excerpt_original.xml.gz").toString(), SOURCE_XML_TABLE);
    }

    /**
     * <p>Creates test components in a structure that mimics the structure used by the <tt>jcore-pipeline-builder</tt>.</p>
     * <p>This consists of:
     * <ol>
     *     <li>a <tt>CollectionReader</tt></li>
     *     <li>an AAE containing all other components:
     *     <ol>
     *         <li>an optional <tt>CAS multiplier</tt></li>
     *         <li>an aggregate containing all AEs</li>
     *         <li>an aggregate containing all CAS consumers</li>
     *     </ol>
     *     </li>
     *     The CAS consumers in this test consist of two "mock" CCs, a "real" XMI Writer and DB Checkpoint AE.
     * </ol>
     * We here want to test if we can successfully route the CAS through those inner AAEs when the multiplier adds
     * the correct {@link de.julielab.jcore.types.casflow.ToVisit} annotation using a {@link de.julielab.jcore.flow.annotationdefined.AnnotationDefinedFlowController}.
     * </p>
     */
    private static void createTestPipelineComponents() throws Exception {
        TypeSystemDescription tsDesc = TypeSystemDescriptionFactory.createTypeSystemDescription("de.julielab.jcore.types.jcore-document-meta-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types", "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types", "de.julielab.jcore.types.jcore-casflow-types", "de.julielab.jcore.types.jcore-xmi-splitter-types");

        testCr = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                tsDesc,
                DBMultiplierReader.PARAM_TABLE, XML_SUBSET_TABLE,
                DBMultiplierReader.PARAM_RESET_TABLE, false,
                DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfigSourceTable,
                // We set a batch size of 1 to have more refined testing.
                // Otherwise, the multiplier would receive all 3 test documents at once and
                // would process them all in one batch
                DBMultiplierReader.PARAM_BATCH_SIZE, 1
        );

        AnalysisEngineDescription testAe1 = AnalysisEngineFactory.createEngineDescription(TestAnnotator.class, tsDesc, "name", "TestAE 1");
        AnalysisEngineDescription testAe2 = AnalysisEngineFactory.createEngineDescription(TestAnnotator.class, tsDesc, "name", "TestAE 2");
        AnalysisEngineDescription testCc1 = AnalysisEngineFactory.createEngineDescription(TestAnnotator.class, tsDesc, "name", "TestCC 1");
        AnalysisEngineDescription testCc2 = AnalysisEngineFactory.createEngineDescription(TestAnnotator.class, tsDesc, "name", "TestCC 2");
        AnalysisEngineDescription xmiDbWriter = AnalysisEngineFactory.createEngineDescription(XMIDBWriter.class,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, TARGET_XMI_TABLE,
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{"de.julielab.jcore.types.Annotation"},
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, false,
                XMIDBWriter.PARAM_ADD_SHA_HASH, "document_text",
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfigTargetTable,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_DO_GZIP, false
        );
        AnalysisEngineDescription dbCheckpointAe = AnalysisEngineFactory.createEngineDescription(DBCheckpointAE.class,
                DBCheckpointAE.PARAM_CHECKPOINT_NAME, "end",
                DBCheckpointAE.PARAM_COSTOSYS_CONFIG, costosysConfigSourceTable,
                DBCheckpointAE.PARAM_INDICATE_FINISHED, true
        );

        FlowControllerDescription flowControllerDescription = FlowControllerFactory.createFlowControllerDescription(AnnotationDefinedFlowController.class);
        AnalysisEngineDescription aeAaeDesc = AnalysisEngineFactory.createEngineDescription(List.of(testAe1, testAe2), List.of("TestAE 1", "TestAE 2"), null, null, flowControllerDescription);
        AnalysisEngineDescription ccAaeDesc = AnalysisEngineFactory.createEngineDescription(List.of(testCc1, testCc2, xmiDbWriter, dbCheckpointAe), List.of("TestCC 1", "TestCC 2", "XMI Writer", "Checkpoint Writer"), null, null, flowControllerDescription);

        AnalysisEngineDescription multiplierDescription = AnalysisEngineFactory.createEngineDescription(XMLDBMultiplier.class,
                tsDesc,
                XMLDBMultiplier.PARAM_MAPPING_FILE, Path.of("src", "test", "resources", "medlineMappingFile.xml").toString(),
                // The core of this whole test: The components to be visited in case of matching hash codes.
                // We want to skip all components except the checkpoint writer that marks the document as
                // "processed" in the XML subset table
                XMLDBMultiplier.PARAM_TO_VISIT_KEYS, new String[]{"Checkpoint Writer"},
                // The next three parameters are required for the hash comparison
                XMLDBMultiplier.PARAM_ADD_SHA_HASH, "document_text",
                XMLDBMultiplier.PARAM_TABLE_DOCUMENT, TARGET_XMI_TABLE,
                XMLDBMultiplier.PARAM_TABLE_DOCUMENT_SCHEMA, "xmi_text");

        testAggregate = AnalysisEngineFactory.createEngine(List.of(multiplierDescription, aeAaeDesc, ccAaeDesc), List.of("multiplier", "AeAAE", "CcAAE"), null, null);

        cas = JCasFactory.createJCas(tsDesc);
    }

    @Test
    public void testInitialProcessingProcessing() throws Exception {
        assertThat(testCr.hasNext());
        while (testCr.hasNext()) {
            testCr.getNext(cas.getCas());
            testAggregate.process(cas);
            // Check that all components have been visited as expected from a normal, fixed flow
            assertThat(namesOfRunComponents).containsExactly("TestAE 1", "TestAE 2", "TestCC 1", "TestCC 2");
            namesOfRunComponents.clear();
            cas.reset();
        }
        testAggregate.collectionProcessComplete();
        assertThat(dbc.tableExists(TARGET_XMI_TABLE));
        // After this first processing, the XMI document table exists. We can now create a mirror on it. This is important
        // because we want to see that the mirror is only reset for rows that have actually changed in subsequent tests.
        dbc.defineMirrorSubset(XMI_MIRROR_TABLE, TARGET_XMI_TABLE, true, "The XMI test mirror table.", "xmi_text");
        // We mark the XMI mirror subset as completely processed. This simulates a state where the initial batch of
        // documents has been completely processed, before the update comes in.
        dbc.markAsProcessed(XMI_MIRROR_TABLE);
        SubsetStatus status = dbc.status(XML_SUBSET_TABLE, EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED, DataBaseConnector.StatusElement.IN_PROCESS));
        // Check that all rows have been processed in the XML source subset table.
        assertThat(status.isProcessed).isEqualTo(3);
        assertThat(status.inProcess).isEqualTo(0);
    }

    /**
     * Adds its name to {@link #namesOfRunComponents}.
     */
    public static class TestAnnotator extends JCasAnnotator_ImplBase {
        @ConfigurationParameter(name = "name")
        private String name;

        @Override
        public void initialize(UimaContext aContext) throws ResourceInitializationException {
            super.initialize(aContext);
            this.name = (String) aContext.getConfigParameterValue("name");
        }

        @Override
        public void process(JCas jCas) {
            namesOfRunComponents.add(name);
            new Annotation(jCas).addToIndexes();
        }
    }

    @Nested
    class AfterInitialProcessing {
        @Test
        public void updateXML() throws Exception {
            dbc.updateFromXML(Path.of("src", "test", "resources", "pubmed21n1016_excerpt_partially_changed.xml.gz").toString(), SOURCE_XML_TABLE, true);
            // The update contains all three originally imported XML documents. Only that the second has not been changed.
            // But the XML mirror should have been reset completely.
            SubsetStatus status = dbc.status(XML_SUBSET_TABLE, EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED, DataBaseConnector.StatusElement.IN_PROCESS));
            // Check that the XML mirror subset has been reset due to the update
            assertThat(status.isProcessed).isEqualTo(0);
            assertThat(status.inProcess).isEqualTo(0);
        }

        @Nested
        class AfterUpdatingXML {
            @Test
            public void testOnlyNewDocumentsProcessed() throws Exception {

                testCr.reconfigure();
                testAggregate.reconfigure();
                assertThat(testCr.hasNext()).withFailMessage("The XML DB Collection reader does not report any non-processed rows.").isTrue();
                // Run the whole pipeline again. Only this time we only expect all the components run in a single case.
                List<String> allNamesOfRunComponents = new ArrayList<>();
                while (testCr.hasNext()) {
                    cas.reset();
                    testCr.getNext(cas.getCas());
                    testAggregate.process(cas);
                    // Check that all components have been visited as expected from a normal, fixed flow
                    allNamesOfRunComponents.addAll(namesOfRunComponents);
                    namesOfRunComponents.clear();
                    cas.reset();
                }
                testAggregate.collectionProcessComplete();
                // There should be only two components documents now that have visited all components
                assertThat(allNamesOfRunComponents).containsExactly("TestAE 1", "TestAE 2", "TestCC 1", "TestCC 2", "TestAE 1", "TestAE 2", "TestCC 1", "TestCC 2");
                testAggregate.collectionProcessComplete();
                // Check again that all the XML documents have been processed.
                SubsetStatus status = dbc.status(XML_SUBSET_TABLE, EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED));
                // Check that all rows have been processed in the XML source subset table.
                assertThat(status.isProcessed).isEqualTo(3);

                // Now the more interesting part: In the XMI mirror there should now be two unprocessed tables, namely
                // the two documents with a changed document text. The unchanged document should still be marked as
                // processed.
                SubsetStatus xmiMirrorStatus = dbc.status(XMI_MIRROR_TABLE, EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED));
                // Check that all rows have been processed in the XML source subset table.
                assertThat(xmiMirrorStatus.isProcessed).isEqualTo(1);
            }
        }
    }
}
