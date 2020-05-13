# jcore-flair-embedding-writer

**Descriptor Path**:
```
de.julielab.jcore.consumer.few.desc.jcore-flair-embedding-writer
```

This component takes as input a number of precomputed embeddings that are compatible with Flair and a UIMA type. If any annotations of the given type are found in a CAS, the embedding vectors for the words covered by the annotations are computed and written to file.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| param1 | UIMA-Type | Boolean | Boolean | Description |
| param2 | UIMA-Type | Boolean | Boolean | Description |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| param1 | Syntax-Description | `Example` |
| param2 | Syntax-Description | `Example` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.TYPE |  | `+` |
| de.julielab.jcore.types.ace.TYPE | `+` |  |


[1] Some Literature?
