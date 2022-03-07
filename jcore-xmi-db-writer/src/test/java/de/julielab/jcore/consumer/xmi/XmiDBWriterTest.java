package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.xml.XmiSplitConstants;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
public class XmiDBWriterTest {
    @Container
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:" + DataBaseConnector.POSTGRES_VERSION);
    private static String costosysConfig;
    private static DataBaseConnector dbc;

    @BeforeAll
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.reserveConnection();
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        dbc.releaseConnections();
        DBTestUtils.createAndSetHiddenConfig("src/test/resources/hiddenConfig.txt", postgres);
    }

    @AfterAll
    public static void shutDown() {
        dbc.close();
    }

    public static JCas getJCasWithRequiredTypes() throws UIMAException {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
                "de.julielab.jcore.types.jcore-xmi-splitter-types");
    }

    @Test
    public void testXmiDBWriterSplitAnnotationsSpecifyAnnotationSchemas() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{"tokenschema:" + Token.class.getCanonicalName(), "sentenceschema:" + Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
        );
        JCas jCas = getJCasWithRequiredTypes();
        final Header header = new Header(jCas);
        header.setDocId("789");
        header.addToIndexes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        new Sentence(jCas, 0, 19).addToIndexes();
        new Sentence(jCas, 20, 40).addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        new Token(jCas, 0, 19).addToIndexes();
        new Token(jCas, 20, 40).addToIndexes();
        assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
        jCas.reset();
        xmiWriter.collectionProcessComplete();

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
            assertThat(dbc.tableExists("_data.documents")).isTrue();

            final List<Map<String, Object>> infos = dbc.getTableColumnInformation("_data.documents", "column_name");
            final Set<String> columnNames = infos.stream().map(info -> info.get("column_name")).map(String.class::cast).collect(Collectors.toSet());

            final String tokenColumn = "tokenschema$de_julielab_jcore_types_token";
            final String sentenceColumn = "sentenceschema$de_julielab_jcore_types_sentence";
            assertThat(columnNames).contains(tokenColumn, sentenceColumn);

            assertThat(dbc.isEmpty("_data.documents", tokenColumn)).isFalse();
            assertThat(dbc.isEmpty("_data.documents", sentenceColumn)).isFalse();
        }
    }

    @Test
    public void testXmiDBWriterSplitAnnotationsDefaultAnnotationSchemas() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_ANNO_DEFAULT_QUALIFIER, "testschema",
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
        );
        JCas jCas = getJCasWithRequiredTypes();
        final Header header = new Header(jCas);
        header.setDocId("789");
        header.addToIndexes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        new Sentence(jCas, 0, 19).addToIndexes();
        new Sentence(jCas, 20, 40).addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        new Token(jCas, 0, 19).addToIndexes();
        new Token(jCas, 20, 40).addToIndexes();
        assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
        jCas.reset();
        xmiWriter.collectionProcessComplete();

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
            assertThat(dbc.tableExists("_data.documents")).isTrue();

            final List<Map<String, Object>> infos = dbc.getTableColumnInformation("_data.documents", "column_name");
            final Set<String> columnNames = infos.stream().map(info -> info.get("column_name")).map(String.class::cast).collect(Collectors.toSet());

            final String tokenColumn = "testschema$de_julielab_jcore_types_token";
            final String sentenceColumn = "testschema$de_julielab_jcore_types_sentence";
            assertThat(columnNames).contains(tokenColumn, sentenceColumn);
        }
    }

    @Test
    public void testXmiSubtypeStorage() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents3",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{InternalReference.class.getCanonicalName()}
        );
        JCas jCas = getJCasWithRequiredTypes();
        final Header header = new Header(jCas);
        header.setDocId("789");
        header.addToIndexes();
        jCas.setDocumentText("This is a sentence.1,2");
        new de.julielab.jcore.types.pubmed.InternalReference(jCas, 19, 20).addToIndexes();
        new de.julielab.jcore.types.pubmed.InternalReference(jCas, 21, 22).addToIndexes();
        assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
        jCas.reset();
        xmiWriter.collectionProcessComplete();

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
            assertThat(dbc.tableExists("_data.documents3")).isTrue();
            ResultSet rs = ignored.createStatement().executeQuery("SELECT " + XmiSplitConstants.BASE_DOC_COLUMN + " FROM " + "_data.documents3");
            assertThat(rs.next()).isTrue();
