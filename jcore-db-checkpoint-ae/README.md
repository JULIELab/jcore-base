# JCoRe Database Checkpoint AE

**Descriptor Path**:
```
de.julielab.desc.jcore-db-checkpoint-ae
```

This is a JeDiS[1] component. It can be used to set the 'last component' column in a subset table. This help to keep track of the pipeline status.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| CheckpointName | String | true | false | String parameter. A name that identifies this checkpoint in the database. |
| IndicateFinished | Boolean | false | false | Whether or not the checkpoint should mark the end of processing of the pipeline. If set to true, this component will not only set its name as checkpoint in the subset table but also set the 'is processed' flag to true and the 'is in process' flag to false. |
| CostosysConfigFile | String | true | false | File path or classpath resource location of a Corpus Storage System (CoStoSys) configuration file. This file specifies the database to write the XMI data into and the data table schema. This schema must at least define the primary key columns that the storage tables should have for each document. The primary key is currently just the document ID. Thus, at the moment, primary keys can only consist of a single element when using this component. This is a shortcoming of this specific component and must be changed here, if necessary. |
| WriteBatchSize | Integer | false | false | The number of processed CASes after which the checkpoint should be written into the database. Defaults to 50. |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| CheckpointName | string | `AfterParsing` |
| IndicateFinished | boolean | `false` |
| CostosysConfigFile | File or classpath address | `config/costosys.xml` |
| WriteBatchSize | integer | `100` |


[1] Faessler, Erik, & Hahn, Udo (2018). Annotation data management with JeDIS. in: DocEng '18 – Proceedings of the 18th ACM Symposium on Document Engineering 2018. Halifax, Nova Scotia, Canada, August 28-31, 2018, #42.
