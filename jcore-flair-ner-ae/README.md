# JCoRe FLAIR NER AE

**Descriptor Path**:
```
de.julielab.jcore.ae.flairner.desc.jcore-flair-ner-ae
```

This component uses the Zalando FLAIR toolkit (https://github.com/zalandoresearch/flair) for named entity recognition. Since FLAIR is a python library, this component must use python for the actual NER part.

The python executable lookup works as follows:
1. If it is configured in the descriptor, this value is used.
2. Otherwise, if the environment variable `PYTHON` is set, this value is used.
3. Otherwise, the `python` command is used.

Tested with flair 0.6.1 and PyTorch 1.7.1.

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
