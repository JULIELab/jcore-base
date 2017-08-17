# JCoRe PMC Collection Reader
A Collection Reader that reads NXML files from PubMed Central (PMC) and converts them to CAS objects.

**Descriptor Path**:
```
de.julielab.jcore.reader.pmc.desc.jcore-pmc-reader
```

### Objective
The JULIE Lab PMC Reader is a UIMA Collection Reader (CR). TODO more information
### Requirements and Dependencies
TODO information
### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/reader/pmc/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| Input | String | true | false | The NXML file to read or a directory of NXML files to read. The read will also check subdirectories for NXML files. Each file that includes the string 'nxml' is deemed to be a PubMed Central NXML file to be read. |
| AlreadyRead | String | false | false | A file that contains a list of already read file names. Those will be skipped by the reader. While reading, the reader will append read files to this list. If it is not given, the file will not be maintained. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| Input | A valid path to an input file or directory | `data/example.nxml` |
| AlreadyRead | A valid path to a file that contains a list of already read file names | `TODO` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| - |  - | - |



### Reference
