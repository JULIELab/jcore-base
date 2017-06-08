# JCore Text Consumer
A simple text consumer.

**Descriptor Path**:
```
TODO
```
### Objective
The JULIE Lab SentenceTokenAnnotator is a UIMA Cosumer (CC). It outputs sentences (one per line), tokens (white-space-separated) and adds a POS feature to it's token, separated by the delimiter '|'.

### Requirements and Dependencies
The input of a CC is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types). The output should be a valid txt-file.

### Using the CC - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/consumer/xmi/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| outDirectory | String | yes | yes | Path to an output directory |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| outDirectory | Valid Path to an output directory | `data/example.txt` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Header | `+` |  |
| de.julielab.jcore.types.Sentence | `+` |  |
| de.julielab.jcore.types.POSTag | `+` |  |
| de.julielab.jcore.types.Token | `+` |  |
