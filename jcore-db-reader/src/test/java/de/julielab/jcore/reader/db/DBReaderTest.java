package de.julielab.jcore.reader.db;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileBased;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;


public class DBReaderTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        DataBaseConnector dbc = new DataBaseConnector(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        dbc.setActiveTableSchema("medline_2017");
        dbc.createTable(Constants.DEFAULT_DATA_TABLE_NAME, "Test data table for DBReaderTest.");
        dbc.importFromXMLFile("src/test/resources/pubmedsample18n0001.xml.gz", Constants.DEFAULT_DATA_TABLE_NAME);
        dbc.createSubsetTable("testsubset", Constants.DEFAULT_DATA_TABLE_NAME, "Test subset");
//        String hiddenConfigPath = "src/test/resources/hiddenConfig.txt";
//        try (BufferedWriter w = new BufferedWriter(new FileWriter(hiddenConfigPath))) {
//            w.write(postgres.getDatabaseName());
//            w.newLine();
//            w.write(postgres.getUsername());
//            w.newLine();
//            w.write(postgres.getPassword());
//            w.newLine();
//        }
//        System.setProperty(Constants.HIDDEN_CONFIG_PATH, hiddenConfigPath);
    }

    @Test
    public void testDBReader() throws ResourceInitializationException, IOException, CollectionException, ConfigurationException {

        XMLConfiguration costosysconfig = new XMLConfiguration();
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.activeDBConnection", "testconn");
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.DBConnections.DBConnection[@name]", "testconn");
        costosysconfig.setProperty("databaseConnectorConfiguration.DBConnectionInformation.DBConnections.DBConnection[@url]", postgres.getJdbcUrl());
        FileHandler fh = new FileHandler((FileBased) costosysconfig);
        String costosysConfig = "src/test/resources/testconfig.xml";
        fh.save(costosysConfig);
        CollectionReader reader = CollectionReaderFactory.createReader(DBReaderTestImpl.class, DBReader.PARAM_BATCH_SIZE, 2, DBReader.PARAM_TABLE, "testsubset", DBReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
    }

    public static class DBReaderTestImpl extends DBReader {

        @Override
        protected String getReaderComponentName() {
            return "Test DB Reader Implementation";
        }

        @Override
        public void getNext(JCas jCas) throws IOException, CollectionException {
            byte[][] artifactData = getNextArtifactData();

            XMLMapper xmlMapper = new XMLMapper(new FileInputStream(new File("src/test/resources/medline2016MappingFile.xml")));
            xmlMapper.parse(artifactData[0], artifactData[1], jCas);
        }
    }
}
