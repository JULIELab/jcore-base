# JCoRe Clinical Trials Reader

**Descriptor Path**:
```
de.julielab.jcore.reader.ct.desc.jcore-ct-reader
```

A UIMA reader for clinical trials as provided by [ClinicalTrials.gov](https://clinicaltrials.gov/).

The reader reads most of the information encoded into the XML documents and annotates them with specific types for this kind of documents, for example

* `BriefTitle`
* `OfficialTitle`
* `Summary`
* `Description`
* `OutcomeMeasure`
* `OutcomeDescription`
* `Condition`
* `InterventionType`
* `InterventionName`
* `ArmGroupDescription`
* `Inclusion`
* `Exclusion`
* `MeshHeading`

and more. If you seek a specific element that is missing from the enumeration, it is best to search for it in the
`de.julielab.jcore.reader.ct.ClinicalTrialsReader` class which is not very large.

Thus, lacking elements are, for example, the authors or other bibliographic data. Those information were not needed
for the original task for the reader. We welcome contributions to enhance it.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| InputDirectory | String | true | false | The root directory that contains the clinical trials. The reader supports a subdirectory structure where the actual XML documents are located on deeper levels. |
| FileNames | String | false | true | For debugging: Restrict the documents read to the given document file names. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| InputDirectory | A platform-dependent file path. | `/data/corpora/clinicaltrials` |
| FileNames | A whitelist of files to read. Files not on this list will be skipped. | `[NCT01259219.xml, NCT01259674.xml]` |

