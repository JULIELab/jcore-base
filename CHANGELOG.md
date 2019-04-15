# Changelog

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
