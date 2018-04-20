package de.julielab.jcore.reader.db;

public class SubsetReaderConstants {
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
    public static final String PARAM_ADDITIONAL_TABLE_SCHEMA = "AdditionalTableSchemas";
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
     * a dataTimestamp newer then the passed value will be processed.
     */
    public static final String PARAM_DATA_TIMESTAMP = "Timestamp";
}
