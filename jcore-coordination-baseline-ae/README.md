# JCoRe Coordination Baseline Analysis Engine
This is the baseline to the [JCoRe Coordination Analysis Engine](https://github.com/JULIELab/jcore-base/tree/issue7-coordFix/jcore-coordination-ae). As of now the Coordination AE is only available in its baseline form for the JCoRe package. The full-fledged AE is work-in-progress and not yet ready for distribution (see issue 7).

### Objective


### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**
`no parameters to define`

**2. Predefined Settings**
`see 1.`

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence | `+` |  |
| de.julielab.jcore.types.Token | `+` |  |
| de.julielab.jcore.types.Entity | `+` |  |
| de.julielab.jcore.types.Coordination |  | `+` |
| de.julielab.jcore.types.CoordinationElement |  | `+` |
