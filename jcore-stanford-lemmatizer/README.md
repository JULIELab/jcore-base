# Stanford Lemmatizer
  

**Descriptor Path**:
```
de.julielab.jcore.ae.stanford.lemma.desc.jcore-stanford-lemmatizer
```


### Objective
The JULIE Lab Stanford Lemmatizer is an UIMA Analysis Engine that annotates tokens with their lemma, or in other words, their dictionary or canonical form. This Engine uses the class `Morphology` from the Stanford JavaNLP API, which provides a method `lemma(String word, String tag)`. This method returns the lemma of an input word being sensitive to the input tag.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under `src/main/resources/de/julielab/jcore/ae/stanford/lemma/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| - | - | - | - | - |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| - | - | - |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Token  |`+`|   |
| de.julielab.jcore.types.POSTag |`+`|   |
| de.julielab.jcore.types.Lemma  |   |`+`|


### Reference



