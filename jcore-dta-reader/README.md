# JCoRe DTA Collection Reader
Reader for DTA files (German digital humanities corpus).
DTA uses a TEI variant, cf. http://www.deutschestextarchiv.de/doku/basisformat
Transformation of DTA to plaintext can be achieved with https://github.com/JULIELab/dta-converter
See also (in German): Hellrich, Matthies & Hahn (2017): UIMA als Plattform für die nachhaltige Software-Entwicklung in den Digital Humanities. In: DHd 2017, pp. 279-281. http://www.dhd2017.ch/wp-content/uploads/2017/02/Abstractband_ergaenzt.pdf

**Descriptor Path**:
```
de.julielab.jcore.reader.dta.desc.jcore-dta-reader
```

### Objective
The JULIE Lab DTAFileReader is a UIMA Collection Reader (CR). It reads files from the German Digital Humanities Corpus, which are given as tcf.xml files and converts them to types defined in the UIMA type system that we provide as well.


### Requirement and Dependencies
The input files for the JULIE Lab DTAFile Reader can be downloaded at the [Deutsches Textarchiv (DTA)](http://www.deutschestextarchiv.de/).


### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputFile | String | yes | no | Path to a dta file |
| normalize | Boolean | no | no | Decides whether the input should be normalized |
| format2017 | Boolean | no | no | Switch between 2017 (default) and 2016 DTA format |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputFile | valid Path to a specific DTA file | `data/example.tcf.xml` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.reader.dta.mapping.MappingService |  | `+` |
| de.julielab.jcore.types.Lemma |  | `+` |
| de.julielab.jcore.types.STTSPOSTag |  | `+` |
| de.julielab.jcore.types.Sentence |  | `+` |
| de.julielab.jcore.types.Token |  | `+` |
| de.julielab.jcore.types.extensions.dta.Header |  | `+` |
| de.julielab.jcore.types.extensions.dta.PersonInfo|  | `+` |
| de.julielab.xml.JulieXMLConstants |  | `+` |
| de.julielab.xml.JulieXMLTools |  | `+` |
