# JCoRe XML Mapper
NOTE: This is not a UIMA component but rather a library used by some JCoRe components.
This is a generic XML mapper to create CAS instances reflecting contents of XML documents.

### Objective
The JULIE Lab XMLMapper is a mapper which maps XML elements from an XML document onto (UIMA) Types or Type Features. For that task it uses a mapping file, which comes as an input.
Examples for mapping files are found in some [jcore-projects](https://github.com/JULIELab/jcore-projects) components,
for example the [jcore-pubmed-reader](https://github.com/JULIELab/jcore-projects/tree/master/jcore-pubmed-reader), its
MEDLINE-pendant or the database versions of both. 

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.


