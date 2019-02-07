# JCoRe Piped Format Writer

**Descriptor Path**:
```
de.julielab.jcore.consumer.ppd.desc.jcore-ppd-writer
```

This component writes CAS data to the piped text annotation format. The format looks like this:

`Although|IN|O CD34|NN|GENE +|SYM|O progenitor|NN|O -|HYPH|O derived|VBN|O immature|JJ|O dendritic|JJ|O cells|NNS|O [...]`

Thus, tokens a are whitespace-separated and token meta information like parts of speech or entity annotations
are appended to the token with the pipe character as a delimiter.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| TypeToLabelMappings | String | true | true | A parameter to define one or multiple mappings from a UIMA type to token labels/classes. A token that is completely overlapped by one of the UIMA types defined in the mapping will be given the mapped label in the PPD output. The format is [qualified type]=[label string / feature path]. I.e. you may map a type to a simple label string or you can read the actual label value from within the type. Examples: "de.julielab.jcore.types.Gene=GENE". This would give all tokens that are complete covered by a Gene annotation the label "GENE" in the PPD output. The mapping "de.julielab.jcore.types.Gene=/specificType" would use the value of the "specificType" feature of a Gene annotation as the label for the covered tokens in the PPD output. |
| MetaDataTypesMapping | String | false | true | A parameter to define one or multiple mappings from a UIMA type to token meta data in the PPD output. The minimal form of the PPD output is "token|label", e.g. "il-2|Gene". Additionally, you may deliver as much information as desired, e.g. the part of speech tag: "il-2|NN|Gene". This is done by defining meta data mappings with this parameter. The mapping has the form "[qualified type]=[feature path]", for example "de.julielab.jcore.types.PennBioIEPOSTag=/value". This will use the feature "value" to fill in the respective meta data slot in the PPD output. The order in which multiple meta data information is written into the PPD is the order you specify in this mapping array. |
| OutsideLabel | String | true | false | The label for all tokens that do not belong to a class of interest. All tokens not covered by at least one UIMA type defined in the TypeToLabelMappings parameter will get this outside label in the PPD output. The default value is "O". |
| OutputFile | String | true | false | The path where the output PPD file should be written to. |
**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| TypeToLabelMappings | <qualified type name>=<PPD label> | `de.julielab.jcore.types.Gene=GENE` |
| MetaDataTypesMapping | <qualified type name>=<feature path> | `de.julielab.jcore.types.PennBioIEPOSTag=/value` |
| OutsideLabel     |      Some string | `O` |


