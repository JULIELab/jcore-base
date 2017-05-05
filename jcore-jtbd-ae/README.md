# JCoRe Token Boundary Detection Analysis Engine
A machine-learning-based token boundary detector.

### Objective
JTBD is a ML-based UIMA Analysis Engine that splits sentences and annotates tokens. It can be retrained on supported
training material and is thus neither language nor domain dependent.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).
JTBD is based on a slightly modified version of the machine learning toolkit MALLET (Version 2.0.x). The
necessary libraries are included in the executable JAR (see below) and accessible via the JULIE Nexus artifact manager.


### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. As of now this component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use them in your pipeline.


**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelFileName | String | yes | no | Path to the ModelFile |
| useCompleteDocText | Boolean | no | no | If the whole document text should be tokenized, default is `false` |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelFileName | valid Path to the ModelFiles  | `resources/TokenizerGenia.bin.gz` |
| useCompleteDocText | Boolean  | `false` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.ace.Sentence | `+` |  |
| de.julielab.jcore.types.Token |  | `+` |


### Reference

