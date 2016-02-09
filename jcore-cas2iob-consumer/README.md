# JCoRe CAS2IOB Consumer
Consumer that generates IOB formatted files for specified annotations.
If two annotations are concurring for the same token, the annotation with the longer span is preferred.

### Objective


### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| labels | String | no | yes | Labels not to be expoted into IOB format. These are no references to UIMA types but to specific labels acquired by `labelNameMethods`. |
| labelNameMethods| String | yes | yes | Pairs of UIMA-annotation-types and their corresponding method for extracting the annotation label. Format: `<objName>[\s=/\\|]<method Name>`. If the name of the annotation class itself is to be being used as label, only the class name is expected: `<objName>`. You also may specify a mix of pairs and single class names. If you give the name extracting method for a class and have also specified its superclass as a single class name, the given method is used rather than the superclass name. |
| iobLabelNames | String | no | yes | Pairs of label names in UIMA (aquired by the methods given in labelNameMethods) and the name the label is supposed to get in the outcoming IOB file. Format: `<UIMA label name>[\s=/\\|]<IOB label name>` |
| outFolder | String | yes | no | Path to folder where IOB-files should be written to. |
| typePath | String | no | no | The path of the UIMA types, e.g. `de.julielab.jcore.` (with terminating "."!). It is prepended to the class names in labelNameMethods. This parameter may be null which is equivalent to the empty String "". |
| mode | String | yes | no | IOB or IO mode. The parameter is not case sensitiv, thus "iob" or "IOB" works both and will result in a sequence of IOBTokens (object of SegmentationEvaluator). |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| labels |  |  |
| labelNameMethods |  |  |
| iobLabelNames |  |  |
| outFolder |  |  |
| typePath |  |  |
| mode |  |  |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
|  | `+` |  |
|  |  | `+` |
