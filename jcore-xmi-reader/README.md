# JCoRe XMI Collection Reader
A CollectionReader which reads CAS data stored as XMI files from the file system. 

**Descriptor Path**:
```
de.julielab.jcore.reader.xmi.jcore-xmi-reader
```
### Objective
The JULIE Lab XMI Reader is a UIMA Collection Reader (CR) for reading XMI files, providing them to UIMA for further processing. The reader grounds on IBM's XmiCollectionReader delivered with older versions of UIMA and has been extended by the Julie Lab team at the University of Jena.
This XMI reader is capable of reading (g)zipped XMI files and is able to recursively search subdirectories of a delivered root directory for XMI files.


### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).


### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDir| String | yes | no | File path to the directory to read XMI files from. |
| SearchRecursively | Boolean | no | no | If set to true, also searches subdirectories of the input directory for XMI files to read. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| InputDir | valid Path to a specific directory | `data/example.xmi` |
| SearchRecursively | Boolean | `false` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
|- | - | - |

