# JCoRe MUC7 Collection Reader
Reader that converts MUC-7 (Message Understanding Conference) files to CAS objects

### Objective
The MUC7 Reader reads in the data from the Message Understanding Conference (MUC) 7 Corpus.
However, be aware that this reader only accepts well formed XML and not the SGML files from the Linguistic Data Consortium (LDC). There is a Python script in the `scripts` folder that converts a MUC7 SGML to an XML:  
`python muc7_SGML2XML.py FILE`

### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDirectory | String | yes | no | Path to MUC7 files |
| generateJcoreTypes| Boolean | no | no | Determines if JULIE Lab Types (jcore-semantics-muc7-types.xml) should be generated in addition to types from jcore-mux7-types.xml |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputDirectory | valid Path to the ACE files | `data/MUC7Data` |
| generateJcoreTypes| Boolean Variable | `true` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.muc7.Coref |  | `+` |
| de.julielab.jcore.types.muc7.MUC7Header |  | `+` |

**3.1 Capabilities in Preparation**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.muc7.ENAMEX |  | `+` |
| de.julielab.jcore.types.muc7.NUMEX |  | `+` |
| de.julielab.jcore.types.muc7.TIMEX |  | `+` |
