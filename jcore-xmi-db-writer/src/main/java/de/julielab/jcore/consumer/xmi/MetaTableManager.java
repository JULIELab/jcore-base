package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.xml.XmiSplitConstants;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;

public class MetaTableManager {
    public static final String XMI_NS_TABLE = XmiSplitConstants.XMI_NS_TABLE;
    public static final String PREFIX = XmiSplitConstants.PREFIX;
    public static final String NS_URI = XmiSplitConstants.NS_URI;
    private static final Logger log = LoggerFactory.getLogger(MetaTableManager.class);
    private Set<String> knownNSPrefixes = new HashSet<String>();

    private DataBaseConnector dbc;
    private String nsSchema;

    public MetaTableManager(DataBaseConnector dbc, String nsSchema) {
        this.dbc = dbc;
        this.nsSchema = nsSchema;
        createNamespaceTable(dbc);
    }

    void manageXMINamespaces(Map<String, String> nsAndXmiVersionMap) {
        List<Entry<String, String>> notFound = new ArrayList<>();
        for (Entry<String, String> nsEntry : nsAndXmiVersionMap.entrySet()) {
            if (!knownNSPrefixes.contains(nsEntry.getKey()))
                notFound.add(nsEntry);
        }
        String prefix = null;
        String uri = null;
        if (notFound.size() > 0) {
            try (CoStoSysConnection conn = dbc.reserveConnection()) {
                conn.setAutoCommit(true);
                Statement stmt = conn.createStatement();
                String sql = String.format("SELECT %s FROM %s", PREFIX, nsSchema + "." + XMI_NS_TABLE);
                ResultSet rs = stmt.executeQuery(String.format(sql));
                while (rs.next()) {
                    String knownPrefix = rs.getString(1);
                    knownNSPrefixes.add(knownPrefix);
                }

                String template = "INSERT INTO %s VALUES('%s','%s')";
                for (Entry<String, String> nsEntry : notFound) {
                    prefix = nsEntry.getKey();
                    uri = nsEntry.getValue();
                    sql = String.format(template, nsSchema + "." + XMI_NS_TABLE, prefix, uri);
                    stmt.execute(sql);
                }

                for (Entry<String, String> nsEntry : notFound) {
                    knownNSPrefixes.add(nsEntry.getKey());
                }
            } catch (PSQLException e) {
                log.debug("Tried to add already existing namespace \"{}={}\", ignoring.", prefix, uri);
            } catch (SQLException e) {
                e.printStackTrace();
                SQLException ne = e.getNextException();
                if (null != ne)
                    ne.printStackTrace();
            }
        }
    }


    private void createNamespaceTable(DataBaseConnector dbc) {
        if (!dbc.tableExists(nsSchema + "." + XMI_NS_TABLE)) {
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                conn.setAutoCommit(true);
                if (!dbc.schemaExists(nsSchema))
                    dbc.createSchema(nsSchema);
                Statement stmt = conn.createStatement();
                log.info("Creating XMI namespace table at {}", nsSchema + "."+ XMI_NS_TABLE);
                String sql = String.format("CREATE TABLE %s (%s text PRIMARY KEY, %s text)", nsSchema + "." + XMI_NS_TABLE, PREFIX, NS_URI);
                stmt.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
                SQLException ne = e.getNextException();
                if (null != ne)
                    ne.printStackTrace();
            }
        }
    }

}
