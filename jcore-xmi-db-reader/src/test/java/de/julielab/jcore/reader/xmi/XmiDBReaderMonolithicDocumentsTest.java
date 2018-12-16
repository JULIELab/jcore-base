package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class XmiDBReaderMonolithicDocumentsTest {
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmisubset;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        postgres.start();
        XmiDBSetupHelper.createDbcConfig(postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("xmi_complete_cas", 1, postgres);
        XmiDBSetupHelper.processAndStoreCompleteXMIData(costosysConfig, true);
        dbc.reserveConnection();
        assertTrue(dbc.tableExists("_data.documents"), "The data document table exists");
        xmisubset = "xmisubset";
        dbc.setActiveTableSchema("xmi_complete_cas");
        dbc.createSubsetTable(xmisubset, "_data.documents", "Test XMI subset");
        dbc.initSubset(xmisubset, "_data.documents");
        dbc.close();
    }

    @AfterClass
    public static void shutdown() {
        postgres.close();
    }

    @Test
    public void testXmiDBReader() throws UIMAException, IOException {
        System.out.println("HIER: " + Thread.currentThread().getId());
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, false,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[0],
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_RESET_TABLE, true
        );
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        List<String> tokenText = new ArrayList<>();
        List<String> sentenceText = new ArrayList<>();
        assertTrue(xmiReader.hasNext());
        while (xmiReader.hasNext()) {
            xmiReader.getNext(jCas.getCas());
            // throws an exception if there is no such element
            JCasUtil.selectSingle(jCas, Header.class);
            JCasUtil.select(jCas, Token.class).stream().map(Annotation::getCoveredText).forEach(tokenText::add);
            JCasUtil.select(jCas, Sentence.class).stream().map(Annotation::getCoveredText).forEach(sentenceText::add);
            jCas.reset();
        }
        assertFalse(tokenText.isEmpty());
        assertFalse(sentenceText.isEmpty());
    }
}
