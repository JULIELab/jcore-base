# JCoRe Sentence Boundary Detection Analysis Engine
A machine learning based sentence boundary detector

### Objective
JSBD is a ML-based sentence splitter. It can be retrained on supported training material and is thus neither language nor domain dependent.

### Requirement and Dependencies
JTBD is based on a slightly modified version of the machine learning toolkit MALLET (Version 2.0.x). The necessary libraries are included in the executable JAR (see below) and accessible via the JULIE Nexus artifact manager.

 The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| ModelFilename | String | yes | no | filename of trained model for JSBD |
| Postprocessing| Boolean | no | no | Indicates whether postprocessing should be run. Default: no postprocessing |
| ProcessingScope | String | no | no | The UIMA annotation type over which to iterate for doing the sentence segmentation. If nothing is given, the document text from the CAS is taken as scope! This is recommended as default! |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| ModelFilename | valid path to a model file | `de/julielab/jcore/ae/jsbd/model/jsbd-2.0.gz`* |
| Postprocessing| Boolean Variable | `true` |
| ProcessingScope | Boolean Variable | `none` |
\* a model is not included; see the [biomed-project](https://github.com/JULIELab/jcore-projects/tree/master/jcore-jsbd-ae-biomedical-english) or [medical-project](https://github.com/JULIELab/jcore-projects/tree/master/jcore-jsbd-ae-medical-german) for a pre-built one 

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence |  | `+` |

### Manual
An extensive documentation can be found under `doc/`.

You can also run JSBD just via the self-executing JAR `jsbd-<version>.jar`. This will show the available modes.
