package de.julielab.jcore.reader.db;

import de.julielab.costosys.dbconnection.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Optional;

import static de.julielab.jcore.reader.db.TableReaderConstants.*;

public abstract class DBReaderBase extends JCasCollectionReader_ImplBase {
    public static final String PARAM_TABLE = TableReaderConstants.PARAM_TABLE;
    public static final String PARAM_COSTOSYS_CONFIG_NAME = TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME;
    public static final String PARAM_BATCH_SIZE = TableReaderConstants.PARAM_BATCH_SIZE;
    private static final Logger log = LoggerFactory.getLogger(DBReaderBase.class);
    /**
     * Default size of document batches fetched from the database. The default is
     * {@value #DEFAULT_BATCH_SIZE}.
     */
    private static final String DEFAULT_BATCH_SIZE = "100";

    @ConfigurationParameter(name = PARAM_BATCH_SIZE, defaultValue = DEFAULT_BATCH_SIZE, mandatory = false)
    protected int batchSize;

    @ConfigurationParameter(name = PARAM_DB_DRIVER, mandatory = false, description = "Currently unused because the " +
            "Hikari JDBC library should recognize the correct driver. However, there seem to be cases where this " +
            "doesn't work (HSQLDB). So we keep the parameter for later. When this issue comes up, the driver would " +
            "have to be set manually. This isn't done right now.")
    protected String driver;
    @ConfigurationParameter(name = PARAM_TABLE, description = "The data or subset database table to read from. The " +
            "name will be resolved against the active Postgres schema defined in the CoStoSys configuration file." +
            "However, if the name contains a schema qualification (i.e. 'schemaname.tablename), the configuration " +
            "file will be ignored in this point.")
    protected String tableName;
    @ConfigurationParameter(name = PARAM_SELECTION_ORDER, defaultValue = "", mandatory = false, description =
            "WARNING: Potential SQL injection vulnerability. Do not let unknown users interact with your database " +
                    "with this component. An SQL " +
                    "ORDER clause specifying in which order the documents in the target database table should be processed. " +
                    "Only the clause itself must be specified, the ORDER keyword is automatically added.")
    protected String selectionOrder;

    @ConfigurationParameter(name = PARAM_WHERE_CONDITION, mandatory = false,
            description = "WARNING: Potential SQL injection vulnerability. Do not let unknown users interact with your " +
                    "database with this component. Only used when reading data tables directly. No effect when the " +
                    "'tableName' parameter specifies a subset table. The parameter value should be an SQL WHERE clause " +
                    "restricting the documents to be read. Only the clause itself must be specified, the WHERE keyword " +
                    "is added automatically.")
    protected String whereCondition;
    @ConfigurationParameter(name = PARAM_LIMIT, mandatory = false)
    protected Integer limitParameter;


    protected volatile int numberFetchedDocIDs = 0;

    protected boolean joinTables = false;
    protected DataBaseConnector dbc;
    protected boolean hasNext;
    protected int totalDocumentCount;
    protected int processedDocuments = 0;
    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG_NAME, mandatory = true, description = "File path or classpath" +
            " resource location to the CoStoSys XML configuration. This configuration must specify the table schema " +
            "of the table referred to by the 'Table' parameter as active table schema. The active table schema " +
            "is always the schema of the data table that is either queried directly for documents or, if 'tableName' " +
            "points to a subset table, indirectly through the subset table. Make also sure that the active " +
            "database connection in the configuration points to the correct database.")
    protected String costosysConfig;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        driver = (String) getConfigParameterValue(PARAM_DB_DRIVER);
        batchSize = Optional.ofNullable((Integer) getConfigParameterValue(PARAM_BATCH_SIZE)).orElse(Integer.parseInt(DEFAULT_BATCH_SIZE));
        tableName = (String) getConfigParameterValue(PARAM_TABLE);
        selectionOrder = (String) getConfigParameterValue(PARAM_SELECTION_ORDER);
        whereCondition = (String) getConfigParameterValue(PARAM_WHERE_CONDITION);
        limitParameter = (Integer) getConfigParameterValue(PARAM_LIMIT);
        costosysConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);

        checkParameters();

        try {
            // It might happen that a subclass has already initialized the DBC
            if (dbc == null)
                dbc = new DataBaseConnector(costosysConfig);
            dbc.setQueryBatchSize(batchSize);
            checkTableExists();
            logConfigurationState();
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        numberFetchedDocIDs = 0;
    }

    private void checkTableExists() throws ResourceInitializationException {
        // Check whether the table we are supposed to read from actually exists.
        if (!dbc.withConnectionQueryBoolean(c -> c.tableExists(tableName))) {
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
        if (costosysConfig == null || costosysConfig.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_COSTOSYS_CONFIG_NAME});
        }
    }

    private void logConfigurationState() {
        log.info("BatchSize is set to {}.", batchSize);
    }

}
