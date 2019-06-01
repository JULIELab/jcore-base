package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.consumer.xmi.XMIDBWriter;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.*;
import de.julielab.xmlData.dataBase.DataBaseConnector;
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

public class XmiDBReaderTest {
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
    public void testXmiDBReader() throws UIMAException, IOException {
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
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

    @Test
    public void testReadAnnotationFromSpecifiedSchema() throws Exception {
        // Store some data in separate schemas for the tokens and sentences
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{"tokenschema:"+Token.class.getCanonicalName(), "sentenceschema:"+Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
        );
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        de.julielab.jcore.types.pubmed.Header header = new de.julielab.jcore.types.pubmed.Header(jCas);
        header.setDocId("12345");
        header.addToIndexes();
        new Sentence(jCas, 0, 19).addToIndexes();
        new Sentence(jCas, 20, 40).addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        new Token(jCas, 0, 19).addToIndexes();
        new Token(jCas, 20, 40).addToIndexes();

        xmiWriter.process(jCas);
        jCas.reset();
        xmiWriter.collectionProcessComplete();

        // Now read the token and sentence from their respective dables
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{"tokenschema:"+Token.class.getCanonicalName(), "sentenceschema:"+Sentence.class.getCanonicalName()},
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_RESET_TABLE, true
        );
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
