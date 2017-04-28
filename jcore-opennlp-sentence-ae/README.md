# JCoRe OpenNLP Sentence Segmenter Wrapper

### Objective
JULIE Lab SentenceAnnotator is an UIMA Analysis Engine that identifies sentences in given texts with respect to user-defined end-of-sentence punctuation markers. This Engine is a Wrapper for the OpenNLP `SentenceDetector`, which can detect that a punctuation character marks the end of a sentence or not. It has a method named `sentPosDetect(String s)`, which returns an array of spans for each detected sentence.
To instantiate the `SentenceDetectorME`, the Sentence Model must be loaded first.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. As of now the present component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use them in your pipeline.

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

