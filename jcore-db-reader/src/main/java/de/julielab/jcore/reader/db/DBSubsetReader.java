package de.julielab.jcore.reader.db;

import de.julielab.costosys.cli.TableNotFoundException;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.costosys.dbconnection.util.CoStoSysSQLRuntimeException;
import de.julielab.costosys.dbconnection.util.TableSchemaMismatchException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static de.julielab.jcore.reader.db.SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS;
import static de.julielab.jcore.reader.db.SubsetReaderConstants.PARAM_FETCH_IDS_PROACTIVELY;

public abstract class DBSubsetReader extends DBReaderBase {
    public final static String PARAM_ADDITIONAL_TABLES = SubsetReaderConstants.PARAM_ADDITIONAL_TABLES;
    public static final String PARAM_RESET_TABLE = SubsetReaderConstants.PARAM_RESET_TABLE;
    public static final String PARAM_ADDITONAL_TABLES_STORAGE_PG_SCHEMA = "AdditionalTablesPostgresSchema";

    static final String DESC_ADDITIONAL_TABLES = "An array of table " +
            "names or a string in the form of a qualified Java class, i.e. a dot-separated path. In the latter case, " +
            "an existing table is searched for by converting the dots to underscores. A specific Postgres schema can be specified " +
            "by prepending the Java-style path with a schema name followed by a colon, e.g. 'myschema:de.julielab.jcore.types.Token'. " +
            "By default, the table names will be resolved against the active data postgres schema " +
            "configured in the CoStoSys configuration file. If a name is already schema qualified, i.e. contains " +
            "a dot or a colon, the active data schema will be ignored for this table. When reading documents from the document data table, " +
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

