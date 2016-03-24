# JCoRe MST Parser Analysis Engine
Wrapper for the MST Dependency Parser; comes with a slightly modified version of the MST parser (0.5.1). The changes are mainly for integrating the parser seamlessly into the UIMA workflow.

### Reference
Ryan McDonald, Fernando Pereira, Kiril Ribarov, and Jan Haji\v{c}. 2005. Non-projective dependency parsing using spanning tree algorithms. In HLT-EMNLP’05 – Proceedings of the Conference on Empirical Methods in Natural Language Processing, pages 523–530.

### Source
The MST parser is licensed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0) and available from [sourceforge.net](http://sourceforge.net/projects/mstparser/) and as a [Maven Artifact](http://mvnrepository.com/artifact/net.sourceforge.mstparser/mstparser)

### NOTE!
As of now our Parser depends on a modified version of `trove` namely `jules-trove`. This dependency is shipped in the projects `src/main/resources/repo` folder and marked accordingly in the pom.xml. That is, it should automatically be installed in your Maven repository folder.

As part of our relaunch of JCoRe we tried to move from our modified `trove` version to the one on Maven Central `trove4j`, but to no avail as the accuracy of the parser dropped by around 10 points! To complicate things further, we were not able to get access to the sourcecode of our modified trove and there is no documentation on what was changed so we're not able to reproduce it and have to ship it like this.

#### Shared Ressource Config

### To Do
Investigate what causes the issues with `trove4j` and try to solve it to get the MST parser working with it.

### Evaluation
Running the MST parser with standard settings on the train.lab & test.lab files that are shipped with the original parser results in the following
```
EVALUATION PERFORMANCE:
Tokens: 4639
Correct: 3502
Unlabeled Accuracy: 0.7549040741539125
Unlabeled Complete Correct: 0.125
Labeled Accuracy: 0.7145936624272472
Labeled Complete Correct: 0.07
```
