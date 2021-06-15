package de.julielab.jcore.reader.db;

import de.julielab.costosys.Constants;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class DBReaderTest {
    @Container
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:11.12");

    @BeforeAll
    public static void setup() throws SQLException {
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.reserveConnection();
        DBTestUtils.setupDatabase("src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 20, postgres);
        dbc.close();
    }

    @Test
    public void testDBReader() throws UIMAException, IOException, ConfigurationException {
        String costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBReaderTestImpl.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, "testsubset",
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        int docCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            assertNotNull(JCoReTools.getDocId(jCas));
            ++docCount;
            jCas.reset();
        }
        assertEquals(20, docCount);
    }

    @Test
    public void testReadDataTable() throws ConfigurationException, UIMAException, IOException {
        String costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        // Here, we do not specify the subset table but the data table directly
        CollectionReader reader = CollectionReaderFactory.createReader(DBReaderTestImpl.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, Constants.DEFAULT_DATA_TABLE_NAME,
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        int docCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            assertNotNull(JCoReTools.getDocId(jCas));
            ++docCount;
            jCas.reset();
        }
        assertEquals(177, docCount);
    }

    public static class DBReaderTestImpl extends DBReader {
        private final static Logger log = LoggerFactory.getLogger(DBReaderTestImpl.class);

        @Override
        protected String getReaderComponentName() {
            return "Test DB Reader Implementation";
        }

        @Override
        public void getNext(JCas jCas) throws IOException, CollectionException {
            byte[][] artifactData = getNextArtifactData();

            log.trace("Getting next document from database");
            XMLMapper xmlMapper = new XMLMapper(new FileInputStream(new File("src/test/resources/medline2016MappingFile.xml")));
            xmlMapper.parse(artifactData[1], artifactData[0], jCas);
        }
    }

}
