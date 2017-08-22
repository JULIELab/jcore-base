# JCoRe JEmAS
This is a UIMA AE implementation of the core functionality of [JEmAS, the Jena Emotion Analysis System](https://github.com/JULIELab/JEmAS).

**Descriptor Path**:
```
de.julielab.jcore.ae.jemas.desc.jcore-jemas-ae
```

### Objective
The JULIE Lab Emotion Analysis System (JEmAS) is an UIMA Analysis Engine. It extracts one single emotion score out of an entire document using a lexicon-based approach. Emotion is represented using the psychological Valence-Arousal-Dominance model of affect. Please see the [original stand-alone tool](https://github.com/JULIELab/JEmAS) or the paper (below) for further information. 
### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the JCoRe Type System.

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
[1] Sven Buechel and Udo Hahn: Emotion Analysis as a Regression Problem - Dimensional Models and Their Implications on Emotion Representation and Metrical Evaluation. In: ECAI 2016. 22nd European Conference on Artificial Intelligence. August 29 - September 2, 2016, The Hague, Netherlands, pp. 1114-1122.
