# JCoRe Lingpipe Gazetteer Analysis Engine
An Analysis Engine for Named-Entity Recognition using the LingPipe Libraries.

**Descriptor Path**:
```
de.julielab.jcore.ae.lingpipegazetteer.desc.GazetteerAnnotator (??)
```

### Objective
The JULIE Lab GazetteerAnnotator is an Analysis Engine, which is a Wrapper for LingPipe's entity tagger based on a dictionary look-up. There are two modes: exact matching (only terms which map exactly to 
those specified in dictionary are found) and approximate matching (by means of weighted levenstein distance, approximate matches are found). 
For exact matching, LingPipe provides an implementation of the Aho-Corasick algorithm.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under `src/main/resources/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| checkAcronyms | Boolean | yes | no | Checks acronyms, needs to be true because of chunker injection |
| useApproximateMatching | Boolean | yes | no | Gazetteer mode, default value is false |
| mantraMode | Boolean | no | no | default value is false |
| transliterate | Boolean | yes | no | Parameter to indicate whether text should be transliterated |
| normalize | Boolean | yes | no | Parameter to indicate whether text should be normalized by completely removing dashes, parenthesis, genitive's and perhaps more |
| outputType | String | yes | no | the output type|
| caseSensitive | Boolean | no | no | The approximate chunker is always case sensitive |
| generateVariants| Boolean | no | no | - |
| dictFile | Inputstream | yes | no | The Dictionary File |
| stopFile | Inputstream | yes | no | The StopWords File |


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| checkAcronyms | Boolean | `true` |
| useApproximateMatching | Boolean | `false` |
| transliterate | Boolean | `true` |
| normalize | Boolean | `true` |
| outputType | A valid output Type | `XML`|
| dictFile | A valid Path to the Dictionary File | `data/exampleDict` |
| stopFile | A valid Path to the StopWords File | `data/stopWords`|


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Abbreviation |`+`| |
| de.julielab.jcore.types.ConceptMention |`+`|  |
| de.julielab.jcore.types.mantra.Entity |  |`+`|  

### Reference
Aho, Alfred V., and Margaret J. Corasick. "Efficient string matching: an aid to bibliographic search." Communications of the ACM 18.6 (1975): 333-340.

