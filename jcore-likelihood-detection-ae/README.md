# JCoRe Likelihood Detection AE

**Descriptor Path**:
```
de.julielab.jcore.ae.likelihooddetection.desc.jcore-likelihood-detection-ae
```

Analysis Engine to detect epistemic modal expressions and assign the appropriate likelihood category.



**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| LikelihoodDict | String | yes | no | File or classpath location of the dictionary to use. |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| LikelihoodDict | `de/julielab/jcore/ae/likelihooddetection/resources/likelihood_neg_invest_dict` | Full dictionary with negation and investigation clue words. |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.TYPE |  | `+` |
| de.julielab.jcore.types.ace.TYPE | `+` |  |

**4. References**

Engelmann, Christine, & Hahn, Udo (2014). An empirically grounded approach to extend the linguistic coverage and lexical diversity of verbal probabilities. In: _CogSci 2014 - Proceedings of the 36th Annual Cognitive Science Conference. Cognitive Science Meets Artificial Intelligence: Human and Artificial Agents in Interactive Contexts._ Québec City, Québec, Canada, July 23-26, 2014., 451-456.
