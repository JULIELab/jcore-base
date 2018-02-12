 # JCoRe File Collection Reader
 
 JCoRe File Reader for reading in text files.  

**Descriptor Path**:
```
de.julielab.jcore.reader.file.desc.jcore-file-reader
```

### Objective
This is a reader for reading in text files, providing them to UIMA for further processing.

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDirectory | String | yes | no | Directory where the text files reside. |
| UseFilenameAsDocId | Boolean | no | no | If this is set to true, the document name (without extension) is used as document id. |
| PublicationDatesAsFile | String | no | no | A file that maps document ids to publication dates |
| ReadSubDirs | Boolean | no | no | If this is set to true, all subdirs of the InputDirectory are read. |
| FileNameSplitUnderscore | Boolean | no | no | Only used in conjunction with "`UseFilenameAsDocId`": If this is set to true, the split to determine the filename will also be done on underscores ("`_`"). |
| AllowedFileExtensions | String | no | yes | A list of file extensions that should be read. If empty, all files are read. |
| OriginalFolder | String | no | no | Path to the folder where the "original" files reside. [1] |
| OriginalFileExt | String | no | no | File extension of the "original" files [1] |
| SentencePerLine | Boolean | no | no | If true, `Sentence` annotations are stored in the `CAS` according a "one line one sentence" format. [1] |
| TokenByToken | Boolean | no | no | If true, `Token` annotations are stored in the `CAS`, where every whitespace separated "entity" in the document is one token. [1] |

[1] The last four parameters (`OriginalFolder`, `OriginalFileExt`, `SentencePerLine`, `TokenByToken`) are best used in conjunction with each other. For instance, you have documents that are free text and others that are basically the same but structure the text in such a way that sentences have each their own line and tokens are separated by whitespace. You don't want the `document text` in the `CAS` to be structured like the latter two but rather like in the "original" text file. That's where you should specify the aforementioned parameters accordingly.

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| InputDirectory | valid Path to the files to read in | `data/files/` |
| UseFilenameAsDocId | boolean Variable | `false` |
| PublicationDatesAsFile | valid Path to the ACE files | `data/publicationDates` |
| ReadSubDirs | boolean Variable | `false` |
| FileNameSplitUnderscore | boolean Variable | `false` |
| AllowedFileExtensions | String (Multi) | `empty` |
| OriginalFolder | valid path to "original" files | `none` |
| OriginalFileExt | string of file extension | `txt` |
| SentencePerLine | boolean Variable | `false` |
| TokenByToken | boolean Variable | `false` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Date |  | `+` |
| de.julielab.jcore.types.pubmed.Header |  | `+` |
| de.julielab.jcore.types.Sentence |  | `+` |
| de.julielab.jcore.types.Token |  | `+` |
