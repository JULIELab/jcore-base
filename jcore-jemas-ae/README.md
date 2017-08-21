# JCoRe JEmAS
This is a UIMA AE implementation of the core functionality of JEmAS, the Jena Emotion Analysis System.

**Descriptor Path**:
```
de.julielab.jcore.ae.jemas.desc.jcore-jemas-ae
```

### Objective
The JULIE Lab Emotion Analysis System (JEmAS) is an UIMA Analysis Engine, ... TODO more information
### Requirements and Dependencies
TODO more information

### Using the AE - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| lexiconPath | String | true | false | The path to the emotion lexicon |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| lexiconPath | A valid Path to the emotion lexicon | `src/main/resources/de/julielab/jcore/ae/jemas/lexicons/warriner.vad` |


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Token | `+` |  |
| de.julielab.jcore.types.POSTag | `+` |  |
| de.julielab.jcore.types.Lemma | `+` |  |
| de.julielab.jcore.types.LexicalDocumentEmotion | | `+` |


### Reference
[1] TODO
