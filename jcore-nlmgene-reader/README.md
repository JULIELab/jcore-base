# JCoRe Component Skeleton
`Text that describes the component in brevity...`

**Descriptor Path**:
```
de.julielab.jcore.{reader, ae, consumer}.NAME.desc.ARTIFACT-NAME
```

`More thorough description`
`Are there any requirements or dependencies for this component?`

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


NLM-Gene annotation code meanings (taken from the file `NLM-Gene-Annotation-Guidelines.docx` on the FTP server linked in the paper):

* 000: Mention is not explicitly linked to a species; use the gene ID of the mention at another text position where the species is specified.
* 111: The given ID is actually the ID of an ortholog of it because the gene does not yet have an ID in NCBI Gene. The used ID should stem from the article, if such an ortholog is mentioned there.
* 222: This is a family/group/class of genes. Annotate with all the gene IDs of that family/group/class that appear in the same article.
* 333: This ia a family/group/class but none of its members were used in the abstract. Use some family member gene that belongs to the main organism discussed in the article. This code is also used for references to protein domains.
* 444: This is a protein complex. Analogous to families, use the ID of the subunits mentioned in the article.
* 555: This is a protein complex without mentions of subunits in the same article. Use the ID of some subunit that belongs to the main organism of the abstract.

Gene annotations with multiple IDs:
* for enumerations with ellipsis, IDs are separated by semicolons
* for other text phrases that have multiple IDs, IDs are separated by commas
* for some IDs, their homologene-ID is also given, separated by a pipe (this does not seem to be documented anywhere; for this reason, the homologene-ID is stripped by this reader)

[1] Islamaj, R., Wei, C. H., Cissel, D., Miliaras, N., Printseva, O., Rodionov, O., … Lu, Z. (2021). NLM-Gene, a richly annotated gold standard dataset for gene entities that addresses ambiguity and multi-species gene recognition. Journal of Biomedical Informatics, 118(March), 103779. https://doi.org/10.1016/j.jbi.2021.103779
