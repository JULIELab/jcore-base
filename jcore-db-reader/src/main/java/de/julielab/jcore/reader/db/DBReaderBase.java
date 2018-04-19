package de.julielab.jcore.reader.db;

import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Optional;

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
     * Boolean parameter. Determines whether to return random samples of unprocessed
     * documents rather than proceeding sequentially. This parameter is defined for
     * subset reading only.
     */
    public static final String PARAM_SELECTION_ORDER = "SelectionOrder";


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

    @ConfigurationParameter(name = PARAM_WHERE_CONDITION, mandatory = false)
    protected String whereCondition;
    @ConfigurationParameter(name = PARAM_LIMIT, mandatory = false)
    protected Integer limitParameter;


    protected volatile int numberFetchedDocIDs = 0;

    protected boolean joinTables = false;
    protected DataBaseConnector dbc;
    protected boolean hasNext;
    protected int totalDocumentCount;
    protected int processedDocuments = 0;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG_NAME, mandatory = true)
    String dbcConfig;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        driver = (String) getConfigParameterValue(PARAM_DB_DRIVER);
        Integer batchSize = Optional.ofNullable((Integer) getConfigParameterValue(PARAM_BATCH_SIZE)).orElse(Integer.parseInt(DEFAULT_BATCH_SIZE));
        tableName = (String) getConfigParameterValue(PARAM_TABLE);
        selectionOrder = (String) getConfigParameterValue(PARAM_SELECTION_ORDER);
        whereCondition = (String) getConfigParameterValue(PARAM_WHERE_CONDITION);
        limitParameter = (Integer) getConfigParameterValue(PARAM_LIMIT);
        this.batchSize = batchSize;

        dbcConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);

        checkParameters();

        try {
            dbc = new DataBaseConnector(dbcConfig);
            dbc.setQueryBatchSize(batchSize);
            checkTableExists();
            logConfigurationState();
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
    }

    private void checkTableExists() throws ResourceInitializationException {
        // Check whether the table we are supposed to read from actually exists.
        if (!dbc.tableExists(tableName)) {
            throw new ResourceInitializationException(
                    new IllegalArgumentException("The configured table \"" + tableName + "\" does not exist."));
        }
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
        log.info("BatchSize is set to {}.", batchSize);
    }

}
