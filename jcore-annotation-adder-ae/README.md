# JCoRe Annotation Adder

**Descriptor Path**:
```
de.julielab.desc.jcore-annotation-adder-ae
```

Adds annotations from external sources, e.g. files, into the CAS.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| `OffsetMode` | String | false | false | Determines the interpretation of annotation offsets. Possible values: "CHARACTER" and "TOKEN". For the TOKEN offset mode, the correct tokenization must be given in the CAS. TOKEN offsets start with 1, CHARACTER offsets are 0-based. Defaults to CHARACTER. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| `OffsetMode` | `CHARACTER` or `TOKEN` | `CHARACTER` |

**3. External Resource Dependencies**

This component requires an external resource given with the `AnnotationSource`. This dependency is present in the provided default descriptor.

The external dependency may currently be a file which is read completely into an in-memory map by the `de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileEntityProvider` class which implements the required external resource interface `de.julielab.jcore.ae.annotationadder.annotationsources.AnnotationProvider`.

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
                <implementationName>de.julielab.jcore.ae.annotationadder.annotationsources.InMemoryFileEntityProvider
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
    
First, the dependency of an external resource is defined. No further action needed.

Then, the actual resource that contains the annotations needs to be given. Here, you need to add the file URL in the `url` element. The file is expected to be in tab-separated format and have the columns `documentId`, `offset_begin`, `offset_end`, `UIMA_class`. The UIMA class is the fully qualified UIMA type name that should be created for the annotations, e.g. `de.julielab.jcore.types.Gene`.

Lastly, the resource is bound to the dependency. No further action needed.