# JCoRe Acronym Analysis Engine
This is a reimplementation of the Schwartz and Hearst Algorithm for the resolution of acronyms (short form → long form).

### Objective
JULIE Lab Acronym Annotator (JACRO) is an UIMA Analysis Engine that annotates acronyms with their full-forms when locally introduced in the current document. The functionality of the engine is based on the simple algorithm for abbreviation recognition by Schwartz and Hearst. We have reimplemented the algorithm and extended it with respect to some pattern definitions and normalizations.
During the processing of documents, this UIMA annotator takes `jcore.types.Sentence` annotions from the CAS and creates an `jcore.types.Abbreviation` annotation object for each identified acronym. The `jcore.types.Abbreviation` annotation stores the corresponding full-form, whether the acronym was introduced at the respective position, and a reference to the full-form in the text.

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ae/acronymtagger/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| ConsistencyAnno | Boolean | yes | no | specifies whether only the first or all occurences of the acronym are annotated in the document |
| MaxLength| Integer | yes | no | defines how far (how many words, ignoring stopwords) the AE is supposed to look for the beginning of the fullform |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| ConsistencyAnno | set to true if you want to annotate all occurences of the acronyms that were found | `true` |
| MaxLength| just an integer | `5` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence | `+` |  |
| de.julielab.jcore.types.Abbrevation |  | `+` |


### Reference
Ariel S. Schwartz and Marti H. Hearst. 2003. A simple algorithm for identifying abbreviation definitions in biomedical text. In Russ B. Altman, A. Keith Dunker, Lawrence Hunter, Tiffany A. Jung, and Teri E. Klein, editors, *PSB 2003 – Proceedings of the Pacific Symposium on Biocomputing 2003. Kauai, Hawaii, USA, January 3-7, 2003*, pages 451–462.
