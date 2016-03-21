# JCoRe Named Entity Tagger Analysis Engine
Tagger for automatically detecting and classifying Named Entity Mentions in written plain text

### Objective

The JULIE Lab Named Entity Tagger (JNET) is a generic and configurable multi-class named entity recognizer. Given a plain text of written natural language, it automatically detects and classifies named entity mentions. JNET’s comprehensive feature sets allows to employ JNET for most domain and entity types. JNET was intensively tested on the general-language news paper domain (recognition of the classical MUC entities: person, location, organization) and several entity classes in the bio-medical domain.

As JNET employs a machine learning (ML) approach (see Section 7), a model (for the specific domain and entity classes to be predicted) needs to be trained first. Thus, JNET offers a training mode. Furthermore, JNET also provides several evaluation modes to assess the current model performance in terms of recall (R), precision (P), and f-score (F).

JNET offers the following functionalities:
* generation of training data containing multiple annotations
* training a model
* prediction using a previously trained model
* evaluation
* flexible feature parametrization

### Requirement and Dependencies
 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

 `TODO`

**2. Predefined Settings**

 `TODO`

**3. Capabilities**

 `TODO`

### Manual
AN extensive documentation can be found under `doc/`

data.ppd means data in socalled "piped format" which looks the following:
`<token1>|<POS>|<label><token2>|<POS>|<label> <token3>|<POS>|<label> ...`
One sentence per line.

Running JNET consumes data in piped format which can be produced from IOB and
further meta data, such as PoS information, by using the FormatConverter class
in JNET. This may be done by running JNET with the f parameter.

Remember to use the correct <name>.tags file which is needed in almost all
actions performed by JNET.

For comparing gold standard with the predictions made by the tagger, just
use the mode "c" and give both the gold standard and the predictions in IOB
format (make sure, both files have the same length, i.e. especially an empty
line at the end and a line break).

JNET may be run by giving console commands in the following form:

runJNET.sh <parameters>

If you want to see which modes JNET provides just leave the parameters blank.
