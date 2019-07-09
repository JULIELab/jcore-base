package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class XmiDBMultiplierDifferentNsSchemaTest {
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static int subsetCounter;

    @BeforeClass
    public static void setup() throws UIMAException, IOException, ConfigurationException {
        postgres.start();
        XmiDBSetupHelper.createDbcConfig(postgres);
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("xmi_text", 10, postgres);
        new File(costosysConfig).deleteOnExit();
        XmiDBSetupHelper.processAndSplitData(costosysConfig, false, false,"fancyschema");
        assertTrue(dbc.withConnectionQueryBoolean(c -> c.tableExists("_data.documents")), "The data document table exists");
        dbc.close();

        subsetCounter = 0;
    }

    @AfterClass
    public static void shutdown() {
        postgres.close();
    }

    @Test(threadPoolSize = 3, invocationCount = 10, timeOut = 500000)
    public void testXmiDBReader() throws Exception {
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        String xmisubset = "xmisubset" + subsetCounter++;
        dbc.setActiveTableSchema("xmi_text");
        dbc.reserveConnection();
        dbc.createSubsetTable(xmisubset, "_data.documents", "Test XMI subset");
        dbc.initSubset(xmisubset, "_data.documents");
        dbc.close();


        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBMultiplierReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_RESET_TABLE, true,
                XmiDBReader.PARAM_XMI_NAMESPACES_SCHEMA, "fancyschema"
        );
        final AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(XmiDBMultiplier.class);
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        List<String> tokenText = new ArrayList<>();
        List<String> sentenceText = new ArrayList<>();
        assertTrue(xmiReader.hasNext());
        while (xmiReader.hasNext()) {
            xmiReader.getNext(jCas.getCas());
            final JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            while (jCasIterator.hasNext()) {
                final JCas multiplierCas = jCasIterator.next();
                // throws an exception if there is no such element
                JCasUtil.selectSingle(multiplierCas, Header.class);
                JCasUtil.select(multiplierCas, Token.class).stream().map(Annotation::getCoveredText).forEach(tokenText::add);
                JCasUtil.select(multiplierCas, Sentence.class).stream().map(Annotation::getCoveredText).forEach(sentenceText::add);
                assertTrue(JCasUtil.selectSingle(multiplierCas, DBProcessingMetaData.class) != null);
                jCasIterator.release();
            }
            jCas.reset();
        }
        assertFalse(tokenText.isEmpty());
        assertFalse(sentenceText.isEmpty());
    }
}
