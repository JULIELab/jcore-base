# JCoRe PMC Collection Reader
A Collection Reader that reads NXML files from PubMed Central (PMC) and converts them to CAS objects.

**Descriptor Path**:
```
de.julielab.jcore.reader.pmc.desc.jcore-pmc-reader
```

### Objective
The JULIE Lab PMC Reader is a UIMA Collection Reader (CR). The document type handeled by this CR is the [Pubmed Central](https://www.ncbi.nlm.nih.gov/pmc/) [NXML](https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html) format.
This format is intended for journal publications of scientific full text papers. Thus, the CR makes efforts to not only parse the document's text contents but also
to the given document structure elements like sections and paragraphs and text object like figures and tables. Figures and tables themselves are currently omitted since it is not a trivial task to represent them appropriatly in a UIMA CAS object.
However, figure and table captions are accounted for by the reader. 
### Requirements and Dependencies
* jcore-types
* julie-xml-tools
* snakeyaml
### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/reader/pmc/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/d/uimaj-current/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| Input | String | true | false | The NXML file to read or a directory of NXML files to read. The reader will also check subdirectories for NXML files. Each file that includes the string 'nxml' is deemed to be a PubMed Central NXML file to be read. |
| AlreadyRead | String | false | false | A file that contains a list of already read file names. Those will be skipped by the reader. While reading, the reader will append read files to this list. If it is not given, the file will not be maintained. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| Input | A valid path to an input file or directory | `data/example.nxml` |
| AlreadyRead | A valid path to a file that contains a list of already read file names | `data/alreadyRead.txt` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| - |  - | - |



### Reference
[NXML format description](https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html)


## Configuration of the CR
The PubMed Central reader works with two different types of components:
1. The general parsing class `de.julielab.jcore.reader.pmc.parser.DefaultElementParser`
2. Specialialized classes parsing specific XML elements, e.g. `de.julielab.jcore.reader.pmc.parser.FrontParser`

The workflow of parsing a single NXML document is as follows: Each element of the XML tree is traversed in a depth-first fashion. For each element, a parser class registered to the name of the element is sought.
If an associated parser class is found, this specific class handels the element. If no parser class is found, the `DefaultElementParser` is employed. The reason for this workflow lies in the fact that a number
of elements are simple text span markup elements that do not require special treatment or overly complicated parsing, for example paragraphs or sections. Also, some elements might be more complicated, like references,
but are not yet handeled by the reader to their full extend. By using the `DefaultElementParser`, these element are still accounted for while reading without handeling their inner semantics.
For such more complicated elements like references, footnotes or elements that contain meta data that should not be reflected in the CAS document text like front and back matter, a special class can be
created that handles the respective element. Such parsing classes 