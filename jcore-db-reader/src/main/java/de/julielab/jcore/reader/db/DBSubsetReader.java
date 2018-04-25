package de.julielab.jcore.reader.db;

import de.julielab.xmlData.dataBase.DataBaseConnector;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

import static de.julielab.jcore.reader.db.SubsetReaderConstants.*;

public abstract class DBSubsetReader extends DBReaderBase {

    static final String DESC_ADDITIONAL_TABLES = "An array of table " +
            "names. By default, the table names will be resolved against the active data postgres schema " +
            "configured in the CoStoSys configuration file. If a name is already schema qualified, i.e. contains " +
            "a dot, the active data schema will be ignored. When reading documents from the document data table, " +
            "the additional tables will be joined onto the data table using the primary keys of the queried " +
            "documents. Using the table schema for the additional documents defined by the 'AdditionalTableSchema' " +
            "parameter, the columns that are marked as 'retrieve=true' in the table schema, are returned " +
            "together with the main document data. This mechanism is most prominently used to retrieve annotation table " +
            "data together with the original document text in XMI format for the JeDIS system.";
    static final String DESC_ADDITIONAL_TABLE_SCHEMAS = "The table schemas " +
            "that corresponds to the additional tables given with the 'AdditionalTables' parameter. If only one schema " +
            "name is given, that schema must apply to all additional tables.";
    private final static Logger log = LoggerFactory.getLogger(DBSubsetReader.class);
    @ConfigurationParameter(name = PARAM_RESET_TABLE, defaultValue = "false", mandatory = false, description = "If set " +
            "to true and the parameter 'Table' is set to a subset table, the subset table will be reset at" +
            "the initialization of the reader to be ready for processing of the whole subset. Do not use when multiple " +
            "readers read the same subset table.")
    protected Boolean resetTable;
    @ConfigurationParameter(name = PARAM_DATA_TIMESTAMP, mandatory = false, description = "PostgreSQL timestamp " +
            "expression that is evaluated against the data table. The data table schema, which must be the " +
            "active data table schema in the CoStoSys configuration as always, must specify a single timestamp " +
            "field for this parameter to work. Only data rows with a timestamp value larger than the given " +
            "timestamp expression will be processed. Note that when reading from a subset table, there may be " +
            "subset rows indicated to be in process which are finally not read from the data table. This is " +
            "an implementational shortcoming and might be addressed if respective feature requests are given " +
            "through the JULIE Lab GitHub page or JCoRe issues.")
    protected String dataTimestamp;
    @ConfigurationParameter(name = PARAM_FETCH_IDS_PROACTIVELY, defaultValue = "true", description = "If set to " +
            "true and when reading from a subset table, batches of document IDs will be retrieved in a background " +
            "thread while the previous batch is already in process. This is meant to minimize waiting time " +
            "for the database. Deactivate this feature if you encounter issues with databaase connections.")
    protected Boolean fetchIdsProactively;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false, description = DESC_ADDITIONAL_TABLES)
    protected String[] additionalTableNames;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLE_SCHEMAS, mandatory = false, description = DESC_ADDITIONAL_TABLE_SCHEMAS)
    protected String[] additionalTableSchemas;
    protected String hostName;
    protected String pid;
    protected String dataTable;
    protected Boolean readDataTable = false;
    protected String[] tables;
    protected String[] schemas;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        hostName = getHostName();
        pid = getPID();

        dataTimestamp = (String) getConfigParameterValue(PARAM_DATA_TIMESTAMP);
        resetTable = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_RESET_TABLE)).orElse(false);
        this.fetchIdsProactively = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY)).orElse(true);
        additionalTableNames = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
        additionalTableSchemas = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLE_SCHEMAS);
        checkAdditionalTableParameters(additionalTableNames, additionalTableSchemas);
        determineDataTable();
        try {
            // Check whether a subset table name or a data table name was given.
            if (readDataTable) {
                if (additionalTableNames != null)
                    throw new NotImplementedException("At the moment multiple tables can only be joined"
                            + " if the data table is referenced by a subset, for which the name has to be"
                            + " given in the Table parameter.");
                dbc.checkTableDefinition(tableName);
                readDataTable = true;
                Integer tableRows = dbc.countRowsOfDataTable(tableName, whereCondition);
                totalDocumentCount = limitParameter != null ? Math.min(tableRows, limitParameter) : tableRows;
                hasNext = !dbc.isEmpty(tableName);
                tables = new String[]{tableName};
                schemas = new String[]{dbc.getActiveTableSchema()};
            } else {
                if (batchSize == 0)
                    log.warn("Batch size of retrieved documents is set to 0. Nothing will be returned.");
                if (resetTable)
                    dbc.resetSubset(tableName);

                Integer unprocessedDocs = dbc.countUnprocessed(tableName);
                totalDocumentCount = limitParameter != null ? Math.min(unprocessedDocs, limitParameter) : unprocessedDocs;
                dataTable = dbc.getReferencedTable(tableName);
                hasNext = dbc.hasUnfetchedRows(tableName);
                log.debug("Checking if the subset table \"{}\" has unfetched rows. Result: {}", tableName, hasNext);

                if (additionalTableNames != null && additionalTableNames.length > 0) {
                    joinTables = true;

                    dbc.checkTableSchemaCompatibility(dbc.getActiveTableSchema(), additionalTableSchemas);
                    ImmutablePair<Integer, String[]> additionalTableNumAndNames = checkAndAdjustAdditionalTables(dbc, dataTable, additionalTableNames);
                    int numAdditionalTables = additionalTableNumAndNames.getLeft();
                    tables = additionalTableNumAndNames.getRight();

                    // Assemble the data table schema together with all additional table schemas in one array.
                    schemas = new String[numAdditionalTables + 1];
                    schemas[0] = dbc.getActiveTableSchema();
                    System.arraycopy(additionalTableSchemas, 0, schemas, 1, additionalTableSchemas.length);
                } else {
                    tables = new String[]{dataTable};
                    schemas = new String[1];
                    schemas[0] = dbc.getActiveTableSchema();
                }
            }
        } catch (TableSchemaMismatchException e) {
            throw new ResourceInitializationException(e);
        }
        logConfigurationState();
    }


    private void logConfigurationState() {
        log.info("Subset table {} will be reset upon pipeline start: {}", tableName, resetTable);
        if (log.isInfoEnabled())
            log.info("Names of additional tables to join: {}", StringUtils.join(additionalTableNames, ", "));
        log.info("TableName is: \"{}\"; referenced data table name is: \"{}\"", tableName, dataTable);
    }


    /**
     * Convenience method for extending classes.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        List<Object> numColumnsAndFields = dbc.getNumColumnsAndFields(joinTables, tables, schemas);
        for (int i = 1; i < numColumnsAndFields.size(); i++) {
            List<Map<String, Object>> retrievedSchemaFields = (List<Map<String, Object>>) numColumnsAndFields.get(i);
            for (Map<String, Object> field : retrievedSchemaFields)
                fields.add(field);
        }
        return fields;

    }

    private String getPID() {
        String id = ManagementFactory.getRuntimeMXBean().getName();
        return id.substring(0, id.indexOf('@'));
    }

    private String getHostName() {
        InetAddress address;
        String hostName;
        try {
            address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        return hostName;
    }

    private void determineDataTable() throws ResourceInitializationException {
        try {
            String nextDataTable = dbc.getNextDataTable(tableName);
            if (nextDataTable != null) {
                readDataTable = false;
                dataTable = nextDataTable;
            } else {
                log.info("The table \"{}\" is a data table, documents will not be marked to be in process and no " +
                        "synchronization of multiple DB readers will happen.", tableName);
                readDataTable = true;
            }
        } catch (SQLException e) {
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * This method checks whether the required parameters are set to meaningful
     * values and throws an IllegalArgumentException when not.
     *
     * @throws ResourceInitializationException
     */
    protected void checkAdditionalTableParameters(String[] additionalTableNames, String[] additionalTableSchemas) throws ResourceInitializationException {
        if (additionalTableNames != null && additionalTableSchemas == null) {
            throw new ResourceInitializationException(new IllegalArgumentException("If multiple tables will be joined"
                    + " the table schema for the additional tables (besides the base document table which should be configured using the database connector configuration) must be specified."));
        }
        List<Integer> nullindexes = new ArrayList<>();
        for (int i = 0; additionalTableNames != null && i < additionalTableNames.length; i++) {
            String additionalTableName = additionalTableNames[i];
            if (StringUtils.isBlank(additionalTableName))
                nullindexes.add(i);
        }
        if (!nullindexes.isEmpty())
            throw new ResourceInitializationException(new IllegalArgumentException("The following 0-based array indexes " +
                    "of the passed additional tables were null or empty: " + nullindexes));

        nullindexes.clear();
        for (int i = 0; additionalTableSchemas != null && i < additionalTableSchemas.length; i++) {
            String additionalTableSchemaName = additionalTableSchemas[i];
            if (StringUtils.isBlank(additionalTableSchemaName))
                nullindexes.add(i);
        }
        if (!nullindexes.isEmpty())
            throw new ResourceInitializationException(new IllegalArgumentException("The following 0-based array indexes " +
                    "of the passed additional table schemas were null or empty: " + nullindexes));
    }

    /**
     * Checks whether the given additional tables exist. If not, it is checked if
     * the table names contain dots which are reserved for schema qualification in
     * Postgres. It is tried again to find the tables with underscores ('_'), then.
     * The tables are also searched in the data schema. When the names contain dots,
     * the substring up to the first dot is tried as schema qualification before
     * prepending the data schema.
     */
    protected ImmutablePair<Integer, String[]> checkAndAdjustAdditionalTables(DataBaseConnector dbc, String dataTable, String[] additionalTableNames) {
        List<String> foundTables = new ArrayList<>();
        foundTables.add(dataTable);
        for (int i = 0; i < additionalTableNames.length; i++) {
            String resultTableName = null;
            if (dbc.tableExists(additionalTableNames[i]))
                resultTableName = additionalTableNames[i];
            // Try with default data postgres schema prepended.
            if (null == resultTableName) {
                String tn = dbc.getActiveDataPGSchema() + "." + additionalTableNames[i];
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with all dots converted to underscores but the first dot, so
            // that the substring up to the first dot could be a schema
            // qualification.
            if (null == resultTableName) {
                int dotIndex = additionalTableNames[i].indexOf('.');
                String prefix = additionalTableNames[i].substring(0, dotIndex);
                String rest = additionalTableNames[i].substring(dotIndex + 1);
                String tn = prefix + "." + rest.replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with the original name but all dots converted to underscores.
            if (null == resultTableName) {
                String tn = additionalTableNames[i].replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with all all dots converted to underscored and the active
            // data schema prepended.
            if (null == resultTableName) {
                String tn = dbc.getActiveDataPGSchema() + "." + additionalTableNames[i].replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // We have really tried...
            if (null == resultTableName) {
                log.warn("The table {} does not exist!", additionalTableNames[i]);
            } else
                foundTables.add(resultTableName);
        }
        String[] tables = foundTables.toArray(new String[foundTables.size()]);
        // -1 because here we also have added the document table which is not an
        // additional table but the main table!
        int numAdditionalTables = tables.length - 1;
        return new ImmutablePair<Integer, String[]>(numAdditionalTables, tables);
    }

}
