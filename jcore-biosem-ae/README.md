# JCoRe BioSEM Analysis Engine
Wrapper for the [BioSEM Event Extraction System](https://github.com/JULIELab/jcore-dependencies/tree/master/biosem-event-extractor)

### Objective
The JULIE Lab BioSemEventAnnotator is an Analysis Engine, which deals with extraction of events and relations in the biomedical domain. 

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).
Furthermore this AE is only a wrapper, so the [BioSEM Event Extraction System](https://github.com/JULIELab/jcore-dependencies/tree/master/biosem-event-extractor) is needed, too.

### Using the AE - Descriptor Configuration
For this component the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. This component has a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use this in your pipeline.

**1. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Gene | `+` |  |
| de.julielab.jcore.types.EventTrigger |  | `+` |
| de.julielab.jcore.types.EventMention |  | `+` |
| de.julielab.jcore.types.ArgumentMention |  | `+` |

### Reference
Quoc C. Bui and Peter M. A. Sloot. 2012. A robust approach to extract biomedical events from literature. In *Bioinformatics (Oxford, England), 28(20)*, pages 2654–2661.
### Original Code
http://dl.dropbox.com/u/10256952/BioEvent.zip
