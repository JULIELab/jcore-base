 # JCoRe JULIE Lab Entity Evaluator Consumer
 
 JCoRe consumer to write an entity evaluation format, similar to the BioCreative II Gene Normalization format, that can be read by the JULIE Lab Entity Evaluator in the [jcore-dependencies](https://github.com/JULIELab/jcore-dependencies) repository.

**Descriptor Path**:
```
de.julielab.jcore.consumer.entityevaluator.desc.jcore-julielab-entity-evaluator-consumer
```

### Objective
This consumer writes a tab-separated file from desired entities in the CAS for evaluation purposes.

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the Consumer - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**
TODO
| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDirectory | String | yes | no | Directory where the text files reside. |
| UseFilenameAsDocId | Boolean | no | no | If this is set to true, the document name (without extension) is used as document id. |
| PublicationDatesAsFile | String | no | no | A file that maps document ids to publication dates |
| ReadSubDirs | Boolean | no | no | If this is set to true, all subdirs of the InputDirectory are read. |
| FileNameSplitUnderscore | Boolean | no | no | Only used in conjunction with "`UseFilenameAsDocId`": If this is set to true, the split to determine the filename will also be done on underscores ("`_`"). |

**2. Predefined Settings**
None.

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Annotation |  | `+` |
| de.julielab.jcore.types.pubmed.Header |  | `+` |
