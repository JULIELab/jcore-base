 # JCore Gene Species Assigner
 JCoRe Analysis Engine to assign gene mention annotations a species via NCBI Taxonomy ID from Organism annotations.  

**Descriptor Path**:
```
de.julielab.jcore.ae.gene-species-assigner.desc.jcore-gene-species-assigner-ae
```

### Objective
JULIE Lab Gene Species Assignment Annotator is an UIMA Analysis Engine that assigns each Gene in the CAS index a taxonomy identifier from the NCBI Taxonomy.

### Requirement and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the AE - Descriptor Configuration
 In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/ ` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

None.

**2. Predefined Settings**

None.

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Gene | `+` | |
| de.julielab.jcore.types.Organism | `+` | |
| de.julielab.jcore.types.Gene#species | | `+` |
