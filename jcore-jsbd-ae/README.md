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

`To Do`

**2. Predefined Settings**

`To Do`

**3. Capabilities**

`To Do`

### Manual
An extensive documentation can be found under `doc/`.

You can also run JSBD just via the self-executing JAR `jsbd-<version>.jar`. This will show the available modes.
