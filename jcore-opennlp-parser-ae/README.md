# JCoRe OpenNLP Parser Wrapper

**Descriptor Path**:
```
de.julielab.jcore.ae.opennlpparser.desc.jcore-opennlp-parser
```

### Objective
The JULIE Lab ParseAnnotator is an Analysis Engine, which turns given sentences into a parse tree with part of speech tags. This Engine is a UIMA Wrapper for the OpenNLP `Parser`, which provides an interface also named `Parser`. This interface has a method `parse(Parse tokens)` The mentioned method returns a parse for the specified parse of tokens. 

### Requirements and Dependencies

### Using the CR - Descriptor Configuration

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

### Reference
[1] Some Reference
