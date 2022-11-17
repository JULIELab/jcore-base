# JCoRe XMI Database Reader

**Descriptor Paths**:
```
de.julielab.jcore.reader.xmi.desc.jcore-xmi-db-reader
de.julielab.jcore.reader.xmi.desc.jcore-xmi-db-multiplier-reader
de.julielab.jcore.reader.xmi.desc.jcore-xmi-db-multiplier
```

### Objective
The default UIMA CAS serialization results in an XML format called XMI.
The `jcore-xmi-splitter` project is able to extract portions from XMI data that represent annotations of the CAS document
 text. It then can assemble valid XMI data from multiple such annotation portions together with the corresponding base document, i.e. the document without annotations, XMI data.
This component employs this assembly step to read XMI annotation data, that is stored in the document database across 
different tables, back into a CAS instance. It is also possible to read complete XMI data without the assembly step (see the <code>ReadsBaseDocument</code> configuration parameter).

### Functionality
This component requires a PostgreSQL database set up via the CoStoSys project and populated by the [jcore-xmi-db-writer](https://github.com/JULIELab/jcore-projects/tree/2.3.0-SNAPSHOT/jcore-pubmed-db-writer)
 component.
The CoStoSys configuration used for the reader can just be the exact same file the has been used for the 
`jcore-xmi-db-writer`. The table schema, specified in the CoStoSys configuration, must exhibit the correct primary keys
for the read table. All other columns are ignored by this ready because all XMI data follows a specific XMI table
layout. The primary key is determined when creating the input table by the `jcore-xmi-db-writer`. When the original
data source is PubMed, the typical workflow is:
1. Import PubMed XML data into a PubMed XML data table with CoStoSys. This step requires an XML table schema that already defines the primary key of each document.
2. Reading of PubMed XML data via the [jcore-pubmed-db-reader](https://github.com/JULIELab/jcore-projects/tree/2.3.0-SNAPSHOT/jcore-pubmed-db-reader) that uses the same CoStoSys configuration that was employed for step 1 and processing the documents with analysis engines.
3. Writing the base document table with the `jcore-xmi-db-writer`. This is a new data table that will be explained below. Also, annotations are stored separately. Again, the same CoStoSys configuration is used.
4. Reading the new base document table and the desired annotations with this component, the `jcore-xmi-db-reader`, the also uses the original CoStoSys configuration.
5. Either performing more document analysis and writing the annotation data back into the database using the `jcore-xmi-db-writer` **without** storing the base document again, or using another consumer to output analysis results for further processing.

Starting with step 3, the original PubMed XML data table is no longer required if the dataset is static and won't change
and the original data is not used otherwise.
The `jcore-xmi-db-writer` serializes the UIMA CAS object into XMI which is an XML format to store an UIMA annotation graph.
From this annotation graph, the part holding the document text is extracted and stored as the *base document* in a
new table. The structure of this table is *automatically* derived from the table schema that was specified for the original
PubMed XML data. This is possible because only the primary key to identify the documents is required. The XMI data is
stored in fixed columns on which the user has no influence.

The annotations are stored in a similar way by the `jcore-xmi-db-writer`. The `jcore-xmi-db-reader` has the capability
to read both the base document and the annotations and assemble a new, valid XMI document from them. To do this, the base
document table or, preferred, a subset referencing the base document table must be set for the `Table` parameter
of the reader. To read specific annotations that have been stored in the database, set the qualified Java names of their
respective UIMA annotation types to the `AdditionalTables` parameter. The names are automatically mapped to valid
table names.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| DoGzip | Boolean | true | false | Whether or not the XMI data in the database is compressed. This parameter is also set by the jcore-xmi-db-writer and determines if the data will be stored compressed or not. |
| ReadsBaseDocument | Boolean | true | false | Indicates if this reader reads segmented annotation data. If set to false, the XMI data is expected to represent complete annotated documents. If it is set to true, a segmented annotation graph is expected and the table given with the 'Table' parameter will contain the document text together with some basic annotations. What exactly is stored in which manner is determined by the jcore-xmi-db-consumer used to write the data into the database. |
| StoreMaxXmiId | Boolean | false | false | This parameter is required to be set to true, if this reader is contained in a pipeline that also contains a jcore-xmi-db-writer andt he writer will segment the CAS annotation graph and store only parts of it. Then, it is important to keep track of the free XMI element IDs that may be assigned to new annotation elements to avoid ID clashes when assembling an XMI document from separately stored annotation graph segments. |
| IncreasedAttributeSize | Integer | false | false | Maxmimum XML attribute size in bytes. Since the CAS document text is stored as an XMI attribute, it might happen for large documents that there is an error because the maximum attribute size is exceeded. This parameter allows to specify the maxmimum  attribute size in order to avoid such errors. Should only be set if required. |
| XercesAttributeBufferSize | Integer | false | false | Initial XML parser buffer size in bytes. For large documents, it can happen that XMI parsing is extremely slow. By employing monitoring tools like the jconsole or (j)visualvm, the hot spots of work can be identified. If one of those is the XML attribute buffer resizing, this parameter should be set to a size that makes buffer resizing unnecessary. |
| ResetTable | Boolean | false | false | If set to true and the parameter 'Table' is set to a subset table, the subset table will be reset atthe initialization of the reader to be ready for processing of the whole subset. Do not use when multiple readers read the same subset table. |
| FetchIdsProactively | Boolean | true | false | If set to true and when reading from a subset table, batches of document IDs will be retrieved in a background thread while the previous batch is already in process. This is meant to minimize waiting time for the database. Deactivate this feature if you encounter issues with database connections. |
| AdditionalTables | String | false | true | An array of qualified UIMA type names. The type names will be transformed into valid PostgreSQL table names by replacing dots with underscores. The resulting table names will be resolved against the active data postgres schema configured in the CoStoSys configuration file. The additional tables will be joined to the data table using the primary keys of the queried documents, allowing to retrieve document text data together with the selected annotations. |
| BatchSize | Integer | false | false | Determines the number of documents fetched from the database with each database request. Typical values range between 50 and 500. |
| Table | String | true | false | The data or subset database table to read from. The name will be resolved against the active Postgres schema defined in the CoStoSys configuration file.However, if the name contains a schema qualification (i.e. 'schemaname.tablename), the configuration file will be ignored in this point. |
| SelectionOrder | String | false | false | WARNING: Potential SQL injection vulnerability. Do not let unknown users interact with your database with this component. An SQL ORDER clause specifying in which order the documents in the target database table should be processed. Only the clause itself must be specified, the ORDER keyword is automatically added. |
| CostosysConfigFile | String | true | false | File path or classpath resource location to the CoStoSys XML configuration. This configuration must specify the table schema of the table referred to by the 'Table' parameter as active table schema. The active table schema is always the schema of the data table that is either queried directly for documents or, if 'tableName' points to a subset table, indirectly through the subset table. Make also sure that the active database connection in the configuration points to the correct database. |

**2. Capabilities**

The input is the assembled XMI data that could contain any possible type.


### Reference
None.
