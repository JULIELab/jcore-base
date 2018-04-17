package de.julielab.jcore.reader.db;

import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class DBReaderBase extends JCasCollectionReader_ImplBase {

    public static final String PARAM_DB_DRIVER = "DBDriver";
    public static final String PARAM_BATCH_SIZE = "BatchSize";
    /**
     * String parameter. Determines the table from which rows are read and returned.
     * Both subset and data tables are allowed. For data tables, an optional 'where'
     * condition can be specified, restricting the rows to be returned. Note that
     * only reading from subset tables works correctly for concurrent access of
     * multiple readers (for data tables each reader will return the whole table).
     */
    public static final String PARAM_TABLE = "Table";
    /**
     * String parameter representing a long value. If not null, only documents with
     * a timestamp newer then the passed value will be processed.
     */
    public static final String PARAM_TIMESTAMP = "Timestamp";
    /**
     * Boolean parameter. Determines whether to return random samples of unprocessed
     * documents rather than proceeding sequentially. This parameter is defined for
     * subset reading only.
     */
    public static final String PARAM_SELECTION_ORDER = "SelectionOrder";
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
     * String parameter. Used only when reading directly from data tables. Only rows
     * are returned which satisfy the specified 'where' clause. If empty or set to
     * <code>null</code>, all rows are returned.
     */
    public static final String PARAM_WHERE_CONDITION = "WhereCondition";
    /**
     * Integer parameter. Determines the maximum amount of documents being read by
     * this reader. The reader will also not mark more documents to be in process as
     * specified with this parameter.
     */
    public static final String PARAM_LIMIT = "Limit";
    /**
     * Constant denoting the name of the external dependency representing the
     * configuration file for the DataBaseConnector.<br>
     * The name of the resource is assured by convention only as alternative names
     * are not reject from the descriptor when entering them manually.
     */
    public static final String PARAM_COSTOSYS_CONFIG_NAME = "CostosysConfigFile";


    private static final Logger log = LoggerFactory.getLogger(DBReaderBase.class);
    /**
     * Default size of document batches fetched from the database. The default is
     * {@value #DEFAULT_BATCH_SIZE}.
     */
    private static final String DEFAULT_BATCH_SIZE = "50";

    @ConfigurationParameter(name = PARAM_BATCH_SIZE, defaultValue = DEFAULT_BATCH_SIZE)
    protected int batchSize;
    /**
     * Currently unused because the Hikari JDBC library should recognize the correct
     * driver. However, there seem to be cases where this doesn't work (HSQLDB). So
     * we keep the parameter for later. When this issue comes up, the driver would
     * have to be set manually. This isn't done right now.
     */
    @ConfigurationParameter(name = PARAM_DB_DRIVER, mandatory = false)
    protected String driver;
    @ConfigurationParameter(name = PARAM_TABLE, mandatory = true)
    protected String tableName;
    @ConfigurationParameter(name = PARAM_SELECTION_ORDER, defaultValue = "", mandatory = false)
    protected String selectionOrder;
    @ConfigurationParameter(name = PARAM_FETCH_IDS_PROACTIVELY, defaultValue = "true")
    protected Boolean fetchIdsProactively;
    @ConfigurationParameter(name = PARAM_WHERE_CONDITION, mandatory = false)
    protected String whereCondition;
    @ConfigurationParameter(name = PARAM_LIMIT, mandatory = false)
    protected Integer limitParameter;


    protected volatile int numberFetchedDocIDs = 0;
    protected String[] tables;
    protected boolean joinTables = false;
    protected DataBaseConnector dbc;
    protected boolean hasNext;
    protected String dataTable;
    protected Boolean readDataTable = false;
    protected int totalDocumentCount;
    protected int processedDocuments = 0;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG_NAME, mandatory = true)
    String dbcConfig;



    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);


        driver = (String) getConfigParameterValue(PARAM_DB_DRIVER);
        Integer batchSize = (Integer) getConfigParameterValue(PARAM_BATCH_SIZE);
        tableName = (String) getConfigParameterValue(PARAM_TABLE);
        selectionOrder = (String) getConfigParameterValue(PARAM_SELECTION_ORDER);
        Boolean fetchIdsProactively = (Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY);
        whereCondition = (String) getConfigParameterValue(PARAM_WHERE_CONDITION);
        limitParameter = (Integer) getConfigParameterValue(PARAM_LIMIT);

        if (batchSize == null)
            batchSize = Integer.parseInt(DEFAULT_BATCH_SIZE);
        this.batchSize = batchSize;
        if (fetchIdsProactively == null)
            fetchIdsProactively = true;
        this.fetchIdsProactively = fetchIdsProactively;

        dbcConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);

        checkParameters();

        InputStream is = null;
        is = getClass().getResourceAsStream(dbcConfig.startsWith("/") ? dbcConfig : "/" + dbcConfig);
        if (is == null && dbcConfig != null && dbcConfig.length() > 0) {
            try {
                is = new FileInputStream(dbcConfig);
            } catch (FileNotFoundException e) {
                log.error("File '{}' was not found.", dbcConfig);
                throw new ResourceInitializationException(e);
            }
        }

        dbc = new DataBaseConnector(is, batchSize);

        // Check whether the table we are supposed to read from actually exists.
        if (!dbc.tableExists(tableName)) {
            throw new ResourceInitializationException(
                    new IllegalArgumentException("The configured table \"" + tableName + "\" does not exist."));
        }

        logConfigurationState();
    }

    /**
     * This method checks whether the required parameters are set to meaningful
     * values and throws an IllegalArgumentException when not.
     *
     * @throws ResourceInitializationException
     */
    private void checkParameters() throws ResourceInitializationException {
        if (tableName == null || tableName.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_TABLE});
        }
        if (dbcConfig == null || dbcConfig.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_COSTOSYS_CONFIG_NAME});
        }
    }

    private void logConfigurationState() {
        log.info("TableName is: \"{}\"; referenced data table name is: \"{}\"", tableName, dataTable);
        log.info("BatchSize is set to {}.", batchSize);
    }

    protected int unprocessedDocumentCount() {
        int unprocessed = -1;
        if (readDataTable) {
            unprocessed = totalDocumentCount - processedDocuments;
        } else
            unprocessed = dbc.countUnprocessed(tableName);
        return unprocessed;
    }
}
