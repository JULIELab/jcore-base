# JCoRe Part-of-Speech Tagging Analysis Engine
Tagger for annotating a text with (arbitrarily chosen) part-of-speech tags

### Objective
The JULIE Lab Part of Speech Tagger (JPOS) is a generic and configurable POS tagger. JPOS was tested on the general-language news paper domain and in the biomedical domain; it performs very good for German texts, yet only mediocre for English [HMFH15].
As JPOS employs a machine learning (ML) approach, a model (for the specific domain and entity classes to be predicted) needs to be trained first. Thus, JPOS offers a training mode. Furthermore, JPOS also provides an evaluation mode to assess the current model performance in terms of accuracy.

### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

`To Do`

**2. Predefined Settings**

`To Do`

**3. Capabilities**

`To Do`

### Manual
An extensive documentation can be found under `doc/`.

### References
|   |   |
|---|---|
[HMFH15] |  Johannes Hellrich, Franz Matthies, Erik Faessler & Udo Hahn: Sharing Models and Tools for Processing German Clinical Text. In: Ronald Cornet, Lăcrămioara Stoicu-Tivadar, Alexander Hörbst, Carlos Luis Parra Calderón, Stig Kjær Andersen, Mira Hercigonja-Szekeres (Eds.): *Digital Healthcare Empowering Europeans [= Proceedings of MIE2015]*, 2015, pp. 734-738 (Studies in Health Technology and Informatics, 210).
