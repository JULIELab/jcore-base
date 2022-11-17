package de.julielab.jcore.reader.db;

import de.julielab.costosys.Constants;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.SQLException;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class DBMultiplierReaderTest {
    @Container
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:" + DataBaseConnector.POSTGRES_VERSION);

    @BeforeAll
    public static void setup() throws SQLException {
        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.obtainOrReserveConnection();
        DBTestUtils.setupDatabase(dbc, "src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 20, postgres);
    }

    @Test
    public void testDBMultiplierReader() throws UIMAException, IOException, ConfigurationException {

        String costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 2, postgres);
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class,
                PARAM_BATCH_SIZE, 5,
                PARAM_TABLE, "testsubset",
                PARAM_COSTOSYS_CONFIG_NAME, costosysConfig);
        assertTrue(reader.hasNext());
        int batchCount = 0;
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            RowBatch rowbatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            assertNotNull(rowbatch);
            assertNotNull(rowbatch.getIdentifiers());
            assertEquals(5, rowbatch.getIdentifiers().size());
            assertNotNull(rowbatch.getTables());
            assertEquals(Constants.DEFAULT_DATA_TABLE_NAME, rowbatch.getTables(0));
            ++batchCount;
            jCas.reset();
        }
        assertEquals(4, batchCount);
    }

}
