package de.julielab.jcore.reader.xmi;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.SQLException;

public class XmiDBReaderTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException, ResourceInitializationException, IOException, InvalidXMLException {
        //XmiDBSetupHelper.setupDatabase(postgres);
    }
}
