package de.julielab.jcore.multiplier.pmc;

import de.julielab.costosys.Constants;
import de.julielab.costosys.dbconnection.DBCIterator;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.types.pubmed.Header;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class PMCDBMultiplierTest {
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:" + DataBaseConnector.POSTGRES_VERSION);
    private static String costosysConfig;

    @BeforeAll
    public static void setup() throws ConfigurationException {
        postgres.start();
        DBTestUtils.createAndSetHiddenConfig(Path.of("src", "test", "resources", "hiddenConfig").toString(), postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.setActiveTableSchema("pmc");
        costosysConfig = DBTestUtils.createTestCostosysConfig("pmc", 2, postgres);
        new File(costosysConfig).deleteOnExit();
        dbc.withConnectionExecute(d -> d.createTable(Constants.DEFAULT_DATA_TABLE_NAME, "Test data table."));
        dbc.withConnectionExecute(d -> d.importFromXMLFile(Path.of("src", "test", "resources", "testdocs").toString(), Constants.DEFAULT_DATA_TABLE_NAME));
        dbc.withConnectionExecute(d -> d.createSubsetTable("testsubset", Constants.DEFAULT_DATA_TABLE_NAME, "Test subset."));
        dbc.withConnectionExecute(d -> d.initSubset("testsubset", Constants.DEFAULT_DATA_TABLE_NAME));
        assertThat(dbc.countRowsOfDataTable(Constants.DEFAULT_DATA_TABLE_NAME, null));
        DBCIterator<byte[][]> documentIterator = (DBCIterator<byte[][]>) dbc.withConnectionQuery(d -> d.queryDataTable(Constants.DEFAULT_DATA_TABLE_NAME, null));
        // check that the documents are actually in the database as expected
        List<String> docIds = StreamSupport.stream(Spliterators.spliteratorUnknownSize(documentIterator, 0), false).map(b -> new String(b[0], StandardCharsets.UTF_8)).collect(Collectors.toList());
        assertThat(docIds).containsExactlyInAnyOrder("PMC6949206", "PMC7511315");
    }

    @Test
    public void next() throws Exception {
        AnalysisEngine engine = AnalysisEngineFactory.createEngine(PMCDBMultiplier.class);
        JCasIterator jCasIterator = engine.processAndOutputNewCASes(prepareCas());
        List<String> documentTexts = new ArrayList<>();
        List<String> docIds = new ArrayList<>();
        while (jCasIterator.hasNext()) {
            JCas newCas = jCasIterator.next();
            documentTexts.add(newCas.getDocumentText());
            docIds.add(JCasUtil.selectSingle(newCas, Header.class).getDocId());
            newCas.release();
        }
        assertThat(docIds).containsExactlyInAnyOrder("PMC6949206", "PMC7511315");
    }

    /**
     * Creates a JCas and adds a RowBatch for the test documents in the source XML table as well as the data table and subset table and schema names.
     *
     * @return A JCas prepared for the tests in this class.
     * @throws UIMAException If some UIMA operation fails.
     */
    private JCas prepareCas() throws UIMAException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types", "de.julielab.jcore.types.jcore-casflow-types");
        RowBatch rowBatch = new RowBatch(jCas);
        StringArray dataTable = new StringArray(jCas, 1);
        dataTable.set(0, Constants.DEFAULT_DATA_TABLE_NAME);
        rowBatch.setTables(dataTable);
        StringArray tableSchema = new StringArray(jCas, 1);
        tableSchema.set(0, "pmc");
        rowBatch.setTableSchemas(tableSchema);
        rowBatch.setTableName("testsubset");
        FSArray pks = new FSArray(jCas, 2);
        // Read all documents
        List<String> pkStrings = List.of("PMC6949206", "PMC7511315");
        for (String pkString : pkStrings) {
            StringArray pk = new StringArray(jCas, 1);
            pk.set(0, pkString);
            pks = JCoReTools.addToFSArray(pks, pk);
        }
        rowBatch.setIdentifiers(pks);
        rowBatch.setCostosysConfiguration(costosysConfig);
        rowBatch.addToIndexes();
        return jCas;
    }
}