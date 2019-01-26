package de.julielab.jcore.ae.checkpoint;

import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.xmi.XmiDBReader;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.xmlData.cli.TableNotFoundException;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import de.julielab.xmlData.dataBase.SubsetStatus;
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
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class DBCheckpointAETest {
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmisubset;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        postgres.start();
        XmiDBSetupHelper.createDbcConfig(postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("xmi_text", 1, postgres);
        XmiDBSetupHelper.processAndSplitData(costosysConfig, false);
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
    public void testXmiDBReader() throws UIMAException, IOException, TableNotFoundException {
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_RESET_TABLE, true
        );
        final AnalysisEngine checkpointae = AnalysisEngineFactory.createEngine(DBCheckpointAE.class, DBCheckpointAE.PARAM_CHECKPOINT_NAME, "testCP", DBCheckpointAE.PARAM_COSTOSYS_CONFIG, costosysConfig, DBCheckpointAE.PARAM_INDICATE_FINISHED, true);
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        assertTrue(xmiReader.hasNext());
        while (xmiReader.hasNext()) {
            xmiReader.getNext(jCas.getCas());
            checkpointae.process(jCas);
            jCas.reset();
        }
        checkpointae.collectionProcessComplete();

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        final SubsetStatus status = dbc.status(xmisubset, EnumSet.allOf(DataBaseConnector.StatusElement.class));
        System.out.println(status);
        assertEquals(1L, (long)status.isProcessed);
    }
}
