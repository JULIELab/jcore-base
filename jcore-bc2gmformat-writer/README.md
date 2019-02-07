# JCoRe BioCreative II Gene Mention Format Writer

**Descriptor Path**:
```
de.julielab.jcore.consumer.bc2gmformat.desc.jcore-bc2gmformat-writer
```

This component allows to write CAS entity annotations into the format used for the BioCreative II Gene Mention challenge [1].



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| OutputDirectory | String | true | false | The directory to store the sentence and gene annotation files. |
| SentencesFileName | String | true | false | The name of the file that will contain the sentences, one per line. |
| GenesFileName | String | true | false | The name of the file that will contain the gene mention offsets for each sentence. |



[1] Smith, Lawrence H., & Tanabe, Lorraine K., & Johnson [nee Ando], Rie, & Kuo, Cheng-Ju, & Chung, I-Fang, & Hsu, Chun-Nan, & Lin, Yu-Shi, & Klinger, Roman, & Friedrich, Christoph M., & Ganchev, Kuzman, & Torii, Manabu, & Liu, Hongfang, & Haddow, Barry, & Struble, Craig A., & Povinelli, Richard J., & Vlachos, Andreas, & Baumgartner Jr., William A., & Hunter, Lawrence E., & Carpenter, Bob, & Tsai, Richard Tzong-Han, & Dai, Hong-Jie J., & Liu, Feng, & Chen, Yifei, & Sun, Chengjie, & Katrenko, Sophia, & Adriaans, Pieter W., & Blaschke, Christian, & Torres, Rafael, & Neves, Mariana Lara, & Nakov, Preslav I., & Divoli, Anna, & Maña López, Manuel J., & Mata, Jacinto, & Wilbur, W. John (2008). Overview of BioCreative II gene mention recognition. in: Genome Biology, 9, S2.

