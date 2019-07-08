package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DBCIterator;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class XmiDBWriterBinaryFormatTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmlSubsetTable;
    private static DataBaseConnector dbc;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.reserveConnection();
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        xmlSubsetTable = DBTestUtils.setupDatabase(dbc, "src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 177, postgres);
        dbc.releaseConnections();
    }

    @AfterClass
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
    public void testXmiDBWriterSplitAnnotations() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()},
                XMIDBWriter.PARAM_USE_BINARY_FORNAT, true
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
            assertThat(dbc.tableExists("_data.documents")).isTrue();
            assertThat(dbc.tableExists("_data.de_julielab_jcore_types_token")).isTrue();
            assertThat(dbc.tableExists("_data.de_julielab_jcore_types_sentence")).isTrue();
            assertThat(dbc.isEmpty("_data.de_julielab_jcore_types_token")).isFalse();
            assertThat(dbc.isEmpty("_data.de_julielab_jcore_types_sentence")).isFalse();

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
        }
    }

    @Test
    public void testXmiDBWriterSplitAnnotationsSpecifyAnnotationSchemas() throws UIMAException, IOException {

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
                XMIDBWriter.PARAM_USE_BINARY_FORNAT, true
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
        try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
            assertThat(dbc.tableExists("_data.documents")).isTrue();
            assertThat(dbc.tableExists("tokenschema.de_julielab_jcore_types_token")).isTrue();
            assertThat(dbc.tableExists("sentenceschema.de_julielab_jcore_types_sentence")).isTrue();
            assertThat(dbc.isEmpty("tokenschema.de_julielab_jcore_types_token")).isFalse();
            assertThat(dbc.isEmpty("sentenceschema.de_julielab_jcore_types_sentence")).isFalse();
        }
    }
}
