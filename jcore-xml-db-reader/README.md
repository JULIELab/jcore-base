# JCoRe XML Database Reader

**Descriptor Path**:
```
de.julielab.jcore.reader.xml.desc.jcore-xml-db.reader
```

### Objective
Reads XML document data from database tables. These tables should be created using the JeDIS tool.
The database then serves as a document storage and concurrent access management facility. Thus, this component can be used by multiple pipelines running concurrently while accessing the same table without reading documents twice. 

### Requirements and Dependencies

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| RowMapping | String | true | true | A list of mappings from retrieved database table fields into UIMA types and feature values. The exact format is explained below.    |
| MappingFile | String | true | false | A mapping file specifying how the document XML data should be used to populate the CAS. The reader uses the [jcore-xml-mapper] for this task.    |
| BatchSize | Integer | true | false | The number of documents (plus annotations) to be fetched from the database per request. Defaults to 50 (<code>jcore-db-reader</code> default).    |
| DBDriver | String | true | false | The database driver appropriate for the used DBMS. Defaults to <code>org.postgresql.Driver</code>. Is auto-detected if missing.    |
| Timestamp | String | true | false | A timestamp string in the appropriate DBMS format. If set, only subset rows newer than the given timestamp are read.   |
| Table | String | true | false | The JeDIS database subset table to read from. This subset table must reference an XMI data table.    |
| AdditionalTables | String | true | true | Additional tables to read. Must have the same primary key as the table given for the <code>Table</code> parameter. Which data exactly is returned is defined by the JeDIS data schema given in the <code>AdditionalTableSchema</code> parameter.    |
| AdditionalTableSchema | String | true | false | The JeDIS table schema for the additional tables. The schema defines which columns are joined to the primary XML data. The <code>RowMapping</code> can be used to add these data into the CAS    |
| RandomSelection | String | true | false | Orders the read documents randomly.   |
| FetchIdsProactively | Boolean | true | false |  When set to <code>true</code>, the next batch of documents will be fetched in a background thread from the database. Defaults to <code>true</code>.   |
| WhereCondition | String | true | false | An SQL WHERE condition string used for arbitrary restrictions on retrieved documents. Beware of SQL injections.    |
| Limit | Integer | true | false | A limit to the number of read documents. After the limit is hit, the reader will signal that there are no more documents, causing reading to stop.    |
| jedisConfigFile | String | true | false |   The JeDIS configuration.   |
| ResetTable | Boolean | true | false |   Whether the subset table, from which data is read, should be reset at initialization of the reader. Must not be set to <code>true</code> when multiple readers concurrently read from the same subset table.   |


**2. Capabilities**

The input could contain all possible types.


### Reference
None.
