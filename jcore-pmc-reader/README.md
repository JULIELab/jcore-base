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
created that handles the respective element. Such parsing classes extend `de.julielab.jcore.reader.pmc.parser.NxmlElementParser` and are registered to the parser registry in `de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser`.

Now, configuration can only be done on the `DefaultElementParser`. It is using the configuration file at `src/main/resources/de/julielab/jcore/reader/pmc/resources/elementproperties.yml`. This file in YAML format accepts a set
of properties and rules associated with an element type in the following format:
```yml
elementName
    property1
    property2
    ...
elementName
    ...
```

The following properties are currently supported:

| Property Name          |  Property Type  | Description |
|------------------------|-----------------|-------------|
| attributes             | list of objects | Allows to specify properties only to elements matching a specific attribute value on a given attribute name. |
| block-element          | boolean         | Specifies whether the element should be handled as a block element. Block elements always begin at a new line and have a line break after their last line. This is mainly important for a rough document text layout in the CAS. |
| default-feature-values | object          | Specifies key-value pairs where the key is a UIMA type feature name of the annotation type that is created for the current element (see the type property below). For eligible elements, their created annotation will have set the respective default feature values to the specified features. |
| omit-element           | boolean         | Whether or not to omit this element. For elements that should not be included in the document text but that also are not handled by any parser, this property may be used to skip an element type. |
| paths                  | list of objects | Allows to specify a relative or absolute XPath like sequence of element names in the form `abstract/sec/title` and properties that should be applied to elements matching this path. |
| type                   | string          | The UIMA type that should be used to annotate the text contents of the element |

The `attribute` and `path` properties define criteria where the base properties are overwritten by the properties specified in association with the given attribute-value combination or path. For example, it is possible to include a certain element for document text but omit it if has a specific element as parent or some attribute value.

Here is an example taken directly from the `elementproperties.yml` file:
```yml
title:
    block-element: true
    type: de.julielab.jcore.types.Title
    default-feature-values:
        titleType: other
    paths:
        - path: sec/title
          type: de.julielab.jcore.types.SectionTitle
          default-feature-values:
            titleType: section
        - path: abstract/sec/title
          type: de.julielab.jcore.types.AbstractSectionHeading
          default-feature-values:
            titleType: abstractSection
```
The rule here says "XML elements with the name 'title' are a block element and are annotated with an annotation of type 'de.julielab.jcore.types.Title' with the 'titleType' feature set to the string 'other'. However, if the path 'sec/title' or 'abstract/sec/title' apply to the element, change the annotation type to 'de.julielab.jcore.types.SectionTitle' or 'de.julielab.jcore.types.AbstractSectionHeading', respectively, and set the 'titleType' feature value to 'section' or 'abstractSection', respectively.
