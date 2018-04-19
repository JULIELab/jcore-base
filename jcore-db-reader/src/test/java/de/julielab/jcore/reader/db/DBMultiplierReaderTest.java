package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.DocumentIds;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.*;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DBMultiplierReaderTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        TestDBSetupHelper.setupDatabase(postgres);
    }

    @Test
    public void testDBReader() throws UIMAException, IOException, ConfigurationException {

        String costosysConfig = TestDBSetupHelper.createTestCostosysConfig("medline_2017", postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                DBReader.PARAM_BATCH_SIZE, 5,
                DBReader.PARAM_TABLE, "testsubset",
                DBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        int docCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-stringid-multiplier-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            DocumentIds documentIds = JCasUtil.selectSingle(jCas, DocumentIds.class);
            assertNotNull(documentIds);
            assertNotNull(documentIds.getIdentifiers());
            assertEquals(5, documentIds.getIdentifiers().size());
            ++docCount;
            jCas.reset();
        }
        assertEquals(20, docCount);
    }

}
