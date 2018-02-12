# JCoRe CAS2Conll Consumer
Consumer that generates CoNLL (Conference on Computational Natural Language Learning) formatted files for specified annotations.  

**Descriptor Path**:
```
`de.julielab.jcore.consumer.cas2conll.jcore-cas2conll-consumer`
```

### Objective
This consumer writes annotations in a UIMA CAS out to the CoNLL format. If two annotations are concurring for the same token, the annotation with the longer span is preferred.

### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| outputDir | String | yes | no | Path to directory where CoNLL-files should be written to. |
| dependencyParse | Boolean | no | no | Whether dependency parsing should be written or not, default is false. |
| posTag | Boolean | no | no | Whether POS Tags should be written or not, default is true. |
| lemma | Boolean | no | no | Whether Lemma should be written or not, default is true. |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| outputDir | valid path to an output location | `data/outFiles` |
| dependencyParse | Boolean | `false` |
| posTag | Boolean | `true` |
| lemma | Boolean | `true` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
|de.julielab.jcore.types.Sentence  | `+` |  |
|de.julielab.jcore.types.Token  | `+` |  |
|de.julielab.jcore.types.POSTag  | `+` |  |
|de.julielab.jcore.types.Lemma  | `+` |  |
|de.julielab.jcore.types.DependencyRelation  | `+` |  |
|de.julielab.jcore.types.Header  | `+` |  |
