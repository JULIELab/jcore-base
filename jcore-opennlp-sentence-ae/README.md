# JCoRe OpenNLP Sentence Segmenter Wrapper

**Descriptor Path**:
```
TODO - insert descriptor path
```

### Objective
JULIE Lab SentenceAnnotator is an UIMA Analysis Engine that identifies sentences in given texts with respect to user-defined end-of-sentence punctuation markers. This Engine is a Wrapper for the OpenNLP `SentenceDetector`, which can detect that a punctuation character marks the end of a sentence or not. It has a method named `sentPosDetect(String s)`, which returns an array of spans for each detected sentence.
To instantiate the `SentenceDetectorME`, the Sentence Model must be loaded first.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under ... but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.


**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelFileName | String | yes | no | Path to the OpenNLP SentenceDectector model |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelFileName | valid Path to the ModelFiles  | `resources/SentDetectGenia.bin.gz` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |  |`+`|



### Reference
[1] Gregory Grefenstette, Pasi Tapanainen. 1994. What is a word? What is a sentence? Problems of Tokenization. pp 4-9.
