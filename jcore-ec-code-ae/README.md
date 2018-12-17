# JCoRe ECN AE

**Descriptor Path**:
```
de.julielab.jcore.ae.ecn.desc.jcore-ecn-ae.xml
```

### Objective

This component recognizes Enzyme Commission (EC) numbers in documents. See [1] for a description of these numbers.
The ultimate goal is to identify gene name mentions in document text associated with an EC number

### Requirements and Dependencies

### Using the AE - Descriptor Configuration

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
[1] https://en.wikipedia.org/wiki/Enzyme_Commission_number
