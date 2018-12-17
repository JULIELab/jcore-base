# JCoRe Database Collection Reader
A Collection Reader that employs [the Corpus Storage System (CoStoSys)](https://github.com/JULIELab/costosys) to read
documents that are stored in a PostgreSQL database. The classes herein are are mostly of abstract nature. They are
extended by the [jcore-xml-db-reader](https://github.com/JULIELab/jcore-base/tree/master/jcore-xml-db-reader) and
[jcore-xmi-db-reader](https://github.com/JULIELab/jcore-base/tree/master/jcore-xmi-db-reader) projects.

**Descriptor Path**:
```
de.julielab.jcore.reader.db.desc.jcore-db-multiplier-reader.xml
```

### Objective
The database reader reads document data from database tables. It follows the CoStoSys idea of data tables and subset
tables. Data tables store the actual document data where subset tables are references to data tables. Subsets
may have a row for each document in a data table or only a subset (hence, the name) of these. They are used to
build smaller portions of a larger corpus and to track processing and to synchronize multiple readers. For this purpose
they have the columns `in_process`, `is_processed`, `has_errors`, `timestamp` and more. Refer to the CoStoSys
documentation for more information.

### Using the CR - Descriptor Configuration
This project has actually three reading components: the `DBReader`, the `DBMultiplierReader` and the `DBMultiplier`.
The `DBReader`

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |   Comment    |
|----------------|----------------|-----------|-------------|-------------|--------------|
| ResetTable | boolean | false | false | If set to true and the parameter 'Table' is set to a subset table, the subset table will be reset atthe initialization of the reader to be ready for processing of the whole subset. Do not use when multiple readers read the same subset table. | |
| Timestamp | string | false | false | PostgreSQL timestamp expression that is evaluated against the data table. The data table schema, which must be the active data table schema in the CoStoSys configuration as always, must specify a single timestamp field for this parameter to work. Only data rows with a timestamp value larger than the given timestamp expression will be processed. Note that when reading from a subset table, there may be subset rows indicated to be in process which are finally not read from the data table. This is an implementational shortcoming and might be addressed if respective feature requests are given through the JULIE Lab GitHub page or JCoRe issues. | |
| FetchIdsProactively | boolean | false | false | If set to true and when reading from a subset table, batches of document IDs will be retrieved in a background thread while the previous batch is already in process. This is meant to minimize waiting time for the database. Deactivate this feature if you encounter issues with databaase connections. | |
| AdditionalTables | string |false | true | An array of table names. By default, the table names will be resolved against the active data postgres schema configured in the CoStoSys configuration file. If a name is already schema qualified, i.e. contains a dot, the active data schema will be ignored. When reading documents from the document data table, the additional tables will be joined onto the data table using the primary keys of the queried documents. Using the table schema for the additional documents defined by the 'AdditionalTableSchema' parameter, the columns that are marked as 'retrieve=true' in the table schema, are returned together with the main document data. This mechanism is most prominently used to retrieve annotation table data together with the original document text in XMI format for the JeDIS system. | |
| AdditionalTableSchemas | string | false | true | The table schemas that corresponds to the additional tables given with the 'AdditionalTables' parameter. If only one schema name is given, that schema must apply to all additional tables. | |
| BatchSize | integer | false | false | Number of table rows read with each database request. | |
| DBDriver | string | false | false | Currently unused because the Hikari JDBC library should recognize the correct driver. However, there seem to be cases where this doesn't work (HSQLDB). So we keep the parameter for later. When this issue comes up, the driver would have to be set manually. This isn't done right now. | |
| Table | string | true | false | The data or subset database table to read from. The name will be resolved against the active Postgres schema defined in the CoStoSys configuration file.However, if the name contains a schema qualification (i.e. 'schemaname.tablename), the configuration file will be ignored in this point. | |
| SelectionOrder | string | false | false | WARNING: Potential SQL injection vulnerability. Do not let unknown users interact with your database with this component. An SQL ORDER clause specifying in which order the documents in the target database table should be processed. Only the clause itself must be specified, the ORDER keyword is automatically added. | |
| WhereCondition | string | false | false | WARNING: Potential SQL injection vulnerability. Do not let unknown users interact with your database with this component. Only used when reading data tables directly. No effect when the 'tableName' parameter specifies a subset table. The parameter value should be an SQL WHERE clause restricting the documents to be read. Only the clause itself must be specified, the WHERE keyword is added automatically. | |
| Limit | integer | false | false | The maximum number of documents read from the table. Only used for data tables. | |
| CostosysConfigFile | string | true | false | File path or classpath resource location to the CoStoSys XML configuration. This configuration must specify the table schema of the table referred to by the 'Table' parameter as active table schema. The active table schema is always the schema of the data table that is either queried directly for documents or, if 'tableName' points to a subset table, indirectly through the subset table. Make also sure that the active database connection in the configuration points to the correct database. | |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax |
|----------------|------------------|
| inputDirectory | valid Path to the ACE files |
| generateJcoreTypes| boolean Variable |

**3. Capabilities**

