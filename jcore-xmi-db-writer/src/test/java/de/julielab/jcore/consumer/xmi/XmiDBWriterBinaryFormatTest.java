package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.*;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.binary.BinaryDecodingResult;
import de.julielab.xml.binary.BinaryJeDISNodeDecoder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class XmiDBWriterBinaryFormatTest {
    @Container
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmlSubsetTable;
    private static DataBaseConnector dbc;

    @BeforeAll
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.reserveConnection();
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        xmlSubsetTable = DBTestUtils.setupDatabase(dbc, "src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 177, postgres);
        dbc.releaseConnections();
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

    @BeforeEach
    public void cleanForTest() throws SQLException {
        String binaryMappingTable = "public." + MetaTableManager.BINARY_MAPPING_TABLE;
        String binaryFeaturesToMapTable = "public." + MetaTableManager.BINARY_FEATURES_TO_MAP_TABLE;
        try (CoStoSysConnection ignore = dbc.obtainOrReserveConnection()) {
            if (dbc.tableExists(binaryMappingTable))
                dbc.dropTable(binaryMappingTable);
            if (dbc.tableExists(binaryFeaturesToMapTable))
                dbc.dropTable(binaryFeaturesToMapTable);
        }
    }

    @Test
    public void testXmiDBWriterSplitAnnotations() throws Exception {
        {
            final AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                    XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                    XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                    XMIDBWriter.PARAM_STORE_ALL, false,
                    XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                    XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents2",
                    XMIDBWriter.PARAM_DO_GZIP, false,
                    XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                    XMIDBWriter.PARAM_UPDATE_MODE, true,
                    XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()},
                    XMIDBWriter.PARAM_USE_BINARY_FORMAT, true
            );
            JCas jCas = getJCasWithRequiredTypes();
            final Header header = new Header(jCas);
            header.setDocId("789");
            header.addToIndexes();
            jCas.setDocumentText("This is a sentence. This is another one.");
            final Sentence s1 = new Sentence(jCas, 0, 19);
            s1.setComponentId("SentenceTagger");
            s1.addToIndexes();
            final Sentence s2 = new Sentence(jCas, 20, 40);
            s2.setComponentId("SentenceTagger");
            s2.addToIndexes();
            // Of course, these token offsets are wrong, but it doesn't matter to the test
            final Token t1 = new Token(jCas, 0, 19);
            t1.setComponentId("TokenSplitter");
            t1.addToIndexes();
            final Token t2 = new Token(jCas, 20, 40);
            t2.setComponentId("TokenSplitter");
            t2.addToIndexes();
            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            xmiWriter.collectionProcessComplete();

            dbc = DBTestUtils.getDataBaseConnector(postgres);
            String binaryMappingTable = "public." + MetaTableManager.BINARY_MAPPING_TABLE;
            String binaryFeaturesToMapTable = "public." + MetaTableManager.BINARY_FEATURES_TO_MAP_TABLE;
            Set<String> itemsInInitialMapping = new HashSet<>();
            Map<String, Boolean> initialFeaturesToMap = new HashMap<>();
            try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
                assertThat(dbc.tableExists("_data.documents2")).isTrue();
                final Set<String> columnNames = dbc.getTableColumnNames("_data.documents2").collect(Collectors.toSet());
                assertThat(columnNames).contains(XmiSplitConstants.BASE_DOC_COLUMN, "de_julielab_jcore_types_token", "de_julielab_jcore_types_sentence");

                assertThat(dbc.isEmpty("_data.documents2", "de_julielab_jcore_types_token")).isFalse();
                assertThat(dbc.isEmpty("_data.documents2", "de_julielab_jcore_types_sentence")).isFalse();

                assertThat(dbc.tableExists(binaryMappingTable)).isTrue();
                assertThat(dbc.tableExists(binaryFeaturesToMapTable)).isTrue();

                assertThat(dbc.isEmpty(binaryMappingTable)).isFalse();
                assertThat(dbc.isEmpty(binaryFeaturesToMapTable)).isFalse();

                final Statement stmt = costoConn.createStatement();
                final ResultSet rs = stmt.executeQuery(String.format("SELECT %s FROM %s", MetaTableManager.BINARY_MAPPING_COL_STRING, binaryMappingTable));
                while (rs.next()) {
                    itemsInInitialMapping.add(rs.getString(1));
                }
                // There is more that we check here. The details are left to the tests of the XmiSplitter.
                assertThat(itemsInInitialMapping).contains("types:Token", "types:Sentence", "TokenSplitter", "SentenceTagger", "sofa", "mimeType", "cas:Sofa", "begin", "end");

                final ResultSet rs2 = stmt.executeQuery(String.format("SELECT %s,%s FROM %s", MetaTableManager.BINARY_FEATURES_TO_MAP_COL_FEATURE, MetaTableManager.BINARY_FEATURES_TO_MAP_COL_MAP, binaryFeaturesToMapTable));
                while (rs2.next()) {
                    initialFeaturesToMap.put(rs2.getString(1), rs2.getBoolean(2));
                }
                assertThat(initialFeaturesToMap.get(Annotation.class.getCanonicalName() + ":componentId")).isTrue();
                assertThat(initialFeaturesToMap.get(CAS.TYPE_NAME_SOFA + ":mimeType")).isFalse();
                assertThat(initialFeaturesToMap.get(CAS.FEATURE_FULL_NAME_SOFAID)).isFalse();
                assertThat(initialFeaturesToMap.get(CAS.FEATURE_FULL_NAME_SOFASTRING)).isFalse();
            }

            // Now we check if the update of the meta tables for new features and values to map is working as intended.
            jCas.reset();
            final Header h2 = new Header(jCas);
            h2.setDocId("8910");
            h2.addToIndexes();
            final Token t3 = new Token(jCas);
            t3.setComponentId("AnotherTokenSplitter");
            t3.setOrthogr("lowercase");
            t3.addToIndexes();

            xmiWriter.process(jCas);
            xmiWriter.collectionProcessComplete();

            try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
                Set<String> itemsInCurrentMapping = new HashSet<>();
                final Statement stmt = costoConn.createStatement();
                final ResultSet rs = stmt.executeQuery(String.format("SELECT %s FROM %s", MetaTableManager.BINARY_MAPPING_COL_STRING, binaryMappingTable));
                while (rs.next()) {
                    itemsInCurrentMapping.add(rs.getString(1));
                }
                // There is more that we check here. The details are left to the tests of the XmiSplitter.
                assertThat(itemsInCurrentMapping).containsAll(itemsInInitialMapping).contains("AnotherTokenSplitter", "orthogr");


                Map<String, Boolean> currentFeaturestoMap = new HashMap<>();
                final ResultSet rs2 = stmt.executeQuery(String.format("SELECT %s,%s FROM %s", MetaTableManager.BINARY_FEATURES_TO_MAP_COL_FEATURE, MetaTableManager.BINARY_FEATURES_TO_MAP_COL_MAP, binaryFeaturesToMapTable));
                while (rs2.next()) {
                    currentFeaturestoMap.put(rs2.getString(1), rs2.getBoolean(2));
                }
                assertThat(currentFeaturestoMap).containsAllEntriesOf(initialFeaturesToMap).containsEntry("de.julielab.jcore.types.Token:orthogr", false);

                final ResultSet rs3 = stmt.executeQuery("SELECT pmid FROM _data.documents2 WHERE " + XmiSplitConstants.BASE_DOC_COLUMN + " IS NULL");
                assertThat(rs3.next()).isFalse();
            }
        }
        {
            // Now we "update" the annotations. We don't write the base document again because it is already there.
            AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                    XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                    XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                    XMIDBWriter.PARAM_STORE_ALL, false,
                    XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, false,
                    XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents2",
                    XMIDBWriter.PARAM_DO_GZIP, false,
                    XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                    XMIDBWriter.PARAM_UPDATE_MODE, true,
                    XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()},
                    XMIDBWriter.PARAM_USE_BINARY_FORMAT, true
            );

            JCas jCas = getJCasWithRequiredTypes();
            final Header header = new Header(jCas);
            header.setDocId("789");
            header.addToIndexes();
            jCas.setDocumentText("This is a sentence. This is another one.");
            final Sentence s1 = new Sentence(jCas, 0, 19);
            s1.setComponentId("SentenceTagger");
            s1.addToIndexes();
            final Sentence s2 = new Sentence(jCas, 20, 40);
            s2.setComponentId("SentenceTagger");
            s2.addToIndexes();
            // Of course, these token offsets are wrong, but it doesn't matter to the test
            final Token t1 = new Token(jCas, 0, 19);
            t1.setComponentId("TokenSplitter");
            t1.addToIndexes();
            final Token t2 = new Token(jCas, 20, 40);
            t2.setComponentId("TokenSplitter");
            t2.addToIndexes();
            final XmiMetaData xmiMetaData = new XmiMetaData(jCas);
            xmiMetaData.setMaxXmiId(100);
            final StringArray sofaMappings = new StringArray(jCas, 1);
            sofaMappings.set(0, "1=_InitialView");
            xmiMetaData.setSofaIdMappings(sofaMappings);
            xmiMetaData.addToIndexes();

            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            xmiWriter.collectionProcessComplete();

            dbc = DBTestUtils.getDataBaseConnector(postgres);
            try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
                assertThat(dbc.tableExists("_data.documents2")).isTrue();
                final Set<String> columnNames = dbc.getTableColumnNames("_data.documents2").collect(Collectors.toSet());
                assertThat(columnNames).contains(XmiSplitConstants.BASE_DOC_COLUMN, "de_julielab_jcore_types_token", "de_julielab_jcore_types_sentence");
                assertThat(dbc.isEmpty("_data.documents2", XmiSplitConstants.BASE_DOC_COLUMN)).isFalse();

                final ResultSet rs3 = costoConn.createStatement().executeQuery("SELECT pmid FROM _data.documents2 WHERE " + XmiSplitConstants.BASE_DOC_COLUMN + " IS NULL");
                assertThat(rs3.next()).isFalse();
            }
        }
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
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()},
                XMIDBWriter.PARAM_USE_BINARY_FORMAT, true
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

            final ResultSet rs = costoConn.createStatement().executeQuery("SELECT " + XmiSplitConstants.BASE_DOC_COLUMN + "," + tokenColumn + "," + sentenceColumn + " FROM _data.documents");
            assertTrue(rs.next());
            // Check that the data for the base document, the tokens and the sentences are there
            assertNotNull(rs.getString(1));
            assertNotNull(rs.getString(2));
            assertNotNull(rs.getString(3));

            Map<String, InputStream> encodedXmiData = new HashMap<>();
            encodedXmiData.put(XmiSplitConstants.BASE_DOC_COLUMN, new ByteArrayInputStream(rs.getBytes(1)));
            encodedXmiData.put(tokenColumn, new ByteArrayInputStream(rs.getBytes(2)));
            encodedXmiData.put(sentenceColumn, new ByteArrayInputStream(rs.getBytes(3)));

            // Read all the meta table data required to decode the binary XMI data
            Map<Integer, String> mapping = new HashMap<>();
            final ResultSet rs2 = costoConn.createStatement().executeQuery(String.format("SELECT * FROM %s", "public." + XmiSplitConstants.BINARY_MAPPING_TABLE));
            while (rs2.next())
                mapping.put(rs2.getInt(2), rs2.getString(1));
            final ResultSet rs3 = costoConn.createStatement().executeQuery(String.format("SELECT * FROM %s", "public." + XmiSplitConstants.BINARY_FEATURES_TO_MAP_TABLE));
            Map<String, Boolean> mappedFeatures = new HashMap();
            while (rs3.next())
                mappedFeatures.put(rs3.getString(1), rs3.getBoolean(2));
            Map<String, String> nsMap = new HashMap<>();
            final ResultSet rs4 = costoConn.createStatement().executeQuery(String.format("SELECT * FROM %s", "public." + XmiSplitConstants.XMI_NS_TABLE));
            while (rs4.next())
                nsMap.put(rs4.getString(1), rs4.getString(2));

            final BinaryJeDISNodeDecoder decoder = new BinaryJeDISNodeDecoder(Collections.emptySet(), false);
            final BinaryDecodingResult decodingResult = decoder.decode(encodedXmiData, jCas.getTypeSystem(), mapping, mappedFeatures, nsMap);

            String finalXmi = decodingResult.getXmiData().toString(UTF_8);
            assertThat(finalXmi).contains("This is a sentence. This is another one.", "types:Token", "types:Sentence");
        }
    }

    @Test
    public void testBinaryFeatureMapBlacklist() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents2",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()},
                XMIDBWriter.PARAM_USE_BINARY_FORMAT, true,
                XMIDBWriter.PARAM_BINARY_FEATURES_BLACKLIST, new String[]{de.julielab.jcore.types.Annotation.class.getCanonicalName() + ":componentId"}
        );
        JCas jCas = getJCasWithRequiredTypes();
        final Header header = new Header(jCas);
        header.setDocId("789");
        header.addToIndexes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        final Sentence s1 = new Sentence(jCas, 0, 19);
        s1.setComponentId("SentenceTagger");
        s1.addToIndexes();
        final Sentence s2 = new Sentence(jCas, 20, 40);
        s2.setComponentId("SentenceTagger");
        s2.addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        final Token t1 = new Token(jCas, 0, 19);
        t1.setComponentId("TokenSplitter");
        t1.addToIndexes();
        final Token t2 = new Token(jCas, 20, 40);
        t2.setComponentId("TokenSplitter");
        t2.addToIndexes();
        assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
        xmiWriter.collectionProcessComplete();

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        String binaryFeaturesToMapTable = "public." + MetaTableManager.BINARY_FEATURES_TO_MAP_TABLE;
        Map<String, Boolean> initialFeaturesToMap = new HashMap<>();
        try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
            final Statement stmt = costoConn.createStatement();
            final ResultSet rs2 = stmt.executeQuery(String.format("SELECT %s,%s FROM %s", MetaTableManager.BINARY_FEATURES_TO_MAP_COL_FEATURE, MetaTableManager.BINARY_FEATURES_TO_MAP_COL_MAP, binaryFeaturesToMapTable));
            while (rs2.next()) {
                initialFeaturesToMap.put(rs2.getString(1), rs2.getBoolean(2));
            }
            assertThat(initialFeaturesToMap.get(de.julielab.jcore.types.Annotation.class.getCanonicalName() + ":componentId")).isFalse();
        }
    }
}
