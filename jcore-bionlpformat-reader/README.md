# JCoRe BioNLP Format Reader
Reader that converts [BioNLP Shared Task](http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/index.shtml#data) formatted files to CAS objects.  

**Descriptor Path**:
```
de.julielab.jcore.reader.bionlpformat.desc.jcore-bionlpformat-reader-biomedical-sharedtask
de.julielab.jcore.reader.bionlpformat.desc.jcore-bionlpformat-reader-medical
de.julielab.jcore.reader.bionlpformat.desc.jcore-bionlpformat-reader-segment
```

### Objective
This reader takes as input a folder with `txt`,`a1` & `a2` files - format of the [BioNLP Shared Task](http://www.nactem.ac.uk/tsujii/GENIA/SharedTask/index.shtml#data) - and creates CAS annotations as described in **Capabilities**. Therefore it serves e.g. as reader in [relation extraction pipelines](https://github.com/JULIELab/jcore-pipelines/tree/master/jcore-relation-extraction-pipeline).

### Using the Consumer - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/reader/bionlp09event/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.
 
 **1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inDirectory | String | yes | no | Path to an input folder |
| bioEventServiceMode | Boolean | no | no | *desc here* |
| abstractFile | String | no | no | *desc here* | 
| proteinFile | String | no | no | *desc here* |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inDirectory | valid Path to an input directory | `data/BioNLPinData` |
| bioEventServiceMode | boolean Variable | `false` |
| abstractFile | *desc* | `none` |
| proteinFile | *desc* | `none` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.CorefExpression |  | `+` |
| de.julielab.jcore.types.AbstractText |  | `+` |
| de.julielab.jcore.types.Annotation |  | `+` |
| de.julielab.jcore.types.ArgumentMention |  | `+` |
| de.julielab.jcore.types.CorefRelation |  | `+` |
| de.julielab.jcore.types.Entity |  | `+` |
| de.julielab.jcore.types.EntityMention |  | `+` |
| de.julielab.jcore.types.EventMention |  | `+` |
| de.julielab.jcore.types.EventTrigger |  | `+` |
| de.julielab.jcore.types.Gene |  | `+` |
| de.julielab.jcore.types.Title |  | `+` |
| de.julielab.jcore.types.pubmed.Header |  | `+` |

### Reference
Jin-Dong Kim, Tomoko Ohta, Sampo Pyysalo, Yoshinobu Kano, and Jun’ichi Tsujii. 2009. Overview of BioNLP’09 Shared Task on Event Extraction. In *BioNLP 2009 - Proceedings of the Companion Volume: Shared Task on Event Extraction. Boulder, CO, USA, June 5, 2009*, pages 1–9.
