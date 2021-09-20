# JCoRe Annotation Adder

**Descriptor Path**:
```
de.julielab.desc.jcore-annotation-adder-ae
```

Adds annotations from external sources, e.g. files, into the CAS. Currently, two types of annotations are supported:

1. Annotations anchored to the text by a start and end offset.
2. Document class annotations. Those will always result in `de.julielab.jcore.types.DocumentClass` annotations to be created and added to a single instance of `de.julielab.jcore.types.AutoDescriptor`.

For document class annotations, no offset mode is required, obviously. Whether text annotations or document classes should be added to the CAS is determined by the choice of the external resource interface, see section 3.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| `OffsetMode` | String | false | false | Determines the interpretation of annotation offsets. Possible values: "CHARACTER" and "TOKEN". For the TOKEN offset mode, the correct tokenization must be given in the CAS. TOKEN offsets start with 1, CHARACTER offsets are 0-based. Defaults to CHARACTER. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| `OffsetMode` | `CHARACTER` or `TOKEN` | `CHARACTER` |

**3. External Resource Dependencies**

This component requires an external resource given with the `AnnotationSource` key. This dependency definition is pre-configured in the provided default descriptor and must be added to point to the correct annotation source.

The external dependency may currently be a file which is read completely into an in-memory map by the `de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileTextAnnotationProvider` class for textual annotations with offsets or by the `de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileDocumentClassAnnotationProvider` class for document classes. Both provider classes implement the required external resource interface `de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider`.

Other approaches, that are possible easier on the resources - might be implemented if necessary.

Currently, the external resource definition looks as follows:

    <externalResourceDependencies>
        <externalResourceDependency>
            <key>AnnotationSource</key>
            <description>A provider of annotations to add to the CAS. Must implement the
                de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider interface.
            </description>
            <interfaceName>de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider</interfaceName>
            <optional>false</optional>
        </externalResourceDependency>
    </externalResourceDependencies>
    <resourceManagerConfiguration>
        <externalResources>
            <externalResource>
                <name>InMemoryFileEntityProvider</name>
                <description/>
                <configurableDataResourceSpecifier>
                    <url>

                    </url>
                    <resourceMetaData>
                        <name/>
                        <configurationParameters/>
                        <configurationParameterSettings/>
                    </resourceMetaData>
                </configurableDataResourceSpecifier>
                <implementationName>de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileTextAnnotationProvider
                </implementationName>
            </externalResource>
        </externalResources>
        <externalResourceBindings>
            <externalResourceBinding>
                <key>AnnotationSource</key>
                <resourceName>InMemoryFileEntityProvider
                </resourceName>
            </externalResourceBinding>
        </externalResourceBindings>
    </resourceManagerConfiguration>
    
First, the dependency of an external resource is defined through the `externalResourceDependencies` element. No further action needed.

Then, the actual resource that contains the annotations needs to be given. Here, you need to add the file URL in the `url` element.
There are currently two file formats supported:
1. Files holding textual annotations, e.g. entity annotations, are expected in a tab-separated format with the columns `documentId`, `offset_begin`, `offset_end`, `UIMA_class`. The UIMA class is the fully qualified UIMA type name that should be created for the annotations, e.g. `de.julielab.jcore.types.Gene`.
2. Files holding document class information. Such files are expected to hold the tab-separated columns `confidence of the document class assignment`, `documentId`, `document class`, `assigning component ID`.

According to the file format to be read, the `resourcesManagerConfiguration/externalResources/externalResource/implementationName` element must be set to the correct provider class as described above.

Lastly, the resource is bound to the dependency. No further action needed.