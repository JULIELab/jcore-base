package de.julielab.jcore.reader.xml;


import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.db.test.DBTestUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
public class XMLDBMultiplierTest {

    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11.12");
    private static String costosysConfig;
    private static final String SOURCE_XML_TABLE = "source_xml_table";
    private static final String TARGET_XMI_TABLE = "target_xmi_table";
    private static final String PMID_FIELD_NAME = "pmid";
    private static final String DOCID_FIELD_NAME = "docid";
    private static final String XML_FIELD_NAME = "xml";
    private static final String BASE_DOCUMENT_FIELD_NAME = "base_document";
    private static final String HASH_FIELD_NAME = "documentText_sha256";
    private static final String MAX_XMI_ID_FIELD_NAME = "max_xmi_id";
    private static final String SOFA_MAPPING_FIELD_NAME = "sofa_mapping";

    @BeforeAll
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        postgres.start();
        DBTestUtils.createAndSetHiddenConfig(Path.of("src", "test", "resources", "hiddenConfig").toString(), postgres);

        DataBaseConnector dbc = DBTestUtils.getDataBaseConnector(postgres);
        dbc.setActiveTableSchema("medline_2016_nozip");
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2016_nozip", 1, postgres);
        new File(costosysConfig).deleteOnExit();
        try(CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            prepareSourceXMLTable(dbc, conn);
            prepareTargetXMITable(dbc, conn);
        }
        assertThat(dbc.getNumRows(SOURCE_XML_TABLE)).isEqualTo(10);
        assertThat(dbc.getNumRows(TARGET_XMI_TABLE)).isEqualTo(5);
        dbc.close();
    }

    private static void prepareSourceXMLTable(DataBaseConnector dbc, CoStoSysConnection conn) throws SQLException {
        String xmlFmt = "<xml><docid>%d</docid></xml>";
        dbc.createTable(SOURCE_XML_TABLE, "Test table for hash comparison test.");
        String sql = String.format("INSERT INTO %s (%s,%s) VALUES (?,?)", SOURCE_XML_TABLE, PMID_FIELD_NAME, XML_FIELD_NAME);
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < 10; i++) {
            String xml = String.format(xmlFmt, i);
            ps.setString(1, String.valueOf(i));
            ps.setString(2, xml);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    private static void prepareTargetXMITable(DataBaseConnector dbc, CoStoSysConnection conn) throws SQLException {
        // Note that the root is "xmi" and not "xml"
        String xmlFmt = "<xmi><docid>%d</docid></xmi>";
        dbc.createTable(TARGET_XMI_TABLE, "xmi_text", "Test table for hash comparison test.");
        dbc.assureColumnsExist(TARGET_XMI_TABLE, List.of(HASH_FIELD_NAME), "text");
        String sql = String.format("INSERT INTO %s (%s,%s,%s,%s,%s) VALUES (?,XMLPARSE(CONTENT ?),?,?,?)", TARGET_XMI_TABLE, DOCID_FIELD_NAME, BASE_DOCUMENT_FIELD_NAME, HASH_FIELD_NAME, MAX_XMI_ID_FIELD_NAME, SOFA_MAPPING_FIELD_NAME);
        PreparedStatement ps = conn.prepareStatement(sql);
        // Note that we only add half of the documents compared to the source XML import. This way we test
        // if the code behaves right when the target document does not yet exist at all.
        for (int i = 0; i < 5; i++) {
            String xml = String.format(xmlFmt, i);
            ps.setString(1, String.valueOf(i));
            ps.setString(2, xml);
            ps.setString(3, getHash(xml));
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

    @Test
    public void testHashComparison() {

    }


}
