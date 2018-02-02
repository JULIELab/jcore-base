# JCoRe XMI Database Reader

**Descriptor Path**:
```
de.julielab.jcore.reader.xmi.desc.jcore-xmi-db-reader
```

### Objective
The default UIMA CAS serialization results in an XML format called XMI.
The `jcore-xmi-splitter` project is able to extract portions from XMI data that represent annotations of the CAS document text. It then can assemble valid XMI data from multiple such annotation portions together with the corresponding base document, i.e. the document without annotations, XMI data.
This component employs this assembly step to read XMI annotation data, that is stored in the document database across different tables, back into a CAS instance. It is also possible to read complete XMI data without the assembly step (see the <code>ReadsBaseDocument</code> configuration parameter).

### Requirements and Dependencies
This component requires a PostgreSQL database managed via the JeDIS project and populated by the `jcore-xmi-db-writer` component.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| Table | String | true | false |  The JeDIS database subset table to read from. This subset table must reference an XMI data table.   |
| jedisConfigFile | String | true | false |  The JeDIS configuration.   |
| PerformGUNZIP | Boolean | false | false |  Whether the incoming data is in GZIP format. This parameter interacts with the table schema definition of JeDIS. In JeDIS, text columns can be configured to be GZIP-compressed. Then, JeDIS will automatically decompress the data upon retrieval. Thus, this parameter defaults to <code>false</code>.   |
| AdditionalTables | String | false | true | A list of database table names that contain annotations that should be added to the CAS.    |
| AdditionalTableSchema | String | false | false | The schema of the annotation tables. Defaults to <code>xmi_annotation_gzip</code>    |
| StoreMaxXmiId | Boolean | false | false | The maximum XMI ID refers to the element IDs in the XMI serialization format. Since the document and its annotations are distributed across multiple tables, the information which ID range is still free must be maintained separately. This parameter should be set to <code>true</code> if the pipeline contains `jcore-xmi-db-writer` components because those must create valid XMI IDs. Otherwise, the parameter may be set fo <code>false</code>.   |
| BatchSize | Integer | false | false | The number of documents (plus annotations) to be fetched from the database per request. Defaults to 50 (<code>jcore-db-reader</code> default).    |
| LogFinalXmi | Boolean | false | false | Whether or not to write the assembled XMI data to the logger. Only for debug purposes.   |
| ResetTable | Boolean | false | false | Whether the subset table, from which data is read, should be reset at initialization of the reader. Must not be set to <code>true</code> when multiple readers concurrently read from the same subset table.    |
| FetchIdsProactively | Boolean | false | false | When set to <code>true</code>, the next batch of documents will be fetched in a background thread from the database. Defaults to <code>true</code>.    |
| ReadsBaseDocument | Boolean | false | false | Whether this component reads split XMI from multiple tables (or only the base document) or complete, self sufficient XMI files. This is important even when no annotations are loaded since the base document is not stored as valid XMI data itself and required additional data to be read from the database.  |
| IncreasedAttributeSize | Integer | false | false | Low-level XML parser setting. With large documents, e.g. scientific full texts, it happens that an error occurs about too large attribute values. This parameter can be adjusted to avoid this error. Defaults to 25000000 bytes (25MiB which should be enough for most purposes). |
| XercesAttributeBufferSize | Integer | false | false | Low-level XML parser setting. Required the JULIE Lab version of the Xerces Parser on the classpath (defaut for this component). For large documents, XML parsing might be extremely slow due to internal buffer resizing. This parameter forces the initial buffer size to be set to the given value, reducing the need for slow resizing. To know if this issue is occurring, the usage of a profiling tool like (j)VisualVM is very helpful. It shows if significant time is spent in the buffer resizing method of the XML parser during program execution.  |


**2. Capabilities**

The input is the assembled XMI data that could contain any possible type.


### Reference
None.
