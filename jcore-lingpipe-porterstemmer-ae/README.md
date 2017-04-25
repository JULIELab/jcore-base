# JCoRe Lingpipe Porterstemmer
An Analysis Engine for Porter Stemming using the LingPipe libraries.

**Descriptor Path**:
```
de.julielab.jcore.ae.lingpipe.porterstemmer.desc.jcore-lingpipe-porterstemmer-ae
```

### Objective
The JULIE Lab Lingpipe Porterstemmer is an UIMA Analysis Engine that annotates tokens with their stemmed form, or in other words, their morphological base form. This engine uses the class `PorterStemmerTokenizerFactory` from the LingPipe library, which provides a static method `stem(String)`. This method returns a stemmed form of an input string.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under `src/main/resources/de/julielab/jcore/ae/lingpipe/porterstemmer/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.


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
| de.julielab.jcore.types.Token | `+`  |   |
| de.julielab.jcore.types.StemmedForm |  | `+` |


### Reference
M.F. Porter. 1980. An algorithm for suffix stripping. Program, Vol. 14 No.3, pp. 130-137.
Peter Willett. 2006. The Porter stemming algorithm: then and now. Program, Volume 40 Issue: 3, pages 219-223.
