# JCoRe Lingpipe Gazetteer Analysis Engine
An Analysis Engine for Named-Entity Recognition using the LingPipe Libraries.

**Descriptor Path**:
`de/julielab/jcore/ae/lingpipegazetteer/desc/jcore-lingpipe-gazetteer-ae-configurable-resource.xml`

### Objective
The JULIE Lab GazetteerAnnotator is an Analysis Engine which is a wrapper for LingPipe's entity tagger based on a dictionary look-up. There are two modes: exact matching (only terms which map exactly to 
those specified in dictionary are found) and approximate matching (by means of weighted levenstein distance, approximate matches are found). 
For exact matching, LingPipe provides an implementation of the Aho-Corasick algorithm.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is avaiable under the descriptor path given above but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.
The Ligpipe Gazetteer component relies heavily on the external resource mechanism of UIMA. Thus, most configuration
parameters are not given to the AE itself but rather to its external resource which represents the dictionary
and determines the processing configuration. The descriptor given at the top of this README file defines
the external resource and its parameters but must be filled with the desired parameter values.

First, the component's own configuration parameters are explained below, then the external resource parameters.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| MantraMode | Boolean | true | false |  |
| CheckAcronyms | Boolean | true | false |  |
| OutputType | String | true | false |  |

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| CheckAcronyms | Boolean | no | no | Checks acronyms, needs to be true because of chunker injection |
| MantraMode | Boolean | no | no | Activate this to use gazetteer files which contain detailed information like cuis or sources|
| OutputType | String | yes | no | The UIMA annotation type that should be generated for text passages matching a dictionary entry|


**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| CheckAcronyms | Boolean | `true` |
| MantraMode | Boolean | `false` | 
| OutputType | A valid output Type | `<none>`|


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Abbreviation |`+`| |
| de.julielab.jcore.types.ConceptMention |`+`|  |
| de.julielab.jcore.types.mantra.Entity |  |`+`| 

**4. External Resource Parameters**

The external resource uses the exact same configuration mechanism as common UIMA components. The following
table explains the parameters.

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| TransliterateText | Boolean | no | no | Whether to strip accents and other character variations from the text |
| NormalizeText | Boolean | no | no | Parameter to indicate whether text should be normalized by completely removing dashes, parenthesis, genitive 's and perhaps more. The goal is to also match slight writing variants, similarly to the 'MakeVariants' parameter. |
| CaseSensitive | Boolean | no | no | Only used in the annotator if exact matching is enabled |
| MakeVariants| Boolean | no | no | Whether (non-)hyphenated/(non-)parenthesized dictionary variants should be generated. As with the 'NormalizeText' parameter, this method aims at finding writing variants of the dictionary entries. However, instead or removing punctuation from the input text, the dictionary is extended by automatically generated variant strings containing dashes and other punctuation that is common for writing variation.  |
| dictFile | Inputstream | yes | no | The Dictionary File. This file ought to have two tab-separted columns. The first column represents the dictionary, the second is the tagged category of the respective entry. |
| stopFile | Inputstream | yes | no | The StopWords File. |

### Reference
Aho, Alfred V., and Margaret J. Corasick. "Efficient string matching: an aid to bibliographic search." Communications of the ACM 18.6 (1975): 333-340.
http://alias-i.com/



