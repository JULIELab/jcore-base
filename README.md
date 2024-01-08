![Build Status](https://github.com/JULIELab/jcore-base/actions/workflows/maven.yml/badge.svg) [![Automated Release Notes by gren](https://img.shields.io/badge/%F0%9F%A4%96-release%20notes-00B2EE.svg)](https://github-tools.github.io/github-release-notes/)

# JCoRe Base

Base modules of JCoRe as described in (please cite this paper if you publish results using JCoRe):

```Udo Hahn, Franz Matthies, Erik Faessler and Johannes Hellrich: UIMA-Based JCoRe 2.0 Goes GitHub and Maven Central ― State-of-the-Art Software Resource Engineering and Distribution of NLP Pipelines. In: Nicoletta Calzolari (Conference Chair), Khalid Choukri, Thierry Declerck, Marko Grobelnik, Bente Maegaard, Joseph Mariani, Asuncion Moreno, Jan Odijk, Stelios Piperidis (Eds.): Proceedings of the Tenth International Conference on Language Resources and Evaluation (LREC 2016), 2016. Portorož, Slovenia.``` [[Full Text](http://www.lrec-conf.org/proceedings/lrec2016/pdf/774_Paper.pdf)]

### Objective
These are the base components of the JULIE Lab Component Repository (JCoRe), an open software repository for full-scale natural language processing based on the UIMA middleware framework. JCoRe offers a broad range of text analytics (mostly) for English-language scientific abstracts and full-text articles, especially for the biology domain.
In order to automate the builds of complex NLP pipelines and properly represent and track dependencies of the underlying Java code, all our components are also available as artifacts from Maven Central.
A description for each individual component can be found in their respective `README.md`.

### Requirements & Dependencies
In order to use our components you need at least [JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html) (Java SE Development Kit 11), [UIMA 2.x](https://uima.apache.org/index.html) & [Maven 3](https://maven.apache.org/). We develop with the [Eclipse IDE for Java Developers](http://www.eclipse.org/downloads/) and [IntelliJ IDEA](https://www.jetbrains.com/idea/) Java IDEs. If course you're free to try it with different versions or tools than those mentioned, but we can't make promises for a flawless functioning of our components in these cases.

### UIMA's Collection Processing Engine (CPE)
UIMA offers a relatively easy way to combine UIMA components together in order to analyze a collection of artifacts. If you're not firm or willing to deal with Java Code, the usage of a CPE might be the right choice.
For more detailed information see [UIMA's CPE Documentation](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tutorials_and_users_guides/tutorials_and_users_guides.html#ugr.tug.cpe).

A newer alternative is [UIMA AS](https://uima.apache.org/doc-uimaas-what.html). It is today's officially recommended way to use and scale UIMA pipelines. Our existing CPE infrastructure serves us well, however, so we mostly stick to those for the time being.

### JCoRe UIMA Pipeline Builder

Most CPE configurations consisting of JCoRe components can be easily built using the [JCoRe UIMA Pipeline Builder](https://github.com/JULIELab/jcore-pipeline-modules).
This is a Java program that offers a simple command line interface for the creation of CPEs. There is also support for UIMA AS.

### Maven Artifacts
If not stated otherwise, all the components found in this project are at least in their latest release version also available as Maven artifacts:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>COMPONENT-NAME</artifactId>
    <version>${jcore-version}</version>
</dependency>
```
Where `COMPONENT-NAME` is exactly the same as the name on GitHub.
For instance, to get the Acronym Resolver, include this in your Maven dependencies:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>jcore-acronym-ae</artifactId>
    <version>${jcore-version}</version>
</dependency>
```
The version variable `${jcore-version}` is defined in the jcore-parent pom and should not be edited manually, as it ensures compatibility. An exemption from this rule are bugfix version, which might also be available from Maven Central. If for instance we deployed a first bugfix version for the `jcore-acronym-ae` from the `2.0.0` release, the Maven coordinates would be the following:
```
<dependency>
    <groupId>de.julielab</groupId>
    <artifactId>jcore-acronym-ae</artifactId>
    <version>2.0.1</version>
</dependency>
```
*(some more information on using components the Java/Maven way)*

### Components with pretrained models
For some components the "Base" version won't be sufficient if you're **not** planning on training your own model but rather want to use them out-of-the-box in a prediction pipeline. As of now all of the following components have a specialized project with pretrained models (available from the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects)):
*   jcore-biosem-ae
*   jcore-jnet-ae
*   jcore-jpos-ae
*   jcore-jsbd-ae
*   jcore-jtbd-ae
*   jcore-mstparser-ae
*   jcore-opennlp-chunk-ae
*   jcore-opennlp-parser-ae
*   jcore-opennlp-postag-ae
*   jcore-opennlp-sentence-ae
*   jcore-opennlp-token-ae
*   jcore-xml-reader

Please refer to the [JCoRe Projects Pages](https://github.com/JULIELab/jcore-projects) for information on how to use them in your pipeline.

### Prebuilt pipelines
For illustration purposes we provide some pipelines that utilize our components and can be used as a template if you want to build your own either with a UIMA CPE or as a Java project. As of now, these pipelines exist:
*   [BioSEM Relation Extraction Pipeline; BioNLP ST11 model, English](https://github.com/JULIELab/jcore-pipelines/tree/master/jcore-relation-extraction-pipeline) *(Java and CPE pipeine)*
*   [Biomedical Named Entity Tagger Pipeline; English](https://github.com/JULIELab/jcore-pipelines/tree/master/jcore-named-entity-pipeline) *(CPE pipeline)*
*   [Medical POS Pipeline; German](https://github.com/JULIELab/jcore-pipelines/tree/master/jcore-medical-pos-pipeline)

## Create a JCoRe Base Release
To create a new release, the following steps should be performed
*   Set the release version via `scripts/setProjectVersion.sh`. This automatically updates the meta descriptors.
*   If `jcore-dependency` modules (located in the `jcore-dependency` repository) have changed, those have to be deployed first. Install, from this repository, `jcore-types` and `jcore-utilities` locally as they are required by some of the `jcore-dependency` projects (might not actually be necessary):
    *   Locally install `jcore-types`: `mvn install -f jcore-types`.
    *   Locally install `jcore-utilities`:`mvn install -f jcore-utilities`.
    *   Release the required `jcore-dependencies` modules. 
