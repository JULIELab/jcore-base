# JCoRe XML Mapper
NOTE: This is not a UIMA component but rather a library used by some JCoRe components.
This is a generic XML mapper to create CAS instances reflecting contents of XML documents.

### Objective
The JULIE Lab XMLMapper is a mapper which maps XML elements from an XML document onto (UIMA) types or type features. For that task it uses a mapping file, which comes as an input.
Examples for mapping files are found in some [jcore-projects](https://github.com/JULIELab/jcore-projects) components,
for example the [jcore-pubmed-reader](https://github.com/JULIELab/jcore-projects/tree/master/jcore-pubmed-reader), its
MEDLINE-pendant or the database versions of both. 

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

### Mapping File Syntax
Please note that this section is incomplete. The mapping file of the [jcore-pubmed-reader](https://github.com/JULIELab/jcore-projects/tree/master/jcore-pubmed-reader) includes examples for all supported features.

The basic structure of the mapping file consists of the `<mappings>` root element, a `<documentText>` root child element and an arbitrary number of `<tsType>` ('type system type', referring to the UIMA type system to be employed) root child elements:

```xml
<mappings>
    <documentText>
        ...
    </documentText>
    <tsType>
        ...        
    </tsType>
    <tsType>
        ...
    </tsType>
    ...
</mappings>
```

## Document Text
The CAS document text is populated with the `<documentText>` mapping element. It defines an arbitrary number of `<documentPart>` elements of whose mapping values the document text will be comprised, in the order of the `<documentPart>` elements in the mapping file. Each document part is given a manually defined ID which can be referred to in order to create a UIMA annotation covering the respective document part text. The location of the actual character data in the mapped document XML files is specified via XPath.

```xml
<documentText>
	<partOfDocumentText id="0">
		<xPath>/MedlineCitation/Article/ArticleTitle</xPath>
	</partOfDocumentText>
	<partOfDocumentText id="1">
		<xPath>/MedlineCitation/Article/Abstract</xPath>
	</partOfDocumentText>
</documentText>
```

This example collects the article title, and the abstract of a MEDLINE XML document for the CAS document text.

The `<partOfDocumentText>` may have an optional child element named `<externalParser>`. This is useful or even a necessity when the document structure for this element is not static, i.e. has a varying number of children. In such a case, a user-delivered class on the classpath can be specified. This class must implement the `de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextPartParser` interface and received the document XML element that the XPath in the mapping file points to. It then returns a list of strings using to comprise the respective part of the document text:

```xml
<partOfDocumentText id="1">
	<xPath>/MedlineCitation/Article/Abstract</xPath>
	<externalParser>
		de.julielab.jcore.reader.xmlmapper.mapper.StructuredAbstractParser
	</externalParser>
</partOfDocumentText>
```

The `StructuredAbstractParser` is able to parse the child elements of `/MedlineCitation/Article/Abstract`, namely `AbstractText` elements which also have attributes, `Label` and `NlmCategory`. Those are details to the MEDLINE XML format and are just use here as an example use case for external parsers.

## UIMA Type Annotations

Annotations are added with the `<tsType>` element. Its main children are `<tsFullClassName>` and `<tsFeature>`, defining the actual type to be instantiated and any feature values that should be added to the type. Since a UIMA type feature can itself be a type, `<tsFeature>` elements can be nested. Then, the `<xpath>` child of a `<tsFeature>` element is resolved *relative* the `<xpath>` of the parent `<tsFeature>` element. Thus, when the parent `<tsFeature>` element does not specify an `<xpath>` element, which is perfectly legal, the given xpath is resolved from the XML document root:

```xml
<tsType>
    <tsFullClassName>fully qualified UIMA type name</tsFullClassName>
    <tsFeature>
        <tsFeatureName>feature name of the type</tsFeatureName>
        <isType>true if the feature value is a UIMA feature structure (annotation) itself</isType>
        <tsFullClassName>
            The value data type of the feature as it is passed to the setter for this feature in Java code.
            This can also be an array type, e.g. org.apache.uima.jcas.cas.FSArray.
        </tsFullClassName>
        <tsFeature>
            <tsFeatureName>optional if the parent tsFullClassName is an array type</tsFeatureName>
            <isType>true</isType>
            <xPath>
                absolute xpath since the parent does not specify an xpath
            </xPath>
            <tsFullClassName>
                fully qualified UIMA type name of this nested type
            </tsFullClassName>
    
            <tsFeature>
                <tsFeatureName>name of this feature relative to the parent fsFullClassName type</tsFeatureName>
                <xPath>relative xpath to the parent xpath</xPath>
                <tsFullClassName>a primitive data type (or a string) since this is not a UIMA type itself (missing isType element).</tsFullClassName>
            </tsFeature>
        </tsFeature>
    </tsFeature>
</tsType>
```
The above example showcases the structure of a nested annotation, i.e. a feature path. The outer type will have another type as feature value which in turn has a primitive value as the final feature value.

**Important** The `<xpath>` values are evaluated for *all occurrences* of the respective XPath in the XML document. Thus, the above annotations will be created for all XPath matches. This holds true for every level of `<xpath>` specifications. This allows collecting child XML document elements into arrays. An outer xpath points to the collection document elements, and an inner xpath points the children.

The `<tsFeature>` element again accepts the child element `<externalParser>`. In this case, the external parser needs to implement the `de.julielab.jcore.reader.xmlmapper.typeParser.TypeParser` interface. It might be helpful to extend the class `de.julielab.jcore.reader.xmlmapper.typeParser.StandardTypeParser` and use its `parseSingleType` method.

Finally, the `<tsFeature>` element accepts the `<offset>` child element which can point to a part of document text, thus create an annotation for the respective document text part as identified by its ID:

```xml
<offset>
	<partOfDocumentText>
		<id>0</id>
	</partOfDocumentText>
</offset>
```

