# JCoRe IEXML Collection Reader
A Collection Reader for IEXML files as used in the mantra project/challenge  .

**Descriptor Path**:
```
de.julielab.jcore.reader.iexml.desc.jcore-iexml-reader
```

### Objective
The JULIE Lab IEXMLFileReader is a UIMA Collection Reader (CR). It reads from the IEXML files (a specific, task-dependent file format as used in the mantra challenge) and converts it to types defined in the UIMA type system that we provide as well.


### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputFile | String | yes | no | Path to a Mantra file |
| maxRecordsCount| Integer | no | no |  |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputFile | valid Path to a specific IEXML file | `data/mantra-input.xml` |
| maxRecordsCount| Integer for how many records should be maximally processed | `1000` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.mantra.Unit |  | `+` |
| de.julielab.jcore.types.mantra.Entity |  | `+` |
| de.julielab.jcore.types.mantra.DocumentInformation |  | `+` |
