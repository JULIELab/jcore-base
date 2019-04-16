# JCoRe Flair Token Embedding Annotator

**Descriptor Path**:
```
de.julielab.jcore.ae.fte.desc.jcore-flair-token-embedding-ae
```

Given a Flair compatible embedding file, computed the token embeddings of the CAS and sets them to the embeddingVector feature of the tokens.

The python executable lookup works as follows:
1. If it is configured in the descriptor, this value is used.
2. Otherwise, if the environment variable `PYTHON` is set, this value is used.
3. Otherwise, the `python` command is used.

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
