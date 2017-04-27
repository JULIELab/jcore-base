# JCoRe OpenNLP POS Tagger Wrapper

**Descriptor Path**:
```
de.julielab.jcore.ae.opennlppostag.desc.jcore-opennlppostag

```

### Objective
The JULIE Lab PosTagAnnotator is an Analysis Engine that provides Part-Of-Speech tags for tokens. This Engine is a UIMA Wrapper for the OpenNLP `POS Tagger` It provides a method `tag(String[] sentence)`, which assigns pos tags to the sentence of tokens and returns an array of pos tags for each token provided in sentence.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under `src/main/resources/de/julielab/jcore/ae/opennlppostag/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelFile | String | yes | no | Path to the OpenNLP POS Tagger model |
| tagset | String | yes | no | The UIMA POSTag subtype to be used for the POS annotations |
| language | String | yes | no | Language (e.g. english) |
| caseSensitive | Boolean  | no | no | True, if a tag dictionary is case sensitive |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelFile | A valid Path to a POS Tagger model file | `resources/POSTaggerPennBio.bin.gz` |
| tagset | CAS Type | `de.julielab.jcore.types.jcore-basic-types` |
| language | ISO 639-1/2 | `en` |
| caseSensitive | Boolean | `true` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |`+`|   |
| de.julielab.jcore.types.Token    |`+`|   |
| de.julielab.jcore.types.POSTag   |   |`+`|


### Reference