//            String documentString = rs.getString(1);
//            System.out.println(documentString);

        }
    }

    @Nested
    class WriteWithMirrorSubsets {
        /**
         * This test checks that the XMI is split as intended and distributed into database table columns as annotation modules.
         * @throws Exception
         */
        @Test
        public void testXmiDBWriterSplitAnnotations() throws Exception {

            AnalysisEngine xmiWriter = getXmiWriterForDocuments2();
            JCas jCas = getJCasWithRequiredTypes();
            prepareDocument1(jCas);
            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            jCas.reset();
            prepareDocument2(jCas);
            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            xmiWriter.collectionProcessComplete();

            dbc = DBTestUtils.getDataBaseConnector(postgres);
            dbc.setActiveTableSchema("medline_2017");
            try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                assertThat(dbc.tableExists("_data.documents2")).isTrue();

                assertThat(dbc.getTableColumnNames("_data.documents2")).contains("de_julielab_jcore_types_token", "de_julielab_jcore_types_sentence");
                assertThat(dbc.isEmpty("_data.documents2", XmiSplitConstants.BASE_DOC_COLUMN)).isFalse();
                assertThat(dbc.isEmpty("_data.documents2", XmiDataInserter.FIELD_MAX_XMI_ID)).isFalse();
                assertThat(dbc.isEmpty("_data.documents2", "sofa_mapping")).isFalse();
                assertThat(dbc.isEmpty("_data.documents2", "de_julielab_jcore_types_token")).isFalse();
                assertThat(dbc.isEmpty("_data.documents2", "de_julielab_jcore_types_sentence")).isFalse();

            }

            // create a subset for nested tests and set its only entry to "processed"
            try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                dbc.createSubsetTable("_data._data_mirror", "_data.documents2", 1, "Test subset", "medline_2017");
                dbc.initMirrorSubset("_data._data_mirror", "_data.documents2", true, "medline_2017");
                List<byte[][]> idsList = new ArrayList<>();
                idsList.add(new byte[][]{"789".getBytes(StandardCharsets.UTF_8)});
                idsList.add(new byte[][]{"890".getBytes(StandardCharsets.UTF_8)});
                dbc.setProcessed("_data._data_mirror", idsList);
                assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED)).isProcessed).isEqualTo(2);
            }
        }

        /**
         * Produces the test XMI writer for this nested test group. It stores the base document which should cause
         * mirror subsets to reset the "is processed" status to false for the written documents.
         * @return The XMI writer for testing.
         * @throws InvalidXMLException
         * @throws IOException
         * @throws ResourceInitializationException
         */
        private AnalysisEngine getXmiWriterForDocuments2() throws InvalidXMLException, IOException, ResourceInitializationException {
            return AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                    XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                    XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                    XMIDBWriter.PARAM_STORE_ALL, false,
                    XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                    XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents2",
                    XMIDBWriter.PARAM_DO_GZIP, false,
                    XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                    XMIDBWriter.PARAM_UPDATE_MODE, true,
                    XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
            );
        }

        /**
         * Prepares the first of two documents used in these nested tests.
         * @param jCas The CAS to populate with the test data.
         */
        private void prepareDocument1(JCas jCas) {
            final Header header = new Header(jCas);
            header.setDocId("789");
            header.addToIndexes();
            jCas.setDocumentText("This is a sentence. This is another one.");
            new Sentence(jCas, 0, 19).addToIndexes();
            new Sentence(jCas, 20, 40).addToIndexes();
            // Of course, these token offsets are wrong, but it doesn't matter to the test
            new Token(jCas, 0, 19).addToIndexes();
            new Token(jCas, 20, 40).addToIndexes();
        }

        /**
         * Prepares the second of two documents used in these nested tests.
         * @param jCas The CAS to populate with the test data.
         */
        private void prepareDocument2(JCas jCas) {
            final Header header2 = new Header(jCas);
            header2.setDocId("890");
            header2.addToIndexes();
            jCas.setDocumentText("Sentence of document 2.");
            new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        }

        /**
         * Default case: mirror subsets should be reset after writing the base document
         */
        @Nested
        class CheckMirrorSubsetIsReset {
            @Test
            public void testMirrorSubsetReset() throws Exception {
                AnalysisEngine xmiWriter = getXmiWriterForDocuments2();
                JCas jCas = getJCasWithRequiredTypes();
                prepareDocument1(jCas);
                assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
                jCas.reset();
                prepareDocument2(jCas);
                assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
                xmiWriter.collectionProcessComplete();

                dbc = DBTestUtils.getDataBaseConnector(postgres);
                dbc.setActiveTableSchema("medline_2017");

                // check that the subset table has been reset
                try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED)).isProcessed).isEqualTo(0);
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.TOTAL)).total).isEqualTo(2);
                    // set it again to processed for the next test
                    List<byte[][]> idsList = new ArrayList<>();
                    idsList.add(new byte[][]{"789".getBytes(StandardCharsets.UTF_8)});
                    idsList.add(new byte[][]{"890".getBytes(StandardCharsets.UTF_8)});
                    dbc.setProcessed("_data._data_mirror", idsList);
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED)).isProcessed).isEqualTo(2);
                }
            }
        }

        /**
         * The interesting test case: Given a DBProcessingMetaData annotation that specifies that the document
         * text hasn't changed between a former document version in the database and the newly written version,
         * the mirror subsets should not be reset to "is not processed" for the given document.
         */
        @Nested
        class CheckMirrorSubsetIsNotReset {
            @Test
            public void testMirrorSubsetNotReset() throws Exception {
                // precondition check: the mirror subset is currently processed
                // this main test will be to ensure that the mirror subset stays this way
                try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED)).isProcessed).isEqualTo(2);
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.TOTAL)).total).isEqualTo(2);
                }
                AnalysisEngine xmiWriter = getXmiWriterForDocuments2();
                JCas jCas = getJCasWithRequiredTypes();
                prepareDocument1(jCas);
                // This is the important part: tell the writer not to reset mirror subsets for this document
                DBProcessingMetaData processingMetaData = new DBProcessingMetaData(jCas);
                processingMetaData.setIsDocumentHashUnchanged(true);
                StringArray pk = new StringArray(jCas, 1);
                pk.set(0, "789");
                processingMetaData.setPrimaryKey(pk);
                processingMetaData.addToIndexes();
                assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
                jCas.reset();
                prepareDocument2(jCas);
                assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
                xmiWriter.collectionProcessComplete();

                dbc = DBTestUtils.getDataBaseConnector(postgres);
                dbc.setActiveTableSchema("medline_2017");

                // check that the subset table has NOT been reset for document 789 but for the other
                try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.IS_PROCESSED)).isProcessed).isEqualTo(1);
                    assertThat(dbc.status("_data._data_mirror", EnumSet.of(DataBaseConnector.StatusElement.TOTAL)).total).isEqualTo(2);
                }
            }
        }
    }

}
