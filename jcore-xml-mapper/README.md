# JCoRe XML Mapper
This is a generic XML mapper to create CAS instances reflecting contents of XML documents.

### Objective
The JULIE Lab XMLMapper is a mapper which maps XML elements from an XML document onto (JCore) Types or Type Features. For that issue it uses a mapping file, which comes as an input. 

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| - | - | - | - |- |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| -| - | - |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType |  | `+` |

