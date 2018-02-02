# JCoRe XMI Database Reader

**Descriptor Path**:
```
de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer
```

### Objective
The default UIMA CAS serialization results in an XML format called XMI.
The `jcore-xmi-splitter` project is able to extract portions from XMI data that represent annotations of the CAS document text. It then can assemble valid XMI data from multiple such annotation portions together with the corresponding base document, i.e. the document without annotations, XMI data.
This component employs this splitting step to create portions of XMI annotation data for storage in the document database across different tables. It is also possible to write complete XMI data without the splitting step (see the <code>StoreEntireXmiData</code> configuration parameter).

### Requirements and Dependencies
This component requires a PostgreSQL database set up via the JeDIS project to write (split) XMI data to.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| jedisConfigFile | String | true | false |  The JeDIS configuration.   |
| PerformGZIP | Boolean | false | false |  Whether the written data should be compressed using GZIP. This parameter interacts with the table schema definition of JeDIS. In JeDIS, text columns can be configured to be GZIP-compressed. Then, JeDIS will automatically compress stored data. Thus, this parameter defaults to <code>false</code>.   |
| StoreEntireXmiData | Boolean | false | false | Whether to store the complete XMI CAS data instead of splitting it across multiple tables.    |
| DocumentTable | String | true | false | The table to store the base document (see the <code>BaseDocumentAnnotationTypes</code> parameter) or the complete XMI document (when <code>StoreEntireXmiData</code> is set to <code>true</code>).   |
| AnnotationsToStore | String | false | true | A list of fully qualified type names that should be split from the XMI data and stored in tables of their own.    |
| StoreRecursively | Boolean | false | false | Whether also to store types embedded in other types, even when they are not listed in <code>AnnotationsToStore</code>. For example, JCoRe `Token` annotations have a feature for their part of speech. The PoS will be saved together with the tokens if this parameter is set to <code>true</code>.   |
| AnnotationTableSchema | String | false | false | The JeDIS table schema to apply for the annotation tables.    |
| UpdateMode | Boolean | false | false | This parameter is required to be set to <code>true</code> if document or annotation data will be overridden. Otherwise a duplicate primary key error will be raised.    |
| StoreBaseDocument | Boolean | false | false | Whether to store the base document. This is not necessary when the base document is already present in the database and only annotations should be stored.    |
| BaseDocumentAnnotationTypes | String | false | true | This parameter defines what the base document is comprised of. The sofa data, i.e. the actual document text, is always included. Each annotation given here will also be stored together with the document text. Typically, basic document meta data like the <code>Header</code>, MeSH headings and other data that was delivered with the original document is added to the base document. Only required if the base document should be stored.    |
| DeleteObsoleteAnnotations | Boolean | false | false | Boolean parameter that indicates whether annotations, that have become obsolete by updating referenced annotations, should be deleted from their table. This is the case when, for example, tokens and their PoS tags are stored in separate tables and then the tokens are updated. This can help to avoid errors when there is a chance that the obsolete annotations could be read later, leading to invalid XMI due to references to invalid XMI IDs. However, when those referenced annotations are also updated, the overhead of deleting them would not be necessary.    |
| IncreasedAttributeSize | Integer | false | false | Low-level XML parser setting. With large documents, e.g. scientific full texts, it happens that an error occurs about too large attribute values. This parameter can be adjusted to avoid this error. Defaults to 25000000 bytes (25MiB which should be enough for most purposes).    |


**2. Capabilities**

All types are acceptable.


### Reference
None.
