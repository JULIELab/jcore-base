package de.julielab.jcore.reader.db;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.configuration2.ex.ConfigurationException;
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

import static de.julielab.jcore.reader.db.TableReaderConstants.PARAM_BATCH_SIZE;
import static de.julielab.jcore.reader.db.TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME;
import static de.julielab.jcore.reader.db.TableReaderConstants.PARAM_TABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;


public class DBReaderTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();

    @BeforeClass
    public static void setup() throws SQLException {
       TestDBSetupHelper.setupDatabase(postgres);
    }

    @Test
    public void testDBReader() throws UIMAException, IOException, ConfigurationException {
        String costosysConfig = TestDBSetupHelper.createTestCostosysConfig("medline_2017", postgres);
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