    @ConfigurationParameter(name = PARAM_FETCH_IDS_PROACTIVELY, defaultValue = "true", description = "If set to " +
            "true and when reading from a subset table, batches of document IDs will be retrieved in a background " +
            "thread while the previous batch is already in process. This is meant to minimize waiting time " +
            "for the database. Deactivate this feature if you encounter issues with databaase connections.")
    protected Boolean fetchIdsProactively;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false, description = DESC_ADDITIONAL_TABLES)
    /**
     * The names of tables to read data from aside from the primary data table.
     */
    protected String[] additionalTableNames;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLE_SCHEMAS, mandatory = false, description = DESC_ADDITIONAL_TABLE_SCHEMAS)
    /**
     * The table schemas of the tables to read aside from the primary data table, parallel to {@link #additionalTableNames}.
     */
    protected String[] additionalTableSchemas;
    protected String hostName;
    protected String pid;
    /**
     * The name of the primary data table to read data from. This is either the exact parameter value given with
     * {@link #PARAM_TABLE} or, if this parameter denotes a subset table, the data table referenced by the subset.
     */
    protected String dataTable;
    /**
     * This is true if the table name provided by the {@link #PARAM_TABLE} parameter is a data table.
     * If this parameter is false it means that we read from a subset table.
     */
    protected Boolean readDataTable = false;
    /**
     * The list of tables to read data from. The first element is always the data table. The following entries
     * reference the "additional tables".
     */
    protected String[] tables;
    /**
     * The list of table schemas of the tables to read from. This is parallel to {@link #tables}.
     */
    protected String[] schemas;
    @ConfigurationParameter(name = PARAM_ADDITONAL_TABLES_STORAGE_PG_SCHEMA, mandatory = false, description =
            "This optional parameter specifies the Postgres schema in which the additional tables to read are searched by default. If " +
                    "omitted, the active data schema from the CoStoSys configuration is assumed. The default can be overwritten for individual " +
                    "types. For details, see the description of the '" + PARAM_ADDITIONAL_TABLES + "' parameter.")
    private String additionalTablesPGSchema;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        hostName = getHostName();
        pid = getPID();


        resetTable = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_RESET_TABLE)).orElse(false);
        this.fetchIdsProactively = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY)).orElse(true);
        additionalTableNames = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
        additionalTableSchemas = (String[]) context.getConfigParameterValue(PARAM_ADDITIONAL_TABLE_SCHEMAS);
        additionalTablesPGSchema = Optional.ofNullable((String) getConfigParameterValue(PARAM_ADDITONAL_TABLES_STORAGE_PG_SCHEMA)).orElse(dbc.getActiveDataPGSchema());
        checkAdditionalTableParameters(additionalTableNames, additionalTableSchemas);
        determineDataTable();
        try {
            // Check whether a subset table name or a data table name was given.
            if (readDataTable) {
                if (additionalTableNames != null && additionalTableNames.length > 0) {
                    prepareTableJoin();
                } else {
                    tables = new String[]{tableName};
                    schemas = new String[]{dbc.getActiveTableSchema()};
                }
                dbc.checkTableDefinition(tableName);
                Integer tableRows = dbc.withConnectionQueryInteger(c -> c.countRowsOfDataTable(tableName, whereCondition));
                totalDocumentCount = limitParameter != null ? Math.min(tableRows, limitParameter) : tableRows;
                hasNext = !dbc.withConnectionQueryBoolean(c -> c.isEmpty(tableName));
            } else {
                if (batchSize == 0)
                    log.warn("Batch size of retrieved documents is set to 0. Nothing will be returned.");
                try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                    if (resetTable)
                        dbc.resetSubset(tableName);

                    Integer unprocessedDocs = dbc.countUnprocessed(tableName);
                    totalDocumentCount = limitParameter != null ? Math.min(unprocessedDocs, limitParameter) : unprocessedDocs;
                    dataTable = dbc.getReferencedTable(tableName);
                    if (dataTable == null)
                        throw new ResourceInitializationException(new IllegalStateException("The subset table " + tableName + " does not have a data table. This is an inconsistent state, each subset table must have a referenced data table. The data table has probably been deleted."));
                    hasNext = dbc.hasUnfetchedRows(tableName);
                    log.debug("Checking if the subset table \"{}\" has unfetched rows. Result: {}", tableName, hasNext);

                    if (additionalTableNames != null && additionalTableNames.length > 0) {
                        prepareTableJoin();
                    } else {
                        log.debug("No additional tables were given, reading data solely from table {}", dataTable);
                        tables = new String[]{dataTable};
                        schemas = new String[1];
                        schemas[0] = dbc.getActiveTableSchema();
                    }
                    dbc.checkTableHasSchemaColumns(dataTable, schemas[0]);
                }
            }
        } catch (TableSchemaMismatchException | TableNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        logConfigurationState();
    }

    /**
     * Searches for the additional tables by potentially converting Java type names into a valid Postgres representation.
     * Broadcasts the additional table schema into the <tt>schemas</tt> field.
     *
     * @throws TableSchemaMismatchException
     */
    private void prepareTableJoin() throws TableSchemaMismatchException {
        log.debug("Additional tables were given: {}", Arrays.toString(additionalTableNames));
        log.debug("Preparing for reading from multiple tables.");
        joinTables = true;

        dbc.checkTableSchemaCompatibility(dbc.getActiveTableSchema(), additionalTableSchemas);
        ImmutablePair<Integer, String[]> additionalTableNumAndNames = checkAndAdjustAdditionalTables(dbc, dataTable, additionalTableNames);
        int numAdditionalTables = additionalTableNumAndNames.getLeft();
        tables = additionalTableNumAndNames.getRight();
        if (numAdditionalTables > 0)
            System.arraycopy(tables, 1, additionalTableNames, 0, additionalTableNames.length);

        // Assemble the data table schema together with all additional table schemas in one array.
        schemas = new String[numAdditionalTables + 1];
        if (additionalTableSchemas.length == 1)
            Arrays.fill(schemas, additionalTableSchemas[0]);
        else
            System.arraycopy(additionalTableSchemas, 0, schemas, 1, additionalTableSchemas.length);
        schemas[0] = dbc.getActiveTableSchema();
    }


    private void logConfigurationState() {
        if (!readDataTable)
            log.info("Subset table {} will be reset upon pipeline start: {}", tableName, resetTable);
        if (log.isInfoEnabled())
            log.info("Names of additional tables to join: {}", StringUtils.join(additionalTableNames, ", "));
        log.info("TableName is: \"{}\"; referenced data table name is: \"{}\"", tableName, dataTable);
        log.info("List of all tables to read: {}", tables);
        log.info("List of the table schemas: {}", schemas);
    }


    /**
     * Convenience method for extending classes.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        Pair<Integer, List<Map<String, String>>> numColumnsAndFields = dbc.getNumColumnsAndFields(joinTables, schemas);
        return numColumnsAndFields.getRight().stream().map(HashMap<String, Object>::new).collect(Collectors.toList());
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

    /**
     * Determines the data table. This may be the specified table itself or, if it is a subset, the first referenced data table.
     *
     * @throws ResourceInitializationException If an SQL exception occurs.
     */
    private void determineDataTable() throws ResourceInitializationException {
        try {
            readDataTable = dbc.withConnectionQueryBoolean(c -> c.isDataTable(tableName));
            dataTable = dbc.withConnectionQueryString(c -> c.getNextOrThisDataTable(tableName));
            if (readDataTable)
                log.info("The table \"{}\" is a data table, documents will not be marked to be in process and no " +
                        "synchronization of multiple DB readers will happen.", tableName);
        } catch (CoStoSysSQLRuntimeException e) {
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
        if (additionalTableNames != null && additionalTableNames.length != 0 && additionalTableSchemas == null) {
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
     * In this case, a colon character is interpreted as the separation between a
     * specified Postgres schema and a Java-style path to be converted to a valid
     * table name by replacing dots with underscores. Always includes the data table.
     */
    protected ImmutablePair<Integer, String[]> checkAndAdjustAdditionalTables(DataBaseConnector dbc, String dataTable, String[] additionalTableNames) {
        List<String> foundTables = new ArrayList<>();
        foundTables.add(dataTable);
        for (int i = 0; i < additionalTableNames.length; i++) {
            String resultTableName = null;
            String rawName = additionalTableNames[i];

            if (rawName.contains(":")) {
                int colonIndex = rawName.indexOf(':');
                if (colonIndex == rawName.length() - 1)
                    throw new IllegalArgumentException("The table name \"" + rawName + "\" is invalid. Consult the description of the " + PARAM_ADDITIONAL_TABLES + " parameter for more information.");
                String schema = rawName.substring(0, colonIndex);
                String rawTableName = rawName.substring(colonIndex + 1);
                String tableName = rawTableName.replaceAll("\\.", "_");
                resultTableName = schema + "." + tableName;
            } else if (dbc.tableExists(rawName)) {
                resultTableName = rawName;
            } else if (dbc.tableExists(additionalTablesPGSchema + "." + rawName.replaceAll("\\.", "_"))) {
                resultTableName = additionalTablesPGSchema + "." + rawName.replaceAll("\\.", "_");
            }

            if (null == resultTableName) {
                throw new IllegalArgumentException("The table " + additionalTableNames[i] + " does not exist.");
            } else
                foundTables.add(resultTableName);
        }
        String[] tables = foundTables.toArray(new String[foundTables.size()]);
        // -1 because here we also have added the document table which is not an
        // additional table but the main table!
        int numAdditionalTables = tables.length - 1;
        return new ImmutablePair<>(numAdditionalTables, tables);
    }

}
