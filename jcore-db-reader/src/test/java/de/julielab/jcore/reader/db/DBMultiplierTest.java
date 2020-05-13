package de.julielab.jcore.reader.db;

import de.julielab.costosys.Constants;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;
import static org.junit.Assert.*;

public class DBMultiplierTest {
    private final static Logger log = LoggerFactory.getLogger(DBMultiplierTest.class);
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.reserveConnection();
        DBTestUtils.setupDatabase("src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 20, postgres);
        dbc.close();
    }

    @Test
    public void testDBMultiplierReader() throws UIMAException, IOException, ConfigurationException {
// This test does not really need 2 connections, but the other test in the class needs it. Since the connection pools
// are cached in a static map, the pool created here will also be used in the other test even if it has its own configuration.
        String costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 2, postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, "testsubset",
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(TestMultiplier.class,
                TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");
        int numReadDocs = 0;
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            RowBatch rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            assertTrue(rowBatch.getIdentifiers().size() > 0);
            JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            while (jCasIterator.hasNext()) {
                JCas mJCas = jCasIterator.next();
                assertNotNull(mJCas.getDocumentText());
                assertTrue(JCoReTools.getDocId(mJCas) != null);
                assertTrue(JCoReTools.getDocId(mJCas).length() > 0);
                log.debug(StringUtils.abbreviate(mJCas.getDocumentText(), 200));
                mJCas.release();
                ++numReadDocs;
            }
            jCas.reset();
        }
        assertEquals(20, numReadDocs);
    }

    @Test
    public void testDBMultiplierFromDataTable() throws UIMAException, IOException, ConfigurationException {

        String costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 2, postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, Constants.DEFAULT_DATA_TABLE_NAME,
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(TestMultiplier.class,
                TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");
        int numReadDocs = 0;
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            RowBatch rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            log.trace("Got batch of {} document IDs from the multiplier reader", rowBatch.getIdentifiers().size());
            assertTrue(rowBatch.getIdentifiers().size() > 0);
            assertTrue(rowBatch.getIdentifiers().size() < 50);
            JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            log.trace("Got iterator from multiplier, now reading the multiplier documents");
            int numDoc = 0;
            while (jCasIterator.hasNext()) {
                log.trace("Reader multiplier document no. {}", ++numDoc);
                JCas mJCas = jCasIterator.next();
                assertNotNull(mJCas.getDocumentText());
                assertTrue(JCoReTools.getDocId(mJCas) != null);
                assertTrue(JCoReTools.getDocId(mJCas).length() > 0);
                //log.debug(StringUtils.abbreviate(mJCas.getDocumentText(), 200));
                mJCas.release();
                numReadDocs++;
            }
            jCas.reset();
            log.trace("Processed batch of multiplier documents, now getting the next batch from the reader.");
        }
        assertEquals(177, numReadDocs);
       multiplier.collectionProcessComplete();
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        assertEquals(0, dbc.getNumReservedConnections());
    }

    public static class TestMultiplier extends DBMultiplier {
        private final static Logger log = LoggerFactory.getLogger(TestMultiplier.class);

        @Override
        public AbstractCas next() throws AnalysisEngineProcessException {
            JCas jCas = getEmptyJCas();
            boolean hasNext = documentDataIterator.hasNext();
            assertTrue(hasNext);
            if (hasNext) {
                log.trace("Reading next document from database");
                byte[][] artifactData = documentDataIterator.next();
                try {
                    XMLMapper xmlMapper = new XMLMapper(new FileInputStream(new File("src/test/resources/medline2016MappingFile.xml")));
                    xmlMapper.parse(artifactData[1], artifactData[0], jCas);
                } catch (IOException e) {
                    throw new AnalysisEngineProcessException(e);
                }
            }
            return jCas;
        }

        @Override
        public void collectionProcessComplete() {
            documentDataIterator.close();
        }
    }
}
