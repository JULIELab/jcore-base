# JCoRe OpenNLP Tokenizer Wrapper

**Descriptor Path**:
```
jcore-base.jcore-opennlp-token-ae.desc.TokenAnnotator
```

### Objective
JULIE Lab TokenAnnotator is an UIMA Analysis Engine that annotates tokes in given sentences. This Engine is a Wrapper for the OpenNLP `Tokenizer` and assumes that sentences have been annotated in the CAS. 
It iterates over sentences and invokes the OpenNLP `Tokenizer` on each sentence. To instantiate the `TokenizerME` (the learnable Tokenizer), a Token Model must be created first.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under `jcore-base/jcore-opennlp-token-ae/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.


**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| ModelFile | String | yes | no | Path to the ModelFiles |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| ModelFile | valid Path to the ModelFiles  | `resources/TokenizerGenia.bin.gz` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |`+`|  |
| de.julielab.jcore.types.Token |   |`+`| 


### Reference
[1] Gregory Grefenstette, Pasi Tapanainen. 1994. What is a word? What is a sentence? Problems of Tokenization. pp 4-9.
