# JCoRe BioNLP 09 Event Consumer
Consumer that writes CAS annotations into the [BioNLP Shared Task](http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/index.shtml#data) format.
 
### Objective
This consumer takes the annotations specified in **Capabilities** and outputs three seperate text files for each document to `outDirectory`. The text files follow the format of the [BioNLP Shared Task](http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/index.shtml#data) and are therefore applicable for being evaluated by their eval tool or the online evaluation of the test files.

### Using the Consumer - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/consumer/bionlp09event/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.
 
 **1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| outDirectory | String | yes | no | Path to an output folder |
| bioEventServiceMode | Boolean | no | no | *desc here* |
| a2FileString | String | no | no | *desc here* | 

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| outDirectory | valid Path to an output directory | `data/BioNLPoutData` |
| bioEventServiceMode | boolean Variable | `false` |
| a2FileString | *desc* | `none` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Gene | `+` |  |
| de.julielab.jcore.types.EventTrigger | `+` |  |
| de.julielab.jcore.types.EntityMention | `+` |  |
 
### Reference
Jin-Dong Kim, Tomoko Ohta, Sampo Pyysalo, Yoshinobu Kano, and Jun’ichi Tsujii. 2009. Overview of BioNLP’09 Shared Task on Event Extraction. In *BioNLP 2009 - Proceedings of the Companion Volume: Shared Task on Event Extraction. Boulder, CO, USA, June 5, 2009*, pages 1–9.
