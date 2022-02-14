package de.julielab.jcore.multiplier.pmc;


import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.types.casflow.ToVisit;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test is an adaption of the XMLDBMultiplierTest in jcore-xml-db-reader. It tests whether the hash code comparison
 * works as intended.
 */
public class PMCDBMultiplierHashComparisonTest {

    private static final String SOURCE_XML_TABLE = "source_xml_table";
    private static final String TARGET_XMI_TABLE = "target_xmi_table";
    private static final String PMCID_FIELD_NAME = "pmcid";
    private static final String DOCID_FIELD_NAME = "docid";
    private static final String XML_FIELD_NAME = "xml";
    private static final String BASE_DOCUMENT_FIELD_NAME = "base_document";
    private static final String HASH_FIELD_NAME = "documentText_sha256";
    private static final String MAX_XMI_ID_FIELD_NAME = "max_xmi_id";
    private static final String SOFA_MAPPING_FIELD_NAME = "sofa_mapping";
    private static final String SUBSET_TABLE = "test_subset";
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:"+DataBaseConnector.POSTGRES_VERSION);
    private static String costosysConfig;

    @BeforeAll
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        postgres.start();
        DBTestUtils.createAndSetHiddenConfig(Path.of("src", "test", "resources", "hiddenConfig").toString(), postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.setActiveTableSchema("pmc");
        costosysConfig = DBTestUtils.createTestCostosysConfig("pmc", 2, postgres);
        new File(costosysConfig).deleteOnExit();
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            // We create two tables. One is the XML table the multiplier reads from and maps the contents to the JCas.
            // The other is a simulation of an XMI table used to serialize CAS instances via the jcore-xmi-db-writer.
            // We need that target table to test the hash value comparison mechanism: If a document does not exist
            // in the target table or has a non-matching hash on its document text, proceed as normal.
            // But if the hash matches, we want to reserve the possibility to skip most part of the subsequent pipeline.
            // For this, we could use the AnnnotationDefinedFlowController for jcore-flow-controllers. This controller
            // looks for annotations of the ToVisit type that specify which exact components in an aggregate should
            // be applied to the CAS carrying the ToVisit annotation.
            prepareSourceXMLTable(dbc, conn);
            prepareTargetXMITable(dbc, conn);
        }
        dbc.defineSubset(SUBSET_TABLE, SOURCE_XML_TABLE, "Test subset");
        assertThat(dbc.getNumRows(SOURCE_XML_TABLE)).isEqualTo(10);
        assertThat(dbc.getNumRows(TARGET_XMI_TABLE)).isEqualTo(5);

