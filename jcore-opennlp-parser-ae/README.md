# JCoRe OpenNLP Parser Wrapper

### Objective
The JULIE Lab ParseAnnotator is an Analysis Engine, which turns given sentences into a parse tree with part of speech tags. This Engine is a UIMA Wrapper for the OpenNLP `Parser`, which provides an interface also named `Parser`. This interface has a method `parse(Parse tokens)`. The mentioned method returns a parse for the specified parse of tokens. 

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. This component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to that website for information on how to use this in your pipeline.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelDir | String | yes | no | Path to the directory with OpenNLP Parser models |
| tagset | String | yes | no | CAS Type to annotate |
| useTagDict | Boolean | yes | no | True, if a dictionary should be used |
| caseSensitive | Boolean | no | no | True, if a dictionary is case-sensitive |
| fun | Boolean | no | no | True, if parsing with functional tags (e.g. subj, obj) |
| mappings | String | yes | yes | Mappings between CAS constituent tags and OpenNLP Parser tags |
| beamSize | Integer | no | no | Beam size |
| advancePercentage | String | no | no | Amount of probability mass required of advanced outcomes |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelDir | Valid Path to the model directory | `resources/modelsGenia` |
| tagset | CAS sub-type of Constituent | `de.julielab.jcore.types.jcore-basic-types` |
| useTagDict | Boolean | `true` |
| mappings | OpenNLP name; CAS name | `S;S` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |`+`| |
| de.julielab.jcore.types.Token |`+`|  |
| de.julielab.jcore.types.Constituent | |`+`|  


### Reference

