# JCoRe GNormPlus Annotator

**Descriptor Path**:
```
de.julielab.jcore.ae.gnp.desc.jcore-gnormplus-ae
```

Wrapper for the JULIE Lab variant of the GNormPlus gene ID mapper.



**1. Parameters**

Note that these are not all parameters but just those that are the least self-explanatory.

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| SkipUnchangedDocuments | Boolean | false | false | Whether GNormPlus should run on documents that exist in the database and have unchanged text contents. |
| AddToVisitKeys | Boolean | false | false | Whether to add the value of the `ToVisitKeys` parameter to the CAS. See description of `ToVisitKeys` for more details. |
| AddUnchangedDocumentTextFlag | Boolean | false | false | Whether to set a flag in the CAS that stores the information if the document text has changed in comparison to a potentially existing document in the database with the same document ID. Used by downstream components like the XMI DB Writer to manage actions depending on (un)changed text contents. |
| ToVisitKeys | String[] | false | true | The UIMA-aggregate-keys of the pipeline components that should still be visited if the document text is unchanged in comparison to a potentially existing document in the database with the same ID. In CoStoSys pipelines we want to run the DB Checkpoint AE in any way, unchanged document text or not, because we want to keep track of all documents if they have already been processed by the pipeline. |

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
