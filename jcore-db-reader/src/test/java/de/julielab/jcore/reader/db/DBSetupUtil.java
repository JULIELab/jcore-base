package de.julielab.jcore.reader.db;

import de.julielab.xmlData.Constants;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

public class DBSetupUtil {

    public static void setupDatabase(PostgreSQLContainer postgres) throws SQLException {
        DataBaseConnector dbc = new DataBaseConnector(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        dbc.setActiveTableSchema("medline_2017");
        dbc.createTable(Constants.DEFAULT_DATA_TABLE_NAME, "Test data table for DBReaderTest.");
        dbc.importFromXMLFile("src/test/resources/pubmedsample18n0001.xml.gz", Constants.DEFAULT_DATA_TABLE_NAME);
        dbc.createSubsetTable("testsubset", Constants.DEFAULT_DATA_TABLE_NAME, "Test subset");
        dbc.initRandomSubset(20, "testsubset", Constants.DEFAULT_DATA_TABLE_NAME);
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
