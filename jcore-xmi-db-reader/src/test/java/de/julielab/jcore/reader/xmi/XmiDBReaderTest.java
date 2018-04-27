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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XmiDBReaderTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmisubset;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        String subsetTable = DBTestUtils.setupDatabase(dbc, "src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 177, postgres);
        XmiDBSetupHelper.processAndSplitData(costosysConfig, subsetTable, postgres);
        assertTrue("The data document table exists", dbc.tableExists("_data.documents"));
        xmisubset = "xmisubset";
        dbc.createSubsetTable(xmisubset, "_data.documents", "Test XMI subset");
        dbc.initSubset(xmisubset, "_data.documents");
        dbc.close();
    }

    @Test
    public void testXmiDBReader() throws UIMAException, IOException {
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XmiDBReader.PARAM_TABLE, xmisubset,
                XmiDBReader.PARAM_DO_GZIP, false,
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
