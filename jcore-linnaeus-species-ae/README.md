# JCoRe LINNAEUS Species Analysis Engine
A species mention recognition engine.

### Objective
JCoRe LINNAEUS Species AE is an UIMA Analysis Engine that wraps the [LINNAEUS](http://linnaeus.sourceforge.net/) species tagger as a UIMA component. It recognizes species mentions in document text and creates an `Organism` annotation for each mention. Each mention is also mapped to an [NCBI Taxonomy](https://www.ncbi.nlm.nih.gov/taxonomy) identifier stored as a `ResourceEntry` in `Organism#resourceEntryList`.

### Requirements and Dependencies
The input and output of an AE is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).


### Using the AE - Descriptor Configuration
For this component this "Base" version won't work out-of-the-box since no dictionaries are included. As of now this component has specialized projects with one dictionary each (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)).
Please refer to this link for information on how to use them in your pipeline.


**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| ConfigFile | String | yes | no | Path to or classpath location of the LINNAEUS configuration file. |

**2. Predefined Settings**

None.


**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Organism |  | `+` |
| de.julielab.jcore.types.ResourceEntry |  | `+` |


### Reference
* Gerner M., Nenadic, G. and Bergman, C. M. (2010) LINNAEUS: a species name identification system for biomedical literature. BMC Bioinformatics 11:85. 
* http://linnaeus.sourceforge.net/