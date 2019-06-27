package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.consumer.xmi.XMIDBWriter;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.db.TableReaderConstants;
import de.julielab.jcore.types.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XmiDBReaderDifferentNsSchemaTest {
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmisubset;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        postgres.start();
        XmiDBSetupHelper.createDbcConfig(postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("xmi_text", 2, postgres);
        XmiDBSetupHelper.processAndSplitData(costosysConfig, false, "someotherschema");
        assertTrue("The data document table exists", dbc.withConnectionQueryBoolean(c -> c.tableExists("_data.documents")));
        xmisubset = "xmisubset";
        dbc.setActiveTableSchema("xmi_text");
        dbc.reserveConnection();
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
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_RESET_TABLE, true,
                XmiDBReader.PARAM_XMI_NAMESPACES_SCHEMA, "someotherschema"
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
