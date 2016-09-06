 JCoRe File Reader for reading in text files.  

**Descriptor Path**:
```
Path
```

### Objective
 This is a reader for reading in text files, providing them to UIMA for further processing.

### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDirectory | String | yes | no | Directory where the text files reside. |
| UseFilenameAsDocId | Boolean | no | no | If this is set to true, the document name (without extension) is used as document id. |
| PublicationDatesAsFile | String | no | no | A file that maps document ids to publication dates |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| InputDirectory | valid Path to the files to read in | `data/files/` |
| UseFilenameAsDocId | boolean Variable | `false` |
| PublicationDatesAsFile | valid Path to the ACE files | `data/publicationDates` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Date |  | `+` |
| de.julielab.jcore.types.pubmed.Header |  | `+` |
