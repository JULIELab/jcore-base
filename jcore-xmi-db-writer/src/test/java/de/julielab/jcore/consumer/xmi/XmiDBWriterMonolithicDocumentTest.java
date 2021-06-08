package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmiDBWriterMonolithicDocumentTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static DataBaseConnector dbc;

    @BeforeAll
    public static void setup() throws ConfigurationException {
        dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
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
    public void testXmiDBWriterSplitAnnotations() throws Exception {

        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, true,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, false,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, false
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
            assertThat(dbc.isEmpty("_data.documents")).isFalse();

            final ResultSet rs = costoConn.createStatement().executeQuery("SELECT xmi FROM _data.documents");
            assertTrue(rs.next());
            final byte[] xmiData = rs.getBytes(1);
            jCas.reset();

            XmiCasDeserializer.deserialize(new ByteArrayInputStream(xmiData), jCas.getCas());

            assertThat(JCasUtil.select(jCas, Header.class)).isNotEmpty();
            assertThat(JCasUtil.select(jCas, Token.class)).isNotEmpty();
            assertThat(JCasUtil.select(jCas, Sentence.class)).isNotEmpty();
        }
    }

}
