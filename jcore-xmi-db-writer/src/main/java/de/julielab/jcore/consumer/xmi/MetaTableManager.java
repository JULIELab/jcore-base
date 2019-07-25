package de.julielab.jcore.consumer.xmi;

import com.google.common.collect.Sets;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.binary.BinaryStorageAnalysisResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.TypeSystem;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;

public class MetaTableManager {
    public static final String BINARY_MAPPING_TABLE = XmiSplitConstants.BINARY_MAPPING_TABLE;
    public static final String BINARY_FEATURES_TO_MAP_TABLE = XmiSplitConstants.BINARY_FEATURES_TO_MAP_TABLE;
    public static final String BINARY_MAPPING_COL_STRING = XmiSplitConstants.BINARY_MAPPING_COL_STRING;
    public static final String BINARY_MAPPING_COL_ID = XmiSplitConstants.BINARY_MAPPING_COL_ID;
    public static final String BINARY_FEATURES_TO_MAP_COL_FEATURE = XmiSplitConstants.BINARY_FEATURES_TO_MAP_COL_FEATURE;
    public static final String BINARY_FEATURES_TO_MAP_COL_MAP = XmiSplitConstants.BINARY_FEATURES_TO_MAP_COL_MAP;
    public static final String XMI_NS_TABLE = XmiSplitConstants.XMI_NS_TABLE;
    public static final String PREFIX = XmiSplitConstants.PREFIX;
    public static final String NS_URI = XmiSplitConstants.NS_URI;
    private static final Logger log = LoggerFactory.getLogger(MetaTableManager.class);
    private Set<String> knownNSPrefixes = new HashSet<String>();

    private DataBaseConnector dbc;
    private String xmiMetaSchema;

    public MetaTableManager(DataBaseConnector dbc, String xmiMetaSchema) {
        this.dbc = dbc;
        this.xmiMetaSchema = xmiMetaSchema;
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
                String sql = String.format("SELECT %s FROM %s", PREFIX, xmiMetaSchema + "." + XMI_NS_TABLE);
                ResultSet rs = stmt.executeQuery(String.format(sql));
                while (rs.next()) {
                    String knownPrefix = rs.getString(1);
                    knownNSPrefixes.add(knownPrefix);
                }

                String template = "INSERT INTO %s VALUES('%s','%s')";
                for (Entry<String, String> nsEntry : notFound) {
                    prefix = nsEntry.getKey();
                    uri = nsEntry.getValue();
                    sql = String.format(template, xmiMetaSchema + "." + XMI_NS_TABLE, prefix, uri);
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
        if (!dbc.tableExists(xmiMetaSchema + "." + XMI_NS_TABLE)) {
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                conn.setAutoCommit(true);
                if (!dbc.schemaExists(xmiMetaSchema))
                    dbc.createSchema(xmiMetaSchema);
                Statement stmt = conn.createStatement();
                log.info("Creating XMI namespace table {}", xmiMetaSchema + "." + XMI_NS_TABLE);
                String sql = String.format("CREATE TABLE %s (%s text PRIMARY KEY, %s text)", xmiMetaSchema + "." + XMI_NS_TABLE, PREFIX, NS_URI);
                stmt.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
                SQLException ne = e.getNextException();
                if (null != ne)
                    ne.printStackTrace();
            }
        }
    }

