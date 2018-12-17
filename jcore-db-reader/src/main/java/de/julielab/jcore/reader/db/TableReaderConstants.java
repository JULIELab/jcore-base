package de.julielab.jcore.reader.db;

public class TableReaderConstants {
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
}
