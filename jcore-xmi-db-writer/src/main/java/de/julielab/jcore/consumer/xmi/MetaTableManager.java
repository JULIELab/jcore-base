package de.julielab.jcore.consumer.xmi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.julielab.xmlData.dataBase.CoStoSysConnection;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.xml.XmiSplitConstants;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class MetaTableManager {
	private static final Logger log = LoggerFactory.getLogger(MetaTableManager.class);

	public static final String XMI_NS_TABLE = XmiSplitConstants.XMI_NS_TABLE;
	public static final String PREFIX = XmiSplitConstants.PREFIX;
	public static final String NS_URI = XmiSplitConstants.NS_URI;

	private Set<String> knownNSPrefixes = new HashSet<String>();

	private DataBaseConnector dbc;

	public MetaTableManager(DataBaseConnector dbc) {
		this.dbc = dbc;
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
				String sql = String.format("SELECT %s FROM %s", PREFIX, dbc.getActiveDataPGSchema() + "." + XMI_NS_TABLE);
				ResultSet rs = stmt.executeQuery(String.format(sql));
				while (rs.next()) {
					String knownPrefix = rs.getString(1);
					knownNSPrefixes.add(knownPrefix);
				}

				String template = "INSERT INTO %s VALUES('%s','%s')";
				for (Entry<String, String> nsEntry : notFound) {
					prefix = nsEntry.getKey();
					uri = nsEntry.getValue();
					sql = String.format(template, dbc.getActiveDataPGSchema() + "." + XMI_NS_TABLE, prefix, uri);
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
		if (!dbc.tableExists(dbc.getActiveDataPGSchema() + "." + XMI_NS_TABLE)) {
			 try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()){
				conn.setAutoCommit(true);
				Statement stmt = conn.createStatement();
				String sql = String.format("CREATE TABLE %s (%s text PRIMARY KEY, %s text)", dbc.getActiveDataPGSchema() + "." + XMI_NS_TABLE, PREFIX, NS_URI);
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
