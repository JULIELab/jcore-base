# JCoRe PubTator Format Reader

**Descriptor Path**:
```
de.julielab.jcore.reader.pubtator.desc.jcore-pubtator-reader
```

### Objective
This Collection Reader reads files in [PubTator format](https://www.ncbi.nlm.nih.gov/CBBresearch/Lu/Demo/tmTools/Format.html) or directories containing PubTator-formatted files. Files must have the .txt or .txt.gz extension. In the latter case, the files must be GZIP compressed.

### Requirements and Dependencies
No special dependencies.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| Input | String | yes | no | Path of a PubTator-formatted file or a directory containing such files. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| Input | A regular file path. | data/corpora/pubtator |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Title |  | `+` |
| de.julielab.jcore.types.AbstractText |  | `+` |
| de.julielab.jcore.types.Header |  | `+` |
| de.julielab.jcore.types.Gene |  | `+` |
| de.julielab.jcore.types.GeneResourceEntry |  | `+` |
| de.julielab.jcore.types.Chemical |  | `+` |
| de.julielab.jcore.types.Disease |  | `+` |
| de.julielab.jcore.types.Organism |  | `+` |
| de.julielab.jcore.types.EntityMention |  | `+` |
| de.julielab.jcore.types.ResourceEntry |  | `+` |



### Reference
Chih-Hsuan Wei,  Hung-Yu Kao and Zhiyong Lu1, PubTator: a web-based text mining tool for assisting biocuration, Nucleic Acids Res. 2013 Jul; 41(Web Server issue): W518–W522.
