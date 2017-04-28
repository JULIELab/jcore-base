# JCoRe OpenNLP Tokenizer Wrapper

### Objective
JULIE Lab TokenAnnotator is an UIMA Analysis Engine that annotates tokes in given sentences. This Engine is a Wrapper for the OpenNLP `Tokenizer` and assumes that sentences have been annotated in the CAS. 
It iterates over sentences and invokes the OpenNLP `Tokenizer` on each sentence. To instantiate the `TokenizerME` (the learnable Tokenizer), a Token Model must be created first.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. As of now this component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use them in your pipeline.

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

