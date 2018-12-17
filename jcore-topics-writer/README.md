# JCoRe Topics Writer

**Descriptor Path**:
```
de.julielab.jcore.consumer.topics.desc.jcore-topics-writer
```

Writes the topic weights, given the jcore-topic-indexing-ae running before, into a simple text file. Thus, the output consists of a sequency of double numbers encodes as strings, separated by white spaces. The topic ID is just the 0-based index of each number, from left to right in the written file. The first entry of each file is the document ID.



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
