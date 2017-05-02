# JCoRe OpenNLP Chunker Wrapper

### Objective
The JULIE Lab ChunkAnnotator is an Analysis Engine, which turns given tokenized sentences into syntactically correlated parts of words. This Engine is a UIMA Wrapper for the OpenNLP `Chunker`, which provides  a method `chunk(String[] tokens, String[] tags)`. The mentioned method enerates chunk tags for the given sequence and returns the result in an array. 

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. This component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use this in your pipeline.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelFile | String | yes | no | Path to the model |
| posTagSetPreference | String | yes | no | The POS Tagset preferred by this Chunker |
| mappings | String | yes | yes | Mappings between CAS constituent tags and OpenNLP Chunker tags |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelFile | Valid Path to the model file | `resources/modelsGenia` |
| posTagSetPreference | A valid String | A valid String |
| mappings | OpenNLP name; CAS name | `ChunkNP;ChunkNP` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |`+`| |
| de.julielab.jcore.types.Token |`+`|  |
| de.julielab.jcore.types.POSTag |`+`|  |
| de.julielab.jcore.types.Chunk |  |`+`|  

### Reference
[1] Some Reference
