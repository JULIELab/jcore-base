# JCoRe ElasticSearchConsumer

**Descriptor Path**:
```
.jcore-elasticsearch-consumer.src.main.resources.de.julielab.jcore.consumer.es.desc.jcore-elasticsearch-consumer
.jcore-elasticsearch-consumer.src.main.resources.de.julielab.jcore.consumer.es.desc.jcore-json-consumer
```

### Objective
This project offers a mechanism to transform UIMA CAS into a JSON format that is accepted by ElasticSearch. The JSON can be written to disc or be sent to an ElasticSearch cluster, depending on the exact component being used. Also, and more importantly, the project is able to produce a special document field value format called ''preanalyzed'' field values. It is possible to exactly specify ElasticSearch/Lucene index terms by offset, position increment, flags, payload and the actual term. To make use of this format, the [elasticsearch-preanalyzed-mapper-plugin], developed at the JULIE Lab, is required.


### Requirements and Dependencies
Apache HTTP Components for the ElasticSearch HTTP connection.
Gson for JSON creation.
Guava for its Multimaps.
ICU4j for token filters.
Lucene Analyzers for some token filters.

### Using the CR - Descriptor Configuration

**1. Parameters**
Common parameters:

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| FieldGenerators      |       String      |       false      |       true      |       An array of qualified Java class names. Each enumerated class must implement the FieldGenerator interface and is delivered by the user. These classes will be applied to the consumed CAS and populate Document instances with fields and thus determine the structure and content of the output documents. The field values are derived from CAS data. FieldGenerators always populate a single Document instance with fields. If multiple documents must be created for each CAS, refer to the DocumentGenerators parameter. |
| DocumentGenerators      |       String      |       false      |       true      |       An array of qualified Java class names. Each enumerated class must extend the abstract DocumentGenerator class and is delivered by the user. Unlike FieldGenerator classes, DocumentGenerators put out whole Document instances instead of only populating a single Document with fields. This is required when multiple ElasticSearch documents should be created from a single CAS. When only the creation of a single document with a range of fields is required, leave this parameter empty and refer to the FieldGenerators parameter. |
| FilterBoards      |       String      |       false      |       true      |       An array of qualified Java names. Each enumerated class must extend the FilterBoard class and is delivered by the user. FieldGenerators and DocumentGenerators may make use of several filters that a applied to tokens derived from UIMA annotations. Often, the same kind of filter is required across differnet fields (e.g. all full text fields will undergo a very similar text transformation process to create index tokens). To centralize the creation and management of the filters, one or multiple filter boards may be created. The filter boards are passed to each field and document generator. Also, the filter boards feature an annotation-driven access to the external resource mechanism used by UIMA for shared resources. Using shared resources helps to reduce memory consumption and the annotation-driven approach facilitates configuration. |
| IdField      |       String      |       false      |       false      |       The name of the field that contains the document ID. If not set, the document ID will be read from the Header annotation of the CAS. If both methods to obtain a document ID fail, an exception will be raised. |
| IdPrefix      |       String      |       false      |       false      |       A string that will be prepended to each document ID. |

Parameters specific to the ElasticSearch consumer:

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| urls      |       String      |       true      |       true      |       A list of URLs pointing to different nodes of the ElasticSearch cluster, e.g. http://localhost:9300/. Documents will be sent bulk-wise to the nodes in a round-robin fashion. |
| indexName      |       String      |       true      |       false      |       The ElasticSearch index name to send the created documents to. |
| type      |       String      |       true      |       false      |       The index type the generated documents should have. The types are removed from ElasticSearch with version 7 so this parameter is set to have the same value for all documents. |


Parameters specific to the JSON consumer:

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| OutputDir      |       String      |       true      |       false      |       The directory where the JSON files will be put to. If does not exist, it will be created, including all parent directories. |
| GZIP      |       Boolean      |       false      |       false      |        |


**2. Capabilities**

Accepts all possible types.


### Reference
None.
