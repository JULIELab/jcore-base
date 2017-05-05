# JCoRe Part-of-Speech Tagging Analysis Engine
Tagger for annotating a text with (arbitrarily chosen) part-of-speech tags

### Objective
The JULIE Lab Part of Speech Tagger (JPOS) is a generic and configurable POS tagger. JPOS was tested on the general-language news paper domain and in the biomedical domain; it performs very good for German texts, yet only mediocre for English [HMFH15].
As JPOS employs a machine learning (ML) approach, a model (for the specific domain and entity classes to be predicted) needs to be trained first. Thus, JPOS offers a training mode. Furthermore, JPOS also provides an evaluation mode to assess the current model performance in terms of accuracy.

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. As of now the present component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use them in your pipeline.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| modelFileName | String | yes | no | Filename of trained model for JPOS |
| postagset | String | yes | no | A set of the Part-Of-Speech tags |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| modelFileName | A valid Path to the File of the model | `??` |
| postagset | Valid postags | `ART;ADJA;NN` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence | `+` | |
| de.julielab.jcore.types.Token | `+` |  |
| de.julielab.jcore.types.POSTag |  | `+` |

### Manual
An extensive documentation can be found under `doc/`.

### References
[HMFH15] |  Johannes Hellrich, Franz Matthies, Erik Faessler & Udo Hahn: Sharing Models and Tools for Processing German Clinical Text. In: Ronald Cornet, Lăcrămioara Stoicu-Tivadar, Alexander Hörbst, Carlos Luis Parra Calderón, Stig Kjær Andersen, Mira Hercigonja-Szekeres (Eds.): *Digital Healthcare Empowering Europeans [= Proceedings of MIE2015]*, 2015, pp. 734-738 (Studies in Health Technology and Informatics, 210).
