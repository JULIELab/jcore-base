# JCore IGN Reader
A Collection Reader that converts BioC format files to CAS objects.

**Descriptor Path**:
```
de.julielab.jcore.reader.ign.desc.jcore-ign-reader
```
### Objective
The JULIE Lab IGN Reader is a UIMA Collection Reader (CR). It reads corpus files in BioC-format. There are XML files comprising the actual text (as well as passage and sentence annotations) and there are separate XML files comprising the
annotations. The Reader converts the files to types defined in the UIMA type system that we provide as well.

### Requirements and Dependencies
The input files for the JULIE Lab IGN Reader can be purchased at (http://bioc.sourceforge.net/). The output of the CR is in the form of annotation objects. The classes corresponding to these objects are part of our [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/reader/ace/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputDirectoryText | String | yes | no | Directory containing files in BioC-format that comprise the actual text. |
| inputDirectoryAnnotations| String | no | no | Directory containing files in BioC-format that comprise the annotations. |
| publicationDatesFile | String | yes | no | File containing a mapping between article ids and publication years. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputDirectoryText | valid Path to the BioC files | `data/BioCText` |
| inputDirectoryAnnotations| valid Path to the BioC files | `data/BioCAnnotations` |
| publicationDatesFile | valid File name | `/de/julielab/jcore/reader/ign/pubdates/IGN_publicationDates`  (default) |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Gene |  | `+` |
| de.julielab.jcore.types.Date |  | `+` |
| de.julielab.jcore.types.GeneResourceEntry |  | `+` |
| de.julielab.jcore.types.Journal |  | `+` |
| de.julielab.jcore.types.pubmed.Header |  | `+` |

So if you want to use this CR out-of-the-box (as a Maven Dependency in another project or as Component in a CPE) make sure to either put the data in the predefined inputDirectory or change this parameter to your liking.

### Reference
Comeau DC, Islamaj Doğan R, Ciccarese P, et al. BioC: a minimalist approach to interoperability for biomedical text processing. Database: The Journal of Biological Databases and Curation. 2013;2013:bat064. doi:10.1093/database/bat064.
