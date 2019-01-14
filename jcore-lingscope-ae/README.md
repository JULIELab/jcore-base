# JCoRe Lingscope AE


This component marks spans of biomedical text to lie in the scope of a negation or hedge. The Lingscope (https://sourceforge.net/projects/lingscope/) algorithm is used to do this.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| CueModel | String | true | false | The model that is used to recognize the negation or hedge cue words in text. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE. |
| ScopeModel | String | true | false | The model that is used to detect the scope of a previously found negation or hedge cue word. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE. |
| LikelihoodDict | String | false | false | String parameter indicating path to likelihood dictionary (One entry per line; Entries consist of tab-separated lemmatized likelihood indicators and assigned likelihood category). The dictionary passed here is only used to assign likelihood scores (low, medium, high) to negation and hedge cues. It is not used to detect the cues in the first place. |
| IsNegationAnnotator | Boolean | false | false | If set to true, the recognized cue words will all be assigned the 'negation' likelihood, even if the model used is a hedge model. |

**2. Predefined Settings**

Lingscope supports a range of different models for negation and hedging. This project can be run with any of them but none are preconfigured.

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence | `+` |  |
| de.julielab.jcore.types.Token | `+` |  |
| de.julielab.jcore.types.PennBioIEPOSTag | `+` |  |
| de.julielab.jcore.types.LikelihoodIndicator |  | `+` |
| de.julielab.jcore.types.Scope |  | `+` |


[1] ﻿Agarwal, S., & Yu, H. (2010). Biomedical negation scope detection with conditional random fields. *Journal of the American Medical Informatics Association*, 17(6), 696–701. https://doi.org/10.1136/jamia.2010.003228
