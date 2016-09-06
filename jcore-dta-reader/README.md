# JCoRe DTA Collection Reader
Reader for DTA files (German digital humanties corpus).
DTA uses a TEI variant, cf. http://www.deutschestextarchiv.de/doku/basisformat  

**Descriptor Path**:
```
Path
```

### Objective


### Requirement and Dependencies


### Using the CR - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputFile | String | yes | no | Path to a dta file |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputFile | valid Path to a specific DTA file | `foo` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| foo |  | `+` |