        dbc.close();
    }

    private static void prepareSourceXMLTable(DataBaseConnector dbc, CoStoSysConnection conn) throws SQLException {
        String xmlFmt = "<!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD with MathML3 v1.3 20210610//EN\" \"JATS-archivearticle1-3-mathml3.dtd\">\n" +
                "<article><front><article-meta><article-id pub-id-type=\"pmc\">%d</article-id><volume>42</volume></article-meta></front>\n" +
                "<body><sec><p>This is text nr %d.</p></sec></body>\n" +
                "</article>";
        dbc.createTable(SOURCE_XML_TABLE, "Test table for hash comparison test.");
        String sql = String.format("INSERT INTO %s (%s,%s) VALUES (?,XMLPARSE(CONTENT ?))", SOURCE_XML_TABLE, PMCID_FIELD_NAME, XML_FIELD_NAME);
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < 10; i++) {
            String xml = String.format(xmlFmt, i, i);
            ps.setString(1, String.valueOf(i));
            ps.setString(2, xml);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    private static void prepareTargetXMITable(DataBaseConnector dbc, CoStoSysConnection conn) throws SQLException {
        // The PMC parser tries to format blocks of content using newlines which makes the test a bit awkward.
        // The test might break if this formatting is changed.
        String documentTextFmt = "\nThis is text nr %d.\n\n";
        dbc.createTable(TARGET_XMI_TABLE, "xmi_text", "Test table for hash comparison test.");
        dbc.assureColumnsExist(TARGET_XMI_TABLE, List.of(HASH_FIELD_NAME), "text");
        String sql = String.format("INSERT INTO %s (%s,%s,%s,%s,%s) VALUES (?,XMLPARSE(CONTENT ?),?,?,?)", TARGET_XMI_TABLE, DOCID_FIELD_NAME, BASE_DOCUMENT_FIELD_NAME, HASH_FIELD_NAME, MAX_XMI_ID_FIELD_NAME, SOFA_MAPPING_FIELD_NAME);
        PreparedStatement ps = conn.prepareStatement(sql);
        // Note that we only add half of the documents compared to the source XML import. This way we test
        // if the code behaves right when the target document does not yet exist at all.
        for (int i = 0; i < 5; i++) {
            String xml = String.format(documentTextFmt, i, i);
            ps.setString(1, String.valueOf(i));
            ps.setString(2, xml);
            // For one document in the "target XMI" table we put in a wrong hash. Thus, this document should not trigger
            // the "toVisit" mechanism.
            if (i != 3)
                ps.setString(3, getHash(xml));
            else ps.setString(3, "someanotherhash");
            ps.setInt(4, 0);
            ps.setString(5, "dummy");
            ps.addBatch();
        }
        ps.executeBatch();
    }

    @AfterAll
    public static void tearDown() {
        postgres.stop();
    }

    private static String getHash(String str) {
        final byte[] sha = DigestUtils.sha256(str.getBytes());
        return Base64.encodeBase64String(sha);
    }

    /**
     * Creates a JCas and adds a RowBatch for all 10 documents in the source XML table as well as the data table and subset table and schema names.
     *
     * @return A JCas prepared for the tests in this class.
     * @throws UIMAException If some UIMA operation fails.
     */
    private JCas prepareCas() throws UIMAException {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types", "de.julielab.jcore.types.jcore-casflow-types");
        RowBatch rowBatch = new RowBatch(jCas);
        StringArray dataTable = new StringArray(jCas, 1);
        dataTable.set(0, SOURCE_XML_TABLE);
        rowBatch.setTables(dataTable);
        StringArray tableSchema = new StringArray(jCas, 1);
        tableSchema.set(0, "pmc");
        rowBatch.setTableSchemas(tableSchema);
        rowBatch.setTableName(SUBSET_TABLE);
        FSArray pks = new FSArray(jCas, 10);
        // Read all documents
        for (int i = 0; i < 10; i++) {
            StringArray pk = new StringArray(jCas, 1);
            pk.set(0, String.valueOf(i));
            pks = JCoReTools.addToFSArray(pks, pk);
        }
        rowBatch.setIdentifiers(pks);
        rowBatch.setCostosysConfiguration(costosysConfig);
        rowBatch.addToIndexes();
        return jCas;
    }

    @Test
    public void testHashComparison() throws Exception {
        JCas jCas = prepareCas();
        TypeSystemDescription tsDesc = TypeSystemDescriptionFactory.createTypeSystemDescription("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types", "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types", "de.julielab.jcore.types.jcore-casflow-types");
        AnalysisEngine engine = AnalysisEngineFactory.createEngine(PMCDBMultiplier.class, tsDesc,
                PMCDBMultiplier.PARAM_ADD_SHA_HASH, "documentText",
                PMCDBMultiplier.PARAM_TABLE_DOCUMENT, TARGET_XMI_TABLE,
                PMCDBMultiplier.PARAM_TABLE_DOCUMENT_SCHEMA, "xmi_text",
                PMCDBMultiplier.PARAM_TO_VISIT_KEYS, "ThisIsTheVisitKey"
        );
        JCasIterator jCasIterator = engine.processAndOutputNewCASes(jCas);
        List<String> toVisitKeys = new ArrayList<>();
        while (jCasIterator.hasNext()) {
            JCas newCas = jCasIterator.next();
            Collection<ToVisit> select = JCasUtil.select(newCas, ToVisit.class);
            select.forEach(tv -> tv.getDelegateKeys().forEach(k -> toVisitKeys.add(k)));
            newCas.release();
        }
        // There are 4 documents in the target table with the correct hash so we expect the delegate key 4 times
        assertThat(toVisitKeys).containsExactly("ThisIsTheVisitKey", "ThisIsTheVisitKey", "ThisIsTheVisitKey", "ThisIsTheVisitKey");
    }

    @Test
    public void testHashComparison2() throws Exception {
        JCas jCas = prepareCas();
        TypeSystemDescription tsDesc = TypeSystemDescriptionFactory.createTypeSystemDescription("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types", "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types", "de.julielab.jcore.types.jcore-casflow-types");
        // In this test, we do not specify the keys to visit; the whole subsequent pipeline should be skipped.
        // To indicate that, there should be ToVisit annotations but they should be null.
        AnalysisEngine engine = AnalysisEngineFactory.createEngine(PMCDBMultiplier.class, tsDesc,
                PMCDBMultiplier.PARAM_ADD_SHA_HASH, "documentText",
                PMCDBMultiplier.PARAM_TABLE_DOCUMENT, TARGET_XMI_TABLE,
                PMCDBMultiplier.PARAM_TABLE_DOCUMENT_SCHEMA, "xmi_text"
        );
        JCasIterator jCasIterator = engine.processAndOutputNewCASes(jCas);
        List<ToVisit> emptyToVisitAnnotation = new ArrayList<>();
        while (jCasIterator.hasNext()) {
            JCas newCas = jCasIterator.next();
            Collection<ToVisit> select = JCasUtil.select(newCas, ToVisit.class);
            select.stream().filter(tv -> tv.getDelegateKeys() == null).forEach(emptyToVisitAnnotation::add);
            newCas.release();
        }
        // There are 4 documents in the target table with the correct hash so we expect the delegate key 4 times
        assertThat(emptyToVisitAnnotation).hasSize(4);
    }
}
