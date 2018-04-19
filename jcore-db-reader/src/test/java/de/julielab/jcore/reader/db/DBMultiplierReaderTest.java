package de.julielab.jcore.reader.db;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileBased;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
       DBSetupUtil.setupDatabase(postgres);
    }

    @Test
    public void testDBReader() throws UIMAException, IOException, ConfigurationException {

        XMLConfiguration costosysconfig = new XMLConfiguration();
        costosysconfig.setProperty("databaseConnectorConfiguration.DBSchemaInformation.activeTableSchema", "medline_2017");
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.activeDBConnection", postgres.getDatabaseName());
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.DBConnections.DBConnection[@name]", postgres.getDatabaseName());
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.DBConnections.DBConnection[@url]", postgres.getJdbcUrl());
        FileHandler fh = new FileHandler((FileBased) costosysconfig);
        String costosysConfig = "src/test/resources/testconfig-.xml";
        fh.save(costosysConfig);
        CollectionReader reader = CollectionReaderFactory.createReader(DBReaderTest.DBReaderTestImpl.class,
                DBReader.PARAM_BATCH_SIZE, 5,
                DBReader.PARAM_TABLE, "testsubset",
                DBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        int docCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-stringid-multiplier-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            assertNotNull(JCoReTools.getDocId(jCas));
            ++docCount;
            jCas.reset();
        }
        assertEquals(20, docCount);
    }

}
