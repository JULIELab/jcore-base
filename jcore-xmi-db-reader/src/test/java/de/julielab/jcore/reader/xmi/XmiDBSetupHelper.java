package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.reader.xml.XMLDBMultiplier;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

public class XmiDBSetupHelper {
    /**
     * Imports the file "src/test/resources/pubmedsample18n0001.xml.gz" into the empty database, and creates a subset
     * named "testsubset" of size 20.
     * @param postgres
     * @throws SQLException
     */
    public static void setupDatabase(PostgreSQLContainer postgres) throws SQLException, UIMAException, IOException {
        writeHiddenConfig(postgres);
        DataBaseConnector dbc = new DataBaseConnector(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        dbc.setActiveTableSchema("medline_2017");
        dbc.createTable(Constants.DEFAULT_DATA_TABLE_NAME, "Test data table for DBReaderTest.");
        dbc.importFromXMLFile("src/test/resources/pubmedsample18n0001.xml.gz", Constants.DEFAULT_DATA_TABLE_NAME);
        dbc.createSubsetTable("testsubset", Constants.DEFAULT_DATA_TABLE_NAME, "Test subset");
        dbc.initRandomSubset(20, "testsubset", Constants.DEFAULT_DATA_TABLE_NAME);

        // TODO not finished!
        CollectionReader pubmedXmlReader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.xml.desc.jcore-pubmed-multiplier-reader.xml", DBMultiplierReader.PARAM_TABLE, "testsubset", DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
        AnalysisEngine jtbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jtbd.desc.jcore-jtbd-ae-biomedical-english");

        dbc.close();
    }

    private static void writeHiddenConfig(PostgreSQLContainer postgres) {
        String hiddenConfigPath = "src/test/resources/hiddenConfig.txt";
        try (BufferedWriter w = new BufferedWriter(new FileWriter(hiddenConfigPath))) {
            w.write(postgres.getDatabaseName());
            w.newLine();
            w.write(postgres.getUsername());
            w.newLine();
            w.write(postgres.getPassword());
            w.newLine();
            w.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.setProperty(Constants.HIDDEN_CONFIG_PATH, hiddenConfigPath);
    }
}
