package de.julielab.jcore.ae.checkpoint;

import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.xmi.XmiDBMultiplier;
import de.julielab.jcore.reader.xmi.XmiDBMultiplierReader;
import de.julielab.jcore.reader.xmi.XmiDBReader;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import de.julielab.xmlData.dataBase.SubsetStatus;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;

import static org.testng.AssertJUnit.assertEquals;

public class DBCheckpointAETest {
    private final static Logger log = LoggerFactory.getLogger(DBCheckpointAETest.class);
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
        XmiDBSetupHelper.processAndSplitData(costosysConfig, false);
        Assert.assertTrue(dbc.withConnectionQueryBoolean(c -> c.tableExists("_data.documents")), "The data document table exists");
        dbc.close();

        subsetCounter = 0;
    }

    @AfterClass
    public static void shutdown() {
        postgres.close();
        log.info("Closed postgres testcontainer instance, test class done.");
    }

    @Test
    public void testCheckpoint() throws Exception {
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
                XmiDBReader.PARAM_RESET_TABLE, true
        );
        final AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(XmiDBMultiplier.class);
        final AnalysisEngine checkpointAE = AnalysisEngineFactory.createEngine(DBCheckpointAE.class, DBCheckpointAE.PARAM_INDICATE_FINISHED, true, DBCheckpointAE.PARAM_COSTOSYS_CONFIG, costosysConfig, DBCheckpointAE.PARAM_CHECKPOINT_NAME, "testCP");
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        Assert.assertTrue(xmiReader.hasNext());
        while (xmiReader.hasNext()) {
            xmiReader.getNext(jCas.getCas());
            final JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            while (jCasIterator.hasNext()) {
                final JCas multiplierCas = jCasIterator.next();
                // throws an exception if there is no such element
                checkpointAE.process(multiplierCas);
                jCasIterator.release();
            }
            jCas.reset();
        }
        xmiReader.close();
        multiplier.collectionProcessComplete();
        checkpointAE.collectionProcessComplete();

        dbc = DBTestUtils.getDataBaseConnector(postgres);
        final SubsetStatus status = dbc.status(xmisubset, EnumSet.allOf(DataBaseConnector.StatusElement.class));
        assertEquals(1L, (long) status.isProcessed);
        assertEquals(0L, (long) status.inProcess);
        dbc.close();
    }
}
