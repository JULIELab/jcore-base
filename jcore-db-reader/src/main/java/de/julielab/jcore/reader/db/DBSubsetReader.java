package de.julielab.jcore.reader.db;

import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class DBSubsetReader extends DBReaderBase {
    /**
     * Multi-valued String parameter indicating which tables will be read from
     * additionally to the referenced data table. The tables will be joined to a
     * single CAS.
     */
    public static final String PARAM_ADDITIONAL_TABLES = "AdditionalTables";
    /**
     * Multi-valued String parameter indicating different schemas in case tables
     * will be joined. The schema for the referenced data table has to be the first
     * element. The schema for the additional tables has to be the second element.
     */
    public static final String PARAM_ADDITIONAL_TABLE_SCHEMA = "AdditionalTableSchema";
    /**
     * Boolean parameter. Determines whether a background thread should be used
     * which fetches the next batch of document IDs to process while the former
     * batch is already being processed. Using the background thread boosts
     * performance as waiting time is minimized. However, as the next batch of
     * documents is marked in advance as being in process, this approach is only
     * suitable when reading all available data.
     */
    public static final String PARAM_FETCH_IDS_PROACTIVELY = "FetchIdsProactively";
    /**
     * Boolean parameter. Indicates whether the read subset table is to be reset
     * before reading.
     */
    public static final String PARAM_RESET_TABLE = "ResetTable";
    /**
     * String parameter representing a long value. If not null, only documents with
     * a timestamp newer then the passed value will be processed.
     */
    public static final String PARAM_TIMESTAMP = "Timestamp";
    private final static Logger log = LoggerFactory.getLogger(DBSubsetReader.class);
    @ConfigurationParameter(name = PARAM_RESET_TABLE, defaultValue = "false", mandatory = false)
    protected Boolean resetTable;
    @ConfigurationParameter(name = PARAM_TIMESTAMP, mandatory = false)
    protected String timestamp;
    @ConfigurationParameter(name = PARAM_FETCH_IDS_PROACTIVELY, defaultValue = "true")
    protected Boolean fetchIdsProactively;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES, mandatory = false)
    protected String[] additionalTableNames;
    @ConfigurationParameter(name = PARAM_ADDITIONAL_TABLE_SCHEMA, mandatory = false)
    protected String additionalTableSchema;
    protected int numAdditionalTables;
    protected String hostName;
    protected String pid;

    protected String[] tables;
    protected String[] schemas;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        hostName = getHostName();
        pid = getPID();

        timestamp = (String) getConfigParameterValue(PARAM_TIMESTAMP);
        resetTable = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_RESET_TABLE)).orElse(false);
        this.fetchIdsProactively = Optional.ofNullable((Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY)).orElse(true);
        additionalTableNames = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
        additionalTableSchema = (String) getConfigParameterValue(PARAM_ADDITIONAL_TABLE_SCHEMA);
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
                hasNext = dbc.queryDataTable(tableName, whereCondition).hasNext();
            } else {
                if (batchSize == 0)
                    log.warn("Batch size of retrieved documents is set to 0. Nothing will be returned.");
                if (resetTable)
                    dbc.resetSubset(tableName);


                Integer unprocessedDocs = unprocessedDocumentCount();
                totalDocumentCount = limitParameter != null ? Math.min(unprocessedDocs, limitParameter) : unprocessedDocs;
                dataTable = dbc.getReferencedTable(tableName);
                hasNext = dbc.hasUnfetchedRows(tableName);
                log.debug("Checking if the subset table \"{}\" has unfetched rows. Result: {}", tableName, hasNext);

                if (additionalTableNames != null && additionalTableNames.length > 0) {
                    joinTables = true;

                    numAdditionalTables = additionalTableNames.length;
                    dbc.checkTableSchemaCompatibility(dbc.getActiveTableSchema(), additionalTableSchema);
                    checkAndAdjustAdditionalTables();

                    schemas = new String[numAdditionalTables + 1];
                    schemas[0] = dbc.getActiveTableSchema();
                    for (int i = 1; i < schemas.length; i++) {
                        schemas[i] = additionalTableSchema;
                    }
                } else {
                    numAdditionalTables = 0;
                }
            }
        } catch (TableSchemaMismatchException e) {
            throw new ResourceInitializationException(e);
        }
        checkParameters();
        logConfigurationState();
    }
    /**
     * This method checks whether the required parameters are set to meaningful
     * values and throws an IllegalArgumentException when not.
     *
     * @throws ResourceInitializationException
     */
    private void checkParameters() throws ResourceInitializationException {
        if (additionalTableNames != null && additionalTableSchema == null) {
            throw new ResourceInitializationException(new IllegalArgumentException("If multiple tables will be joined"
                    + " the table schema for the additional tables (besides the base document table which should be configured using the database connector configuration) must be specified."));
        }
    }
    private void logConfigurationState() {
        log.info("Subset table {} will be reset upon pipeline start: {}", tableName, resetTable);
        if (log.isInfoEnabled())
            log.info("Names of additional tables to join: {}", StringUtils.join(additionalTableNames, ", "));
    }

    /**
     * Checks whether the given additional tables exist. If not, it is checked if
     * the table names contain dots which are reserved for schema qualification in
     * Postgres. It is tried again to find the tables with underscores ('_'), then.
     * The tables are also searched in the data schema. When the names contain dots,
     * the substring up to the first dot is tried as schema qualification before
     * prepending the data schema.
     */
    private void checkAndAdjustAdditionalTables() {
        List<String> foundTables = new ArrayList<String>();
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
        tables = foundTables.toArray(new String[foundTables.size()]);
        // -1 because here we also have added the document table which is not an
        // additional table but the main table!
        numAdditionalTables = tables.length - 1;
    }


    /**
     * Convenience method for extending classes.
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
}