    /**
     * <p>Synchronized updates to the map XMI string -> ID. The 'XMI strings' are XML element names, attribute names and also attribute values which don't seem to have a lot of
     * values as determined by the {@link de.julielab.xml.binary.BinaryJeDISNodeEncoder#findMissingItemsForMapping(Collection, TypeSystem, Map, Map)} method. An attribute is assumed to
     * not have a lot of different values if it has at most half as many different values as there are occurrences of the attribute, and, thus, the respective UIMA type feature.
     * This strategy is currently only applied to string values.</p>
     * <p>This method checks if there are items to be mapped which are not yet present in the current mapping which is passed from the <tt>XMIDBWriter</tt>. If there
     * are new items, the mapping table is locked from concurrent access and updated with the new values. The updated
     * mapping is returned to be kept for future applications.</p>
     *
     * @param missingItemsFunction A function that wraps the call to {@link de.julielab.xml.binary.BinaryJeDISNodeEncoder#findMissingItemsForMapping(Collection, TypeSystem, Map, Map)} to keep uninteresting parameters from being passed to this method.
     * @param currentMappingState  The mapping as it is currently known to the <tt>XMIDBWriter</tt> instance.
     * @return The mapping with all known mappings from the database, potentially with updated elements from the current document.
     * @throws AnalysisEngineProcessException If the database communication fails.
     */
    public Pair<Map<String, Integer>, Map<String, Boolean>> updateBinaryStringMappingTable(BiFunction<Map<String, Integer>, Map<String, Boolean>, BinaryStorageAnalysisResult> missingItemsFunction, Map<String, Integer> currentMappingState, Map<String, Boolean> currentMappedAttributes, boolean writeToDatabase) throws AnalysisEngineProcessException {
        Map<String, Integer> completeMapping = currentMappingState;
        Map<String, Boolean> completeMappedAttributes = currentMappedAttributes;
        final BinaryStorageAnalysisResult missingItemsFromCurrentState = missingItemsFunction.apply(currentMappingState, currentMappedAttributes);
        if (!missingItemsFromCurrentState.getMissingValuesToMap().isEmpty()) {
            String mappingTableName = xmiMetaSchema + "." + BINARY_MAPPING_TABLE;
            String featuresToMapTableName = xmiMetaSchema + "." + BINARY_FEATURES_TO_MAP_TABLE;
            String sql = null;
            try (CoStoSysConnection costoConn = dbc.obtainOrReserveConnection()) {
                costoConn.getConnection().beginRequest();
                boolean wasAutoCommit = costoConn.getAutoCommit();
                costoConn.setAutoCommit(false);
                final Statement stmt = costoConn.createStatement();

                createBinaryMetaTables(mappingTableName, featuresToMapTableName, costoConn);

                // Completely lock the tables. This is a synchronization mechanism: All mapping updates will wait at this
                // exact location. On gaining access exclusive access, the table is first updated before it is
                // released again (which happens on the end of the transaction).
                long time = System.currentTimeMillis();
                obtainLockToMappingTable(mappingTableName, stmt);
                obtainLockToMappedFeaturesTable(featuresToMapTableName, stmt);
                time = System.currentTimeMillis() - time;
                log.debug("Thread {} obtained locks to the binary mapping tables after {}ms.", Thread.currentThread(), time);

                // Read the mapping table
                Map<String, Integer> existingMappingWithDbUpdate = updateMapping(mappingTableName, currentMappingState, stmt);
                Map<String, Boolean> featuresToMapFromDatabase = readFeaturesToMapFromDatabase(featuresToMapTableName, stmt);

                final ImmutablePair<Map<String, Integer>, Map<String, Boolean>> missing = performMappingUpdate(missingItemsFunction, featuresToMapFromDatabase, currentMappedAttributes, existingMappingWithDbUpdate, mappingTableName, featuresToMapTableName, costoConn, wasAutoCommit, writeToDatabase);
                final Map<String, Integer> missingItems = missing.getLeft();
                final Map<String, Boolean> missingFeaturesToMap = missing.getRight();

                completeMapping = existingMappingWithDbUpdate;
                completeMapping.putAll(missingItems);

                completeMappedAttributes = featuresToMapFromDatabase;
                completeMappedAttributes.putAll(missingFeaturesToMap);
            } catch (SQLException e) {
                log.error("Could not retrieve or update binary meta data tables. The last sent SQL query was {}", sql, e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        return new ImmutablePair(completeMapping, completeMappedAttributes);
    }

    private void writeMappingsToDatabase(Map<String, Boolean> currentMappedAttributes, String mappingTableName, String featuresToMapTableName, CoStoSysConnection costoConn, boolean wasAutoCommit, Map<String, Boolean> featuresToMapFromDatabase, Map<String, Integer> missingItems, Map<String, Boolean> missingFeaturesToMap) throws SQLException {
        insertMissingMappings(mappingTableName, costoConn, missingItems);
        insertMissingFeaturesToMap(featuresToMapTableName, costoConn, missingFeaturesToMap, currentMappedAttributes, featuresToMapFromDatabase);

        // Commit the changes made
        log.debug("Thread {} is committing mapping changes to the database.", Thread.currentThread().getName());
        costoConn.commit();
        costoConn.setAutoCommit(wasAutoCommit);
        costoConn.getConnection().endRequest();
    }

    private ImmutablePair<Map<String, Integer>, Map<String, Boolean>> performMappingUpdate(BiFunction<Map<String, Integer>, Map<String, Boolean>, BinaryStorageAnalysisResult> missingItemsFunction, Map<String, Boolean> featuresToMapFromDatabase, Map<String, Boolean> currentMappedAttributes, Map<String, Integer> existingMappingWithDbUpdate, String mappingTableName, String featuresToMapTableName, CoStoSysConnection costoConn, boolean wasAutoCommit, boolean writeToDatabase) throws SQLException {
        // Run the analysis with the fresh data from the database
        Map<String, Boolean> existingFeaturesToMapWithDbUpdate = new HashMap<>(currentMappedAttributes);
        existingFeaturesToMapWithDbUpdate.putAll(featuresToMapFromDatabase);
        final BinaryStorageAnalysisResult analysisResult = missingItemsFunction.apply(existingMappingWithDbUpdate, existingFeaturesToMapWithDbUpdate);

        Map<String, Integer> missingItems = analysisResult.getMissingItemsMapping();

        Map<String, Boolean> missingFeaturesToMap = analysisResult.getMissingFeaturesToMap();
        if (writeToDatabase) {
            writeMappingsToDatabase(currentMappedAttributes, mappingTableName, featuresToMapTableName, costoConn, wasAutoCommit, featuresToMapFromDatabase, missingItems, missingFeaturesToMap);
        }
        return new ImmutablePair<>(missingItems, missingFeaturesToMap);
    }

    private void obtainLockToMappingTable(String mappingTableName, Statement stmt) throws AnalysisEngineProcessException {
        String sql = null;
        try {
            sql = String.format("LOCK TABLE ONLY %s IN ACCESS EXCLUSIVE MODE", mappingTableName);
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Could not lock table {}. SQL was: {}", mappingTableName, sql);
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void obtainLockToMappedFeaturesTable(String featuresToMapTableName, Statement stmt) throws AnalysisEngineProcessException {
        String sql = null;
        try {
            sql = String.format("LOCK TABLE ONLY %s IN ACCESS EXCLUSIVE MODE", featuresToMapTableName);
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Could not lock table {}. SQL was: {}", featuresToMapTableName, sql);
            throw new AnalysisEngineProcessException(e);
        }
    }

    private Map<String, Integer> updateMapping(String mappingTableName, Map<String, Integer> currentMappingState, Statement stmt) throws AnalysisEngineProcessException {
        String sql = null;
        try {
            Map<String, Integer> existingMapping = new HashMap<>(currentMappingState);
            // Only request what we don't already have. Since the mapping IDs are a enumeration,
            // we can just order by them descendingly and get the head of this list. The remainder
            // of size currentMappingState.size() is already known.
            sql = String.format("SELECT %s,%s FROM %s ORDER BY %s DESC LIMIT (SELECT count(%s) FROM %s)-%d", BINARY_MAPPING_COL_STRING, BINARY_MAPPING_COL_ID, mappingTableName, BINARY_MAPPING_COL_ID, BINARY_MAPPING_COL_ID, mappingTableName, currentMappingState.size());
            final ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                existingMapping.put(rs.getString(1), rs.getInt(2));
            }
            return existingMapping;
        } catch (SQLException e) {
            log.error("Could not retrieve mappings from the mapping table {}. SQL was: {}", mappingTableName, sql);
            throw new AnalysisEngineProcessException(e);
        }
    }

    private Map<String, Boolean> readFeaturesToMapFromDatabase(String featuresToMapTableName, Statement stmt) throws AnalysisEngineProcessException {
        String sql = null;
        try {
            Map<String, Boolean> existingFeaturesToMap = new HashMap<>();
            sql = String.format("SELECT %s,%s FROM %s", BINARY_FEATURES_TO_MAP_COL_FEATURE, BINARY_FEATURES_TO_MAP_COL_MAP, featuresToMapTableName);
            final ResultSet rsFeaturesToMap = stmt.executeQuery(sql);
            while (rsFeaturesToMap.next()) {
                existingFeaturesToMap.put(rsFeaturesToMap.getString(1), rsFeaturesToMap.getBoolean(2));
            }
            return existingFeaturesToMap;
        } catch (SQLException e) {
            log.error("Could not retrieve the features to be mapped from {}. SQL was: {}", featuresToMapTableName, sql);
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void insertMissingMappings(String mappingTableName, CoStoSysConnection costoConn, Map<String, Integer> missingItems) {
        log.debug("Inserting {} missing mappings into the mapping table {}", missingItems.size(), mappingTableName);
        if (missingItems.isEmpty())
            return;
        String sql = null;
        try {
            // Add the missing mapping items into the mapping table
            sql = String.format("INSERT INTO %s values(?, ?)", mappingTableName);
            final PreparedStatement ps = costoConn.prepareStatement(sql);
            for (String mappedString : missingItems.keySet()) {
                ps.setString(1, mappedString);
                ps.setInt(2, missingItems.get(mappedString));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Could not insert new mapping into the table {}. SQL was: ", mappingTableName, sql);
        }
    }

    private void insertMissingFeaturesToMap(String featuresToMapTableName, CoStoSysConnection costoConn, Map<String, Boolean> missingFeaturesToMap, Map<String, Boolean> currentMappedAttributes, Map<String, Boolean> featuresToMapFromDatabase) {
        String sql = null;
        try {
            Map<String, Boolean> toInsert = new HashMap<>(missingFeaturesToMap);
            // If we gave a blacklist of features to the XmiXbWriter and this is the very first table update,
            // there will be elements in the current state which are not yet in the table
            final Sets.SetView<String> predefinedFeaturesToMap = Sets.difference(currentMappedAttributes.keySet(), featuresToMapFromDatabase.keySet());
            predefinedFeaturesToMap.forEach(feature -> {
                toInsert.put(feature, currentMappedAttributes.get(feature));
                featuresToMapFromDatabase.put(feature, currentMappedAttributes.get(feature));
            });

            sql = String.format("INSERT INTO %s values(?, ?)", featuresToMapTableName);
            final PreparedStatement psFeaturesToMap = costoConn.prepareStatement(sql);
            for (String mappedString : toInsert.keySet()) {
                psFeaturesToMap.setString(1, mappedString);
                psFeaturesToMap.setBoolean(2, toInsert.get(mappedString));
                psFeaturesToMap.addBatch();
            }
            psFeaturesToMap.executeBatch();
        } catch (SQLException e) {
            log.error("Could not insert new features to be mapped into the table {}. SQL was: {}", featuresToMapTableName, sql);
        }
    }

    private void createBinaryMetaTables(String mappingTableName, String featuresToMapTableName, CoStoSysConnection costoConn) throws SQLException {
        String sql;
        final Statement stmt = costoConn.createStatement();
        // Create mapping table
        try {
            if (!dbc.tableExists(mappingTableName)) {
                sql = String.format("CREATE TABLE %s (%s TEXT, %s INTEGER PRIMARY KEY)", mappingTableName, BINARY_MAPPING_COL_STRING, BINARY_MAPPING_COL_ID);
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            log.debug("Tried to create table {} but did not succeed. The table was probably already created by another process or thread.", mappingTableName);
        }
        costoConn.commit();
        // Create features to map table
        try {
            if (!dbc.tableExists(featuresToMapTableName)) {
                sql = String.format("CREATE TABLE %s (%s TEXT, %s BOOL)", featuresToMapTableName, BINARY_FEATURES_TO_MAP_COL_FEATURE, BINARY_FEATURES_TO_MAP_COL_MAP);
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            log.debug("Tried to create table {} but did not succeed. The table was probably already created by another process or thread.", mappingTableName);
        }
        costoConn.commit();
    }
}
