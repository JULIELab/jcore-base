# Changelog

## v2.6.0 (17/11/2022)
- [**enhancement**] Add an annotation remover. [#154](https://github.com/JULIELab/jcore-base/issues/154)
- [**enhancement**] Add a truncation size parameter to PMC XML multipliers [#153](https://github.com/JULIELab/jcore-base/issues/153)
- [**new component**] Add GNP Multiplier for PMC DB Reader [#152](https://github.com/JULIELab/jcore-base/issues/152)
- [**new component**] Add GNP multipliers for XMI and XML DB multiplier readers [#150](https://github.com/JULIELab/jcore-base/issues/150)
- [**new component**] Add a GNormPlus UIMA component [#147](https://github.com/JULIELab/jcore-base/issues/147)
- [**enhancement**] Allow regular expression feature value filters for EntityConsumer [#146](https://github.com/JULIELab/jcore-base/issues/146)
- [**new component**] Add reader for NLM-Gene [#145](https://github.com/JULIELab/jcore-base/issues/145)
- [**bug**] Fix a bug where half of the DBMultiplierReader IDs were not actually processed [#144](https://github.com/JULIELab/jcore-base/issues/144)
- [**enhancement**] Add H2-based annotation source for AnnotationAdder [#138](https://github.com/JULIELab/jcore-base/issues/138)
- [**enhancement**] Add a feature-based format to the annotation adder [#137](https://github.com/JULIELab/jcore-base/issues/137)
- [**new component**] Add an MMAX2 reader. [#136](https://github.com/JULIELab/jcore-base/issues/136)
- [**enhancement**] Let GNP BioC writer add Gene annotations. [#134](https://github.com/JULIELab/jcore-base/issues/134)
- [**enhancement**] Implement max XMI ID retrieval for the BioC GNormPlus Reader [#133](https://github.com/JULIELab/jcore-base/issues/133)
- [**new component**] Add GNormPlus BioC XML reader [#131](https://github.com/JULIELab/jcore-base/issues/131)
- [**enhancement**] Add a mechanism to avoid mirror subset reset for updated JeDIS document whose text hasn't changed [#130](https://github.com/JULIELab/jcore-base/issues/130)
- [**new component**] Add a GNormPlus BioC Writer [#129](https://github.com/JULIELab/jcore-base/issues/129)
- [**new component**] Add a PMC DB Reader [#128](https://github.com/JULIELab/jcore-base/issues/128)
- [**enhancement**] PMCReader: Add option to omit bibliographical references [#127](https://github.com/JULIELab/jcore-base/issues/127)
- [**bug**] ESConsumer: Fix bugs with cache index when resource file is updated [#126](https://github.com/JULIELab/jcore-base/issues/126)
- [**bug**] Fix Bug in JSBD where Cut Away Types can still be included in the begin of a sentence. [#125](https://github.com/JULIELab/jcore-base/issues/125)
- [**closed**] Map payloads to features for AnnotationAdder [#124](https://github.com/JULIELab/jcore-base/issues/124)
- [**closed**] ES Consumer: Provide persistent Map and Addon terms providers [#123](https://github.com/JULIELab/jcore-base/issues/123)
- [**bug**] JSBD: Fix bug that occurs when cut away types and sentence delimiter types are given [#121](https://github.com/JULIELab/jcore-base/issues/121)
- [**closed**] Add a flow controller that creates a flow based on a CAS annotation [#120](https://github.com/JULIELab/jcore-base/issues/120)
- [**closed**] JCoreOverlapIndex: Return the list instead of a stream [#117](https://github.com/JULIELab/jcore-base/issues/117)
- [**closed**] lingpipe gazetteer: fix offset issues [#116](https://github.com/JULIELab/jcore-base/issues/116)
- [**new component**] Add a consumer to write relations to Neo4j [#113](https://github.com/JULIELab/jcore-base/issues/113)
- [**new component**] Add an embedding writer [#102](https://github.com/JULIELab/jcore-base/issues/102)
- [**new component**] Add a Flair embedding adding component [#101](https://github.com/JULIELab/jcore-base/issues/101)
- [**new component**] Add a FLAIR NER component [#100](https://github.com/JULIELab/jcore-base/issues/100)

---

## v2.5.0 (13/05/2020)
This release of JCoRe includes a lot of fixes, small enhancements and updated dependency libraries.
The employed UIMA version is 2.10.4.
New components include
* JCoRe Flair NER AE
* JCoRe Flair Embedding AE
* JCoRe Cord19 Reader

and more.
---

## v2.4.0 (29/03/2019)
This release sees quite a few bug fixes and streamline in various components.
Most importantly, there are a few new components:

* `jcore-annotation-adder-ae`: Reads annotations from external sources and adds them to the CAS. Helpful for the integration of annotations that could not be created from within UIMA.
* `jcore-db-checkpoint-ae`: This is a [JeDIS](https://github.com/JULIELab/jedis/) component. It helps to set checkpoints within a pipeline that are stored in the database and thus provide collection processing progression information. This component can also mark the end of a pipeline, setting documents that have reached this point in a pipeline to `is_processed` and removing the `is_in_process` flag.
* `jcore-bc2gm-reader`: Allows to read the original format used for the data of the [BioCreative II Gene Mention](https://biocreative.bioinformatics.udel.edu/tasks/biocreative-ii/task-1a-gene-mention-tagging/) challenge.
* `jcore-bc2gmformat-writer` writes CAS annotation in the BC2GM format.
* `jcore-lingscope-ae` wraps the [Lingscope](https://sourceforge.net/projects/lingscope/) code as a UIMA AE. The component needs to be further configured regarding which models to use, possibly requiring downloads from the original project. One concrete configuration has been made available for JCoRe in the [jcore-projects](https://github.com/JULIELab/jcore-projects/tree/v2.4/jcore-lingscope-negation-ae) repository.
* `jcore-mutationfinder-ae` wraps the [MutationFinder](http://mutationfinder.sourceforge.net/) algorithm as a UIMA component. Finds single point gene mutations via regular expressions in text and normalizes them.
* `jcore-ppd-writer` writes CAS annotation data into the piped format (that is, tokens that are annotated with meta data, separated by the pipe character: The|DET blue|ADJ house|NN.|.). This format is used be the [JNET](https://github.com/JULIELab/jcore-base/tree/master/jcore-jnet-ae) named entity recognizer for training.


---

## v2.3.8.1 (03/01/2019)
This is the first bugfix release for version 2.3.8, hence 2.3.8.1. JeDIS components have reached version 2.3.9.
This is a critical bugfix release for JeDIS. There was a bug in the `jcore-xmi-splitter` that could destroy data consistence in the database when adding annotation after the base document was already written.
---

## v2.3.8 (30/12/2018)
This is a maintance release and recommended to use.
A lot of components, most notable the [JeDIS](https://julielab.de/jedis/)-related components, have been updated and bugfixed quite a bit.
The version jump is attributed to minor version bumps of the updated components.
---

## v2.3.3 (10/12/2018)
This is a bug fix release but even more so a consolidation release as to have all components using the same parent POM.
---

## v2.3.0 (11/08/2018)
This update sees a lot of cleaning (e.g. descriptor versions) as well as new components. Amongst others, the [JeDIS](https://julielab.de/jedis/) components that realize a UIMA-based annotation warehouse around a Postgres database have been added. Those consist of the `jcore-xmi-db-writer`, the `jcore-xmi-db-reader` and the `jcore-xmi-splitter`, where the latter is at at the core at the two former components and found in the JCoRe Dependencies repository.

For more new components, browse the repository list associated with this release.
---

## JCoRe 2.2.0 - The medical & DH Update (23/09/2016)
This release features a new reader (jcore-dta-reader) and changes so that medical models (namely FraMed) could be trained with jsbd and jtbd.
The MST Parser was dismissed due to ongoing problems with our implementation and the trove library.
We're planning on having a release 2.3.0 in short time that incorporates MATE Tools as new dependency parser.
If you're planning on using a dependency parser in the time between the two releases please refer to JCoRe 2.1.0.

---

## JCoRe Base 2.1.0 (10/05/2016)

---

## JCoRe Base 2.0.0 (04/11/2015)
This is the 2.0.0 release version of the JULIE Component Repository (JCoRe) with which we changed our SCM from a private svn to a public git.
The components within come without model. We're planning on releasing a compatible repository (JCoRe Projects) that features said models.
