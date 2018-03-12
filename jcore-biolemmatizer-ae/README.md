# JCore BioLemmatizer Analysis Engine

**Descriptor Path**:
```
de.julielab.jcore.ae.biolemmatizer.desc.jcore-biolemmatizer-ae
```

### Objective
This AE uses the BioLemmatizer 1.2 as lemmatization tool for morphological analysis of biomedical 
literature downloaded from SourceForge. It analyses standard English lexems as well as 
terms that are specific for the biomedical domain.  


### Requirements and Dependencies  
The BioLemmatizer 1.2 is needed. The BioLexicon which is used for lemmatization is part of the 
'fat jar' (biolemmatizer-core-1.2-jar-with-dependencies.jar) distributed by the source link below.


**1. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.POSTag | `+` |  |
| de.julielab.jcore.types.Token | `+` |  |
| de.julielab.jcore.types.Lemma |  | `+` |


### Reference

Haibin Liu, Tom Christiansen, William A Baumgartner Jr, and Karin Verspoor 
BioLemmatizer: a lemmatization tool for morphological processing of 
biomedical text Journal of Biomedical Semantics 2012, 3:3.

Thompson P, McNaught J, Montemagni S, Calzolari N, del Gratta R, 
Lee V, Marchi S, Monachini M, Pezik P, Quochi V, Rupp C, Sasaki Y, 
Venturi G, Rebholz-Schuhmann D, Ananiadou S: The BioLexicon: 
a large-scale terminological resource for biomedical text mining. 
BMC Bioinformatics 2011, 12:397.


**Source Link**
https://sourceforge.net/projects/biolemmatizer/files/
