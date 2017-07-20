# JCoRe IEXML Consumer
Consumer that generates stand-off IEXML files as used in the mantra project/challenge  

**Descriptor Path**:
```
de.julielab.jcore.consumer.iexml.desc.jcore-iexml-consumer
```

### Objective
This consumer takes the annotations specified in **Capabilities** and outputs IEXML files for each document to a specified `outputFile`.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CC - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/consumer/iexml/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.
 
 **1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| outputFile | String | yes | no | Path to an output file |
| author | String | no | no | The author of the document |
| description| String | no | no | The description of the document| 

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| outputFile | valid Path to an output file | `data/IEXMLOutputData` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.mantra.Corpus | `+` |  |
| de.julielab.jcore.types.mantra.Document | `+` |  |
| de.julielab.jcore.types.mantra.Unit | `+` |  |
| de.julielab.jcore.types.mantra.Entity | `+` |  |
 
### Reference
Rebholz-Schuhmann, Dietrich, Harald Kirsch, and Goran Nenadic. "IeXML: towards an annotation framework for biomedical semantic types enabling interoperability of text processing modules." SIG BioLink, ISMB (2006).
