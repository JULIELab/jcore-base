# JCoRe MST Parser Analysis Engine
Wrapper for the MST Dependency Parser

### Reference
Ryan McDonald, Fernando Pereira, Kiril Ribarov, and Jan Haji\v{c}. 2005. Non-projective dependency parsing using spanning tree algorithms. In HLT-EMNLP’05 – Proceedings of the Conference on Empirical Methods in Natural Language Processing, pages 523–530.

### NOTE!
As of now the Parser depends on a modified version of "trove" namely "jules-trove" ("trove" didn't feature things we needed back then). This dependency is shipped in the project repo folder and marked accordingly in the pom.xml.

To do: test if the new version of "trove" works now. If so, change dependency from "jules-trove" to "trove" (Maven Central) and retrain the model.
