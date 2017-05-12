# JCoRe XML Collection Reader
Reader that employs a mapping file for reading XML documents e.g. PubMed &amp; MEDLINE files; depends on the [JCoRe XML Mapper](https://github.com/JULIELab/jcore-xml-mapper).

**Descriptor Path**:
```
???
```

### Objective
The JULIE Lab XML Reader is a UIMA Collection Reader (CR) for reading XML files, providing them to UIMA for further processing. It depends on the XMLMapper.


### Requirement and Dependencies
This Reader depends on the [JCoRe XML Mapper](https://github.com/JULIELab/jcore-xml-mapper), which  maps XML elements from an XML document onto (JCore) Types or Type Features.


### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputDirectory | String | yes | no | Path to the directory of the file(s) |
| inputFile | String | no | no | Path to a XML file |
| headerTypeName | String | no | no | The name of the header type |
| mappingFileStr | String | no | no | TODO |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputDirectory | valid Path to a specific directory | `data/example.xml` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
|de.julielab.jcore.types.Header |  | `+` |

